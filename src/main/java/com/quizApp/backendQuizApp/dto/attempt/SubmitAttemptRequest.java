package com.quizApp.backendQuizApp.dto.attempt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SubmitAttemptRequest {
    @NotBlank
    private String quizId;

    @NotEmpty
    private List<AttemptAnswer> answers;
}
