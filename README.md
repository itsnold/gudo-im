# IM

Campus Equipment Borrowing and Inventory Management System.

Both PDFs and the submitted SQL are in `sources/`. The SQL matches the W2 document: six tables, sample records and report queries. This looks like the file Andrew was asking for. Check with the group if there's a newer copy.

Start with the [walkthrough](docs/WALKTHROUGH.md). It covers setup, the tables, what to demo and questions we should be ready for.

`practice.sql` uses a separate database and removes a MySQL foreign-key/check conflict from the submitted script. `demo.sql` has the queries to walk through. The files in `sources/` are unchanged.

The original SQL starts with `DROP DATABASE`. Use a separate practice instance, not a database with work you want to keep. The practice copy still needs an import check on MySQL; there's no MySQL server available here to run it.
