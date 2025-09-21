package com.blog.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserManagementServlet extends HttpServlet {
    private static final Map<String, User> users = new ConcurrentHashMap<>();
    
    static {
        // Initialize with sample users
        users.put("admin", new User("admin", "admin@blog.com", "Administrator", "admin", true));
        users.put("editor", new User("editor", "editor@blog.com", "Editor", "editor", true));
        users.put("author", new User("johndoe", "john@example.com", "John Doe", "author", true));
        users.put("subscriber", new User("janesmith", "jane@example.com", "Jane Smith", "subscriber", false));
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
                // Get all users with optional filtering
                String roleParam = request.getParameter("role");
                String activeParam = request.getParameter("active");
                
                StringBuilder json = new StringBuilder();
                json.append("{\"users\": [");
                
                boolean first = true;
                for (User user : users.values()) {
                    boolean include = true;
                    
                    if (roleParam != null && !roleParam.isEmpty()) {
                        if (!user.getRole().equalsIgnoreCase(roleParam)) {
                            include = false;
                        }
                    }
                    
                    if (activeParam != null && !activeParam.isEmpty()) {
                        boolean isActive = Boolean.parseBoolean(activeParam);
                        if (user.isActive() != isActive) {
                            include = false;
                        }
                    }
                    
                    if (include) {
                        if (!first) json.append(", ");
                        json.append(buildUserJson(user, false)); // Don't include password
                        first = false;
                    }
                }
                
                json.append("]}");
                out.print(json.toString());
                
            } else {
                // Get specific user
                String username = pathInfo.substring(1);
                User user = users.get(username);
                
                if (user != null) {
                    out.print(buildUserJson(user, false));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\": \"User not found\"}");
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
            String username = request.getParameter("username");
            String email = request.getParameter("email");
            String displayName = request.getParameter("displayName");
            String role = request.getParameter("role");
            String password = request.getParameter("password");
            
            if (username == null || email == null || displayName == null || 
                role == null || password == null ||
                username.trim().isEmpty() || email.trim().isEmpty() || 
                displayName.trim().isEmpty() || role.trim().isEmpty() || 
                password.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Missing required fields\"}");
                return;
            }
            
            if (users.containsKey(username.trim())) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                out.print("{\"error\": \"Username already exists\"}");
                return;
            }
            
            User newUser = new User(
                username.trim(), 
                email.trim(), 
                displayName.trim(), 
                role.trim(), 
                true
            );
            newUser.setPassword(password.trim());
            
            users.put(username.trim(), newUser);
            
            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print(buildUserJson(newUser, false));
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to create user\"}");
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
            out.print("{\"error\": \"Username is required\"}");
            return;
        }
        
        try {
            String username = pathInfo.substring(1);
            User user = users.get(username);
            
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"User not found\"}");
                return;
            }
            
            String email = request.getParameter("email");
            String displayName = request.getParameter("displayName");
            String role = request.getParameter("role");
            String activeParam = request.getParameter("active");
            String password = request.getParameter("password");
            
            if (email != null && !email.trim().isEmpty()) {
                user.setEmail(email.trim());
            }
            
            if (displayName != null && !displayName.trim().isEmpty()) {
                user.setDisplayName(displayName.trim());
            }
            
            if (role != null && !role.trim().isEmpty()) {
                user.setRole(role.trim());
            }
            
            if (activeParam != null) {
                user.setActive(Boolean.parseBoolean(activeParam));
            }
            
            if (password != null && !password.trim().isEmpty()) {
                user.setPassword(password.trim());
            }
            
            out.print(buildUserJson(user, false));
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to update user\"}");
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
            out.print("{\"error\": \"Username is required\"}");
            return;
        }
        
        try {
            String username = pathInfo.substring(1);
            
            if ("admin".equals(username)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"error\": \"Cannot delete admin user\"}");
                return;
            }
            
            User removed = users.remove(username);
            
            if (removed != null) {
                out.print("{\"message\": \"User deleted successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"User not found\"}");
            }
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to delete user\"}");
        }
    }
    
    private String buildUserJson(User user, boolean includePassword) {
        String passwordField = includePassword ? 
            String.format(", \"password\": \"%s\"", escapeJson(user.getPassword())) : "";
        
        return String.format(
            "{\"username\": \"%s\", \"email\": \"%s\", \"displayName\": \"%s\", " +
            "\"role\": \"%s\", \"active\": %b%s}",
            escapeJson(user.getUsername()),
            escapeJson(user.getEmail()),
            escapeJson(user.getDisplayName()),
            user.getRole(),
            user.isActive(),
            passwordField
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
    
    // Inner User class
    private static class User {
        private String username;
        private String email;
        private String displayName;
        private String role; // admin, editor, author, subscriber
        private boolean active;
        private String password;
        
        public User(String username, String email, String displayName, String role, boolean active) {
            this.username = username;
            this.email = email;
            this.displayName = displayName;
            this.role = role;
            this.active = active;
        }
        
        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
