package com.quizApp.backendQuizApp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quizApp.backendQuizApp.config.properties.GeminiProperties;
import com.quizApp.backendQuizApp.dto.quiz.QuizGenerationRequest;
import com.quizApp.backendQuizApp.model.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAiService {

    private final WebClient geminiWebClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Question> generateQuestions(QuizGenerationRequest request) {
        String prompt = buildPrompt(request);

        // Build payload: { "contents": [ { "parts": [ { "text": prompt } ] } ] }
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
                    return Mono.just("{}");
                })
                .block();

        return parseQuestionsFromResponse(response);
    }

    private String buildPrompt(QuizGenerationRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert quiz generator. Generate ")
                .append(req.getNumberOfQuestions()).append(" questions on the topic: '")
                .append(req.getTopic()).append("'. ");
        sb.append("Difficulty: ").append(req.getDifficulty());
        if (StringUtils.isNotBlank(req.getCategory())) {
            sb.append(", Category: ").append(req.getCategory());
        }
        sb.append(". Return STRICTLY valid JSON with this schema: \n");
        sb.append("{\n  \"questions\": [\n    {\n      \"questionText\": \"string\",\n      \"type\": \"MULTIPLE_CHOICE|TRUE_FALSE|SINGLE_CHOICE\",\n      \"options\": [\"string\"],\n      \"correctAnswer\": \"string\",\n      \"explanation\": \"string\",\n      \"points\": 1,\n      \"difficulty\": \"EASY|MEDIUM|HARD\",\n      \"category\": \"string\",\n      \"tags\": [\"string\"]\n    }\n  ]\n}\n");
        sb.append("Ensure options include the correct answer. Keep explanations concise.");
        return sb.toString();
    }

    private List<Question> parseQuestionsFromResponse(String response) {
        List<Question> questions = new ArrayList<>();
        if (response == null || response.isBlank()) return questions;
        try {
            JsonNode root = objectMapper.readTree(response);
            // Gemini returns candidates[0].content.parts[0].text containing JSON string
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode textNode = candidates.get(0)
                        .path("content").path("parts");
                String text;
                if (textNode.isArray() && textNode.size() > 0) {
                    text = textNode.get(0).path("text").asText("");
                } else {
                    text = candidates.get(0).path("content").path("text").asText("");
                }
                if (!text.isBlank()) {
                    // Some models wrap JSON in markdown. Strip code fences if present.
                    text = text.replaceAll("^```json\\n|```$", "").trim();
                    JsonNode parsed = objectMapper.readTree(text);
                    JsonNode qArr = parsed.path("questions");
                    if (qArr.isArray()) {
                        for (Iterator<JsonNode> it = qArr.elements(); it.hasNext(); ) {
                            JsonNode q = it.next();
                            Question.QuestionType type = Question.QuestionType.valueOf(q.path("type").asText("MULTIPLE_CHOICE"));
                            Question.DifficultyLevel diff = Question.DifficultyLevel.valueOf(q.path("difficulty").asText("MEDIUM"));
                            List<String> options = new ArrayList<>();
                            if (q.path("options").isArray()) {
                                q.path("options").forEach(n -> options.add(n.asText()));
                            }
                            questions.add(Question.builder()
                                    .questionText(q.path("questionText").asText())
                                    .type(type)
                                    .options(options)
                                    .correctAnswer(q.path("correctAnswer").asText())
                                    .explanation(q.path("explanation").asText(null))
                                    .points(q.path("points").asInt(1))
                                    .difficulty(diff)
                                    .category(q.path("category").asText(null))
                                    .tags(q.path("tags").isArray() ? objectMapper.convertValue(q.path("tags"), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)) : null)
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage(), e);
        }
        return questions;
    }
}
