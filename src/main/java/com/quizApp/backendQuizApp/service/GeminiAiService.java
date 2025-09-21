package com.quizApp.backendQuizApp.service;

import com.fasterxml.jackson.core.type.TypeReference;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
                    return Mono.just("{}");
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
            return Collections.emptyList();
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode textNode = root.at("/candidates/0/content/parts/0/text");

            if (textNode.isMissingNode()) {
                log.error("Could not find text in Gemini response: {}", response);
                return Collections.emptyList();
            }

            String jsonText = textNode.asText();
            Map<String, List<Question>> result = objectMapper.readValue(jsonText, new TypeReference<>() {});

            return result.getOrDefault("questions", Collections.emptyList());

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", response, e);
            return Collections.emptyList();
        }
    }
}