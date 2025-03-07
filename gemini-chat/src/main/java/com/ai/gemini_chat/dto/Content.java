package com.ai.gemini_chat.dto;

import lombok.Data;

import java.util.List;

@Data
public class Content {
    private List<Part> parts;
}
