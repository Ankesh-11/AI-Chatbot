package com.ai.gemini_chat.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String question;

    @Lob
    @Column(nullable = false)
    private String answer;
}
