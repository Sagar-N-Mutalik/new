package com.quizApp.backendQuizApp.dto.quiz;

import com.quizApp.backendQuizApp.model.Question;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class QuizGenerationRequest {
    @NotBlank
    private String topic;

    @Min(1)
    @Max(50)
    private Integer numberOfQuestions = 10;

    private Question.DifficultyLevel difficulty = Question.DifficultyLevel.MEDIUM;

    private String category;

    private List<String> tags;

    private Integer timeLimitMinutes = 10;
}
