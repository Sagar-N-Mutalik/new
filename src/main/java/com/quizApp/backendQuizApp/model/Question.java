package com.quizApp.backendQuizApp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Question {
    
    @NotBlank(message = "Question text is required")
    private String questionText;
    
    @NotNull(message = "Question type is required")
    private QuestionType type;
    
    @NotEmpty(message = "Options are required")
    private List<String> options;
    
    @NotBlank(message = "Correct answer is required")
    private String correctAnswer;
    
    private String explanation;
    
    @Builder.Default
    private Integer points = 1;
    
    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;
    
    private String category;
    
    private List<String> tags;
    
    public enum QuestionType {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        SINGLE_CHOICE,
        FILL_IN_THE_BLANK
    }
    
    public enum DifficultyLevel {
        EASY, MEDIUM, HARD
    }
}
