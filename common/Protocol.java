package common;

public class Protocol {
    // Port on which the Quizzer server listens
    public static final int PORT = 5000;

    // Commands sent from Teacher
    public static final String CMD_CREATE = "CREATE";
    public static final String CMD_START = "START";
    public static final String CMD_NEXT = "NEXT";

    // Commands sent from Student
    public static final String CMD_JOIN = "JOIN";
    public static final String CMD_ANSWER = "ANSWER";
    public static final String CMD_CHEAT = "CHEAT";

    // Commands broadcasted/sent from Server
    public static final String RES_CREATED = "CREATED";
    public static final String RES_JOINED = "JOINED";
    public static final String RES_STUDENT_LIST = "STUDENTS";
    public static final String RES_QUESTION = "QUESTION";
    public static final String RES_LEADERBOARD = "LEADERBOARD";
    public static final String RES_FINAL = "FINAL";
    public static final String RES_ERROR = "ERROR";

    // Standard delimiter for message tokens
    public static final String DELIMITER = "\\|";
    public static final String SEP = "|";
}