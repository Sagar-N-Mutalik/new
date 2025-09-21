package com.quizApp.backendQuizApp.dto.attempt;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttemptAnswer {
    @NotNull
    private Integer questionIndex;

    @NotNull
    private String answer;
}
