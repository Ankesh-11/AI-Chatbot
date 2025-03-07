package com.ai.gemini_chat.repository;

import com.ai.gemini_chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findTop10ByOrderByIdDesc();
}
