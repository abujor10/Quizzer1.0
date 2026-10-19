package server;

import common.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class QuizServer {
    // Map of active games keyed by 4-digit room code
    private static Map<String, GameState> activeGames = new HashMap<String, GameState>();

    /**
     * Server entry point: binds server socket and accepts incoming connections.
     */
    public static void main(String[] args) {
        System.out.println("=== Quizzer Server Starting ===");
        try (ServerSocket serverSocket = new ServerSocket(Protocol.PORT)) {
            System.out.println("Quizzer Server listening on port " + Protocol.PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());

                // Spawn a dedicated thread for every connection
                ClientHandler handler = new ClientHandler(clientSocket);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    /**
     * Creates a new game instance identified by a random 4-digit code.
     */
    public static synchronized GameState createRoom(ClientHandler teacherHandler) {
        String code = generateRoomCode();
        GameState game = new GameState(code, teacherHandler);
        activeGames.put(code, game);
        System.out.println("Created Game Room: " + code);
        return game;
    }

    /**
     * Finds an ongoing game by room code.
     */
    public static synchronized GameState getRoom(String code) {
        return activeGames.get(code);
    }

    /**
     * Generates a 4-digit numeric room code.
     */
    private static String generateRoomCode() {
        Random rand = new Random();
        int num = 1000 + rand.nextInt(9000);
        return String.valueOf(num);
    }
}
