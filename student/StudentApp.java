package student;

import common.Protocol;
import common.Theme;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.*;

public class StudentApp extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private CardLayout cardLayout;
    private JPanel mainCardPanel;

    private String studentName;
    private boolean isQuestionActive = false;
    private int questionTimeLimit = 15;
    private int timeRemaining = 15;
    private Timer countdownTimer;

    private JLabel lblStatus;
    private JLabel lblQuestionText;
    private JProgressBar progressBar;
    private JButton btnA;
    private JButton btnB;
    private JButton btnC;
    private JButton btnD;
    private JLabel lblCheatWarning;

    /**
     * Initializes Student window, focus listener, and UI cards.
     */
    public StudentApp() {
        // UI Fix: Ensures disabled buttons keep their bright white text instead of turning gray
        UIManager.put("Button.disabledText", Color.WHITE);

        setTitle("Quizzer - Student");
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);

        cardLayout = new CardLayout();
        mainCardPanel = new JPanel(cardLayout);
        mainCardPanel.setBackground(Theme.BG_DARK);

        mainCardPanel.add(buildLoginPanel(), "LOGIN");
        mainCardPanel.add(buildWaitingPanel(), "WAITING");
        mainCardPanel.add(buildGamePanel(), "GAME");

        add(mainCardPanel, BorderLayout.CENTER);

        setupAntiCheatFocusListener();
        connectToServer();
        startListeningThread();
    }

    /**
     * Login view to submit room PIN and nickname.
     */
    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Join Quizzer Game", SwingConstants.CENTER);
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.TEXT_LIGHT);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(lblTitle, gbc);

        JLabel lblPin = new JLabel("Game PIN:");
        lblPin.setForeground(Theme.TEXT_LIGHT);
        lblPin.setFont(Theme.FONT_BODY);
        gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(lblPin, gbc);

        JTextField txtPin = new JTextField(10);
        txtPin.setFont(Theme.FONT_BODY);
        gbc.gridx = 1;
        panel.add(txtPin, gbc);

        JLabel lblName = new JLabel("Your Name:");
        lblName.setForeground(Theme.TEXT_LIGHT);
        lblName.setFont(Theme.FONT_BODY);
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(lblName, gbc);

        JTextField txtName = new JTextField(10);
        txtName.setFont(Theme.FONT_BODY);
        gbc.gridx = 1;
        panel.add(txtName, gbc);

        JButton btnJoin = new JButton("Enter Room");
        btnJoin.setFont(Theme.FONT_BUTTON);
        btnJoin.setBackground(Theme.ACCENT_GREEN);
        btnJoin.setForeground(Color.BLACK);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        btnJoin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String pin = txtPin.getText().trim();
                String name = txtName.getText().trim();
                if (!pin.isEmpty() && !name.isEmpty()) {
                    studentName = name;
                    out.println(Protocol.CMD_JOIN + Protocol.SEP + pin + Protocol.SEP + name);
                } else {
                    JOptionPane.showMessageDialog(StudentApp.this, "Please enter both PIN and Name!");
                }
            }
        });
        panel.add(btnJoin, gbc);

        return panel;
    }

    /**
     * Waiting lobby view.
     */
    private JPanel buildWaitingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_DARK);

        lblStatus = new JLabel("You're in! Waiting for teacher to start...", SwingConstants.CENTER);
        lblStatus.setFont(Theme.FONT_TITLE);
        lblStatus.setForeground(Theme.ACCENT_GREEN);
        panel.add(lblStatus, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Question solving view with countdown and Kahoot answer buttons.
     */
    private JPanel buildGamePanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topContainer = new JPanel(new BorderLayout(10, 10));
        topContainer.setBackground(Theme.BG_DARK);

        lblQuestionText = new JLabel("Question will appear here...", SwingConstants.CENTER);
        lblQuestionText.setFont(Theme.FONT_QUESTION);
        lblQuestionText.setForeground(Theme.TEXT_LIGHT);
        topContainer.add(lblQuestionText, BorderLayout.NORTH);

        progressBar = new JProgressBar();
        progressBar.setForeground(Theme.ACCENT_GREEN);
        progressBar.setBackground(Theme.BG_CARD);
        progressBar.setPreferredSize(new Dimension(100, 20));
        topContainer.add(progressBar, BorderLayout.CENTER);

        lblCheatWarning = new JLabel("", SwingConstants.CENTER);
        lblCheatWarning.setFont(Theme.FONT_BODY);
        lblCheatWarning.setForeground(Theme.OPTION_A);
        topContainer.add(lblCheatWarning, BorderLayout.SOUTH);

        panel.add(topContainer, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        gridPanel.setBackground(Theme.BG_DARK);

        btnA = createAnswerButton(Theme.OPTION_A);
        btnB = createAnswerButton(Theme.OPTION_B);
        btnC = createAnswerButton(Theme.OPTION_C);
        btnD = createAnswerButton(Theme.OPTION_D);

        btnA.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { submitAnswer("A"); }
        });
        btnB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { submitAnswer("B"); }
        });
        btnC.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { submitAnswer("C"); }
        });
        btnD.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { submitAnswer("D"); }
        });

        gridPanel.add(btnA);
        gridPanel.add(btnB);
        gridPanel.add(btnC);
        gridPanel.add(btnD);

        panel.add(gridPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Anti-cheat focus detector: logs cheating when user switches windows during active question.
     */
    private void setupAntiCheatFocusListener() {
        this.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {}

            @Override
            public void windowLostFocus(WindowEvent e) {
                if (isQuestionActive) {
                    out.println(Protocol.CMD_CHEAT + Protocol.SEP + studentName);
                    lblCheatWarning.setText("WARNING: Tab/Window switch detected! Cheating logged.");
                }
            }
        });
    }

    /**
     * Styled answer button creator.
     */
    private JButton createAnswerButton(Color color) {
        JButton btn = new JButton();
        btn.setFont(Theme.FONT_BUTTON);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        return btn;
    }

    /**
     * Sends student choice and calculated elapsed time to server.
     */
    private void submitAnswer(String choice) {
        if (!isQuestionActive) return;

        isQuestionActive = false;
        if (countdownTimer != null) countdownTimer.stop();

        int timeTaken = questionTimeLimit - timeRemaining;
        out.println(Protocol.CMD_ANSWER + Protocol.SEP + choice + Protocol.SEP + timeTaken);
        setAnswerButtonsEnabled(false);
        lblQuestionText.setText("Answer Submitted! Waiting for next question...");
    }

    /**
     * Enables or disables choice buttons.
     */
    private void setAnswerButtonsEnabled(boolean enabled) {
        btnA.setEnabled(enabled);
        btnB.setEnabled(enabled);
        btnC.setEnabled(enabled);
        btnD.setEnabled(enabled);
    }

    /**
     * Connects to QuizServer on port 5000.
     */
    private void connectToServer() {
        try {
            socket = new Socket("localhost", Protocol.PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "QuizServer is not running!", "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Background message handler.
     */
    private void startListeningThread() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        String[] tokens = line.split(Protocol.DELIMITER);
                        String action = tokens[0];

                        if (Protocol.RES_JOINED.equals(action)) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    cardLayout.show(mainCardPanel, "WAITING");
                                }
                            });
                        } else if (Protocol.RES_QUESTION.equals(action)) {
                            final String qText = tokens[1];
                            final String optA = tokens[2];
                            final String optB = tokens[3];
                            final String optC = tokens[4];
                            final String optD = tokens[5];
                            final int limit = Integer.parseInt(tokens[6]);

                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    displayNewQuestion(qText, optA, optB, optC, optD, limit);
                                }
                            });
                        } else if (Protocol.RES_FINAL.equals(action)) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    if (countdownTimer != null) countdownTimer.stop();
                                    isQuestionActive = false;
                                    lblQuestionText.setText("Quiz Finished! Check Teacher's screen for standings.");
                                    // Bugfix: hide progress bar and disable answers at final screen
                                    progressBar.setVisible(false);
                                    lblCheatWarning.setText("");
                                    setAnswerButtonsEnabled(false);
                                }
                            });
                        } else if (Protocol.RES_ERROR.equals(action)) {
                            final String msg = tokens.length > 1 ? tokens[1] : "Error";
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    JOptionPane.showMessageDialog(StudentApp.this, msg);
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Student connection lost.");
                }
            }
        });
        thread.start();
    }

    /**
     * Resets and starts question display.
     */
    private void displayNewQuestion(String q, String a, String b, String c, String d, int limit) {
        cardLayout.show(mainCardPanel, "GAME");
        lblQuestionText.setText(q);
        btnA.setText("A: " + a);
        btnB.setText("B: " + b);
        btnC.setText("C: " + c);
        btnD.setText("D: " + d);

        lblCheatWarning.setText("");
        setAnswerButtonsEnabled(true);
        progressBar.setVisible(true);

        this.questionTimeLimit = limit;
        this.timeRemaining = limit;
        this.isQuestionActive = true;

        progressBar.setMaximum(limit);
        progressBar.setValue(limit);

        if (countdownTimer != null) countdownTimer.stop();

        countdownTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timeRemaining--;
                progressBar.setValue(timeRemaining);

                if (timeRemaining <= 0) {
                    countdownTimer.stop();
                    isQuestionActive = false;
                    setAnswerButtonsEnabled(false);
                    lblQuestionText.setText("Time's Up!");
                }
            }
        });
        countdownTimer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new StudentApp().setVisible(true);
            }
        });
    }
}
