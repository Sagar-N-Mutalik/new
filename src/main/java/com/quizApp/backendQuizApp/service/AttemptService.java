package com.quizApp.backendQuizApp.service;

import com.quizApp.backendQuizApp.dto.attempt.AttemptAnswer;
import com.quizApp.backendQuizApp.dto.attempt.SubmitAttemptRequest;
import com.quizApp.backendQuizApp.model.Question;
import com.quizApp.backendQuizApp.model.Quiz;
import com.quizApp.backendQuizApp.model.QuizAttempt;
import com.quizApp.backendQuizApp.model.User;
import com.quizApp.backendQuizApp.repository.QuizAttemptRepository;
import com.quizApp.backendQuizApp.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;

    public QuizAttempt submitAttempt(SubmitAttemptRequest request, User user) {
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        Map<Integer, String> userAnswers = request.getAnswers().stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionIndex, AttemptAnswer::getAnswer));

        Map<Integer, String> correctMap = new HashMap<>();
        Map<Integer, Boolean> resultsMap = new HashMap<>();
        int totalQuestions = quiz.getQuestions().size();
        int correctCount = 0;
        int totalPoints = 0;
        int earnedPoints = 0;

        List<Question> questions = quiz.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            totalPoints += (q.getPoints() == null ? 1 : q.getPoints());
            String correct = q.getCorrectAnswer();
            correctMap.put(i, correct);
            String userAns = userAnswers.get(i);
            boolean isCorrect = userAns != null && userAns.equalsIgnoreCase(correct);
            resultsMap.put(i, isCorrect);
            if (isCorrect) {
                correctCount++;
                earnedPoints += (q.getPoints() == null ? 1 : q.getPoints());
            }
        }

        QuizAttempt attempt = QuizAttempt.builder()
                .quizId(quiz.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .quizTitle(quiz.getTitle())
                .userAnswers(userAnswers)
                .correctAnswerMap(correctMap)
                .questionResultsMap(resultsMap)
                .totalQuestions(totalQuestions)
                .correctAnswers(correctCount)
                .incorrectAnswers(totalQuestions - correctCount)
                .totalPoints(totalPoints)
                .earnedPoints(earnedPoints)
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .status(QuizAttempt.AttemptStatus.COMPLETED)
                .build();
        attempt.calculateScore();
        attempt.calculateTimeTaken();

        // update quiz stats
        quiz.incrementAttempts();
        quiz.updateAverageScore(attempt.getScorePercentage());
        quizRepository.save(quiz);

        return attemptRepository.save(attempt);
    }

    public List<QuizAttempt> getMyAttempts(User user) {
        return attemptRepository.findByUserId(user.getId());
    }

    public List<QuizAttempt> getAttemptsForQuiz(String quizId) {
        return attemptRepository.findByQuizId(quizId);
    }
}
