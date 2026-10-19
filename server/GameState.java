package server;

import common.DB;
import common.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState {
    private String roomCode;
    private int currentQuestionIndex;
    private List<Question> questions;
    private Map<String, Integer> scores;
    private Map<String, Integer> cheatCounts;
    private ClientHandler teacherHandler;
    private List<ClientHandler> studentHandlers;

    /**
     * Initializes a new quiz room with a unique room code.
     */
    public GameState(String roomCode, ClientHandler teacherHandler) {
        this.roomCode = roomCode;
        this.teacherHandler = teacherHandler;
        this.currentQuestionIndex = -1;
        this.questions = new ArrayList<Question>();
        this.scores = new HashMap<String, Integer>();
        this.cheatCounts = new HashMap<String, Integer>();
        this.studentHandlers = new ArrayList<ClientHandler>();
        loadQuestionsFromDatabase();
    }

    /**
     * Queries up to 20 questions from the MySQL database.
     */
    private void loadQuestionsFromDatabase() {
        String query = "SELECT id, text, optA, optB, optC, optD, correct, time_limit FROM questions LIMIT 20";
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Question q = new Question(
                        rs.getInt("id"),
                        rs.getString("text"),
                        rs.getString("optA"),
                        rs.getString("optB"),
                        rs.getString("optC"),
                        rs.getString("optD"),
                        rs.getString("correct"),
                        rs.getInt("time_limit")
                );
                questions.add(q);
            }
        } catch (SQLException e) {
            System.err.println("Database load error: " + e.getMessage());
        }
    }

    /**
     * Registers a new student into the active game session.
     */
    public synchronized void addStudent(String name, ClientHandler handler) {
        studentHandlers.add(handler);
        scores.put(name, 0);
        cheatCounts.put(name, 0);
    }

    /**
     * Returns a comma-separated list of joined student names.
     */
    public synchronized String getStudentListString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < studentHandlers.size(); i++) {
            sb.append(studentHandlers.get(i).getClientName());
            if (i < studentHandlers.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Increments the cheat attempt counter for a specific student.
     */
    public synchronized void recordCheat(String studentName) {
        if (cheatCounts.containsKey(studentName)) {
            int current = cheatCounts.get(studentName);
            cheatCounts.put(studentName, current + 1);
        }
    }

    /**
     * Verifies the answer and computes score based on response speed.
     */
    public synchronized void recordAnswer(String studentName, String answer, int timeTakenSeconds) {
        Question q = getCurrentQuestion();
        if (q == null) return;

        if (q.getCorrect().equalsIgnoreCase(answer)) {
            // Speed bonus formula: faster response yields higher points
            int earnedPoints = 1000 - (timeTakenSeconds * (500 / Math.max(1, q.getTimeLimit())));
            if (earnedPoints < 500) earnedPoints = 500;

            int total = scores.getOrDefault(studentName, 0) + earnedPoints;
            scores.put(studentName, total);
        }
    }

    /**
     * Advances to the next question. Returns null if quiz is finished.
     */
    public synchronized Question nextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex < questions.size()) {
            return questions.get(currentQuestionIndex);
        }
        return null;
    }

    /**
     * Returns the question currently being solved.
     */
    public synchronized Question getCurrentQuestion() {
        if (currentQuestionIndex >= 0 && currentQuestionIndex < questions.size()) {
            return questions.get(currentQuestionIndex);
        }
        return null;
    }

    /**
     * Builds leaderboard string in format: name:score:cheats,name:score:cheats
     */
    public synchronized String getLeaderboardString(int limit) {
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(scores.entrySet());

        // Sort descending by score
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Integer> entry : list) {
            if (count >= limit) break;
            String name = entry.getKey();
            int score = entry.getValue();
            int cheats = cheatCounts.getOrDefault(name, 0);
            sb.append(name).append(":").append(score).append(":").append(cheats).append(",");
            count++;
        }

        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Saves final game outcomes to the MySQL results table.
     */
    public synchronized void saveResultsToDatabase() {
        String sql = "INSERT INTO results (room_code, student_name, score, cheat_count) VALUES (?, ?, ?, ?)";
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                String name = entry.getKey();
                int score = entry.getValue();
                int cheats = cheatCounts.getOrDefault(name, 0);

                stmt.setString(1, roomCode);
                stmt.setString(2, name);
                stmt.setInt(3, score);
                stmt.setInt(4, cheats);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Failed to save quiz results: " + e.getMessage());
        }
    }

    public String getRoomCode() { return roomCode; }
    public ClientHandler getTeacherHandler() { return teacherHandler; }
    public List<ClientHandler> getStudentHandlers() { return studentHandlers; }
}