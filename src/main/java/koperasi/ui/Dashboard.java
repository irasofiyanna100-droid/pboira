package koperasi.ui;

import koperasi.config.IdentitasKoperasi;
import koperasi.data.AccountStore;
import koperasi.data.KoperasiStore;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public final class Dashboard extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Color NAVY = new Color(14, 37, 91);
    private static final Color BLUE = new Color(42, 105, 190);
    private static final Color BACKGROUND = new Color(244, 247, 252);
    private static final Color STAFF_BLUE = new Color(17, 79, 159);
    private static final Color STAFF_LIGHT_BLUE = new Color(233, 241, 255);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", new Locale("id", "ID"));

    private final AccountStore.Account currentAccount;
    private final AccountStore accountStore;
    private final KoperasiStore koperasiStore;
    private final Runnable onLogout;
    private final JPanel content = new JPanel(new BorderLayout(0, 16));
    private final List<JButton> staffNavItems = new ArrayList<JButton>();
    private JLabel identityLabel;
    private JLabel staffPageTitle;
    private JLabel staffPageDescription;
    private JLabel staffNotificationBadge;
    private JButton memberProfileButton;
    private JPanel staffSidebar;
    private String currentPage = "home";

    public Dashboard(AccountStore.Account account, AccountStore store, Runnable onLogout)
            throws IOException {
        currentAccount = account;
        accountStore = store;
        this.onLogout = onLogout;
        koperasiStore = new KoperasiStore();
        setTitle(koperasiStore.getCooperativeName() + " - " + account.role);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(isStaff() || isAdmin() || isMember() ? 1366 : 1180,
                isStaff() || isAdmin() || isMember() ? 768 : 760);
        setMinimumSize(new Dimension(isStaff() || isAdmin() ? 1180 : 980,
                isStaff() || isAdmin() || isMember() ? 700 : 650));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.add(createSidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BACKGROUND);
        main.add(createHeader(), BorderLayout.NORTH);
        content.setBackground(BACKGROUND);
        content.setBorder(isStaff() || isAdmin()
                ? BorderFactory.createEmptyBorder(18, 24, 18, 24)
                : isMember() ? BorderFactory.createEmptyBorder(10, 28, 12, 20)
                : BorderFactory.createEmptyBorder(22, 28, 26, 28));
        main.add(content, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);
        setContentPane(root);
        showPage("home");
    }

    private JPanel createSidebar() {
        if (isStaff()) {
            return createStaffSidebar();
        }
        if (isAdmin()) {
            return createAdminSidebar();
        }
        if (isMember()) {
            return createMemberSidebar();
        }
        JPanel sidebar = new JPanel();
        sidebar.setBackground(NAVY);
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 14, 18, 14));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(238, 0));

        JLabel brand = new JLabel("<html><b>" + escape(koperasiStore.getCooperativeName())
                + "</b><br><span style='font-size:11px'>SISTEM KOPERASI</span></html>");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        brand.setBorder(BorderFactory.createEmptyBorder(0, 10, 25, 4));
        sidebar.add(brand);
        sidebar.add(navButton("⌂  Dashboard", "home"));

        if (isAdmin()) {
            sidebar.add(navButton("♙  Data Anggota", "members"));
            sidebar.add(navButton("♧  Data Petugas", "staff"));
            sidebar.add(navButton("▤  Simpanan", "savings"));
            sidebar.add(navButton("↔  Pinjaman", "loans"));
            sidebar.add(navButton("▣  Angsuran", "installments"));
            sidebar.add(navButton("▥  Laporan", "reports"));
            sidebar.add(navButton("♙  Pengguna", "users"));
            sidebar.add(navButton("⚙  Pengaturan", "settings"));
        } else if (isStaff()) {
            sidebar.add(navButton("♙  Data Anggota", "members"));
            sidebar.add(navButton("▤  Simpanan", "savings"));
            sidebar.add(navButton("↘  Penarikan Sukarela", "withdraw"));
            sidebar.add(navButton("⇧  Pencairan Pinjaman", "disburse"));
            sidebar.add(navButton("▣  Angsuran", "installments"));
            sidebar.add(navButton("↻  Riwayat Transaksi", "history"));
            sidebar.add(navButton("▧  Cetak Bukti", "receipts"));
        } else {
            sidebar.add(navButton("♙  Profil Saya", "profile"));
            sidebar.add(navButton("▤  Simpanan Saya", "mySavings"));
            sidebar.add(navButton("＋  Pengajuan Pinjaman", "myLoans"));
            sidebar.add(navButton("▣  Informasi Angsuran", "myInstallments"));
            sidebar.add(navButton("♢  Notifikasi", "notifications"));
        }
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(navButton("⇥  Logout", "logout"));
        return sidebar;
    }

    private JPanel createHeader() {
        if (isStaff() || isAdmin()) {
            return createStaffHeader();
        }
        if (isMember()) {
            return createMemberHeader();
        }
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 242)),
                BorderFactory.createEmptyBorder(16, 28, 16, 26)));
        identityLabel = new JLabel(currentAccount.fullName + "  ·  " + currentAccount.role);
        identityLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        identityLabel.setForeground(NAVY);
        JLabel today = new JLabel(java.time.LocalDate.now().toString());
        today.setForeground(new Color(105, 118, 143));
        header.add(identityLabel, BorderLayout.WEST);
        header.add(today, BorderLayout.EAST);
        return header;
    }

    private JPanel createMemberSidebar() {
        JPanel sidebar = new AdminSidebarPanel();
        staffSidebar = sidebar;
        sidebar.setBorder(BorderFactory.createEmptyBorder(22, 11, 16, 11));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(228, 0));

        JLabel logo = new JLabel(new DashboardGlyph("brand", Color.WHITE, 62));
        logo.setAlignmentX(CENTER_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(10));
        JLabel brand = new JLabel("<html><div style='text-align:center'><b>KOPERASI<br>"
                + "SIMPAN PINJAM</b></div></html>");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 16));
        brand.setAlignmentX(CENTER_ALIGNMENT);
        brand.setMaximumSize(new Dimension(190, 48));
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(17));
        sidebar.add(sidebarDivider());
        sidebar.add(Box.createVerticalStrut(28));

        addStaffNav(sidebar, "Dashboard", "home", "home");
        addStaffNav(sidebar, "Profil Saya", "profile", "profile");
        addStaffNav(sidebar, "Simpanan", "mySavings", "piggy");
        addStaffNav(sidebar, "Pengajuan Pinjaman", "myLoans", "handshake");
        addStaffNav(sidebar, "Angsuran", "myInstallments", "document");
        addStaffNav(sidebar, "Notifikasi", "notifications", "bell");
        sidebar.add(Box.createVerticalStrut(17));
        sidebar.add(sidebarDivider());
        sidebar.add(Box.createVerticalStrut(14));
        addStaffNav(sidebar, "Keluar", "logout", "logout");
        sidebar.add(Box.createVerticalGlue());
        refreshStaffNavigation();
        return sidebar;
    }

    private JPanel sidebarDivider() {
        JPanel divider = new JPanel();
        divider.setOpaque(true);
        divider.setBackground(new Color(210, 230, 255, 115));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(190, 1));
        return divider;
    }

    private JPanel createMemberHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 54));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 233, 246)),
                BorderFactory.createEmptyBorder(5, 24, 5, 24)));

        JButton notifications = new JButton(new DashboardGlyph("bell", NAVY, 22));
        notifications.setToolTipText("Lihat notifikasi");
        notifications.setContentAreaFilled(false);
        notifications.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        notifications.setFocusPainted(false);
        notifications.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        notifications.addActionListener(event -> showPage("notifications"));
        staffNotificationBadge = new NotificationBadgeLabel();
        JPanel bell = new JPanel(new BorderLayout());
        bell.setOpaque(false);
        bell.add(notifications, BorderLayout.CENTER);
        bell.add(staffNotificationBadge, BorderLayout.NORTH);

        memberProfileButton = new JButton(currentAccount.fullName + "  ⌄",
                new DashboardGlyph("memberAvatar", new Color(31, 99, 185), 34));
        memberProfileButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        memberProfileButton.setForeground(NAVY);
        memberProfileButton.setIconTextGap(8);
        memberProfileButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        memberProfileButton.setContentAreaFilled(false);
        memberProfileButton.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 0));
        memberProfileButton.setFocusPainted(false);
        memberProfileButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        memberProfileButton.addActionListener(event -> showPage("profile"));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 1));
        right.setOpaque(false);
        right.add(bell);
        JPanel separator = new JPanel();
        separator.setBackground(new Color(225, 233, 246));
        separator.setPreferredSize(new Dimension(1, 31));
        right.add(separator);
        right.add(memberProfileButton);
        header.add(right, BorderLayout.EAST);
        updateMemberNotificationBadge();
        return header;
    }

    private void updateMemberNotificationBadge() {
        if (staffNotificationBadge == null) return;
        int count = koperasiStore.getNotifications(currentAccount.username).size();
        staffNotificationBadge.setText(count == 0 ? "" : Integer.toString(count));
        staffNotificationBadge.setVisible(count > 0);
    }

    private JPanel createStaffSidebar() {
        JPanel sidebar = new BlueSidebarPanel();
        staffSidebar = sidebar;
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 12, 14, 12));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));

        JLabel logo = new JLabel(new DashboardGlyph("brand", Color.WHITE, 58));
        logo.setAlignmentX(CENTER_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(8));

        JLabel brand = new JLabel("<html><div style='text-align:center'><b>"
                + escape(koperasiStore.getCooperativeName())
                + "</b><br><span style='font-size:11px'>" + escape(IdentitasKoperasi.SLOGAN)
                + "</span></div></html>");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 14));
        brand.setAlignmentX(CENTER_ALIGNMENT);
        brand.setMaximumSize(new Dimension(190, 54));
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(22));

        addStaffNav(sidebar, "Dashboard", "home", "home");
        addStaffNav(sidebar, "Data Anggota", "members", "members");
        addStaffNav(sidebar, "Simpanan", "savings", "savings");
        addStaffNav(sidebar, "<html>Penarikan Simpanan<br>Sukarela</html>", "withdraw", "withdraw");
        addStaffNav(sidebar, "Pencairan Pinjaman", "disburse", "disburse");
        addStaffNav(sidebar, "Angsuran", "installments", "installments");
        addStaffNav(sidebar, "Riwayat Transaksi", "history", "history");
        addStaffNav(sidebar, "Cetak Bukti Transaksi", "receipts", "receipt");

        sidebar.add(Box.createVerticalStrut(8));
        JPanel separator = new JPanel();
        separator.setBackground(new Color(255, 255, 255, 90));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setPreferredSize(new Dimension(180, 1));
        sidebar.add(separator);
        sidebar.add(Box.createVerticalStrut(8));
        addStaffNav(sidebar, "Logout", "logout", "logout");
        sidebar.add(Box.createVerticalGlue());

        JPanel profile = new JPanel(new BorderLayout(10, 0));
        profile.setOpaque(false);
        profile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(94, 153, 223)),
                BorderFactory.createEmptyBorder(9, 8, 9, 7)));
        profile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        JLabel profileIcon = new JLabel(new DashboardGlyph("profile", Color.WHITE, 42));
        profile.add(profileIcon, BorderLayout.WEST);
        JPanel profileText = new JPanel();
        profileText.setOpaque(false);
        profileText.setLayout(new BoxLayout(profileText, BoxLayout.Y_AXIS));
        JLabel fullName = new JLabel(currentAccount.fullName);
        fullName.setForeground(Color.WHITE);
        fullName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel username = new JLabel(currentAccount.username);
        username.setForeground(new Color(221, 234, 255));
        username.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JLabel online = new JLabel("●  Online");
        online.setForeground(new Color(139, 231, 173));
        online.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        profileText.add(fullName);
        profileText.add(username);
        profileText.add(online);
        profile.add(profileText, BorderLayout.CENTER);
        sidebar.add(profile);
        refreshStaffNavigation();
        return sidebar;
    }

    private JPanel createAdminSidebar() {
        JPanel sidebar = new AdminSidebarPanel();
        staffSidebar = sidebar;
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 10, 14, 10));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));

        JLabel logo = new JLabel(new DashboardGlyph("brand", Color.WHITE, 60));
        logo.setAlignmentX(CENTER_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(10));

        JLabel brand = new JLabel("<html><div style='text-align:center'><b>"
                + escape(koperasiStore.getCooperativeName()) + "</b></div></html>");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 14));
        brand.setAlignmentX(CENTER_ALIGNMENT);
        brand.setMaximumSize(new Dimension(194, 42));
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(20));

        JPanel profile = new JPanel(new BorderLayout(8, 0));
        profile.setOpaque(true);
        profile.setBackground(new Color(255, 255, 255, 28));
        profile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(103, 158, 231)),
                BorderFactory.createEmptyBorder(8, 8, 8, 7)));
        profile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        JLabel profileIcon = new JLabel(new DashboardGlyph("profile", Color.WHITE, 42));
        profile.add(profileIcon, BorderLayout.WEST);
        JPanel profileText = new JPanel();
        profileText.setOpaque(false);
        profileText.setLayout(new BoxLayout(profileText, BoxLayout.Y_AXIS));
        JLabel fullName = new JLabel(currentAccount.fullName);
        fullName.setForeground(Color.WHITE);
        fullName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel role = new JLabel("Administrator");
        role.setForeground(new Color(221, 234, 255));
        role.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        profileText.add(fullName);
        profileText.add(role);
        profile.add(profileText, BorderLayout.CENTER);
        sidebar.add(profile);
        sidebar.add(Box.createVerticalStrut(13));

        addStaffNav(sidebar, "Dashboard", "home", "home");
        addStaffNav(sidebar, "Data Anggota", "members", "members");
        addStaffNav(sidebar, "Data Petugas", "staff", "staff");
        addStaffNav(sidebar, "Simpanan", "savings", "savings");
        addStaffNav(sidebar, "Pinjaman", "loans", "loan");
        addStaffNav(sidebar, "Angsuran", "installments", "installments");
        addStaffNav(sidebar, "Laporan", "reports", "reports");
        addStaffNav(sidebar, "Pengguna", "users", "users");
        addStaffNav(sidebar, "Pengaturan", "settings", "settings");
        sidebar.add(Box.createVerticalGlue());
        JPanel separator = new JPanel();
        separator.setBackground(new Color(255, 255, 255, 85));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setPreferredSize(new Dimension(180, 1));
        sidebar.add(separator);
        sidebar.add(Box.createVerticalStrut(8));
        addStaffNav(sidebar, "Logout", "logout", "logout");
        refreshStaffNavigation();
        return sidebar;
    }

    private void addStaffNav(JPanel sidebar, String title, String page, String icon) {
        JButton button = new JButton(title, new DashboardGlyph(icon, Color.WHITE, 21));
        button.putClientProperty("page", page);
        button.setIconTextGap(13);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        button.setPreferredSize(new Dimension(192, 43));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setBorder(BorderFactory.createEmptyBorder(7, 11, 7, 8));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(event -> {
            if ("logout".equals(page)) {
                requestLogout();
            } else {
                showPage(page);
            }
        });
        staffNavItems.add(button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(3));
    }

    private void refreshStaffNavigation() {
        for (JButton button : staffNavItems) {
            String page = (String) button.getClientProperty("page");
            boolean active = page.equals(currentPage)
                    || ("mySavings".equals(page) && "savings".equals(currentPage))
                    || ("myLoans".equals(page) && "loans".equals(currentPage))
                    || ("myInstallments".equals(page) && "installments".equals(currentPage))
                    || ("history".equals(page) && "receipts".equals(currentPage))
                    || ("receipts".equals(page) && "history".equals(currentPage));
            button.setBackground(active
                    ? (isAdmin() ? new Color(48, 118, 220) : new Color(48, 126, 230))
                    : (isAdmin() ? new Color(8, 70, 153) : STAFF_BLUE));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, active ? 4 : 4, 0, 0,
                            active ? Color.WHITE : STAFF_BLUE),
                    BorderFactory.createEmptyBorder(7, 11, 7, 8)));
        }
    }

    private void updateStaffNotificationBadge() {
        if (staffNotificationBadge == null) return;
        try {
            int count = isAdmin() ? collectAdminAnnouncements().size() : collectStaffAlerts().size();
            staffNotificationBadge.setText(count == 0 ? "" : Integer.toString(count));
            staffNotificationBadge.setVisible(count > 0);
        } catch (IOException e) {
            showError(e);
        }
    }

    private void showStaffAlertsDialog() {
        if (isAdmin()) {
            showAdminAnnouncementsDialog();
            return;
        }
        try {
            List<StaffAlert> alerts = collectStaffAlerts();
            if (alerts.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tidak ada pengingat yang perlu ditindaklanjuti.",
                        "Pengingat & Notifikasi", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            StringBuilder message = new StringBuilder();
            for (StaffAlert alert : alerts) {
                if (message.length() > 0) message.append("\n\n");
                message.append("• ").append(alert.message).append(" — ").append(alert.time);
            }
            JOptionPane.showMessageDialog(this, message.toString(),
                    "Pengingat & Notifikasi", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            showError(e);
        }
    }

    private void showAdminAnnouncementsDialog() {
        try {
            List<String> announcements = collectAdminAnnouncements();
            JOptionPane.showMessageDialog(this,
                    announcements.isEmpty()
                            ? "Tidak ada aktivitas yang memerlukan tindak lanjut."
                            : String.join("\n\n", announcements),
                    "Pengumuman dan Tindak Lanjut",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            showError(e);
        }
    }

    private JPanel createStaffHeader() {
        JPanel header = isAdmin()
                ? new AdminHeaderPanel()
                : new JPanel(new BorderLayout(22, 0));
        if (!isAdmin()) {
            header.setBackground(Color.WHITE);
        }
        header.setPreferredSize(new Dimension(0, 100));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 231, 248)),
                BorderFactory.createEmptyBorder(15, 24, 15, 25)));

        JButton toggleSidebar = new JButton("☰");
        toggleSidebar.setFont(new Font("Segoe UI Symbol", Font.BOLD, 26));
        toggleSidebar.setForeground(STAFF_BLUE);
        toggleSidebar.setBackground(STAFF_LIGHT_BLUE);
        toggleSidebar.setFocusPainted(false);
        toggleSidebar.setBorder(BorderFactory.createEmptyBorder(2, 9, 4, 9));
        toggleSidebar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        toggleSidebar.addActionListener(event -> {
            staffSidebar.setVisible(!staffSidebar.isVisible());
            getContentPane().revalidate();
            getContentPane().repaint();
        });

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        staffPageTitle = new JLabel(isAdmin() ? "Dashboard Admin" : "Dashboard Petugas");
        staffPageTitle.setFont(new Font("Segoe UI", Font.BOLD, 25));
        staffPageTitle.setForeground(NAVY);
        staffPageDescription = new JLabel(isAdmin()
                ? "Selamat datang di sistem koperasi. Kelola data dengan mudah dan aman."
                : "Selamat datang di sistem koperasi. Kelola transaksi dengan mudah dan aman.");
        staffPageDescription.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        staffPageDescription.setForeground(new Color(56, 83, 128));
        titleBlock.add(staffPageTitle);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(staffPageDescription);

        JPanel left = new JPanel(new BorderLayout(18, 0));
        left.setOpaque(false);
        left.add(toggleSidebar, BorderLayout.WEST);
        left.add(titleBlock, BorderLayout.CENTER);
        header.add(left, BorderLayout.CENTER);

        JButton notifications = new JButton(new DashboardGlyph("bell", STAFF_BLUE, 27));
        notifications.setToolTipText("Lihat pengingat dan notifikasi");
        notifications.setContentAreaFilled(false);
        notifications.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
        notifications.setFocusPainted(false);
        notifications.addActionListener(event -> showStaffAlertsDialog());
        staffNotificationBadge = new NotificationBadgeLabel();
        JPanel bell = new JPanel(new BorderLayout());
        bell.setOpaque(false);
        bell.add(notifications, BorderLayout.CENTER);
        bell.add(staffNotificationBadge, BorderLayout.NORTH);

        JPanel dateBlock = new JPanel();
        dateBlock.setOpaque(false);
        dateBlock.setLayout(new BoxLayout(dateBlock, BoxLayout.Y_AXIS));
        java.time.format.DateTimeFormatter date =
                DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        JLabel today = new JLabel(java.time.LocalDate.now().format(date));
        today.setFont(new Font("Segoe UI", Font.BOLD, 12));
        today.setForeground(NAVY);
        JLabel time = new JLabel(java.time.LocalTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        time.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        time.setForeground(new Color(56, 83, 128));
        JPanel dateRow = new JPanel(new BorderLayout(8, 0));
        dateRow.setOpaque(false);
        dateRow.add(new JLabel(new DashboardGlyph("calendar", STAFF_BLUE, 17)), BorderLayout.WEST);
        dateRow.add(today, BorderLayout.CENTER);
        JPanel timeRow = new JPanel(new BorderLayout(8, 0));
        timeRow.setOpaque(false);
        timeRow.add(new JLabel(new DashboardGlyph("clock", STAFF_BLUE, 17)), BorderLayout.WEST);
        timeRow.add(time, BorderLayout.CENTER);
        dateBlock.add(dateRow);
        dateBlock.add(Box.createVerticalStrut(5));
        dateBlock.add(timeRow);

        JPanel right;
        if (isAdmin()) {
            right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 8));
            right.setOpaque(false);
            JPanel separator = new JPanel();
            separator.setBackground(new Color(62, 112, 190));
            separator.setPreferredSize(new Dimension(1, 42));
            right.add(bell);
            right.add(separator);
            right.add(dateBlock);
        } else {
            right = new JPanel(new BorderLayout(16, 0));
            right.setOpaque(false);
            right.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
            right.add(bell, BorderLayout.WEST);
            right.add(dateBlock, BorderLayout.CENTER);
        }
        header.add(right, BorderLayout.EAST);
        updateStaffNotificationBadge();
        return header;
    }

    private JPanel buildStaffHome() throws IOException {
        JPanel body = new JPanel(new BorderLayout(0, 13));
        body.setOpaque(false);

        List<AccountStore.Account> accounts = accountStore.getAccounts();
        int activeMembers = 0;
        for (AccountStore.Account account : accounts) {
            if (AccountStore.MEMBER.equals(account.role)
                    && account.enabled && "Aktif".equals(account.status)) {
                activeMembers++;
            }
        }

        long disbursedPrincipal = 0;
        for (KoperasiStore.Loan loan : koperasiStore.getLoans(null)) {
            if (loan.disbursed) disbursedPrincipal += loan.principal;
        }
        int todayTransactions = 0;
        List<KoperasiStore.SavingsTransaction> transactions =
                koperasiStore.getSavingsTransactions(null);
        for (KoperasiStore.SavingsTransaction transaction : transactions) {
            if (transaction.date.toLocalDate().equals(java.time.LocalDate.now())) {
                todayTransactions++;
            }
        }

        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        metrics.setPreferredSize(new Dimension(0, 112));
        metrics.add(staffMetricCard("Jumlah Anggota", Integer.toString(activeMembers), "members"));
        metrics.add(staffMetricCard("Total Simpanan", money(koperasiStore.getTotalSavings()), "savings"));
        metrics.add(staffMetricCard("Total Pinjaman", money(disbursedPrincipal), "money"));
        metrics.add(staffMetricCard("Transaksi Hari Ini", todayTransactions + " Transaksi", "history"));
        body.add(metrics, BorderLayout.NORTH);

        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);
        JPanel recent = buildStaffRecentTransactions(transactions);
        JPanel quick = buildStaffQuickMenu();
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = 0;
        left.weightx = 0.62;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        left.insets = new java.awt.Insets(0, 0, 0, 7);
        middle.add(recent, left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = 0;
        right.weightx = 0.38;
        right.weighty = 1;
        right.fill = GridBagConstraints.BOTH;
        right.insets = new java.awt.Insets(0, 7, 0, 0);
        middle.add(quick, right);
        body.add(middle, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setOpaque(false);
        bottom.setPreferredSize(new Dimension(0, 158));
        JPanel info = buildStaffCooperativeInfo();
        JPanel alerts = buildStaffNotificationPanel();
        GridBagConstraints infoConstraints = new GridBagConstraints();
        infoConstraints.gridx = 0;
        infoConstraints.gridy = 0;
        infoConstraints.weightx = 0.58;
        infoConstraints.weighty = 1;
        infoConstraints.fill = GridBagConstraints.BOTH;
        infoConstraints.insets = new java.awt.Insets(0, 0, 0, 7);
        bottom.add(info, infoConstraints);
        GridBagConstraints alertConstraints = new GridBagConstraints();
        alertConstraints.gridx = 1;
        alertConstraints.gridy = 0;
        alertConstraints.weightx = 0.42;
        alertConstraints.weighty = 1;
        alertConstraints.fill = GridBagConstraints.BOTH;
        alertConstraints.insets = new java.awt.Insets(0, 7, 0, 0);
        bottom.add(alerts, alertConstraints);
        body.add(bottom, BorderLayout.SOUTH);
        return body;
    }

    private JPanel buildAdminHome() throws IOException {
        JPanel body = new JPanel(new BorderLayout(0, 13));
        body.setOpaque(false);

        List<AccountStore.Account> accounts = accountStore.getAccounts();
        int activeMembers = 0;
        int activeStaff = 0;
        int pendingMembers = 0;
        for (AccountStore.Account account : accounts) {
            if (AccountStore.MEMBER.equals(account.role)) {
                if (account.enabled && "Aktif".equals(account.status)) activeMembers++;
                if ("Menunggu Verifikasi".equals(account.status)) pendingMembers++;
            } else if (AccountStore.STAFF.equals(account.role)
                    && account.enabled && "Aktif".equals(account.status)) {
                activeStaff++;
            }
        }

        List<KoperasiStore.SavingsTransaction> transactions =
                koperasiStore.getSavingsTransactions(null);
        long disbursedPrincipal = 0;
        for (KoperasiStore.Loan loan : koperasiStore.getLoans(null)) {
            if (loan.disbursed) disbursedPrincipal += loan.principal;
        }

        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        metrics.setPreferredSize(new Dimension(0, 112));
        metrics.add(adminMetricCard("Jumlah Anggota", Integer.toString(activeMembers),
                "Anggota aktif", "members"));
        metrics.add(adminMetricCard("Jumlah Petugas", Integer.toString(activeStaff),
                "Petugas aktif", "staff"));
        metrics.add(adminMetricCard("Total Simpanan", money(koperasiStore.getTotalSavings()),
                "Saldo seluruh anggota", "savingsTotal"));
        metrics.add(adminMetricCard("Total Pinjaman", money(disbursedPrincipal),
                "Pinjaman telah dicairkan", "loan"));
        body.add(metrics, BorderLayout.NORTH);

        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);
        GridBagConstraints recentConstraints = new GridBagConstraints();
        recentConstraints.gridx = 0;
        recentConstraints.gridy = 0;
        recentConstraints.weightx = 0.66;
        recentConstraints.weighty = 1;
        recentConstraints.fill = GridBagConstraints.BOTH;
        recentConstraints.insets = new java.awt.Insets(0, 0, 0, 7);
        middle.add(buildAdminRecentTransactions(transactions), recentConstraints);
        GridBagConstraints quickConstraints = new GridBagConstraints();
        quickConstraints.gridx = 1;
        quickConstraints.gridy = 0;
        quickConstraints.weightx = 0.34;
        quickConstraints.weighty = 1;
        quickConstraints.fill = GridBagConstraints.BOTH;
        quickConstraints.insets = new java.awt.Insets(0, 7, 0, 0);
        middle.add(buildAdminQuickMenu(), quickConstraints);
        body.add(middle, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setOpaque(false);
        bottom.setPreferredSize(new Dimension(0, 184));
        GridBagConstraints chartConstraints = new GridBagConstraints();
        chartConstraints.gridx = 0;
        chartConstraints.gridy = 0;
        chartConstraints.weightx = 0.56;
        chartConstraints.weighty = 1;
        chartConstraints.fill = GridBagConstraints.BOTH;
        chartConstraints.insets = new java.awt.Insets(0, 0, 0, 7);
        bottom.add(buildAdminSavingsLoanChart(transactions), chartConstraints);
        GridBagConstraints announcementsConstraints = new GridBagConstraints();
        announcementsConstraints.gridx = 1;
        announcementsConstraints.gridy = 0;
        announcementsConstraints.weightx = 0.44;
        announcementsConstraints.weighty = 1;
        announcementsConstraints.fill = GridBagConstraints.BOTH;
        announcementsConstraints.insets = new java.awt.Insets(0, 7, 0, 0);
        bottom.add(buildAdminAnnouncementsPanel(pendingMembers), announcementsConstraints);
        body.add(bottom, BorderLayout.SOUTH);
        return body;
    }

    private JPanel adminMetricCard(String title, String value, String detail, String iconName) {
        JPanel card = new JPanel(new BorderLayout(9, 0));
        card.setBackground(new Color(239, 245, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(201, 220, 251)),
                BorderFactory.createEmptyBorder(11, 12, 10, 10)));
        JLabel icon = new JLabel(new DashboardGlyph(iconName, new Color(20, 99, 207), 32));
        card.add(icon, BorderLayout.WEST);

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel caption = new JLabel(title);
        caption.setFont(new Font("Segoe UI", Font.BOLD, 12));
        caption.setForeground(new Color(37, 59, 100));
        JLabel amount = new JLabel(value);
        amount.setFont(new Font("Segoe UI", Font.BOLD, 18));
        amount.setForeground(NAVY);
        amount.setToolTipText(value);
        JLabel subtitle = new JLabel(detail);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        subtitle.setForeground(new Color(86, 107, 143));
        labels.add(caption);
        labels.add(Box.createVerticalStrut(5));
        labels.add(amount);
        labels.add(Box.createVerticalStrut(2));
        labels.add(subtitle);
        card.add(labels, BorderLayout.CENTER);
        JLabel watermark = new JLabel(new DashboardGlyph(iconName,
                new Color(77, 145, 231, 58), 48));
        card.add(watermark, BorderLayout.EAST);
        return card;
    }

    private JPanel buildAdminRecentTransactions(
            List<KoperasiStore.SavingsTransaction> transactions) {
        int rowCount = Math.min(5, transactions.size());
        Object[][] rows = new Object[Math.max(1, rowCount)][6];
        DateTimeFormatter shortDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (rowCount == 0) {
            rows[0] = new Object[] {"—", "—", "Belum ada transaksi", "—", "—", "—"};
        } else {
            for (int i = 0; i < rowCount; i++) {
                KoperasiStore.SavingsTransaction item = transactions.get(i);
                String type = item.type;
                if ("Penarikan".equals(item.reference)) type = "Penarikan Simpanan";
                rows[i] = new Object[] {i + 1, item.date.format(shortDate), type,
                        item.memberName, money(Math.abs(item.amount)), item.reference};
            }
        }
        JTable table = makeTable(
                new String[] {"No", "Tanggal", "Jenis Transaksi", "Nama Anggota", "Jumlah", "Keterangan"},
                rows);
        styleStaffTable(table);
        int[] widths = {36, 92, 152, 138, 108, 112};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JPanel section = staffSection("Transaksi Terbaru", "Lihat Semua  →",
                () -> showPage("history"));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildAdminQuickMenu() {
        JPanel section = staffSection("Menu Cepat", null, null);
        JPanel actions = new JPanel(new GridLayout(2, 3, 8, 8));
        actions.setOpaque(false);
        actions.add(staffQuickAction("Tambah\nAnggota", "addMember",
                () -> showAssistedRegistration()));
        actions.add(staffQuickAction("Kelola\nPetugas", "staff",
                () -> showPage("staff")));
        actions.add(staffQuickAction("Kelola\nSimpanan", "savings",
                () -> showPage("savings")));
        actions.add(staffQuickAction("Kelola\nPinjaman", "loan",
                () -> showPage("loans")));
        actions.add(staffQuickAction("Laporan", "reports",
                () -> showPage("reports")));
        actions.add(staffQuickAction("Pengaturan", "settings",
                () -> showPage("settings")));
        section.add(actions, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildAdminSavingsLoanChart(
            List<KoperasiStore.SavingsTransaction> transactions) {
        java.time.YearMonth current = java.time.YearMonth.now();
        String[] months = new String[4];
        long[] savings = new long[4];
        long[] loans = new long[4];
        java.time.YearMonth firstMonth = current.minusMonths(3);
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMM", new Locale("id", "ID"));
        for (int i = 0; i < months.length; i++) {
            months[i] = firstMonth.plusMonths(i).atDay(1).format(monthFormat);
        }
        for (KoperasiStore.SavingsTransaction item : transactions) {
            java.time.YearMonth month = java.time.YearMonth.from(item.date);
            long monthOffset = java.time.temporal.ChronoUnit.MONTHS.between(firstMonth, month);
            if (monthOffset < 0 || monthOffset >= months.length) continue;
            int index = (int) monthOffset;
            if ((KoperasiStore.POKOK.equals(item.type) || KoperasiStore.WAJIB.equals(item.type)
                    || KoperasiStore.SUKARELA.equals(item.type)) && item.amount > 0) {
                savings[index] += item.amount;
            } else if ("Pencairan Pinjaman".equals(item.type) && item.amount > 0) {
                loans[index] += item.amount;
            }
        }
        JPanel section = staffSection("Grafik Simpanan dan Pinjaman", null, null);
        JPanel heading = (JPanel) section.getComponent(0);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        legend.setOpaque(false);
        legend.add(chartLegend("Simpanan", new Color(39, 105, 201)));
        legend.add(chartLegend("Pinjaman", new Color(149, 194, 246)));
        heading.add(legend, BorderLayout.EAST);
        section.add(new AdminSavingsLoanChart(months, savings, loans), BorderLayout.CENTER);
        return section;
    }

    private JLabel chartLegend(String label, Color color) {
        JLabel legend = new JLabel(label);
        legend.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        legend.setForeground(new Color(67, 82, 112));
        legend.setIcon(new javax.swing.Icon() {
            @Override
            public int getIconWidth() {
                return 10;
            }

            @Override
            public int getIconHeight() {
                return 10;
            }

            @Override
            public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
                graphics.setColor(color);
                graphics.fillRoundRect(x, y + 1, 9, 8, 2, 2);
            }
        });
        legend.setIconTextGap(5);
        return legend;
    }

    private JPanel buildAdminAnnouncementsPanel(int pendingMembers) throws IOException {
        JPanel section = staffSection("Pengumuman", "Lihat Semua  →",
                () -> showAdminAnnouncementsDialog());
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        List<String> announcements = collectAdminAnnouncements(pendingMembers);
        if (announcements.isEmpty()) {
            list.add(adminAnnouncementRow("Tidak ada aktivitas yang memerlukan tindak lanjut.",
                    "Semua proses terpantau"));
        } else {
            for (String announcement : announcements) {
                list.add(adminAnnouncementRow(announcement, "Perlu ditinjau"));
                list.add(Box.createVerticalStrut(7));
            }
        }
        section.add(list, BorderLayout.CENTER);
        return section;
    }

    private JPanel adminAnnouncementRow(String message, String status) {
        JPanel row = new JPanel(new BorderLayout(9, 0));
        row.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dot.setForeground(new Color(27, 111, 226));
        row.add(dot, BorderLayout.WEST);
        JLabel label = new JLabel("<html>" + escape(message) + "</html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(new Color(43, 58, 88));
        row.add(label, BorderLayout.CENTER);
        JLabel detail = new JLabel(status);
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        detail.setForeground(new Color(97, 113, 143));
        row.add(detail, BorderLayout.EAST);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(233, 238, 247)));
        return row;
    }

    private List<String> collectAdminAnnouncements() throws IOException {
        int pendingMembers = 0;
        for (AccountStore.Account account : accountStore.getAccounts()) {
            if (AccountStore.MEMBER.equals(account.role)
                    && "Menunggu Verifikasi".equals(account.status)) pendingMembers++;
        }
        return collectAdminAnnouncements(pendingMembers);
    }

    private List<String> collectAdminAnnouncements(int pendingMembers) {
        List<String> announcements = new ArrayList<String>();
        if (pendingMembers > 0) {
            announcements.add("Pendaftaran anggota menunggu verifikasi: "
                    + pendingMembers + " anggota.");
        }
        int pendingLoans = countPendingLoans();
        if (pendingLoans > 0) {
            announcements.add("Pengajuan pinjaman menunggu keputusan: "
                    + pendingLoans + " pengajuan.");
        }
        int overdue = 0;
        java.time.LocalDate today = java.time.LocalDate.now();
        for (KoperasiStore.Installment installment : koperasiStore.getInstallments(null)) {
            if (!installment.paid && installment.dueDate.isBefore(today)) overdue++;
        }
        if (overdue > 0) {
            announcements.add("Angsuran telah melewati jatuh tempo: " + overdue + " angsuran.");
        }
        return announcements;
    }

    private JPanel buildStaffRecentTransactions(
            List<KoperasiStore.SavingsTransaction> transactions) {
        int rowCount = Math.min(5, transactions.size());
        Object[][] rows = new Object[Math.max(1, rowCount)][6];
        if (rowCount == 0) {
            rows[0] = new Object[] {"—", "—", "Belum ada transaksi", "—", "—", "—"};
        } else {
            DateTimeFormatter shortDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (int i = 0; i < rowCount; i++) {
                KoperasiStore.SavingsTransaction item = transactions.get(i);
                rows[i] = new Object[] {i + 1, item.date.format(shortDate), item.type,
                        item.memberName, money(Math.abs(item.amount)), "Berhasil"};
            }
        }
        JTable table = makeTable(
                new String[] {"No", "Tanggal", "Jenis Transaksi", "Nama Anggota", "Jumlah", "Status"},
                rows);
        styleStaffTable(table);
        JPanel section = staffSection("Transaksi Terbaru", "Lihat Semua  →",
                () -> showPage("history"));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildStaffQuickMenu() {
        JPanel section = staffSection("Menu Cepat", null, null);
        JPanel actions = new JPanel(new GridLayout(2, 3, 8, 8));
        actions.setOpaque(false);
        actions.add(staffQuickAction("Tambah\nAnggota", "addMember",
                () -> showAssistedRegistration()));
        actions.add(staffQuickAction("Catat\nSimpanan", "savings",
                () -> showPage("savings")));
        actions.add(staffQuickAction("Proses\nPenarikan", "withdraw",
                () -> showPage("withdraw")));
        actions.add(staffQuickAction("Pencairan\nPinjaman", "disburse",
                () -> showPage("disburse")));
        actions.add(staffQuickAction("Catat\nAngsuran", "installments",
                () -> showPage("installments")));
        actions.add(staffQuickAction("Lihat\nRiwayat", "history",
                () -> showPage("history")));
        section.add(actions, BorderLayout.CENTER);
        return section;
    }

    private JButton staffQuickAction(String label, String icon, Runnable action) {
        JButton button = new JButton("<html><div style='text-align:center'>"
                + label.replace("\n", "<br>") + "</div></html>",
                new DashboardGlyph(icon, BLUE, 30));
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setIconTextGap(5);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        button.setForeground(NAVY);
        button.setBackground(new Color(239, 245, 255));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 231, 252)),
                BorderFactory.createEmptyBorder(6, 3, 6, 3)));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.setToolTipText(label.replace("\n", " "));
        button.addActionListener(event -> action.run());
        return button;
    }

    private JPanel buildStaffCooperativeInfo() throws IOException {
        JPanel section = staffSection("Informasi Koperasi", "Lihat Detail  →",
                () -> new InformasiKoperasi(koperasiStore.getCooperativeName()).setVisible(true));
        JPanel details = new JPanel(new BorderLayout(14, 0));
        details.setOpaque(false);
        CooperativeInfoArtwork artwork = new CooperativeInfoArtwork();
        artwork.setPreferredSize(new Dimension(190, 106));
        details.add(artwork, BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(koperasiStore.getCooperativeName());
        name.setFont(new Font("Segoe UI", Font.BOLD, 13));
        name.setForeground(NAVY);
        JLabel slogan = new JLabel("<html><i>\"" + escape(IdentitasKoperasi.SLOGAN)
                + "\"</i></html>");
        slogan.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        slogan.setForeground(new Color(70, 87, 119));
        JLabel address = cooperativeInfoLine("location", IdentitasKoperasi.ALAMAT);
        JLabel phone = cooperativeInfoLine("phone", IdentitasKoperasi.TELEPON);
        JLabel email = cooperativeInfoLine("email", IdentitasKoperasi.EMAIL);
        text.add(name);
        text.add(Box.createVerticalStrut(1));
        text.add(slogan);
        text.add(Box.createVerticalStrut(4));
        text.add(address);
        text.add(phone);
        text.add(email);
        details.add(text, BorderLayout.CENTER);
        section.add(details, BorderLayout.CENTER);
        return section;
    }

    private JLabel cooperativeInfoLine(String icon, String value) {
        JLabel label = new JLabel(escape(value), new DashboardGlyph(icon, BLUE, 15),
                SwingConstants.LEFT);
        label.setIconTextGap(6);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(new Color(65, 83, 115));
        return label;
    }

    private JPanel buildStaffNotificationPanel() {
        JPanel section = staffSection("Pengingat & Notifikasi", "Lihat Semua  →",
                () -> showStaffAlertsDialog());
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        try {
            List<StaffAlert> alerts = collectStaffAlerts();
            if (alerts.isEmpty()) {
                JLabel empty = new JLabel("Belum ada aktivitas yang perlu ditindaklanjuti.");
                empty.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                empty.setForeground(new Color(100, 112, 134));
                list.add(empty);
            } else {
                for (StaffAlert alert : alerts.subList(0, Math.min(3, alerts.size()))) {
                    list.add(staffAlertRow(alert));
                    list.add(Box.createVerticalStrut(5));
                }
            }
        } catch (IOException e) {
            showError(e);
        }
        section.add(list, BorderLayout.CENTER);
        return section;
    }

    private JPanel staffAlertRow(StaffAlert alert) {
        JPanel row = new JPanel(new BorderLayout(9, 0));
        row.setOpaque(false);
        JLabel icon = new JLabel(new DashboardGlyph(alert.icon, BLUE, 21));
        row.add(icon, BorderLayout.WEST);
        JLabel message = new JLabel(alert.message);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        message.setForeground(NAVY);
        row.add(message, BorderLayout.CENTER);
        JLabel time = new JLabel(alert.time);
        time.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        time.setForeground(new Color(111, 122, 144));
        row.add(time, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        return row;
    }

    private List<StaffAlert> collectStaffAlerts() throws IOException {
        List<StaffAlert> alerts = new ArrayList<StaffAlert>();
        for (AccountStore.Account account : accountStore.getAccounts()) {
            if (AccountStore.MEMBER.equals(account.role)
                    && "Menunggu Verifikasi".equals(account.status)) {
                alerts.add(new StaffAlert("members",
                        "Pendaftaran anggota " + account.fullName + " menunggu verifikasi.",
                        "Perlu verifikasi", "members"));
            }
        }
        for (KoperasiStore.Loan loan : koperasiStore.getLoans(null)) {
            if (loan.disbursed || !"Disetujui".equals(loan.status)) continue;
            alerts.add(new StaffAlert("disburse",
                    "Pinjaman " + loan.memberName + " siap dicairkan.",
                    "Siap dicairkan", "disburse"));
        }
        for (KoperasiStore.SavingsTransaction item : koperasiStore.getSavingsTransactions(null)) {
            if (!"Pembayaran Angsuran".equals(item.type)) continue;
            alerts.add(new StaffAlert("installments",
                    "Pembayaran angsuran diterima dari " + item.memberName + ".",
                    item.date.format(DateTimeFormatter.ofPattern("HH:mm")), "installments"));
        }
        return alerts;
    }

    private JPanel staffSection(String title, String linkText, Runnable action) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 228, 250)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel label = new JLabel(title);
        if (isAdmin()) {
            String icon = null;
            if ("Transaksi Terbaru".equals(title)) icon = "reports";
            else if ("Grafik Simpanan dan Pinjaman".equals(title)) icon = "chartCircle";
            else if ("Pengumuman".equals(title)) icon = "announcement";
            if (icon != null) {
                label.setIcon(new DashboardGlyph(icon, new Color(24, 105, 212), 22));
                label.setIconTextGap(8);
            }
        }
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(NAVY);
        heading.add(label, BorderLayout.CENTER);
        if (linkText != null) {
            JButton link = new JButton(linkText);
            link.setFont(new Font("Segoe UI", Font.BOLD, 11));
            link.setForeground(new Color(19, 100, 223));
            link.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 3));
            link.setContentAreaFilled(false);
            link.setFocusPainted(false);
            link.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            link.addActionListener(event -> action.run());
            heading.add(link, BorderLayout.EAST);
        }
        section.add(heading, BorderLayout.NORTH);
        return section;
    }

    private JPanel staffMetricCard(String label, String value, String iconName) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(STAFF_LIGHT_BLUE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(204, 222, 251)),
                BorderFactory.createEmptyBorder(13, 15, 13, 15)));
        JLabel icon = new JLabel(new DashboardGlyph(iconName, new Color(24, 105, 212), 34));
        card.add(icon, BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel caption = new JLabel(label);
        caption.setFont(new Font("Segoe UI", Font.BOLD, 12));
        caption.setForeground(new Color(47, 69, 108));
        JLabel amount = new JLabel(value);
        amount.setFont(new Font("Segoe UI", Font.BOLD, 19));
        amount.setForeground(NAVY);
        text.add(caption);
        text.add(Box.createVerticalStrut(8));
        text.add(amount);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private void styleStaffTable(JTable table) {
        table.setRowHeight(31);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(new Color(34, 47, 72));
        table.setGridColor(new Color(226, 233, 245));
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(226, 239, 255));
        table.setSelectionForeground(NAVY);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setForeground(NAVY);
        table.getTableHeader().setBackground(new Color(239, 244, 252));
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
    }

    private JButton navButton(String title, final String page) {
        JButton button = new JButton(title);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setForeground(Color.WHITE);
        button.setBackground(NAVY);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 5));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(event -> {
            if ("logout".equals(page)) {
                requestLogout();
            } else {
                showPage(page);
            }
        });
        return button;
    }

    private void showPage(String page) {
        currentPage = page;
        content.removeAll();
        String title;
        String description;
        JPanel body;
        try {
            if ("home".equals(page)) {
                title = isStaff() ? "Dashboard Petugas" : "Dashboard";
                description = isStaff()
                        ? "Selamat datang di sistem koperasi. Kelola transaksi dengan mudah dan aman."
                        : "Ringkasan koperasi dan aktivitas terbaru.";
                body = buildHome();
            } else if ("members".equals(page)) {
                title = "Data Anggota";
                description = "Daftar anggota, status pendaftaran, dan verifikasi bukti pembayaran.";
                body = buildMembers();
            } else if ("staff".equals(page)) {
                title = "Data Petugas";
                description = "Kelola akun dan akses petugas koperasi.";
                body = buildStaff();
            } else if ("savings".equals(page) || "mySavings".equals(page)) {
                title = isMember() ? "Simpanan Saya" : "Simpanan";
                description = "Riwayat simpanan pokok, wajib, dan sukarela.";
                body = buildSavings(isMember());
            } else if ("withdraw".equals(page)) {
                title = "Penarikan Simpanan Sukarela";
                description = "Penarikan hanya dapat menggunakan saldo simpanan sukarela.";
                body = buildWithdrawals();
            } else if ("loans".equals(page) || "myLoans".equals(page)) {
                title = isMember() ? "Pengajuan Pinjaman" : "Pengajuan Pinjaman";
                description = "Status pengajuan, keputusan Admin, dan pencairan oleh Petugas.";
                body = buildLoans(isMember());
            } else if ("disburse".equals(page)) {
                title = "Pencairan Pinjaman";
                description = "Petugas hanya dapat mencairkan pinjaman yang telah disetujui Admin.";
                body = buildDisbursements();
            } else if ("installments".equals(page) || "myInstallments".equals(page)) {
                title = "Informasi Angsuran";
                description = "Jadwal jatuh tempo, pembayaran, dan sisa pinjaman.";
                body = buildInstallments(isMember());
            } else if ("history".equals(page) || "receipts".equals(page)) {
                title = "Riwayat Transaksi";
                description = "Pilih transaksi untuk meninjau atau mencetak bukti.";
                body = buildHistory(isMember());
            } else if ("reports".equals(page)) {
                title = "Laporan";
                description = "Ringkasan dan ekspor data transaksi koperasi.";
                body = buildReports();
            } else if ("users".equals(page)) {
                title = "Pengguna";
                description = "Kelola akun dan status akses sistem.";
                body = buildUsers();
            } else if ("settings".equals(page)) {
                title = "Pengaturan";
                description = "Identitas koperasi dan ketentuan pinjaman.";
                body = buildSettings();
            } else if ("profile".equals(page)) {
                title = "Profil Saya";
                description = "Lihat dan perbarui informasi profil anggota.";
                body = buildProfile();
            } else if ("notifications".equals(page)) {
                title = "Notifikasi";
                description = "Informasi status pendaftaran, pinjaman, dan pembayaran.";
                body = buildNotifications();
            } else {
                title = "Halaman tidak ditemukan";
                description = "";
                body = new JPanel();
            }
        } catch (IOException | RuntimeException e) {
            showError(e);
            title = "Dashboard";
            description = "";
            body = new JPanel();
        }
        if (isStaff() || isAdmin()) {
            staffPageTitle.setText(title);
            staffPageDescription.setText(description);
            updateStaffNotificationBadge();
            refreshStaffNavigation();
            content.add(body, BorderLayout.CENTER);
        } else if (isMember()) {
            refreshStaffNavigation();
            updateMemberNotificationBadge();
            if ("home".equals(page)) {
                content.add(body, BorderLayout.CENTER);
            } else {
                JPanel heading = new JPanel(new BorderLayout(0, 5));
                heading.setOpaque(false);
                JLabel titleLabel = new JLabel(title);
                titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 25));
                titleLabel.setForeground(NAVY);
                JLabel subtitle = new JLabel(description);
                subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                subtitle.setForeground(new Color(102, 113, 136));
                heading.add(titleLabel, BorderLayout.NORTH);
                heading.add(subtitle, BorderLayout.SOUTH);
                content.add(heading, BorderLayout.NORTH);
                content.add(body, BorderLayout.CENTER);
            }
        } else {
            JPanel heading = new JPanel(new BorderLayout(0, 5));
            heading.setOpaque(false);
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 25));
            titleLabel.setForeground(NAVY);
            JLabel subtitle = new JLabel(description);
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            subtitle.setForeground(new Color(102, 113, 136));
            heading.add(titleLabel, BorderLayout.NORTH);
            heading.add(subtitle, BorderLayout.SOUTH);
            content.add(heading, BorderLayout.NORTH);
            content.add(body, BorderLayout.CENTER);
        }
        content.revalidate();
        content.repaint();
    }

    private JPanel buildHome() throws IOException {
        if (isStaff()) {
            return buildStaffHome();
        }
        if (isAdmin()) {
            return buildAdminHome();
        }
        if (isMember()) {
            return buildMemberHome();
        }
        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        List<AccountStore.Account> accounts = accountStore.getAccounts();
        int members = 0;
        int staff = 0;
        int pending = 0;
        for (AccountStore.Account account : accounts) {
            if (AccountStore.MEMBER.equals(account.role)) {
                if ("Aktif".equals(account.status) && account.enabled) members++;
                if ("Menunggu Verifikasi".equals(account.status)) pending++;
            } else if (AccountStore.STAFF.equals(account.role) && account.enabled) {
                staff++;
            }
        }
        JPanel cards = new JPanel(new GridLayout(0, isMember() ? 2 : 3, 12, 12));
        cards.setOpaque(false);
        if (isMember()) {
            long pokok = koperasiStore.getSavingsBalance(currentAccount.username, KoperasiStore.POKOK);
            long wajib = koperasiStore.getSavingsBalance(currentAccount.username, KoperasiStore.WAJIB);
            long sukarela = koperasiStore.getSavingsBalance(currentAccount.username, KoperasiStore.SUKARELA);
            cards.add(metricCard("Saldo Simpanan", money(pokok + wajib + sukarela)));
            cards.add(metricCard("Simpanan Pokok", money(pokok)));
            cards.add(metricCard("Simpanan Wajib", money(wajib)));
            cards.add(metricCard("Simpanan Sukarela", money(sukarela)));
            cards.add(metricCard("Pinjaman Aktif", money(activeMemberLoanBalance())));
            cards.add(metricCard("Angsuran Berikutnya", nextInstallment()));
        } else {
            cards.add(metricCard("Anggota Aktif", Integer.toString(members)));
            cards.add(metricCard("Petugas Aktif", Integer.toString(staff)));
            cards.add(metricCard("Total Simpanan", money(koperasiStore.getTotalSavings())));
            cards.add(metricCard("Sisa Pinjaman Aktif", money(koperasiStore.getActiveLoans())));
            cards.add(metricCard("Menunggu Verifikasi", Integer.toString(pending)));
            cards.add(metricCard("Pinjaman Menunggu", Integer.toString(countPendingLoans())));
        }
        body.add(cards, BorderLayout.NORTH);
        List<KoperasiStore.SavingsTransaction> transactions =
                koperasiStore.getSavingsTransactions(isMember() ? currentAccount.username : null);
        Object[][] rows = new Object[Math.min(8, transactions.size())][4];
        for (int i = 0; i < rows.length; i++) {
            KoperasiStore.SavingsTransaction transaction = transactions.get(i);
            rows[i] = new Object[] {formatDate(transaction.date), transaction.memberName,
                    transaction.type, money(transaction.amount)};
        }
        JPanel recent = panelWithTable("Transaksi Terbaru",
                new String[] {"Tanggal", "Anggota", "Jenis", "Nominal"}, rows, null);
        body.add(recent, BorderLayout.CENTER);
        return body;
    }

    private JPanel buildMemberHome() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);

        JPanel title = new JPanel(new BorderLayout(10, 0));
        title.setOpaque(false);
        JPanel marker = new JPanel();
        marker.setBackground(new Color(20, 91, 206));
        marker.setPreferredSize(new Dimension(8, 34));
        title.add(marker, BorderLayout.WEST);
        JLabel heading = new JLabel("Dashboard Anggota");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 30));
        heading.setForeground(new Color(11, 31, 91));
        title.add(heading, BorderLayout.CENTER);
        title.setPreferredSize(new Dimension(0, 38));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(new MemberWelcomePanel(currentAccount), BorderLayout.CENTER);
        body.add(top, BorderLayout.NORTH);

        JPanel dashboard = new JPanel(new BorderLayout(0, 12));
        dashboard.setOpaque(false);
        JPanel balances = new JPanel(new GridLayout(1, 4, 10, 0));
        balances.setOpaque(false);
        long principal = koperasiStore.getSavingsBalance(
                currentAccount.username, KoperasiStore.POKOK);
        long required = koperasiStore.getSavingsBalance(
                currentAccount.username, KoperasiStore.WAJIB);
        long voluntary = koperasiStore.getSavingsBalance(
                currentAccount.username, KoperasiStore.SUKARELA);
        balances.add(memberMetricCard("Total Simpanan", money(principal + required + voluntary),
                "wallet", false));
        balances.add(memberMetricCard("Simpanan Pokok", money(principal), "savingsTotal", false));
        balances.add(memberMetricCard("Simpanan Wajib", money(required), "calendar", false));
        balances.add(memberMetricCard("Simpanan Sukarela", money(voluntary), "star", true));
        balances.setPreferredSize(new Dimension(0, 98));
        dashboard.add(balances, BorderLayout.NORTH);

        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);
        GridBagConstraints loan = new GridBagConstraints();
        loan.gridx = 0;
        loan.gridy = 0;
        loan.weightx = 0.35;
        loan.weighty = 1;
        loan.fill = GridBagConstraints.BOTH;
        loan.insets = new java.awt.Insets(0, 0, 0, 6);
        middle.add(buildMemberLoanSummary(), loan);
        GridBagConstraints savings = new GridBagConstraints();
        savings.gridx = 1;
        savings.gridy = 0;
        savings.weightx = 0.65;
        savings.weighty = 1;
        savings.fill = GridBagConstraints.BOTH;
        savings.insets = new java.awt.Insets(0, 6, 0, 0);
        middle.add(buildMemberRecentSavings(), savings);
        dashboard.add(middle, BorderLayout.CENTER);

        JPanel notificationPanel = buildMemberNotificationPanel();
        notificationPanel.setPreferredSize(new Dimension(0, 104));
        dashboard.add(notificationPanel, BorderLayout.SOUTH);
        body.add(dashboard, BorderLayout.CENTER);
        return body;
    }

    private JPanel memberMetricCard(String title, String value, String icon, boolean warm) {
        JPanel card = new JPanel(new BorderLayout(11, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 230, 247)),
                BorderFactory.createEmptyBorder(10, 12, 10, 10)));
        JPanel iconTile = new JPanel(new GridBagLayout());
        iconTile.setBackground(warm ? new Color(255, 242, 216) : new Color(228, 239, 255));
        iconTile.setPreferredSize(new Dimension(62, 62));
        iconTile.add(new JLabel(new DashboardGlyph(icon,
                warm ? new Color(247, 164, 0) : new Color(18, 99, 215), 34)));
        card.add(iconTile, BorderLayout.WEST);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel caption = new JLabel(title);
        caption.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        caption.setForeground(new Color(24, 39, 82));
        JLabel amount = new JLabel(value);
        amount.setFont(new Font("Segoe UI", Font.BOLD, 21));
        amount.setForeground(new Color(9, 29, 91));
        amount.setToolTipText(value);
        labels.add(caption);
        labels.add(Box.createVerticalStrut(3));
        labels.add(amount);
        card.add(labels, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildMemberLoanSummary() {
        JPanel section = memberSection("Ringkasan Pinjaman", "document");
        List<KoperasiStore.Loan> loans = koperasiStore.getLoans(currentAccount.username);
        KoperasiStore.Loan active = null;
        for (KoperasiStore.Loan loan : loans) {
            if (loan.disbursed && !"Lunas".equals(loan.status)) {
                active = loan;
                break;
            }
        }

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel illustration = new JLabel(new DashboardGlyph(
                active == null ? "emptyLoan" : "loan", new Color(80, 153, 235), 68));
        illustration.setAlignmentX(CENTER_ALIGNMENT);
        content.add(Box.createVerticalGlue());
        content.add(illustration);
        content.add(Box.createVerticalStrut(8));
        JLabel summary = new JLabel(active == null ? "Belum ada pinjaman aktif"
                : "Sisa pinjaman " + money(active.remaining));
        summary.setFont(new Font("Segoe UI", Font.BOLD, 14));
        summary.setForeground(new Color(18, 38, 91));
        summary.setAlignmentX(CENTER_ALIGNMENT);
        content.add(summary);
        if (active != null) {
            JLabel detail = new JLabel(active.months + " bulan · " + active.status);
            detail.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            detail.setForeground(new Color(93, 109, 139));
            detail.setAlignmentX(CENTER_ALIGNMENT);
            content.add(Box.createVerticalStrut(5));
            content.add(detail);
        }
        content.add(Box.createVerticalStrut(10));
        JButton apply = actionButton(active == null ? "＋  Ajukan Pinjaman" : "Lihat Pinjaman");
        apply.setAlignmentX(CENTER_ALIGNMENT);
        apply.setFont(new Font("Segoe UI", Font.BOLD, 13));
        apply.setPreferredSize(new Dimension(232, 40));
        apply.setMaximumSize(new Dimension(232, 40));
        apply.setBackground(new Color(12, 81, 200));
        apply.addActionListener(event -> showPage("myLoans"));
        content.add(apply);
        content.add(Box.createVerticalGlue());
        section.add(content, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildMemberRecentSavings() {
        JPanel section = memberSection("Riwayat Simpanan Terbaru", "history");
        JButton all = memberLink("Lihat Semua  ›", () -> showPage("mySavings"));
        ((JPanel) section.getComponent(0)).add(all, BorderLayout.EAST);

        List<KoperasiStore.SavingsTransaction> transactions =
                koperasiStore.getSavingsTransactions(currentAccount.username);
        int rowCount = Math.min(3, transactions.size());
        Object[][] rows = new Object[Math.max(1, rowCount)][4];
        DateTimeFormatter shortDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (rowCount == 0) {
            rows[0] = new Object[] {"—", "Belum ada simpanan", "—", "—"};
        } else {
            for (int i = 0; i < rowCount; i++) {
                KoperasiStore.SavingsTransaction item = transactions.get(i);
                rows[i] = new Object[] {item.date.format(shortDate), item.type,
                        money(Math.abs(item.amount)), "✓ Berhasil"};
            }
        }
        JTable table = makeTable(
                new String[] {"Tanggal", "Jenis Simpanan", "Nominal", "Status"}, rows);
        styleStaffTable(table);
        table.setRowHeight(35);
        int[] widths = {112, 160, 112, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(3).setCellRenderer(new MemberStatusCellRenderer());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildMemberNotificationPanel() {
        JPanel section = memberSection("Notifikasi Terbaru", "bell");
        JButton all = memberLink("Lihat Semua  ›", () -> showPage("notifications"));
        ((JPanel) section.getComponent(0)).add(all, BorderLayout.EAST);
        List<KoperasiStore.Notice> notices =
                koperasiStore.getNotifications(currentAccount.username);
        JPanel notice = new JPanel(new BorderLayout(12, 0));
        notice.setBackground(new Color(224, 238, 255));
        notice.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel info = new JLabel(new DashboardGlyph("info", new Color(22, 105, 221), 30));
        notice.add(info, BorderLayout.WEST);
        JPanel messageBlock = new JPanel(new BorderLayout(8, 3));
        messageBlock.setOpaque(false);
        String message = notices.isEmpty()
                ? "Belum ada notifikasi terbaru."
                : notices.get(0).message;
        JLabel messageLabel = new JLabel("<html>" + escape(message) + "</html>");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageLabel.setForeground(new Color(18, 39, 89));
        messageBlock.add(messageLabel, BorderLayout.CENTER);
        if (!notices.isEmpty()) {
            JLabel time = new JLabel(notices.get(0).createdAt.format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy  ·  HH:mm", new Locale("id", "ID"))));
            time.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            time.setForeground(new Color(103, 116, 144));
            messageBlock.add(time, BorderLayout.SOUTH);
        }
        notice.add(messageBlock, BorderLayout.CENTER);
        section.add(notice, BorderLayout.CENTER);
        return section;
    }

    private JPanel memberSection(String title, String icon) {
        JPanel section = new JPanel(new BorderLayout(0, 9));
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 229, 247)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel label = new JLabel(title, new DashboardGlyph(icon,
                new Color(19, 99, 215), 25), SwingConstants.LEFT);
        label.setIconTextGap(8);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(new Color(12, 34, 91));
        heading.add(label, BorderLayout.CENTER);
        section.add(heading, BorderLayout.NORTH);
        return section;
    }

    private JButton memberLink(String title, Runnable action) {
        JButton link = new JButton(title);
        link.setFont(new Font("Segoe UI", Font.BOLD, 11));
        link.setForeground(new Color(13, 91, 209));
        link.setBackground(new Color(229, 240, 255));
        link.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        link.setFocusPainted(false);
        link.setContentAreaFilled(true);
        link.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        link.addActionListener(event -> action.run());
        return link;
    }

    private JPanel buildMembers() throws IOException {
        List<AccountStore.Account> all = accountStore.getAccounts();
        List<AccountStore.Account> members = new ArrayList<AccountStore.Account>();
        for (AccountStore.Account account : all) {
            if (AccountStore.MEMBER.equals(account.role)) members.add(account);
        }
        Object[][] rows = new Object[members.size()][8];
        for (int i = 0; i < members.size(); i++) {
            AccountStore.Account member = members.get(i);
            rows[i] = new Object[] {member.fullName, member.username, member.phone,
                    member.nik, member.address, member.status, member.proofPath,
                    member.enabled ? "Aktif" : "Nonaktif"};
        }
        String[] columns = {"Nama", "Username", "Telepon", "NIK", "Alamat", "Pendaftaran",
                "Bukti Pembayaran (lokasi)", "Akun"};
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        if (isAdmin()) {
            JTable table = makeTable(columns, rows);
            JButton approve = actionButton("Setujui Pendaftaran");
            JButton reject = actionButton("Tolak Pendaftaran");
            JButton edit = actionButton("Ubah Profil");
            JButton addMember = actionButton("Tambah Pendaftaran Anggota");
            JButton proof = actionButton("Buka Bukti Pembayaran");
            JButton delete = actionButton("Hapus Anggota");
            approve.addActionListener(e -> verifyMember(table, true));
            reject.addActionListener(e -> verifyMember(table, false));
            edit.addActionListener(e -> editMember(table));
            addMember.addActionListener(e -> showAssistedRegistration());
            proof.addActionListener(e -> openPaymentProof(table));
            delete.addActionListener(e -> deleteAccount(table, 1));
            actions.add(approve);
            actions.add(reject);
            actions.add(edit);
            actions.add(addMember);
            actions.add(proof);
            actions.add(delete);
            return tablePage(columns, rows, actions, table);
        }
        if (isStaff()) {
            JButton register = actionButton("Bantu Pendaftaran Anggota");
            register.addActionListener(e -> showAssistedRegistration());
            actions.add(register);
            Object[][] staffRows = new Object[members.size()][7];
            for (int i = 0; i < members.size(); i++) {
                AccountStore.Account member = members.get(i);
                staffRows[i] = new Object[] {member.fullName, member.username, member.phone,
                        member.nik, member.address, member.status, member.enabled ? "Aktif" : "Nonaktif"};
            }
            String[] staffColumns = {"Nama", "Username", "Telepon", "NIK", "Alamat",
                    "Pendaftaran", "Akun"};
            return tablePage(staffColumns, staffRows, actions, null);
        }
        return tablePage(columns, rows, actions, null);
    }

    private JPanel buildStaff() throws IOException {
        List<AccountStore.Account> staff = new ArrayList<AccountStore.Account>();
        for (AccountStore.Account account : accountStore.getAccounts()) {
            if (AccountStore.STAFF.equals(account.role)) staff.add(account);
        }
        Object[][] rows = new Object[staff.size()][4];
        for (int i = 0; i < staff.size(); i++) {
            AccountStore.Account item = staff.get(i);
            rows[i] = new Object[] {item.fullName, item.username, item.status,
                    item.enabled ? "Aktif" : "Nonaktif"};
        }
        String[] columns = {"Nama", "Username", "Status", "Akses"};
        JTable table = makeTable(columns, rows);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton add = actionButton("Tambah Petugas");
        JButton edit = actionButton("Ubah Nama");
        JButton toggle = actionButton("Aktifkan/Nonaktifkan");
        JButton remove = actionButton("Hapus Petugas");
        add.addActionListener(e -> showCreateStaffDialog());
        edit.addActionListener(e -> editAccountName(table, 1));
        toggle.addActionListener(e -> toggleUser(table, 1));
        remove.addActionListener(e -> deleteAccount(table, 1));
        actions.add(add);
        actions.add(edit);
        actions.add(toggle);
        actions.add(remove);
        return tablePage(columns, rows, actions, table);
    }

    private JPanel buildSavings(boolean ownOnly) {
        List<KoperasiStore.SavingsTransaction> transactions =
                koperasiStore.getSavingsTransactions(ownOnly ? currentAccount.username : null);
        Object[][] rows = new Object[transactions.size()][7];
        for (int i = 0; i < transactions.size(); i++) {
            KoperasiStore.SavingsTransaction item = transactions.get(i);
            rows[i] = new Object[] {formatDate(item.date), item.memberName, item.username,
                    item.type, money(item.amount), item.reference, item.actor};
        }
        String[] columns = {"Tanggal", "Anggota", "Username", "Jenis", "Nominal", "Referensi", "Petugas"};
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        if (!ownOnly && !isAdmin()) {
            JButton deposit = actionButton("Catat Simpanan");
            deposit.addActionListener(e -> showDepositDialog());
            actions.add(deposit);
        }
        return tablePage(columns, rows, actions, null);
    }

    private JPanel buildWithdrawals() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        JPanel card = metricCard("Saldo Simpanan Sukarela Anggota",
                "Pilih anggota dan lakukan penarikan maksimal sebesar saldo sukarela.");
        body.add(card, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        JButton withdraw = actionButton("Proses Penarikan");
        withdraw.addActionListener(e -> showWithdrawalDialog());
        actions.add(withdraw);
        body.add(actions, BorderLayout.CENTER);
        return body;
    }

    private JPanel buildLoans(boolean ownOnly) {
        List<KoperasiStore.Loan> loans = koperasiStore.getLoans(
                ownOnly ? currentAccount.username : null);
        Object[][] rows = new Object[loans.size()][8];
        for (int i = 0; i < loans.size(); i++) {
            KoperasiStore.Loan loan = loans.get(i);
            rows[i] = new Object[] {loan.id, loan.memberName, loan.username, money(loan.principal),
                    loan.purpose, loan.months + " bulan", loan.status,
                    loan.disbursed ? money(loan.remaining) : "-"};
        }
        String[] columns = {"ID", "Anggota", "Username", "Nominal", "Tujuan",
                "Jangka Waktu", "Status", "Sisa Pinjaman"};
        JTable table = makeTable(columns, rows);
        hideColumn(table, 0);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        if (ownOnly) {
            JButton apply = actionButton("Ajukan Pinjaman");
            apply.addActionListener(e -> showLoanApplication());
            actions.add(apply);
        } else if (isAdmin()) {
            JButton approve = actionButton("Setujui");
            JButton reject = actionButton("Tolak");
            approve.addActionListener(e -> decideLoan(table, true));
            reject.addActionListener(e -> decideLoan(table, false));
            actions.add(approve);
            actions.add(reject);
        }
        return tablePage(columns, rows, actions, table);
    }

    private JPanel buildDisbursements() {
        List<KoperasiStore.Loan> loans = koperasiStore.getLoans(null);
        List<KoperasiStore.Loan> approved = new ArrayList<KoperasiStore.Loan>();
        for (KoperasiStore.Loan loan : loans) {
            if ("Disetujui".equals(loan.status)) approved.add(loan);
        }
        Object[][] rows = new Object[approved.size()][6];
        for (int i = 0; i < approved.size(); i++) {
            KoperasiStore.Loan loan = approved.get(i);
            rows[i] = new Object[] {loan.id, loan.memberName, loan.username, money(loan.principal),
                    loan.months + " bulan", loan.status};
        }
        String[] columns = {"ID", "Anggota", "Username", "Nominal", "Jangka Waktu", "Status"};
        JTable table = makeTable(columns, rows);
        hideColumn(table, 0);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton disburse = actionButton("Cairkan Pinjaman Terpilih");
        disburse.addActionListener(e -> disburseLoan(table));
        actions.add(disburse);
        return tablePage(columns, rows, actions, table);
    }

    private JPanel buildInstallments(boolean ownOnly) {
        List<KoperasiStore.Installment> installments =
                koperasiStore.getInstallments(ownOnly ? currentAccount.username : null);
        Object[][] rows = new Object[installments.size()][7];
        for (int i = 0; i < installments.size(); i++) {
            KoperasiStore.Installment item = installments.get(i);
            rows[i] = new Object[] {item.id, item.memberName, item.username, item.number,
                    item.dueDate.toString(), money(item.amount), item.paid ? "Dibayar" : "Belum Dibayar"};
        }
        String[] columns = {"ID", "Anggota", "Username", "Ke-", "Jatuh Tempo", "Nominal", "Status"};
        JTable table = makeTable(columns, rows);
        hideColumn(table, 0);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        if (!ownOnly) {
            JButton pay = actionButton("Catat Pembayaran Angsuran");
            pay.addActionListener(e -> payInstallment(table));
            actions.add(pay);
        }
        return tablePage(columns, rows, actions, table);
    }

    private JPanel buildHistory(boolean ownOnly) {
        List<KoperasiStore.SavingsTransaction> transactions =
                koperasiStore.getSavingsTransactions(ownOnly ? currentAccount.username : null);
        Object[][] rows = new Object[transactions.size()][7];
        for (int i = 0; i < transactions.size(); i++) {
            KoperasiStore.SavingsTransaction item = transactions.get(i);
            rows[i] = new Object[] {item.id, formatDate(item.date), item.memberName, item.username,
                    item.type, money(item.amount), item.reference};
        }
        String[] columns = {"ID", "Tanggal", "Anggota", "Username", "Jenis", "Nominal", "Referensi"};
        JTable table = makeTable(columns, rows);
        hideColumn(table, 0);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton print = actionButton("Cetak Bukti Transaksi");
        print.addActionListener(e -> printReceipt(table));
        actions.add(print);
        return tablePage(columns, rows, actions, table);
    }

    private JPanel buildReports() throws IOException {
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 0));
        cards.setOpaque(false);
        cards.add(metricCard("Total Simpanan", money(koperasiStore.getTotalSavings())));
        cards.add(metricCard("Pinjaman Aktif", money(koperasiStore.getActiveLoans())));
        cards.add(metricCard("Transaksi Tersimpan",
                Integer.toString(koperasiStore.getSavingsTransactions(null).size())));
        body.add(cards, BorderLayout.NORTH);
        JTextArea summary = new JTextArea("Laporan mencakup transaksi simpanan, pinjaman, dan angsuran. "
                + "Ekspor menghasilkan CSV lokal yang dapat dibuka dengan aplikasi spreadsheet.");
        summary.setEditable(false);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        summary.setBackground(Color.WHITE);
        summary.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        body.add(summary, BorderLayout.CENTER);
        JButton export = actionButton("Ekspor Laporan CSV");
        export.addActionListener(e -> exportReport());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        actions.add(export);
        body.add(actions, BorderLayout.SOUTH);
        return body;
    }

    private JPanel buildUsers() throws IOException {
        List<AccountStore.Account> accounts = accountStore.getAccounts();
        Object[][] rows = new Object[accounts.size()][5];
        for (int i = 0; i < accounts.size(); i++) {
            AccountStore.Account item = accounts.get(i);
            rows[i] = new Object[] {item.fullName, item.username, item.role, item.status,
                    item.enabled ? "Aktif" : "Nonaktif"};
        }
        String[] columns = {"Nama", "Username", "Peran", "Status Pendaftaran", "Akses"};
        JTable table = makeTable(columns, rows);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton addAdmin = actionButton("Tambah Admin");
        JButton add = actionButton("Tambah Petugas");
        JButton edit = actionButton("Ubah Nama");
        JButton toggle = actionButton("Aktifkan/Nonaktifkan");
        JButton delete = actionButton("Hapus Akun");
        addAdmin.addActionListener(e -> showCreateAccountDialog(AccountStore.ADMIN));
        add.addActionListener(e -> showCreateStaffDialog());
        edit.addActionListener(e -> editAccountName(table, 1));
        toggle.addActionListener(e -> toggleUser(table, 1));
        delete.addActionListener(e -> deleteAccount(table, 1));
        actions.add(addAdmin);
        actions.add(add);
        actions.add(edit);
        actions.add(toggle);
        actions.add(delete);
        return tablePage(columns, rows, actions, table);
    }

    private JPanel buildSettings() {
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 7));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JTextField cooperative = new JTextField(koperasiStore.getCooperativeName());
        double interest = koperasiStore.getAnnualInterestPercent();
        JTextField rate = new JTextField(interest < 0 ? "" : Double.toString(interest));
        double penalty = koperasiStore.getLatePenaltyPercent();
        JTextField latePenalty = new JTextField(penalty < 0 ? "" : Double.toString(penalty));
        form.add(new JLabel("Nama koperasi"));
        form.add(cooperative);
        form.add(new JLabel("Bunga sederhana per tahun (%) — kosong berarti tidak dihitung"));
        form.add(rate);
        form.add(new JLabel("Denda satu kali per angsuran terlambat (%) — kosong berarti tanpa denda"));
        form.add(latePenalty);
        JButton save = actionButton("Simpan Pengaturan");
        save.addActionListener(event -> {
            try {
                koperasiStore.updateSettings(cooperative.getText(), rate.getText(),
                        latePenalty.getText());
                JOptionPane.showMessageDialog(this, "Pengaturan berhasil disimpan.");
                setTitle(koperasiStore.getCooperativeName() + " - " + currentAccount.role);
                showPage("settings");
            } catch (IOException | IllegalArgumentException e) {
                showError(e);
            }
        });
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(form, BorderLayout.NORTH);
        body.add(save, BorderLayout.SOUTH);
        return body;
    }

    private JPanel buildProfile() {
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 7));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JTextField name = new JTextField(currentAccount.fullName);
        JTextField phone = new JTextField(currentAccount.phone);
        JTextField nik = new JTextField(currentAccount.nik);
        JTextField address = new JTextField(currentAccount.address);
        form.add(new JLabel("Nama lengkap"));
        form.add(name);
        form.add(new JLabel("Username"));
        form.add(new JLabel(currentAccount.username));
        form.add(new JLabel("Nomor telepon"));
        form.add(phone);
        form.add(new JLabel("NIK"));
        form.add(nik);
        form.add(new JLabel("Alamat"));
        form.add(address);
        JButton save = actionButton("Simpan Profil");
        save.addActionListener(event -> {
            try {
                accountStore.updateMemberProfile(currentAccount.username, name.getText(),
                        phone.getText(), nik.getText(), address.getText());
                    currentAccount.fullName = name.getText().trim();
                    currentAccount.phone = phone.getText().trim();
                    currentAccount.nik = nik.getText().trim();
                    currentAccount.address = address.getText().trim();
                    if (identityLabel != null) {
                        identityLabel.setText(currentAccount.fullName + "  ·  " + currentAccount.role);
                    }
                    if (memberProfileButton != null) {
                        memberProfileButton.setText(currentAccount.fullName + "  ⌄");
                    }
                JOptionPane.showMessageDialog(this, "Profil berhasil disimpan.");
                showPage("profile");
            } catch (IOException | IllegalArgumentException e) {
                showError(e);
            }
        });
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(form, BorderLayout.NORTH);
        body.add(save, BorderLayout.SOUTH);
        return body;
    }

    private JPanel buildNotifications() {
        List<KoperasiStore.Notice> notices =
                koperasiStore.getNotifications(currentAccount.username);
        Object[][] rows = new Object[notices.size()][2];
        for (int i = 0; i < notices.size(); i++) {
            KoperasiStore.Notice notice = notices.get(i);
            rows[i] = new Object[] {formatDate(notice.createdAt), notice.message};
        }
        return tablePage(new String[] {"Tanggal", "Pesan"}, rows, null, null);
    }

    private JPanel tablePage(String[] columns, Object[][] rows, JPanel actions, JTable suppliedTable) {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        if (actions != null && actions.getComponentCount() > 0) body.add(actions, BorderLayout.NORTH);
        JTable table = suppliedTable == null ? makeTable(columns, rows) : suppliedTable;
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(222, 229, 240)));
        body.add(scroll, BorderLayout.CENTER);
        return body;
    }

    private JPanel panelWithTable(String title, String[] columns, Object[][] rows, JPanel actions) {
        JPanel panel = new JPanel(new BorderLayout(0, 9));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 229, 240)),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        heading.setForeground(BLUE);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(new JScrollPane(makeTable(columns, rows)), BorderLayout.CENTER);
        if (actions != null) panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JTable makeTable(String[] columns, Object[][] rows) {
        DefaultTableModel model = new DefaultTableModel(rows, columns) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(29);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setAutoCreateRowSorter(true);
        return table;
    }

    private JPanel metricCard(String label, String value) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 229, 240)),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        JLabel caption = new JLabel(label);
        caption.setForeground(new Color(97, 111, 139));
        caption.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JLabel amount = new JLabel(value);
        amount.setForeground(BLUE);
        amount.setFont(new Font("Segoe UI", Font.BOLD, 20));
        card.add(caption, BorderLayout.NORTH);
        card.add(amount, BorderLayout.CENTER);
        return card;
    }

    private JButton actionButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(BLUE);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }

    private void verifyMember(JTable table, boolean approve) {
        String username = selected(table, 1);
        if (username == null) return;
        AccountStore.Account member = findAccount(username);
        if (member == null || !"Menunggu Verifikasi".equals(member.status)) {
            warn("Hanya pendaftaran yang menunggu verifikasi dapat diproses.");
            return;
        }
        File proof = new File(member.proofPath);
        if (approve && (member.proofPath.isEmpty()
                || !proof.isFile() || !isAllowedProofFile(proof))) {
            warn("Bukti pembayaran tidak ditemukan. Periksa file bukti terlebih dahulu.");
            return;
        }
        if (!confirm(approve ? "Setujui pendaftaran " + member.fullName
                + " dan catat simpanan awal Rp70.000?" : "Tolak pendaftaran " + member.fullName + "?")) {
            return;
        }
        try {
            if (approve) koperasiStore.recordRegistrationSavings(member.username, member.fullName);
            accountStore.setMemberVerification(username, approve);
            JOptionPane.showMessageDialog(this,
                    approve ? "Anggota disetujui. Simpanan pokok Rp50.000 dan wajib Rp20.000 dicatat."
                            : "Pendaftaran anggota ditolak.");
            showPage("members");
        } catch (IOException | IllegalArgumentException e) {
            showError(e);
        }
    }

    private void editMember(JTable table) {
        String username = selected(table, 1);
        AccountStore.Account member = username == null ? null : findAccount(username);
        if (member == null) return;
        JTextField name = new JTextField(member.fullName);
        JTextField phone = new JTextField(member.phone);
        JTextField nik = new JTextField(member.nik);
        JTextField address = new JTextField(member.address);
        JPanel form = labeledForm(new String[] {"Nama", "Telepon", "NIK", "Alamat"},
                new JTextField[] {name, phone, nik, address});
        if (JOptionPane.showConfirmDialog(this, form, "Ubah Data Anggota",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            accountStore.updateMemberProfile(
                    username, name.getText(), phone.getText(), nik.getText(), address.getText());
            showPage("members");
        } catch (IOException | IllegalArgumentException e) {
            showError(e);
        }
    }

    private void editAccountName(JTable table, int usernameColumn) {
        String username = selected(table, usernameColumn);
        AccountStore.Account account = username == null ? null : findAccount(username);
        if (account == null) return;
        String name = JOptionPane.showInputDialog(this, "Nama akun:", account.fullName);
        if (name == null) return;
        try {
            accountStore.renameAccount(username, name);
            showPage(currentPage);
        } catch (IOException | IllegalArgumentException e) {
            showError(e);
        }
    }

    private void openPaymentProof(JTable table) {
        String username = selected(table, 1);
        AccountStore.Account member = username == null ? null : findAccount(username);
        if (member == null) return;
        File proofFile = new File(member.proofPath);
        if (member.proofPath.isEmpty() || !proofFile.isFile() || !isAllowedProofFile(proofFile)) {
            warn("File bukti pembayaran tidak ditemukan.");
            return;
        }
        if (!Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            JOptionPane.showMessageDialog(this, "Bukti tersimpan di:\n" + member.proofPath,
                    "Lokasi Bukti Pembayaran", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Desktop.getDesktop().open(proofFile);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            showError(e);
        }
    }

    private void showCreateStaffDialog() {
        showCreateAccountDialog(AccountStore.STAFF);
    }

    private void showCreateAccountDialog(String role) {
        JTextField name = new JTextField(22);
        JTextField username = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        String roleName = AccountStore.ADMIN.equals(role) ? "Admin" : "Petugas";
        JPanel form = labeledForm(new String[] {"Nama " + roleName, "Username", "Password"},
                new JTextField[] {name, username, password});
        if (JOptionPane.showConfirmDialog(this, form, "Tambah Akun " + roleName,
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        char[] secret = password.getPassword();
        try {
            if (AccountStore.ADMIN.equals(role)) {
                accountStore.createAdmin(name.getText(), username.getText(), secret);
            } else {
                accountStore.createStaff(name.getText(), username.getText(), secret);
            }
            JOptionPane.showMessageDialog(this, "Akun " + roleName + " berhasil dibuat.");
            showPage(currentPage);
        } catch (IOException | java.security.GeneralSecurityException | IllegalArgumentException e) {
            showError(e);
        } finally {
            java.util.Arrays.fill(secret, '\0');
        }
    }

    private void showAssistedRegistration() {
        JTextField name = new JTextField(22);
        JTextField username = new JTextField(22);
        JTextField phone = new JTextField(22);
        JTextField nik = new JTextField(22);
        JTextField address = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        JCheckBox agree = new JCheckBox("Saya telah membaca dan menyetujui ketentuan koperasi.");
        JLabel proofLabel = new JLabel("Belum ada bukti.");
        final File[] proof = new File[1];
        JButton choose = new JButton("Pilih Bukti Pembayaran Rp70.000");
        choose.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                    "Bukti pembayaran (PNG, JPG, PDF)", "png", "jpg", "jpeg", "pdf"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                proof[0] = chooser.getSelectedFile();
                proofLabel.setText(proof[0].getName());
            }
        });
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 5));
        form.add(new JLabel("Nama")); form.add(name);
        form.add(new JLabel("Username")); form.add(username);
        form.add(new JLabel("Telepon")); form.add(phone);
        form.add(new JLabel("NIK (16 digit)")); form.add(nik);
        form.add(new JLabel("Alamat")); form.add(address);
        form.add(new JLabel("Password sementara")); form.add(password);
        form.add(agree);
        form.add(choose); form.add(proofLabel);
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(form),
                "Bantu Pendaftaran Anggota", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (!agree.isSelected()) {
            warn("Persetujuan ketentuan koperasi wajib dicentang.");
            return;
        }
        if (proof[0] == null || !proof[0].isFile() || !isAllowedProofFile(proof[0])) {
            warn("Pilih file bukti pembayaran berformat PNG, JPG, atau PDF.");
            return;
        }
        char[] secret = password.getPassword();
        Path localProof = null;
        try {
            Path folder = Paths.get("data", "bukti-pembayaran");
            Files.createDirectories(folder);
            String filename = proof[0].getName();
            String extension = filename.lastIndexOf('.') >= 0
                    ? filename.substring(filename.lastIndexOf('.')) : ".file";
            localProof = folder.resolve(UUID.randomUUID().toString() + extension);
            Files.copy(proof[0].toPath(), localProof);
            accountStore.registerMember(name.getText(), username.getText(), secret,
                    localProof.toString(), phone.getText(), nik.getText(), address.getText());
            JOptionPane.showMessageDialog(this, "Pendaftaran tersimpan menunggu verifikasi Admin.");
            showPage("members");
        } catch (IOException | java.security.GeneralSecurityException | IllegalArgumentException e) {
            if (localProof != null) {
                try {
                    Files.deleteIfExists(localProof);
                } catch (IOException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }
            showError(e);
        } finally {
            java.util.Arrays.fill(secret, '\0');
        }
    }

    private void showDepositDialog() {
        String username = chooseMember();
        if (username == null) return;
        AccountStore.Account member = findAccount(username);
        if (member == null) return;
        Object choice = JOptionPane.showInputDialog(this, "Pilih jenis simpanan:",
                "Catat Simpanan", JOptionPane.QUESTION_MESSAGE, null,
                new String[] {KoperasiStore.WAJIB, KoperasiStore.SUKARELA}, KoperasiStore.WAJIB);
        if (choice == null) return;
        long amount = KoperasiStore.WAJIB.equals(choice) ? 20000 : requestAmount("Nominal simpanan sukarela");
        if (amount <= 0) return;
        if (!confirm("Catat simpanan " + choice + " sebesar " + money(amount)
                + " untuk " + member.fullName + "?")) return;
        try {
            koperasiStore.addSavings(username, member.fullName, choice.toString(), amount,
                    "Pembayaran", currentAccount.fullName);
            JOptionPane.showMessageDialog(this, "Simpanan berhasil dicatat.");
            showPage("savings");
        } catch (IOException | IllegalArgumentException e) {
            showError(e);
        }
    }

    private void showWithdrawalDialog() {
        String username = chooseMember();
        if (username == null) return;
        AccountStore.Account member = findAccount(username);
        if (member == null) return;
        long balance = koperasiStore.getSavingsBalance(username, KoperasiStore.SUKARELA);
        String value = JOptionPane.showInputDialog(this,
                "Saldo sukarela tersedia: " + money(balance) + "\nNominal penarikan:");
        if (value == null) return;
        long amount;
        try {
            amount = parseAmount(value);
        } catch (IllegalArgumentException e) {
            showError(e);
            return;
        }
        if (!confirm("Proses penarikan " + money(amount) + " untuk " + member.fullName + "?")) return;
        try {
            koperasiStore.withdrawVoluntary(username, member.fullName, amount, currentAccount.fullName);
            JOptionPane.showMessageDialog(this, "Penarikan berhasil diproses.");
            showPage("withdraw");
        } catch (IOException | IllegalArgumentException e) {
            showError(e);
        }
    }

    private void showLoanApplication() {
        JTextField amount = new JTextField();
        JTextField purpose = new JTextField();
        JTextField months = new JTextField("12");
        JPanel form = labeledForm(new String[] {"Nominal (angka positif)", "Tujuan pinjaman",
                "Jangka waktu (bulan, 1–120)"}, new JTextField[] {amount, purpose, months});
        if (JOptionPane.showConfirmDialog(this, form, "Pengajuan Pinjaman",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            long nominal = parseAmount(amount.getText());
            int duration = Integer.parseInt(months.getText().trim());
            if (!confirm("Kirim pengajuan pinjaman " + money(nominal) + "?")) return;
            koperasiStore.requestLoan(currentAccount.username, currentAccount.fullName,
                    nominal, purpose.getText(), duration);
            JOptionPane.showMessageDialog(this, "Pengajuan dikirim dan menunggu keputusan Admin.");
            showPage("myLoans");
        } catch (IOException | IllegalArgumentException e) {
            showError(e);
        }
    }

    private void decideLoan(JTable table, boolean approve) {
        String loanId = selected(table, 0);
        if (loanId == null) return;
        if (!confirm((approve ? "Setujui" : "Tolak") + " pengajuan pinjaman ini?")) return;
        try {
            koperasiStore.decideLoan(loanId, approve);
            JOptionPane.showMessageDialog(this, approve ? "Pinjaman disetujui." : "Pinjaman ditolak.");
            showPage("loans");
        } catch (IOException | IllegalArgumentException e) {
            showError(e);
        }
    }

    private void disburseLoan(JTable table) {
        String loanId = selected(table, 0);
        if (loanId == null) return;
        if (!confirm("Cairkan pinjaman terpilih dan buat jadwal angsuran?")) return;
        try {
            koperasiStore.disburseLoan(loanId, currentAccount.fullName);
            JOptionPane.showMessageDialog(this, "Pinjaman dicairkan dan jadwal angsuran dibuat.");
            showPage("disburse");
        } catch (IOException | IllegalArgumentException e) {
            showError(e);
        }
    }

    private void payInstallment(JTable table) {
        String installmentId = selected(table, 0);
        if (installmentId == null) return;
        if (!confirm("Catat pembayaran angsuran terpilih?")) return;
        try {
            koperasiStore.payInstallment(installmentId, currentAccount.fullName);
            JOptionPane.showMessageDialog(this, "Pembayaran angsuran berhasil dicatat.");
            showPage("installments");
        } catch (IOException | IllegalArgumentException e) {
            showError(e);
        }
    }

    private void printReceipt(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            warn("Pilih transaksi yang akan dicetak.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        StringBuilder text = new StringBuilder();
        text.append(koperasiStore.getCooperativeName()).append("\nBUKTI TRANSAKSI\n\n")
                .append("Tanggal : ").append(table.getModel().getValueAt(modelRow, 1)).append('\n')
                .append("Anggota : ").append(table.getModel().getValueAt(modelRow, 2)).append('\n')
                .append("Username: ").append(table.getModel().getValueAt(modelRow, 3)).append('\n')
                .append("Jenis   : ").append(table.getModel().getValueAt(modelRow, 4)).append('\n')
                .append("Nominal : ").append(table.getModel().getValueAt(modelRow, 5)).append('\n')
                .append("Ref     : ").append(table.getModel().getValueAt(modelRow, 6)).append('\n');
        JTextArea receipt = new JTextArea(text.toString(), 12, 42);
        receipt.setEditable(false);
        try {
            receipt.print();
        } catch (java.awt.print.PrinterException e) {
            showError(e);
        }
    }

    private void exportReport() {
        JFileChooser chooser = new JFileChooser(Paths.get("reports").toFile());
        chooser.setDialogTitle("Simpan Laporan Koperasi");
        chooser.setSelectedFile(new File("laporan-koperasi.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path target = chooser.getSelectedFile().toPath();
        if (!target.toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            target = Paths.get(target.toString() + ".csv");
        }
        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            for (String row : koperasiStore.reportRows()) {
                writer.write(row);
                writer.newLine();
            }
            writer.write("\nLAPORAN PINJAMAN\nID,Anggota,Nominal,Tujuan,Jangka Waktu,Status,Sisa");
            writer.newLine();
            for (KoperasiStore.Loan loan : koperasiStore.getLoans(null)) {
                writer.write(loan.id + "," + csv(loan.memberName) + "," + loan.principal + ","
                        + csv(loan.purpose) + "," + loan.months + "," + loan.status + ","
                        + loan.remaining);
                writer.newLine();
            }
            writer.write("\nLAPORAN ANGSURAN\nAnggota,Jatuh Tempo,Nominal,Status");
            writer.newLine();
            for (KoperasiStore.Installment item : koperasiStore.getInstallments(null)) {
                writer.write(csv(item.memberName) + "," + item.dueDate + "," + item.amount + ","
                        + (item.paid ? "Dibayar" : "Belum Dibayar"));
                writer.newLine();
            }
            JOptionPane.showMessageDialog(this, "Laporan berhasil disimpan:\n" + target);
        } catch (IOException e) {
            showError(e);
        }
    }

    private void toggleUser(JTable table, int usernameColumn) {
        String username = selected(table, usernameColumn);
        if (username == null) return;
        if (username.equalsIgnoreCase(currentAccount.username)) {
            warn("Akun yang sedang digunakan tidak dapat dinonaktifkan.");
            return;
        }
        AccountStore.Account account = findAccount(username);
        if (account == null) return;
        boolean enable = !account.enabled;
        if (!confirm((enable ? "Aktifkan" : "Nonaktifkan") + " akun " + username + "?")) return;
        try {
            accountStore.setAccountEnabled(username, enable);
            showPage(currentPage);
        } catch (IOException | IllegalArgumentException e) {
            showError(e);
        }
    }

    private void deleteAccount(JTable table, int usernameColumn) {
        String username = selected(table, usernameColumn);
        if (username == null) return;
        if (username.equalsIgnoreCase(currentAccount.username)) {
            warn("Akun yang sedang digunakan tidak dapat dihapus.");
            return;
        }
        if (koperasiStore.hasUserData(username)) {
            warn("Akun memiliki riwayat transaksi atau pinjaman. Nonaktifkan akun agar riwayat keuangan tetap utuh.");
            return;
        }
        if (!confirm("Hapus akun " + username + "? Tindakan ini tidak dapat dibatalkan.")) return;
        try {
            accountStore.deleteAccount(username);
            showPage(currentPage);
        } catch (IOException | IllegalArgumentException e) {
            showError(e);
        }
    }

    private JPanel labeledForm(String[] labels, JTextField[] fields) {
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 5));
        for (int i = 0; i < labels.length; i++) {
            form.add(new JLabel(labels[i]));
            form.add(fields[i]);
        }
        return form;
    }

    private String chooseMember() {
        try {
            List<String> options = new ArrayList<String>();
            for (AccountStore.Account account : accountStore.getAccounts()) {
                if (AccountStore.MEMBER.equals(account.role) && "Aktif".equals(account.status)
                        && account.enabled) {
                    options.add(account.username + " — " + account.fullName);
                }
            }
            if (options.isEmpty()) {
                warn("Belum ada anggota aktif.");
                return null;
            }
            Object selected = JOptionPane.showInputDialog(this, "Pilih anggota:",
                    "Anggota", JOptionPane.QUESTION_MESSAGE, null, options.toArray(),
                    options.get(0));
            if (selected == null) return null;
            return selected.toString().split(" — ", 2)[0];
        } catch (IOException e) {
            showError(e);
            return null;
        }
    }

    private AccountStore.Account findAccount(String username) {
        try {
            for (AccountStore.Account item : accountStore.getAccounts()) {
                if (item.username.equalsIgnoreCase(username)) return item;
            }
        } catch (IOException e) {
            showError(e);
        }
        return null;
    }

    private String selected(JTable table, int column) {
        int row = table.getSelectedRow();
        if (row < 0) {
            warn("Pilih data pada tabel terlebih dahulu.");
            return null;
        }
        return table.getModel().getValueAt(table.convertRowIndexToModel(row), column).toString();
    }

    private void hideColumn(JTable table, int index) {
        TableColumn column = table.getColumnModel().getColumn(index);
        column.setMinWidth(0);
        column.setMaxWidth(0);
        column.setPreferredWidth(0);
    }

    private long requestAmount(String prompt) {
        String input = JOptionPane.showInputDialog(this, prompt + " (angka positif):");
        if (input == null) return -1;
        try {
            return parseAmount(input);
        } catch (IllegalArgumentException e) {
            showError(e);
            return -1;
        }
    }

    private long parseAmount(String input) {
        String value = input.trim().replace("Rp", "").replace(".", "").replace(",", "");
        if (!value.matches("[0-9]+")) {
            throw new IllegalArgumentException("Nominal harus berupa angka bulat positif.");
        }
        try {
            long amount = Long.parseLong(value);
            if (amount <= 0) throw new IllegalArgumentException("Nominal harus lebih dari nol.");
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nominal terlalu besar.");
        }
    }

    private long activeMemberLoanBalance() {
        long total = 0;
        for (KoperasiStore.Loan loan : koperasiStore.getLoans(currentAccount.username)) {
            if (loan.disbursed && !"Lunas".equals(loan.status)) total += loan.remaining;
        }
        return total;
    }

    private int countPendingLoans() {
        int count = 0;
        for (KoperasiStore.Loan loan : koperasiStore.getLoans(null)) {
            if ("Menunggu".equals(loan.status)) count++;
        }
        return count;
    }

    private String nextInstallment() {
        for (KoperasiStore.Installment item : koperasiStore.getInstallments(currentAccount.username)) {
            if (!item.paid) return item.dueDate + " · " + money(item.amount);
        }
        return "Tidak ada";
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Konfirmasi",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Perhatian", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(Exception error) {
        JOptionPane.showMessageDialog(this,
                "Operasi tidak dapat diselesaikan:\n" + error.getMessage(),
                "Kesalahan", JOptionPane.ERROR_MESSAGE);
    }

    private void requestLogout() {
        if (confirm("Keluar dari akun ini?")) {
            dispose();
            onLogout.run();
        }
    }

    private static final class StaffAlert {
        final String icon;
        final String message;
        final String time;
        final String page;

        StaffAlert(String icon, String message, String time, String page) {
            this.icon = icon;
            this.message = message;
            this.time = time;
            this.page = page;
        }
    }

    private static final class MemberWelcomePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        MemberWelcomePanel(AccountStore.Account account) {
            setOpaque(false);
            setLayout(new BorderLayout(12, 0));
            setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
            setPreferredSize(new Dimension(0, 130));

            JPanel welcome = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
            welcome.setOpaque(false);
            JLabel avatar = new JLabel(new DashboardGlyph("memberAvatar",
                    new Color(34, 111, 201), 88));
            JPanel details = new JPanel();
            details.setOpaque(false);
            details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
            JLabel greeting = new JLabel("Selamat datang,");
            greeting.setFont(new Font("Segoe UI", Font.BOLD, 18));
            greeting.setForeground(new Color(13, 35, 91));
            JLabel name = new JLabel(account.fullName);
            name.setFont(new Font("Segoe UI", Font.BOLD, 29));
            name.setForeground(new Color(9, 29, 88));
            JLabel memberId = new JLabel("ID Anggota : " + account.username);
            memberId.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            memberId.setForeground(new Color(22, 43, 91));
            details.add(greeting);
            details.add(Box.createVerticalStrut(1));
            details.add(name);
            details.add(Box.createVerticalStrut(2));
            details.add(memberId);
            welcome.add(avatar);
            welcome.add(details);
            add(welcome, BorderLayout.WEST);

            boolean active = account.enabled && "Aktif".equals(account.status);
            JLabel status = new MemberPillLabel(active ? "✓  Anggota Aktif" : account.status,
                    active ? new Color(54, 177, 91) : new Color(215, 145, 45), Color.WHITE);
            status.setFont(new Font("Segoe UI", Font.BOLD, 13));
            status.setBorder(BorderFactory.createEmptyBorder(7, 13, 7, 13));
            JPanel statusWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            statusWrap.setOpaque(false);
            statusWrap.add(status);
            add(statusWrap, BorderLayout.CENTER);

            JLabel cooperative = new JLabel(new DashboardGlyph("members",
                    new Color(43, 119, 213, 170), 98));
            add(cooperative, BorderLayout.EAST);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(220, 235, 255),
                    getWidth(), getHeight(), new Color(189, 217, 253)));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            Path2D wave = new Path2D.Double();
            wave.moveTo(getWidth() * 0.53, getHeight());
            wave.curveTo(getWidth() * 0.69, getHeight() * 0.86,
                    getWidth() * 0.76, getHeight() * 0.52,
                    getWidth() * 0.88, getHeight() * 0.13);
            wave.curveTo(getWidth() * 0.94, getHeight() * 0.02,
                    getWidth() * 0.98, getHeight() * 0.10, getWidth(), getHeight() * 0.08);
            wave.lineTo(getWidth(), getHeight());
            wave.closePath();
            g.setColor(new Color(131, 182, 244, 80));
            g.fill(wave);
            Path2D lowerWave = new Path2D.Double();
            lowerWave.moveTo(getWidth() * 0.61, getHeight());
            lowerWave.curveTo(getWidth() * 0.73, getHeight() * 0.89,
                    getWidth() * 0.79, getHeight() * 0.68,
                    getWidth() * 0.89, getHeight() * 0.58);
            lowerWave.curveTo(getWidth() * 0.93, getHeight() * 0.52,
                    getWidth() * 0.98, getHeight() * 0.64,
                    getWidth(), getHeight() * 0.59);
            lowerWave.lineTo(getWidth(), getHeight());
            lowerWave.closePath();
            g.setColor(new Color(83, 153, 235, 78));
            g.fill(lowerWave);
            g.dispose();
        }
    }

    private static final class MemberPillLabel extends JLabel {
        private static final long serialVersionUID = 1L;
        private final Color pillColor;

        MemberPillLabel(String text, Color pillColor, Color textColor) {
            super(text, SwingConstants.CENTER);
            this.pillColor = pillColor;
            setForeground(textColor);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(pillColor);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class MemberStatusCellRenderer implements TableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            String text = value == null ? "" : value.toString();
            boolean successful = text.contains("Berhasil");
            MemberPillLabel pill = new MemberPillLabel(text,
                    successful ? new Color(225, 247, 232) : new Color(239, 243, 249),
                    successful ? new Color(38, 158, 75) : new Color(90, 105, 132));
            pill.setFont(new Font("Segoe UI", Font.BOLD, 11));
            pill.setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9));
            JPanel cell = new JPanel(new GridBagLayout());
            cell.setOpaque(true);
            cell.setBackground(selected ? table.getSelectionBackground() : Color.WHITE);
            cell.add(pill);
            return cell;
        }
    }

    private static final class NotificationBadgeLabel extends JLabel {
        private static final long serialVersionUID = 1L;

        NotificationBadgeLabel() {
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 10));
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(225, 57, 72));
            g.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class AdminSidebarPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        AdminSidebarPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(5, 61, 139),
                    getWidth(), getHeight(), new Color(9, 81, 166)));
            g.fillRect(0, 0, getWidth(), getHeight());
            Path2D wave = new Path2D.Double();
            wave.moveTo(0, getHeight() - 48);
            wave.curveTo(getWidth() * 0.3, getHeight() - 78, getWidth() * 0.65,
                    getHeight() - 20, getWidth(), getHeight() - 56);
            wave.lineTo(getWidth(), getHeight());
            wave.lineTo(0, getHeight());
            wave.closePath();
            g.setColor(new Color(67, 144, 231, 155));
            g.fill(wave);
            Path2D upperWave = new Path2D.Double();
            upperWave.moveTo(0, getHeight() - 29);
            upperWave.curveTo(getWidth() * 0.34, getHeight() - 61, getWidth() * 0.69,
                    getHeight() - 9, getWidth(), getHeight() - 38);
            upperWave.lineTo(getWidth(), getHeight());
            upperWave.lineTo(0, getHeight());
            upperWave.closePath();
            g.setColor(new Color(104, 173, 247, 155));
            g.fill(upperWave);
            g.dispose();
        }
    }

    private static final class AdminHeaderPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        AdminHeaderPanel() {
            super(new BorderLayout(22, 0));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(238, 245, 255),
                    getWidth(), getHeight(), new Color(218, 233, 255)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }

    private static final class AdminSavingsLoanChart extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final Color GRID = new Color(225, 232, 243);
        private final String[] months;
        private final long[] savings;
        private final long[] loans;

        AdminSavingsLoanChart(String[] months, long[] savings, long[] loans) {
            this.months = months.clone();
            this.savings = savings.clone();
            this.loans = loans.clone();
            setOpaque(false);
            setPreferredSize(new Dimension(420, 116));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 10));

            int left = 43;
            int right = 8;
            int top = 7;
            int bottom = 22;
            int chartWidth = Math.max(1, getWidth() - left - right);
            int chartHeight = Math.max(1, getHeight() - top - bottom);
            long maxValue = 100_000_000L;
            for (int i = 0; i < months.length; i++) {
                maxValue = Math.max(maxValue, Math.max(savings[i], loans[i]));
            }
            long step = (long) Math.ceil(maxValue / 4.0 / 25_000_000.0) * 25_000_000L;
            if (step < 25_000_000L) step = 25_000_000L;
            long axisMaximum = step * 4;

            for (int tick = 0; tick <= 4; tick++) {
                int y = top + chartHeight - (chartHeight * tick / 4);
                g.setColor(GRID);
                g.drawLine(left, y, left + chartWidth, y);
                g.setColor(new Color(39, 49, 73));
                String label = (axisMaximum * tick / 4 / 1_000_000L) + " jt";
                java.awt.FontMetrics metrics = g.getFontMetrics();
                g.drawString(label, left - metrics.stringWidth(label) - 7, y + 4);
            }
            g.setColor(new Color(116, 132, 160));
            g.drawLine(left, top, left, top + chartHeight);
            g.drawLine(left, top + chartHeight, left + chartWidth, top + chartHeight);

            int groupWidth = chartWidth / Math.max(1, months.length);
            int barWidth = Math.min(34, Math.max(12, groupWidth / 4));
            for (int i = 0; i < months.length; i++) {
                int center = left + groupWidth * i + groupWidth / 2;
                int savingsHeight = (int) Math.round(chartHeight
                        * (savings[i] / (double) axisMaximum));
                int loansHeight = (int) Math.round(chartHeight
                        * (loans[i] / (double) axisMaximum));
                g.setColor(new Color(48, 111, 202));
                g.fillRoundRect(center - barWidth - 2,
                        top + chartHeight - savingsHeight, barWidth, savingsHeight, 4, 4);
                g.setColor(new Color(151, 196, 247));
                g.fillRoundRect(center + 2, top + chartHeight - loansHeight,
                        barWidth, loansHeight, 4, 4);
                g.setColor(new Color(39, 49, 73));
                java.awt.FontMetrics metrics = g.getFontMetrics();
                g.drawString(months[i], center - metrics.stringWidth(months[i]) / 2,
                        top + chartHeight + 17);
            }
            g.dispose();
        }
    }

    private static final class BlueSidebarPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        BlueSidebarPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(12, 72, 148),
                    getWidth(), getHeight(), new Color(18, 91, 174)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }

    private static final class CooperativeInfoArtwork extends JPanel {
        private static final long serialVersionUID = 1L;
        private final BufferedImage image;

        CooperativeInfoArtwork() throws IOException {
            setOpaque(false);
            image = loadImage();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            Shape clip = new RoundRectangle2D.Double(0, 0, width, height, 12, 12);
            Shape previousClip = g.getClip();
            g.clip(clip);
            double scale = Math.max(width / (double) image.getWidth(),
                    height / (double) image.getHeight());
            int scaledWidth = (int) Math.ceil(image.getWidth() * scale);
            int scaledHeight = (int) Math.ceil(image.getHeight() * scale);
            int x = (width - scaledWidth) / 2;
            int y = (height - scaledHeight) / 2;
            g.drawImage(image, x, y, scaledWidth, scaledHeight, this);
            g.setClip(previousClip);
            g.setColor(new Color(213, 227, 246));
            g.draw(clip);
            g.dispose();
        }

        private BufferedImage loadImage() throws IOException {
            Path imagePath = Paths.get("assets", "gedung.png");
            if (Files.isRegularFile(imagePath)) {
                BufferedImage loaded = ImageIO.read(imagePath.toFile());
                if (loaded != null) return loaded;
            }
            try (InputStream resource =
                    Dashboard.class.getResourceAsStream("/assets/gedung.png")) {
                if (resource != null) {
                    BufferedImage loaded = ImageIO.read(resource);
                    if (loaded != null) return loaded;
                }
            }
            throw new IOException("Gambar koperasi gedung.png tidak ditemukan atau tidak valid.");
        }
    }

    private static final class DashboardGlyph implements javax.swing.Icon {
        private final String type;
        private final Color color;
        private final int size;

        DashboardGlyph(String type, Color color, int size) {
            this.type = type;
            this.color = color;
            this.size = size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            double scale = size / 24.0;
            g.translate(x, y);
            g.scale(scale, scale);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            if ("brand".equals(type)) {
                g.fill(new Ellipse2D.Double(3, 5, 5, 5));
                g.fill(new Ellipse2D.Double(9.5, 2, 5, 5));
                g.fill(new Ellipse2D.Double(16, 5, 5, 5));
                g.fillRoundRect(1, 11, 9, 7, 4, 4);
                g.fillRoundRect(8, 9, 9, 9, 4, 4);
                g.fillRoundRect(14, 11, 9, 7, 4, 4);
                Path2D hands = new Path2D.Double();
                hands.moveTo(1, 18);
                hands.quadTo(3, 21, 8, 22);
                hands.quadTo(10, 23, 12, 22);
                hands.quadTo(14, 23, 16, 22);
                hands.quadTo(21, 21, 23, 18);
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(hands);
            } else if ("memberAvatar".equals(type)) {
                g.setColor(new Color(225, 238, 255));
                g.fill(new Ellipse2D.Double(0, 0, 24, 24));
                g.setColor(new Color(34, 91, 157));
                g.fillRoundRect(3, 15, 18, 12, 7, 7);
                g.setColor(new Color(231, 167, 112));
                g.fill(new Ellipse2D.Double(6, 4, 12, 13));
                g.setColor(new Color(65, 45, 34));
                g.fillArc(5, 2, 14, 11, 0, 180);
                g.fillRoundRect(5, 7, 3, 6, 2, 2);
                g.fillRoundRect(16, 7, 3, 6, 2, 2);
                g.setColor(new Color(38, 48, 65));
                g.fill(new Ellipse2D.Double(9, 9, 1.2, 1.2));
                g.fill(new Ellipse2D.Double(14, 9, 1.2, 1.2));
                g.setColor(new Color(145, 55, 53));
                g.drawArc(10, 11, 5, 3, 205, 130);
            } else if ("wallet".equals(type)) {
                g.fillRoundRect(2, 6, 20, 15, 3, 3);
                g.setColor(Color.WHITE);
                g.fillRoundRect(13, 10, 10, 7, 2, 2);
                g.setColor(color);
                g.fill(new Ellipse2D.Double(16, 12, 2, 2));
                g.fillRoundRect(4, 4, 15, 3, 2, 2);
            } else if ("piggy".equals(type)) {
                g.fillRoundRect(3, 7, 18, 13, 6, 6);
                g.fill(new Ellipse2D.Double(17, 8, 6, 6));
                g.fill(new Ellipse2D.Double(5, 4, 5, 6));
                g.fill(new Ellipse2D.Double(15, 4, 5, 6));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(18, 11, 1.5, 1.5));
                g.fill(new Ellipse2D.Double(21, 11, 1.5, 1.5));
                g.setColor(color);
                g.fillRoundRect(6, 18, 3, 5, 1, 1);
                g.fillRoundRect(16, 18, 3, 5, 1, 1);
                g.drawLine(10, 5, 14, 5);
            } else if ("handshake".equals(type)) {
                Path2D hand = new Path2D.Double();
                hand.moveTo(2, 9);
                hand.lineTo(7, 6);
                hand.lineTo(11, 9);
                hand.lineTo(15, 6);
                hand.lineTo(22, 10);
                hand.lineTo(18, 15);
                hand.lineTo(14, 13);
                hand.lineTo(11, 16);
                hand.lineTo(4, 12);
                hand.closePath();
                g.fill(hand);
                g.setColor(Color.WHITE);
                g.drawLine(8, 10, 13, 14);
                g.drawLine(12, 9, 17, 13);
                g.drawLine(10, 12, 14, 16);
            } else if ("document".equals(type)) {
                Path2D page = new Path2D.Double();
                page.moveTo(5, 2);
                page.lineTo(15, 2);
                page.lineTo(20, 7);
                page.lineTo(20, 22);
                page.lineTo(5, 22);
                page.closePath();
                g.draw(page);
                g.drawLine(15, 2, 15, 7);
                g.drawLine(15, 7, 20, 7);
                g.drawLine(8, 11, 17, 11);
                g.drawLine(8, 15, 17, 15);
                g.drawLine(8, 19, 14, 19);
            } else if ("star".equals(type)) {
                Path2D star = new Path2D.Double();
                for (int i = 0; i < 10; i++) {
                    double angle = -Math.PI / 2 + i * Math.PI / 5;
                    double radius = i % 2 == 0 ? 10 : 4.5;
                    double px = 12 + Math.cos(angle) * radius;
                    double py = 12 + Math.sin(angle) * radius;
                    if (i == 0) star.moveTo(px, py);
                    else star.lineTo(px, py);
                }
                star.closePath();
                g.fill(star);
            } else if ("emptyLoan".equals(type)) {
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g.drawRoundRect(3, 2, 18, 20, 2, 2);
                g.drawLine(7, 7, 17, 7);
                g.drawLine(7, 11, 17, 11);
                g.drawLine(7, 15, 13, 15);
                g.setColor(new Color(67, 144, 231));
                g.fill(new Ellipse2D.Double(13, 13, 10, 10));
                g.setColor(Color.WHITE);
                g.fillRoundRect(15, 17, 6, 2, 1, 1);
            } else if ("info".equals(type)) {
                g.fill(new Ellipse2D.Double(1, 1, 22, 22));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(11, 6, 2, 2));
                g.fillRoundRect(10, 10, 4, 8, 2, 2);
            } else if ("members".equals(type) || "users".equals(type) || "profile".equals(type)
                    || "addMember".equals(type)) {
                boolean group = "members".equals(type) || "users".equals(type);
                if (group) {
                    g.fill(new Ellipse2D.Double(8.5, 2, 7, 7));
                    g.fill(new Ellipse2D.Double(1.5, 6, 5.5, 5.5));
                    g.fill(new Ellipse2D.Double(17, 6, 5.5, 5.5));
                    g.fillRoundRect(5, 11, 14, 10, 6, 6);
                    g.fillRoundRect(0, 14, 7, 7, 5, 5);
                    g.fillRoundRect(17, 14, 7, 7, 5, 5);
                } else if ("profile".equals(type)) {
                    g.fill(new Ellipse2D.Double(1, 1, 22, 22));
                    g.setColor(new Color(17, 83, 163));
                    g.fill(new Ellipse2D.Double(8, 5, 8, 8));
                    g.fillRoundRect(5, 14, 14, 7, 6, 6);
                    g.setColor(new Color(40, 194, 105));
                    g.fill(new Ellipse2D.Double(17, 17, 6, 6));
                } else {
                    g.fill(new Ellipse2D.Double(7.5, 2, 9, 9));
                    g.fillRoundRect(3, 13, 18, 9, 8, 8);
                    if ("addMember".equals(type)) {
                        g.setColor(new Color(255, 255, 255));
                        g.fillOval(16, 13, 8, 8);
                        g.setColor(color);
                        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.drawLine(20, 15, 20, 19);
                        g.drawLine(18, 17, 22, 17);
                    }
                }
            } else if ("home".equals(type)) {
                Path2D house = new Path2D.Double();
                house.moveTo(2, 11);
                house.lineTo(12, 2);
                house.lineTo(22, 11);
                house.lineTo(19, 11);
                house.lineTo(19, 21);
                house.lineTo(5, 21);
                house.lineTo(5, 11);
                house.closePath();
                g.draw(house);
                g.drawRoundRect(10, 13, 4, 8, 1, 1);
            } else if ("staff".equals(type)) {
                g.drawRoundRect(2, 7, 20, 14, 3, 3);
                g.drawRoundRect(8, 3, 8, 5, 1, 1);
                g.drawLine(2, 12, 22, 12);
                g.drawRoundRect(10, 11, 4, 3, 1, 1);
            } else if ("loan".equals(type)) {
                g.drawRoundRect(2, 8, 11, 10, 2, 2);
                g.drawOval(5, 10, 5, 5);
                g.drawLine(7, 11, 7, 14);
                g.drawLine(6, 12, 9, 12);
                g.draw(new Arc2D.Double(10, 7, 13, 13, 205, 145, Arc2D.OPEN));
                g.drawLine(11, 15, 14, 18);
                g.drawLine(14, 18, 18, 14);
            } else if ("reports".equals(type) || "chart".equals(type)) {
                g.fillRoundRect(3, 13, 4, 8, 1, 1);
                g.fillRoundRect(10, 8, 4, 13, 1, 1);
                g.fillRoundRect(17, 3, 4, 18, 1, 1);
            } else if ("chartCircle".equals(type)) {
                g.draw(new Ellipse2D.Double(2, 2, 20, 20));
                g.fillRoundRect(6, 13, 3, 5, 1, 1);
                g.fillRoundRect(11, 9, 3, 9, 1, 1);
                g.fillRoundRect(16, 5, 3, 13, 1, 1);
            } else if ("announcement".equals(type)) {
                Path2D megaphone = new Path2D.Double();
                megaphone.moveTo(3, 10);
                megaphone.lineTo(16, 6);
                megaphone.lineTo(16, 18);
                megaphone.lineTo(3, 14);
                megaphone.closePath();
                g.fill(megaphone);
                g.fillRoundRect(2, 10, 4, 5, 1, 1);
                g.drawLine(7, 15, 9, 21);
                g.drawLine(18, 9, 22, 7);
                g.drawLine(18, 12, 23, 12);
                g.drawLine(18, 15, 22, 17);
            } else if ("calendar".equals(type)) {
                g.drawRoundRect(3, 5, 18, 16, 2, 2);
                g.drawLine(3, 10, 21, 10);
                g.drawLine(7, 3, 7, 7);
                g.drawLine(17, 3, 17, 7);
                g.fill(new Ellipse2D.Double(7, 13, 2, 2));
                g.fill(new Ellipse2D.Double(11, 13, 2, 2));
                g.fill(new Ellipse2D.Double(15, 13, 2, 2));
            } else if ("clock".equals(type)) {
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                g.drawLine(12, 6, 12, 12);
                g.drawLine(12, 12, 16, 14);
            } else if ("settings".equals(type)) {
                for (int angle = 0; angle < 360; angle += 45) {
                    double radians = Math.toRadians(angle);
                    int x1 = 12 + (int) Math.round(Math.cos(radians) * 7);
                    int y1 = 12 + (int) Math.round(Math.sin(radians) * 7);
                    int x2 = 12 + (int) Math.round(Math.cos(radians) * 10);
                    int y2 = 12 + (int) Math.round(Math.sin(radians) * 10);
                    g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_SQUARE,
                            BasicStroke.JOIN_ROUND));
                    g.drawLine(x1, y1, x2, y2);
                }
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g.draw(new Ellipse2D.Double(4, 4, 16, 16));
                g.fill(new Ellipse2D.Double(9, 9, 6, 6));
            } else if ("savings".equals(type)) {
                g.drawRoundRect(2, 5, 20, 15, 3, 3);
                g.drawLine(4, 8, 19, 8);
                g.drawRoundRect(13, 10, 9, 7, 2, 2);
                g.fill(new Ellipse2D.Double(16, 12, 2, 2));
            } else if ("savingsTotal".equals(type)) {
                g.fill(new Ellipse2D.Double(3, 3, 18, 7));
                g.fillRoundRect(3, 6, 18, 5, 2, 2);
                g.fill(new Ellipse2D.Double(3, 9, 18, 7));
                g.fillRoundRect(3, 12, 18, 5, 2, 2);
                g.fill(new Ellipse2D.Double(3, 15, 18, 7));
                g.setColor(Color.WHITE);
                g.drawArc(3, 6, 18, 7, 180, 180);
                g.drawArc(3, 12, 18, 7, 180, 180);
            } else if ("money".equals(type)) {
                g.drawRoundRect(3, 7, 18, 12, 2, 2);
                g.drawLine(6, 7, 8, 4);
                g.drawLine(8, 4, 16, 4);
                g.drawLine(16, 4, 18, 7);
                g.drawOval(9, 10, 6, 6);
                g.drawLine(12, 11, 12, 15);
                g.drawLine(10, 13, 14, 13);
            } else if ("disburse".equals(type)) {
                g.draw(new Arc2D.Double(2, 12, 20, 9, 20, 140, Arc2D.OPEN));
                g.drawLine(5, 16, 19, 16);
                g.drawOval(8, 3, 8, 8);
                g.drawLine(12, 5, 12, 9);
                g.drawLine(10, 7, 14, 7);
                g.drawLine(12, 11, 12, 17);
                g.drawLine(9, 14, 12, 17);
                g.drawLine(15, 14, 12, 17);
            } else if ("withdraw".equals(type)) {
                g.drawRoundRect(3, 14, 18, 7, 2, 2);
                g.drawLine(12, 3, 12, 16);
                g.drawLine(8, 12, 12, 16);
                g.drawLine(16, 12, 12, 16);
                g.drawLine(6, 18, 9, 18);
                g.drawLine(14, 18, 18, 18);
            } else if ("installments".equals(type)) {
                g.drawRoundRect(3, 4, 18, 18, 3, 3);
                g.drawLine(3, 9, 21, 9);
                g.drawLine(7, 2, 7, 7);
                g.drawLine(17, 2, 17, 7);
                g.fill(new Ellipse2D.Double(7, 12, 2.5, 2.5));
                g.fill(new Ellipse2D.Double(14, 12, 2.5, 2.5));
                g.fill(new Ellipse2D.Double(7, 17, 2.5, 2.5));
                g.fill(new Ellipse2D.Double(14, 17, 2.5, 2.5));
            } else if ("history".equals(type)) {
                g.draw(new Arc2D.Double(4, 4, 17, 17, 35, 310, Arc2D.OPEN));
                g.drawLine(2, 8, 7, 8);
                g.drawLine(2, 8, 4, 4);
                g.drawLine(12, 8, 12, 13);
                g.drawLine(12, 13, 16, 15);
            } else if ("receipt".equals(type)) {
                g.drawRoundRect(3, 8, 18, 11, 2, 2);
                g.drawRoundRect(6, 3, 12, 6, 1, 1);
                g.drawLine(7, 12, 17, 12);
                g.drawLine(7, 16, 14, 16);
                g.drawLine(7, 19, 7, 22);
                g.drawLine(17, 19, 17, 22);
            } else if ("bell".equals(type)) {
                Path2D bell = new Path2D.Double();
                bell.moveTo(4, 17);
                bell.quadTo(7, 14, 7, 9);
                bell.quadTo(7, 3, 12, 3);
                bell.quadTo(17, 3, 17, 9);
                bell.quadTo(17, 14, 20, 17);
                bell.closePath();
                g.draw(bell);
                g.drawLine(4, 17, 20, 17);
                g.fill(new Ellipse2D.Double(10, 19, 4, 3));
            } else if ("logout".equals(type)) {
                g.drawRoundRect(3, 3, 9, 18, 1, 1);
                g.drawLine(8, 12, 22, 12);
                g.drawLine(17, 7, 22, 12);
                g.drawLine(17, 17, 22, 12);
            } else if ("location".equals(type)) {
                Path2D pin = new Path2D.Double();
                pin.moveTo(12, 22);
                pin.curveTo(10, 19, 4, 12, 4, 9);
                pin.curveTo(4, 4, 8, 2, 12, 2);
                pin.curveTo(16, 2, 20, 4, 20, 9);
                pin.curveTo(20, 12, 14, 19, 12, 22);
                pin.closePath();
                g.draw(pin);
                g.draw(new Ellipse2D.Double(9, 6, 6, 6));
            } else if ("phone".equals(type)) {
                Path2D handset = new Path2D.Double();
                handset.moveTo(7, 3);
                handset.curveTo(5, 3, 4, 5, 5, 8);
                handset.curveTo(7, 14, 10, 17, 16, 19);
                handset.curveTo(19, 20, 21, 18, 21, 16);
                handset.lineTo(17, 13);
                handset.lineTo(14, 15);
                handset.curveTo(11, 14, 9, 12, 8, 9);
                handset.lineTo(10, 6);
                handset.closePath();
                g.draw(handset);
            } else if ("email".equals(type)) {
                g.drawRoundRect(2, 5, 20, 14, 2, 2);
                g.drawLine(3, 6, 12, 13);
                g.drawLine(21, 6, 12, 13);
            }
            g.dispose();
        }
    }

    private boolean isAdmin() {
        return AccountStore.ADMIN.equals(currentAccount.role);
    }

    private boolean isStaff() {
        return AccountStore.STAFF.equals(currentAccount.role);
    }

    private boolean isMember() {
        return AccountStore.MEMBER.equals(currentAccount.role);
    }

    private static String money(long amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        format.setMaximumFractionDigits(0);
        return format.format(amount);
    }

    private static String formatDate(java.time.LocalDateTime date) {
        return date.format(DATE_FORMAT);
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static boolean isAllowedProofFile(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg")
                || name.endsWith(".jpeg") || name.endsWith(".pdf");
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
