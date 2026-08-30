import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ManagerPortal extends Application {

    private static final String DB_URL = "jdbc:sqlite:manager_portal.db";
    private final DecimalFormat money = new DecimalFormat("#,##0");

    private Connection connection;
    private String currentUsername;
    private String currentRole;
    private VBox employeeList;
    private TextField searchField;
    private Label selectedInfo;
    private Employee selectedEmployee;

    @Override
    public void start(Stage stage) {
        try {
            connection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        } catch (Exception e) {
            showFatalError("Database error: " + e.getMessage());
            return;
        }

        stage.setTitle("Manager Portal");
        showLogin(stage);
    }

    private void initializeDatabase() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }

        InputStream in = getClass().getResourceAsStream("/database.sql");
        if (in == null) {
            throw new IllegalStateException("database.sql not found");
        }

        StringBuilder sql = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().startsWith("--")) {
                    sql.append(line).append('\n');
                }
            }
        }

        // database.sql uses semicolon-separated statements.
        for (String statement : sql.toString().split(";")) {
            String s = statement.trim();
            if (!s.isEmpty()) {
                try (Statement st = connection.createStatement()) {
                    st.execute(s);
                }
            }
        }
    }

    private void showLogin(Stage stage) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(380);

        Label title = new Label("Manager Portal");
        title.getStyleClass().add("title");

        Label subtitle = new Label("Sign in to view employee information");
        subtitle.getStyleClass().add("subtitle");

        TextField username = new TextField();
        username.setPromptText("Username");
        username.getStyleClass().add("field");

        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.getStyleClass().add("field");

        Label error = new Label();
        error.getStyleClass().add("error");

        Button login = new Button("Login");
        login.getStyleClass().add("primary-button");
        login.setMaxWidth(Double.MAX_VALUE);

        Label demo = new Label(
                "Demo: manager / admin123\nEngineering lead: priya.lead / leadpass1");
        demo.getStyleClass().add("info");

        login.setOnAction(e -> {
            try {
                String role = authenticate(username.getText().trim(), password.getText());
                if (role == null) {
                    error.setText("Invalid username or password.");
                } else {
                    currentUsername = username.getText().trim();
                    currentRole = role;
                    showDashboard(stage);
                }
            } catch (SQLException ex) {
                error.setText("Login error: " + ex.getMessage());
            }
        });

        password.setOnAction(e -> login.fire());

        card.getChildren().addAll(title, subtitle, new Separator(),
                username, password, login, error, demo);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("login-page");
        root.setPadding(new Insets(25));

        Scene scene = new Scene(root, 850, 600);
        addCss(scene);
        stage.setScene(scene);
        stage.show();
    }

    private String authenticate(String username, String password) throws SQLException {
        String sql = "SELECT role FROM managers WHERE username = ? AND password = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("role") : null;
            }
        }
    }

    private void showDashboard(Stage stage) {
        BorderPane root = new BorderPane();

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        Label title = new Label("Manager Dashboard");
        title.getStyleClass().add("title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label loggedIn = new Label("Logged in: " + currentUsername);
        Button logout = new Button("Logout");
        logout.getStyleClass().add("secondary-button");
        logout.setOnAction(e -> showLogin(stage));

        header.getChildren().addAll(title, spacer, loggedIn, logout);
        root.setTop(header);

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        Label section = new Label("Employees");
        section.getStyleClass().add("section-title");

        HBox kpis = createKpis();

        searchField = new TextField();
        searchField.setPromptText("Search employees by name...");
        searchField.getStyleClass().add("field");
        searchField.textProperty().addListener((obs, old, val) -> loadEmployees(val));

        employeeList = new VBox(10);
        ScrollPane scroll = new ScrollPane(employeeList);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        selectedInfo = new Label("Select an employee and click View Info.");
        selectedInfo.setWrapText(true);

        content.getChildren().addAll(section, kpis, searchField, scroll);
        root.setCenter(content);

        loadEmployees("");

        Scene scene = new Scene(root, 1050, 700);
        addCss(scene);
        stage.setScene(scene);
        stage.show();
    }

    private HBox createKpis() {
        HBox box = new HBox(12);

        double hours = 0;
        int employees = 0;
        try {
            String sql = "SELECT COUNT(*), COALESCE(SUM(da.hours),0) " +
                    "FROM employees e LEFT JOIN daily_activity da " +
                    "ON e.id=da.employee_id AND da.work_date=date('now')";
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) {
                    employees = rs.getInt(1);
                    hours = rs.getDouble(2);
                }
            }
        } catch (SQLException ignored) {}

        box.getChildren().addAll(
                kpi("Employees", String.valueOf(employees)),
                kpi("Hours Today", String.format("%.1f", hours)),
                kpi("Role", currentRole.equals("MANAGER") ? "Manager" : "Eng. Lead")
        );
        return box;
    }

    private VBox kpi(String label, String value) {
        VBox card = new VBox(5);
        card.getStyleClass().add("kpi-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label l = new Label(label);
        l.getStyleClass().add("kpi-label");
        Label v = new Label(value);
        v.getStyleClass().add("kpi-value");

        card.getChildren().addAll(l, v);
        return card;
    }

    private void loadEmployees(String search) {
        employeeList.getChildren().clear();

        String sql = "SELECT e.*, COALESCE(da.hours,0) AS hours " +
                "FROM employees e LEFT JOIN daily_activity da " +
                "ON e.id=da.employee_id AND da.work_date=date('now') " +
                "WHERE lower(e.name) LIKE lower(?)";

        if ("ENGINEERING_LEAD".equals(currentRole)) {
            sql += " AND e.department = 'Engineering'";
        }

        sql += " ORDER BY e.name";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "%" + search + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Employee emp = new Employee(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("department"),
                            rs.getString("status"),
                            rs.getString("task"),
                            rs.getString("phone"),
                            rs.getDouble("salary"),
                            rs.getString("address"),
                            rs.getDouble("hours")
                    );
                    employeeList.getChildren().add(employeeCard(emp));
                }
            }
        } catch (SQLException e) {
            employeeList.getChildren().add(new Label("Database error: " + e.getMessage()));
        }
    }

    private VBox employeeCard(Employee emp) {
        VBox card = new VBox(8);
        card.getStyleClass().add("employee-card");

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(emp.name);
        name.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");

        Label status = new Label(emp.status);
        status.getStyleClass().add(statusClass(emp.status));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        top.getChildren().addAll(name, status, spacer,
                new Label(emp.department));

        Label task = new Label("Current task: " + emp.task);
        Label hours = new Label("Hours today: " + String.format("%.1f", emp.hours));

        Button view = new Button("View Info");
        view.getStyleClass().add("secondary-button");
        view.setOnAction(e -> showConfidential(emp));

        card.getChildren().addAll(top, task, hours, view);
        return card;
    }

    private String statusClass(String status) {
        return switch (status) {
            case "Actively Working" -> "status-working";
            case "In Meeting" -> "status-meeting";
            case "On Leave" -> "status-leave";
            default -> "status-idle";
        };
    }

    private void showConfidential(Employee emp) {
        selectedEmployee = emp;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Employee Information");
        dialog.setHeaderText("Confidential information — " + emp.name);

        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("confidential");

        box.getChildren().addAll(
                new Label("Phone: " + emp.phone),
                new Label("Salary: ₹" + money.format(emp.salary)),
                new Label("Address: " + emp.address)
        );

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    private void addCss(Scene scene) {
        var css = getClass().getResource("/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }

    private void showFatalError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Manager Portal");
        alert.setHeaderText("Application could not start");
        alert.showAndWait();
    }

    @Override
    public void stop() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ignored) {}
    }

    private static class Employee {
        int id;
        String name, department, status, task, phone, address;
        double salary, hours;

        Employee(int id, String name, String department, String status,
                 String task, String phone, double salary, String address, double hours) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.status = status;
            this.task = task;
            this.phone = phone;
            this.salary = salary;
            this.address = address;
            this.hours = hours;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
