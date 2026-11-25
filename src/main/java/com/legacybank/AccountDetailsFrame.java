package com.legacybank;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
 * Screen 4 - Account details: account, owner and available balance, with
 * shortcuts to deposit, withdraw and history for the displayed account.
 */
public class AccountDetailsFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextField txtAccount = new JTextField(16);
    private JButton btnLookup = new JButton("Lookup");
    private JButton btnDeposit = new JButton("Deposit");
    private JButton btnWithdraw = new JButton("Withdraw");
    private JButton btnHistory = new JButton("Transactions");
    private JButton btnClose = new JButton("Close");

    private JLabel lblAccount = new JLabel("-");
    private JLabel lblType = new JLabel("-");
    private JLabel lblStatus = new JLabel("-");
    private JLabel lblBalance = new JLabel("-");
    private JLabel lblOpened = new JLabel("-");
    private JLabel lblCustomer = new JLabel("-");
    private JLabel lblEmail = new JLabel("-");
    private JLabel lblPhone = new JLabel("-");

    private String currentAccount = null;

    public AccountDetailsFrame(String accountNumber) {
        super("Account Details");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Account Number:"));
        top.add(txtAccount);
        top.add(btnLookup);

        JPanel info = new JPanel(new GridLayout(8, 2, 4, 6));
        info.setBorder(BorderFactory.createTitledBorder("Account"));
        info.add(new JLabel("Account Number:"));
        info.add(lblAccount);
        info.add(new JLabel("Account Type:"));
        info.add(lblType);
        info.add(new JLabel("Status:"));
        info.add(lblStatus);
        info.add(new JLabel("Available Balance:"));
        info.add(lblBalance);
        info.add(new JLabel("Opened:"));
        info.add(lblOpened);
        info.add(new JLabel("Customer:"));
        info.add(lblCustomer);
        info.add(new JLabel("Email:"));
        info.add(lblEmail);
        info.add(new JLabel("Phone:"));
        info.add(lblPhone);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnDeposit);
        bottom.add(btnWithdraw);
        bottom.add(btnHistory);
        bottom.add(btnClose);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(top, BorderLayout.NORTH);
        root.add(info, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        add(root);

        setActionButtons(false);
        getRootPane().setDefaultButton(btnLookup);

        btnLookup.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadAccount();
            }
        });
        btnDeposit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new DepositFrame(currentAccount).setVisible(true);
            }
        });
        btnWithdraw.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new WithdrawalFrame(currentAccount).setVisible(true);
            }
        });
        btnHistory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new TransactionHistoryFrame(currentAccount).setVisible(true);
            }
        });
        btnClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        pack();
        setSize(Math.max(getWidth(), 520), getHeight());
        setLocationRelativeTo(null);

        if (accountNumber != null) {
            txtAccount.setText(accountNumber);
            loadAccount();
        }
    }

    private void setActionButtons(boolean enabled) {
        btnDeposit.setEnabled(enabled);
        btnWithdraw.setEnabled(enabled);
        btnHistory.setEnabled(enabled);
    }

    private void clearLabels() {
        currentAccount = null;
        lblAccount.setText("-");
        lblType.setText("-");
        lblStatus.setText("-");
        lblBalance.setText("-");
        lblOpened.setText("-");
        lblCustomer.setText("-");
        lblEmail.setText("-");
        lblPhone.setText("-");
        setActionButtons(false);
    }

    private void loadAccount() {
        clearLabels();
        String accountNumber;
        try {
            accountNumber = BankUtil.checkAccountNumber(txtAccount.getText(), "account number");
        } catch (BankingException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Account Details",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(
                    "SELECT A.ACCOUNT_NUMBER, A.ACCOUNT_TYPE, A.STATUS, IFNULL(A.BALANCE, 0) AS BALANCE, "
                    + "substr(A.CREATED_DATE, 1, 10) AS OPENED, "
                    + "C.CUSTOMER_ID, C.CUSTOMER_NAME, IFNULL(C.EMAIL, '-') AS EMAIL, IFNULL(C.PHONE, '-') AS PHONE "
                    + "FROM ACCOUNTS A, CUSTOMERS C "
                    + "WHERE A.CUSTOMER_ID = C.CUSTOMER_ID AND A.ACCOUNT_NUMBER = ?");
            ps.setString(1, accountNumber);
            rs = ps.executeQuery();
            if (rs.next()) {
                currentAccount = rs.getString("ACCOUNT_NUMBER");
                lblAccount.setText(currentAccount);
                lblType.setText(rs.getString("ACCOUNT_TYPE"));
                lblStatus.setText(rs.getString("STATUS"));
                lblBalance.setText(BankUtil.formatMoney(BankUtil.getMoney(rs, "BALANCE")));
                lblOpened.setText(rs.getString("OPENED"));
                lblCustomer.setText(rs.getLong("CUSTOMER_ID") + " - " + rs.getString("CUSTOMER_NAME"));
                lblEmail.setText(rs.getString("EMAIL"));
                lblPhone.setText(rs.getString("PHONE"));
                setActionButtons(true);
                System.out.println("INFO: account details loaded for " + currentAccount);
            } else {
                JOptionPane.showMessageDialog(this, "Account " + accountNumber + " was not found.",
                        "Account Details", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            System.out.println("ERROR: account lookup failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Account Details", JOptionPane.ERROR_MESSAGE);
        } finally {
            DBConnection.close(rs, ps, conn);
        }
    }
}
