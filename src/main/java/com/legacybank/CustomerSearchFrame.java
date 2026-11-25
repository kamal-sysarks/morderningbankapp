package com.legacybank;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Screen 3 - Customer search by customer ID or account number.
 * Shows customer details and all of the customer's accounts.
 */
public class CustomerSearchFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private JComboBox<String> cboSearchBy = new JComboBox<String>(new String[] { "Customer ID", "Account Number" });
    private JTextField txtSearch = new JTextField(16);
    private JButton btnSearch = new JButton("Search");
    private JButton btnDetails = new JButton("Account Details");
    private JButton btnClose = new JButton("Close");

    private JLabel lblId = new JLabel("-");
    private JLabel lblName = new JLabel("-");
    private JLabel lblEmail = new JLabel("-");
    private JLabel lblPhone = new JLabel("-");
    private JLabel lblAddress = new JLabel("-");

    private DefaultTableModel model = new DefaultTableModel(
            new String[] { "Account Number", "Type", "Status", "Available Balance" }, 0) {
        private static final long serialVersionUID = 1L;

        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private JTable table = new JTable(model);

    public CustomerSearchFrame() {
        super("Customer Search");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search by:"));
        top.add(cboSearchBy);
        top.add(txtSearch);
        top.add(btnSearch);

        JPanel info = new JPanel(new GridLayout(5, 2, 4, 4));
        info.setBorder(BorderFactory.createTitledBorder("Customer"));
        info.add(new JLabel("Customer ID:"));
        info.add(lblId);
        info.add(new JLabel("Name:"));
        info.add(lblName);
        info.add(new JLabel("Email:"));
        info.add(lblEmail);
        info.add(new JLabel("Phone:"));
        info.add(lblPhone);
        info.add(new JLabel("Address:"));
        info.add(lblAddress);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Accounts"));
        scroll.setPreferredSize(new Dimension(600, 160));

        JPanel center = new JPanel(new BorderLayout());
        center.add(info, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnDetails);
        bottom.add(btnClose);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(top, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        add(root);

        getRootPane().setDefaultButton(btnSearch);

        btnSearch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSearch();
            }
        });
        btnDetails.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(CustomerSearchFrame.this,
                            "Select an account in the table first.", "Account Details",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                new AccountDetailsFrame((String) model.getValueAt(row, 0)).setVisible(true);
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

    private void clearResults() {
        lblId.setText("-");
        lblName.setText("-");
        lblEmail.setText("-");
        lblPhone.setText("-");
        lblAddress.setText("-");
        model.setRowCount(0);
    }

    private void doSearch() {
        String text = txtSearch.getText().trim();
        if (text.length() == 0) {
            JOptionPane.showMessageDialog(this, "Please enter a search value.",
                    "Customer Search", JOptionPane.WARNING_MESSAGE);
            return;
        }
        clearResults();

        boolean byCustomerId = cboSearchBy.getSelectedIndex() == 0;
        long customerId = 0;
        if (byCustomerId) {
            try {
                customerId = Long.parseLong(text);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Customer ID must be a number.",
                        "Customer Search", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();

            if (byCustomerId) {
                ps = conn.prepareStatement(
                        "SELECT CUSTOMER_ID, CUSTOMER_NAME, EMAIL, PHONE, ADDRESS "
                        + "FROM CUSTOMERS WHERE CUSTOMER_ID = ?");
                ps.setLong(1, customerId);
            } else {
                ps = conn.prepareStatement(
                        "SELECT C.CUSTOMER_ID, C.CUSTOMER_NAME, C.EMAIL, C.PHONE, C.ADDRESS "
                        + "FROM CUSTOMERS C, ACCOUNTS A "
                        + "WHERE C.CUSTOMER_ID = A.CUSTOMER_ID AND A.ACCOUNT_NUMBER = ?");
                ps.setString(1, text);
            }
            rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("INFO: customer search found nothing for " + text);
                JOptionPane.showMessageDialog(this, "No customer found.",
                        "Customer Search", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            long foundId = rs.getLong("CUSTOMER_ID");
            lblId.setText(String.valueOf(foundId));
            lblName.setText(rs.getString("CUSTOMER_NAME"));
            lblEmail.setText(rs.getString("EMAIL"));
            lblPhone.setText(rs.getString("PHONE"));
            lblAddress.setText(rs.getString("ADDRESS"));
            DBConnection.close(rs, ps);

            ps = conn.prepareStatement(
                    "SELECT ACCOUNT_NUMBER, ACCOUNT_TYPE, STATUS, IFNULL(BALANCE, 0) AS BALANCE "
                    + "FROM ACCOUNTS WHERE CUSTOMER_ID = ? ORDER BY ACCOUNT_NUMBER");
            ps.setLong(1, foundId);
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getString("ACCOUNT_NUMBER"),
                        rs.getString("ACCOUNT_TYPE"),
                        rs.getString("STATUS"),
                        BankUtil.formatMoney(BankUtil.getMoney(rs, "BALANCE")) });
            }
            System.out.println("INFO: customer search OK, customer " + foundId + ", accounts " + model.getRowCount());
        } catch (SQLException ex) {
            System.out.println("ERROR: customer search failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Customer Search", JOptionPane.ERROR_MESSAGE);
        } finally {
            DBConnection.close(rs, ps, conn);
        }
    }
}
