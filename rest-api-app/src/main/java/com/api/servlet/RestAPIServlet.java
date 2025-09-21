package com.api.servlet;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.*;
import java.util.*;

@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 10, maxRequestSize = 1024 * 1024 * 15)
public class RestAPIServlet extends HttpServlet {
    
    // In-memory data stores
    private static Map<String, Map<String, Object>> tasks = new HashMap<>();
    private static Map<String, Map<String, Object>> users = new HashMap<>();
    private static Map<String, Map<String, Object>> projects = new HashMap<>();
    private static Map<String, Map<String, Object>> files = new HashMap<>();
    private static Map<String, String> sessions = new HashMap<>();
    private static Map<String, String> apiKeys = new HashMap<>();
    
    private static int taskCounter = 1;
    private static int userCounter = 1;
    private static int projectCounter = 1;
    private static int fileCounter = 1;
    
    static {
        initializeData();
    }
    
    private static void initializeData() {
        // Initialize sample tasks
        Map<String, Object> task1 = new HashMap<>();
        task1.put("id", "1");
        task1.put("title", "Implement API Authentication");
        task1.put("description", "Add JWT-based authentication to REST API");
        task1.put("status", "IN_PROGRESS");
        task1.put("priority", "HIGH");
        task1.put("assignee", "john_doe");
        task1.put("createdDate", new Date());
        tasks.put("1", task1);
        
        Map<String, Object> task2 = new HashMap<>();
        task2.put("id", "2");
        task2.put("title", "Database Integration");
        task2.put("description", "Connect API to PostgreSQL database");
        task2.put("status", "TODO");
        task2.put("priority", "MEDIUM");
        task2.put("assignee", "admin");
        task2.put("createdDate", new Date());
        tasks.put("2", task2);
        taskCounter = 3;
        
        // Initialize sample users
        Map<String, Object> admin = new HashMap<>();
        admin.put("id", "1");
        admin.put("username", "admin");
        admin.put("email", "admin@api.com");
        admin.put("fullName", "Admin User");
        admin.put("role", "ADMIN");
        admin.put("active", true);
        admin.put("createdDate", new Date());
        users.put("1", admin);
        
        Map<String, Object> user = new HashMap<>();
        user.put("id", "2");
        user.put("username", "john_doe");
        user.put("email", "john@example.com");
        user.put("fullName", "John Doe");
        user.put("role", "USER");
        user.put("active", true);
        user.put("createdDate", new Date());
        users.put("2", user);
        userCounter = 3;
        
        // Initialize sample projects
        Map<String, Object> project1 = new HashMap<>();
        project1.put("id", "1");
        project1.put("name", "E-commerce Platform");
        project1.put("description", "Online shopping platform with advanced features");
        project1.put("status", "IN_PROGRESS");
        project1.put("priority", "HIGH");
        project1.put("owner", "admin");
        project1.put("createdDate", new Date());
        projects.put("1", project1);
        
        Map<String, Object> project2 = new HashMap<>();
        project2.put("id", "2");
        project2.put("name", "API Gateway");
        project2.put("description", "Microservices API gateway implementation");
        project2.put("status", "PLANNING");
        project2.put("priority", "MEDIUM");
        project2.put("owner", "john_doe");
        project2.put("createdDate", new Date());
        projects.put("2", project2);
        projectCounter = 3;
        
        // Initialize sample files
        Map<String, Object> file1 = new HashMap<>();
        file1.put("id", "1");
        file1.put("originalName", "sample-document.pdf");
        file1.put("fileName", "file_1_sample-document.pdf");
        file1.put("contentType", "application/pdf");
        file1.put("size", 1024000L);
        file1.put("uploadedBy", "admin");
        file1.put("uploadDate", new Date());
        file1.put("category", "document");
        files.put("1", file1);
        
        Map<String, Object> file2 = new HashMap<>();
        file2.put("id", "2");
        file2.put("originalName", "profile-image.jpg");
        file2.put("fileName", "file_2_profile-image.jpg");
        file2.put("contentType", "image/jpeg");
        file2.put("size", 512000L);
        file2.put("uploadedBy", "john_doe");
        file2.put("uploadDate", new Date());
        file2.put("category", "image");
        files.put("2", file2);
        fileCounter = 3;
        
        // Initialize auth data
        apiKeys.put("demo-api-key-admin", "admin");
        apiKeys.put("demo-api-key-user", "john_doe");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleApiRoot(response);
            } else if (pathInfo.startsWith("/tasks")) {
                handleTasksGet(request, response, pathInfo);
            } else if (pathInfo.startsWith("/users")) {
                handleUsersGet(request, response, pathInfo);
            } else if (pathInfo.startsWith("/projects")) {
                handleProjectsGet(request, response, pathInfo);
            } else if (pathInfo.startsWith("/files")) {
                handleFilesGet(request, response, pathInfo);
            } else if (pathInfo.startsWith("/analytics")) {
                handleAnalyticsGet(request, response, pathInfo);
            } else if (pathInfo.startsWith("/auth")) {
                handleAuthGet(request, response, pathInfo);
            } else {
                sendErrorResponse(response, 404, "API endpoint not found");
            }
        } catch (Exception e) {
            sendErrorResponse(response, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        
        try {
            if (pathInfo.startsWith("/tasks")) {
                handleTasksPost(request, response);
            } else if (pathInfo.startsWith("/users")) {
                handleUsersPost(request, response);
            } else if (pathInfo.startsWith("/projects")) {
                handleProjectsPost(request, response);
            } else if (pathInfo.startsWith("/files")) {
                handleFilesPost(request, response);
            } else if (pathInfo.startsWith("/auth")) {
                handleAuthPost(request, response, pathInfo);
            } else if (pathInfo.startsWith("/analytics")) {
                handleAnalyticsPost(request, response, pathInfo);
            } else {
                sendErrorResponse(response, 404, "API endpoint not found");
            }
        } catch (Exception e) {
            sendErrorResponse(response, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        
        try {
            if (pathInfo.startsWith("/tasks/")) {
                handleTasksPut(request, response, pathInfo);
            } else if (pathInfo.startsWith("/users/")) {
                handleUsersPut(request, response, pathInfo);
            } else if (pathInfo.startsWith("/projects/")) {
                handleProjectsPut(request, response, pathInfo);
            } else {
                sendErrorResponse(response, 404, "API endpoint not found");
            }
        } catch (Exception e) {
            sendErrorResponse(response, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        
        try {
            if (pathInfo.startsWith("/tasks/")) {
                handleTasksDelete(request, response, pathInfo);
            } else if (pathInfo.startsWith("/users/")) {
                handleUsersDelete(request, response, pathInfo);
            } else if (pathInfo.startsWith("/projects/")) {
                handleProjectsDelete(request, response, pathInfo);
            } else if (pathInfo.startsWith("/files/")) {
                handleFilesDelete(request, response, pathInfo);
            } else {
                sendErrorResponse(response, 404, "API endpoint not found");
            }
        } catch (Exception e) {
            sendErrorResponse(response, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-API-Key");
        response.setStatus(200);
    }
    
    // API Root
    private void handleApiRoot(HttpServletResponse response) throws IOException {
        Map<String, Object> apiInfo = new HashMap<>();
        apiInfo.put("name", "REST API Server");
        apiInfo.put("version", "1.0.0");
        apiInfo.put("description", "Comprehensive REST API for WAF training");
        apiInfo.put("timestamp", new Date());
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("tasks", "/api/tasks");
        endpoints.put("users", "/api/users");
        endpoints.put("projects", "/api/projects");
        endpoints.put("files", "/api/files");
        endpoints.put("auth", "/api/auth");
        endpoints.put("analytics", "/api/analytics");
        apiInfo.put("endpoints", endpoints);
        
        sendJsonResponse(response, 200, apiInfo);
    }
    
    // Tasks endpoints
    private void handleTasksGet(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        if (pathInfo.equals("/tasks") || pathInfo.equals("/tasks/")) {
            // List all tasks
            List<Map<String, Object>> taskList = new ArrayList<>(tasks.values());
            
            Map<String, Object> result = new HashMap<>();
            result.put("tasks", taskList);
            result.put("total", taskList.size());
            sendJsonResponse(response, 200, result);
        } else {
            // Get specific task
            String taskId = pathInfo.substring("/tasks/".length());
            Map<String, Object> task = tasks.get(taskId);
            
            if (task != null) {
                sendJsonResponse(response, 200, task);
            } else {
                sendErrorResponse(response, 404, "Task not found");
            }
        }
    }
    
    private void handleTasksPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> taskData = readJsonFromRequest(request);
        
        String title = (String) taskData.get("title");
        String description = (String) taskData.get("description");
        
        if (title == null || description == null) {
            sendErrorResponse(response, 400, "Title and description are required");
            return;
        }
        
        Map<String, Object> newTask = new HashMap<>();
        String taskId = String.valueOf(taskCounter++);
        newTask.put("id", taskId);
        newTask.put("title", title);
        newTask.put("description", description);
        newTask.put("status", taskData.getOrDefault("status", "TODO"));
        newTask.put("priority", taskData.getOrDefault("priority", "MEDIUM"));
        newTask.put("assignee", taskData.getOrDefault("assignee", "unassigned"));
        newTask.put("createdDate", new Date());
        
        tasks.put(taskId, newTask);
        sendJsonResponse(response, 201, newTask);
    }
    
    private void handleTasksPut(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        String taskId = pathInfo.substring("/tasks/".length());
        Map<String, Object> task = tasks.get(taskId);
        
        if (task == null) {
            sendErrorResponse(response, 404, "Task not found");
            return;
        }
        
        Map<String, Object> updates = readJsonFromRequest(request);
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            if (!"id".equals(entry.getKey()) && !"createdDate".equals(entry.getKey())) {
                task.put(entry.getKey(), entry.getValue());
            }
        }
        
        sendJsonResponse(response, 200, task);
    }
    
    private void handleTasksDelete(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        String taskId = pathInfo.substring("/tasks/".length());
        Map<String, Object> task = tasks.remove(taskId);
        
        if (task != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Task deleted successfully");
            result.put("deletedTask", task);
            sendJsonResponse(response, 200, result);
        } else {
            sendErrorResponse(response, 404, "Task not found");
        }
    }
    
    // Similar handlers for users, projects, files, auth, analytics...
    // (For brevity, I'll implement key ones)
    
    private void handleUsersGet(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        if (pathInfo.equals("/users") || pathInfo.equals("/users/")) {
            List<Map<String, Object>> userList = new ArrayList<>(users.values());
            Map<String, Object> result = new HashMap<>();
            result.put("users", userList);
            result.put("total", userList.size());
            sendJsonResponse(response, 200, result);
        } else {
            String userId = pathInfo.substring("/users/".length());
            Map<String, Object> user = users.get(userId);
            
            if (user != null) {
                sendJsonResponse(response, 200, user);
            } else {
                sendErrorResponse(response, 404, "User not found");
            }
        }
    }
    
    private void handleUsersPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> userData = readJsonFromRequest(request);
        
        String username = (String) userData.get("username");
        String email = (String) userData.get("email");
        
        if (username == null || email == null) {
            sendErrorResponse(response, 400, "Username and email are required");
            return;
        }
        
        Map<String, Object> newUser = new HashMap<>();
        String userId = String.valueOf(userCounter++);
        newUser.put("id", userId);
        newUser.put("username", username);
        newUser.put("email", email);
        newUser.put("fullName", userData.getOrDefault("fullName", username));
        newUser.put("role", userData.getOrDefault("role", "USER"));
        newUser.put("active", userData.getOrDefault("active", true));
        newUser.put("createdDate", new Date());
        
        users.put(userId, newUser);
        sendJsonResponse(response, 201, newUser);
    }
    
    private void handleUsersPut(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        String userId = pathInfo.substring("/users/".length());
        Map<String, Object> user = users.get(userId);
        
        if (user == null) {
            sendErrorResponse(response, 404, "User not found");
            return;
        }
        
        Map<String, Object> updates = readJsonFromRequest(request);
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            if (!"id".equals(entry.getKey()) && !"createdDate".equals(entry.getKey())) {
                user.put(entry.getKey(), entry.getValue());
            }
        }
        
        sendJsonResponse(response, 200, user);
    }
    
    private void handleUsersDelete(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        String userId = pathInfo.substring("/users/".length());
        Map<String, Object> user = users.remove(userId);
        
        if (user != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "User deleted successfully");
            result.put("deletedUser", user);
            sendJsonResponse(response, 200, result);
        } else {
            sendErrorResponse(response, 404, "User not found");
        }
    }
    
    // Simplified implementations for other endpoints
    private void handleProjectsGet(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        if (pathInfo.equals("/projects") || pathInfo.equals("/projects/")) {
            List<Map<String, Object>> projectList = new ArrayList<>(projects.values());
            Map<String, Object> result = new HashMap<>();
            result.put("projects", projectList);
            result.put("total", projectList.size());
            sendJsonResponse(response, 200, result);
        } else {
            String projectId = pathInfo.substring("/projects/".length());
            Map<String, Object> project = projects.get(projectId);
            
            if (project != null) {
                sendJsonResponse(response, 200, project);
            } else {
                sendErrorResponse(response, 404, "Project not found");
            }
        }
    }
    
    private void handleProjectsPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> projectData = readJsonFromRequest(request);
        
        String name = (String) projectData.get("name");
        if (name == null) {
            sendErrorResponse(response, 400, "Project name is required");
            return;
        }
        
        Map<String, Object> newProject = new HashMap<>();
        String projectId = String.valueOf(projectCounter++);
        newProject.put("id", projectId);
        newProject.put("name", name);
        newProject.put("description", projectData.getOrDefault("description", ""));
        newProject.put("status", projectData.getOrDefault("status", "PLANNING"));
        newProject.put("priority", projectData.getOrDefault("priority", "MEDIUM"));
        newProject.put("owner", projectData.getOrDefault("owner", "unassigned"));
        newProject.put("createdDate", new Date());
        
        projects.put(projectId, newProject);
        sendJsonResponse(response, 201, newProject);
    }
    
    private void handleProjectsPut(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        String projectId = pathInfo.substring("/projects/".length());
        Map<String, Object> project = projects.get(projectId);
        
        if (project == null) {
            sendErrorResponse(response, 404, "Project not found");
            return;
        }
        
        Map<String, Object> updates = readJsonFromRequest(request);
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            if (!"id".equals(entry.getKey()) && !"createdDate".equals(entry.getKey())) {
                project.put(entry.getKey(), entry.getValue());
            }
        }
        
        sendJsonResponse(response, 200, project);
    }
    
    private void handleProjectsDelete(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        String projectId = pathInfo.substring("/projects/".length());
        Map<String, Object> project = projects.remove(projectId);
        
        if (project != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Project deleted successfully");
            result.put("deletedProject", project);
            sendJsonResponse(response, 200, result);
        } else {
            sendErrorResponse(response, 404, "Project not found");
        }
    }
    
    private void handleFilesGet(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        if (pathInfo.equals("/files") || pathInfo.equals("/files/")) {
            List<Map<String, Object>> fileList = new ArrayList<>(files.values());
            Map<String, Object> result = new HashMap<>();
            result.put("files", fileList);
            result.put("total", fileList.size());
            sendJsonResponse(response, 200, result);
        } else {
            sendErrorResponse(response, 404, "File endpoint not implemented");
        }
    }
    
    private void handleFilesPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "File upload simulated (demo only)");
        result.put("fileId", String.valueOf(fileCounter++));
        sendJsonResponse(response, 201, result);
    }
    
    private void handleFilesDelete(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        String fileId = pathInfo.substring("/files/".length());
        Map<String, Object> file = files.remove(fileId);
        
        if (file != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "File deleted successfully");
            sendJsonResponse(response, 200, result);
        } else {
            sendErrorResponse(response, 404, "File not found");
        }
    }
    
    private void handleAuthGet(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        if (pathInfo.equals("/auth/profile")) {
            Map<String, Object> profile = new HashMap<>();
            profile.put("username", "demo-user");
            profile.put("role", "USER");
            profile.put("authenticated", true);
            sendJsonResponse(response, 200, profile);
        } else {
            sendErrorResponse(response, 404, "Auth endpoint not found");
        }
    }
    
    private void handleAuthPost(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        if (pathInfo.equals("/auth/login")) {
            Map<String, Object> credentials = readJsonFromRequest(request);
            String username = (String) credentials.get("username");
            String password = (String) credentials.get("password");
            
            if ("admin".equals(username) && "admin123".equals(password) ||
                "john_doe".equals(username) && "user123".equals(password)) {
                
                Map<String, Object> loginResult = new HashMap<>();
                loginResult.put("success", true);
                loginResult.put("token", "token_" + username + "_" + System.currentTimeMillis());
                loginResult.put("user", Map.of("username", username, "role", "admin".equals(username) ? "ADMIN" : "USER"));
                sendJsonResponse(response, 200, loginResult);
            } else {
                sendErrorResponse(response, 401, "Invalid credentials");
            }
        } else if (pathInfo.equals("/auth/register")) {
            Map<String, Object> userData = readJsonFromRequest(request);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "User registered successfully");
            result.put("userId", String.valueOf(userCounter++));
            sendJsonResponse(response, 201, result);
        } else {
            sendErrorResponse(response, 404, "Auth endpoint not found");
        }
    }
    
    private void handleAnalyticsGet(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        Map<String, Object> analytics = new HashMap<>();
        
        if (pathInfo.equals("/analytics") || pathInfo.equals("/analytics/")) {
            // Overview dashboard
            analytics.put("totalRequests", 15847);
            analytics.put("totalUsers", users.size());
            analytics.put("totalTasks", tasks.size());
            analytics.put("totalProjects", projects.size());
            analytics.put("systemHealth", "HEALTHY");
            analytics.put("uptime", "99.9%");
        } else if (pathInfo.equals("/analytics/api-usage")) {
            analytics.put("requestsByMethod", Map.of("GET", 8923, "POST", 3421, "PUT", 1876, "DELETE", 1234));
            analytics.put("statusCodes", Map.of("200", 12456, "201", 1876, "400", 512, "401", 234, "404", 123));
            analytics.put("averageResponseTime", 245.7);
        } else if (pathInfo.equals("/analytics/real-time")) {
            analytics.put("activeUsers", 89 + (int)(Math.random() * 20));
            analytics.put("requestsPerSecond", 125 + (int)(Math.random() * 50));
            analytics.put("averageResponseTime", 200 + Math.random() * 100);
            analytics.put("timestamp", new Date());
        } else {
            sendErrorResponse(response, 404, "Analytics endpoint not found");
            return;
        }
        
        sendJsonResponse(response, 200, analytics);
    }
    
    private void handleAnalyticsPost(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        if (pathInfo.equals("/analytics/report")) {
            Map<String, Object> reportRequest = readJsonFromRequest(request);
            Map<String, Object> report = new HashMap<>();
            report.put("id", "report_" + System.currentTimeMillis());
            report.put("type", reportRequest.getOrDefault("type", "comprehensive"));
            report.put("status", "completed");
            report.put("generatedAt", new Date());
            sendJsonResponse(response, 201, report);
        } else {
            sendErrorResponse(response, 404, "Analytics endpoint not found");
        }
    }
    
    // Utility methods
    private Map<String, Object> readJsonFromRequest(HttpServletRequest request) throws IOException {
        StringBuilder jsonBuilder = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonBuilder.append(line);
        }
        
        String json = jsonBuilder.toString();
        if (json.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            JSONObject jsonObject = new JSONObject(json);
            Map<String, Object> map = new HashMap<>();
            
            for (String key : jsonObject.keySet()) {
                map.put(key, jsonObject.get(key));
            }
            
            return map;
        } catch (JSONException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid JSON format");
            error.put("message", e.getMessage());
            return error;
        }
    }
    
    private void sendJsonResponse(HttpServletResponse response, int status, Object data) throws IOException {
        response.setStatus(status);
        response.getWriter().write(toJsonString(data));
    }
    
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", new Date());
        error.put("status", status);
        response.getWriter().write(toJsonString(error));
    }
    
    private String toJsonString(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        if (obj instanceof Map) {
            return mapToJsonString((Map<?, ?>) obj);
        } else if (obj instanceof List) {
            return listToJsonString((List<?>) obj);
        } else if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        } else if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        } else if (obj instanceof Date) {
            return String.valueOf(((Date) obj).getTime());
        } else {
            return "\"" + escapeJson(obj.toString()) + "\"";
        }
    }
    
    private String mapToJsonString(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            
            sb.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
            sb.append(toJsonString(entry.getValue()));
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    private String listToJsonString(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        
        for (Object item : list) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(toJsonString(item));
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\b", "\\b")
                 .replace("\f", "\\f")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
}
