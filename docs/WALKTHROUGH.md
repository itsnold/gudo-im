# Presentation notes

The system tracks individual equipment units, borrowing requests and maintenance. Students and staff request equipment. Equipment staff handle approval, release and return. Admins manage accounts, inventory and reports.

## Get it running

Use MySQL 8.4, or MySQL 8.0.16 or newer. Workbench is just the interface; a MySQL server has to be running too. Check what you're connected to:

```sql
SELECT VERSION();
```

The submitted SQL is in `sources/`. Keep that copy as it is. It begins by dropping and recreating `campus_equipment_borrowing`, so running it can erase an existing database with that name.

For practice, use `practice.sql`. It creates `gudo_im_practice` instead, without dropping a database. It also removes the cascading action from the `processed_by` foreign key. MySQL restricts referential actions on columns used in a `CHECK`; that column is also used by `chk_transaction_status`. The foreign key and status check are both kept.

In Workbench, connect to a local practice server, open `practice.sql` and run the whole file. If `gudo_im_practice` already exists, stop and use a fresh practice instance. Don't keep running later statements after a setup error.

If you use the MySQL command-line client instead, run this from the repo folder. Replace `your_mysql_user` with your local database username:

```sh
mysql -u your_mysql_user -p < practice.sql
```

Then open `demo.sql` in Workbench and run its sections one at a time. Or run all the read-only demo queries from the terminal:

```sh
mysql -u your_mysql_user -p < demo.sql
```

The `-p` option prompts for your local MySQL password. This isn't a Daigler login.

The SQL and seed data have been reviewed, but a MySQL import hasn't been run here. Check the import before presenting. The counts and examples below are expected from the supplied data.

## Check the import

`SHOW TABLES` should list six tables. The row-count query should give:

| Table | Rows |
| --- | ---: |
| USER_ACCOUNT | 10 |
| CATEGORY | 5 |
| EQUIPMENT_ITEM | 12 |
| BORROW_TRANSACTION | 9 |
| BORROW_ITEM | 15 |
| MAINTENANCE_RECORD | 4 |

If the counts differ after a clean import, sort that out before rehearsing the queries.

## How a request works

A request starts as `PENDING`. Staff can approve or reject it, or it can be cancelled. An approved request can be released. It can only be closed once every borrowed item has been returned.

Requesting an item doesn't reserve it. Availability is checked when staff release it. The same physical item can't be out with two borrowers at once.

The SQL checks whether a row's status agrees with its processor and release fields. It doesn't enforce the whole sequence of status changes.

## The six tables

`USER_ACCOUNT` stores people, their role and whether their account is active. University IDs and email addresses must be unique. Having role fields doesn't enforce permissions by itself; the application still has to check them.

`CATEGORY` groups equipment by type. A category can have several equipment units.

`EQUIPMENT_ITEM` is one physical unit, identified by a unique asset tag. Two identical projectors still get separate records because they can be borrowed and returned separately.

`BORROW_TRANSACTION` is the request itself. It records the borrower, the staff member who processed it, dates and request status. The processor can be blank while a request is pending.

`BORROW_ITEM` lists the physical units included in a request. Its primary key combines `transaction_id` and `item_id`, so the same unit can't appear twice in one request. Each unit gets its own return time and condition.

`MAINTENANCE_RECORD` records an equipment issue and who reported it. An item can have several maintenance records over time.

## Why borrowing has two tables

One request can contain several items. The request details go in `BORROW_TRANSACTION`; each borrowed unit goes in `BORROW_ITEM`. That also lets staff record partial returns without pretending the whole request has been returned.

A user can make many requests. An equipment unit can appear in many borrowing records over time, but it can't have overlapping unreturned releases.

## Walk through the queries

Open `demo.sql`. Start with the table counts, then `SHOW CREATE TABLE BORROW_ITEM`. Point out the composite primary key and the two foreign keys.

Next, run the active-loan query. It joins the request to its borrower, borrowing items and equipment records. `status = 'RELEASED'` and `returned_at IS NULL` keep only units that are still out.

There should be four rows. Request 3 has `CAM-001` and `LAP-001`. Request 4 has `AUD-001` and `AUD-002`.

The demo uses October 28, 2026 at noon as a fixed sample date. Request 4's two units show as overdue; request 3's units don't. The report in the original SQL uses `NOW()`, so its labels change depending on when it's run.

Then run the availability query. It checks that a unit is active, in good condition, has no unresolved maintenance and isn't part of an unreturned released loan. There should be six units: `ACC-001`, `ACC-002`, `ACC-003`, `CAM-002`, `PRO-001` and `PRO-002`.

`PRO-002` and `ACC-003` are attached to an approved request, but they're still available under the proposal's rule: a request doesn't reserve equipment. The release is what matters. This query is a snapshot, not a lock; release logic still has to recheck the unit inside a transaction.

Run the last two checks. Both should return no rows. One looks for equipment with overlapping active loans. The other looks for closed requests that still have an unreturned item. Empty results mean these sample records pass those checks; the checks don't prevent future mistakes.

## Show a partial return

This step is optional. Run it on the practice database, in one connection, after the read-only demo:

```sql
USE gudo_im_practice;
START TRANSACTION;

UPDATE BORROW_ITEM
SET returned_at = '2026-10-28 12:00:00', condition_in = 'GOOD'
WHERE transaction_id = 3 AND item_id = 1 AND returned_at IS NULL;

SELECT e.asset_tag, bi.returned_at, bi.condition_in
FROM BORROW_ITEM AS bi
JOIN EQUIPMENT_ITEM AS e ON e.item_id = bi.item_id
WHERE bi.transaction_id = 3
ORDER BY e.asset_tag;

ROLLBACK;
```

The camera gets a return time while the laptop is still out. Request 3 must stay released until all its units come back. `ROLLBACK` undoes this practice change. Run it even if you stop the demo early; don't leave the transaction open.

## What the database enforces

Primary keys identify rows. The composite key stops the same unit appearing twice in one request. Unique constraints protect university IDs, emails, category names and asset tags. Foreign keys stop references to missing records.

The checks cover requested due dates, release/due field consistency, status fields, paired return fields and maintenance-resolution dates. For example, `requested_due_at` must be later than `requested_at`.

Some rules still need application code and transaction handling: checking staff roles and active accounts, preventing overlapping releases, blocking damaged units, requiring an item before release, and waiting for all returns before closure. There are no triggers or stored procedures implementing those rules in this file.

If asked how to prevent two staff members releasing the same unit at once, explain that the release operation needs to lock that equipment row, recheck its loans and maintenance, then record the release in the same transaction. A report query alone doesn't solve that race.

## If we're asked about normalization

For 1NF, each field holds one value. Several units in a request become several `BORROW_ITEM` rows, not `item_1`, `item_2` and `item_3` columns.

For 2NF, the return details in `BORROW_ITEM` depend on both parts of the key. The same equipment unit can have different return details on different requests.

For 3NF, borrower details stay in `USER_ACCOUNT`, category details stay in `CATEGORY` and equipment details stay in `EQUIPMENT_ITEM`. The other tables reference them instead of copying those descriptions into each transaction.

W2 keeps the same six entities as W1. It adds the relational schema, data types, normalization explanation and MySQL implementation with sample records and checks. One wording change to clarify if asked: W1 requires every request to have an item, while W2 says at least one item before release. A foreign key alone doesn't enforce either minimum.

## Before class

Decide who explains the problem, who covers the schema and who runs the queries. Everyone should understand at least one relationship or query, even if one person handles the laptop.

Check whether W2 is the newest SQL copy. Also confirm whether the session is graded or a consultation; the available chat didn't settle that.

MySQL's restriction is documented under [CHECK constraints](https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html).
