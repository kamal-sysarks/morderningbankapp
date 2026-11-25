package com.legacybank;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Screen 1 - Employee login.
 * The button handler queries the EMPLOYEES table directly with JDBC.
 */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextField txtUsername = new JTextField(18);
    private JPasswordField txtPassword = new JPasswordField(18);
    private JButton btnLogin = new JButton("Login");
    private JLabel lblStatus = new JLabel(" ");

    public LoginFrame() {
        super("Legacy Bank - Employee Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);

        JLabel title = new JLabel("LEGACY BANK - Branch Operations");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        panel.add(title, c);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.gridy = 1;
        c.gridx = 0;
        panel.add(new JLabel("Username:"), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        panel.add(txtUsername, c);

        c.gridy = 2;
        c.gridx = 0;
        c.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Password:"), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        panel.add(txtPassword, c);

        c.gridy = 3;
        c.gridx = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(btnLogin, c);

        c.gridy = 4;
        lblStatus.setForeground(java.awt.Color.RED);
        panel.add(lblStatus, c);

        add(panel);
        getRootPane().setDefaultButton(btnLogin);

        btnLogin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });

        setMinimumSize(new Dimension(380, 260));
        pack();
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.length() == 0 || password.length() == 0) {
            lblStatus.setText("Please enter username and password.");
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(
                    "SELECT EMPLOYEE_ID, EMPLOYEE_NAME FROM EMPLOYEES "
                    + "WHERE USERNAME = ? AND PASSWORD = ? AND STATUS = 'ACTIVE'");
            ps.setString(1, username);
            ps.setString(2, password);
            rs = ps.executeQuery();
            if (rs.next()) {
                AppSession.employeeId = rs.getLong("EMPLOYEE_ID");
                AppSession.username = username;
                AppSession.employeeName = rs.getString("EMPLOYEE_NAME");
                System.out.println("INFO: employee logged in: " + username);
                new MainFrame().setVisible(true);
                dispose();
            } else {
                System.out.println("WARN: failed login attempt for user: " + username);
                lblStatus.setText("Invalid username or password.");
                txtPassword.setText("");
            }
        } catch (SQLException ex) {
            System.out.println("ERROR: login failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Unable to connect to the database.\n" + ex.getMessage()
                    + "\n\nCheck the database settings in db.properties.",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            DBConnection.close(rs, ps, conn);
        }
    }
}
