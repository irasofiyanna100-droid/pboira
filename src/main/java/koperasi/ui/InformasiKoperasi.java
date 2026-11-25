package koperasi.ui;

import koperasi.config.IdentitasKoperasi;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class InformasiKoperasi extends JFrame {

    public InformasiKoperasi() {
        this(IdentitasKoperasi.NAMA);
    }

    public InformasiKoperasi(String cooperativeName) {
        setTitle(cooperativeName + " - Informasi Koperasi");
        setSize(620, 340);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel mainPanel = new JPanel(new BorderLayout(18, 18));
        mainPanel.setBackground(new Color(245, 248, 252));
        mainPanel.setBorder(new EmptyBorder(26, 30, 26, 30));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(cooperativeName);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(42, 105, 190));
        JLabel slogan = new JLabel("\"" + IdentitasKoperasi.SLOGAN + "\"");
        slogan.setFont(new Font("Segoe UI", Font.ITALIC, 15));
        slogan.setForeground(new Color(74, 91, 122));
        heading.add(title);
        heading.add(Box.createVerticalStrut(5));
        heading.add(slogan);

        JPanel contactCard = new JPanel(new GridLayout(3, 2, 14, 14));
        contactCard.setBackground(Color.WHITE);
        contactCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 231, 244)),
                new EmptyBorder(22, 22, 22, 22)));
        addContact(contactCard, "Alamat", IdentitasKoperasi.ALAMAT);
        addContact(contactCard, "Telepon", IdentitasKoperasi.TELEPON);
        addContact(contactCard, "Email", IdentitasKoperasi.EMAIL);

        mainPanel.add(heading, BorderLayout.NORTH);
        mainPanel.add(contactCard, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    private void addContact(JPanel panel, String label, String value) {
        JLabel field = new JLabel(label);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setForeground(new Color(104, 119, 145));
        JLabel content = new JLabel(value);
        content.setFont(new Font("Segoe UI", Font.BOLD, 14));
        content.setForeground(new Color(38, 57, 91));
        panel.add(field);
        panel.add(content);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            InformasiKoperasi frame = new InformasiKoperasi();
            frame.setVisible(true);
        });
    }
}
