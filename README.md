# Campus Equipment Borrowing and Inventory Management System

Five Nights at Gudo · CS 2137 Information Management · Class 4-311

The Java console manages equipment inventory and displays six record tables and nine live reports from MySQL. Use MySQL Workbench for the graphical SQL editor, data grids, and ER diagram.

## Start here

1. Follow [MYSQL_WORKBENCH_SETUP.md](MYSQL_WORKBENCH_SETUP.md) to set up JDK 21, MySQL 8.4, and Workbench.
2. Import [database.sql](database.sql) once. It creates the six tables, sample records, and nine report views.
3. Double-click [Run-Gudo.bat](Run-Gudo.bat) and enter your MySQL password.
4. Follow [Gudo_Demo_Guide.pdf](Gudo_Demo_Guide.pdf) for the live presentation.

| File | Purpose |
| --- | --- |
| `Main.java` | Java console with record views, equipment CRUD, and the report menu |
| `database.sql` | Fresh installation and seeded demo data |
| `reports.sql` | The same nine reports for running directly in Workbench |
| Week 1 and Week 2 files | Original proposal, schema submission, and SQL for historical reference |

For the ER diagram, use page 2 of the Week 5 project documentation supplied with the class materials.

The final CLI edits equipment name and location and allows staff to add and delete equipment. Accounts, categories, borrowing records, and maintenance are viewable. The database stores lending histories and enforces constraints. Staff still handle approvals, releases, and returns as operating procedures.

For macOS or Linux, download the same Connector/J JAR to `lib/` using the setup guide, then run `java --class-path lib/mysql-connector-j-9.4.0.jar Main.java` in a terminal.
