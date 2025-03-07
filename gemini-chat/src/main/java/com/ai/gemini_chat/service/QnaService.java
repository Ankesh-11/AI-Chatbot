package com.ai.gemini_chat.service;

import com.ai.gemini_chat.dto.Content;
import com.ai.gemini_chat.dto.QuestionResponseDto;
import com.ai.gemini_chat.dto.RequestBody;
import com.ai.gemini_chat.dto.Part;
import com.ai.gemini_chat.entity.ChatSession;
import com.ai.gemini_chat.entity.ChatMessage;
import com.ai.gemini_chat.repository.ChatSessionRepository;
import com.ai.gemini_chat.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for handling Q&A interactions in a chat system.
 */
@Service
@Slf4j
public class QnaService {

    @Value("${gemini.api.Url}")
    private String geminiURL;

    @Value("${gemini.api.Key}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public QnaService(WebClient.Builder webClient, ChatSessionRepository chatSessionRepository, ChatMessageRepository chatMessageRepository) {
        this.webClient = webClient.build();
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * Creates a new chat session with the first question.
     * @param firstQuestion The first question of the chat session.
     * @return The created chat session ID.
     */
    public Long createNewChat(String firstQuestion) {
        log.info("Creating new chat session...");
        ChatSession chatSession = new ChatSession();
        chatSession.setTitle(firstQuestion);
        chatSession = chatSessionRepository.save(chatSession);
        getAnswer(chatSession.getId(), firstQuestion);
        return chatSession.getId();
    }

    /**
     * Fetches an answer for a given question within a specific chat session.
     * @param chatSessionId The ID of the chat session.
     * @param question The question asked in the session.
     * @return The generated answer.
     */
    public QuestionResponseDto getAnswer(Long chatSessionId, String question) {
        log.info("Fetching answer for chatSessionId: {}", chatSessionId);
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));

        String prompt = buildPrompt(chatSessionId, question, 0, 10);
        Part part = new Part();
        part.setText(prompt);
        Content content = new Content();
        content.setParts(List.of(part));
        RequestBody requestBody = new RequestBody();
        requestBody.setContents(List.of(content));

        try {
            String response = String.valueOf(webClient.post()
                    .uri(geminiURL + "?key=" + geminiApiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("API Error Response: {}", errorBody);
                                        return Mono.error(new RuntimeException("API Error: " + errorBody));
                                    })
                    )
                    .bodyToMono(String.class)
                    .map(this::extractResponseContent)
                    .block());

            if (response != null) {
                ChatMessage message = new ChatMessage();
                message.setChatSession(chatSession);
                message.setQuestion(question);
                message.setAnswer(response);
                chatMessageRepository.save(message);
                return new QuestionResponseDto(question,response);
            } else {
                log.warn("Received null response from API.");
                return new QuestionResponseDto(question,"Error: Received null response.");
            }
        } catch (Exception e) {
            log.error("Error occurred while sending request", e);
            return new QuestionResponseDto(question,"Error occurred while processing request.");
        }

    }

    /**
     * Extracts the response content from the API response.
     * @param response The raw API response.
     * @return The extracted answer text.
     */
    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }

    /**
     * Deletes a chat session by its ID.
     * @param chatSessionId The ID of the chat session to be deleted.
     * @return A message indicating success or failure.
     */
    public String deleteChatSession(Long chatSessionId) {
        if (!chatSessionRepository.existsById(chatSessionId)) {
            return "Chat session not found!";
        }
        chatSessionRepository.deleteById(chatSessionId);
        log.info("Deleted chat session with ID: {}", chatSessionId);
        return "Chat session deleted successfully!";
    }

    /**
     * Retrieves all chat session IDs.
     * @return A list of all chat session IDs.
     */
    public List<Long> getAllChatIds() {
        List<Long> allChatIds = chatSessionRepository.findAllChatIds();
        return allChatIds;
    }

    /**
     * Retrieves paginated questions and answers for a given chat session.
     * @param chatSessionId The chat session ID.
     * @param page The page number.
     * @param size The page size.
     * @return A paginated list of question-response pairs.
     */
    public Page<QuestionResponseDto> getQuestionsByChatId(Long chatSessionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatMessageRepository.findByChatSessionId(chatSessionId, pageable)
                .map(msg -> new QuestionResponseDto(msg.getQuestion(), msg.getAnswer()));
    }

    /**
     * Searches questions based on a keyword.
     * @param keyword The search keyword.
     * @param page The page number.
     * @param size The page size.
     * @return A paginated list of matched questions and answers.
     */
    public Page<QuestionResponseDto> searchQuestions(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatMessageRepository.findByQuestionContainingIgnoreCase(keyword, pageable)
                .map(msg -> new QuestionResponseDto(msg.getQuestion(), msg.getAnswer()));
    }

    /**
     * Builds a prompt including chat history for a given session.
     * @param chatSessionId The chat session ID.
     * @param question The current question.
     * @param page The page number.
     * @param size The page size.
     * @return The built prompt string.
     */
    private String buildPrompt(Long chatSessionId, String question, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<ChatMessage> chatHistoryPage = chatMessageRepository.findByChatSessionId(chatSessionId, pageable);
        if(!chatHistoryPage.isEmpty()){
            List<ChatMessage> chatHistory = chatHistoryPage.getContent();
            StringBuilder prompt = new StringBuilder();
            for (ChatMessage message : chatHistory) {
                prompt.append("Q: ").append(message.getQuestion()).append("\n");
                prompt.append("A: ").append(message.getAnswer()).append("\n\n");
            }
            prompt.append("Q: ").append(question).append("\n");
            prompt.append("A: ");
            return prompt.toString();
        }
        else{
            StringBuilder prompt = new StringBuilder();
            prompt.append("Q: ").append(question).append("\n");
            prompt.append("A: ");
            return prompt.toString();
        }
    }
}
