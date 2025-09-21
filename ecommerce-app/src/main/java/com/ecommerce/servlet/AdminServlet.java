package com.ecommerce.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class AdminServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Simple admin check
        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }
        
        String action = request.getPathInfo();
        if (action == null) action = "/";
        
        switch (action) {
            case "/products":
                showProductManagement(request, response);
                break;
            case "/orders":
                showOrderManagement(request, response);
                break;
            case "/users":
                showUserManagement(request, response);
                break;
            default:
                showAdminDashboard(request, response);
                break;
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }
        
        String action = request.getParameter("action");
        
        switch (action) {
            case "add_product":
                handleAddProduct(request, response);
                break;
            case "update_order":
                handleUpdateOrder(request, response);
                break;
            default:
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }
    
    private boolean isAdmin(HttpServletRequest request) {
        // Simple admin check - in real app, use proper authentication
        String username = (String) request.getSession().getAttribute("username");
        return "admin".equals(username);
    }
    
    private void showAdminDashboard(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>Admin Dashboard</title></head><body>");
        out.println("<h1>Admin Dashboard</h1>");
        out.println("<div>");
        out.println("<h2>Quick Stats</h2>");
        out.println("<p>Total Products: 25</p>");
        out.println("<p>Pending Orders: 5</p>");
        out.println("<p>Total Users: 150</p>");
        out.println("<p>Revenue Today: $2,459.50</p>");
        out.println("</div>");
        
        out.println("<div>");
        out.println("<h2>Management</h2>");
        out.println("<p><a href='/ecommerce/admin/products'>Manage Products</a></p>");
        out.println("<p><a href='/ecommerce/admin/orders'>Manage Orders</a></p>");
        out.println("<p><a href='/ecommerce/admin/users'>Manage Users</a></p>");
        out.println("</div>");
        out.println("</body></html>");
    }
    
    private void showProductManagement(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>Product Management</title></head><body>");
        out.println("<h1>Product Management</h1>");
        
        out.println("<h2>Add New Product</h2>");
        out.println("<form method='post'>");
        out.println("<input type='hidden' name='action' value='add_product'>");
        out.println("<p>Name: <input type='text' name='product_name' required></p>");
        out.println("<p>Description: <textarea name='product_description' required></textarea></p>");
        out.println("<p>Price: $<input type='number' name='product_price' step='0.01' required></p>");
        out.println("<p>Category: <select name='product_category'>");
        out.println("<option value='Electronics'>Electronics</option>");
        out.println("<option value='Books'>Books</option>");
        out.println("<option value='Clothing'>Clothing</option>");
        out.println("</select></p>");
        out.println("<p><input type='submit' value='Add Product'></p>");
        out.println("</form>");
        
        out.println("<h2>Existing Products</h2>");
        out.println("<table border='1'>");
        out.println("<tr><th>ID</th><th>Name</th><th>Price</th><th>Category</th><th>Actions</th></tr>");
        
        // Mock product list
        for (int i = 1; i <= 5; i++) {
            out.println("<tr>");
            out.println("<td>" + i + "</td>");
            out.println("<td>Product " + i + "</td>");
            out.println("<td>$" + (99.99 + i * 50) + "</td>");
            out.println("<td>Electronics</td>");
            out.println("<td><a href='#'>Edit</a> | <a href='#'>Delete</a></td>");
            out.println("</tr>");
        }
        out.println("</table>");
        
        out.println("<p><a href='/ecommerce/admin'>Back to Dashboard</a></p>");
        out.println("</body></html>");
    }
    
    private void showOrderManagement(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>Order Management</title></head><body>");
        out.println("<h1>Order Management</h1>");
        
        out.println("<table border='1'>");
        out.println("<tr><th>Order #</th><th>Customer</th><th>Total</th><th>Status</th><th>Actions</th></tr>");
        
        // Mock order list
        String[] statuses = {"Processing", "Shipped", "Delivered", "Cancelled"};
        for (int i = 1; i <= 10; i++) {
            out.println("<tr>");
            out.println("<td>#100" + i + "</td>");
            out.println("<td>customer" + i + "@example.com</td>");
            out.println("<td>$" + (199.99 + i * 25) + "</td>");
            out.println("<td>" + statuses[i % 4] + "</td>");
            out.println("<td>");
            out.println("<form method='post' style='display:inline;'>");
            out.println("<input type='hidden' name='action' value='update_order'>");
            out.println("<input type='hidden' name='order_id' value='" + i + "'>");
            out.println("<select name='new_status'>");
            for (String status : statuses) {
                out.println("<option value='" + status + "'>" + status + "</option>");
            }
            out.println("</select>");
            out.println("<input type='submit' value='Update'>");
            out.println("</form>");
            out.println("</td>");
            out.println("</tr>");
        }
        out.println("</table>");
        
        out.println("<p><a href='/ecommerce/admin'>Back to Dashboard</a></p>");
        out.println("</body></html>");
    }
    
    private void showUserManagement(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>User Management</title></head><body>");
        out.println("<h1>User Management</h1>");
        
        out.println("<table border='1'>");
        out.println("<tr><th>ID</th><th>Username</th><th>Email</th><th>Registration Date</th><th>Status</th></tr>");
        
        // Mock user list
        for (int i = 1; i <= 10; i++) {
            out.println("<tr>");
            out.println("<td>" + i + "</td>");
            out.println("<td>user" + i + "</td>");
            out.println("<td>user" + i + "@example.com</td>");
            out.println("<td>2025-09-" + (10 + i) + "</td>");
            out.println("<td>Active</td>");
            out.println("</tr>");
        }
        out.println("</table>");
        
        out.println("<p><a href='/ecommerce/admin'>Back to Dashboard</a></p>");
        out.println("</body></html>");
    }
    
    private void handleAddProduct(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        String name = request.getParameter("product_name");
        String description = request.getParameter("product_description");
        String price = request.getParameter("product_price");
        String category = request.getParameter("product_category");
        
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>Product Added</h1>");
        out.println("<p>Product '" + name + "' has been added successfully.</p>");
        out.println("<a href='/ecommerce/admin/products'>Back to Product Management</a>");
        out.println("</body></html>");
    }
    
    private void handleUpdateOrder(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        String orderId = request.getParameter("order_id");
        String newStatus = request.getParameter("new_status");
        
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>Order Updated</h1>");
        out.println("<p>Order #100" + orderId + " status updated to: " + newStatus + "</p>");
        out.println("<a href='/ecommerce/admin/orders'>Back to Order Management</a>");
        out.println("</body></html>");
    }
}
