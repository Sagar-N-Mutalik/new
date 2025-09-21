package com.quizApp.backendQuizApp.controller;

import com.quizApp.backendQuizApp.dto.quiz.QuizGenerationRequest;
import com.quizApp.backendQuizApp.model.Quiz;
import com.quizApp.backendQuizApp.model.User;
import com.quizApp.backendQuizApp.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/generate")
    public ResponseEntity<Quiz> generate(@RequestBody @Valid QuizGenerationRequest request,
                                         @AuthenticationPrincipal User user) {
        Quiz quiz = quizService.generateQuiz(request, user);
        return ResponseEntity.ok(quiz);
    }

    @GetMapping("/public")
    public ResponseEntity<List<Quiz>> listPublic() {
        return ResponseEntity.ok(quizService.listPublicQuizzes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quiz> getById(@PathVariable String id) {
        Quiz quiz = quizService.getQuiz(id);
        return quiz != null ? ResponseEntity.ok(quiz) : ResponseEntity.notFound().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<Quiz>> getMyQuizzes(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(quizService.getMyQuizzes(user.getId()));
    }
}
