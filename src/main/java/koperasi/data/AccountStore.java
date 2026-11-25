package koperasi.data;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class AccountStore {
    public static final String MEMBER = "Anggota";
    public static final String STAFF = "Petugas";
    public static final String ADMIN = "Admin";

    private static final int ITERATIONS = 120000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String ACCOUNT_COLUMNS =
            "full_name, username, role, status, enabled, proof_path, phone, nik, address, "
                    + "password_salt, password_hash";
    private final SecureRandom random = new SecureRandom();

    public synchronized Account authenticate(String username, char[] password, String role)
            throws IOException, GeneralSecurityException {
        String sql = "SELECT " + ACCOUNT_COLUMNS + " FROM koperasi_accounts "
                + "WHERE LOWER(username) = LOWER(?) AND role = ? AND enabled = TRUE "
                + "AND status = 'Aktif'";
        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());
            statement.setString(2, role);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                if (!matches(password, result.getString("password_salt"),
                        result.getString("password_hash"))) {
                    return null;
                }
                return readAccount(result);
            }
        } catch (SQLException e) {
            throw databaseError("Autentikasi akun gagal.", e);
        }
    }

    public synchronized void ensureDefaultAccounts() throws IOException, GeneralSecurityException {
        ensureDefaultAccount("Admin", "Admin", "Admin123".toCharArray(), ADMIN);
        ensureDefaultAccount("Petugas", "Petugas", "Petugas123".toCharArray(), STAFF);
    }

    public synchronized void registerMember(String fullName, String username, char[] password,
            String proofPath, String phone, String nik, String address)
            throws IOException, GeneralSecurityException {
        addAccount(fullName, username, password, MEMBER, "Menunggu Verifikasi", proofPath,
                phone, nik, address);
    }

    public synchronized void createStaff(String fullName, String username, char[] password)
            throws IOException, GeneralSecurityException {
        addAccount(fullName, username, password, STAFF, "Aktif", "", "", "", "");
    }

    public synchronized void createAdmin(String fullName, String username, char[] password)
            throws IOException, GeneralSecurityException {
        addAccount(fullName, username, password, ADMIN, "Aktif", "", "", "", "");
    }

    public synchronized List<Account> getAccounts() throws IOException {
        List<Account> accounts = new ArrayList<Account>();
        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT " + ACCOUNT_COLUMNS + " FROM koperasi_accounts ORDER BY id");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) accounts.add(readAccount(result));
            return accounts;
        } catch (SQLException e) {
            throw databaseError("Daftar akun tidak dapat dimuat.", e);
        }
    }

    public synchronized void setMemberVerification(String username, boolean approved)
            throws IOException {
        updateAccount(username, approved ? "Aktif" : "Ditolak", null);
    }

    public synchronized void setAccountEnabled(String username, boolean enabled)
            throws IOException {
        updateAccount(username, null, Boolean.valueOf(enabled));
    }

    public synchronized void renameAccount(String username, String fullName) throws IOException {
        String cleanName = fullName.trim();
        if (cleanName.isEmpty()) throw new IllegalArgumentException("Nama wajib diisi.");
        update("UPDATE koperasi_accounts SET full_name = ? WHERE LOWER(username) = LOWER(?)",
                cleanName, username.trim());
    }

    public synchronized void updateMemberProfile(
            String username, String fullName, String phone, String address) throws IOException {
        updateMemberProfile(username, fullName, phone, null, address);
    }

    public synchronized void updateMemberProfile(
            String username, String fullName, String phone, String nik, String address)
            throws IOException {
        String cleanName = fullName.trim();
        if (cleanName.isEmpty()) throw new IllegalArgumentException("Nama wajib diisi.");
        validatePhone(phone);
        if (nik != null && !nik.trim().isEmpty() && !nik.trim().matches("[0-9]{16}")) {
            throw new IllegalArgumentException("NIK harus terdiri dari 16 digit angka.");
        }
        String sql = nik == null
                ? "UPDATE koperasi_accounts SET full_name=?, phone=?, address=? "
                        + "WHERE LOWER(username)=LOWER(?) AND role=?"
                : "UPDATE koperasi_accounts SET full_name=?, phone=?, nik=?, address=? "
                        + "WHERE LOWER(username)=LOWER(?) AND role=?";
        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            statement.setString(index++, cleanName);
            statement.setString(index++, phone.trim());
            if (nik != null) statement.setString(index++, nik.trim());
            statement.setString(index++, address.trim());
            statement.setString(index++, username.trim());
            statement.setString(index, MEMBER);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Profil anggota tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw databaseError("Profil anggota tidak dapat diperbarui.", e);
        }
    }

    public synchronized void deleteAccount(String username) throws IOException {
        update("DELETE FROM koperasi_accounts WHERE LOWER(username) = LOWER(?)", username.trim());
    }

    private void updateAccount(String username, String status, Boolean enabled) throws IOException {
        if (status != null) {
            update("UPDATE koperasi_accounts SET status=? WHERE LOWER(username)=LOWER(?)",
                    status, username.trim());
        } else if (enabled != null) {
            try (Connection connection = Database.connect();
                    PreparedStatement statement = connection.prepareStatement(
                            "UPDATE koperasi_accounts SET enabled=? "
                                    + "WHERE LOWER(username)=LOWER(?)")) {
                statement.setBoolean(1, enabled.booleanValue());
                statement.setString(2, username.trim());
                requireUpdatedAccount(statement.executeUpdate());
            } catch (SQLException e) {
                throw databaseError("Status akun tidak dapat diperbarui.", e);
            }
        }
    }

    private void update(String sql, String... values) throws IOException {
        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setString(i + 1, values[i]);
            requireUpdatedAccount(statement.executeUpdate());
        } catch (SQLException e) {
            throw databaseError("Data akun tidak dapat diperbarui.", e);
        }
    }

    private void requireUpdatedAccount(int updated) {
        if (updated == 0) throw new IllegalArgumentException("Akun tidak ditemukan.");
    }

    private void ensureDefaultAccount(String name, String username, char[] password, String role)
            throws IOException, GeneralSecurityException {
        try {
            Account existing = findAccount(username);
            if (existing != null) {
                if (!role.equals(existing.role)) {
                    throw new IllegalStateException(
                            "Username " + username + " sudah dipakai akun dengan peran lain.");
                }
                if (matchesAccountPassword(username, password)) return;
                updatePassword(username, password);
                return;
            }
            addAccount(name, username, password, role, "Aktif", "", "", "", "");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private Account findAccount(String username) throws IOException {
        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT " + ACCOUNT_COLUMNS + " FROM koperasi_accounts "
                                + "WHERE LOWER(username)=LOWER(?)")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? readAccount(result) : null;
            }
        } catch (SQLException e) {
            throw databaseError("Akun tidak dapat dicari.", e);
        }
    }

    private boolean matchesAccountPassword(String username, char[] password)
            throws IOException, GeneralSecurityException {
        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT password_salt, password_hash FROM koperasi_accounts "
                                + "WHERE LOWER(username)=LOWER(?)")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && matches(password, result.getString(1), result.getString(2));
            }
        } catch (SQLException e) {
            throw databaseError("Kredensial akun tidak dapat dibaca.", e);
        }
    }

    private void updatePassword(String username, char[] password)
            throws IOException, GeneralSecurityException {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE koperasi_accounts SET password_salt=?, password_hash=? "
                                + "WHERE LOWER(username)=LOWER(?)")) {
            statement.setString(1, Base64.getEncoder().encodeToString(salt));
            statement.setString(2, hash(password, salt));
            statement.setString(3, username);
            requireUpdatedAccount(statement.executeUpdate());
        } catch (SQLException e) {
            throw databaseError("Password akun bawaan tidak dapat diperbarui.", e);
        }
    }

    private void addAccount(String fullName, String username, char[] password, String role,
            String status, String proofPath, String phone, String nik, String address)
            throws IOException, GeneralSecurityException {
        String cleanName = fullName.trim();
        String cleanUsername = username.trim();
        if (cleanName.isEmpty() || cleanUsername.isEmpty()) {
            throw new IllegalArgumentException("Nama dan username wajib diisi.");
        }
        if (password.length == 0) throw new IllegalArgumentException("Password wajib diisi.");
        if (password.length < 8) {
            throw new IllegalArgumentException("Password minimal terdiri dari 8 karakter.");
        }
        if (MEMBER.equals(role)) {
            validatePhone(phone);
            if (nik == null || !nik.trim().matches("[0-9]{16}")) {
                throw new IllegalArgumentException("NIK harus terdiri dari 16 digit angka.");
            }
        }
        if (findAccount(cleanUsername) != null) {
            throw new IllegalArgumentException("Username sudah terdaftar.");
        }

        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        String sql = "INSERT INTO koperasi_accounts (" + ACCOUNT_COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cleanName);
            statement.setString(2, cleanUsername);
            statement.setString(3, role);
            statement.setString(4, status);
            statement.setBoolean(5, true);
            statement.setString(6, proofPath == null ? "" : proofPath);
            statement.setString(7, phone == null ? "" : phone.trim());
            statement.setString(8, nik == null ? "" : nik.trim());
            statement.setString(9, address == null ? "" : address.trim());
            statement.setString(10, Base64.getEncoder().encodeToString(salt));
            statement.setString(11, hash(password, salt));
            statement.executeUpdate();
        } catch (SQLException e) {
            if (isDuplicate(e)) throw new IllegalArgumentException("Username sudah terdaftar.", e);
            throw databaseError("Akun tidak dapat disimpan.", e);
        }
    }

    private void validatePhone(String phone) {
        String value = phone == null ? "" : phone.trim();
        if (!value.matches("[0-9+() -]{9,18}") || value.replaceAll("\\D", "").length() < 9) {
            throw new IllegalArgumentException("Nomor telepon harus berisi minimal 9 digit.");
        }
    }

    private static Account readAccount(ResultSet result) throws SQLException {
        return new Account(result.getString("full_name"), result.getString("username"),
                result.getString("role"), result.getString("status"),
                result.getBoolean("enabled"), result.getString("proof_path"),
                result.getString("phone"), result.getString("nik"), result.getString("address"));
    }

    private static boolean matches(char[] password, String encodedSalt, String encodedHash)
            throws GeneralSecurityException {
        if (encodedSalt == null || encodedHash == null) return false;
        byte[] salt = Base64.getDecoder().decode(encodedSalt);
        byte[] expected = Base64.getDecoder().decode(encodedHash);
        byte[] actual = derive(password, salt);
        return MessageDigest.isEqual(expected, actual);
    }

    private static String hash(char[] password, byte[] salt) throws GeneralSecurityException {
        return Base64.getEncoder().encodeToString(derive(password, salt));
    }

    private static byte[] derive(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean isDuplicate(SQLException error) {
        return error.getSQLState() != null && error.getSQLState().startsWith("23");
    }

    private IOException databaseError(String message, SQLException cause) {
        Database.logSqlFailure(message, cause);
        return new IOException(message + " Periksa koneksi dan konfigurasi MySQL.", cause);
    }

    public static final class Account {
        public String fullName;
        public final String username;
        public final String role;
        public final String status;
        public final boolean enabled;
        public final String proofPath;
        public String phone;
        public String nik;
        public String address;

        private Account(String fullName, String username, String role, String status,
                boolean enabled, String proofPath, String phone, String nik, String address) {
            this.fullName = fullName;
            this.username = username;
            this.role = role;
            this.status = status;
            this.enabled = enabled;
            this.proofPath = proofPath;
            this.phone = phone;
            this.nik = nik;
            this.address = address;
        }
    }
}
