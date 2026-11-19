# Campus Equipment Borrowing and Inventory Management System

Five Nights at Gudo · CS 2137 Information Management · Class 4-311

The Java Swing dashboard manages equipment inventory and displays six record tables and nine live reports from MySQL. Use MySQL Workbench for the graphical SQL editor, data grids, and ER diagram.

## Start here

1. Extract the whole project. With MySQL Community Server installed and running, double-click [Setup-Gudo.bat](Setup-Gudo.bat). It finds Java 21 or attempts to install it with WinGet, downloads the JDBC driver, prepares the database, and opens the Java app.
2. Enter your existing MySQL password in the connection dialog. If Windows asks for permission to install Java, approve the installer and let it finish.
3. On later runs use [Run-Gudo.bat](Run-Gudo.bat). For Workbench or manual setup, use [MYSQL_WORKBENCH_SETUP.md](MYSQL_WORKBENCH_SETUP.md).
4. Follow [Gudo_Demo_Guide.pdf](Gudo_Demo_Guide.pdf) for the live presentation.

| File | Purpose |
| --- | --- |
| `Main.java` | Swing dashboard with records, equipment CRUD, and live reports |
| `database.sql` | Fresh installation and seeded demo data |
| `reports.sql` | The same nine reports for running directly in Workbench |
| Week 1 and Week 2 files | Original proposal, schema submission, and SQL for historical reference |

For the ER diagram, use page 2 of the Week 5 project documentation supplied with the class materials.

The Equipment screen edits equipment name and location and allows staff to add and delete equipment. Accounts, categories, borrowing records, and maintenance are viewable. The database stores lending histories and enforces constraints. Staff still handle approvals, releases, and returns as operating procedures.

## Use the dashboard

- **Records → Equipment** opens the inventory. Select a row to edit its name or location, or delete it after confirming. **Add equipment** lets you pick a category by name.
- **Reports** opens the nine live reports. Select a report in the left sidebar. **Refresh** runs the current query again.

For macOS or Linux, download the same Connector/J JAR to `lib/` using the setup guide, then run `java --class-path lib/mysql-connector-j-9.4.0.jar Main.java` in a terminal.

## Artwork and icons

- Equipment photograph by [Dustin Kirkpatrick](https://commons.wikimedia.org/wiki/File:Camera_equipment_(Unsplash).jpg), CC0. The repository includes a local copy for offline use.
- Grid, chart, refresh, add, edit, and delete icons are adapted from the [Feather set in React Icons](https://react-icons.github.io/react-icons/icons/fi/) and drawn directly in Swing. See [Feather's MIT license](assets/FEATHER-LICENSE.txt).
