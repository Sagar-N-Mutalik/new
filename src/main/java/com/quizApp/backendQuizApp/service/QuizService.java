package com.quizApp.backendQuizApp.service;

import com.quizApp.backendQuizApp.dto.quiz.QuizGenerationRequest;
import com.quizApp.backendQuizApp.model.Question;
import com.quizApp.backendQuizApp.model.Quiz;
import com.quizApp.backendQuizApp.model.User;
import com.quizApp.backendQuizApp.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final GeminiAiService geminiAiService;

    public Quiz generateQuiz(QuizGenerationRequest request, User creator) {
        List<Question> questions = geminiAiService.generateQuestions(request);
        Quiz quiz = Quiz.builder()
                .title("AI Quiz on " + request.getTopic())
                .description("Auto-generated quiz for topic: " + request.getTopic())
                .topic(request.getTopic())
                .creatorId(creator.getId())
                .creatorUsername(creator.getUsername())
                .questions(questions)
                .difficulty(request.getDifficulty())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .isPublic(true)
                .isActive(true)
                .category(request.getCategory())
                .tags(request.getTags())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .aiPrompt("topic:" + request.getTopic())
                .aiModel("Gemini:" + request.getDifficulty())
                .aiGeneratedAt(LocalDateTime.now())
                .totalQuestions(questions != null ? questions.size() : 0)
                .totalPoints(questions != null ? questions.stream().mapToInt(q -> q.getPoints() == null ? 1 : q.getPoints()).sum() : 0)
                .build();
        return quizRepository.save(quiz);
    }

    public List<Quiz> listPublicQuizzes() {
        return quizRepository.findByIsPublicTrue();
    }

    public Quiz getQuiz(String id) {
        return quizRepository.findById(id).orElse(null);
    }

    public List<Quiz> getMyQuizzes(String userId) {
        return quizRepository.findByCreatorId(userId);
    }
}
