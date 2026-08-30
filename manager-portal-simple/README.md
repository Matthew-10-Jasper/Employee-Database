# Simple Manager Portal

A beginner-friendly JavaFX + SQLite application.

## Files

- `src/main/java/ManagerPortal.java` - the complete JavaFX application and database code
- `src/main/resources/styles.css` - JavaFX styling
- `src/main/resources/database.sql` - SQLite tables and sample data
- `pom.xml` - Maven dependencies and JavaFX run configuration

## Requirements

- JDK 17 or newer
- Maven 3.9+ recommended
- Internet connection on the first Maven run

## Run

Open PowerShell in the project folder:

```powershell
mvn javafx:run
```

The application creates `manager_portal.db` automatically.

## Demo accounts

Manager:
- Username: `manager`
- Password: `admin123`

Engineering lead:
- Username: `priya.lead`
- Password: `leadpass1`

The engineering lead only sees Engineering employees.

## Notes

This is a simple educational/demo application. Passwords are stored as plain demo values in SQLite; a production application should use a secure password hashing algorithm and a server-side authentication layer.
