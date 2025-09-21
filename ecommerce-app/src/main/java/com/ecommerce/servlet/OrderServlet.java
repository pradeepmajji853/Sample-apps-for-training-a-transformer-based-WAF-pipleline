package com.ecommerce.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class OrderServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        
        if ("checkout".equals(action)) {
            showCheckoutPage(request, response);
        } else {
            showOrderHistory(request, response);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        
        if ("place_order".equals(action)) {
            processOrder(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
    
    private void showCheckoutPage(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>Checkout</title></head><body>");
        out.println("<h1>Checkout</h1>");
        out.println("<form method='post'>");
        out.println("<input type='hidden' name='action' value='place_order'>");
        
        out.println("<h2>Billing Information</h2>");
        out.println("<p>Full Name: <input type='text' name='billing_name' required></p>");
        out.println("<p>Email: <input type='email' name='billing_email' required></p>");
        out.println("<p>Phone: <input type='tel' name='billing_phone' required></p>");
        out.println("<p>Address: <input type='text' name='billing_address' required></p>");
        out.println("<p>City: <input type='text' name='billing_city' required></p>");
        out.println("<p>ZIP Code: <input type='text' name='billing_zip' required></p>");
        
        out.println("<h2>Payment Information</h2>");
        out.println("<p>Card Number: <input type='text' name='card_number' pattern='[0-9]{16}' required></p>");
        out.println("<p>Expiry Date: <input type='month' name='card_expiry' required></p>");
        out.println("<p>CVV: <input type='text' name='card_cvv' pattern='[0-9]{3,4}' required></p>");
        out.println("<p>Cardholder Name: <input type='text' name='card_name' required></p>");
        
        out.println("<h2>Shipping Options</h2>");
        out.println("<p><input type='radio' name='shipping' value='standard' checked> Standard Shipping ($5.99)</p>");
        out.println("<p><input type='radio' name='shipping' value='express'> Express Shipping ($12.99)</p>");
        out.println("<p><input type='radio' name='shipping' value='overnight'> Overnight Shipping ($24.99)</p>");
        
        out.println("<p><input type='submit' value='Place Order'></p>");
        out.println("</form>");
        
        out.println("<p><a href='/ecommerce/cart'>Back to Cart</a></p>");
        out.println("</body></html>");
    }
    
    private void processOrder(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        // Extract form data
        String billingName = request.getParameter("billing_name");
        String billingEmail = request.getParameter("billing_email");
        String cardNumber = request.getParameter("card_number");
        String shipping = request.getParameter("shipping");
        
        // Generate order number
        long orderNumber = System.currentTimeMillis() % 1000000;
        
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>Order Confirmation</title></head><body>");
        out.println("<h1>Order Confirmation</h1>");
        out.println("<p>Thank you for your order, " + billingName + "!</p>");
        out.println("<p><strong>Order Number:</strong> #" + orderNumber + "</p>");
        out.println("<p><strong>Email:</strong> " + billingEmail + "</p>");
        out.println("<p><strong>Shipping:</strong> " + shipping + " shipping</p>");
        out.println("<p>A confirmation email has been sent to your email address.</p>");
        out.println("<p>You can track your order status in your <a href='/ecommerce/orders'>order history</a>.</p>");
        out.println("<p><a href='/ecommerce/products'>Continue Shopping</a></p>");
        out.println("</body></html>");
        
        // Clear the cart
        request.getSession().removeAttribute("cart");
    }
    
    private void showOrderHistory(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>Order History</title></head><body>");
        out.println("<h1>Order History</h1>");
        
        // Mock order history
        out.println("<table border='1'>");
        out.println("<tr><th>Order #</th><th>Date</th><th>Total</th><th>Status</th><th>Actions</th></tr>");
        
        for (int i = 1; i <= 3; i++) {
            out.println("<tr>");
            out.println("<td>#100" + i + "</td>");
            out.println("<td>2025-09-" + (20 - i) + "</td>");
            out.println("<td>$" + (199.99 + i * 50) + "</td>");
            out.println("<td>" + (i == 1 ? "Delivered" : i == 2 ? "Shipped" : "Processing") + "</td>");
            out.println("<td><a href='/ecommerce/orders/" + i + "'>View Details</a></td>");
            out.println("</tr>");
        }
        
        out.println("</table>");
        out.println("<p><a href='/ecommerce/products'>Continue Shopping</a></p>");
        out.println("</body></html>");
    }
}
