package koperasi.data;

import koperasi.config.IdentitasKoperasi;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;

public final class KoperasiStoreSmokeTest {
    @Test
    public void jdbcStorageSupportsApplicationFlows() throws Exception {
        main(new String[0]);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("koperasi.db.url",
                "jdbc:h2:mem:koperasi-smoke;MODE=MySQL;DB_CLOSE_DELAY=-1");
        System.setProperty("koperasi.db.user", "sa");
        System.setProperty("koperasi.db.password", "");
        verifyRegistrationAndAuthentication();
        verifyTransactionsPersistAndEnforceRules();
        verifyFailedSaveRollsBack();
        System.out.println("KoperasiStoreSmokeTest: PASS");
    }

    private static void verifyRegistrationAndAuthentication() throws Exception {
        AccountStore accounts = new AccountStore();
        accounts.ensureDefaultAccounts();
        AccountStore.Account admin = accounts.authenticate(
                "Admin", "Admin123".toCharArray(), AccountStore.ADMIN);
        require(admin != null, "Akun Admin bawaan harus dapat login dengan peran Admin.");
        AccountStore.Account staff = accounts.authenticate(
                "Petugas", "Petugas123".toCharArray(), AccountStore.STAFF);
        require(staff != null, "Akun Petugas bawaan harus dapat login dengan peran Petugas.");
        try (Connection connection = DriverManager.getConnection(
                System.getProperty("koperasi.db.url"),
                System.getProperty("koperasi.db.user"),
                System.getProperty("koperasi.db.password"));
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE koperasi_accounts "
                    + "SET password_salt='AAAAAAAAAAAAAAAAAAAAAA==', "
                    + "password_hash='AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=' "
                    + "WHERE LOWER(username)='petugas'");
        }
        accounts.ensureDefaultAccounts();
        require(accounts.authenticate("Petugas", "Petugas123".toCharArray(),
                        AccountStore.STAFF) != null,
                "Inisialisasi harus memulihkan hash akun Petugas tanpa membuat akun duplikat.");
        try (Connection connection = DriverManager.getConnection(
                System.getProperty("koperasi.db.url"),
                System.getProperty("koperasi.db.user"),
                System.getProperty("koperasi.db.password"));
                Statement statement = connection.createStatement();
                java.sql.ResultSet result = statement.executeQuery(
                        "SELECT COUNT(*) FROM koperasi_accounts "
                                + "WHERE LOWER(username) IN ('admin', 'petugas')")) {
            result.next();
            require(result.getInt(1) == 2,
                    "Inisialisasi berulang tidak boleh membuat akun bawaan duplikat.");
        }
        expectInvalid(() -> accounts.registerMember("No NIK", "no-nik",
                "Member123".toCharArray(), "proofs/payment.png", "08123456789", "", "Alamat uji"));
        char[] password = "Member123".toCharArray();
        accounts.registerMember("Test Member", "test-member", password,
                "proofs/payment.png", "08123456789", "3201010101010001", "Alamat uji");
        require(accounts.authenticate("test-member", "Member123".toCharArray(),
                AccountStore.MEMBER) == null, "Pendaftaran menunggu harus belum bisa login.");
        accounts.setMemberVerification("test-member", true);
        AccountStore.Account active = accounts.authenticate("test-member",
                "Member123".toCharArray(), AccountStore.MEMBER);
        require(active != null && "Aktif".equals(active.status),
                "Anggota terverifikasi seharusnya bisa login.");
        require("3201010101010001".equals(active.nik),
                "NIK pendaftar harus tersimpan pada data akun.");
        accounts.setAccountEnabled("test-member", false);
        require(accounts.authenticate("test-member", "Member123".toCharArray(),
                AccountStore.MEMBER) == null, "Akun nonaktif tidak boleh login.");
        accounts.setAccountEnabled("test-member", true);
        require(accounts.authenticate("test-member", "salah".toCharArray(),
                AccountStore.MEMBER) == null, "Password salah tidak boleh lolos autentikasi.");
        char[] spacedUsernamePassword = "Member123".toCharArray();
        accounts.registerMember("Spaced Username", "spaced member", spacedUsernamePassword,
                "proofs/payment.png", "08123456780", "3201010101010002", "Alamat uji");
        accounts.setMemberVerification("spaced member", true);
        AccountStore.Account spacedUsername = accounts.authenticate(
                "spaced member", "Member123".toCharArray(), AccountStore.MEMBER);
        require(spacedUsername != null && "spaced member".equals(spacedUsername.username),
                "Username yang mengandung spasi harus tetap tersimpan dan dapat digunakan.");
        java.util.Arrays.fill(spacedUsernamePassword, '\0');
        java.util.Arrays.fill(password, '\0');
    }

    private static void verifyTransactionsPersistAndEnforceRules() throws Exception {
        KoperasiStore store = new KoperasiStore();
        require(IdentitasKoperasi.NAMA.equals(store.getCooperativeName()),
                "Nama koperasi bawaan harus memakai identitas yang ditetapkan.");
        store.recordRegistrationSavings("member", "Test Member");
        store.recordRegistrationSavings("member", "Test Member");
        require(store.getSavingsBalance("member", KoperasiStore.POKOK) == 50000,
                "Simpanan pokok awal harus dicatat satu kali.");
        require(store.getSavingsBalance("member", KoperasiStore.WAJIB) == 20000,
                "Simpanan wajib bulan pertama tidak boleh dihitung dua kali.");
        expectInvalid(() -> store.addSavings("member", "Test Member", KoperasiStore.WAJIB,
                20000, "Pembayaran", "Petugas"));

        store.addSavings("member", "Test Member", KoperasiStore.SUKARELA,
                10000, "Pembayaran", "Petugas");
        store.withdrawVoluntary("member", "Test Member", 4000, "Petugas");
        require(store.getSavingsBalance("member", KoperasiStore.SUKARELA) == 6000,
                "Penarikan harus mengurangi saldo sukarela.");
        expectInvalid(() -> store.withdrawVoluntary("member", "Test Member", 6001, "Petugas"));

        KoperasiStore.Loan loan = store.requestLoan("member", "Test Member",
                120000, "Modal usaha", 3);
        store.decideLoan(loan.id, true);
        store.disburseLoan(loan.id, "Petugas");
        List<KoperasiStore.Installment> installments = store.getInstallments("member");
        require(installments.size() == 3, "Pencairan harus membuat jadwal angsuran.");
        require(installments.get(0).amount == 40000,
                "Bunga yang belum diatur tidak boleh dihitung.");
        store.payInstallment(installments.get(0).id, "Petugas");
        require(store.getLoans("member").get(0).remaining == 80000,
                "Pembayaran harus memperbarui sisa pinjaman.");

        KoperasiStore reopened = new KoperasiStore();
        require(reopened.getSavingsBalance("member", KoperasiStore.SUKARELA) == 6000,
                "Saldo harus tetap ada setelah data dimuat ulang.");
        require(reopened.getLoans("member").get(0).remaining == 80000,
                "Sisa pinjaman harus tetap ada setelah data dimuat ulang.");
        require(reopened.getInstallments("member").get(0).paid,
                "Status pembayaran angsuran harus tetap ada setelah data dimuat ulang.");

        reopened.updateSettings("Koperasi Uji", "12", "5");
        KoperasiStore.Loan interestLoan = reopened.requestLoan(
                "member", "Test Member", 120000, "Peralatan usaha", 12);
        reopened.decideLoan(interestLoan.id, true);
        reopened.disburseLoan(interestLoan.id, "Petugas");
        require(interestLoan.totalRepayable == 134400,
                "Bunga harus mengikuti pengaturan tahunan dan jangka waktu.");
        require(reopened.getLatePenaltyPercent() == 5,
                "Pengaturan denda harus tersimpan.");
        KoperasiStore.Installment lateInstallment = null;
        for (KoperasiStore.Installment item : reopened.getInstallments("member")) {
            if (interestLoan.id.equals(item.loanId) && item.number == 1) {
                lateInstallment = item;
                break;
            }
        }
        require(lateInstallment != null, "Jadwal pinjaman kedua harus tersedia.");
        reopened.payInstallmentAt(lateInstallment.id, "Petugas",
                lateInstallment.dueDate.plusDays(1).atStartOfDay());
        boolean penaltyRecorded = false;
        for (KoperasiStore.SavingsTransaction transaction
                : reopened.getSavingsTransactions("member")) {
            if ("Denda Angsuran".equals(transaction.type) && transaction.amount == 560) {
                penaltyRecorded = true;
            }
        }
        require(penaltyRecorded, "Denda yang dikonfigurasi harus diterapkan satu kali saat terlambat.");
    }

    private static void verifyFailedSaveRollsBack() throws Exception {
        KoperasiStore store = new KoperasiStore();
        try (Connection connection = DriverManager.getConnection(
                System.getProperty("koperasi.db.url"),
                System.getProperty("koperasi.db.user"),
                System.getProperty("koperasi.db.password"));
                Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE koperasi_savings ADD CONSTRAINT "
                    + "reject_test_amount CHECK (amount <> 5000)");
        }
        boolean failed = false;
        try {
            store.addSavings("member", "Test Member", KoperasiStore.SUKARELA,
                    5000, "Pembayaran", "Petugas");
        } catch (java.io.IOException expected) {
            failed = true;
        }
        require(failed, "Kegagalan tulis seharusnya dilaporkan.");
        require(store.getSavingsBalance("member", KoperasiStore.SUKARELA) == 6000,
                "Kegagalan database harus membatalkan transaksi dan mengembalikan state.");
        try (Connection connection = DriverManager.getConnection(
                System.getProperty("koperasi.db.url"),
                System.getProperty("koperasi.db.user"),
                System.getProperty("koperasi.db.password"));
                Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE koperasi_savings DROP CONSTRAINT reject_test_amount");
        }
    }

    private static void expectInvalid(CheckedAction action) throws Exception {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Operasi tidak valid seharusnya ditolak.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private interface CheckedAction {
        void run() throws Exception;
    }
}
