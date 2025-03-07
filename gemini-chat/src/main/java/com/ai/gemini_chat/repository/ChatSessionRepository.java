package com.ai.gemini_chat.repository;

import com.ai.gemini_chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    @Query("SELECT c.id FROM ChatSession c")
    List<Long> findAllChatIds();
}
