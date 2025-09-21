package com.api.model;

import java.util.Date;

public class Project {
    private String id;
    private String name;
    private String description;
    private String status;
    private String priority;
    private String owner;
    private Date createdDate;
    private Date dueDate;
    
    public Project() {}
    
    public Project(String name, String description, String priority, String owner) {
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.owner = owner;
        this.status = "PLANNING";
        this.createdDate = new Date();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
}
