# IM walkthrough

Our system is the Campus Equipment Borrowing and Inventory Management System. It keeps track of who borrowed equipment, which physical units they took and whether those units have been returned or need maintenance.

## Files

Everything is in this folder:

- `4-311Group_FiveNightsAtGudoW1.pdf`: the proposal, business rules and first ERD.
- `4-311Group_FiveNightsAtGudoW2.pdf`: the relational schema, normalization, data dictionary and updated ERD.
- `4-311Group_FiveNightsAtGudoW2.sql`: the database setup, sample records and queries.

The SQL matches the W2 document. It looks like the file Andrew was asking for, though we should check if someone has a newer copy.

## Open the database first

Use a local MySQL server with Workbench connected to it. MySQL 8.4 or 8.0.16+ supports the checks used in the script. To see the server version, run:

```sql
SELECT VERSION();
```

Open `4-311Group_FiveNightsAtGudoW2.sql` in Workbench. Before running it, look at the first line of SQL: it drops `campus_equipment_borrowing` and creates it again. Use a throwaway MySQL instance. Don't run it against a database with work we need to keep.

Run the file and check for errors before moving on. A full MySQL import hasn't been tested here yet.

There's one import issue to watch for. `processed_by` is used in both a foreign key with `ON UPDATE CASCADE` and the `chk_transaction_status` check. MySQL restricts that combination. If the import stops on that constraint, change only this foreign key in the open editor tab to:

```sql
CONSTRAINT fk_transaction_processor FOREIGN KEY (processed_by)
    REFERENCES USER_ACCOUNT (user_id),
```

That keeps the foreign key and status check, but uses the default restrictive actions instead of cascading updates. Leave the other foreign keys alone. The submitted file in this repo is unchanged. Restarting the full script will reset its database, so only do that on the throwaway instance.

Once the import works, select individual queries to run. We don't need to keep rerunning the setup.

## Check what got imported

The script already has `SHOW TABLES`, a `SELECT *` for each table and a row-count query near the bottom. Start there. The sample data should give:

| Table | Rows |
| --- | ---: |
| USER_ACCOUNT | 10 |
| CATEGORY | 5 |
| EQUIPMENT_ITEM | 12 |
| BORROW_TRANSACTION | 9 |
| BORROW_ITEM | 15 |
| MAINTENANCE_RECORD | 4 |

These counts were checked against the supplied records. If a fresh import gives different results, fix that before practicing the presentation.

## Explain the tables

`USER_ACCOUNT` is for students, staff and admins. It stores their role and whether the account is active. University IDs and emails have to be unique.

`CATEGORY` groups equipment by type, like cameras or projectors. `EQUIPMENT_ITEM` stores each physical unit. Two projectors of the same model still need separate records and asset tags because one could be borrowed while the other is under repair.

`BORROW_TRANSACTION` is the request. It records the borrower, dates, status and whoever processed it. `processed_by` can be empty while the request is pending.

`BORROW_ITEM` lists the units in that request. Its primary key is the pair `(transaction_id, item_id)`. This stops the same unit being listed twice in one request, but lets that unit appear in later requests.

`MAINTENANCE_RECORD` stores an issue with a unit, who reported it and whether it's been resolved.

The reason borrowing uses two tables is that one request can contain several units. The request details belong in `BORROW_TRANSACTION`, while each unit's condition and return time belong in `BORROW_ITEM`. That lets someone return a camera first and a laptop later without marking the whole request as returned.

## Follow one request

A request starts as `PENDING`. Staff can approve or reject it, or it can be cancelled. An approved request can be released. It should only be closed once all its units have been returned.

Requesting equipment doesn't reserve it. Availability is decided at release. That's why an approved request and an active loan aren't the same thing.

Use request 3 as an example. It contains `CAM-001` and `LAP-001`. Both have a condition recorded at release, but their return fields are still empty. They're still out with the borrower.

Compare that with request 1. Its camera and tripod both have return times and conditions, and the request is `CLOSED`. The camera appears again in request 3 because equipment can be borrowed repeatedly over time.

## Run the report already in the SQL

Near the bottom, find the query that starts with `SELECT t.transaction_id, u.full_name AS borrower`. It joins the request, borrower, borrowing items and equipment so we can read the loan without looking up IDs ourselves.

The important filter is:

```sql
WHERE t.status = 'RELEASED' AND bi.returned_at IS NULL
```

It keeps units that were actually released and haven't come back. The sample data should return four rows: `CAM-001` and `LAP-001` from request 3, plus `AUD-001` and `AUD-002` from request 4.

The query uses `NOW()` to decide whether each loan is overdue. On October 28, 2026, request 4 is overdue and request 3 isn't yet. The labels will change when we run it on another date; the sample due dates don't change by themselves.

Then run the last two queries. Both should return no rows. The first looks for a unit in more than one active loan. The second looks for a closed request that still has something unreturned.

An empty result means the sample records pass that check. It doesn't mean the database automatically prevents the problem.

## If sir asks about the rules

Primary keys identify rows. Foreign keys stop references to missing records. Unique constraints protect IDs, emails, category names and asset tags. The checks cover things like due dates, release fields, return fields and maintenance-resolution dates.

Some rules still need application code. The SQL doesn't stop two requests releasing the same unit at once, check the processor's role, block an inactive borrower or wait for every item to come back before allowing closure. There are no triggers or stored procedures doing those jobs in this file.

For double-booking, the release operation would need to lock the equipment row, recheck its loans and maintenance, then record the release in the same transaction. Running a report afterward only tells us that something went wrong.

## Normalization, using our own tables

For 1NF, each field holds one value. A request with several units gets several `BORROW_ITEM` rows rather than `item_1`, `item_2` and `item_3` columns.

For 2NF, the return details depend on both parts of the `BORROW_ITEM` key. An item's return time only makes sense for a particular request.

For 3NF, names stay in `USER_ACCOUNT`, category descriptions stay in `CATEGORY` and equipment details stay in `EQUIPMENT_ITEM`. The borrowing tables reference those records instead of copying all their details.

W2 keeps the same six entities as W1 and turns them into the MySQL schema. It also adds the data dictionary, normalization explanation and sample data. One detail to clarify if asked: W1 says every request needs an item, while W2 says at least one item before release. Neither minimum is enforced by a foreign key alone.

Before presenting, agree on who explains the system, who covers the schema and who runs the queries. Everyone should be able to explain one relationship or query. We also still need to confirm whether the session is graded or a consultation.
