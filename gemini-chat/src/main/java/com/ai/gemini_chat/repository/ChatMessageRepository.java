package com.ai.gemini_chat.repository;

import com.ai.gemini_chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Fetch paginated questions for a given chat session
    Page<ChatMessage> findByChatSessionId(Long chatSessionId, Pageable pageable);

    // Search for questions containing a keyword (case-insensitive) with pagination
    Page<ChatMessage> findByQuestionContainingIgnoreCase(String keyword, Pageable pageable);
}
