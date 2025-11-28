package com.mslh.quizapp;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private TextView tvQuestion, tvQuestionNumber, tvLiveScore, tvMultiHint;
    private ImageView ivQuestionImage;
    private LinearLayout llAnswersContainer;
    private Button btnSubmit, btnNext, btnBack;

    private List<Question> questionList = new ArrayList<>();
    private int currentIndex = 0;
    private Question currentQuestion;
    private List<Answer> selectedAnswers = new ArrayList<>();
    private int score = 0;
    private int totalQuestions = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Views
        tvQuestion = findViewById(R.id.tvQuestion);
        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
        tvLiveScore = findViewById(R.id.tvLiveScore);
        tvMultiHint = findViewById(R.id.tvMultiHint);
        ivQuestionImage = findViewById(R.id.ivQuestionImage);
        llAnswersContainer = findViewById(R.id.llAnswersContainer);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);

        setQuizViewsVisibility(View.GONE);

        QuizData quizData = loadQuizFromAssets();
        if (quizData != null && quizData.getQuiz() != null && !quizData.getQuiz().isEmpty()) {
            questionList = quizData.getQuiz();
            showQuestionNumberDialog();
        } else {
            Toast.makeText(this, "No questions found!", Toast.LENGTH_SHORT).show();
        }

        btnNext.setOnClickListener(v -> nextQuestion());
        btnBack.setOnClickListener(v -> previousQuestion());
        btnSubmit.setOnClickListener(v -> checkMultipleChoice());
    }

    private void setQuizViewsVisibility(int visibility) {
        tvQuestion.setVisibility(visibility);
        tvQuestionNumber.setVisibility(visibility);
        tvLiveScore.setVisibility(visibility);
        tvMultiHint.setVisibility(visibility);
        ivQuestionImage.setVisibility(visibility);
        llAnswersContainer.setVisibility(visibility);
        btnSubmit.setVisibility(visibility);
        btnNext.setVisibility(visibility);
        btnBack.setVisibility(visibility);
    }

    private void showQuestionNumberDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Number of questions");

        new AlertDialog.Builder(this)
                .setTitle("MSID Quiz")
                .setMessage("How many questions do you want to answer?")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    int numQuestions = value.isEmpty() ? questionList.size() : Integer.parseInt(value);
                    if (numQuestions > questionList.size()) numQuestions = questionList.size();
                    questionList = questionList.subList(0, numQuestions);
                    totalQuestions = numQuestions;
                    currentIndex = 0;

                    setQuizViewsVisibility(View.VISIBLE);
                    loadQuestion(currentIndex);
                })
                .show();
    }

    private void loadQuestion(int index) {
        if (index < 0 || index >= questionList.size()) return;

        currentQuestion = questionList.get(index);
        selectedAnswers.clear();

        btnNext.setVisibility(View.GONE);
        btnBack.setVisibility(index > 0 ? View.VISIBLE : View.GONE);
        btnSubmit.setVisibility(currentQuestion.getType().equals("multiple") ? View.VISIBLE : View.GONE);

        tvQuestion.setText(currentQuestion.getQuestion());
        tvQuestionNumber.setText("Question " + (index + 1) + "/" + questionList.size());
        tvLiveScore.setText("Points: " + score);

        tvMultiHint.setVisibility(currentQuestion.getType().equals("multiple") ? View.VISIBLE : View.GONE);

        if (currentQuestion.getImage() != null && !currentQuestion.getImage().isEmpty()) {
            ivQuestionImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(currentQuestion.getImage()).into(ivQuestionImage);
        } else {
            ivQuestionImage.setVisibility(View.GONE);
        }

        llAnswersContainer.removeAllViews();
        if (currentQuestion.isShuffleAnswers()) Collections.shuffle(currentQuestion.getAnswers());

        for (Answer answer : currentQuestion.getAnswers()) {
            View itemView = getLayoutInflater().inflate(R.layout.item_answer, llAnswersContainer, false);
            TextView tvAnswerText = itemView.findViewById(R.id.tvAnswerText);
            tvAnswerText.setText(answer.getText());

            itemView.setOnClickListener(v -> {
                if (currentQuestion.getType().equals("single") || currentQuestion.getType().equals("true_false")) {
                    handleSingleChoice(answer);
                } else {
                    handleMultipleChoice(answer, itemView);
                }
            });

            llAnswersContainer.addView(itemView);
        }
    }

    private void handleSingleChoice(Answer answer) {
        disableAllOptions();
        selectedAnswers.clear();
        selectedAnswers.add(answer);
        showAnswerColors();

        // Score simple : +1 si correct
        if (answer.isCorrect()) score += 1;

        tvLiveScore.setText("Points: " + score);
        showDescriptionBottomSheet(answer.isCorrect() ? "Correct!" : "Wrong!", currentQuestion.getDescription());
        btnNext.setVisibility(View.VISIBLE);
    }

    private void handleMultipleChoice(Answer answer, View itemView) {
        if (selectedAnswers.contains(answer)) {
            selectedAnswers.remove(answer);
            itemView.setBackgroundResource(R.drawable.answer_background);
        } else {
            selectedAnswers.add(answer);
            itemView.setBackgroundColor(Color.LTGRAY);
        }
    }

    private void checkMultipleChoice() {
        disableAllOptions();
        showAnswerColors();

        if (allCorrect()) {
            score += 1;
        }

        tvLiveScore.setText("Points: " + score);

        showDescriptionBottomSheet(allCorrect() ? "Correct!" : "Wrong!", currentQuestion.getDescription());

        btnNext.setVisibility(View.VISIBLE);
    }


    private void showAnswerColors() {
        for (int i = 0; i < llAnswersContainer.getChildCount(); i++) {
            View itemView = llAnswersContainer.getChildAt(i);
            Answer answer = currentQuestion.getAnswers().get(i);
            TextView tvAnswerText = itemView.findViewById(R.id.tvAnswerText);

            GradientDrawable bgDrawable = (GradientDrawable) getDrawable(R.drawable.answer_background).mutate();

            if (answer.isCorrect()) {
                bgDrawable.setColor(Color.parseColor("#4CAF50"));
                tvAnswerText.setTextColor(Color.WHITE);
            } else if (selectedAnswers.contains(answer)) {
                bgDrawable.setColor(Color.parseColor("#F44336"));
                tvAnswerText.setTextColor(Color.WHITE);
            } else {
                bgDrawable.setColor(Color.WHITE);
                tvAnswerText.setTextColor(Color.BLACK);
            }
            itemView.setBackground(bgDrawable);
        }
    }

    private boolean allCorrect() {
        for (Answer a : currentQuestion.getAnswers()) {
            if (a.isCorrect() && !selectedAnswers.contains(a)) return false;
            if (!a.isCorrect() && selectedAnswers.contains(a)) return false;
        }
        return true;
    }


    private void disableAllOptions() {
        for (int i = 0; i < llAnswersContainer.getChildCount(); i++) {
            llAnswersContainer.getChildAt(i).setClickable(false);
        }
    }

    private void nextQuestion() {
        currentIndex++;
        if (currentIndex < questionList.size()) {
            loadQuestion(currentIndex);
        } else {
            showFinalScore();
        }
    }

    private void previousQuestion() {
        if (currentIndex > 0) {
            currentIndex--;
            loadQuestion(currentIndex);
        }
    }

    private QuizData loadQuizFromAssets() {
        try {
            InputStream is = getAssets().open("quiz.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            return new Gson().fromJson(br, QuizData.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showDescriptionBottomSheet(String title, String description) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_description, null);

        TextView tvTitle = view.findViewById(R.id.tvBottomSheetTitle);
        TextView tvContent = view.findViewById(R.id.tvBottomSheetContent);
        Button btnClose = view.findViewById(R.id.btnCloseBottomSheet);

        tvTitle.setText(title);
        tvContent.setText(description);

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void showFinalScore() {
        int maxScore = totalQuestions;
        String note;
        float percent = ((float) score / maxScore) * 100;

        if (percent >= 90) note = "Excellent";
        else if (percent >= 75) note = "Good";
        else if (percent >= 50) note = "Average";
        else note = "Needs Improvement";

        new AlertDialog.Builder(this)
                .setTitle("Quiz Finished!")
                .setMessage("MSID \nScore: " + score + "/" + maxScore + "\nNote: " + note)
                .setCancelable(false)
                .setPositiveButton("Restart Quiz", (dialog, which) -> restartQuiz())
                .show();
    }

    private void restartQuiz() {
        score = 0;
        currentIndex = 0;
        tvLiveScore.setText("Points: " + score);

        showQuestionNumberDialog();
        setQuizViewsVisibility(View.GONE);
    }

}
