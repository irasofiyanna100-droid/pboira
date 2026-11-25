
package koperasi.ui;

import koperasi.config.IdentitasKoperasi;
import koperasi.data.AccountStore;
import koperasi.data.KoperasiStore;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Login extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(Login.class.getName());

    private static final int CARD_WIDTH = 500;
    private static final int FORM_WIDTH = 440;
    private static final int FIELD_HEIGHT = 50;

    private final Color BLUE = new Color(42, 105, 190);
    private final Color TEXT = new Color(40, 48, 60);

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnEye;
    private JCheckBox remember;
    private final AccountStore accountStore = new AccountStore();
    private boolean accountStoreReady;
    private String cooperativeName = IdentitasKoperasi.NAMA;
    private JLabel cooperativeNameLabel;

    private String selectedRole = "Anggota";
    private boolean passwordVisible = false;

    private JButton btnAnggota;
    private JButton btnPetugas;
    private JButton btnAdmin;
    private JPanel memberRegistration;
    private JPanel loginCard;

    public Login() {
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 12));
        UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 12));
        UIManager.put("CheckBox.font", new Font("Segoe UI", Font.PLAIN, 11));
        try {
            cooperativeName = new KoperasiStore().getCooperativeName();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Identitas koperasi tidak dapat dimuat:\n" + e.getMessage(),
                    "Kesalahan Data",
                    JOptionPane.ERROR_MESSAGE
            );
        }
        setTitle(cooperativeName);
        setSize(1100, 720);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        try {
            accountStore.ensureDefaultAccounts();
            accountStoreReady = true;
        } catch (IOException | GeneralSecurityException | IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Inisialisasi akun bawaan gagal. Exception type: {0}",
                    e.getClass().getName());
            JOptionPane.showMessageDialog(
                    this,
                    "Akun bawaan tidak dapat disiapkan:\n" + e.getMessage(),
                    "Kesalahan Data",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Color.WHITE);
        JPanel leftPanel = createLeftPanel();
        leftPanel.setPreferredSize(new Dimension(420, 0));
        main.add(leftPanel, BorderLayout.WEST);
        main.add(createRightPanel(), BorderLayout.CENTER);

        setContentPane(main);
    }

    // PANEL KIRI
    private JPanel createLeftPanel() {
        JPanel panel = new ArtworkPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(24, 16, 0, 16));

        JPanel introduction = new JPanel();
        introduction.setOpaque(false);
        introduction.setLayout(new BoxLayout(introduction, BoxLayout.Y_AXIS));
        introduction.setBorder(new EmptyBorder(88, 0, 0, 0));

        JLabel logo = new JLabel(new CooperativeLogoIcon(150));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        cooperativeNameLabel = new JLabel(formatCooperativeName(cooperativeName));
        cooperativeNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 29));
        cooperativeNameLabel.setForeground(new Color(11, 34, 105));
        cooperativeNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel(
            "<html><div style='text-align:center'><i>\"" + IdentitasKoperasi.SLOGAN
                + "\"</i></div></html>"
        );
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitle.setForeground(new Color(16, 43, 115));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        introduction.add(logo);
        panel.add(introduction, BorderLayout.NORTH);

        JPanel brandingText = new JPanel(new GridBagLayout());
        brandingText.setOpaque(false);
        JPanel textGroup = new JPanel();
        textGroup.setOpaque(false);
        textGroup.setLayout(new BoxLayout(textGroup, BoxLayout.Y_AXIS));
        cooperativeNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        textGroup.add(cooperativeNameLabel);
        textGroup.add(Box.createVerticalStrut(10));
        textGroup.add(subtitle);
        GridBagConstraints textConstraints = new GridBagConstraints();
        textConstraints.anchor = GridBagConstraints.CENTER;
        textConstraints.insets = new Insets(0, 0, 272, 0);
        brandingText.add(textGroup, textConstraints);
        panel.add(brandingText, BorderLayout.CENTER);
        return panel;
    }

    // PANEL KANAN
    private JPanel createRightPanel() {
        JPanel panel = new RightBackgroundPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel card = new RoundedPanel();
        loginCard = card;
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new RoundBorder(new Color(220, 230, 248), 1, 22),
            new EmptyBorder(16, 18, 16, 18)
        ));

        JPanel headingPanel = new JPanel(new BorderLayout());
        headingPanel.setOpaque(false);
        headingPanel.setPreferredSize(new Dimension(FORM_WIDTH, 36));
        headingPanel.setMaximumSize(new Dimension(FORM_WIDTH, 36));
        JLabel heading = new JLabel("Selamat Datang!");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(new Color(10, 28, 93));
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        headingPanel.add(heading, BorderLayout.CENTER);
        JLabel headingDecoration = new JLabel(new SparkIcon());
        headingDecoration.setHorizontalAlignment(SwingConstants.RIGHT);
        headingPanel.add(headingDecoration, BorderLayout.EAST);

        JLabel description = new JLabel("Silakan masuk ke akun Anda");
        description.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        description.setForeground(new Color(32, 48, 100));
        description.setHorizontalAlignment(SwingConstants.CENTER);
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        description.setMaximumSize(new Dimension(FORM_WIDTH, description.getPreferredSize().height));

        card.add(headingPanel);
        card.add(Box.createVerticalStrut(8));
        card.add(description);
        card.add(Box.createVerticalStrut(8));

        JLabel roleLabel = createLabel("Masuk sebagai");
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        roleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        roleLabel.setMaximumSize(new Dimension(FORM_WIDTH, roleLabel.getPreferredSize().height));
        card.add(roleLabel);
        card.add(Box.createVerticalStrut(10));

        JPanel rolePanel = new JPanel(new GridLayout(1, 3, 10, 0));
        rolePanel.setOpaque(false);
        rolePanel.setPreferredSize(new Dimension(FORM_WIDTH, 54));
        rolePanel.setMaximumSize(new Dimension(FORM_WIDTH, 54));
        rolePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnAnggota = createRoleButton("Anggota");
        btnPetugas = createRoleButton("Petugas");
        btnAdmin = createRoleButton("Admin");
        rolePanel.add(btnAnggota);
        rolePanel.add(btnPetugas);
        rolePanel.add(btnAdmin);
        updateRole();

        card.add(rolePanel);
        card.add(Box.createVerticalStrut(10));

        txtUsername = new HintTextField("Username");
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        styleInput(txtUsername);
        card.add(createInputPanel(new PersonIcon(), txtUsername, null));
        card.add(Box.createVerticalStrut(10));

        txtPassword = new HintPasswordField("Password");
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtPassword.setBorder(new EmptyBorder(0, 3, 0, 3));
        txtPassword.setEchoChar('•');
        btnEye = new JButton(new EyeIcon(true));
        btnEye.setToolTipText("Tampilkan password");
        styleIconButton(btnEye);
        btnEye.addActionListener(e -> togglePassword());
        card.add(createInputPanel(new LockIcon(), txtPassword, btnEye));
        card.add(Box.createVerticalStrut(8));

        JPanel optionsPanel = new JPanel(new BorderLayout());
        optionsPanel.setOpaque(false);
        optionsPanel.setPreferredSize(new Dimension(FORM_WIDTH, 24));
        optionsPanel.setMaximumSize(new Dimension(FORM_WIDTH, 24));
        optionsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        remember = new JCheckBox("Ingat Saya");
        remember.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        remember.setForeground(new Color(45, 59, 104));
        remember.setOpaque(false);
        remember.setFocusPainted(false);
        remember.setIcon(new CheckBoxIcon(false));
        remember.setSelectedIcon(new CheckBoxIcon(true));
        java.util.prefs.Preferences preferences =
                java.util.prefs.Preferences.userRoot().node("koperasi");
        remember.setSelected(preferences.getBoolean("rememberUsername", false));
        if (remember.isSelected()) {
            txtUsername.setText(preferences.get("lastUsername", ""));
        }
        JButton forgotPassword = createLinkButton("Lupa Password?");
        forgotPassword.addActionListener(e -> JOptionPane.showMessageDialog(
            this,
            "Silakan hubungi petugas koperasi untuk bantuan pemulihan password.",
            "Lupa Password",
            JOptionPane.INFORMATION_MESSAGE
        ));
        optionsPanel.add(remember, BorderLayout.WEST);
        optionsPanel.add(forgotPassword, BorderLayout.EAST);
        card.add(optionsPanel);
        card.add(Box.createVerticalStrut(8));

        JButton btnLogin = createActionButton(
            "Login", null, BLUE, new Color(20, 75, 190)
        );
        btnLogin.addActionListener(e -> prosesLogin());
        txtPassword.addActionListener(e -> prosesLogin());
        card.add(btnLogin);
        card.add(Box.createVerticalStrut(8));

        memberRegistration = new JPanel();
        memberRegistration.setOpaque(false);
        memberRegistration.setLayout(new BoxLayout(memberRegistration, BoxLayout.Y_AXIS));
        memberRegistration.setAlignmentX(Component.CENTER_ALIGNMENT);
        memberRegistration.add(Box.createVerticalStrut(8));

        JPanel registerTitle = new JPanel(new BorderLayout(8, 0));
        registerTitle.setOpaque(false);
        registerTitle.setPreferredSize(new Dimension(FORM_WIDTH, 20));
        registerTitle.setMaximumSize(new Dimension(FORM_WIDTH, 20));
        registerTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel belum = new JLabel("Belum punya akun?");
        belum.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        belum.setForeground(new Color(47, 59, 99));
        belum.setHorizontalAlignment(SwingConstants.CENTER);
        registerTitle.add(createSeparator(), BorderLayout.WEST);
        registerTitle.add(belum, BorderLayout.CENTER);
        registerTitle.add(createSeparator(), BorderLayout.EAST);
        memberRegistration.add(registerTitle);
        memberRegistration.add(Box.createVerticalStrut(8));

        JButton btnDaftar = createActionButton(
            "Daftar Anggota", new AddPersonIcon(), Color.WHITE, new Color(0, 92, 255)
        );
        btnDaftar.setForeground(new Color(0, 75, 215));
        btnDaftar.addActionListener(e -> showRegistrationDialog());
        memberRegistration.add(btnDaftar);
        card.add(memberRegistration);

        Dimension cardSize = card.getPreferredSize();
        card.setPreferredSize(new Dimension(
            Math.max(CARD_WIDTH, cardSize.width),
            Math.max(500, cardSize.height)
        ));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(card, constraints);
        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(TEXT);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private void styleInput(JTextField field) {
        field.setBorder(new EmptyBorder(0, 3, 0, 3));
    }

    private JPanel createInputPanel(Icon icon, JTextField field, JButton trailingButton) {
        JPanel input = new JPanel(new BorderLayout(8, 0));
        input.setBackground(Color.WHITE);
        Dimension size = new Dimension(FORM_WIDTH, FIELD_HEIGHT);
        input.setPreferredSize(size);
        input.setMaximumSize(size);
        input.setAlignmentX(Component.CENTER_ALIGNMENT);
        input.setBorder(createInputBorder(false));

        JLabel leadingIcon = new JLabel(icon);
        leadingIcon.setBorder(new EmptyBorder(0, 0, 0, 1));
        input.add(leadingIcon, BorderLayout.WEST);
        input.add(field, BorderLayout.CENTER);
        if (trailingButton != null) {
            input.add(trailingButton, BorderLayout.EAST);
        }
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                input.setBorder(createInputBorder(true));
            }

            @Override
            public void focusLost(FocusEvent e) {
                input.setBorder(createInputBorder(false));
            }
        });
        return input;
    }

    private Border createInputBorder(boolean focused) {
        return new CompoundBorder(
            new RoundBorder(
                focused ? new Color(44, 115, 224) : new Color(202, 215, 242),
                focused ? 1.4f : 1f,
                12
            ),
            new EmptyBorder(3, 12, 3, 10)
        );
    }

    private void styleIconButton(JButton button) {
        button.setBackground(Color.WHITE);
        button.setBorder(new EmptyBorder(4, 6, 4, 2));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private JButton createLinkButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(new Color(0, 74, 205));
        button.setBorder(new EmptyBorder(2, 4, 2, 0));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createActionButton(String text, Icon icon, Color fill, Color outline) {
        JButton button = new JButton(text, icon);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(fill.equals(Color.WHITE) ? new Color(0, 75, 215) : Color.WHITE);
        button.setBackground(fill);
        button.setIconTextGap(9);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(0, 12, 0, 12));
        Dimension size = new Dimension(FORM_WIDTH, 50);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setUI(new RoundedButtonUI(outline, 13));
        return button;
    }

    private JSeparator createSeparator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(210, 218, 237));
        separator.setBackground(new Color(210, 218, 237));
        return separator;
    }

    private JButton createRoleButton(String text) {
        JButton button = new JButton(text);
        button.setIcon(new RoleIcon(text));
        button.setIconTextGap(6);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(6, 4, 6, 4));
        button.setUI(new RoundedButtonUI(null, 28));

        button.addActionListener(e -> {
            selectedRole = text;
            updateRole();
        });

        return button;
    }

    private void updateRole() {
        JButton[] buttons = {
            btnAnggota, btnPetugas, btnAdmin
        };

        String[] roles = {
            "Anggota", "Petugas", "Admin"
        };

        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] == null) continue;

            boolean active = selectedRole.equals(roles[i]);

            Color roleFill = active ? new Color(32, 104, 204) : new Color(242, 247, 253);
            buttons[i].setBackground(roleFill);
            buttons[i].setForeground(active ? Color.WHITE : new Color(44, 68, 112));
            buttons[i].setIcon(new RoleIcon(roles[i], active ? Color.WHITE : new Color(65, 103, 163)));
            buttons[i].setToolTipText("Masuk sebagai " + roles[i]);
            buttons[i].setUI(new RoundedButtonUI(
                    active ? roleFill : new Color(223, 233, 247), 28));
        }

        if (memberRegistration != null) {
            boolean memberSelected = "Anggota".equals(selectedRole);
            memberRegistration.setVisible(memberSelected);
            if (loginCard != null) {
                loginCard.setPreferredSize(new Dimension(CARD_WIDTH, memberSelected ? 500 : 420));
                loginCard.revalidate();
                loginCard.repaint();
            }
        }
    }

    // SHOW / HIDE PASSWORD
    private void togglePassword() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            txtPassword.setEchoChar((char) 0);
            btnEye.setIcon(new EyeIcon(false));
            btnEye.setToolTipText("Sembunyikan password");
        } else {
            txtPassword.setEchoChar('•');
            btnEye.setIcon(new EyeIcon(true));
            btnEye.setToolTipText("Tampilkan password");
        }
    }

    private void prosesLogin() {
        if (!accountStoreReady) {
            showAccountStoreUnavailable();
            return;
        }
        String username = txtUsername.getText().trim();
        char[] password = txtPassword.getPassword();

        if (username.isEmpty() || password.length == 0) {
            JOptionPane.showMessageDialog(
                this,
                "Username dan password wajib diisi!",
                "Peringatan",
                JOptionPane.WARNING_MESSAGE
            );
            Arrays.fill(password, '\0');
            return;
        }

        try {
            AccountStore.Account account = accountStore.authenticate(username, password, selectedRole);
            if (account == null) {
                JOptionPane.showMessageDialog(
                    this,
                    "Nama/akun belum terdaftar, password salah, atau peran yang dipilih tidak sesuai.",
                    "Login gagal",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            java.util.prefs.Preferences preferences =
                    java.util.prefs.Preferences.userRoot().node("koperasi");
            preferences.putBoolean("rememberUsername", remember.isSelected());
            if (remember.isSelected()) {
                preferences.put("lastUsername", username);
            } else {
                preferences.remove("lastUsername");
            }
            txtPassword.setText("");
            Dashboard dashboard = new Dashboard(account, accountStore, () -> {
                refreshCooperativeName();
                setVisible(true);
            });
            setVisible(false);
            dashboard.setVisible(true);
        } catch (IOException | GeneralSecurityException e) {
            JOptionPane.showMessageDialog(
                this,
                "Data akun tidak dapat dibaca:\n" + e.getMessage(),
                "Kesalahan Data",
                JOptionPane.ERROR_MESSAGE
            );
        } finally {
            Arrays.fill(password, '\0');
        }

    }

    private void refreshCooperativeName() {
        try {
            cooperativeName = new KoperasiStore().getCooperativeName();
            setTitle(cooperativeName);
            cooperativeNameLabel.setText(formatCooperativeName(cooperativeName));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Identitas koperasi tidak dapat dimuat:\n" + e.getMessage(),
                    "Kesalahan Data",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static String formatCooperativeName(String value) {
        StringBuilder lines = new StringBuilder("<html><div style='text-align:center'>");
        int lineLength = 0;
        for (String word : value.split("\\s+")) {
            if (lineLength > 0) {
                if (lineLength + word.length() + 1 > 20) {
                    lines.append("<br>");
                    lineLength = 0;
                } else {
                    lines.append(' ');
                    lineLength++;
                }
            }
            lines.append(escapeHtml(word));
            lineLength += word.length();
        }
        return lines.append("</div></html>").toString();
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void showRegistrationDialog() {
        if (!accountStoreReady) {
            showAccountStoreUnavailable();
            return;
        }
        setVisible(false);
        RegistrationWizard wizard = new RegistrationWizard(this, accountStore, registeredUsername -> {
            txtUsername.setText(registeredUsername);
            selectedRole = AccountStore.MEMBER;
            updateRole();
            setVisible(true);
        }, () -> setVisible(true));
        wizard.setVisible(true);
    }

    private void showAccountStoreUnavailable() {
        JOptionPane.showMessageDialog(
                this,
                "Fitur akun belum tersedia karena akun bawaan gagal disiapkan.",
                "Kesalahan Data",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static class HintTextField extends JTextField {
        private static final long serialVersionUID = 1L;
        private final String hint;

        HintTextField(String hint) {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(115, 128, 164));
                g2.setFont(getFont());
                FontMetrics metrics = g2.getFontMetrics();
                g2.drawString(hint, getInsets().left + 2,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
                g2.dispose();
            }
        }
    }

    private static class HintPasswordField extends JPasswordField {
        private static final long serialVersionUID = 1L;
        private final String hint;

        HintPasswordField(String hint) {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getPassword().length == 0 && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(115, 128, 164));
                g2.setFont(getFont());
                FontMetrics metrics = g2.getFontMetrics();
                g2.drawString(hint, getInsets().left + 2,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
                g2.dispose();
            }
        }
    }

    private static class RoundBorder extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final Color color;
        private final float thickness;
        private final int arc;

        RoundBorder(Color color, float thickness, int arc) {
            this.color = color;
            this.thickness = thickness;
            this.arc = arc;
        }

        @Override
        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.draw(new RoundRectangle2D.Float(
                x + thickness / 2, y + thickness / 2,
                width - thickness, height - thickness, arc, arc
            ));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(7, 9, 7, 9);
        }
    }

    private static class RoundedPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final int arc = 22;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(28, 62, 126, 28));
            g2.fillRoundRect(2, 5, getWidth() - 3, getHeight() - 3, arc, arc);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 4, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedButtonUI extends BasicButtonUI {
        private final Color outline;
        private final int arc;

        RoundedButtonUI(Color outline, int arc) {
            this.outline = outline;
            this.arc = arc;
        }

        @Override
        public void paint(Graphics g, JComponent component) {
            AbstractButton button = (AbstractButton) component;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = button.getBackground();
            if (button.getModel().isPressed()) {
                fill = fill.darker();
            } else if (button.getModel().isRollover()) {
                fill = fill.brighter();
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, component.getWidth(), component.getHeight(), arc, arc);
            if (outline != null && !outline.equals(fill)) {
                g2.setColor(outline);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, component.getWidth() - 1,
                    component.getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paint(g, component);
        }
    }

    private static class ArtworkPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        ArtworkPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, new Color(248, 251, 255),
                w * 0.8f, h, new Color(203, 224, 255)));
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(255, 255, 255, 115));
            g2.fillOval((int) (w * 0.54), -h / 5, w, (int) (h * 0.57));
            g2.fillOval(-w / 2, (int) (h * 0.28), w, (int) (h * 0.48));

            Path2D farHill = new Path2D.Double();
            farHill.moveTo(0, h * 0.77);
            farHill.curveTo(w * 0.21, h * 0.69, w * 0.32, h * 0.81, w * 0.51, h * 0.74);
            farHill.curveTo(w * 0.72, h * 0.67, w * 0.83, h * 0.76, w, h * 0.68);
            farHill.lineTo(w, h);
            farHill.lineTo(0, h);
            farHill.closePath();
            g2.setColor(new Color(129, 188, 255, 110));
            g2.fill(farHill);

            Path2D nearHill = new Path2D.Double();
            nearHill.moveTo(0, h * 0.87);
            nearHill.curveTo(w * 0.24, h * 0.78, w * 0.36, h * 0.91, w * 0.57, h * 0.84);
            nearHill.curveTo(w * 0.77, h * 0.77, w * 0.89, h * 0.87, w, h * 0.79);
            nearHill.lineTo(w, h);
            nearHill.lineTo(0, h);
            nearHill.closePath();
            g2.setColor(new Color(72, 150, 246, 145));
            g2.fill(nearHill);

            drawTree(g2, (int) (w * 0.13), (int) (h * 0.83), h * 0.16);
            drawTree(g2, (int) (w * 0.87), (int) (h * 0.82), h * 0.17);
            drawBankBuilding(g2, w, h);
            g2.dispose();
        }

        private void drawTree(Graphics2D g2, int x, int baseY, double height) {
            double scale = height / 100.0;
            Graphics2D tree = (Graphics2D) g2.create();
            tree.translate(x, baseY);
            tree.scale(scale, scale);
            tree.setColor(new Color(112, 105, 80));
            tree.fillRoundRect(-6, -34, 12, 38, 6, 6);
            tree.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            tree.drawLine(0, -27, -16, -43);
            tree.drawLine(1, -28, 16, -46);

            Path2D canopy = new Path2D.Double();
            canopy.moveTo(-38, -37);
            canopy.curveTo(-43, -54, -32, -69, -17, -69);
            canopy.curveTo(-14, -91, 13, -96, 23, -78);
            canopy.curveTo(43, -76, 48, -52, 34, -40);
            canopy.curveTo(29, -28, -25, -27, -38, -37);
            canopy.closePath();
            tree.setPaint(new GradientPaint(-20, -88, new Color(93, 183, 135),
                    18, -28, new Color(34, 133, 95)));
            tree.fill(canopy);
            tree.setColor(new Color(255, 255, 255, 38));
            tree.fill(new Ellipse2D.Double(-20, -78, 20, 12));
            tree.dispose();
        }

        private void drawBankBuilding(Graphics2D g2, int w, int h) {
            double unit = Math.min(w / 500.0, h / 320.0);
            Graphics2D bank = (Graphics2D) g2.create();
            bank.translate((w - 390 * unit) / 2, h * 1.025 - 240 * unit);
            bank.scale(unit, unit);

            bank.setColor(new Color(24, 66, 121, 42));
            bank.fill(new Ellipse2D.Double(38, 214, 314, 22));

            bank.setColor(new Color(31, 81, 146));
            bank.fillRoundRect(35, 211, 320, 13, 5, 5);
            bank.setPaint(new GradientPaint(47, 198, new Color(255, 255, 255),
                    340, 210, new Color(178, 210, 247)));
            bank.fillRoundRect(48, 198, 294, 14, 4, 4);
            bank.setColor(new Color(103, 158, 219));
            bank.fillRoundRect(59, 190, 272, 9, 3, 3);

            bank.setPaint(new GradientPaint(75, 91, new Color(255, 255, 255),
                    320, 190, new Color(216, 234, 255)));
            bank.fillRect(70, 83, 250, 108);
            bank.setColor(new Color(104, 157, 215));
            bank.setStroke(new BasicStroke(2f));
            bank.drawRect(70, 83, 250, 108);

            Path2D roof = new Path2D.Double();
            roof.moveTo(42, 82);
            roof.lineTo(195, 23);
            roof.lineTo(348, 82);
            roof.closePath();
            bank.setPaint(new GradientPaint(90, 37, new Color(55, 121, 202),
                    300, 82, new Color(25, 76, 148)));
            bank.fill(roof);
            bank.setColor(new Color(255, 255, 255, 190));
            bank.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            bank.drawLine(65, 75, 195, 27);
            bank.drawLine(195, 27, 325, 75);

            bank.setColor(new Color(24, 74, 139));
            bank.fillRoundRect(66, 77, 258, 10, 3, 3);
            bank.setColor(new Color(247, 251, 255));
            bank.fillRoundRect(92, 69, 206, 16, 4, 4);
            bank.setColor(new Color(34, 83, 145));
            bank.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics metrics = bank.getFontMetrics();
            String sign = "KOPERASI SIMPAN PINJAM";
            bank.drawString(sign, 195 - metrics.stringWidth(sign) / 2, 81);

            for (int x : new int[] {108, 242}) {
                bank.setColor(new Color(82, 143, 207));
                bank.fillRoundRect(x - 3, 109, 43, 61, 5, 5);
                bank.setPaint(new GradientPaint(x, 111, new Color(225, 242, 255),
                        x + 36, 167, new Color(143, 195, 239)));
                bank.fillRoundRect(x, 112, 37, 55, 3, 3);
                bank.setColor(new Color(255, 255, 255, 210));
                bank.fillRect(x + 17, 114, 3, 51);
                bank.fillRect(x + 2, 137, 33, 3);
            }

            for (int i = 0; i < 4; i++) {
                int x = 92 + i * 62;
                bank.setColor(new Color(123, 170, 221));
                bank.fillRoundRect(x - 4, 96, 18, 87, 5, 5);
                bank.setPaint(new GradientPaint(x, 96, Color.WHITE,
                        x + 13, 180, new Color(201, 224, 252)));
                bank.fillRoundRect(x, 98, 10, 82, 4, 4);
                bank.setColor(new Color(105, 157, 211));
                bank.fillRoundRect(x - 6, 94, 22, 5, 2, 2);
                bank.fillRoundRect(x - 6, 179, 22, 5, 2, 2);
            }

            bank.setColor(new Color(37, 91, 156));
            bank.fillRoundRect(176, 139, 39, 52, 16, 16);
            bank.setColor(new Color(173, 208, 248));
            bank.fillRoundRect(182, 145, 27, 46, 12, 12);
            bank.setColor(new Color(42, 95, 159));
            bank.fillRoundRect(187, 159, 17, 32, 3, 3);
            bank.setColor(Color.WHITE);
            bank.fillOval(199, 174, 3, 3);

            bank.setColor(new Color(255, 255, 255, 160));
            bank.fillRoundRect(81, 187, 228, 2, 2, 2);
            bank.dispose();
        }
    }

    private static class RightBackgroundPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        RightBackgroundPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(234, 243, 255),
                getWidth(), getHeight(), new Color(248, 250, 255)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 255, 255, 150));
            g2.fillOval(getWidth() / 2, -getHeight() / 3, getWidth(), getHeight() / 2);
            g2.dispose();
        }
    }

    private static class CooperativeLogoIcon implements Icon {
        private final int size;

        CooperativeLogoIcon(int size) {
            this.size = size;
        }

        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double scale = size / 120.0;
            g2.translate(x + (size - 120 * scale) / 2, y + (size - 120 * scale) / 2);
            g2.scale(scale, scale);
            g2.setColor(new Color(22, 53, 105, 28));
            g2.fill(new Ellipse2D.Double(15, 17, 92, 92));
            g2.setPaint(new GradientPaint(19, 13, Color.WHITE,
                    100, 104, new Color(225, 238, 255)));
            g2.fill(new Ellipse2D.Double(11, 9, 98, 98));
            g2.setColor(new Color(31, 90, 170));
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(new Ellipse2D.Double(12, 10, 96, 96));
            g2.setPaint(new GradientPaint(32, 24, new Color(49, 128, 218),
                    91, 93, new Color(20, 68, 151)));
            g2.fill(new Ellipse2D.Double(20, 18, 80, 80));

            g2.setColor(new Color(255, 255, 255, 75));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new Ellipse2D.Double(25, 23, 70, 70));

            g2.setColor(new Color(255, 255, 255));
            g2.fill(new Ellipse2D.Double(34, 38, 14, 14));
            g2.fill(new Ellipse2D.Double(52, 30, 16, 16));
            g2.fill(new Ellipse2D.Double(73, 38, 14, 14));

            Path2D leftPerson = new Path2D.Double();
            leftPerson.moveTo(28, 77);
            leftPerson.curveTo(29, 65, 32, 57, 40, 57);
            leftPerson.curveTo(46, 57, 48, 62, 49, 68);
            leftPerson.lineTo(49, 77);
            leftPerson.closePath();
            g2.fill(leftPerson);

            Path2D centerPerson = new Path2D.Double();
            centerPerson.moveTo(50, 77);
            centerPerson.curveTo(50, 60, 52, 51, 60, 51);
            centerPerson.curveTo(68, 51, 70, 60, 70, 77);
            centerPerson.closePath();
            g2.fill(centerPerson);

            Path2D rightPerson = new Path2D.Double();
            rightPerson.moveTo(71, 68);
            rightPerson.curveTo(72, 62, 74, 57, 80, 57);
            rightPerson.curveTo(88, 57, 91, 65, 92, 77);
            rightPerson.lineTo(71, 77);
            rightPerson.closePath();
            g2.fill(rightPerson);

            g2.setColor(new Color(255, 205, 91));
            g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Arc2D.Double(34, 78, 52, 12, 190, 160, Arc2D.OPEN));
            g2.dispose();
        }
    }

    private static class RoleIcon implements Icon {
        private final String role;
        private final Color color;

        RoleIcon(String role) {
            this(role, new Color(31, 44, 91));
        }

        RoleIcon(String role, Color color) {
            this.role = role;
            this.color = color;
        }

        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            if ("Anggota".equals(role)) {
                g2.fillOval(x + 6, y + 1, 8, 8);
                g2.fillRoundRect(x + 2, y + 11, 16, 8, 8, 8);
            } else if ("Petugas".equals(role)) {
                g2.fillRoundRect(x + 2, y + 6, 14, 10, 2, 2);
                g2.drawRoundRect(x + 6, y + 2, 6, 5, 2, 2);
                g2.setColor(Color.WHITE);
                g2.drawLine(x + 8, y + 9, x + 10, y + 9);
            } else {
                Polygon shield = new Polygon(
                    new int[] {x + 9, x + 16, x + 15, x + 13, x + 9, x + 5, x + 3, x + 2},
                    new int[] {y + 1, y + 4, y + 12, y + 15, y + 17, y + 15, y + 12, y + 4}, 8
                );
                g2.fillPolygon(shield);
                g2.setColor(Color.WHITE);
                g2.fillOval(x + 7, y + 5, 4, 4);
                g2.fillRoundRect(x + 6, y + 10, 6, 4, 3, 3);
            }
            g2.dispose();
        }
    }

    private static class PersonIcon implements Icon {
        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(67, 91, 151));
            g2.fillOval(x + 5, y + 1, 8, 8);
            g2.fillRoundRect(x + 2, y + 10, 14, 8, 7, 7);
            g2.dispose();
        }
    }

    private static class LockIcon implements Icon {
        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(67, 91, 151));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x + 4, y + 1, 10, 10, 6, 6);
            g2.fillRoundRect(x + 2, y + 7, 14, 10, 3, 3);
            g2.setColor(Color.WHITE);
            g2.fillOval(x + 8, y + 10, 3, 3);
            g2.dispose();
        }
    }

    private static class CheckBoxIcon implements Icon {
        private final boolean checked;

        CheckBoxIcon(boolean checked) {
            this.checked = checked;
        }

        public int getIconWidth() { return 15; }
        public int getIconHeight() { return 15; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(checked ? new Color(0, 88, 226) : new Color(168, 184, 220));
            g2.fillRoundRect(x, y, 14, 14, 4, 4);
            if (checked) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawLine(x + 3, y + 7, x + 6, y + 10);
                g2.drawLine(x + 6, y + 10, x + 11, y + 4);
            } else {
                g2.setColor(Color.WHITE);
                g2.drawRoundRect(x, y, 14, 14, 4, 4);
            }
            g2.dispose();
        }
    }

    private static class AddPersonIcon implements Icon {
        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 89, 225));
            g2.fillOval(x + 2, y + 1, 8, 8);
            g2.fillRoundRect(x, y + 10, 12, 7, 5, 5);
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(x + 14, y + 5, x + 14, y + 14);
            g2.drawLine(x + 10, y + 9, x + 18, y + 9);
            g2.dispose();
        }
    }

    private static class SparkIcon implements Icon {
        public int getIconWidth() { return 24; }
        public int getIconHeight() { return 24; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 78, 221));
            Path2D sparkle = new Path2D.Double();
            sparkle.moveTo(x + 11, y + 1);
            sparkle.lineTo(x + 14, y + 9);
            sparkle.lineTo(x + 22, y + 12);
            sparkle.lineTo(x + 14, y + 15);
            sparkle.lineTo(x + 11, y + 23);
            sparkle.lineTo(x + 8, y + 15);
            sparkle.lineTo(x, y + 12);
            sparkle.lineTo(x + 8, y + 9);
            sparkle.closePath();
            g2.fill(sparkle);
            g2.setColor(new Color(101, 156, 245));
            g2.fill(new Ellipse2D.Double(x + 17, y + 1, 4, 4));
            g2.dispose();
        }
    }

    // IKON MATA TANPA LIBRARY TAMBAHAN
    static class EyeIcon implements Icon {

        private boolean hidden;

        public EyeIcon(boolean hidden) {
            this.hidden = hidden;
        }

        public int getIconWidth() { return 24; }
        public int getIconHeight() { return 24; }

        public void paintIcon(
            Component c, Graphics g, int x, int y
        ) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            Path2D eye = new Path2D.Double();
            eye.moveTo(x + 2, y + 12);
            eye.quadTo(x + 12, y + 2, x + 22, y + 12);
            eye.quadTo(x + 12, y + 22, x + 2, y + 12);
            g2.setColor(new Color(84, 105, 142));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(eye);
            g2.setColor(new Color(44, 95, 173));
            g2.fill(new Ellipse2D.Double(x + 8, y + 8, 8, 8));
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(x + 10, y + 9, 2.5, 2.5));

            if (hidden) {
                g2.setColor(new Color(84, 105, 142));
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 4, y + 20, x + 20, y + 4);
            }

            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Login login = new Login();
            login.setVisible(true);
        });
    }
}
