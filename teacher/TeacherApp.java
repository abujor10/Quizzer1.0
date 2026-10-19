package teacher;

import common.DB;
import common.Protocol;
import common.Theme;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class TeacherApp extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private CardLayout cardLayout;
    private JPanel mainCardPanel;

    private JLabel lblRoomCode;
    private JTextArea txtStudentRoster;
    private JLabel lblQuestionDisplay;
    private DefaultTableModel leaderboardModel;
    private JButton btnNextQuestion;

    /**
     * Initializes Teacher window, cards, and networking.
     */
    public TeacherApp() {
        setTitle("Quizzer - Teacher Console");
        setSize(850, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);

        cardLayout = new CardLayout();
        mainCardPanel = new JPanel(cardLayout);
        mainCardPanel.setBackground(Theme.BG_DARK);

        mainCardPanel.add(buildDashboardPanel(), "DASHBOARD");
        mainCardPanel.add(buildLobbyPanel(), "LOBBY");
        mainCardPanel.add(buildLiveGamePanel(), "GAME");

        add(mainCardPanel, BorderLayout.CENTER);

        connectToServer();
        startListeningThread();
    }

    /**
     * Dashboard with game creation and question persistence form.
     */
    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Teacher Control Center", SwingConstants.CENTER);
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_LIGHT);
        panel.add(title, BorderLayout.NORTH);

        JPanel formCard = new JPanel(new GridLayout(8, 2, 8, 8));
        formCard.setBackground(Theme.BG_CARD);
        formCard.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.ACCENT_PINK), "Add New Question to Database",
                0, 0, Theme.FONT_BODY, Theme.TEXT_LIGHT));

        JTextField txtQ = new JTextField();
        JTextField txtA = new JTextField();
        JTextField txtB = new JTextField();
        JTextField txtC = new JTextField();
        JTextField txtD = new JTextField();
        String[] options = {"A", "B", "C", "D"};
        JComboBox<String> cmbCorrect = new JComboBox<String>(options);
        JTextField txtTime = new JTextField("15");

        formCard.add(createStyledLabel("Question Text:")); formCard.add(txtQ);
        formCard.add(createStyledLabel("Option A:")); formCard.add(txtA);
        formCard.add(createStyledLabel("Option B:")); formCard.add(txtB);
        formCard.add(createStyledLabel("Option C:")); formCard.add(txtC);
        formCard.add(createStyledLabel("Option D:")); formCard.add(txtD);
        formCard.add(createStyledLabel("Correct Answer:")); formCard.add(cmbCorrect);
        formCard.add(createStyledLabel("Time Limit (sec):")); formCard.add(txtTime);

        JButton btnSaveQ = new JButton("Save Question to DB");
        btnSaveQ.setBackground(Theme.ACCENT_GREEN);
        btnSaveQ.setForeground(Color.BLACK);
        btnSaveQ.setFont(Theme.FONT_BUTTON);
        btnSaveQ.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveQuestionToDB(txtQ.getText(), txtA.getText(), txtB.getText(),
                        txtC.getText(), txtD.getText(), (String) cmbCorrect.getSelectedItem(), txtTime.getText());
                txtQ.setText(""); txtA.setText(""); txtB.setText(""); txtC.setText(""); txtD.setText("");
            }
        });
        formCard.add(new JLabel("")); formCard.add(btnSaveQ);
        panel.add(formCard, BorderLayout.CENTER);

        JButton btnCreate = new JButton("Create New Game Room");
        btnCreate.setPreferredSize(new Dimension(200, 50));
        btnCreate.setBackground(Theme.ACCENT_PINK);
        btnCreate.setForeground(Color.WHITE);
        btnCreate.setFont(Theme.FONT_BUTTON);
        btnCreate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (out != null) out.println(Protocol.CMD_CREATE);
            }
        });
        panel.add(btnCreate, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Lobby with room code and live student roster.
     */
    private JPanel buildLobbyPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.setBackground(Theme.BG_DARK);

        JLabel lblTitle = new JLabel("Join with Code:", SwingConstants.CENTER);
        lblTitle.setFont(Theme.FONT_BODY);
        lblTitle.setForeground(Theme.TEXT_LIGHT);

        lblRoomCode = new JLabel("----", SwingConstants.CENTER);
        lblRoomCode.setFont(new Font("Segoe UI", Font.BOLD, 48));
        lblRoomCode.setForeground(Theme.ACCENT_GREEN);

        topPanel.add(lblTitle);
        topPanel.add(lblRoomCode);
        panel.add(topPanel, BorderLayout.NORTH);

        txtStudentRoster = new JTextArea();
        txtStudentRoster.setEditable(false);
        txtStudentRoster.setBackground(Theme.BG_CARD);
        txtStudentRoster.setForeground(Theme.TEXT_LIGHT);
        txtStudentRoster.setFont(Theme.FONT_BODY);
        txtStudentRoster.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.ACCENT_PINK), "Connected Students",
                0, 0, Theme.FONT_BODY, Theme.TEXT_LIGHT));

        panel.add(new JScrollPane(txtStudentRoster), BorderLayout.CENTER);

        JButton btnStart = new JButton("Start Quiz");
        btnStart.setPreferredSize(new Dimension(200, 50));
        btnStart.setBackground(Theme.ACCENT_GREEN);
        btnStart.setForeground(Color.BLACK);
        btnStart.setFont(Theme.FONT_BUTTON);
        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (out != null) {
                    out.println(Protocol.CMD_START);
                    cardLayout.show(mainCardPanel, "GAME");
                }
            }
        });
        panel.add(btnStart, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Game panel containing live question details, standings, and next control.
     */
    private JPanel buildLiveGamePanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        lblQuestionDisplay = new JLabel("Quiz Started...", SwingConstants.CENTER);
        lblQuestionDisplay.setFont(Theme.FONT_QUESTION);
        lblQuestionDisplay.setForeground(Theme.TEXT_LIGHT);
        panel.add(lblQuestionDisplay, BorderLayout.NORTH);

        String[] columns = {"Rank", "Student Name", "Score", "Cheat Status"};
        leaderboardModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(leaderboardModel);
        table.setRowHeight(30);
        table.setFont(Theme.FONT_BODY);
        table.getTableHeader().setFont(Theme.FONT_BUTTON);
        table.setBackground(Theme.BG_CARD);
        table.setForeground(Theme.TEXT_LIGHT);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        btnNextQuestion = new JButton("Next Question");
        btnNextQuestion.setPreferredSize(new Dimension(200, 50));
        btnNextQuestion.setBackground(Theme.ACCENT_PINK);
        btnNextQuestion.setForeground(Color.WHITE);
        btnNextQuestion.setFont(Theme.FONT_BUTTON);
        btnNextQuestion.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (out != null) out.println(Protocol.CMD_NEXT);
            }
        });
        panel.add(btnNextQuestion, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Connects socket to QuizServer port 5000.
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
     * Listens for server notifications and updates UI safely.
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

                        if (Protocol.RES_CREATED.equals(action)) {
                            final String code = tokens[1];
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    lblRoomCode.setText(code);
                                    cardLayout.show(mainCardPanel, "LOBBY");
                                }
                            });
                        } else if (Protocol.RES_STUDENT_LIST.equals(action)) {
                            final String names = tokens.length > 1 ? tokens[1].replace(",", "\n") : "";
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    txtStudentRoster.setText(names);
                                }
                            });
                        } else if (Protocol.RES_QUESTION.equals(action)) {
                            final String qText = tokens[1];
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    lblQuestionDisplay.setText("Live Question: " + qText);
                                }
                            });
                        } else if (Protocol.RES_LEADERBOARD.equals(action)) {
                            final String rawData = tokens.length > 1 ? tokens[1] : "";
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    updateLeaderboard(rawData);
                                }
                            });
                        } else if (Protocol.RES_FINAL.equals(action)) {
                            final String finalData = tokens.length > 1 ? tokens[1] : "";
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    lblQuestionDisplay.setText("Quiz Ended! Final Standings Saved to DB.");
                                    // Bugfix: hide next question button when finished
                                    btnNextQuestion.setVisible(false);
                                    updateLeaderboard(finalData);
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Teacher disconnected.");
                }
            }
        });
        thread.start();
    }

    /**
     * Updates rows inside leaderboard table.
     */
    private void updateLeaderboard(String rawData) {
        leaderboardModel.setRowCount(0);
        if (rawData.isEmpty()) return;

        String[] players = rawData.split(",");
        int rank = 1;
        for (String player : players) {
            String[] fields = player.split(":");
            if (fields.length >= 3) {
                String name = fields[0];
                String score = fields[1];
                int cheats = Integer.parseInt(fields[2]);
                String cheatAlert = cheats > 0 ? "[!] WARNING (" + cheats + ")" : "Clean";
                leaderboardModel.addRow(new Object[]{rank++, name, score, cheatAlert});
            }
        }
    }

    /**
     * Inserts new questions into database.
     */
    private void saveQuestionToDB(String q, String a, String b, String c, String d, String correct, String timeStr) {
        if (q.trim().isEmpty() || a.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in question fields.");
            return;
        }

        int time = 15;
        try {
            time = Integer.parseInt(timeStr.trim());
        } catch (NumberFormatException ignored) {}

        String sql = "INSERT INTO questions (text, optA, optB, optC, optD, correct, time_limit) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, q);
            stmt.setString(2, a);
            stmt.setString(3, b);
            stmt.setString(4, c);
            stmt.setString(5, d);
            stmt.setString(6, correct);
            stmt.setInt(7, time);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Question saved successfully!");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error saving: " + ex.getMessage());
        }
    }

    /**
     * Creates styled label helper.
     */
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.TEXT_LIGHT);
        label.setFont(Theme.FONT_BODY);
        return label;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new TeacherApp().setVisible(true);
            }
        });
    }
}