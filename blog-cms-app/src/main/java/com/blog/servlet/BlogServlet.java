package com.blog.servlet;

import com.blog.model.BlogPost;
import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BlogServlet extends HttpServlet {
    private static final Map<Integer, BlogPost> blogs = new ConcurrentHashMap<>();
    private static final AtomicInteger blogIdCounter = new AtomicInteger(1);
    private static final Gson gson = new Gson();
    
    static {
        // Initialize with sample blog posts
        BlogPost post1 = new BlogPost("Welcome to Our Blog", 
            "This is our first blog post. Welcome to our amazing blog where we share insights about technology, programming, and web security.", 
            "admin");
        post1.setId(blogIdCounter.getAndIncrement());
        post1.setTags(Arrays.asList("welcome", "blog", "introduction"));
        post1.setCategory("general");
        post1.setStatus("published");
        blogs.put(post1.getId(), post1);
        
        BlogPost post2 = new BlogPost("Understanding Web Security", 
            "Web security is crucial in today's digital world. In this post, we explore common vulnerabilities and how to protect against them.", 
            "security_expert");
        post2.setId(blogIdCounter.getAndIncrement());
        post2.setTags(Arrays.asList("security", "web", "cybersecurity"));
        post2.setCategory("security");
        post2.setStatus("published");
        blogs.put(post2.getId(), post2);
            
        BlogPost post3 = new BlogPost("Modern JavaScript Frameworks", 
            "A comprehensive guide to popular JavaScript frameworks like React, Vue, and Angular. Learn which one suits your project best.", 
            "dev_blogger");
        post3.setId(blogIdCounter.getAndIncrement());
        post3.setTags(Arrays.asList("javascript", "frameworks", "frontend"));
        post3.setCategory("programming");
        post3.setStatus("published");
        blogs.put(post3.getId(), post3);
            
        BlogPost post4 = new BlogPost("Database Design Best Practices", 
            "Learn how to design efficient and scalable databases. This post covers normalization, indexing, and performance optimization.", 
            "db_expert");
        post4.setId(blogIdCounter.getAndIncrement());
        post4.setTags(Arrays.asList("database", "design", "performance"));
        post4.setCategory("database");
        post4.setStatus("published");
        blogs.put(post4.getId(), post4);
            
        BlogPost post5 = new BlogPost("Cloud Computing Trends 2025", 
            "Explore the latest trends in cloud computing including serverless architecture, containers, and multi-cloud strategies.", 
            "cloud_architect");
        post5.setId(blogIdCounter.getAndIncrement());
        post5.setTags(Arrays.asList("cloud", "trends", "technology"));
        post5.setCategory("technology");
        post5.setStatus("draft");
        blogs.put(post5.getId(), post5);
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
                // Get all blogs with optional filtering
                String category = request.getParameter("category");
                String status = request.getParameter("status");
                String author = request.getParameter("author");
                String tags = request.getParameter("tags");
                String search = request.getParameter("search");
                String sortBy = request.getParameter("sortBy");
                String sortOrder = request.getParameter("sortOrder");
                
                // Pagination
                String pageParam = request.getParameter("page");
                String limitParam = request.getParameter("limit");
                
                int page = 1;
                int limit = 10;
                
                try {
                    if (pageParam != null) page = Integer.parseInt(pageParam);
                    if (limitParam != null) limit = Integer.parseInt(limitParam);
                } catch (NumberFormatException e) {
                    // Use defaults
                }
                
                List<BlogPost> filteredBlogs = new ArrayList<>();
                
                for (BlogPost blog : blogs.values()) {
                    boolean include = true;
                    
                    if (category != null && !category.isEmpty()) {
                        if (!category.equalsIgnoreCase(blog.getCategory())) {
                            include = false;
                        }
                    }
                    
                    if (status != null && !status.isEmpty()) {
                        if (!status.equalsIgnoreCase(blog.getStatus())) {
                            include = false;
                        }
                    }
                    
                    if (author != null && !author.isEmpty()) {
                        if (!author.equalsIgnoreCase(blog.getAuthor())) {
                            include = false;
                        }
                    }
                    
                    if (tags != null && !tags.isEmpty()) {
                        String[] tagArray = tags.split(",");
                        boolean hasTag = false;
                        for (String tag : tagArray) {
                            if (blog.getTags().contains(tag.trim())) {
                                hasTag = true;
                                break;
                            }
                        }
                        if (!hasTag) include = false;
                    }
                    
                    if (search != null && !search.isEmpty()) {
                        String searchLower = search.toLowerCase();
                        if (!blog.getTitle().toLowerCase().contains(searchLower) &&
                            !blog.getContent().toLowerCase().contains(searchLower) &&
                            !blog.getAuthor().toLowerCase().contains(searchLower) &&
                            !blog.getCategory().toLowerCase().contains(searchLower)) {
                            include = false;
                        }
                    }
                    
                    if (include) {
                        filteredBlogs.add(blog);
                    }
                }
                
                // Sort blogs
                if ("title".equals(sortBy)) {
                    filteredBlogs.sort((a, b) -> "desc".equals(sortOrder) ? 
                        b.getTitle().compareTo(a.getTitle()) : a.getTitle().compareTo(b.getTitle()));
                } else if ("author".equals(sortBy)) {
                    filteredBlogs.sort((a, b) -> "desc".equals(sortOrder) ? 
                        b.getAuthor().compareTo(a.getAuthor()) : a.getAuthor().compareTo(b.getAuthor()));
                } else if ("category".equals(sortBy)) {
                    filteredBlogs.sort((a, b) -> "desc".equals(sortOrder) ? 
                        b.getCategory().compareTo(a.getCategory()) : a.getCategory().compareTo(b.getCategory()));
                } else {
                    // Default sort by date (newest first)
                    filteredBlogs.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                }
                
                // Apply pagination
                int start = (page - 1) * limit;
                int end = Math.min(start + limit, filteredBlogs.size());
                
                List<BlogPost> paginatedBlogs = filteredBlogs.subList(start, end);
                
                out.print(buildBlogsJson(paginatedBlogs, filteredBlogs.size(), page, limit));
                
            } else {
                // Get specific blog by ID
                String idStr = pathInfo.substring(1);
                try {
                    int blogId = Integer.parseInt(idStr);
                    BlogPost blog = blogs.get(blogId);
                    
                    if (blog != null) {
                        // Increment view count
                        blog.incrementViewCount();
                        out.print(buildBlogJson(blog));
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        out.print("{\"error\": \"Blog post not found\"}");
                    }
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\": \"Invalid blog ID\"}");
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
            String title = request.getParameter("title");
            String content = request.getParameter("content");
            String author = request.getParameter("author");
            String category = request.getParameter("category");
            String status = request.getParameter("status");
            String tagsParam = request.getParameter("tags");
            
            if (title == null || content == null || author == null ||
                title.trim().isEmpty() || content.trim().isEmpty() || author.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Missing required fields: title, content, author\"}");
                return;
            }
            
            BlogPost newBlog = new BlogPost(title.trim(), content.trim(), author.trim());
            newBlog.setId(blogIdCounter.getAndIncrement());
            
            if (category != null && !category.trim().isEmpty()) {
                newBlog.setCategory(category.trim());
            }
            
            if (status != null && !status.trim().isEmpty()) {
                newBlog.setStatus(status.trim());
            } else {
                newBlog.setStatus("draft");
            }
            
            if (tagsParam != null && !tagsParam.trim().isEmpty()) {
                newBlog.setTagsFromString(tagsParam.trim());
            }
            
            blogs.put(newBlog.getId(), newBlog);
            
            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print(buildBlogJson(newBlog));
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to create blog post\"}");
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
            out.print("{\"error\": \"Blog ID is required\"}");
            return;
        }
        
        try {
            String idStr = pathInfo.substring(1);
            int blogId = Integer.parseInt(idStr);
            BlogPost blog = blogs.get(blogId);
            
            if (blog == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Blog post not found\"}");
                return;
            }
            
            String title = request.getParameter("title");
            String content = request.getParameter("content");
            String author = request.getParameter("author");
            String category = request.getParameter("category");
            String status = request.getParameter("status");
            String tagsParam = request.getParameter("tags");
            
            if (title != null && !title.trim().isEmpty()) {
                blog.setTitle(title.trim());
            }
            
            if (content != null && !content.trim().isEmpty()) {
                blog.setContent(content.trim());
            }
            
            if (author != null && !author.trim().isEmpty()) {
                blog.setAuthor(author.trim());
            }
            
            if (category != null && !category.trim().isEmpty()) {
                blog.setCategory(category.trim());
            }
            
            if (status != null && !status.trim().isEmpty()) {
                blog.setStatus(status.trim());
            }
            
            if (tagsParam != null) {
                blog.setTagsFromString(tagsParam.trim());
            }
            
            blog.setUpdatedAt(java.time.LocalDateTime.now());
            
            out.print(buildBlogJson(blog));
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Invalid blog ID\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to update blog post\"}");
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
            out.print("{\"error\": \"Blog ID is required\"}");
            return;
        }
        
        try {
            String idStr = pathInfo.substring(1);
            int blogId = Integer.parseInt(idStr);
            
            BlogPost removed = blogs.remove(blogId);
            
            if (removed != null) {
                out.print("{\"message\": \"Blog post deleted successfully\", \"id\": " + blogId + "}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Blog post not found\"}");
            }
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Invalid blog ID\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Failed to delete blog post\"}");
        }
    }
    
    private String buildBlogsJson(List<BlogPost> blogList, int total, int page, int limit) {
        StringBuilder json = new StringBuilder();
        json.append("{\"blogs\": [");
        
        for (int i = 0; i < blogList.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(buildBlogJson(blogList.get(i)));
        }
        
        json.append("], \"pagination\": {");
        json.append("\"total\": ").append(total).append(", ");
        json.append("\"page\": ").append(page).append(", ");
        json.append("\"limit\": ").append(limit).append(", ");
        json.append("\"totalPages\": ").append((int) Math.ceil((double) total / limit));
        json.append("}}");
        
        return json.toString();
    }
    
    private String buildBlogJson(BlogPost blog) {
        return String.format(
            "{\"id\": %d, \"title\": \"%s\", \"content\": \"%s\", \"author\": \"%s\", " +
            "\"category\": \"%s\", \"status\": \"%s\", \"tags\": [%s], " +
            "\"createdAt\": \"%s\", \"updatedAt\": \"%s\", \"viewCount\": %d}",
            blog.getId(),
            escapeJson(blog.getTitle()),
            escapeJson(blog.getContent()),
            escapeJson(blog.getAuthor()),
            blog.getCategory(),
            blog.getStatus(),
            blog.getTags().stream().map(tag -> "\"" + escapeJson(tag) + "\"")
                .reduce((a, b) -> a + ", " + b).orElse(""),
            blog.getCreatedAt().toString(),
            blog.getUpdatedAt().toString(),
            blog.getViewCount()
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
