package com.blog.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class BlogPost {
    private int id;
    private String title;
    private String content;
    private String author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status; // draft, published, archived
    private List<String> tags;
    private String category;
    private int viewCount;
    
    public BlogPost() {
        this.tags = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "draft";
        this.viewCount = 0;
    }
    
    public BlogPost(String title, String content, String author) {
        this();
        this.title = title;
        this.content = content;
        this.author = author;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    
    public void incrementViewCount() { this.viewCount++; }
    
    public String getTagsAsString() {
        return String.join(", ", tags);
    }
    
    public void setTagsFromString(String tagsString) {
        if (tagsString != null && !tagsString.trim().isEmpty()) {
            this.tags = List.of(tagsString.split("\\s*,\\s*"));
        }
    }
}
