import javafx.application.Application;
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

    // =========================================================
    // DATABASE INITIALIZATION
    // =========================================================

    private void initializeDatabase() throws Exception {

        try (Statement st = connection.createStatement()) {

            st.execute("PRAGMA foreign_keys = ON");
        }

        InputStream in =
                getClass().getResourceAsStream("/database.sql");

        if (in == null) {

            throw new IllegalStateException(
                    "database.sql not found"
            );
        }

        StringBuilder sql = new StringBuilder();

        try (BufferedReader br =
                     new BufferedReader(
                             new InputStreamReader(
                                     in,
                                     StandardCharsets.UTF_8))) {

            String line;

            while ((line = br.readLine()) != null) {

                if (!line.trim().startsWith("--")) {

                    sql.append(line).append('\n');
                }
            }
        }

        for (String statement : sql.toString().split(";")) {

            String s = statement.trim();

            if (!s.isEmpty()) {

                try (Statement st =
                             connection.createStatement()) {

                    st.execute(s);
                }
            }
        }

        // Import HR CSV only when the employee table is empty
        importEmployeesFromCSV();
    }

    // =========================================================
    // IMPORT CSV
    // =========================================================

    private void importEmployeesFromCSV() throws Exception {

        String checkSql =
                "SELECT COUNT(*) FROM employees";

        try (Statement st =
                     connection.createStatement();
             ResultSet rs =
                     st.executeQuery(checkSql)) {

            if (rs.next() && rs.getInt(1) > 0) {

                return;
            }
        }

        InputStream in =
                getClass().getResourceAsStream(
                        "/HR_Analytics_with_Employee_Names.csv"
                );

        if (in == null) {

            throw new IllegalStateException(
                    "HR_Analytics_with_Employee_Names.csv not found"
            );
        }

        String sql = """
                INSERT INTO employees
                (
                    id,
                    name,
                    age,
                    department,
                    job_role,
                    monthly_income,
                    overtime,
                    job_satisfaction,
                    years_at_company
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (BufferedReader br =
                     new BufferedReader(
                             new InputStreamReader(
                                     in,
                                     StandardCharsets.UTF_8));

             PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            // Skip CSV header
            br.readLine();

            String line;

            while ((line = br.readLine()) != null) {

                String[] data = parseCSVLine(line);

                if (data.length < 35) {

                    continue;
                }

                /*
                 * CSV columns (HR_Analytics_with_Employee_Names):
                 *
                 * 0  Age
                 * 1  EmployeeName
                 * 2  Attrition
                 * 3  BusinessTravel
                 * 4  DailyRate
                 * 5  Department
                 * 6  DistanceFromHome
                 * 7  Education
                 * 8  EducationField
                 * 9  EmployeeCount
                 * 10 EmployeeNumber
                 * 11 EnvironmentSatisfaction
                 * 12 Gender
                 * 13 HourlyRate
                 * 14 JobInvolvement
                 * 15 JobLevel
                 * 16 JobRole
                 * 17 JobSatisfaction
                 * 18 MaritalStatus
                 * 19 MonthlyIncome
                 * 20 MonthlyRate
                 * 21 NumCompaniesWorked
                 * 22 Over18
                 * 23 OverTime
                 * 24 PercentSalaryHike
                 * 25 PerformanceRating
                 * 26 RelationshipSatisfaction
                 * 27 StandardHours
                 * 28 StockOptionLevel
                 * 29 TotalWorkingYears
                 * 30 TrainingTimesLastYear
                 * 31 WorkLifeBalance
                 * 32 YearsAtCompany
                 * 33 YearsInCurrentRole
                 * 34 YearsSinceLastPromotion
                 * 35 YearsWithCurrManager
                 */

                int age =
                        Integer.parseInt(data[0]);

                String employeeName =
                        data[1];

                int employeeNumber =
                        Integer.parseInt(data[10]);

                String department =
                        data[5];

                String jobRole =
                        data[16];

                double monthlyIncome =
                        Double.parseDouble(data[19]);

                String overtime =
                        data[23];

                int jobSatisfaction =
                        Integer.parseInt(data[17]);

                int yearsAtCompany =
                        Integer.parseInt(data[32]);

                ps.setInt(1, employeeNumber);
                ps.setString(2, employeeName);
                ps.setInt(3, age);
                ps.setString(4, department);
                ps.setString(5, jobRole);
                ps.setDouble(6, monthlyIncome);
                ps.setString(7, overtime);
                ps.setInt(8, jobSatisfaction);
                ps.setInt(9, yearsAtCompany);

                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    // =========================================================
    // SIMPLE CSV PARSER
    // =========================================================

    private String[] parseCSVLine(String line) {

        List<String> values = new ArrayList<>();

        boolean insideQuotes = false;

        StringBuilder value =
                new StringBuilder();

        for (int i = 0; i < line.length(); i++) {

            char c = line.charAt(i);

            if (c == '"') {

                insideQuotes = !insideQuotes;

            } else if (c == ',' && !insideQuotes) {

                values.add(value.toString().trim());

                value.setLength(0);

            } else {

                value.append(c);
            }
        }

        values.add(value.toString().trim());

        return values.toArray(new String[0]);
    }

    // =========================================================
    // LOGIN
    // =========================================================

    private void showLogin(Stage stage) {

        VBox card = new VBox(12);

        card.setAlignment(Pos.CENTER_LEFT);

        card.getStyleClass().add("login-card");

        card.setMaxWidth(380);

        Label title =
                new Label("Manager Portal");

        title.getStyleClass().add("title");

        Label subtitle =
                new Label(
                        "Sign in to view employee information"
                );

        subtitle.getStyleClass().add("subtitle");

        TextField username =
                new TextField();

        username.setPromptText("Username");

        username.getStyleClass().add("field");

        PasswordField password =
                new PasswordField();

        password.setPromptText("Password");

        password.getStyleClass().add("field");

        Label error =
                new Label();

        error.getStyleClass().add("error");

        Button login =
                new Button("Login");

        login.getStyleClass().add("primary-button");

        login.setMaxWidth(
                Double.MAX_VALUE
        );

        Label demo =
                new Label(
                        "Demo: manager / admin123\n" +
                        "Engineering lead: priya.lead / leadpass1"
                );

        demo.getStyleClass().add("info");

        login.setOnAction(e -> {

            try {

                String role =
                        authenticate(
                                username.getText().trim(),
                                password.getText()
                        );

                if (role == null) {

                    error.setText(
                            "Invalid username or password."
                    );

                } else {

                    currentUsername =
                            username.getText().trim();

                    currentRole = role;

                    showDashboard(stage);
                }

            } catch (SQLException ex) {

                error.setText(
                        "Login error: " +
                                ex.getMessage()
                );
            }
        });

        password.setOnAction(
                e -> login.fire()
        );

        card.getChildren().addAll(
                title,
                subtitle,
                new Separator(),
                username,
                password,
                login,
                error,
                demo
        );

        StackPane root =
                new StackPane(card);

        root.getStyleClass().add("login-page");

        root.setPadding(
                new Insets(25)
        );

        Scene scene =
                new Scene(
                        root,
                        850,
                        600
                );

        addCss(scene);

        stage.setScene(scene);

        stage.show();
    }

    private String authenticate(
            String username,
            String password
    ) throws SQLException {

        String sql =
                "SELECT role FROM managers " +
                "WHERE username = ? AND password = ?";

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setString(1, username);

            ps.setString(2, password);

            try (ResultSet rs =
                         ps.executeQuery()) {

                return rs.next()
                        ? rs.getString("role")
                        : null;
            }
        }
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    private void showDashboard(Stage stage) {

        BorderPane root =
                new BorderPane();

        // -------------------------
        // HEADER
        // -------------------------

        HBox header =
                new HBox(15);

        header.setAlignment(
                Pos.CENTER_LEFT
        );

        header.getStyleClass().add(
                "header"
        );

        Label title =
                new Label(
                        "Manager Dashboard"
                );

        title.getStyleClass().add(
                "title"
        );

        Region spacer =
                new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        Label loggedIn =
                new Label(
                        "Logged in: " +
                                currentUsername
                );

        Button logout =
                new Button("Logout");

        logout.getStyleClass().add(
                "secondary-button"
        );

        logout.setOnAction(
                e -> showLogin(stage)
        );

        header.getChildren().addAll(
                title,
                spacer,
                loggedIn,
                logout
        );

        root.setTop(header);

        // -------------------------
        // CONTENT
        // -------------------------

        VBox content =
                new VBox(16);

        content.setPadding(
                new Insets(20)
        );

        Label section =
                new Label("Employees");

        section.getStyleClass().add(
                "section-title"
        );

        HBox kpis =
                createKpis();

        searchField =
                new TextField();

        searchField.setPromptText(
                "Search by employee ID, department or job role..."
        );

        searchField.getStyleClass().add(
                "field"
        );

        searchField.textProperty().addListener(
                (obs, old, value) ->
                        loadEmployees(value)
        );

        employeeList =
                new VBox(10);

        ScrollPane scroll =
                new ScrollPane(employeeList);

        scroll.setFitToWidth(true);

        VBox.setVgrow(
                scroll,
                Priority.ALWAYS
        );

        content.getChildren().addAll(
                section,
                kpis,
                searchField,
                scroll
        );

        root.setCenter(content);

        loadEmployees("");

        Scene scene =
                new Scene(
                        root,
                        1050,
                        700
                );

        addCss(scene);

        stage.setScene(scene);

        stage.show();
    }

    // =========================================================
    // KPI CARDS
    // =========================================================

    private HBox createKpis() {

        HBox box =
                new HBox(12);

        int employees = 0;

        int overtimeEmployees = 0;

        double averageIncome = 0;

        int departments = 0;

        try {

            String employeeSql = """
                    SELECT
                        COUNT(*) AS total,
                        COALESCE(
                            SUM(
                                CASE
                                    WHEN overtime = 'Yes'
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS overtime_count,
                        COALESCE(
                            AVG(monthly_income),
                            0
                        ) AS average_income,
                        COUNT(
                            DISTINCT department
                        ) AS department_count
                    FROM employees
                    """;

            try (
                    Statement st =
                            connection.createStatement();

                    ResultSet rs =
                            st.executeQuery(employeeSql)
            ) {

                if (rs.next()) {

                    employees =
                            rs.getInt("total");

                    overtimeEmployees =
                            rs.getInt(
                                    "overtime_count"
                            );

                    averageIncome =
                            rs.getDouble(
                                    "average_income"
                            );

                    departments =
                            rs.getInt(
                                    "department_count"
                            );
                }
            }

        } catch (SQLException ignored) {
        }

        box.getChildren().addAll(

                kpi(
                        "Employees",
                        String.valueOf(
                                employees
                        )
                ),

                kpi(
                        "Departments",
                        String.valueOf(
                                departments
                        )
                ),

                kpi(
                        "Overtime",
                        String.valueOf(
                                overtimeEmployees
                        )
                ),

                kpi(
                        "Avg. Income",
                        "₹" +
                                money.format(
                                        averageIncome
                                )
                )
        );

        return box;
    }

    private VBox kpi(
            String label,
            String value
    ) {

        VBox card =
                new VBox(5);

        card.getStyleClass().add(
                "kpi-card"
        );

        HBox.setHgrow(
                card,
                Priority.ALWAYS
        );

        Label l =
                new Label(label);

        l.getStyleClass().add(
                "kpi-label"
        );

        Label v =
                new Label(value);

        v.getStyleClass().add(
                "kpi-value"
        );

        card.getChildren().addAll(
                l,
                v
        );

        return card;
    }

    // =========================================================
    // LOAD EMPLOYEES
    // =========================================================

    private void loadEmployees(
            String search
    ) {

        employeeList
                .getChildren()
                .clear();

        String sql = """
                SELECT *
                FROM employees
                WHERE (
                    CAST(id AS TEXT)
                        LIKE ?
                    OR lower(name)
                        LIKE lower(?)
                    OR lower(department)
                        LIKE lower(?)
                    OR lower(job_role)
                        LIKE lower(?)
                )
                """;

        if ("ENGINEERING_LEAD"
                .equals(currentRole)) {

            sql +=
                    " AND department = 'Engineering'";
        }

        sql +=
                " ORDER BY id";

        try (
                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            String pattern =
                    "%" + search + "%";

            ps.setString(1, pattern);

            ps.setString(2, pattern);

            ps.setString(3, pattern);

            ps.setString(4, pattern);

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                while (rs.next()) {

                    Employee emp =
                            new Employee(

                                    rs.getInt("id"),

                                    rs.getString("name"),

                                    rs.getInt("age"),

                                    rs.getString(
                                            "department"
                                    ),

                                    rs.getString(
                                            "job_role"
                                    ),

                                    rs.getDouble(
                                            "monthly_income"
                                    ),

                                    rs.getString(
                                            "overtime"
                                    ),

                                    rs.getInt(
                                            "job_satisfaction"
                                    ),

                                    rs.getInt(
                                            "years_at_company"
                                    )
                            );

                    employeeList
                            .getChildren()
                            .add(
                                    employeeCard(emp)
                            );
                }
            }

        } catch (SQLException e) {

            employeeList
                    .getChildren()
                    .add(
                            new Label(
                                    "Database error: " +
                                            e.getMessage()
                            )
                    );
        }
    }

    // =========================================================
    // EMPLOYEE CARD
    // =========================================================

    private VBox employeeCard(
            Employee emp
    ) {

        VBox card =
                new VBox(8);

        card.getStyleClass().add(
                "employee-card"
        );

        // -------------------------
        // TOP ROW
        // -------------------------

        HBox top =
                new HBox(10);

        top.setAlignment(
                Pos.CENTER_LEFT
        );

        Label id =
                new Label(
                        emp.name != null && !emp.name.isBlank()
                                ? emp.name
                                : "Employee #" + emp.id
                );

        id.setStyle(
                "-fx-font-size: 17px;" +
                " -fx-font-weight: bold;"
        );

        Label department =
                new Label(
                        emp.department
                );

        Region spacer =
                new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        top.getChildren().addAll(
                id,
                spacer,
                department
        );

        // -------------------------
        // DETAILS
        // -------------------------

        Label role =
                new Label(
                        "Job Role: " +
                                emp.jobRole
                );

        Label income =
                new Label(
                        "Monthly Income: ₹" +
                                money.format(
                                        emp.monthlyIncome
                                )
                );

        Label overtime =
                new Label(
                        "Overtime: " +
                                emp.overtime
                );

        Button view =
                new Button(
                        "View Details"
                );

        view.getStyleClass().add(
                "secondary-button"
        );

        view.setOnAction(
                e -> showEmployeeDetails(emp)
        );

        card.getChildren().addAll(
                top,
                role,
                income,
                overtime,
                view
        );

        return card;
    }

    // =========================================================
    // EMPLOYEE DETAILS
    // =========================================================

    private void showEmployeeDetails(
            Employee emp
    ) {

        Dialog<ButtonType> dialog =
                new Dialog<>();

        dialog.setTitle(
                "Employee Details"
        );

        dialog.setHeaderText(
                emp.name != null && !emp.name.isBlank()
                        ? emp.name
                        : "Employee #" + emp.id
        );

        VBox box =
                new VBox(10);

        box.setPadding(
                new Insets(10)
        );

        box.getStyleClass().add(
                "confidential"
        );

        box.getChildren().addAll(

                new Label(
                        "Age: " +
                                emp.age
                ),

                new Label(
                        "Department: " +
                                emp.department
                ),

                new Label(
                        "Job Role: " +
                                emp.jobRole
                ),

                new Label(
                        "Monthly Income: ₹" +
                                money.format(
                                        emp.monthlyIncome
                                )
                ),

                new Label(
                        "Overtime: " +
                                emp.overtime
                ),

                new Label(
                        "Job Satisfaction: " +
                                emp.jobSatisfaction +
                                " / 4"
                ),

                new Label(
                        "Years at Company: " +
                                emp.yearsAtCompany
                )
        );

        dialog.getDialogPane()
                .setContent(box);

        dialog.getDialogPane()
                .getButtonTypes()
                .add(
                        ButtonType.OK
                );

        dialog.showAndWait();
    }

    // =========================================================
    // CSS
    // =========================================================

    private void addCss(
            Scene scene
    ) {

        var css =
                getClass()
                        .getResource(
                                "/styles.css"
                        );

        if (css != null) {

            scene.getStylesheets()
                    .add(
                            css.toExternalForm()
                    );
        }
    }

    // =========================================================
    // ERROR
    // =========================================================

    private void showFatalError(
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR,
                        message,
                        ButtonType.OK
                );

        alert.setTitle(
                "Manager Portal"
        );

        alert.setHeaderText(
                "Application could not start"
        );

        alert.showAndWait();
    }

    // =========================================================
    // CLOSE DATABASE
    // =========================================================

    @Override
    public void stop() {

        try {

            if (connection != null) {

                connection.close();
            }

        } catch (SQLException ignored) {
        }
    }

    // =========================================================
    // EMPLOYEE CLASS
    // =========================================================

    private static class Employee {

        int id;
        String name;
        int age;

        String department;
        String jobRole;

        double monthlyIncome;

        String overtime;

        int jobSatisfaction;
        int yearsAtCompany;

        Employee(
                int id,
                String name,
                int age,
                String department,
                String jobRole,
                double monthlyIncome,
                String overtime,
                int jobSatisfaction,
                int yearsAtCompany
        ) {

            this.id = id;

            this.name = name;

            this.age = age;

            this.department =
                    department;

            this.jobRole =
                    jobRole;

            this.monthlyIncome =
                    monthlyIncome;

            this.overtime =
                    overtime;

            this.jobSatisfaction =
                    jobSatisfaction;

            this.yearsAtCompany =
                    yearsAtCompany;
        }
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(
            String[] args
    ) {

        launch(args);
    }
}
