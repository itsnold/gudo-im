import java.io.Console;
import java.util.Arrays;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    private static final Scanner input = new Scanner(System.in);

    public static void main(String[] args) {
        // Read connection settings from the environment, or use the local defaults.
        String url = System.getenv().getOrDefault("DB_URL",
                "jdbc:mysql://127.0.0.1:3306/campus_equipment_borrowing");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = databasePassword(user);

        // Connector/J opens the MySQL connection; try-with-resources closes it on exit.
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to MySQL successfully!");
            // Show the menu repeatedly until the user chooses to exit.
            while (true) {
                System.out.println("\n=== CAMPUS EQUIPMENT BORROWING ===");
                System.out.println("1. View records");
                System.out.println("2. Insert equipment");
                System.out.println("3. Update equipment");
                System.out.println("4. Delete equipment");
                System.out.println("5. Run reports");
                System.out.println("0. Exit");
                String choice = prompt("Choose an option: ");
                if (choice == null || choice.equals("0")) break;
                try {
                    switch (choice) {
                        case "1": view(connection); break;
                        case "2": insert(connection); break;
                        case "3": update(connection); break;
                        case "4": delete(connection); break;
                        case "5": reports(connection); break;
                        default: System.out.println("Invalid option.");
                    }
                } catch (SQLException e) {
                    System.out.println("Database error: " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }
            }
            System.out.println("Goodbye!");
        } catch (SQLException e) {
            System.out.println("Could not connect to MySQL: " + e.getMessage());
            System.out.println("Check that MySQL is running, the database exists, and your credentials and JDBC driver are correct.");
            System.exit(1);
        }
    }

    private static String databasePassword(String user) {
        String password = System.getenv("DB_PASSWORD");
        Console console = System.console();
        if (password != null || console == null) return password == null ? "" : password;
        char[] entered = console.readPassword("MySQL password for %s: ", user);
        if (entered == null) return "";
        try {
            return new String(entered);
        } finally {
            Arrays.fill(entered, '\0');
        }
    }

    private static void reports(Connection connection) throws SQLException {
        String[] titles = {
            "Available equipment",
            "Currently borrowed equipment",
            "Overdue equipment",
            "Pending borrowing requests",
            "Requests per user",
            "Equipment never released",
            "Items per category",
            "Users with multiple requests",
            "Unresolved maintenance issues"
        };
        String[] sources = {
            "v_available_equipment ORDER BY item_name",
            "v_currently_borrowed ORDER BY due_at, item_name",
            "v_overdue_equipment ORDER BY due_at, borrower",
            "v_pending_requests ORDER BY requested_at",
            "v_requests_per_user ORDER BY total_requests DESC, full_name",
            "v_equipment_never_released ORDER BY item_name",
            "v_items_per_category ORDER BY total_items DESC, category_name",
            "v_users_multiple_requests ORDER BY total_requests DESC, full_name",
            "v_unresolved_maintenance ORDER BY reported_at"
        };
        System.out.println("\nChoose a report:");
        for (int i = 0; i < titles.length; i++) {
            System.out.println((i + 1) + ". " + titles[i]);
        }
        System.out.println("0. Return to main menu");
        displaySelection(connection, titles, sources, "Report number: ");
    }

    private static void view(Connection connection) throws SQLException {
        // The user can view any of these six database tables.
        String[] tables = {"USER_ACCOUNT", "CATEGORY", "EQUIPMENT_ITEM",
                "BORROW_TRANSACTION", "BORROW_ITEM", "MAINTENANCE_RECORD"};
        System.out.println("\nChoose a table:");
        for (int i = 0; i < tables.length; i++) {
            System.out.println((i + 1) + ". " + tables[i]);
        }
        displaySelection(connection, tables, tables, "Table number: ");
    }

    private static void displaySelection(Connection connection, String[] titles, String[] sources,
                                         String promptText) throws SQLException {
        String choice = required(promptText);
        if (choice.equals("0")) return;
        int index;
        try {
            index = Integer.parseInt(choice) - 1;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a valid menu number.");
        }
        if (index < 0 || index >= sources.length) {
            throw new IllegalArgumentException("Enter a valid menu number.");
        }
        display(connection, titles[index], "SELECT * FROM " + sources[index]);
    }

    private static void display(Connection connection, String title, String query) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet rows = statement.executeQuery()) {
            // Metadata provides the column names for whichever table was chosen.
            ResultSetMetaData metadata = rows.getMetaData();
            int columns = metadata.getColumnCount();
            System.out.println("\n--- " + title + " ---");
            boolean found = false;
            while (rows.next()) {
                found = true;
                for (int i = 1; i <= columns; i++) {
                    if (i > 1) System.out.print(" | ");
                    System.out.print(metadata.getColumnLabel(i) + ": " + rows.getString(i));
                }
                System.out.println();
            }
            if (!found) System.out.println("No records found.");
        }
    }

    private static void insert(Connection connection) throws SQLException {
        // MySQL generates the item ID and applies the default equipment statuses.
        int categoryId = positiveId("Category ID (view categories to see IDs): ");
        String tag = required("Asset tag: ");
        String name = required("Equipment name: ");
        String location = required("Location: ");
        String sql = "INSERT INTO EQUIPMENT_ITEM (category_id, asset_tag, item_name, location) VALUES (?, ?, ?, ?)";
        // Prepared statements keep user input separate from the SQL command.
        try (PreparedStatement statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, categoryId);
            statement.setString(2, tag);
            statement.setString(3, name);
            statement.setString(4, location);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                System.out.println("Equipment inserted successfully" + (keys.next() ? " (ID: " + keys.getLong(1) + ")" : "") + ".");
            }
        }
    }

    private static void update(Connection connection) throws SQLException {
        // Change the name and location of the equipment with the selected ID.
        int id = positiveId("Equipment ID to update: ");
        String name = required("New equipment name: ");
        String location = required("New location: ");
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE EQUIPMENT_ITEM SET item_name = ?, location = ? WHERE item_id = ?")) {
            statement.setString(1, name);
            statement.setString(2, location);
            statement.setInt(3, id);
            System.out.println(statement.executeUpdate() > 0 ? "Equipment updated successfully." : "Equipment ID not found.");
        }
    }

    private static void delete(Connection connection) throws SQLException {
        // Ask for confirmation; MySQL prevents deletion of referenced equipment.
        int id = positiveId("Equipment ID to delete: ");
        String confirmation = required("Delete equipment " + id + "? Type YES to confirm: ");
        if (!confirmation.equals("YES")) {
            System.out.println("Delete cancelled.");
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM EQUIPMENT_ITEM WHERE item_id = ?")) {
            statement.setInt(1, id);
            System.out.println(statement.executeUpdate() > 0 ? "Equipment deleted successfully." : "Equipment ID not found.");
        }
    }

    private static int positiveId(String message) {
        // IDs must be positive whole numbers.
        try {
            int id = Integer.parseInt(required(message));
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
            // Show the same message for non-numeric and non-positive IDs.
        }
        throw new IllegalArgumentException("ID must be a positive whole number.");
    }

    private static String required(String message) {
        // Required fields cannot be blank.
        String value = prompt(message);
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("Input cannot be empty.");
        return value;
    }

    private static String prompt(String message) {
        System.out.print(message);
        return input.hasNextLine() ? input.nextLine().trim() : null;
    }
}
