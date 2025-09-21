package com.blog.model;

import java.time.LocalDateTime;

public class Comment {
    private int id;
    private int postId;
    private String author;
    private String email;
    private String content;
    private LocalDateTime createdAt;
    private String status; // approved, pending, spam
    private String ipAddress;
    
    public Comment() {
        this.createdAt = LocalDateTime.now();
        this.status = "pending";
    }
    
    public Comment(int postId, String author, String email, String content) {
        this();
        this.postId = postId;
        this.author = author;
        this.email = email;
        this.content = content;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
