import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class Main {
    private static final String SCHEMA = "campus_equipment_borrowing";
    private static final Pattern SETUP_URL = Pattern.compile(
            "(jdbc:mysql://[^/?]+/)" + SCHEMA + "(\\?.*)?");
    private static final Pattern SQL_OBJECT = Pattern.compile("CREATE (TABLE|VIEW) (\\w+)");
    private static final String URL = System.getenv().getOrDefault("DB_URL",
            "jdbc:mysql://127.0.0.1:3306/" + SCHEMA);
    private static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String[] RECORD_TITLES = {
        "Equipment", "Users", "Categories", "Borrowing requests", "Borrowed items", "Maintenance"
    };
    private static final String[] RECORD_SOURCES = {
        "EQUIPMENT_ITEM ORDER BY item_id", "USER_ACCOUNT ORDER BY user_id",
        "CATEGORY ORDER BY category_id", "BORROW_TRANSACTION ORDER BY transaction_id",
        "BORROW_ITEM ORDER BY transaction_id, item_id", "MAINTENANCE_RECORD ORDER BY record_id"
    };
    private static final String[] REPORT_TITLES = {
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
    private static final String[] REPORT_SOURCES = {
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

    public static void main(String[] args) {
        boolean setup = args.length == 1 && args[0].equals("--setup");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ignored) {
                // Swing's default look and feel remains available.
            }
            new Dashboard(setup).connect();
        });
    }

    private static void setupDatabase(String url, String user, String password)
            throws IOException, SQLException {
        var target = SETUP_URL.matcher(url);
        if (!target.matches()) {
            throw new SQLException("DB_URL must point to the " + SCHEMA + " schema on your MySQL server.");
        }
        List<String> statements = readSetupStatements(Path.of("database.sql"));
        String serverUrl = target.group(1) + (target.group(2) == null ? "" : target.group(2));
        try (Connection connection = DriverManager.getConnection(serverUrl, user, password);
             Statement statement = connection.createStatement()) {
            try {
                connection.setCatalog(SCHEMA);
            } catch (SQLException e) {
                if (e.getErrorCode() != 1049) throw e; // MySQL: unknown database
                for (String sql : statements) {
                    statement.execute(sql);
                }
                System.out.println("Installed the database and loaded the sample records.");
                return;
            }

            try {
                for (String sql : statements) {
                    var object = SQL_OBJECT.matcher(sql);
                    if (object.lookingAt()) {
                        statement.execute("SELECT 1 FROM " + object.group(2) + " LIMIT 0");
                    }
                }
            } catch (SQLException e) {
                throw new SQLException("The existing database could not be verified. Its data was left intact. "
                        + "See MYSQL_WORKBENCH_SETUP.md. MySQL says: " + e.getMessage(), e);
            }
            System.out.println("Found the existing database. Keeping its records.");
        }
    }

    private static List<String> readSetupStatements(Path script) throws IOException {
        List<String> statements = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        // database.sql uses standalone comment lines and ends each statement on its own line.
        for (String line : Files.readAllLines(script)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
            sql.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                statements.add(sql.toString().stripTrailing());
                sql.setLength(0);
            }
        }
        if (sql.length() != 0 || statements.isEmpty()) {
            throw new IOException("database.sql is empty or has an unfinished statement.");
        }
        return statements;
    }

    private record Category(long id, String name) {
        @Override public String toString() { return name; }
    }

    private static final class Dashboard {
        private static final Color NAV = new Color(0x142538);
        private static final Color BG = new Color(0xF1F5F9);
        private static final Color TEXT = new Color(0x142538);
        private static final Color MUTED = new Color(0x516479);
        private static final Color ACCENT = new Color(0x116C64);
        private static final Color LINE = new Color(0xDFE7EF);

        private final JFrame frame = new JFrame("Gudo | Campus Equipment");
        private final JTable table = new JTable();
        private final JLabel page = label("Equipment", 27, TEXT, true);
        private final JLabel pageHint = label("Browse physical assets and manage inventory.", 13, MUTED, false);
        private final JLabel status = label("Waiting for MySQL", 12, MUTED, false);
        private final JPanel navigation = new JPanel(new GridLayout(0, 1, 0, 4));
        private final JButton records = button("Records", "grid", NAV, Color.WHITE);
        private final JButton reports = button("Reports", "chart", NAV, Color.WHITE);
        private final JButton refresh = button("Refresh", "refresh", Color.WHITE, TEXT);
        private final JButton add = button("Add equipment", "plus", ACCENT, Color.WHITE);
        private final JButton edit = button("Edit", "edit", Color.WHITE, TEXT);
        private final JButton delete = button("Delete", "trash", Color.WHITE, new Color(0xA52B3B));
        private final boolean setup;
        private boolean reportMode;
        private boolean connected;
        private boolean busy;
        private String password;
        private int index;
        private final List<JButton> links = new ArrayList<>();

        Dashboard(boolean setup) {
            this.setup = setup;
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setContentPane(layout());
            frame.setMinimumSize(new Dimension(820, 560));
            frame.setSize(1120, 720);
            frame.setLocationRelativeTo(null);
            records.addActionListener(e -> showSection(false));
            reports.addActionListener(e -> showSection(true));
            refresh.addActionListener(e -> load());
            add.addActionListener(e -> addEquipment());
            edit.addActionListener(e -> editEquipment());
            delete.addActionListener(e -> deleteEquipment());
            table.getSelectionModel().addListSelectionListener(e -> updateControls());
            showSection(false);
            frame.setVisible(true);
        }

        private JPanel layout() {
            JPanel root = new JPanel(new BorderLayout());
            JPanel sidebar = new JPanel(new BorderLayout(0, 12));
            sidebar.setBackground(NAV);
            sidebar.setPreferredSize(new Dimension(252, 0));
            sidebar.setBorder(new EmptyBorder(24, 16, 20, 16));
            JPanel brand = new JPanel();
            brand.setOpaque(false);
            brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
            brand.add(label("Gudo", 26, Color.WHITE, true));
            brand.add(label("Campus Equipment", 13, new Color(0xAFC0D2), false));
            ImageIcon artwork = new ImageIcon("assets/equipment.jpg");
            if (artwork.getIconWidth() > 0) {
                brand.add(Box.createVerticalStrut(15));
                brand.add(new JLabel(new ImageIcon(artwork.getImage()
                        .getScaledInstance(220, 104, Image.SCALE_SMOOTH))));
            }
            brand.add(Box.createVerticalStrut(18));
            JPanel tabs = new JPanel(new GridLayout(1, 2, 5, 0));
            tabs.setOpaque(false);
            tabs.setAlignmentX(Component.LEFT_ALIGNMENT);
            tabs.add(records);
            tabs.add(reports);
            brand.add(tabs);
            sidebar.add(brand, BorderLayout.NORTH);
            navigation.setOpaque(false);
            JPanel linkWrap = new JPanel(new BorderLayout());
            linkWrap.setOpaque(false);
            JScrollPane navScroll = new JScrollPane(linkWrap);
            navScroll.setBorder(null);
            navScroll.setOpaque(false);
            navScroll.getViewport().setOpaque(false);
            navScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            linkWrap.add(navigation, BorderLayout.NORTH);
            sidebar.add(navScroll, BorderLayout.CENTER);
            sidebar.add(label("CS 2137  /  Class 4-311", 12, new Color(0xAFC0D2), false), BorderLayout.SOUTH);
            root.add(sidebar, BorderLayout.WEST);

            JPanel main = new JPanel(new BorderLayout(0, 16));
            main.setBackground(BG);
            main.setBorder(new EmptyBorder(24, 26, 18, 26));
            JPanel heading = new JPanel(new BorderLayout(0, 10));
            heading.setOpaque(false);
            JPanel title = new JPanel(new BorderLayout(0, 4));
            title.setOpaque(false);
            title.add(page, BorderLayout.NORTH);
            title.add(pageHint, BorderLayout.SOUTH);
            heading.add(title, BorderLayout.NORTH);
            JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            tools.setOpaque(false);
            tools.add(add);
            tools.add(edit);
            tools.add(delete);
            tools.add(refresh);
            heading.add(tools, BorderLayout.SOUTH);
            main.add(heading, BorderLayout.NORTH);

            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setRowHeight(34);
            table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            table.setForeground(TEXT);
            table.setSelectionBackground(new Color(0xD7EFEA));
            table.setSelectionForeground(TEXT);
            table.setShowGrid(false);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.getTableHeader().setReorderingAllowed(false);
            table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            table.setFillsViewportHeight(true);
            table.setAutoCreateRowSorter(true);
            DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable source, Object value,
                        boolean selected, boolean focus, int row, int column) {
                    super.getTableCellRendererComponent(source, value, selected, focus, row, column);
                    setBorder(new EmptyBorder(0, 10, 0, 10));
                    if (!selected) setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF7FAFC));
                    return this;
                }
            };
            table.setDefaultRenderer(Object.class, renderer);
            table.setDefaultRenderer(Long.class, renderer);
            JScrollPane scroll = new JScrollPane(table);
            scroll.getViewport().setBackground(Color.WHITE);
            scroll.setBorder(BorderFactory.createLineBorder(LINE));
            main.add(scroll, BorderLayout.CENTER);
            main.add(status, BorderLayout.SOUTH);
            root.add(main, BorderLayout.CENTER);
            return root;
        }

        void connect() {
            String supplied = System.getenv("DB_PASSWORD");
            if (supplied == null) {
                JPasswordField field = new JPasswordField(20);
                String action = setup ? "Set up database and connect" : "Connect to MySQL";
                Object[] body = {action + " as " + USER + ".", field};
                if (JOptionPane.showConfirmDialog(frame, body, "MySQL connection",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                    frame.dispose();
                    return;
                }
                char[] secret = field.getPassword();
                supplied = new String(secret);
                Arrays.fill(secret, '\0');
                field.setText("");
            }
            password = supplied;
            run("Connecting to MySQL…", () -> {
                if (setup) setupDatabase(URL, USER, password);
                return readTable(RECORD_SOURCES[0]);
            }, model -> {
                connected = true;
                display(model);
            }, error -> {
                showError(error);
                if (System.getenv("DB_PASSWORD") == null
                        && JOptionPane.showConfirmDialog(frame, "Try connecting again?", "MySQL connection",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    SwingUtilities.invokeLater(this::connect);
                } else {
                    frame.dispose();
                    System.exit(1);
                }
            });
        }

        private void showSection(boolean showReports) {
            reportMode = showReports;
            index = 0;
            navigation.removeAll();
            links.clear();
            records.setBackground(reportMode ? NAV : ACCENT);
            reports.setBackground(reportMode ? ACCENT : NAV);
            String[] titles = reportMode ? REPORT_TITLES : RECORD_TITLES;
            for (int i = 0; i < titles.length; i++) {
                final int selected = i;
                JButton link = button(titles[i], null, NAV, Color.WHITE);
                link.setHorizontalAlignment(SwingConstants.LEFT);
                link.addActionListener(event -> {
                    index = selected;
                    updateHeading();
                    load();
                });
                links.add(link);
                navigation.add(link);
            }
            navigation.revalidate();
            navigation.repaint();
            updateHeading();
            load();
        }

        private void updateHeading() {
            page.setText((reportMode ? REPORT_TITLES : RECORD_TITLES)[index]);
            pageHint.setText(reportMode ? "Live report from MySQL"
                    : inventory() ? "Browse physical assets and manage inventory."
                    : "Campus equipment borrowing records");
            for (int i = 0; i < links.size(); i++) {
                links.get(i).setBackground(i == index ? new Color(0x29415B) : NAV);
            }
            updateControls();
        }

        private boolean inventory() { return !reportMode && index == 0; }

        private void updateControls() {
            boolean ready = connected && !busy;
            records.setEnabled(ready);
            reports.setEnabled(ready);
            for (JButton link : links) link.setEnabled(ready);
            refresh.setEnabled(ready);
            add.setVisible(inventory());
            edit.setVisible(inventory());
            delete.setVisible(inventory());
            add.setEnabled(ready);
            edit.setEnabled(ready && table.getSelectedRow() >= 0);
            delete.setEnabled(edit.isEnabled());
        }

        private void load() {
            if (!connected) return;
            String source = (reportMode ? REPORT_SOURCES : RECORD_SOURCES)[index];
            table.setModel(new DefaultTableModel());
            run("Loading records…", () -> readTable(source), this::display, this::showError);
        }

        private Connection open() throws SQLException {
            return DriverManager.getConnection(URL, USER, password);
        }

        private DefaultTableModel readTable(String source) throws SQLException {
            try (Connection connection = open();
                 Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT * FROM " + source)) {
                ResultSetMetaData metadata = rows.getMetaData();
                List<Class<?>> types = new ArrayList<>();
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    String type = metadata.getColumnClassName(i);
                    types.add(type.equals("java.lang.Long") || type.equals("java.lang.Integer")
                            ? Long.class : Object.class);
                }
                DefaultTableModel model = new DefaultTableModel() {
                    @Override public boolean isCellEditable(int row, int column) { return false; }
                    @Override public Class<?> getColumnClass(int column) {
                        return Long.class.equals(types.get(column)) ? Long.class : Object.class;
                    }
                };
                int columns = metadata.getColumnCount();
                for (int i = 1; i <= columns; i++) model.addColumn(metadata.getColumnLabel(i));
                while (rows.next()) {
                    Object[] values = new Object[columns];
                    for (int i = 1; i <= columns; i++) {
                        Object value = rows.getObject(i);
                        values[i - 1] = value instanceof Number number && types.get(i - 1) == Long.class
                                ? number.longValue() : value;
                    }
                    model.addRow(values);
                }
                return model;
            }
        }

        private void display(DefaultTableModel model) {
            table.setModel(model);
            int header = 0;
            while (header < model.getColumnCount()) {
                String name = model.getColumnName(header);
                var column = table.getColumnModel().getColumn(header++);
                column.setHeaderValue(name.replace('_', ' '));
                int width = Math.max(90, name.length() * 8 + 24);
                for (int row = 0; row < Math.min(model.getRowCount(), 150); row++) {
                    Object value = model.getValueAt(row, column.getModelIndex());
                    if (value != null) {
                        width = Math.max(width, table.getFontMetrics(table.getFont())
                                .stringWidth(value.toString()) + 26);
                    }
                }
                column.setPreferredWidth(Math.min(360, width));
            }
            status.setText(model.getRowCount() + (model.getRowCount() == 1 ? " row" : " rows") + "  •  MySQL connected");
        }

        private void addEquipment() {
            run("Loading categories…", () -> {
                List<Category> categories = new ArrayList<>();
                try (Connection connection = open();
                     Statement statement = connection.createStatement();
                     ResultSet rows = statement.executeQuery(
                             "SELECT category_id, category_name FROM CATEGORY ORDER BY category_name")) {
                    while (rows.next()) categories.add(new Category(rows.getLong(1), rows.getString(2)));
                }
                return categories;
            }, categories -> {
                status.setText(table.getRowCount() + " rows  •  MySQL connected");
                if (categories.isEmpty()) {
                    showError(new IllegalStateException("Create a category in MySQL before adding equipment."));
                    return;
                }
                JComboBox<Category> category = new JComboBox<>(categories.toArray(Category[]::new));
                JTextField tag = new JTextField(22);
                JTextField name = new JTextField(22);
                JTextField location = new JTextField(22);
                JPanel form = form("Category", category, "Asset tag", tag,
                        "Equipment name", name, "Location", location);
                if (!confirmForm(form, "Add equipment", new JTextField[]{tag, name, location},
                        new int[]{30, 120, 100})) return;
                Category selected = (Category) category.getSelectedItem();
                write("INSERT INTO EQUIPMENT_ITEM (category_id, asset_tag, item_name, location) VALUES (?, ?, ?, ?)",
                        selected.id(), tag.getText().strip(), name.getText().strip(), location.getText().strip());
            }, this::showError);
        }

        private void editEquipment() {
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            Object id = cell(row, "item_id");
            JTextField name = new JTextField(cell(row, "item_name").toString(), 22);
            JTextField location = new JTextField(cell(row, "location").toString(), 22);
            JPanel form = form("Equipment name", name, "Location", location);
            if (confirmForm(form, "Edit " + cell(row, "asset_tag"), new JTextField[]{name, location},
                    new int[]{120, 100})) {
                write("UPDATE EQUIPMENT_ITEM SET item_name = ?, location = ? WHERE item_id = ?",
                        name.getText().strip(), location.getText().strip(), id);
            }
        }

        private void deleteEquipment() {
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            if (JOptionPane.showConfirmDialog(frame, "Delete " + cell(row, "asset_tag") + " from inventory?",
                    "Delete equipment", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                write("DELETE FROM EQUIPMENT_ITEM WHERE item_id = ?", cell(row, "item_id"));
            }
        }

        private Object cell(int row, String name) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            return model.getValueAt(row, model.findColumn(name));
        }

        private void write(String sql, Object... values) {
            run("Saving changes…", () -> {
                try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(sql)) {
                    for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
                    return statement.executeUpdate();
                }
            }, count -> {
                if (count == 0) {
                    JOptionPane.showMessageDialog(frame, "No rows changed. Refreshing inventory.");
                }
                load();
            }, this::showError);
        }

        private <T> void run(String message, Callable<T> task, Consumer<T> success, Consumer<Throwable> failure) {
            busy = true;
            status.setText(message);
            updateControls();
            new SwingWorker<T, Void>() {
                @Override protected T doInBackground() throws Exception { return task.call(); }

                @Override protected void done() {
                    busy = false;
                    updateControls();
                    try {
                        success.accept(get());
                    } catch (ExecutionException e) {
                        failure.accept(e.getCause());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failure.accept(e);
                    }
                }
            }.execute();
        }

        private void showError(Throwable error) {
            status.setText("Operation failed");
            JOptionPane.showMessageDialog(frame, error.getMessage(), "Gudo", JOptionPane.ERROR_MESSAGE);
        }

        private boolean confirmForm(JPanel form, String title, JTextField[] fields, int[] limits) {
            while (JOptionPane.showConfirmDialog(frame, form, title, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                boolean valid = true;
                for (int i = 0; i < fields.length; i++) {
                    String value = fields[i].getText().strip();
                    if (value.isEmpty() || value.codePointCount(0, value.length()) > limits[i]) {
                        JOptionPane.showMessageDialog(frame,
                                "Enter " + fields[i].getName() + " (1–" + limits[i] + " characters).",
                                "Check equipment details", JOptionPane.WARNING_MESSAGE);
                        valid = false;
                        break;
                    }
                }
                if (valid) return true;
            }
            return false;
        }

        private static JPanel form(Object... pairs) {
            JPanel form = new JPanel(new GridLayout(0, 2, 12, 9));
            for (int i = 0; i < pairs.length; i += 2) {
                String name = (String) pairs[i];
                JComponent input = (JComponent) pairs[i + 1];
                JLabel label = new JLabel(name);
                label.setLabelFor(input);
                input.setName(name.toLowerCase(java.util.Locale.ROOT));
                form.add(label);
                form.add(input);
            }
            return form;
        }

        private static JLabel label(String text, int size, Color color, boolean bold) {
            JLabel label = new JLabel(text);
            label.setForeground(color);
            label.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size));
            return label;
        }

        private static JButton button(String title, String icon, Color background, Color foreground) {
            JButton button = new JButton(title) {
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D shape = (Graphics2D) graphics.create();
                    shape.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    shape.setColor(isEnabled() && getModel().isRollover() ? getBackground().darker() : getBackground());
                    shape.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    if (hasFocus()) {
                        shape.setColor(new Color(0x66AEA8));
                        shape.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                    }
                    shape.dispose();
                    super.paintComponent(graphics);
                }
            };
            button.setContentAreaFilled(false);
            button.setBorder(new EmptyBorder(8, 10, 8, 10));
            button.setBackground(background);
            button.setForeground(foreground);
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            if (icon != null) button.setIcon(new FeatherIcon(icon));
            return button;
        }
    }

    // Geometry adapted from the Feather icon set distributed by react-icons/fi.
    private record FeatherIcon(String name) implements Icon {
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }

        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D shape = (Graphics2D) graphics.create();
            shape.translate(x, y);
            shape.scale(16.0 / 24, 16.0 / 24);
            shape.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            shape.setColor(component.isEnabled() ? component.getForeground() : Color.GRAY);
            shape.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (name) {
                case "grid" -> {
                    shape.drawRect(3, 3, 7, 7);
                    shape.drawRect(14, 3, 7, 7);
                    shape.drawRect(3, 14, 7, 7);
                    shape.drawRect(14, 14, 7, 7);
                }
                case "chart" -> {
                    shape.drawLine(6, 20, 6, 14);
                    shape.drawLine(12, 20, 12, 4);
                    shape.drawLine(18, 20, 18, 9);
                }
                case "refresh" -> {
                    shape.drawArc(4, 4, 16, 16, 25, 280);
                    shape.drawPolyline(new int[]{21, 21, 16}, new int[]{4, 9, 9}, 3);
                }
                case "plus" -> {
                    shape.drawLine(12, 5, 12, 19);
                    shape.drawLine(5, 12, 19, 12);
                }
                case "edit" -> shape.drawPolyline(new int[]{4, 16, 20, 8, 4, 4},
                        new int[]{16, 4, 8, 20, 20, 16}, 6);
                case "trash" -> {
                    shape.drawLine(3, 6, 21, 6);
                    shape.drawPolyline(new int[]{5, 6, 18, 19}, new int[]{6, 21, 21, 6}, 4);
                    shape.drawPolyline(new int[]{9, 9, 15, 15}, new int[]{6, 3, 3, 6}, 4);
                    shape.drawLine(10, 10, 10, 17);
                    shape.drawLine(14, 10, 14, 17);
                }
            }
            shape.dispose();
        }
    }
}
