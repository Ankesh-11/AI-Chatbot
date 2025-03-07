package com.ai.gemini_chat.controller;

import com.ai.gemini_chat.dto.QuestionResponseDto;
import com.ai.gemini_chat.service.QnaService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing chat-related operations.
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://127.0.0.1:5500/")
public class ChatController {

    private final QnaService qnaService;

    /**
     * Constructor for ChatController.
     *
     * @param qnaService The service responsible for handling chat-related operations.
     */
    public ChatController(QnaService qnaService) {
        this.qnaService = qnaService;
    }

    /**
     * Creates a new chat session and stores the first question.
     *
     * @param firstQuestion The first question to start the chat session.
     * @return The ID of the newly created chat session.
     */
    @PostMapping("/new")
    public ResponseEntity<Long> createNewChat(@RequestParam String firstQuestion) {
        Long chatId = qnaService.createNewChat(firstQuestion);
        return ResponseEntity.ok(chatId);
    }

    /**
     * Retrieves an answer for a given question within a chat session.
     *
     * @param chatId   The ID of the chat session.
     * @param question The question to be answered.
     * @return The generated answer.
     */
    @GetMapping("/{chatId}/ask")
    public ResponseEntity<QuestionResponseDto> askQuestion(@PathVariable Long chatId, @RequestParam String question) {
        return ResponseEntity.ok(qnaService.getAnswer(chatId, question));
    }

    /**
     * Retrieves a list of all chat session IDs.
     *
     * @return A list of chat session IDs.
     */
    @GetMapping("/allChatIds")
    public ResponseEntity<List<Long>> getAllChatIds() {
        List<Long> chatIds = qnaService.getAllChatIds();
        return ResponseEntity.ok(chatIds);
    }

    /**
     * Deletes a chat session by its ID.
     *
     * @param chatId The ID of the chat session to delete.
     * @return A message indicating the success or failure of the operation.
     */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<String> deleteChat(@PathVariable Long chatId) {
        String response = qnaService.deleteChatSession(chatId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of questions and answers for a specific chat session.
     *
     * @param chatId The ID of the chat session.
     * @param page   The page number for pagination.
     * @param size   The number of records per page.
     * @return A paginated list of questions and answers.
     */
    @GetMapping("/{chatId}/questions")
    public ResponseEntity<Page<QuestionResponseDto>> getChatQuestions(@PathVariable Long chatId, @RequestParam int page, @RequestParam int size) {
        Page<QuestionResponseDto> questionsAndAnswers = qnaService.getQuestionsByChatId(chatId, page, size);
        return ResponseEntity.ok(questionsAndAnswers);
    }

    /**
     * Searches for questions containing a specific keyword.
     *
     * @param keyword The keyword to search for in questions.
     * @param page    The page number for pagination.
     * @param size    The number of records per page.
     * @return A paginated list of matching questions and answers.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<QuestionResponseDto>> searchQuestions(@RequestParam String keyword, @RequestParam int page, @RequestParam int size) {
        Page<QuestionResponseDto> results = qnaService.searchQuestions(keyword, page, size);
        return ResponseEntity.ok(results);
    }
}
