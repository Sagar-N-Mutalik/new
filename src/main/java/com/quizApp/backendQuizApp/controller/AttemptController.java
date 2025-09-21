package com.quizApp.backendQuizApp.controller;

import com.quizApp.backendQuizApp.dto.attempt.SubmitAttemptRequest;
import com.quizApp.backendQuizApp.model.QuizAttempt;
import com.quizApp.backendQuizApp.model.User;
import com.quizApp.backendQuizApp.service.AttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @PostMapping
    public ResponseEntity<QuizAttempt> submit(@RequestBody @Valid SubmitAttemptRequest request,
                                              @AuthenticationPrincipal User user) {
        QuizAttempt attempt = attemptService.submitAttempt(request, user);
        return ResponseEntity.ok(attempt);
    }

    @GetMapping("/me")
    public ResponseEntity<List<QuizAttempt>> myAttempts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(attemptService.getMyAttempts(user));
    }

    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<List<QuizAttempt>> attemptsForQuiz(@PathVariable String quizId) {
        return ResponseEntity.ok(attemptService.getAttemptsForQuiz(quizId));
    }
}
