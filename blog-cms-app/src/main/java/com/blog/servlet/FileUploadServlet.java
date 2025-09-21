package com.blog.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@MultipartConfig(
    fileSizeThreshold = 1024 * 1024, // 1MB
    maxFileSize = 1024 * 1024 * 10,  // 10MB
    maxRequestSize = 1024 * 1024 * 50 // 50MB
)
public class FileUploadServlet extends HttpServlet {
    private static final String UPLOAD_DIR = "uploads";
    private static final List<String> allowedExtensions = List.of(
        "jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "txt", "zip"
    );
    
    @Override
    public void init() throws ServletException {
        // Create upload directory if it doesn't exist
        String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIR;
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIR;
            
            List<String> uploadedFiles = new ArrayList<>();
            
            for (Part part : request.getParts()) {
                String fileName = extractFileName(part);
                if (fileName != null && !fileName.isEmpty()) {
                    
                    // Validate file extension
                    String extension = getFileExtension(fileName).toLowerCase();
                    if (!allowedExtensions.contains(extension)) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        out.print("{\"error\": \"File type not allowed: " + extension + "\"}");
                        return;
                    }
                    
                    // Generate unique filename
                    String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
                    String filePath = uploadPath + File.separator + uniqueFileName;
                    
                    // Write file
                    part.write(filePath);
                    
                    uploadedFiles.add(uniqueFileName);
                }
            }
            
            if (uploadedFiles.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"No files uploaded\"}");
            } else {
                StringBuilder json = new StringBuilder();
                json.append("{\"message\": \"Files uploaded successfully\", \"files\": [");
                
                for (int i = 0; i < uploadedFiles.size(); i++) {
                    if (i > 0) json.append(", ");
                    json.append("\"").append(uploadedFiles.get(i)).append("\"");
                }
                
                json.append("]}");
                out.print(json.toString());
            }
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"File upload failed: " + e.getMessage() + "\"}");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            // List all uploaded files
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            
            String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIR;
            File uploadDir = new File(uploadPath);
            
            List<String> files = new ArrayList<>();
            if (uploadDir.exists()) {
                File[] fileArray = uploadDir.listFiles();
                if (fileArray != null) {
                    for (File file : fileArray) {
                        if (file.isFile()) {
                            files.add(file.getName());
                        }
                    }
                }
            }
            
            StringBuilder json = new StringBuilder();
            json.append("{\"files\": [");
            
            for (int i = 0; i < files.size(); i++) {
                if (i > 0) json.append(", ");
                json.append("\"").append(files.get(i)).append("\"");
            }
            
            json.append("]}");
            out.print(json.toString());
            
        } else {
            // Download specific file
            String fileName = pathInfo.substring(1);
            String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIR;
            String filePath = uploadPath + File.separator + fileName;
            
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                response.setContentType(getServletContext().getMimeType(fileName));
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                
                Files.copy(Paths.get(filePath), response.getOutputStream());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"error\": \"File not found\"}");
            }
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"File name is required\"}");
            return;
        }
        
        try {
            String fileName = pathInfo.substring(1);
            String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIR;
            String filePath = uploadPath + File.separator + fileName;
            
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                if (file.delete()) {
                    out.print("{\"message\": \"File deleted successfully\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print("{\"error\": \"Failed to delete file\"}");
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"File not found\"}");
            }
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"File deletion failed\"}");
        }
    }
    
    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length() - 1);
            }
        }
        return null;
    }
    
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex + 1);
    }
}
