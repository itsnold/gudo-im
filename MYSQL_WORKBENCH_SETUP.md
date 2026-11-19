# MySQL Workbench setup on Windows

## 1. Install the three tools

- [MySQL Community Server 8.4 LTS](https://dev.mysql.com/downloads/mysql/). Run MySQL Configurator after installation. For a local demo, use TCP port **3306**, set a root password you can remember, and configure MySQL as a Windows service. Start the service.
- [MySQL Workbench](https://dev.mysql.com/downloads/workbench/). Workbench is the graphical client for the server.
- [JDK 21](https://adoptium.net/temurin/releases/?version=21). Enable the installer's PATH option, then open a fresh Command Prompt and run `java -version`. It should show Java 21 or newer.

Extract the complete project ZIP first. Keep `Main.java`, `Run-Gudo.bat`, `database.sql`, and `reports.sql` together.

## 2. Connect Workbench to the local server

1. Open Workbench and click **+** next to **MySQL Connections**.
2. Name it **Gudo Local**. Set **Connection Method** to **Standard (TCP/IP)**, **Hostname** to `127.0.0.1`, **Port** to `3306`, and **Username** to `root`.
3. Click **Test Connection** and enter the MySQL password you set in Configurator. Click **OK**, then open **Gudo Local**.

## 3. Import the project once

1. Open **File > Open SQL Script** and choose `database.sql`.
2. In the query toolbar, ensure **stop on errors** is enabled. Click the lightning bolt that executes the selection or whole script. Leave the editor text unselected so it runs the whole script.
3. Inspect **Action Output** for errors. Refresh **Schemas** in the left sidebar, then expand **campus_equipment_borrowing**, **Tables**, and **Views**.
4. In a new SQL tab, run:

   ```sql
   USE campus_equipment_borrowing;
   SHOW FULL TABLES;
   SELECT COUNT(*) AS equipment_count FROM EQUIPMENT_ITEM;
   ```

   Expect six base tables, nine views, and 12 equipment rows on a fresh install.

The installer creates a fresh schema and has no DROP statements. If the schema already exists, stop and inspect its contents. A working installation can be reused. If an earlier import stopped halfway or you have a Week 2 schema, import on a separate disposable MySQL instance or back up the existing schema before you replace it. Avoid running the historical Week 2 SQL during the demo because that script drops and recreates the schema.

## 4. Launch the Java application

1. Double-click **Run-Gudo.bat**. On first launch it downloads Connector/J 9.4.0 into `lib` using curl.
2. Type the MySQL password at the Java prompt and press Enter. Password characters are hidden.
3. Choose **1**, then **3**, to view inventory. Use **5** for reports and **0** to exit.

If the driver download fails, get [mysql-connector-j-9.4.0.jar](https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/9.4.0/mysql-connector-j-9.4.0.jar) directly and save it as `lib/mysql-connector-j-9.4.0.jar`. Launch the BAT once before class and keep the downloaded JAR with the project for offline use. Connector/J [installation reference](https://dev.mysql.com/doc/connectors/en/connector-j-installing.html).

For a different port or user, open **Command Prompt** in the project folder and set the matching connection values before running the BAT:

```bat
set "DB_URL=jdbc:mysql://127.0.0.1:3307/campus_equipment_borrowing"
set "DB_USER=root"
Run-Gudo.bat
```

Java also accepts `DB_PASSWORD` from the environment for scripted runs. For a live demo, use the hidden password prompt.

## 5. Show data in Workbench

Open `reports.sql` and run its `USE` statement once. Highlight one report SELECT, then click the execute-selection lightning bolt. You can also right-click a table and choose **Select Rows - Limit 1000** to open a results grid. Rerun the SELECT after changing a row in Java to see the new data. [SQL Query Toolbar reference](https://dev.mysql.com/doc/workbench/en/wb-sql-editor-toolbar.html).

Optional ERD view: choose **Database > Reverse Engineer**, select the local connection and the project schema, then select the six tables. [Reverse Engineering reference](https://dev.mysql.com/doc/workbench/en/wb-reverse-engineer-live.html).

## Common fixes

| Symptom | Check |
| --- | --- |
| Can't connect or communications link failure | Start the MySQL service in Windows Services. Match the port in Workbench and `DB_URL`. |
| Access denied | Use the same MySQL username and password that passed **Test Connection**. |
| Unknown database | Run the complete `database.sql` import against the same server. |
| Report view missing | Check Workbench Action Output. An incomplete import or historical Week 2 schema may have tables without views. |
| No suitable driver | Confirm the downloaded file is a JAR at `lib/mysql-connector-j-9.4.0.jar`. Download it again if it was corrupt. |
| Duplicate demo tag | Find the earlier demo row in Equipment, delete it by its ID if it is yours, or insert with a fresh tag. |
