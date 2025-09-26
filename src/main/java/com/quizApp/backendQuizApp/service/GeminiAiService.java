package com.quizApp.backendQuizApp.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quizApp.backendQuizApp.config.properties.GeminiProperties;
import com.quizApp.backendQuizApp.dto.quiz.QuizGenerationRequest;
import com.quizApp.backendQuizApp.exception.GeminiApiException;
import com.quizApp.backendQuizApp.model.Question;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAiService {

    private final WebClient geminiWebClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Question> generateQuestions(QuizGenerationRequest request) {
        String prompt = buildPrompt(request);

        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = objectMapper.createArrayNode();
        ObjectNode contentObj = objectMapper.createObjectNode();
        ArrayNode parts = objectMapper.createArrayNode();
        ObjectNode part = objectMapper.createObjectNode();
        part.put("text", prompt);
        parts.add(part);
        contentObj.set("parts", parts);
        contents.add(contentObj);
        root.set("contents", contents);

        // Add generation config to enforce JSON output
        ObjectNode generationConfig = objectMapper.createObjectNode();
        generationConfig.put("response_mime_type", "application/json");
        root.set("generationConfig", generationConfig);


        String uri = String.format("/models/%s:generateContent?key=%s",
                geminiProperties.getModel(), geminiProperties.getApiKey());

        String response = geminiWebClient.post()
                .uri(uri)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(root.toString()))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> {
                    log.error("Gemini API error: {}", ex.getMessage(), ex);
                    return Mono.error(new GeminiApiException("Failed to generate quiz from Gemini AI", ex));
                })
                .block();

        return parseQuestionsFromResponse(response);
    }

    private String buildPrompt(QuizGenerationRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate ").append(req.getNumberOfQuestions())
                .append(" quiz questions for the topic: '").append(req.getTopic()).append("'.\n");
        sb.append("Difficulty: ").append(req.getDifficulty()).append(".\n");
        if (StringUtils.isNotBlank(req.getCategory())) {
            sb.append("Category: ").append(req.getCategory()).append(".\n");
        }
        sb.append("Return a JSON object with a single key 'questions', which is an array of question objects. ");
        sb.append("Each question object must have the following fields: 'questionText', 'type', 'options', 'correctAnswer', 'explanation', 'difficulty', 'category', and 'tags'.");
        return sb.toString();
    }

    private List<Question> parseQuestionsFromResponse(String response) {
        if (response == null || response.isBlank()) {
            throw new GeminiApiException("Received empty response from Gemini API");
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            
            // Check if there are any error messages from Gemini
            JsonNode errorNode = root.at("/error");
            if (!errorNode.isMissingNode()) {
                String errorMessage = errorNode.get("message").asText();
                throw new GeminiApiException("Gemini API returned an error: " + errorMessage);
            }

            // Validate response structure
            if (!root.has("candidates") || root.get("candidates").isEmpty()) {
                throw new GeminiApiException("Invalid response structure: missing or empty candidates array");
            }

            JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
            if (textNode.isMissingNode()) {
                throw new GeminiApiException("Could not find text content in Gemini response: " + response);
            }

            String jsonText = textNode.asText();
            Map<String, List<Question>> result = objectMapper.readValue(jsonText, new TypeReference<>() {});
            
            List<Question> questions = result.get("questions");
            if (questions == null || questions.isEmpty()) {
                throw new GeminiApiException("No questions generated in the response");
            }

            // Validate each question
            for (int i = 0; i < questions.size(); i++) {
                Question question = questions.get(i);
                validateQuestion(question, i + 1);
            }

            return questions;

        } catch (GeminiApiException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", response, e);
            throw new GeminiApiException("Failed to process Gemini API response: " + e.getMessage(), e);
        }
    }

    private void validateQuestion(Question question, int index) {
        if (question.getQuestionText() == null || question.getQuestionText().isBlank()) {
            throw new GeminiApiException("Question " + index + " is missing question text");
        }
        if (question.getType() == null) {
            throw new GeminiApiException("Question " + index + " is missing question type");
        }
        if (question.getOptions() == null || question.getOptions().isEmpty()) {
            throw new GeminiApiException("Question " + index + " is missing options");
        }
        if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
            throw new GeminiApiException("Question " + index + " is missing correct answer");
        }
        if (!question.getOptions().contains(question.getCorrectAnswer())) {
            throw new GeminiApiException("Question " + index + " has a correct answer that is not in the options");
        }
    }
}