package com.legacybank;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Screen 2 - Main dashboard. Menu bar and buttons open the other screens.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public MainFrame() {
        super("Legacy Bank - Main Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ----- menu bar -----
        JMenuBar menuBar = new JMenuBar();

        JMenu mnuFile = new JMenu("File");
        JMenuItem miLogout = new JMenuItem("Logout");
        JMenuItem miExit = new JMenuItem("Exit");
        mnuFile.add(miLogout);
        mnuFile.addSeparator();
        mnuFile.add(miExit);

        JMenu mnuCustomers = new JMenu("Customers");
        JMenuItem miSearch = new JMenuItem("Customer Search");
        JMenuItem miAccount = new JMenuItem("Account Details");
        mnuCustomers.add(miSearch);
        mnuCustomers.add(miAccount);

        JMenu mnuTxn = new JMenu("Transactions");
        JMenuItem miDeposit = new JMenuItem("Deposit");
        JMenuItem miWithdraw = new JMenuItem("Withdrawal");
        JMenuItem miTransfer = new JMenuItem("Fund Transfer");
        JMenuItem miHistory = new JMenuItem("Transaction History");
        mnuTxn.add(miDeposit);
        mnuTxn.add(miWithdraw);
        mnuTxn.add(miTransfer);
        mnuTxn.addSeparator();
        mnuTxn.add(miHistory);

        JMenu mnuHelp = new JMenu("Help");
        JMenuItem miAbout = new JMenuItem("About");
        mnuHelp.add(miAbout);

        menuBar.add(mnuFile);
        menuBar.add(mnuCustomers);
        menuBar.add(mnuTxn);
        menuBar.add(mnuHelp);
        setJMenuBar(menuBar);

        // ----- body -----
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 5, 20));
        JLabel lblTitle = new JLabel("LEGACY BANK - Branch Operations", SwingConstants.CENTER);
        lblTitle.setFont(lblTitle.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        JLabel lblUser = new JLabel("Logged in as: " + AppSession.employeeName
                + " (" + AppSession.username + ")", SwingConstants.CENTER);
        header.add(lblTitle);
        header.add(lblUser);

        JPanel buttons = new JPanel(new GridLayout(3, 2, 12, 12));
        buttons.setBorder(BorderFactory.createEmptyBorder(15, 40, 25, 40));
        JButton btnSearch = new JButton("Customer Search");
        JButton btnAccount = new JButton("Account Details");
        JButton btnDeposit = new JButton("Cash Deposit");
        JButton btnWithdraw = new JButton("Cash Withdrawal");
        JButton btnTransfer = new JButton("Fund Transfer");
        JButton btnHistory = new JButton("Transaction History");
        buttons.add(btnSearch);
        buttons.add(btnAccount);
        buttons.add(btnDeposit);
        buttons.add(btnWithdraw);
        buttons.add(btnTransfer);
        buttons.add(btnHistory);

        add(header, BorderLayout.NORTH);
        add(buttons, BorderLayout.CENTER);

        // ----- listeners -----
        ActionListener openSearch = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new CustomerSearchFrame().setVisible(true);
            }
        };
        ActionListener openAccount = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new AccountDetailsFrame(null).setVisible(true);
            }
        };
        ActionListener openDeposit = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new DepositFrame(null).setVisible(true);
            }
        };
        ActionListener openWithdraw = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new WithdrawalFrame(null).setVisible(true);
            }
        };
        ActionListener openTransfer = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new TransferFrame(null).setVisible(true);
            }
        };
        ActionListener openHistory = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new TransactionHistoryFrame(null).setVisible(true);
            }
        };

        btnSearch.addActionListener(openSearch);
        miSearch.addActionListener(openSearch);
        btnAccount.addActionListener(openAccount);
        miAccount.addActionListener(openAccount);
        btnDeposit.addActionListener(openDeposit);
        miDeposit.addActionListener(openDeposit);
        btnWithdraw.addActionListener(openWithdraw);
        miWithdraw.addActionListener(openWithdraw);
        btnTransfer.addActionListener(openTransfer);
        miTransfer.addActionListener(openTransfer);
        btnHistory.addActionListener(openHistory);
        miHistory.addActionListener(openHistory);

        miLogout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("INFO: employee logged out: " + AppSession.username);
                AppSession.clear();
                new LoginFrame().setVisible(true);
                dispose();
            }
        });
        miExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        miAbout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "Legacy Banking Transaction Management System\n"
                        + "Java Swing + JDBC + SQLite Database\nVersion 1.0.0",
                        "About", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        setMinimumSize(new Dimension(520, 360));
        pack();
        setLocationRelativeTo(null);
    }
}
