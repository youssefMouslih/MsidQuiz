package com.mslh.quizapp;

import java.util.List;

public class Question {
    private String id;
    private String question;
    private String image;
    private String type;
    private int timeLimit;
    private boolean shuffleAnswers;
    private List<Answer> answers;
    private String hint;
    private String description;
    private int points;

    public String getId() { return id; }
    public String getQuestion() { return question; }
    public String getImage() { return image; }
    public String getType() { return type; }
    public int getTimeLimit() { return timeLimit; }
    public boolean isShuffleAnswers() { return shuffleAnswers; }
    public List<Answer> getAnswers() { return answers; }
    public String getHint() { return hint; }
    public String getDescription() { return description; }
    public int getPoints() { return points; }
}
