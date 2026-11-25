package com.legacybank;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Screen 8 - Transaction history by account number, optional type filter,
 * latest transactions first.
 */
public class TransactionHistoryFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String ALL_TYPES = "ALL";

    private JTextField txtAccount = new JTextField(14);
    private JComboBox<String> cboType = new JComboBox<String>(new String[] {
            ALL_TYPES, "DEPOSIT", "WITHDRAWAL", "TRANSFER_DEBIT", "TRANSFER_CREDIT" });
    private JButton btnSearch = new JButton("Search");
    private JButton btnClose = new JButton("Close");
    private JLabel lblCount = new JLabel(" ");

    private DefaultTableModel model = new DefaultTableModel(
            new String[] { "Transaction ID", "Type", "Amount", "Date", "Reference", "Description" }, 0) {
        private static final long serialVersionUID = 1L;

        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private JTable table = new JTable(model);

    public TransactionHistoryFrame(String accountNumber) {
        super("Transaction History");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Account Number:"));
        top.add(txtAccount);
        top.add(new JLabel("Type:"));
        top.add(cboType);
        top.add(btnSearch);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(820, 320));
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setPreferredWidth(300);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(lblCount, BorderLayout.WEST);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(btnClose);
        bottom.add(btnPanel, BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        add(root);

        getRootPane().setDefaultButton(btnSearch);

        btnSearch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSearch();
            }
        });
        btnClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        pack();
        setLocationRelativeTo(null);

        if (accountNumber != null) {
            txtAccount.setText(accountNumber);
            doSearch();
        }
    }

    private void doSearch() {
        model.setRowCount(0);
        lblCount.setText(" ");
        String accountNumber;
        try {
            accountNumber = BankUtil.checkAccountNumber(txtAccount.getText(), "account number");
        } catch (BankingException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Transaction History",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String type = (String) cboType.getSelectedItem();

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT TRANSACTION_ID, TRANSACTION_TYPE, AMOUNT, ");
        sql.append("TRANSACTION_DATE AS TXN_DATE, ");
        sql.append("REFERENCE_NUMBER, IFNULL(DESCRIPTION, ' ') AS DESCRIPTION ");
        sql.append("FROM TRANSACTIONS WHERE ACCOUNT_NUMBER = ? ");
        if (!ALL_TYPES.equals(type)) {
            sql.append("AND TRANSACTION_TYPE = ? ");
        }
        sql.append("ORDER BY TRANSACTION_DATE DESC, TRANSACTION_ID DESC");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql.toString());
            ps.setString(1, accountNumber);
            if (!ALL_TYPES.equals(type)) {
                ps.setString(2, type);
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[] {
                        Long.valueOf(rs.getLong("TRANSACTION_ID")),
                        rs.getString("TRANSACTION_TYPE"),
                        BankUtil.formatMoney(BankUtil.getMoney(rs, "AMOUNT")),
                        rs.getString("TXN_DATE"),
                        rs.getString("REFERENCE_NUMBER"),
                        rs.getString("DESCRIPTION") });
            }
            lblCount.setText(model.getRowCount() + " transaction(s) found");
            System.out.println("INFO: history for " + accountNumber + " type " + type
                    + " rows " + model.getRowCount());
        } catch (SQLException ex) {
            System.out.println("ERROR: history search failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Transaction History", JOptionPane.ERROR_MESSAGE);
        } finally {
            DBConnection.close(rs, ps, conn);
        }
    }
}
