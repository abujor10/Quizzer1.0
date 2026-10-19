package common;

public class Question {
    private int id;
    private String text;
    private String optA;
    private String optB;
    private String optC;
    private String optD;
    private String correct;
    private int timeLimit;

    /**
     * Constructor to initialize a complete question object.
     */
    public Question(int id, String text, String optA, String optB, String optC, String optD, String correct, int timeLimit) {
        this.id = id;
        this.text = text;
        this.optA = optA;
        this.optB = optB;
        this.optC = optC;
        this.optD = optD;
        this.correct = correct;
        this.timeLimit = timeLimit;
    }

    /**
     * Formats the question for network transmission according to the protocol.
     */
    public String toProtocolString() {
        return Protocol.RES_QUESTION + Protocol.SEP +
                text + Protocol.SEP +
                optA + Protocol.SEP +
                optB + Protocol.SEP +
                optC + Protocol.SEP +
                optD + Protocol.SEP +
                timeLimit;
    }

    public int getId() { return id; }
    public String getText() { return text; }
    public String getOptA() { return optA; }
    public String getOptB() { return optB; }
    public String getOptC() { return optC; }
    public String getOptD() { return optD; }
    public String getCorrect() { return correct; }
    public int getTimeLimit() { return timeLimit; }
}
