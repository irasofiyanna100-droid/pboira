package koperasi.ui;

import koperasi.config.IdentitasKoperasi;
import koperasi.data.AccountStore;
import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;

public final class RegistrationWizard extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Color NAVY = new Color(11, 34, 105);
    private static final Color BLUE = new Color(5, 91, 225);
    private static final Color LINE = new Color(203, 218, 250);
    private static final Color MUTED = new Color(85, 105, 151);

    private final AccountStore store;
    private final Consumer<String> onRegistered;
    private final Runnable onCancel;
    private final JPanel cardHost = new JPanel(new java.awt.CardLayout());
    private JPanel termDetail;
    private JLabel termHeading;
    private JLabel termInfo;
    private JButton informationButton;
    private JButton savingsButton;
    private JButton loansButton;
    private JButton shuButton;
    private final JTextField name = input("Masukkan nama lengkap");
    private final JTextField phone = input("08xxxxxxxxxx");
    private final JTextField nik = input("Masukkan 16 digit NIK");
    private final JTextField username = input("Buat username, boleh pakai spasi");
    private final JTextField address = input("Masukkan alamat lengkap");
    private final JPasswordField password = passwordInput("Buat password");
    private final JPasswordField confirmation = passwordInput("Ulangi password");
    private final JLabel selectedFile = new JLabel("Klik untuk mengunggah bukti transfer");
    private final JCheckBox agreement = new JCheckBox(
            "Saya setuju dengan ketentuan koperasi.");
    private Dimension sizeBeforePayment;
    private Dimension minimumSizeBeforePayment;
    private File proof;
    private boolean completed;

    public RegistrationWizard(Frame owner, AccountStore store, Consumer<String> onRegistered,
            Runnable onCancel) {
        this.store = store;
        this.onRegistered = onRegistered;
        this.onCancel = onCancel;
        setTitle("Ketentuan Koperasi");
        Dimension screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds().getSize();
        int width = Math.min(1366, screen.width);
        int height = Math.min(768, screen.height);
        setSize(width, height);
        setMinimumSize(new Dimension(Math.min(980, width), Math.min(640, height)));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        cardHost.setBackground(Color.WHITE);
        cardHost.add(buildTermsPage(), "terms");
        cardHost.add(buildRegistrationPage(), "form");
        cardHost.add(buildPaymentPage(), "payment");
        setContentPane(cardHost);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                if (!completed) RegistrationWizard.this.onCancel.run();
            }
        });
    }

    private JPanel buildTermsPage() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(buildTermsSidebar(), BorderLayout.WEST);

        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBackground(new Color(248, 250, 255));
        page.setBorder(BorderFactory.createEmptyBorder(14, 17, 15, 18));
        page.add(new TermsBanner(), BorderLayout.NORTH);

        JPanel lower = new JPanel(new BorderLayout(0, 14));
        lower.setOpaque(false);
        JPanel title = new JPanel(new BorderLayout(12, 0));
        title.setOpaque(false);
        JPanel marker = new JPanel();
        marker.setBackground(BLUE);
        marker.setPreferredSize(new Dimension(9, 38));
        title.add(marker, BorderLayout.WEST);
        termHeading = new JLabel("Ketentuan Simpanan");
        termHeading.setFont(new Font("Segoe UI", Font.BOLD, 28));
        termHeading.setForeground(NAVY);
        title.add(termHeading, BorderLayout.CENTER);
        title.setPreferredSize(new Dimension(0, 42));
        lower.add(title, BorderLayout.NORTH);
        termDetail = new JPanel(new GridLayout(1, 3, 16, 0));
        termDetail.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                fitSavingAmounts();
            }
        });
        populateSavingCards();
        lower.add(termDetail, BorderLayout.CENTER);

        JPanel information = new TermsInfoPanel();
        information.setLayout(new BorderLayout(18, 0));
        information.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        information.add(new JLabel(new TermsIcon("info", BLUE, 40)), BorderLayout.WEST);
        JLabel info = new JLabel("<html><b>Informasi Penting</b><br>"
                + "<span style='font-size:15px'>Simpanan Pokok dan Simpanan Wajib tidak dapat ditarik.<br>"
                + "Pembayaran awal Rp70.000 mencakup simpanan pokok dan simpanan wajib bulan pertama."
                + "</span></html>");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        info.setForeground(NAVY);
        termInfo = info;
        information.add(info, BorderLayout.CENTER);
        agreement.setOpaque(false);
        agreement.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        agreement.setForeground(NAVY);
        agreement.setIcon(new TermsCheckboxIcon());
        agreement.setIconTextGap(12);
        JButton next = new TermsContinueButton("Lanjutkan  →");
        next.setPreferredSize(new Dimension(250, 58));
        next.setFont(new Font("Segoe UI", Font.BOLD, 19));
        next.setEnabled(false);
        agreement.addActionListener(event -> next.setEnabled(agreement.isSelected()));
        next.addActionListener(event -> showCard("form", "Pendaftaran Anggota"));
        JPanel continueRow = new JPanel(new BorderLayout(16, 0));
        continueRow.setOpaque(false);
        continueRow.setBorder(BorderFactory.createEmptyBorder(5, 10, 2, 0));
        continueRow.setPreferredSize(new Dimension(0, 68));
        continueRow.add(agreement, BorderLayout.CENTER);
        continueRow.add(next, BorderLayout.EAST);
        page.add(lower, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 14));
        bottom.setOpaque(false);
        bottom.add(information, BorderLayout.CENTER);
        bottom.add(continueRow, BorderLayout.SOUTH);
        page.add(bottom, BorderLayout.SOUTH);
        root.add(page, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildTermsSidebar() {
        JPanel sidebar = new TermsSidebarPanel();
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(225, 232, 247)),
                BorderFactory.createEmptyBorder(22, 14, 18, 12)));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel(new TermsIcon("brand", BLUE, 72));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(8));
        JLabel brand = new JLabel("<html><div style='text-align:center'><b>Koperasi</b><br>"
                + "Simpan Pinjam</div></html>");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 19));
        brand.setForeground(NAVY);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(5));
        JLabel slogan = new JLabel(IdentitasKoperasi.SLOGAN);
        slogan.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        slogan.setForeground(MUTED);
        slogan.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(slogan);
        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(sidebarDivider());
        sidebar.add(Box.createVerticalStrut(17));
        JLabel menuHeading = new JLabel("MENU KETENTUAN");
        menuHeading.setFont(new Font("Segoe UI", Font.BOLD, 11));
        menuHeading.setForeground(MUTED);
        menuHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(menuHeading);
        sidebar.add(Box.createVerticalStrut(10));
        savingsButton = sidebarButton("document", "Ketentuan Simpanan", "simpanan");
        loansButton = sidebarButton("card", "Ketentuan Pinjaman", "pinjaman");
        shuButton = sidebarButton("shield", "Pembagian SHU", "shu");
        informationButton = sidebarButton("info", "Informasi Koperasi", "informasi");
        selectTermsButton(savingsButton);
        sidebar.add(savingsButton);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(loansButton);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(shuButton);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(informationButton);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(sidebarDivider());
        sidebar.add(Box.createVerticalStrut(12));
        JLabel signature = new JLabel("Koperasi Untuk Kita  ♥");
        signature.setFont(new Font("Segoe UI", Font.BOLD, 13));
        signature.setForeground(NAVY);
        signature.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(signature);
        return sidebar;
    }

    private JButton sidebarButton(String icon, String label, String category) {
        JButton button = new JButton(label, new TermsIcon(icon, BLUE, 26));
        button.putClientProperty("icon", icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.putClientProperty("active", Boolean.FALSE);
        button.setIconTextGap(9);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(NAVY);
        button.setBackground(Color.WHITE);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 11, 10, 8));
        button.setUI(new TermsNavButtonUI());
        button.setPreferredSize(new Dimension(232, 56));
        button.setMaximumSize(new Dimension(232, 56));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setToolTipText(label);
        button.addActionListener(event -> {
            showTermsCategory(category);
            selectTermsButton(button);
        });
        return button;
    }

    private void selectTermsButton(JButton selected) {
        JButton[] buttons = {informationButton, savingsButton, loansButton, shuButton};
        for (JButton button : buttons) {
            boolean active = button == selected;
            button.putClientProperty("active", active);
            button.setBackground(active ? BLUE : Color.WHITE);
            button.setForeground(active ? Color.WHITE : NAVY);
            button.setBorder(BorderFactory.createEmptyBorder(10, 11, 10, 8));
            button.setIcon(new TermsIcon(
                    (String) button.getClientProperty("icon"),
                    active ? Color.WHITE : BLUE, 26));
            button.repaint();
        }
    }

    private JPanel sidebarDivider() {
        JPanel divider = new JPanel();
        divider.setBackground(new Color(207, 220, 244));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(232, 1));
        return divider;
    }

    private void showTermsCategory(String category) {
        termDetail.removeAll();
        if ("pinjaman".equals(category)) {
            termHeading.setText("Ketentuan Pinjaman");
            termDetail.add(infoCard("Pengajuan pinjaman",
                    "Isi nominal, tujuan, dan jangka waktu. Pengajuan diperiksa dan disetujui Admin."));
            termDetail.add(infoCard("Pencairan dan angsuran",
                    "Pinjaman yang disetujui dicairkan oleh Petugas dan dibayar sesuai jadwal angsuran."));
            termDetail.add(infoCard("Bunga dan denda",
                    "Bunga atau denda hanya berlaku jika ketentuannya ditetapkan oleh Admin."));
            termInfo.setText("<html><b>Informasi Pinjaman</b><br>"
                    + "<span style='font-size:15px'>Petugas tidak dapat menyetujui pengajuan pinjaman.</span></html>");
        } else if ("shu".equals(category)) {
            termHeading.setText("Pembagian Sisa Hasil Usaha");
            termDetail.add(infoCard("Pembagian SHU",
                    "Sisa Hasil Usaha dibagikan sesuai keputusan Rapat Anggota Tahunan."));
            termDetail.add(infoCard("Perhitungan",
                    "Porsi jasa usaha dan jasa modal mengikuti ketentuan koperasi yang berlaku."));
            termDetail.add(infoCard("Transparansi",
                    "Laporan pembagian SHU disampaikan kepada anggota secara terbuka."));
            termInfo.setText("<html><b>Informasi SHU</b><br>"
                    + "<span style='font-size:15px'>Ketentuan rinci diumumkan melalui koperasi.</span></html>");
        } else if ("informasi".equals(category)) {
            termHeading.setText("Informasi Koperasi");
            termDetail.add(infoCard(IdentitasKoperasi.NAMA,
                    "\"" + IdentitasKoperasi.SLOGAN
                            + "\"<br>Layanan simpanan dan pinjaman untuk anggota."));
            termDetail.add(infoCard("Keanggotaan",
                    "Pendaftaran diverifikasi Admin. Akun aktif setelah data dan pembayaran awal disetujui."));
            termDetail.add(infoCard("Keamanan",
                    "Data anggota digunakan untuk pelayanan dan administrasi koperasi."));
            termInfo.setText("<html><b>Informasi Penting</b><br>"
                    + "<span style='font-size:15px'>Baca ketentuan simpanan dan pinjaman sebelum mendaftar.</span></html>");
        } else {
            termHeading.setText("Ketentuan Simpanan");
            populateSavingCards();
            termInfo.setText("<html><b>Informasi Penting</b><br>"
                    + "<span style='font-size:15px'>Simpanan Pokok dan Simpanan Wajib tidak dapat ditarik.<br>"
                    + "Pembayaran awal Rp70.000 mencakup simpanan pokok dan simpanan wajib bulan pertama."
                    + "</span></html>");
        }
        termDetail.revalidate();
        termDetail.repaint();
        fitSavingAmounts();
    }

    private JPanel infoCard(String title, String detail) {
        JPanel card = new TermsCardPanel();
        card.setLayout(new BorderLayout(0, 7));
        JLabel heading = centered(title, 20, Font.BOLD);
        JLabel text = new JLabel("<html><div style='text-align:center'>" + detail + "</div></html>",
                SwingConstants.CENTER);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        text.setForeground(MUTED);
        text.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.add(heading, BorderLayout.NORTH);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private void populateSavingCards() {
        termDetail.add(savingCard("coins", "Simpanan Pokok", "Rp50.000",
                "Dibayarkan satu kali", new Color(255, 174, 0)));
        termDetail.add(savingCard("calendar", "Simpanan Wajib", "Rp20.000 / bulan",
                "Dibayarkan setiap bulan", BLUE));
        termDetail.add(savingCard("heart", "Simpanan Sukarela", "Bebas",
                "Dapat disetor dan ditarik", new Color(244, 71, 109)));
    }

    private JPanel savingCard(String icon, String title, String value, String detail, Color iconColor) {
        JPanel card = new TermsCardPanel();
        JLabel image = new JLabel(new TermsIcon(icon, iconColor, 46));
        image.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel heading = centered(title, 19, Font.BOLD);
        JLabel amount = centered(value, 25, Font.BOLD);
        amount.putClientProperty("savingAmount", Boolean.TRUE);
        amount.setForeground(NAVY);
        JLabel note = centered("<html><div style='text-align:center'>" +
                detail.replace("\n", "<br>") + "</div></html>", 14, Font.PLAIN);
        image.setMaximumSize(new Dimension(Integer.MAX_VALUE, image.getPreferredSize().height));
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, heading.getPreferredSize().height));
        amount.setMaximumSize(new Dimension(Integer.MAX_VALUE, amount.getPreferredSize().height));
        note.setMaximumSize(new Dimension(Integer.MAX_VALUE, note.getPreferredSize().height));
        card.add(image);
        card.add(Box.createVerticalStrut(5));
        card.add(heading);
        card.add(Box.createVerticalStrut(5));
        card.add(amount);
        card.add(Box.createVerticalStrut(5));
        card.add(note);
        return card;
    }

    private void fitSavingAmounts() {
        for (Component component : termDetail.getComponents()) {
            if (!(component instanceof JPanel)) continue;
            int maxWidth = Math.max(130, component.getWidth() - 42);
            for (Component child : ((JPanel) component).getComponents()) {
                if (!(child instanceof JLabel)
                        || !Boolean.TRUE.equals(((JLabel) child).getClientProperty("savingAmount"))) {
                    continue;
                }
                JLabel amount = (JLabel) child;
                int size = 25;
                Font font = new Font("Segoe UI", Font.BOLD, size);
                while (size > 18
                        && amount.getFontMetrics(font).stringWidth(amount.getText()) > maxWidth) {
                    font = new Font("Segoe UI", Font.BOLD, --size);
                }
                amount.setFont(font);
            }
        }
    }

    private JPanel buildRegistrationPage() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(buildStepper(1), BorderLayout.WEST);

        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setBackground(new Color(248, 250, 255));
        page.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));

        JPanel heading = new JPanel(new BorderLayout(8, 0));
        heading.setOpaque(false);
        JLabel icon = new JLabel(new TermsIcon("member", BLUE, 36));
        JLabel title = new JLabel("<html><b>Form Pendaftaran Anggota</b><br>"
                + "<span style='font-size:14px;font-weight:normal'>Lengkapi data diri Anda dengan benar</span></html>");
        title.setFont(new Font("Segoe UI", Font.BOLD, 23));
        title.setForeground(NAVY);
        heading.add(icon, BorderLayout.WEST);
        heading.add(title, BorderLayout.CENTER);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 4, 8, 4));
        page.add(heading, BorderLayout.NORTH);
        page.add(buildMemberForm(), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JButton back = outlineButton("←  Kembali");
        JButton next = primaryButton("Lanjutkan  →");
        back.setFont(new Font("Segoe UI", Font.BOLD, 14));
        back.setPreferredSize(new Dimension(160, 46));
        next.setFont(new Font("Segoe UI", Font.BOLD, 14));
        next.setPreferredSize(new Dimension(180, 46));
        back.addActionListener(event -> showCard("terms", "Ketentuan Koperasi"));
        next.addActionListener(event -> continueToPayment());
        footer.add(back, BorderLayout.WEST);
        footer.add(next, BorderLayout.EAST);
        page.add(footer, BorderLayout.SOUTH);
        root.add(page, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildMemberForm() {
        JPanel form = new TermsCardPanel();
        form.setLayout(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        addFormCell(form, "Nama Lengkap *", name, 0, 0);
        addFormCell(form, "Username *", username, 1, 0);
        addFormCell(form, "Nomor Telepon *", phone, 0, 1);
        addFormCell(form, "NIK *", nik, 1, 1);
        addFormCell(form, "Alamat *", address, 0, 2);
        addFormCell(form, "Password *", password, 1, 2);
        addFormCell(form, "", new JLabel(""), 0, 3);
        addFormCell(form, "Konfirmasi Password *", confirmation, 1, 3);
        return form;
    }

    private void addFormCell(JPanel form, String label, Component field, int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(8, column == 0 ? 0 : 12, 8, column == 0 ? 12 : 0);
        JPanel cell = new JPanel(new BorderLayout(0, 7));
        cell.setOpaque(false);
        if (!label.isEmpty()) {
            JLabel caption = new JLabel("<html>" + label.replace("*",
                    "<font color='#e53935'>*</font>") + "</html>");
            caption.setFont(new Font("Segoe UI", Font.BOLD, 14));
            caption.setForeground(NAVY);
            cell.add(caption, BorderLayout.NORTH);
        }
        if (field instanceof JTextField) {
            JTextField textField = (JTextField) field;
            textField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            if (field instanceof JPasswordField) {
                cell.add(passwordWrapper((JPasswordField) field), BorderLayout.CENTER);
                form.add(cell, constraints);
                return;
            }
            textField.setPreferredSize(new Dimension(280, 46));
        }
        cell.add(field, BorderLayout.CENTER);
        form.add(cell, constraints);
    }

    private JPanel passwordWrapper(JPasswordField field) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(inputBorder());
        wrapper.setPreferredSize(new Dimension(280, 46));
        field.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 2));
        field.setBackground(Color.WHITE);
        JButton reveal = new JButton("◉");
        reveal.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 15));
        reveal.setForeground(MUTED);
        reveal.setToolTipText("Tampilkan atau sembunyikan password");
        reveal.setContentAreaFilled(false);
        reveal.setFocusPainted(false);
        reveal.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 10));
        reveal.addActionListener(event ->
                field.setEchoChar(field.getEchoChar() == 0 ? '•' : (char) 0));
        wrapper.add(field, BorderLayout.CENTER);
        wrapper.add(reveal, BorderLayout.EAST);
        return wrapper;
    }

    private JPanel buildPaymentPage() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        PaymentArtwork artwork = new PaymentArtwork();
        artwork.setPreferredSize(new Dimension(158, 0));
        root.add(artwork, BorderLayout.WEST);

        JPanel page = new JPanel(new BorderLayout(0, 7));
        page.setBackground(new Color(248, 250, 255));
        page.setBorder(BorderFactory.createEmptyBorder(10, 7, 9, 10));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(paymentDetails());
        content.add(Box.createVerticalStrut(10));
        JLabel uploadTitle = new JLabel("Unggah Bukti Transfer");
        uploadTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        uploadTitle.setForeground(NAVY);
        content.add(uploadTitle);
        content.add(Box.createVerticalStrut(7));
        content.add(uploadPanel());
        page.add(content, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JButton back = outlineButton("←  Kembali");
        JButton submit = primaryButton("➤  Kirim Pendaftaran");
        back.addActionListener(event -> showCard("form", "Pendaftaran Anggota"));
        submit.addActionListener(event -> submitRegistration());
        footer.add(back, BorderLayout.WEST);
        footer.add(submit, BorderLayout.EAST);
        page.add(footer, BorderLayout.SOUTH);
        root.add(page, BorderLayout.CENTER);
        return root;
    }

    private JPanel paymentDetails() {
        JPanel details = new JPanel(new BorderLayout(0, 4));
        details.setBackground(Color.WHITE);
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JLabel title = new JLabel("▰  Rincian Pembayaran");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(NAVY);
        details.add(title, BorderLayout.NORTH);
        JPanel rows = new JPanel(new GridLayout(3, 1, 0, 2));
        rows.setOpaque(false);
        rows.add(paymentRow("Simpanan Pokok", "Rp50.000", false));
        rows.add(paymentRow("Simpanan Wajib (Bulan Pertama)", "Rp20.000", false));
        rows.add(paymentRow("Total", "Rp70.000", true));
        details.add(rows, BorderLayout.CENTER);
        return details;
    }

    private JPanel paymentRow(String label, String amount, boolean total) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(total ? new Color(226, 234, 255) : Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 2));
        JLabel name = new JLabel(label);
        JLabel value = new JLabel(":  " + amount);
        name.setFont(new Font("Segoe UI", total ? Font.BOLD : Font.PLAIN, total ? 16 : 14));
        value.setFont(new Font("Segoe UI", Font.BOLD, total ? 17 : 14));
        value.setForeground(NAVY);
        row.add(name, BorderLayout.CENTER);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private JPanel uploadPanel() {
        JPanel upload = new JPanel(new BorderLayout(0, 2));
        upload.setBackground(new Color(243, 247, 255));
        upload.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(new Color(116, 159, 255), 1.2f, 4, 3, true),
                BorderFactory.createEmptyBorder(7, 5, 5, 5)));
        upload.setPreferredSize(new Dimension(300, 90));
        upload.setMaximumSize(new Dimension(420, 90));
        JButton choose = new JButton("☁");
        choose.setFont(new Font("Segoe UI Symbol", Font.BOLD, 28));
        choose.setForeground(BLUE);
        choose.setContentAreaFilled(false);
        choose.setBorder(BorderFactory.createEmptyBorder());
        choose.addActionListener(event -> chooseProof());
        choose.setToolTipText("Pilih file bukti pembayaran");
        selectedFile.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        selectedFile.setHorizontalAlignment(SwingConstants.CENTER);
        selectedFile.setForeground(NAVY);
        JLabel formats = new JLabel("<html><div style='text-align:center'>Format: JPG, PNG, atau PDF (Maks. 2 MB)</div></html>");
        formats.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formats.setForeground(MUTED);
        formats.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        text.add(selectedFile);
        text.add(formats);
        upload.add(choose, BorderLayout.NORTH);
        upload.add(text, BorderLayout.CENTER);
        upload.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                chooseProof();
            }
        });
        selectedFile.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                chooseProof();
            }
        });
        formats.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                chooseProof();
            }
        });
        return upload;
    }

    private JPanel buildStepper(int activeStep) {
        JPanel stepper = new StepperPanel();
        stepper.setBackground(new Color(246, 249, 255));
        stepper.setPreferredSize(new Dimension(198, 0));
        stepper.setBorder(BorderFactory.createEmptyBorder(34, 20, 20, 14));
        stepper.add(stepItem(1, "Isi Data Diri", activeStep));
        stepper.add(Box.createVerticalStrut(20));
        stepper.add(stepItem(2, "Unggah Bukti\nTransfer", activeStep));
        stepper.add(Box.createVerticalStrut(20));
        stepper.add(stepItem(3, "Menunggu\nVerifikasi", activeStep));
        stepper.add(Box.createVerticalGlue());
        JPanel help = new JPanel(new BorderLayout(3, 0));
        help.setBackground(new Color(220, 232, 255));
        help.setBorder(BorderFactory.createEmptyBorder(10, 9, 10, 8));
        JLabel headset = new JLabel("◉");
        headset.setForeground(BLUE);
        headset.setFont(new Font("Segoe UI", Font.BOLD, 22));
        JLabel info = new JLabel("<html><b>Butuh bantuan?</b><br>"
                + "<span style='font-size:11px'>Hubungi petugas koperasi<br>pada jam kerja.</span></html>");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setForeground(NAVY);
        help.add(headset, BorderLayout.WEST);
        help.add(info, BorderLayout.CENTER);
        stepper.add(help);
        return stepper;
    }

    private JPanel stepItem(int number, String text, int activeStep) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        JLabel circle = new StepCircle(
                number < activeStep ? "✓" : Integer.toString(number), number <= activeStep);
        circle.setPreferredSize(new Dimension(34, 34));
        JLabel caption = new JLabel("<html><b>" + number + ". "
                + text.replace("\n", "<br>")
                + "</b></html>");
        caption.setFont(new Font("Segoe UI", Font.BOLD, 13));
        caption.setForeground(number <= activeStep ? BLUE : MUTED);
        GridBagConstraints circleConstraints = new GridBagConstraints();
        circleConstraints.gridx = 0;
        circleConstraints.gridy = 0;
        circleConstraints.anchor = GridBagConstraints.NORTHWEST;
        circleConstraints.insets = new Insets(0, 0, 0, 10);
        row.add(circle, circleConstraints);
        GridBagConstraints captionConstraints = new GridBagConstraints();
        captionConstraints.gridx = 1;
        captionConstraints.gridy = 0;
        captionConstraints.weightx = 1;
        captionConstraints.anchor = GridBagConstraints.NORTHWEST;
        captionConstraints.fill = GridBagConstraints.HORIZONTAL;
        row.add(caption, captionConstraints);
        return row;
    }

    private void continueToPayment() {
        String cleanName = name.getText().trim();
        String cleanPhone = phone.getText().trim();
        String cleanNik = nik.getText().trim();
        String cleanUsername = username.getText().trim();
        String cleanAddress = address.getText().trim();
        char[] secret = password.getPassword();
        char[] repeated = confirmation.getPassword();
        try {
            if (cleanName.isEmpty() || cleanPhone.isEmpty() || cleanNik.isEmpty()
                    || cleanUsername.isEmpty() || cleanAddress.isEmpty()
                    || secret.length == 0 || repeated.length == 0) {
                warn("Semua data bertanda * wajib diisi.");
                return;
            }
            if (!cleanPhone.matches("[0-9+() -]{9,18}")
                    || cleanPhone.replaceAll("\\D", "").length() < 9) {
                warn("Nomor telepon harus berisi minimal 9 digit.");
                return;
            }
            if (!cleanNik.matches("[0-9]{16}")) {
                warn("NIK harus terdiri dari 16 digit angka.");
                return;
            }
            if (!cleanUsername.matches("[A-Za-z0-9._ -]{3,30}")) {
                warn("Username harus 3–30 karakter dan hanya berisi huruf, angka, spasi, titik, garis bawah, atau tanda hubung.");
                return;
            }
            if (secret.length < 8) {
                warn("Password minimal terdiri dari 8 karakter.");
                return;
            }
            if (!Arrays.equals(secret, repeated)) {
                warn("Password dan konfirmasi password tidak sama.");
                return;
            }
            showCard("payment", "Pembayaran Pendaftaran");
        } finally {
            Arrays.fill(secret, '\0');
            Arrays.fill(repeated, '\0');
        }
    }

    private void chooseProof() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih Bukti Pembayaran Rp70.000");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                "Bukti pembayaran (JPG, PNG, PDF)", "jpg", "jpeg", "png", "pdf"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File selected = chooser.getSelectedFile();
        if (!isAllowedProof(selected)) {
            warn("Pilih file berformat JPG, PNG, atau PDF.");
            return;
        }
        if (selected.length() > 2L * 1024 * 1024) {
            warn("Ukuran bukti pembayaran maksimal 2 MB.");
            return;
        }
        proof = selected;
        selectedFile.setText("<html><div style='text-align:center'>✓  "
                + selected.getName() + "</div></html>");
    }

    private void submitRegistration() {
        if (proof == null || !proof.isFile()) {
            warn("Unggah bukti transfer terlebih dahulu.");
            return;
        }
        int confirmationResult = JOptionPane.showConfirmDialog(this,
                "Kirim pendaftaran dengan bukti pembayaran Rp70.000?",
                "Konfirmasi Pendaftaran", JOptionPane.YES_NO_OPTION);
        if (confirmationResult != JOptionPane.YES_OPTION) return;

        Path savedProof = null;
        char[] secret = password.getPassword();
        try {
            Path folder = Paths.get("data", "bukti-pembayaran");
            Files.createDirectories(folder);
            String filename = proof.getName();
            String extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
            savedProof = folder.resolve(UUID.randomUUID().toString() + extension);
            Files.copy(proof.toPath(), savedProof);
            store.registerMember(name.getText(), username.getText(), secret,
                    savedProof.toString(), phone.getText(), nik.getText(), address.getText());
            completed = true;
            JOptionPane.showMessageDialog(this,
                    "Pendaftaran berhasil dikirim.\nStatus: Menunggu Verifikasi Admin.\n"
                            + "Login dapat dilakukan setelah pendaftaran disetujui.",
                    "Pendaftaran Terkirim", JOptionPane.INFORMATION_MESSAGE);
            String registeredUsername = username.getText().trim();
            dispose();
            onRegistered.accept(registeredUsername);
        } catch (IllegalArgumentException | IOException | GeneralSecurityException e) {
            if (savedProof != null) {
                try {
                    Files.deleteIfExists(savedProof);
                } catch (IOException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }
            JOptionPane.showMessageDialog(this,
                    "Pendaftaran tidak dapat disimpan:\n" + e.getMessage(),
                    "Kesalahan", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    private void showCard(String card, String title) {
        if ("payment".equals(card) && sizeBeforePayment == null) {
            sizeBeforePayment = getSize();
            minimumSizeBeforePayment = getMinimumSize();
            Dimension screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getMaximumWindowBounds().getSize();
            int width = Math.min(920, screen.width);
            int height = Math.min(620, screen.height);
            setMinimumSize(new Dimension(Math.min(820, width), Math.min(540, height)));
            setSize(width, height);
            setLocationRelativeTo(getOwner());
        } else if (!"payment".equals(card) && sizeBeforePayment != null) {
            setMinimumSize(minimumSizeBeforePayment);
            setSize(sizeBeforePayment);
            sizeBeforePayment = null;
            minimumSizeBeforePayment = null;
            setLocationRelativeTo(getOwner());
        }
        setTitle(title);
        ((java.awt.CardLayout) cardHost.getLayout()).show(cardHost, card);
        cardHost.revalidate();
        cardHost.repaint();
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Data belum valid",
                JOptionPane.WARNING_MESSAGE);
    }

    private JLabel centered(String text, int size, int style) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(NAVY);
        return label;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 10));
        button.setForeground(Color.WHITE);
        button.setBackground(BLUE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }

    private JButton outlineButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 10));
        button.setForeground(BLUE);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(101, 151, 255)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return button;
    }

    private static JTextField input(String hint) {
        JTextField field = new PlaceholderTextField(hint);
        field.setToolTipText(hint);
        field.setBorder(inputBorder());
        field.setBackground(Color.WHITE);
        return field;
    }

    private static JPasswordField passwordInput(String hint) {
        JPasswordField field = new PlaceholderPasswordField(hint);
        field.setToolTipText(hint);
        field.setEchoChar('•');
        field.setBorder(inputBorder());
        field.setBackground(Color.WHITE);
        return field;
    }

    private static Border inputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(204, 217, 245), 1, true),
                BorderFactory.createEmptyBorder(4, 9, 4, 7));
    }

    private static final class PlaceholderTextField extends JTextField {
        private static final long serialVersionUID = 1L;
        private final String hint;

        PlaceholderTextField(String hint) {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setColor(new Color(115, 128, 164));
                g.setFont(getFont());
                java.awt.FontMetrics metrics = g.getFontMetrics();
                g.drawString(hint, getInsets().left + 2,
                        (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
                g.dispose();
            }
        }
    }

    private static final class PlaceholderPasswordField extends JPasswordField {
        private static final long serialVersionUID = 1L;
        private final String hint;

        PlaceholderPasswordField(String hint) {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getPassword().length == 0 && !isFocusOwner()) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setColor(new Color(115, 128, 164));
                g.setFont(getFont());
                java.awt.FontMetrics metrics = g.getFontMetrics();
                g.drawString(hint, getInsets().left + 2,
                        (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
                g.dispose();
            }
        }
    }

    private static final class StepCircle extends JLabel {
        private static final long serialVersionUID = 1L;
        private final String label;
        private final boolean active;

        StepCircle(String label, boolean active) {
            this.label = label;
            this.active = active;
            setPreferredSize(new Dimension(28, 28));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(active ? BLUE : Color.WHITE);
            g.fillOval(1, 1, getWidth() - 3, getHeight() - 3);
            g.setColor(active ? BLUE : new Color(202, 213, 237));
            g.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g.setColor(active ? Color.WHITE : MUTED);
            java.awt.FontMetrics metrics = g.getFontMetrics();
            g.drawString(label, (getWidth() - metrics.stringWidth(label)) / 2,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }

    private static final class StepperPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        StepperPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }
    }

    private static boolean isAllowedProof(File file) {
        String filename = file.getName().toLowerCase(java.util.Locale.ROOT);
        return filename.endsWith(".jpg") || filename.endsWith(".jpeg")
                || filename.endsWith(".png") || filename.endsWith(".pdf");
    }

    private static final class TermsNavButtonUI extends BasicButtonUI {
        @Override
        public void paint(Graphics graphics, JComponent component) {
            JButton button = (JButton) component;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = component.getWidth();
            int height = component.getHeight();
            boolean active = Boolean.TRUE.equals(button.getClientProperty("active"));
            boolean hovered = button.getModel().isRollover();
            if (active) {
                g.setColor(new Color(25, 72, 150, 34));
                g.fillRoundRect(3, 4, width - 6, height - 5, 18, 18);
                g.setPaint(new java.awt.GradientPaint(2, 1, new Color(34, 112, 226),
                        width - 2, height - 1, new Color(5, 82, 205)));
                g.fillRoundRect(1, 1, width - 5, height - 7, 18, 18);
            } else if (hovered) {
                g.setColor(new Color(226, 238, 255));
                g.fillRoundRect(1, 1, width - 5, height - 7, 18, 18);
            }
            if (component.hasFocus()) {
                g.setColor(new Color(80, 145, 235, 190));
                g.setStroke(new BasicStroke(1.3f));
                g.drawRoundRect(1, 1, width - 6, height - 8, 18, 18);
            }
            g.dispose();
            super.paint(graphics, component);
        }
    }

    private static final class TermsSidebarPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        TermsSidebarPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new java.awt.GradientPaint(0, 0, Color.WHITE,
                    getWidth(), getHeight(), new Color(241, 247, 255)));
            g.fillRect(0, 0, getWidth(), getHeight());
            Path2D wave = new Path2D.Double();
            wave.moveTo(0, getHeight() - 112);
            wave.curveTo(getWidth() * 0.3, getHeight() - 155,
                    getWidth() * 0.65, getHeight() - 45, getWidth(), getHeight() - 96);
            wave.lineTo(getWidth(), getHeight());
            wave.lineTo(0, getHeight());
            wave.closePath();
            g.setColor(new Color(174, 207, 255, 85));
            g.fill(wave);
            g.dispose();
        }
    }

    private static final class TermsCardPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        TermsCardPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g.setColor(new Color(28, 62, 126, 20));
            g.fillRoundRect(2, 4, width - 5, height - 6, 20, 20);
            g.setColor(Color.WHITE);
            g.fillRoundRect(1, 1, width - 4, height - 7, 20, 20);
            g.setColor(LINE);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(1, 1, width - 4, height - 7, 20, 20);
            g.dispose();
        }
    }

    private static final class TermsInfoPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        TermsInfoPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 118));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g.setColor(new Color(28, 62, 126, 18));
            g.fillRoundRect(2, 4, width - 5, height - 6, 20, 20);
            g.setClip(new java.awt.geom.RoundRectangle2D.Double(
                    0, 0, width - 1, height - 2, 20, 20));
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(219, 234, 255),
                    width, height, new Color(191, 217, 255)));
            g.fillRect(0, 0, width, height);
            Path2D wave = new Path2D.Double();
            wave.moveTo(width * 0.62, height);
            wave.curveTo(width * 0.76, height * 0.80,
                    width * 0.85, height * 0.40,
                    width, height * 0.34);
            wave.lineTo(width, height);
            wave.closePath();
            g.setColor(new Color(120, 173, 246, 60));
            g.fill(wave);
            g.dispose();
        }
    }

    private static final class TermsIcon implements javax.swing.Icon {
        private final String type;
        private final Color color;
        private final int size;

        TermsIcon(String type, Color color, int size) {
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
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.scale(size / 32.0, size / 32.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            if ("brand".equals(type)) {
                g.setColor(new Color(29, 71, 139, 34));
                g.fill(new Ellipse2D.Double(2, 3, 29, 29));
                g.setPaint(new java.awt.GradientPaint(4, 2, new Color(69, 150, 245),
                        27, 30, new Color(8, 77, 188)));
                g.fill(new Ellipse2D.Double(1, 1, 29, 29));
                g.setColor(new Color(255, 255, 255, 120));
                g.setStroke(new BasicStroke(0.8f));
                g.draw(new Ellipse2D.Double(2.5, 2.5, 26, 26));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(6, 8, 5, 5));
                g.fill(new Ellipse2D.Double(13.5, 5, 5.5, 5.5));
                g.fill(new Ellipse2D.Double(21, 8, 5, 5));
                g.fillRoundRect(4, 15, 10, 9, 5, 5);
                g.fillRoundRect(10, 13, 12, 12, 6, 6);
                g.fillRoundRect(19, 15, 10, 9, 5, 5);
                g.setColor(new Color(255, 207, 92));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(8, 19, 16, 8, 200, 140);
            } else if ("member".equals(type)) {
                g.setColor(new Color(225, 238, 255));
                g.fill(new Ellipse2D.Double(1, 1, 30, 30));
                g.setColor(color);
                g.fill(new Ellipse2D.Double(11, 6, 10, 10));
                g.fillRoundRect(6, 18, 20, 10, 9, 9);
            } else if ("document".equals(type)) {
                Path2D page = new Path2D.Double();
                page.moveTo(7, 3);
                page.lineTo(20, 3);
                page.lineTo(26, 9);
                page.lineTo(26, 28);
                page.lineTo(7, 28);
                page.closePath();
                g.draw(page);
                g.drawLine(20, 3, 20, 9);
                g.drawLine(20, 9, 26, 9);
                g.fillRoundRect(11, 13, 3, 3, 1, 1);
                g.fillRoundRect(11, 19, 3, 3, 1, 1);
                g.drawLine(17, 14, 23, 14);
                g.drawLine(17, 20, 23, 20);
            } else if ("coins".equals(type)) {
                for (int i = 0; i < 4; i++) {
                    int top = 5 + i * 6;
                    g.setColor(color);
                    g.fill(new Ellipse2D.Double(5, top, 22, 8));
                    g.setColor(new Color(255, 255, 255, 210));
                    g.draw(new Arc2D.Double(5, top, 22, 8, 180, 180, Arc2D.OPEN));
                }
            } else if ("calendar".equals(type)) {
                g.drawRoundRect(4, 5, 24, 23, 3, 3);
                g.drawLine(4, 12, 28, 12);
                g.drawLine(10, 2, 10, 8);
                g.drawLine(22, 2, 22, 8);
                for (int row = 0; row < 2; row++) {
                    for (int column = 0; column < 3; column++) {
                        g.fillRoundRect(9 + column * 6, 16 + row * 6, 3, 3, 1, 1);
                    }
                }
            } else if ("heart".equals(type)) {
                Path2D heart = new Path2D.Double();
                heart.moveTo(16, 28);
                heart.curveTo(13, 25, 3, 17, 3, 10);
                heart.curveTo(3, 2, 13, 1, 16, 8);
                heart.curveTo(20, 1, 29, 2, 29, 10);
                heart.curveTo(29, 17, 20, 25, 16, 28);
                heart.closePath();
                g.draw(heart);
            } else if ("card".equals(type)) {
                g.drawRoundRect(3, 6, 26, 20, 3, 3);
                g.fillRect(4, 12, 24, 4);
                g.drawRoundRect(7, 20, 7, 2, 1, 1);
            } else if ("shield".equals(type)) {
                Path2D shield = new Path2D.Double();
                shield.moveTo(16, 2);
                shield.lineTo(28, 7);
                shield.lineTo(26, 19);
                shield.quadTo(23, 26, 16, 30);
                shield.quadTo(9, 26, 6, 19);
                shield.lineTo(4, 7);
                shield.closePath();
                g.fill(shield);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(10, 15, 14, 19);
                g.drawLine(14, 19, 22, 11);
            } else if ("info".equals(type)) {
                g.fill(new Ellipse2D.Double(2, 2, 28, 28));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(14, 7, 4, 4));
                g.fillRoundRect(13, 13, 6, 12, 3, 3);
            }
            g.dispose();
        }
    }

    private static final class TermsCheckboxIcon implements javax.swing.Icon {
        @Override
        public int getIconWidth() {
            return 34;
        }

        @Override
        public int getIconHeight() {
            return 34;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            JCheckBox checkbox = (JCheckBox) component;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(checkbox.isSelected() ? BLUE : Color.WHITE);
            g.fillRoundRect(x + 2, y + 2, 30, 30, 7, 7);
            g.setColor(checkbox.isSelected() ? BLUE : new Color(159, 186, 239));
            g.setStroke(new BasicStroke(2.2f));
            g.drawRoundRect(x + 2, y + 2, 30, 30, 7, 7);
            if (checkbox.isSelected()) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(x + 9, y + 17, x + 14, y + 22);
                g.drawLine(x + 14, y + 22, x + 25, y + 11);
            }
            g.dispose();
        }
    }

    private static final class TermsContinueButton extends JButton {
        private static final long serialVersionUID = 1L;

        TermsContinueButton(String text) {
            super(text);
            setForeground(Color.WHITE);
            setBackground(BLUE);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(isEnabled() ? BLUE : new Color(152, 177, 220));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class TermsBanner extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final String SLOGAN = "Bersama Membangun Kesejahteraan Bersama";

        TermsBanner() {
            setPreferredSize(new Dimension(0, 154));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g.setColor(new Color(30, 82, 155, 22));
            g.fillRoundRect(2, 4, width - 5, height - 6, 22, 22);
            g.setClip(new java.awt.geom.RoundRectangle2D.Double(
                    0, 0, width - 1, height - 2, 22, 22));
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(226, 240, 255),
                    width, height, new Color(164, 207, 255)));
            g.fillRect(0, 0, width, height);

            Path2D upperWave = new Path2D.Double();
            upperWave.moveTo(0, height * 0.55);
            upperWave.curveTo(width * 0.18, height * 0.04, width * 0.42, height * 0.12,
                    width * 0.58, height * 0.52);
            upperWave.curveTo(width * 0.74, height * 0.91, width * 0.87, height * 0.27,
                    width, height * 0.30);
            upperWave.lineTo(width, 0);
            upperWave.lineTo(0, 0);
            upperWave.closePath();
            g.setColor(new Color(255, 255, 255, 95));
            g.fill(upperWave);

            Path2D wave = new Path2D.Double();
            wave.moveTo(0, height * 0.79);
            wave.curveTo(width * 0.20, height * 0.56, width * 0.38, height * 0.94,
                    width * 0.57, height * 0.75);
            wave.curveTo(width * 0.74, height * 0.57, width * 0.87, height * 0.94,
                    width, height * 0.62);
            wave.lineTo(width, height);
            wave.lineTo(0, height);
            wave.closePath();
            g.setPaint(new java.awt.GradientPaint(0f, height * 0.68f,
                    new Color(69, 150, 241, 100), width, height,
                    new Color(14, 111, 226, 155)));
            g.fill(wave);

            Path2D accentWave = new Path2D.Double();
            accentWave.moveTo(width * 0.44, height);
            accentWave.curveTo(width * 0.62, height * 0.78,
                    width * 0.80, height * 0.70, width, height * 0.50);
            accentWave.lineTo(width, height);
            accentWave.closePath();
            g.setColor(new Color(44, 126, 226, 100));
            g.fill(accentWave);

            int horizontalPadding = Math.max(32, width / 28);
            int fontSize = 34;
            Font sloganFont = new Font("Segoe UI", Font.BOLD, fontSize);
            while (fontSize > 20
                    && g.getFontMetrics(sloganFont).stringWidth(SLOGAN)
                            > width - horizontalPadding * 2) {
                sloganFont = new Font("Segoe UI", Font.BOLD, --fontSize);
            }
            g.setFont(sloganFont);
            g.setColor(NAVY);
            FontMetrics metrics = g.getFontMetrics();
            int x = Math.max(horizontalPadding, (width - metrics.stringWidth(SLOGAN)) / 2);
            int baseline = (height - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(SLOGAN, x, baseline);

            g.setColor(new Color(92, 149, 219, 120));
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(0, 0, width - 1, height - 2, 22, 22);
            g.dispose();
        }
    }

    private static final class PaymentArtwork extends JPanel {
        private static final long serialVersionUID = 1L;

        PaymentArtwork() {
            setBackground(new Color(223, 238, 255));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(222, 239, 255),
                    width, height, new Color(164, 208, 255)));
            g.fillRect(0, 0, width, height);
            g.setColor(new Color(255, 255, 255, 140));
            g.fillOval(-35, 18, width + 60, height / 2);
            g.setColor(new Color(88, 153, 241, 150));
            g.fillOval(-20, height - 58, width + 45, 80);

            Graphics2D phone = (Graphics2D) g.create();
            phone.rotate(-0.15, width / 2.0, height / 2.0);
            int px = width / 2 - 41;
            int py = height / 2 - 84;
            phone.setColor(new Color(14, 53, 117));
            phone.fillRoundRect(px, py, 82, 154, 18, 18);
            phone.setColor(new Color(246, 250, 255));
            phone.fillRoundRect(px + 6, py + 11, 70, 132, 13, 13);
            phone.setColor(new Color(36, 198, 139));
            phone.fillOval(px + 20, py + 38, 42, 42);
            phone.setColor(Color.WHITE);
            phone.setStroke(new java.awt.BasicStroke(4, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            phone.drawLine(px + 29, py + 58, px + 38, py + 67);
            phone.drawLine(px + 38, py + 67, px + 53, py + 49);
            phone.setColor(NAVY);
            phone.setFont(new Font("Segoe UI", Font.BOLD, 13));
            phone.drawString("Rp70.000", px + 12, py + 105);
            phone.setFont(new Font("Segoe UI", Font.PLAIN, 8));
            phone.drawString("Total Pembayaran", px + 8, py + 119);
            phone.dispose();

            g.setColor(new Color(255, 197, 66));
            g.fillOval(width / 2 - 76, height - 75, 38, 38);
            g.setColor(new Color(231, 153, 37));
            g.drawOval(width / 2 - 76, height - 75, 38, 38);
            g.fillRoundRect(width / 2 + 25, height - 99, 34, 18, 8, 8);
            g.setColor(new Color(255, 216, 112));
            g.fillOval(width / 2 + 25, height - 104, 34, 11);
            g.setColor(new Color(42, 119, 227));
            g.fillOval(width - 70, height - 94, 58, 58);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI Symbol", Font.BOLD, 31));
            g.drawString("↑", width - 54, height - 55);
            g.setColor(new Color(13, 44, 110));
            g.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g.drawString("“", 12, height - 31);
            g.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g.drawString("Segera lakukan pembayaran", 26, height - 27);
            g.drawString("sebesar Rp70.000 dan unggah", 26, height - 14);
            g.dispose();
        }
    }
}
