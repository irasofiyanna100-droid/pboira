package koperasi.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

final class Database {
    private static final Logger LOGGER = Logger.getLogger(Database.class.getName());
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/koperasi_db?serverTimezone=UTC";

    private Database() {
    }

    static Connection connect() throws SQLException {
        String url = setting("koperasi.db.url", "KOPERASI_DB_URL", DEFAULT_URL);
        String username = setting("koperasi.db.user", "KOPERASI_DB_USER", null);
        String password = setting("koperasi.db.password", "KOPERASI_DB_PASSWORD", null);
        if (username == null || username.trim().isEmpty()) {
            throw new SQLException("Atur KOPERASI_DB_USER sebelum menjalankan aplikasi.");
        }
        if (password == null) {
            password = "";
        }
        
        if (url.startsWith("jdbc:mysql:")) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException(
                        "MySQL Connector/J tidak tersedia. Jalankan aplikasi melalui Maven.", e);
            }
        }

        Connection connection = DriverManager.getConnection(url, username, password);
        try {
            initializeSchema(connection);
            return connection;
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException closeError) {
                e.addSuppressed(closeError);
            }
            throw e;
        }
    }

    private static void initializeSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS koperasi_accounts ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "full_name VARCHAR(200) NOT NULL,"
                    + "username VARCHAR(30) NOT NULL UNIQUE,"
                    + "role VARCHAR(20) NOT NULL,"
                    + "status VARCHAR(40) NOT NULL,"
                    + "enabled BOOLEAN NOT NULL,"
                    + "proof_path VARCHAR(1024) NOT NULL,"
                    + "phone VARCHAR(30) NOT NULL,"
                    + "nik VARCHAR(16) NOT NULL,"
                    + "address VARCHAR(1000) NOT NULL,"
                    + "password_salt VARCHAR(64) NOT NULL,"
                    + "password_hash VARCHAR(128) NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS koperasi_settings ("
                    + "singleton_id INTEGER NOT NULL PRIMARY KEY,"
                    + "cooperative_name VARCHAR(200) NOT NULL,"
                    + "annual_interest_percent DOUBLE NOT NULL,"
                    + "late_penalty_percent DOUBLE NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS koperasi_savings ("
                    + "id VARCHAR(36) NOT NULL PRIMARY KEY,"
                    + "username VARCHAR(30) NOT NULL,"
                    + "member_name VARCHAR(200) NOT NULL,"
                    + "type VARCHAR(50) NOT NULL,"
                    + "amount BIGINT NOT NULL,"
                    + "reference VARCHAR(255) NOT NULL,"
                    + "actor VARCHAR(100) NOT NULL,"
                    + "transaction_date TIMESTAMP NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS koperasi_loans ("
                    + "id VARCHAR(36) NOT NULL PRIMARY KEY,"
                    + "username VARCHAR(30) NOT NULL,"
                    + "member_name VARCHAR(200) NOT NULL,"
                    + "principal BIGINT NOT NULL,"
                    + "purpose VARCHAR(2000) NOT NULL,"
                    + "months INTEGER NOT NULL,"
                    + "requested_at TIMESTAMP NOT NULL,"
                    + "status VARCHAR(30) NOT NULL,"
                    + "disbursed BOOLEAN NOT NULL,"
                    + "total_repayable BIGINT NOT NULL,"
                    + "remaining BIGINT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS koperasi_installments ("
                    + "id VARCHAR(36) NOT NULL PRIMARY KEY,"
                    + "loan_id VARCHAR(36) NOT NULL,"
                    + "username VARCHAR(30) NOT NULL,"
                    + "member_name VARCHAR(200) NOT NULL,"
                    + "installment_number INTEGER NOT NULL,"
                    + "due_date DATE NOT NULL,"
                    + "amount BIGINT NOT NULL,"
                    + "paid BOOLEAN NOT NULL,"
                    + "paid_at TIMESTAMP NULL,"
                    + "FOREIGN KEY (loan_id) REFERENCES koperasi_loans(id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS koperasi_notices ("
                    + "id VARCHAR(36) NOT NULL PRIMARY KEY,"
                    + "username VARCHAR(30) NOT NULL,"
                    + "message VARCHAR(1000) NOT NULL,"
                    + "created_at TIMESTAMP NOT NULL)");
        }
    }

    static void logSqlFailure(String operation, SQLException error) {
        String category = diagnose(error);
        LOGGER.log(Level.SEVERE,
                "{0} SQL/JDBC failure: {1}; SQLState={2}; vendorCode={3}; exception={4}",
                new Object[] {operation, category, error.getSQLState(),
                        Integer.toString(error.getErrorCode()), error.getClass().getName()});
    }

    private static String diagnose(SQLException error) {
        int code = error.getErrorCode();
        if (code == 1045 || code == 1044) return "MySQL menolak autentikasi atau hak akses";
        if (code == 1049) return "database tidak ditemukan";
        if (code == 1146) return "tabel tidak ditemukan";
        if (code == 1054) return "kolom tidak ditemukan";
        if (code == 1064) return "sintaks SQL tidak valid";
        if (code == 1062) return "nilai duplikat melanggar indeks unik";
        if (code == 1451 || code == 1452) return "relasi foreign key tidak terpenuhi";

        String message = error.getMessage();
        if (message != null && message.contains("Atur KOPERASI_DB_USER")) {
            return "environment variable KOPERASI_DB_USER belum diatur";
        }
        if (message != null && message.contains("Atur KOPERASI_DB_PASSWORD")) {
            return "environment variable KOPERASI_DB_PASSWORD belum diatur";
        }
        if (message != null && message.contains("MySQL Connector/J tidak tersedia")) {
            return "MySQL Connector/J tidak tersedia di classpath";
        }
        if (error.getSQLState() != null && error.getSQLState().startsWith("08")) {
            return "koneksi ke server database gagal";
        }
        if (error.getSQLState() != null && error.getSQLState().startsWith("23")) {
            return "constraint database tidak terpenuhi";
        }
        return "kesalahan SQL/JDBC; periksa SQLState dan vendorCode";
    }

    private static String setting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null) value = System.getenv(environment);
        return value == null ? fallback : value;
    }
}
