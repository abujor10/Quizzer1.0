package server;

import common.Protocol;
import common.Question;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;
    private String role; // "TEACHER" or "STUDENT"
    private GameState currentGame;

    /**
     * Prepares reader and writer streams on the accepted client socket.
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Handler setup error: " + e.getMessage());
        }
    }

    /**
     * Continuously listens for incoming network lines from this client.
     */
    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                processMessage(line);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + clientName);
        } finally {
            closeConnection();
        }
    }

    /**
     * Parses protocol tokens and triggers game updates.
     */
    private void processMessage(String message) {
        String[] tokens = message.split(Protocol.DELIMITER);
        String action = tokens[0];

        if (Protocol.CMD_CREATE.equals(action)) {
            // Teacher initiates a new room
            this.role = "TEACHER";
            this.clientName = "Teacher";
            this.currentGame = QuizServer.createRoom(this);
            sendMessage(Protocol.RES_CREATED + Protocol.SEP + currentGame.getRoomCode());

        } else if (Protocol.CMD_JOIN.equals(action)) {
            // Student attempts to join: JOIN|<roomCode>|<studentName>
            String code = tokens[1];
            String name = tokens[2];
            GameState game = QuizServer.getRoom(code);

            if (game != null) {
                this.role = "STUDENT";
                this.clientName = name;
                this.currentGame = game;
                game.addStudent(name, this);
                sendMessage(Protocol.RES_JOINED + Protocol.SEP + code);

                // Notify teacher with updated student roster
                game.getTeacherHandler().sendMessage(Protocol.RES_STUDENT_LIST + Protocol.SEP + game.getStudentListString());
            } else {
                sendMessage(Protocol.RES_ERROR + Protocol.SEP + "Invalid Room Code!");
            }

        } else if (Protocol.CMD_START.equals(action) || Protocol.CMD_NEXT.equals(action)) {
            // Teacher moves the quiz to the next question
            if (currentGame != null) {
                Question nextQ = currentGame.nextQuestion();
                if (nextQ != null) {
                    // Broadcast question to students and teacher
                    String qMsg = nextQ.toProtocolString();
                    broadcastToRoom(qMsg);
                } else {
                    // Quiz ended: display final results and persist to DB
                    String finalScores = currentGame.getLeaderboardString(100);
                    broadcastToRoom(Protocol.RES_FINAL + Protocol.SEP + finalScores);
                    currentGame.saveResultsToDatabase();
                }
            }

        } else if (Protocol.CMD_ANSWER.equals(action)) {
            // Student answers: ANSWER|<choice>|<timeTaken>
            if (currentGame != null) {
                String choice = tokens[1];
                int timeTaken = Integer.parseInt(tokens[2]);
                currentGame.recordAnswer(clientName, choice, timeTaken);

                // Immediately send live top 5 update to the teacher
                String lb = currentGame.getLeaderboardString(5);
                currentGame.getTeacherHandler().sendMessage(Protocol.RES_LEADERBOARD + Protocol.SEP + lb);
            }

        } else if (Protocol.CMD_CHEAT.equals(action)) {
            // Focus loss reported: CHEAT|<studentName>
            if (currentGame != null) {
                currentGame.recordCheat(clientName);
                String lb = currentGame.getLeaderboardString(5);
                currentGame.getTeacherHandler().sendMessage(Protocol.RES_LEADERBOARD + Protocol.SEP + lb);
            }
        }
    }

    /**
     * Broadcasts a protocol packet to every connected participant in this game.
     */
    private void broadcastToRoom(String msg) {
        if (currentGame == null) return;
        currentGame.getTeacherHandler().sendMessage(msg);
        for (ClientHandler student : currentGame.getStudentHandlers()) {
            student.sendMessage(msg);
        }
    }

    /**
     * Sends a single raw text line over the network stream.
     */
    public void sendMessage(String msg) {
        out.println(msg);
    }

    /**
     * Closes socket and IO handles gracefully.
     */
    public void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getClientName() {
        return clientName;
    }
}
