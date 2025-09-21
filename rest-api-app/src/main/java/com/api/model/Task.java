package com.api.model;

import java.util.Date;

public class Task {
    private String id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String assignee;
    private Date createdDate;
    private Date dueDate;
    
    public Task() {}
    
    public Task(String title, String description, String priority, String assignee) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.assignee = assignee;
        this.status = "TODO";
        this.createdDate = new Date();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
}
