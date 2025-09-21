package com.blog.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class SearchServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            String query = request.getParameter("q");
            String type = request.getParameter("type"); // posts, users, comments, content
            String category = request.getParameter("category");
            String author = request.getParameter("author");
            String dateFrom = request.getParameter("dateFrom");
            String dateTo = request.getParameter("dateTo");
            String status = request.getParameter("status");
            String tags = request.getParameter("tags");
            
            // Pagination parameters
            String pageParam = request.getParameter("page");
            String limitParam = request.getParameter("limit");
            
            int page = 1;
            int limit = 10;
            
            try {
                if (pageParam != null) page = Integer.parseInt(pageParam);
                if (limitParam != null) limit = Integer.parseInt(limitParam);
            } catch (NumberFormatException e) {
                // Use default values
            }
            
            if (query == null || query.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Search query is required\"}");
                return;
            }
            
            // Simulate search results based on query and filters
            List<SearchResult> results = performSearch(query, type, category, author, 
                                                     dateFrom, dateTo, status, tags, page, limit);
            
            out.print(buildSearchResultsJson(results, query, page, limit));
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Search failed\"}");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Advanced search with POST body
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            // This would typically parse JSON from request body
            // For simplicity, using parameters
            String query = request.getParameter("query");
            String filters = request.getParameter("filters");
            String sortBy = request.getParameter("sortBy");
            String sortOrder = request.getParameter("sortOrder");
            
            if (query == null || query.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Search query is required\"}");
                return;
            }
            
            List<SearchResult> results = performAdvancedSearch(query, filters, sortBy, sortOrder);
            
            out.print(buildAdvancedSearchResultsJson(results, query, filters));
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Advanced search failed\"}");
        }
    }
    
    private List<SearchResult> performSearch(String query, String type, String category, 
                                           String author, String dateFrom, String dateTo, 
                                           String status, String tags, int page, int limit) {
        List<SearchResult> results = new ArrayList<>();
        
        // Simulate different types of search results
        if (type == null || "posts".equals(type)) {
            results.add(new SearchResult("post", 1, "Getting Started with Java", 
                "Learn the basics of Java programming with this comprehensive guide...", 
                "John Doe", "2024-01-15", "programming,java,tutorial"));
            results.add(new SearchResult("post", 2, "Advanced Java Concepts", 
                "Dive deeper into Java with advanced topics like generics and streams...", 
                "Jane Smith", "2024-01-20", "programming,java,advanced"));
        }
        
        if (type == null || "users".equals(type)) {
            results.add(new SearchResult("user", 1, "John Doe", 
                "Experienced Java developer and technical writer", 
                "john@example.com", "2024-01-10", "author"));
        }
        
        if (type == null || "comments".equals(type)) {
            results.add(new SearchResult("comment", 1, "Great tutorial!", 
                "Thanks for this amazing explanation of Java concepts...", 
                "Mike Johnson", "2024-01-16", "approved"));
        }
        
        if (type == null || "content".equals(type)) {
            results.add(new SearchResult("content", 1, "About Us Page", 
                "Welcome to our blog platform where we share knowledge...", 
                "Admin", "2024-01-01", "published"));
        }
        
        // Filter results based on search query
        return results.stream()
                .filter(result -> result.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                               result.getDescription().toLowerCase().contains(query.toLowerCase()))
                .limit(limit)
                .collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2));
    }
    
    private List<SearchResult> performAdvancedSearch(String query, String filters, 
                                                   String sortBy, String sortOrder) {
        // Advanced search with more complex logic
        List<SearchResult> results = new ArrayList<>();
        
        // Simulate fuzzy search results
        results.add(new SearchResult("post", 3, "Java Best Practices", 
            "Follow these best practices to write better Java code...", 
            "Sarah Wilson", "2024-01-25", "programming,java,best-practices"));
        results.add(new SearchResult("post", 4, "Spring Framework Tutorial", 
            "Complete guide to Spring Framework for Java developers...", 
            "Bob Chen", "2024-01-30", "java,spring,framework"));
        
        return results;
    }
    
    private String buildSearchResultsJson(List<SearchResult> results, String query, int page, int limit) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"query\": \"").append(escapeJson(query)).append("\",");
        json.append("\"page\": ").append(page).append(",");
        json.append("\"limit\": ").append(limit).append(",");
        json.append("\"total\": ").append(results.size()).append(",");
        json.append("\"results\": [");
        
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(buildSearchResultJson(results.get(i)));
        }
        
        json.append("]}");
        return json.toString();
    }
    
    private String buildAdvancedSearchResultsJson(List<SearchResult> results, String query, String filters) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"query\": \"").append(escapeJson(query)).append("\",");
        json.append("\"filters\": \"").append(escapeJson(filters)).append("\",");
        json.append("\"total\": ").append(results.size()).append(",");
        json.append("\"results\": [");
        
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(buildSearchResultJson(results.get(i)));
        }
        
        json.append("]}");
        return json.toString();
    }
    
    private String buildSearchResultJson(SearchResult result) {
        return String.format(
            "{\"type\": \"%s\", \"id\": %d, \"title\": \"%s\", \"description\": \"%s\", " +
            "\"author\": \"%s\", \"date\": \"%s\", \"metadata\": \"%s\"}",
            result.getType(),
            result.getId(),
            escapeJson(result.getTitle()),
            escapeJson(result.getDescription()),
            escapeJson(result.getAuthor()),
            result.getDate(),
            escapeJson(result.getMetadata())
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
    
    // Inner SearchResult class
    private static class SearchResult {
        private String type;
        private int id;
        private String title;
        private String description;
        private String author;
        private String date;
        private String metadata;
        
        public SearchResult(String type, int id, String title, String description, 
                          String author, String date, String metadata) {
            this.type = type;
            this.id = id;
            this.title = title;
            this.description = description;
            this.author = author;
            this.date = date;
            this.metadata = metadata;
        }
        
        // Getters
        public String getType() { return type; }
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getAuthor() { return author; }
        public String getDate() { return date; }
        public String getMetadata() { return metadata; }
    }
}
