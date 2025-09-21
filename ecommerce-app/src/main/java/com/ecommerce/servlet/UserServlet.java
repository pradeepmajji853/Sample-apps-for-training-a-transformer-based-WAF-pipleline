package com.ecommerce.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class UserServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String action = request.getPathInfo();
        if (action == null) action = "/";
        
        switch (action) {
            case "/login":
                showLoginForm(request, response);
                break;
            case "/register":
                showRegistrationForm(request, response);
                break;
            case "/logout":
                handleLogout(request, response);
                break;
            case "/profile":
                showProfile(request, response);
                break;
            default:
                showUserHome(request, response);
                break;
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String action = request.getPathInfo();
        if (action == null) action = "/";
        
        switch (action) {
            case "/login":
                handleLogin(request, response);
                break;
            case "/register":
                handleRegistration(request, response);
                break;
            case "/profile":
                handleProfileUpdate(request, response);
                break;
            default:
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }
    
    private void showLoginForm(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>Login</title></head><body>");
        out.println("<h1>Login</h1>");
        out.println("<form method='post' action='/ecommerce/user/login'>");
        out.println("<p>Username: <input type='text' name='username' required></p>");
        out.println("<p>Password: <input type='password' name='password' required></p>");
        out.println("<p><input type='submit' value='Login'></p>");
        out.println("</form>");
        out.println("<a href='/ecommerce/user/register'>Don't have an account? Register here</a>");
        out.println("</body></html>");
    }
    
    private void handleLogin(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        
        // Simple authentication (in real app, use database and hashing)
        if (username != null && password != null && 
            (username.equals("admin") || username.equals("user")) && 
            password.equals("password123")) {
            
            HttpSession session = request.getSession();
            session.setAttribute("username", username);
            session.setAttribute("isLoggedIn", true);
            
            response.sendRedirect("/ecommerce/user/profile");
        } else {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<html><body>");
            out.println("<h1>Login Failed</h1>");
            out.println("<p>Invalid username or password.</p>");
            out.println("<a href='/ecommerce/user/login'>Try again</a>");
            out.println("</body></html>");
        }
    }
    
    private void showRegistrationForm(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>Register</title></head><body>");
        out.println("<h1>Create Account</h1>");
        out.println("<form method='post' action='/ecommerce/user/register'>");
        out.println("<p>Username: <input type='text' name='username' required></p>");
        out.println("<p>Email: <input type='email' name='email' required></p>");
        out.println("<p>Password: <input type='password' name='password' required></p>");
        out.println("<p>Confirm Password: <input type='password' name='confirmPassword' required></p>");
        out.println("<p>Full Name: <input type='text' name='fullName' required></p>");
        out.println("<p><input type='submit' value='Register'></p>");
        out.println("</form>");
        out.println("<a href='/ecommerce/user/login'>Already have an account? Login here</a>");
        out.println("</body></html>");
    }
    
    private void handleRegistration(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String fullName = request.getParameter("fullName");
        
        if (!password.equals(confirmPassword)) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<html><body>");
            out.println("<h1>Registration Failed</h1>");
            out.println("<p>Passwords do not match.</p>");
            out.println("<a href='/ecommerce/user/register'>Try again</a>");
            out.println("</body></html>");
            return;
        }
        
        // Simulate successful registration
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>Registration Successful</h1>");
        out.println("<p>Welcome, " + fullName + "! Your account has been created.</p>");
        out.println("<a href='/ecommerce/user/login'>Login now</a>");
        out.println("</body></html>");
    }
    
    private void showProfile(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        HttpSession session = request.getSession();
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        
        if (isLoggedIn == null || !isLoggedIn) {
            response.sendRedirect("/ecommerce/user/login");
            return;
        }
        
        String username = (String) session.getAttribute("username");
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>User Profile</title></head><body>");
        out.println("<h1>User Profile</h1>");
        out.println("<p>Welcome, " + username + "!</p>");
        out.println("<h2>Account Information</h2>");
        out.println("<form method='post' action='/ecommerce/user/profile'>");
        out.println("<p>Username: " + username + "</p>");
        out.println("<p>Email: <input type='email' name='email' value='user@example.com'></p>");
        out.println("<p>Full Name: <input type='text' name='fullName' value='Sample User'></p>");
        out.println("<p><input type='submit' value='Update Profile'></p>");
        out.println("</form>");
        out.println("<p><a href='/ecommerce/orders'>View Orders</a></p>");
        out.println("<p><a href='/ecommerce/user/logout'>Logout</a></p>");
        out.println("</body></html>");
    }
    
    private void handleProfileUpdate(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>Profile Updated</h1>");
        out.println("<p>Your profile has been updated successfully.</p>");
        out.println("<a href='/ecommerce/user/profile'>Back to Profile</a>");
        out.println("</body></html>");
    }
    
    private void handleLogout(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        HttpSession session = request.getSession();
        session.invalidate();
        response.sendRedirect("/ecommerce/products");
    }
    
    private void showUserHome(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>User Account</title></head><body>");
        out.println("<h1>User Account</h1>");
        out.println("<p><a href='/ecommerce/user/login'>Login</a></p>");
        out.println("<p><a href='/ecommerce/user/register'>Register</a></p>");
        out.println("<p><a href='/ecommerce/products'>Browse Products</a></p>");
        out.println("</body></html>");
    }
}
