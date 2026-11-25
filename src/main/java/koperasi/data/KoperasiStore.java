package koperasi.data;

import koperasi.config.IdentitasKoperasi;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class KoperasiStore {
    public static final String POKOK = "Pokok";
    public static final String WAJIB = "Wajib";
    public static final String SUKARELA = "Sukarela";

    private State state;
    private byte[] committedSnapshot;

    public KoperasiStore() throws IOException {
        try (Connection connection = Database.connect()) {
            state = loadState(connection);
            if (!hasSettings(connection)) {
                save();
            } else {
                committedSnapshot = serializeState(state);
            }
        } catch (SQLException e) {
            throw databaseError("Data koperasi tidak dapat dimuat.", e);
        }
    }

    private State loadState(Connection connection) throws SQLException {
        State loaded = new State();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT cooperative_name, annual_interest_percent, late_penalty_percent "
                        + "FROM koperasi_settings WHERE singleton_id=1");
                ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                loaded.cooperativeName = result.getString(1);
                loaded.annualInterestPercent = result.getDouble(2);
                loaded.latePenaltyPercent = result.getDouble(3);
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, username, member_name, type, amount, reference, actor, transaction_date "
                        + "FROM koperasi_savings ORDER BY transaction_date, id");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                loaded.savings.add(new SavingsTransaction(result.getString("id"),
                        result.getString("username"), result.getString("member_name"),
                        result.getString("type"), result.getLong("amount"),
                        result.getString("reference"), result.getString("actor"),
                        result.getTimestamp("transaction_date").toLocalDateTime()));
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, username, member_name, principal, purpose, months, requested_at, "
                        + "status, disbursed, total_repayable, remaining "
                        + "FROM koperasi_loans ORDER BY requested_at, id");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                loaded.loans.add(new Loan(result.getString("id"), result.getString("username"),
                        result.getString("member_name"), result.getLong("principal"),
                        result.getString("purpose"), result.getInt("months"),
                        result.getTimestamp("requested_at").toLocalDateTime(),
                        result.getString("status"), result.getBoolean("disbursed"),
                        result.getLong("total_repayable"), result.getLong("remaining")));
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, loan_id, username, member_name, installment_number, due_date, "
                        + "amount, paid, paid_at FROM koperasi_installments "
                        + "ORDER BY due_date, id");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Timestamp paidAt = result.getTimestamp("paid_at");
                loaded.installments.add(new Installment(result.getString("id"),
                        result.getString("loan_id"), result.getString("username"),
                        result.getString("member_name"), result.getInt("installment_number"),
                        result.getDate("due_date").toLocalDate(), result.getLong("amount"),
                        result.getBoolean("paid"),
                        paidAt == null ? null : paidAt.toLocalDateTime()));
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, username, message, created_at FROM koperasi_notices "
                        + "ORDER BY created_at, id");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                loaded.notices.add(new Notice(result.getString("id"), result.getString("username"),
                        result.getString("message"),
                        result.getTimestamp("created_at").toLocalDateTime()));
            }
        }
        return loaded;
    }

    private boolean hasSettings(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM koperasi_settings WHERE singleton_id=1");
                ResultSet result = statement.executeQuery()) {
            return result.next();
        }
    }

    public synchronized void recordRegistrationSavings(String username, String memberName)
            throws IOException {
        for (SavingsTransaction transaction : state.savings) {
            if (username.equalsIgnoreCase(transaction.username)
                    && "Pendaftaran".equals(transaction.reference)) {
                return;
            }
        }
        state.savings.add(new SavingsTransaction(
                username, memberName, POKOK, 50000, "Pendaftaran", "Admin"));
        state.savings.add(new SavingsTransaction(
                username, memberName, WAJIB, 20000, "Pendaftaran", "Admin"));
        addNotice(username, "Pendaftaran anggota disetujui. Simpanan awal Rp70.000 telah dicatat.");
        save();
    }

    public synchronized SavingsTransaction addSavings(
            String username, String memberName, String type, long amount, String reference,
            String actor) throws IOException {
        requirePositive(amount);
        if (!POKOK.equals(type) && !WAJIB.equals(type) && !SUKARELA.equals(type)) {
            throw new IllegalArgumentException("Jenis simpanan tidak dikenal.");
        }
        if (POKOK.equals(type)) {
            throw new IllegalArgumentException("Simpanan pokok hanya dicatat saat pendaftaran.");
        }
        if (WAJIB.equals(type)) {
            if (amount != 20000) {
                throw new IllegalArgumentException("Simpanan wajib ditetapkan Rp20.000 per bulan.");
            }
            YearMonth currentMonth = YearMonth.now();
            for (SavingsTransaction existing : state.savings) {
                if (username.equalsIgnoreCase(existing.username) && WAJIB.equals(existing.type)
                        && YearMonth.from(existing.date).equals(currentMonth)) {
                    throw new IllegalArgumentException(
                            "Simpanan wajib bulan ini sudah tercatat, termasuk pembayaran awal pendaftaran.");
                }
            }
        }
        SavingsTransaction transaction = new SavingsTransaction(
                username, memberName, type, amount, reference, actor);
        state.savings.add(transaction);
        addNotice(username, "Simpanan " + type.toLowerCase() + " sebesar Rp"
                + amount + " telah dicatat.");
        save();
        return transaction;
    }

    public synchronized SavingsTransaction withdrawVoluntary(
            String username, String memberName, long amount, String actor) throws IOException {
        requirePositive(amount);
        long balance = getSavingsBalance(username, SUKARELA);
        if (amount > balance) {
            throw new IllegalArgumentException("Saldo simpanan sukarela tidak mencukupi.");
        }
        SavingsTransaction transaction = new SavingsTransaction(
                username, memberName, SUKARELA, -amount, "Penarikan", actor);
        state.savings.add(transaction);
        addNotice(username, "Penarikan simpanan sukarela Rp" + amount + " berhasil diproses.");
        save();
        return transaction;
    }

    public synchronized long getSavingsBalance(String username, String type) {
        long balance = 0;
        for (SavingsTransaction transaction : state.savings) {
            if (username.equalsIgnoreCase(transaction.username) && type.equals(transaction.type)) {
                balance += transaction.amount;
            }
        }
        return balance;
    }

    public synchronized List<SavingsTransaction> getSavingsTransactions(String username) {
        List<SavingsTransaction> result = new ArrayList<SavingsTransaction>();
        for (SavingsTransaction item : state.savings) {
            if (username == null || username.equalsIgnoreCase(item.username)) {
                result.add(item);
            }
        }
        Collections.reverse(result);
        return result;
    }

    public synchronized Loan requestLoan(
            String username, String memberName, long amount, String purpose, int months)
            throws IOException {
        requirePositive(amount);
        if (purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("Tujuan pinjaman wajib diisi.");
        }
        if (months < 1 || months > 120) {
            throw new IllegalArgumentException("Jangka waktu harus 1 sampai 120 bulan.");
        }
        Loan loan = new Loan(username, memberName, amount, purpose.trim(), months);
        state.loans.add(loan);
        addNotice(username, "Pengajuan pinjaman Rp" + amount + " menunggu keputusan Admin.");
        save();
        return loan;
    }

    public synchronized List<Loan> getLoans(String username) {
        List<Loan> result = new ArrayList<Loan>();
        for (Loan loan : state.loans) {
            if (username == null || username.equalsIgnoreCase(loan.username)) {
                result.add(loan);
            }
        }
        Collections.reverse(result);
        return result;
    }

    public synchronized void decideLoan(String loanId, boolean approved) throws IOException {
        Loan loan = findLoan(loanId);
        if (!"Menunggu".equals(loan.status)) {
            throw new IllegalArgumentException("Pengajuan ini sudah diproses.");
        }
        loan.status = approved ? "Disetujui" : "Ditolak";
        addNotice(loan.username, "Pengajuan pinjaman Rp" + loan.principal + " "
                + (approved ? "disetujui." : "ditolak."));
        save();
    }

    public synchronized void disburseLoan(String loanId, String actor) throws IOException {
        Loan loan = findLoan(loanId);
        if (!"Disetujui".equals(loan.status)) {
            throw new IllegalArgumentException("Hanya pinjaman yang disetujui Admin dapat dicairkan.");
        }
        if (loan.disbursed) {
            throw new IllegalArgumentException("Pinjaman ini sudah dicairkan.");
        }
        long total = loan.principal;
        if (state.annualInterestPercent >= 0) {
            total += Math.round(loan.principal * state.annualInterestPercent
                    * loan.months / 1200.0);
        }
        loan.totalRepayable = total;
        loan.remaining = total;
        loan.disbursed = true;
        loan.status = "Aktif";
        LocalDate firstDueDate = LocalDate.now().plusMonths(1);
        long regularAmount = total / loan.months;
        long remainder = total % loan.months;
        for (int i = 0; i < loan.months; i++) {
            long installmentAmount = regularAmount + (i == loan.months - 1 ? remainder : 0);
            state.installments.add(new Installment(
                    loan.id, loan.username, loan.memberName, i + 1,
                    YearMonth.from(firstDueDate.plusMonths(i)).atDay(
                            Math.min(firstDueDate.getDayOfMonth(),
                                    YearMonth.from(firstDueDate.plusMonths(i)).lengthOfMonth())),
                    installmentAmount
            ));
        }
        state.savings.add(new SavingsTransaction(
                loan.username, loan.memberName, "Pencairan Pinjaman", loan.principal,
                "Pinjaman " + loan.id, actor));
        addNotice(loan.username, "Pinjaman Rp" + loan.principal + " telah dicairkan. "
                + "Jadwal angsuran tersedia.");
        save();
    }

    public synchronized List<Installment> getInstallments(String username) {
        List<Installment> result = new ArrayList<Installment>();
        for (Installment item : state.installments) {
            if (username == null || username.equalsIgnoreCase(item.username)) {
                result.add(item);
            }
        }
        Collections.sort(result, (left, right) -> left.dueDate.compareTo(right.dueDate));
        return result;
    }

    public synchronized void payInstallment(String installmentId, String actor) throws IOException {
        payInstallmentAt(installmentId, actor, LocalDateTime.now());
    }

    synchronized void payInstallmentAt(
            String installmentId, String actor, LocalDateTime paymentTime) throws IOException {
        Installment installment = findInstallment(installmentId);
        if (installment.paid) {
            throw new IllegalArgumentException("Angsuran ini sudah dibayar.");
        }
        long penalty = 0;
        if (state.latePenaltyPercent >= 0
                && paymentTime.toLocalDate().isAfter(installment.dueDate)) {
            penalty = Math.round(installment.amount * state.latePenaltyPercent / 100.0);
        }
        installment.paid = true;
        installment.paidAt = paymentTime;
        Loan loan = findLoan(installment.loanId);
        loan.remaining = Math.max(0, loan.remaining - installment.amount);
        if (loan.remaining == 0) {
            loan.status = "Lunas";
        }
        state.savings.add(new SavingsTransaction(
                installment.username, installment.memberName, "Pembayaran Angsuran",
                -installment.amount, "Angsuran ke-" + installment.number, actor));
        if (penalty > 0) {
            state.savings.add(new SavingsTransaction(
                    installment.username, installment.memberName, "Denda Angsuran",
                    penalty, "Angsuran ke-" + installment.number, actor));
        }
        addNotice(installment.username, "Pembayaran angsuran ke-" + installment.number
                + " sebesar Rp" + installment.amount
                + (penalty > 0 ? " dengan denda Rp" + penalty : "")
                + " telah dicatat.");
        save();
    }

    public synchronized List<Notice> getNotifications(String username) {
        List<Notice> result = new ArrayList<Notice>();
        for (Notice notice : state.notices) {
            if (username.equalsIgnoreCase(notice.username)) {
                result.add(notice);
            }
        }
        Collections.reverse(result);
        return result;
    }

    public synchronized boolean hasUserData(String username) {
        for (SavingsTransaction transaction : state.savings) {
            if (username.equalsIgnoreCase(transaction.username)) return true;
        }
        for (Loan loan : state.loans) {
            if (username.equalsIgnoreCase(loan.username)) return true;
        }
        for (Installment installment : state.installments) {
            if (username.equalsIgnoreCase(installment.username)) return true;
        }
        for (Notice notice : state.notices) {
            if (username.equalsIgnoreCase(notice.username)) return true;
        }
        return false;
    }

    public synchronized String getCooperativeName() {
        return state.cooperativeName;
    }

    public synchronized double getAnnualInterestPercent() {
        return state.annualInterestPercent;
    }

    public synchronized double getLatePenaltyPercent() {
        return state.latePenaltyPercent;
    }

    public synchronized void updateSettings(String cooperativeName, String interest,
            String latePenalty)
            throws IOException {
        String cleanName = cooperativeName.trim();
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Nama koperasi wajib diisi.");
        }
        double rate = parseOptionalPercent(interest, "Bunga");
        double penalty = parseOptionalPercent(latePenalty, "Denda");
        state.cooperativeName = cleanName;
        state.annualInterestPercent = rate;
        state.latePenaltyPercent = penalty;
        save();
    }

    private double parseOptionalPercent(String input, String label) {
        if (input.trim().isEmpty()) return -1;
        double value;
        try {
            value = Double.parseDouble(input.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " harus berupa angka.");
        }
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0 || value > 100) {
            throw new IllegalArgumentException(label + " harus antara 0 sampai 100 persen.");
        }
        return value;
    }

    public synchronized long getTotalSavings() {
        long total = 0;
        for (SavingsTransaction item : state.savings) {
            if (POKOK.equals(item.type) || WAJIB.equals(item.type) || SUKARELA.equals(item.type)) {
                total += item.amount;
            }
        }
        return total;
    }

    public synchronized long getActiveLoans() {
        long total = 0;
        for (Loan loan : state.loans) {
            if (loan.disbursed && !"Lunas".equals(loan.status)) {
                total += loan.remaining;
            }
        }
        return total;
    }

    public synchronized List<String> reportRows() {
        List<String> rows = new ArrayList<String>();
        rows.add("Tanggal,Jenis,Anggota,Nominal,Referensi,Petugas");
        for (SavingsTransaction item : state.savings) {
            rows.add(item.date + "," + csv(item.type) + "," + csv(item.memberName) + ","
                    + item.amount + "," + csv(item.reference) + "," + csv(item.actor));
        }
        return rows;
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private Loan findLoan(String id) {
        for (Loan loan : state.loans) {
            if (loan.id.equals(id)) {
                return loan;
            }
        }
        throw new IllegalArgumentException("Data pinjaman tidak ditemukan.");
    }

    private Installment findInstallment(String id) {
        for (Installment item : state.installments) {
            if (item.id.equals(id)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Data angsuran tidak ditemukan.");
    }

    private void addNotice(String username, String message) {
        state.notices.add(new Notice(username, message));
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Nominal harus berupa angka positif.");
        }
    }

    private void save() throws IOException {
        byte[] snapshot = serializeState(state);
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                persistState(connection);
                connection.commit();
                committedSnapshot = snapshot;
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                restoreCommittedState(e);
                throw databaseError("Perubahan data koperasi gagal disimpan.", e);
            }
        } catch (SQLException e) {
            restoreCommittedState(e);
            throw databaseError("Koneksi database koperasi gagal.", e);
        }
    }

    private void persistState(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM koperasi_installments")) {
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM koperasi_savings")) {
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM koperasi_notices")) {
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM koperasi_loans")) {
            statement.executeUpdate();
        }

        int settingsUpdated;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE koperasi_settings SET cooperative_name=?, annual_interest_percent=?, "
                        + "late_penalty_percent=? WHERE singleton_id=1")) {
            statement.setString(1, state.cooperativeName);
            statement.setDouble(2, state.annualInterestPercent);
            statement.setDouble(3, state.latePenaltyPercent);
            settingsUpdated = statement.executeUpdate();
        }
        if (settingsUpdated == 0) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO koperasi_settings (singleton_id, cooperative_name, "
                            + "annual_interest_percent, late_penalty_percent) VALUES (1, ?, ?, ?)")) {
                statement.setString(1, state.cooperativeName);
                statement.setDouble(2, state.annualInterestPercent);
                statement.setDouble(3, state.latePenaltyPercent);
                statement.executeUpdate();
            }
        }

        String loanSql = "INSERT INTO koperasi_loans (id, username, member_name, principal, "
                + "purpose, months, requested_at, status, disbursed, total_repayable, remaining) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(loanSql)) {
            for (Loan loan : state.loans) {
                statement.setString(1, loan.id);
                statement.setString(2, loan.username);
                statement.setString(3, loan.memberName);
                statement.setLong(4, loan.principal);
                statement.setString(5, loan.purpose);
                statement.setInt(6, loan.months);
                statement.setTimestamp(7, Timestamp.valueOf(loan.requestedAt));
                statement.setString(8, loan.status);
                statement.setBoolean(9, loan.disbursed);
                statement.setLong(10, loan.totalRepayable);
                statement.setLong(11, loan.remaining);
                statement.addBatch();
            }
            statement.executeBatch();
        }

        String savingsSql = "INSERT INTO koperasi_savings (id, username, member_name, type, "
                + "amount, reference, actor, transaction_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(savingsSql)) {
            for (SavingsTransaction item : state.savings) {
                statement.setString(1, item.id);
                statement.setString(2, item.username);
                statement.setString(3, item.memberName);
                statement.setString(4, item.type);
                statement.setLong(5, item.amount);
                statement.setString(6, item.reference);
                statement.setString(7, item.actor);
                statement.setTimestamp(8, Timestamp.valueOf(item.date));
                statement.addBatch();
            }
            statement.executeBatch();
        }

        String installmentSql = "INSERT INTO koperasi_installments (id, loan_id, username, "
                + "member_name, installment_number, due_date, amount, paid, paid_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(installmentSql)) {
            for (Installment item : state.installments) {
                statement.setString(1, item.id);
                statement.setString(2, item.loanId);
                statement.setString(3, item.username);
                statement.setString(4, item.memberName);
                statement.setInt(5, item.number);
                statement.setDate(6, Date.valueOf(item.dueDate));
                statement.setLong(7, item.amount);
                statement.setBoolean(8, item.paid);
                if (item.paidAt == null) {
                    statement.setTimestamp(9, null);
                } else {
                    statement.setTimestamp(9, Timestamp.valueOf(item.paidAt));
                }
                statement.addBatch();
            }
            statement.executeBatch();
        }

        String noticeSql = "INSERT INTO koperasi_notices (id, username, message, created_at) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(noticeSql)) {
            for (Notice notice : state.notices) {
                statement.setString(1, notice.id);
                statement.setString(2, notice.username);
                statement.setString(3, notice.message);
                statement.setTimestamp(4, Timestamp.valueOf(notice.createdAt));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void restoreCommittedState(Exception failure) {
        if (committedSnapshot == null) return;
        try {
            state = deserializeState(committedSnapshot);
        } catch (IOException restoreError) {
            failure.addSuppressed(restoreError);
        }
    }

    private IOException databaseError(String message, SQLException cause) {
        Database.logSqlFailure(message, cause);
        return new IOException(message + " Periksa koneksi dan konfigurasi MySQL.", cause);
    }

    private static byte[] serializeState(State value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        return bytes.toByteArray();
    }

    private static State deserializeState(byte[] bytes) throws IOException {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object value = input.readObject();
            if (!(value instanceof State)) {
                throw new IOException("Format data koperasi tidak dikenali.");
            }
            return (State) value;
        } catch (ClassNotFoundException e) {
            throw new IOException("Versi data koperasi tidak kompatibel.", e);
        }
    }

    private static final class State implements Serializable {
        private static final long serialVersionUID = 1L;
        final List<SavingsTransaction> savings = new ArrayList<SavingsTransaction>();
        final List<Loan> loans = new ArrayList<Loan>();
        final List<Installment> installments = new ArrayList<Installment>();
        final List<Notice> notices = new ArrayList<Notice>();
        String cooperativeName = IdentitasKoperasi.NAMA;
        double annualInterestPercent = -1;
        double latePenaltyPercent = -1;
    }

    public static final class SavingsTransaction implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String id;
        public final String username;
        public final String memberName;
        public final String type;
        public final long amount;
        public final String reference;
        public final String actor;
        public final LocalDateTime date;

        private SavingsTransaction(String username, String memberName, String type, long amount,
                String reference, String actor) {
            this(UUID.randomUUID().toString(), username, memberName, type, amount,
                    reference, actor, LocalDateTime.now());
        }

        private SavingsTransaction(String id, String username, String memberName, String type,
                long amount, String reference, String actor, LocalDateTime date) {
            this.id = id;
            this.username = username;
            this.memberName = memberName;
            this.type = type;
            this.amount = amount;
            this.reference = reference;
            this.actor = actor;
            this.date = date;
        }
    }

    public static final class Loan implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String id;
        public final String username;
        public final String memberName;
        public final long principal;
        public final String purpose;
        public final int months;
        public final LocalDateTime requestedAt;
        public String status = "Menunggu";
        public boolean disbursed;
        public long totalRepayable;
        public long remaining;

        private Loan(String username, String memberName, long principal, String purpose, int months) {
            this(UUID.randomUUID().toString(), username, memberName, principal, purpose, months,
                    LocalDateTime.now(), "Menunggu", false, 0, 0);
        }

        private Loan(String id, String username, String memberName, long principal, String purpose,
                int months, LocalDateTime requestedAt, String status, boolean disbursed,
                long totalRepayable, long remaining) {
            this.id = id;
            this.username = username;
            this.memberName = memberName;
            this.principal = principal;
            this.purpose = purpose;
            this.months = months;
            this.requestedAt = requestedAt;
            this.status = status;
            this.disbursed = disbursed;
            this.totalRepayable = totalRepayable;
            this.remaining = remaining;
        }
    }

    public static final class Installment implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String id;
        public final String loanId;
        public final String username;
        public final String memberName;
        public final int number;
        public final LocalDate dueDate;
        public final long amount;
        public boolean paid;
        public LocalDateTime paidAt;

        private Installment(String loanId, String username, String memberName, int number,
                LocalDate dueDate, long amount) {
            this(UUID.randomUUID().toString(), loanId, username, memberName, number, dueDate,
                    amount, false, null);
        }

        private Installment(String id, String loanId, String username, String memberName,
                int number, LocalDate dueDate, long amount, boolean paid, LocalDateTime paidAt) {
            this.id = id;
            this.loanId = loanId;
            this.username = username;
            this.memberName = memberName;
            this.number = number;
            this.dueDate = dueDate;
            this.amount = amount;
            this.paid = paid;
            this.paidAt = paidAt;
        }
    }

    public static final class Notice implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String id;
        public final String username;
        public final String message;
        public final LocalDateTime createdAt;

        private Notice(String username, String message) {
            this(UUID.randomUUID().toString(), username, message, LocalDateTime.now());
        }

        private Notice(String id, String username, String message, LocalDateTime createdAt) {
            this.id = id;
            this.username = username;
            this.message = message;
            this.createdAt = createdAt;
        }
    }
}
