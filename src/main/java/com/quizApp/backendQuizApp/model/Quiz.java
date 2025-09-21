package com.quizApp.backendQuizApp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "quizzes")
public class Quiz {
    
    @Id
    private String id;
    
    @NotBlank(message = "Quiz title is required")
    @Indexed
    private String title;
    
    private String description;
    
    @NotBlank(message = "Topic is required")
    @Indexed
    private String topic;
    
    @NotBlank(message = "Creator ID is required")
    private String creatorId;
    
    private String creatorUsername;
    
    @NotEmpty(message = "Quiz must have at least one question")
    private List<Question> questions;
    
    @NotNull(message = "Difficulty level is required")
    private Question.DifficultyLevel difficulty;
    
    @Positive(message = "Time limit must be positive")
    private Integer timeLimitMinutes;
    
    @Builder.Default
    private Boolean isPublic = true;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private String category;
    
    private List<String> tags;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Quiz statistics
    private Integer totalAttempts;
    
    private Double averageScore;
    
    private Integer totalQuestions;
    
    private Integer totalPoints;
    
    // AI Generation metadata
    private String aiPrompt;
    
    private String aiModel;
    
    private LocalDateTime aiGeneratedAt;
    
    // Access control
    private Set<String> allowedUserIds;
    
    private String accessCode;
    
    @Builder.Default
    private QuizVisibility visibility = QuizVisibility.PUBLIC;
    
    public enum QuizVisibility {
        PUBLIC, PRIVATE, RESTRICTED
    }
    
    // Helper methods
    public void incrementAttempts() {
        this.totalAttempts = (this.totalAttempts == null) ? 1 : this.totalAttempts + 1;
    }
    
    public void updateAverageScore(Double newScore) {
        if (this.averageScore == null) {
            this.averageScore = newScore;
        } else {
            this.averageScore = ((this.averageScore * (this.totalAttempts - 1)) + newScore) / this.totalAttempts;
        }
    }
}
