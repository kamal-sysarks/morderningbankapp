package com.legacybank;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Screen 5 - Cash deposit.
 * Validation, balance update and transaction record are done in this event handler
 * inside one JDBC transaction.
 */
public class DepositFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextField txtAccount = new JTextField(16);
    private JTextField txtAmount = new JTextField(16);
    private JTextField txtDescription = new JTextField(24);
    private JLabel lblBalance = new JLabel("-");
    private JLabel lblReference = new JLabel("-");
    private JButton btnDeposit = new JButton("Deposit");
    private JButton btnClose = new JButton("Close");

    public DepositFrame(String accountNumber) {
        super("Cash Deposit");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel form = new JPanel(new GridLayout(5, 2, 6, 8));
        form.setBorder(BorderFactory.createTitledBorder("Deposit"));
        form.add(new JLabel("Account Number:"));
        form.add(txtAccount);
        form.add(new JLabel("Deposit Amount:"));
        form.add(txtAmount);
        form.add(new JLabel("Description (optional):"));
        form.add(txtDescription);
        form.add(new JLabel("Updated Balance:"));
        form.add(lblBalance);
        form.add(new JLabel("Reference Number:"));
        form.add(lblReference);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnDeposit);
        bottom.add(btnClose);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(form, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        add(root);

        if (accountNumber != null) {
            txtAccount.setText(accountNumber);
        }
        getRootPane().setDefaultButton(btnDeposit);

        btnDeposit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doDeposit();
            }
        });
        btnClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private void doDeposit() {
        String accountNumber;
        BigDecimal amount;
        try {
            accountNumber = BankUtil.checkAccountNumber(txtAccount.getText(), "account number");
            amount = BankUtil.parseAmount(txtAmount.getText());
        } catch (BankingException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Deposit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String description = txtDescription.getText().trim();
        if (description.length() == 0) {
            description = "Cash deposit";
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // BEGIN IMMEDIATE: holds the write lock until commit/rollback

            ps = conn.prepareStatement(
                    "SELECT STATUS, IFNULL(BALANCE, 0) AS BALANCE FROM ACCOUNTS WHERE ACCOUNT_NUMBER = ?");
            ps.setString(1, accountNumber);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new BankingException("Account " + accountNumber + " does not exist.");
            }
            String status = rs.getString("STATUS");
            BigDecimal balance = BankUtil.getMoney(rs, "BALANCE");
            DBConnection.close(rs, ps);
            rs = null;
            ps = null;

            if (!"ACTIVE".equals(status)) {
                throw new BankingException("Account " + accountNumber + " is not active (status: " + status + ").");
            }

            BigDecimal newBalance = balance.add(amount);
            BankUtil.updateBalance(conn, accountNumber, newBalance);
            String reference = BankUtil.nextReference(conn, "DEP");
            BankUtil.insertTransaction(conn, accountNumber, "DEPOSIT", amount,
                    description + " (by " + AppSession.username + ")", reference);
            conn.commit();

            System.out.println("INFO: deposit " + amount + " to " + accountNumber + " ref " + reference);
            lblBalance.setText(BankUtil.formatMoney(newBalance));
            lblReference.setText(reference);
            txtAmount.setText("");
            JOptionPane.showMessageDialog(this,
                    "Deposit successful.\nReference: " + reference
                    + "\nUpdated balance: " + BankUtil.formatMoney(newBalance),
                    "Deposit", JOptionPane.INFORMATION_MESSAGE);
        } catch (BankingException ex) {
            DBConnection.rollback(conn);
            System.out.println("WARN: deposit rejected: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Deposit Rejected",
                    JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            DBConnection.rollback(conn);
            System.out.println("ERROR: deposit failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Deposit Failed", JOptionPane.ERROR_MESSAGE);
        } finally {
            DBConnection.close(rs, ps, conn);
        }
    }
}
