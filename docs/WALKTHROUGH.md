# IM borrowing-system walkthrough

> **Preparation guide, not a verified runnable demo yet.** The original PDFs and SQL are missing. No database engine, table names, import commands, or expected outputs have been invented. Complete the pending sections after reading and testing the actual files.

## 1. Start here: what are we presenting?

This is the **IM borrowing-system project**, not Five Nights at Gudo's gameplay, Swing screens, or the game's SQLite database.

The immediate goal is to get everyone working from the same verified SQL file and able to explain the system it defines. The available chat does not settle whether the session is graded or a consultation.

## 2. Get everyone on the same version

1. Obtain the original W1 PDF, W2 PDF, and W2 SQL using the links in [sources](../sources/README.md).
2. Read W1 for the original problem and scope; read W2 for the updated work and requirements. Confirm what each document actually says.
3. Compare the W2 PDF's schema with the SQL. List any tables, columns, or relationships that differ.
4. Ask the group whether there is a newer SQL export. Do not silently replace a newer file with an older submission.
5. Agree on one tested version for rehearsal.

**Pending:** source checksums, version decision, and PDF-to-SQL comparison.

## 3. Understand the SQL before importing

Open the `.sql` file in a text editor before executing it.

Identify:

- The intended database engine and version, using the dump header and SQL syntax.
- Database/schema creation statements and whether a database name is fixed in the script.
- Every `CREATE TABLE`, its primary key, and its columns.
- Foreign keys, uniqueness rules, required values, defaults, and other constraints.
- Whether the script includes sample data, views, triggers, or stored routines.
- Any `DROP`, `TRUNCATE`, broad `DELETE`, account-management, or other destructive statements.
- Any credentials or unnecessary personal information that must not be committed or shown in a demo.

**Do not assume MySQL, PostgreSQL, SQL Server, or SQLite.** In particular, the game's storage choice does not establish the IM project's engine.

**Pending:** verified engine/version and reviewed SQL inventory.

## 4. Import into a practice database

1. Use the database software required by the actual SQL and assignment.
2. Create a separate, disposable practice environment. Do not import an unreviewed script into an existing database containing important work.
3. Use the appropriate import tool for the confirmed engine.
4. Confirm that import finishes successfully. Record errors and resolve them deliberately rather than ignoring them.
5. List the resulting tables and inspect their definitions and row counts.
6. Test again from a fresh practice environment so the group knows the setup is reproducible.

**Pending:** exact prerequisites, import commands, clean-import result, and table counts. There is no verified runnable import command yet.

## 5. Explain the schema in plain language

For each real table, fill in:

| Table | What one row represents | Primary key | Foreign keys | Why it exists |
| --- | --- | --- | --- | --- |
| Pending source review | — | — | — | — |

For each relationship, be ready to say:

> “This table connects to that table through this key. The relationship means …, and the database prevents ….”

Use only relationships enforced by the real schema, or explicitly label a rule that exists only in application logic. Do not claim the system prevents duplicate borrowing, unavailable-item loans, or invalid returns until that has been tested or demonstrated from the implementation.

## 6. Rehearse a small, real demonstration

Choose steps that the implemented schema actually supports. Candidate demonstrations—not confirmed assignment requirements—include:

1. Show relevant sample records.
2. Show a borrowing record together with its related records using a real join.
3. Explain the fields that represent a borrowing transaction.
4. Demonstrate a return or status change **only if implemented**.
5. Demonstrate one constraint and explain the expected rejection, if supported.

Use synthetic demo data. Practice any writes in the disposable environment and restore the baseline afterward using a method supported by the confirmed engine.

For each selected query, document:

- The question it answers.
- Its exact SQL.
- The actual result from a test run.
- A one-sentence explanation of the tables, join, and filter used.

**Pending:** tested project-specific queries and expected outputs. Do not present generic example SQL as if it came from the submission.

## 7. Suggested speaking order

These are unassigned roles, not instructions already agreed by the team:

1. **Overview:** the problem, intended users, and actual project scope.
2. **Schema:** the main tables, primary keys, and relationships.
3. **Demo:** setup and two or three tested queries or supported operations.
4. **Limitations and questions:** what works, what is unfinished, and what needs clarification.

Everyone should be able to explain the overview and one relationship or query. Do not leave all database knowledge with the person operating the laptop.

## 8. Questions worth rehearsing

- What makes this a borrowing system, and what is outside its scope?
- Why did you separate these tables?
- What do the primary and foreign keys do?
- What is the cardinality of each relationship, and where is it enforced?
- Which rules prevent invalid data? Which rules are not yet enforced?
- What does this query return, and why does it need this join?
- What changed between W1 and W2?
- What remains incomplete?

Answer using the actual documents, schema, and test results. “Not implemented yet” is better than claiming an unverified feature works.

## Ready-to-present check

- [ ] Original PDFs and SQL obtained and reviewed.
- [ ] Correct SQL version confirmed with the group.
- [ ] Official requirements and session format checked.
- [ ] Engine and prerequisites documented.
- [ ] Clean import succeeds.
- [ ] Schema explanation matches the SQL.
- [ ] Demo queries run and their outputs are recorded.
- [ ] Demo data contains no unnecessary personal information.
- [ ] Presentation roles agreed and rehearsed.
- [ ] Offline copies of the verified sources and walkthrough are available.
