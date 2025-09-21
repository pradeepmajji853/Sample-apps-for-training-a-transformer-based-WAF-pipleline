package com.blog.servlet;

import com.blog.model.Comment;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CommentServlet extends HttpServlet {
    private static final List<Comment> comments = new ArrayList<>();
    private static final AtomicInteger commentIdCounter = new AtomicInteger(1);
    
    static {
        // Initialize with sample comments
        Comment comment1 = new Comment(1, "John Doe", "john@example.com", "Great article! Very informative.");
        comment1.setId(commentIdCounter.getAndIncrement());
        comment1.setStatus("approved");
        comment1.setIpAddress("192.168.1.100");
        comments.add(comment1);
        
        Comment comment2 = new Comment(1, "Jane Smith", "jane@example.com", "Thanks for sharing this knowledge.");
        comment2.setId(commentIdCounter.getAndIncrement());
        comment2.setStatus("approved");
        comment2.setIpAddress("192.168.1.101");
        comments.add(comment2);
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
                // Get all comments or filter by post ID
                String postIdParam = request.getParameter("postId");
                String statusParam = request.getParameter("status");
                
                List<Comment> filteredComments = new ArrayList<>();
                for (Comment comment : comments) {
                    boolean include = true;
                    
                    if (postIdParam != null) {
                        try {
                            int postId = Integer.parseInt(postIdParam);
                            if (comment.getPostId() != postId) {
                                include = false;
                            }
                        } catch (NumberFormatException e) {
                            include = false;
                        }
                    }
                    
                    if (statusParam != null && !statusParam.isEmpty()) {
                        if (!comment.getStatus().equalsIgnoreCase(statusParam)) {
                            include = false;
                        }
                    }
                    
                    if (include) {
                        filteredComments.add(comment);
                    }
                }
                
                out.print(buildCommentsJson(filteredComments));
                
            } else {
                // Get specific comment by ID
                String idStr = pathInfo.substring(1);
                try {
                    int commentId = Integer.parseInt(idStr);
                    Comment comment = findCommentById(commentId);
                    
                    if (comment != null) {
                        out.print(buildCommentJson(comment));
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        out.print("{\"error\": \"Comment not found\"}");
                    }
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\": \"Invalid comment ID\"}");
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
            String postIdStr = request.getParameter("postId");
            String author = request.getParameter("author");
            String email = request.getParameter("email");
            String content = request.getParameter("content");
            
            if (postIdStr == null || author == null || email == null || content == null ||
                author.trim().isEmpty() || email.trim().isEmpty() || content.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Missing required fields\"}");
                return;
            }
            
            int postId = Integer.parseInt(postIdStr);
            
            Comment comment = new Comment(postId, author.trim(), email.trim(), content.trim());
            comment.setId(commentIdCounter.getAndIncrement());
            comment.setIpAddress(request.getRemoteAddr());
            
            // Simple spam detection
            if (content.toLowerCase().contains("spam") || content.toLowerCase().contains("viagra")) {
                comment.setStatus("spam");
            } else {
                comment.setStatus("pending"); // Require moderation
            }
            
            comments.add(comment);
            
            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print(buildCommentJson(comment));
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Invalid post ID\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to create comment\"}");
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
            out.print("{\"error\": \"Comment ID is required\"}");
            return;
        }
        
        try {
            String idStr = pathInfo.substring(1);
            int commentId = Integer.parseInt(idStr);
            Comment comment = findCommentById(commentId);
            
            if (comment == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Comment not found\"}");
                return;
            }
            
            String status = request.getParameter("status");
            String content = request.getParameter("content");
            
            if (status != null && !status.trim().isEmpty()) {
                comment.setStatus(status.trim());
            }
            
            if (content != null && !content.trim().isEmpty()) {
                comment.setContent(content.trim());
            }
            
            out.print(buildCommentJson(comment));
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Invalid comment ID\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to update comment\"}");
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
            out.print("{\"error\": \"Comment ID is required\"}");
            return;
        }
        
        try {
            String idStr = pathInfo.substring(1);
            int commentId = Integer.parseInt(idStr);
            
            boolean removed = comments.removeIf(comment -> comment.getId() == commentId);
            
            if (removed) {
                out.print("{\"message\": \"Comment deleted successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Comment not found\"}");
            }
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Invalid comment ID\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to delete comment\"}");
        }
    }
    
    private Comment findCommentById(int id) {
        return comments.stream()
                .filter(comment -> comment.getId() == id)
                .findFirst()
                .orElse(null);
    }
    
    private String buildCommentsJson(List<Comment> commentList) {
        StringBuilder json = new StringBuilder();
        json.append("{\"comments\": [");
        
        for (int i = 0; i < commentList.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(buildCommentJson(commentList.get(i)));
        }
        
        json.append("], \"total\": ").append(commentList.size()).append("}");
        return json.toString();
    }
    
    private String buildCommentJson(Comment comment) {
        return String.format(
            "{\"id\": %d, \"postId\": %d, \"author\": \"%s\", \"email\": \"%s\", " +
            "\"content\": \"%s\", \"createdAt\": \"%s\", \"status\": \"%s\", \"ipAddress\": \"%s\"}",
            comment.getId(),
            comment.getPostId(),
            escapeJson(comment.getAuthor()),
            escapeJson(comment.getEmail()),
            escapeJson(comment.getContent()),
            comment.getCreatedAt().toString(),
            comment.getStatus(),
            comment.getIpAddress()
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
}
