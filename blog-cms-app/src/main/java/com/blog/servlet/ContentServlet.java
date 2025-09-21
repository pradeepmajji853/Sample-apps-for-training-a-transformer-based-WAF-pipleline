package com.blog.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContentServlet extends HttpServlet {
    private static final Map<Integer, ContentItem> content = new ConcurrentHashMap<>();
    private static int contentIdCounter = 1;
    
    static {
        // Initialize with sample content
        content.put(contentIdCounter, new ContentItem(contentIdCounter++, "page", "About Us", 
            "<h1>About Our Blog</h1><p>Welcome to our amazing blog platform!</p>", "published"));
        content.put(contentIdCounter, new ContentItem(contentIdCounter++, "page", "Contact", 
            "<h1>Contact Us</h1><p>Get in touch with us at contact@blog.com</p>", "published"));
        content.put(contentIdCounter, new ContentItem(contentIdCounter++, "widget", "Latest Posts Widget", 
            "<div class='widget'>Latest posts will appear here</div>", "published"));
        content.put(contentIdCounter, new ContentItem(contentIdCounter++, "template", "Blog Template", 
            "<!DOCTYPE html><html><head><title>Blog</title></head><body>{content}</body></html>", "draft"));
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Get all content with optional filtering
                String typeParam = request.getParameter("type");
                String statusParam = request.getParameter("status");
                String searchParam = request.getParameter("search");
                
                List<ContentItem> filteredContent = new ArrayList<>();
                
                for (ContentItem item : content.values()) {
                    boolean include = true;
                    
                    if (typeParam != null && !typeParam.isEmpty()) {
                        if (!item.getType().equalsIgnoreCase(typeParam)) {
                            include = false;
                        }
                    }
                    
                    if (statusParam != null && !statusParam.isEmpty()) {
                        if (!item.getStatus().equalsIgnoreCase(statusParam)) {
                            include = false;
                        }
                    }
                    
                    if (searchParam != null && !searchParam.isEmpty()) {
                        String search = searchParam.toLowerCase();
                        if (!item.getTitle().toLowerCase().contains(search) && 
                            !item.getContent().toLowerCase().contains(search)) {
                            include = false;
                        }
                    }
                    
                    if (include) {
                        filteredContent.add(item);
                    }
                }
                
                out.print(buildContentListJson(filteredContent));
                
            } else {
                // Get specific content by ID
                String idStr = pathInfo.substring(1);
                try {
                    int contentId = Integer.parseInt(idStr);
                    ContentItem item = content.get(contentId);
                    
                    if (item != null) {
                        out.print(buildContentItemJson(item));
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        out.print("{\"error\": \"Content not found\"}");
                    }
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\": \"Invalid content ID\"}");
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Internal server error\"}");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            String type = request.getParameter("type");
            String title = request.getParameter("title");
            String contentBody = request.getParameter("content");
            String status = request.getParameter("status");
            
            if (type == null || title == null || contentBody == null ||
                type.trim().isEmpty() || title.trim().isEmpty() || contentBody.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Missing required fields\"}");
                return;
            }
            
            if (status == null || status.trim().isEmpty()) {
                status = "draft";
            }
            
            ContentItem newItem = new ContentItem(
                contentIdCounter++,
                type.trim(),
                title.trim(),
                contentBody.trim(),
                status.trim()
            );
            
            content.put(newItem.getId(), newItem);
            
            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print(buildContentItemJson(newItem));
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to create content\"}");
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Content ID is required\"}");
            return;
        }
        
        try {
            String idStr = pathInfo.substring(1);
            int contentId = Integer.parseInt(idStr);
            ContentItem item = content.get(contentId);
            
            if (item == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Content not found\"}");
                return;
            }
            
            String type = request.getParameter("type");
            String title = request.getParameter("title");
            String contentBody = request.getParameter("content");
            String status = request.getParameter("status");
            
            if (type != null && !type.trim().isEmpty()) {
                item.setType(type.trim());
            }
            
            if (title != null && !title.trim().isEmpty()) {
                item.setTitle(title.trim());
            }
            
            if (contentBody != null && !contentBody.trim().isEmpty()) {
                item.setContent(contentBody.trim());
            }
            
            if (status != null && !status.trim().isEmpty()) {
                item.setStatus(status.trim());
            }
            
            out.print(buildContentItemJson(item));
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Invalid content ID\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to update content\"}");
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Content ID is required\"}");
            return;
        }
        
        try {
            String idStr = pathInfo.substring(1);
            int contentId = Integer.parseInt(idStr);
            
            ContentItem removed = content.remove(contentId);
            
            if (removed != null) {
                out.print("{\"message\": \"Content deleted successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Content not found\"}");
            }
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Invalid content ID\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to delete content\"}");
        }
    }
    
    private String buildContentListJson(List<ContentItem> items) {
        StringBuilder json = new StringBuilder();
        json.append("{\"content\": [");
        
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(buildContentItemJson(items.get(i)));
        }
        
        json.append("], \"total\": ").append(items.size()).append("}");
        return json.toString();
    }
    
    private String buildContentItemJson(ContentItem item) {
        return String.format(
            "{\"id\": %d, \"type\": \"%s\", \"title\": \"%s\", \"content\": \"%s\", \"status\": \"%s\"}",
            item.getId(),
            item.getType(),
            escapeJson(item.getTitle()),
            escapeJson(item.getContent()),
            item.getStatus()
        );
    }
    
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\"", "\\\"")
                   .replace("\\", "\\\\")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    // Inner ContentItem class
    private static class ContentItem {
        private int id;
        private String type; // page, widget, template, etc.
        private String title;
        private String content;
        private String status; // draft, published, archived
        
        public ContentItem(int id, String type, String title, String content, String status) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.content = content;
            this.status = status;
        }
        
        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
