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
 * Screen 7 - Fund transfer between two accounts.
 * Debit, credit and both transaction records happen in ONE JDBC transaction;
 * any failure rolls everything back.
 */
public class TransferFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextField txtFrom = new JTextField(16);
    private JTextField txtTo = new JTextField(16);
    private JTextField txtAmount = new JTextField(16);
    private JTextField txtDescription = new JTextField(24);
    private JLabel lblFromBalance = new JLabel("-");
    private JLabel lblToBalance = new JLabel("-");
    private JLabel lblReference = new JLabel("-");
    private JButton btnTransfer = new JButton("Transfer");
    private JButton btnClose = new JButton("Close");

    public TransferFrame(String fromAccount) {
        super("Fund Transfer");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel form = new JPanel(new GridLayout(7, 2, 6, 8));
        form.setBorder(BorderFactory.createTitledBorder("Transfer"));
        form.add(new JLabel("Source Account:"));
        form.add(txtFrom);
        form.add(new JLabel("Destination Account:"));
        form.add(txtTo);
        form.add(new JLabel("Transfer Amount:"));
        form.add(txtAmount);
        form.add(new JLabel("Description (optional):"));
        form.add(txtDescription);
        form.add(new JLabel("Source Balance:"));
        form.add(lblFromBalance);
        form.add(new JLabel("Destination Balance:"));
        form.add(lblToBalance);
        form.add(new JLabel("Reference Number:"));
        form.add(lblReference);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnTransfer);
        bottom.add(btnClose);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(form, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        add(root);

        if (fromAccount != null) {
            txtFrom.setText(fromAccount);
        }
        getRootPane().setDefaultButton(btnTransfer);

        btnTransfer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doTransfer();
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

    /** Reads one account row (inside the open write transaction) and returns {status, balance}. Throws if the account is missing. */
    private Object[] lockAccount(Connection conn, String accountNumber, String label)
            throws SQLException, BankingException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(
                    "SELECT STATUS, IFNULL(BALANCE, 0) AS BALANCE FROM ACCOUNTS "
                    + "WHERE ACCOUNT_NUMBER = ?");
            ps.setString(1, accountNumber);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new BankingException(label + " account " + accountNumber + " does not exist.");
            }
            return new Object[] { rs.getString("STATUS"), BankUtil.getMoney(rs, "BALANCE") };
        } finally {
            DBConnection.close(rs, ps);
        }
    }

    private void doTransfer() {
        String from;
        String to;
        BigDecimal amount;
        try {
            from = BankUtil.checkAccountNumber(txtFrom.getText(), "source account number");
            to = BankUtil.checkAccountNumber(txtTo.getText(), "destination account number");
            if (from.equals(to)) {
                throw new BankingException("Source and destination accounts must be different.");
            }
            amount = BankUtil.parseAmount(txtAmount.getText());
        } catch (BankingException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Fund Transfer", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String userNote = txtDescription.getText().trim();
        String suffix = (userNote.length() > 0 ? " - " + userNote : "") + " (by " + AppSession.username + ")";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // setAutoCommit(false) started a BEGIN IMMEDIATE transaction: the write lock is held
            // until commit/rollback, so both balances are read and changed atomically
            Object[] fromRow = lockAccount(conn, from, "Source");
            Object[] toRow = lockAccount(conn, to, "Destination");

            if (!"ACTIVE".equals(fromRow[0])) {
                throw new BankingException("Source account " + from + " is not active (status: " + fromRow[0] + ").");
            }
            if (!"ACTIVE".equals(toRow[0])) {
                throw new BankingException("Destination account " + to + " is not active (status: " + toRow[0] + ").");
            }
            BigDecimal fromBalance = (BigDecimal) fromRow[1];
            BigDecimal toBalance = (BigDecimal) toRow[1];
            if (fromBalance.compareTo(amount) < 0) {
                throw new BankingException("Insufficient funds in source account. Available balance is "
                        + BankUtil.formatMoney(fromBalance) + ".");
            }

            BigDecimal newFrom = fromBalance.subtract(amount);
            BigDecimal newTo = toBalance.add(amount);

            String reference = BankUtil.nextReference(conn, "TRF");

            BankUtil.updateBalance(conn, from, newFrom);
            BankUtil.updateBalance(conn, to, newTo);
            BankUtil.insertTransaction(conn, from, "TRANSFER_DEBIT", amount, "Transfer to " + to + suffix, reference);
            BankUtil.insertTransaction(conn, to, "TRANSFER_CREDIT", amount, "Transfer from " + from + suffix, reference);

            conn.commit();

            System.out.println("INFO: transfer " + amount + " from " + from + " to " + to + " ref " + reference);
            lblFromBalance.setText(BankUtil.formatMoney(newFrom));
            lblToBalance.setText(BankUtil.formatMoney(newTo));
            lblReference.setText(reference);
            txtAmount.setText("");
            JOptionPane.showMessageDialog(this,
                    "Transfer successful.\nReference: " + reference
                    + "\nSource balance: " + BankUtil.formatMoney(newFrom)
                    + "\nDestination balance: " + BankUtil.formatMoney(newTo),
                    "Fund Transfer", JOptionPane.INFORMATION_MESSAGE);
        } catch (BankingException ex) {
            DBConnection.rollback(conn);
            System.out.println("WARN: transfer rejected: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Transfer Rejected",
                    JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            DBConnection.rollback(conn);
            System.out.println("ERROR: transfer failed, all changes rolled back: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Transfer failed and was rolled back.\n" + ex.getMessage(),
                    "Transfer Failed", JOptionPane.ERROR_MESSAGE);
        } finally {
            DBConnection.close(conn);
        }
    }
}
