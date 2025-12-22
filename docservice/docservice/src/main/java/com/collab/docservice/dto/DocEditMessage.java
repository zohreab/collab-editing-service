package com.collab.docservice.dto;

public class DocEditMessage {
    public String sender;
    public String content;
    public String type; // "EDIT", "JOIN", "CURSOR"
    public int cursorPosition; // The index of the cursor in the text
}