# Campus Equipment Borrowing and Inventory Management System

Five Nights at Gudo · CS 2137 Information Management · Class 4-311

The Java console manages equipment inventory and displays six record tables and nine live reports from MySQL. Use MySQL Workbench for the graphical SQL editor, data grids, and ER diagram.

## Start here

1. Extract the whole project. With MySQL Community Server installed and running, double-click [Setup-Gudo.bat](Setup-Gudo.bat). It finds Java 21 or attempts to install it with WinGet, downloads the JDBC driver, prepares the database, and opens the Java app.
2. Enter your existing MySQL password once. If Windows asks for permission to install Java, approve the installer and let it finish.
3. On later runs use [Run-Gudo.bat](Run-Gudo.bat). For Workbench or manual setup, use [MYSQL_WORKBENCH_SETUP.md](MYSQL_WORKBENCH_SETUP.md).
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
