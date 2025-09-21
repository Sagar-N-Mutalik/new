package com.quizApp.backendQuizApp.repository;

import com.quizApp.backendQuizApp.model.Quiz;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuizRepository extends MongoRepository<Quiz, String> {
    List<Quiz> findByCreatorId(String creatorId);
    List<Quiz> findByTopicIgnoreCase(String topic);
    List<Quiz> findByIsPublicTrue();
}
