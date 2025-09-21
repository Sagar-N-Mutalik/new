package com.quizApp.backendQuizApp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "quiz_attempts")
public class QuizAttempt {
    
    @Id
    private String id;
    
    @NotBlank(message = "Quiz ID is required")
    @Indexed
    private String quizId;
    
    @NotBlank(message = "User ID is required")
    @Indexed
    private String userId;
    
    private String username;
    
    private String quizTitle;
    
    @NotNull(message = "User answers are required")
    private Map<Integer, String> userAnswers; // questionIndex -> userAnswer
    
    private Map<Integer, String> correctAnswerMap; // questionIndex -> correctAnswer
    
    private Map<Integer, Boolean> questionResultsMap; // questionIndex -> isCorrect
    
    private Integer totalQuestions;
    
    private Integer correctAnswers; // total number of correct answers
    
    private Integer incorrectAnswers;
    
    private Double scorePercentage;
    
    private Integer totalPoints;
    
    private Integer earnedPoints;
    
    private LocalDateTime startedAt;
    
    private LocalDateTime completedAt;
    
    private Long timeTakenSeconds;
    
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;
    
    // Detailed analysis
    private Map<String, Object> performanceAnalysis;
    
    private List<String> strengthAreas;
    
    private List<String> improvementAreas;
    
    private String feedback;
    
    // Metadata
    private String ipAddress;
    
    private String userAgent;
    
    private LocalDateTime createdAt;
    
    public enum AttemptStatus {
        IN_PROGRESS, COMPLETED, ABANDONED, TIMED_OUT
    }
    
    // Helper methods
    public void calculateScore() {
        if (totalQuestions != null && correctAnswers != null) {
            this.scorePercentage = (double) correctAnswers / totalQuestions * 100;
        }
    }
    
    public void calculateTimeTaken() {
        if (startedAt != null && completedAt != null) {
            this.timeTakenSeconds = java.time.Duration.between(startedAt, completedAt).getSeconds();
        }
    }
    
    public boolean isPassed(double passingScore) {
        return scorePercentage != null && scorePercentage >= passingScore;
    }
}
