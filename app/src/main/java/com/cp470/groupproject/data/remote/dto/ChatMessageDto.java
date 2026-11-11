package com.cp470.groupproject.data.remote.dto;

/** One turn of the conversation sent to the backend: {"role":"user"|"assistant","content":"..."} */
public class ChatMessageDto {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    public String role;
    public String content;

    public ChatMessageDto(String role, String content) {
        this.role = role;
        this.content = content;
    }
}