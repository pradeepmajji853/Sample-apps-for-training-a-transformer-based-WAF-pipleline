package com.ecommerce.servlet;

import com.ecommerce.model.Product;
import com.ecommerce.model.CartItem;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingCartServlet extends HttpServlet {
    
    private static final Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        String format = request.getParameter("format");
        HttpSession session = request.getSession();
        
        @SuppressWarnings("unchecked")
        Map<Integer, CartItem> cart = (Map<Integer, CartItem>) session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
        }
        
        if ("add".equals(action)) {
            handleAddToCart(request, response, cart, session);
            return;
        } else if ("remove".equals(action)) {
            handleRemoveFromCart(request, response, cart, session);
            return;
        } else if ("update".equals(action)) {
            handleUpdateCart(request, response, cart, session);
            return;
        } else if ("clear".equals(action)) {
            cart.clear();
            session.setAttribute("cart", cart);
            response.sendRedirect("/ecommerce/cart");
            return;
        }
        
        // Display cart
        if ("json".equals(format)) {
            response.setContentType("application/json");
            List<CartItem> cartItems = new ArrayList<>(cart.values());
            Map<String, Object> cartData = new HashMap<>();
            cartData.put("items", cartItems);
            cartData.put("total", calculateTotal(cart));
            cartData.put("itemCount", cart.size());
            response.getWriter().write(gson.toJson(cartData));
        } else {
            displayCart(request, response, cart);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
    
    private void handleAddToCart(HttpServletRequest request, HttpServletResponse response,
                                Map<Integer, CartItem> cart, HttpSession session) throws IOException {
        try {
            int productId = Integer.parseInt(request.getParameter("product_id"));
            int quantity = 1;
            
            String quantityParam = request.getParameter("quantity");
            if (quantityParam != null && !quantityParam.isEmpty()) {
                quantity = Integer.parseInt(quantityParam);
            }
            
            // In a real app, you'd fetch product from database
            Product product = getProductById(productId);
            if (product != null) {
                if (cart.containsKey(productId)) {
                    CartItem existingItem = cart.get(productId);
                    existingItem.setQuantity(existingItem.getQuantity() + quantity);
                } else {
                    cart.put(productId, new CartItem(product, quantity));
                }
                session.setAttribute("cart", cart);
            }
            
            String redirectUrl = request.getParameter("redirect");
            if (redirectUrl != null) {
                response.sendRedirect(redirectUrl);
            } else {
                response.sendRedirect("/ecommerce/cart");
            }
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID or quantity");
        }
    }
    
    private void handleRemoveFromCart(HttpServletRequest request, HttpServletResponse response,
                                     Map<Integer, CartItem> cart, HttpSession session) throws IOException {
        try {
            int productId = Integer.parseInt(request.getParameter("product_id"));
            cart.remove(productId);
            session.setAttribute("cart", cart);
            response.sendRedirect("/ecommerce/cart");
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID");
        }
    }
    
    private void handleUpdateCart(HttpServletRequest request, HttpServletResponse response,
                                 Map<Integer, CartItem> cart, HttpSession session) throws IOException {
        try {
            int productId = Integer.parseInt(request.getParameter("product_id"));
            int quantity = Integer.parseInt(request.getParameter("quantity"));
            
            if (quantity <= 0) {
                cart.remove(productId);
            } else if (cart.containsKey(productId)) {
                cart.get(productId).setQuantity(quantity);
            }
            
            session.setAttribute("cart", cart);
            response.sendRedirect("/ecommerce/cart");
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID or quantity");
        }
    }
    
    private void displayCart(HttpServletRequest request, HttpServletResponse response,
                           Map<Integer, CartItem> cart) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>Shopping Cart</title></head><body>");
        out.println("<h1>Shopping Cart</h1>");
        
        if (cart.isEmpty()) {
            out.println("<p>Your cart is empty.</p>");
            out.println("<a href='/ecommerce/products'>Continue Shopping</a>");
        } else {
            out.println("<table border='1'>");
            out.println("<tr><th>Product</th><th>Price</th><th>Quantity</th><th>Subtotal</th><th>Actions</th></tr>");
            
            double total = 0;
            for (CartItem item : cart.values()) {
                double subtotal = item.getProduct().getPrice() * item.getQuantity();
                total += subtotal;
                
                out.println("<tr>");
                out.println("<td>" + item.getProduct().getName() + "</td>");
                out.println("<td>$" + item.getProduct().getPrice() + "</td>");
                out.println("<td>");
                out.println("<form method='post' style='display:inline;'>");
                out.println("<input type='hidden' name='action' value='update'>");
                out.println("<input type='hidden' name='product_id' value='" + item.getProduct().getId() + "'>");
                out.println("<input type='number' name='quantity' value='" + item.getQuantity() + "' min='1' style='width:60px;'>");
                out.println("<input type='submit' value='Update'>");
                out.println("</form>");
                out.println("</td>");
                out.println("<td>$" + String.format("%.2f", subtotal) + "</td>");
                out.println("<td><a href='/ecommerce/cart?action=remove&product_id=" + 
                           item.getProduct().getId() + "'>Remove</a></td>");
                out.println("</tr>");
            }
            
            out.println("<tr><td colspan='3'><strong>Total</strong></td><td><strong>$" + 
                       String.format("%.2f", total) + "</strong></td><td></td></tr>");
            out.println("</table>");
            
            out.println("<br>");
            out.println("<a href='/ecommerce/products'>Continue Shopping</a> | ");
            out.println("<a href='/ecommerce/orders?action=checkout'>Checkout</a> | ");
            out.println("<a href='/ecommerce/cart?action=clear'>Clear Cart</a>");
        }
        
        out.println("</body></html>");
    }
    
    private double calculateTotal(Map<Integer, CartItem> cart) {
        return cart.values().stream()
            .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
            .sum();
    }
    
    private Product getProductById(int id) {
        // Simplified product lookup - in real app, use database
        List<Product> products = List.of(
            new Product(1, "Laptop", "High-performance laptop", 999.99, "Electronics"),
            new Product(2, "Smartphone", "Latest smartphone", 699.99, "Electronics"),
            new Product(3, "Headphones", "Wireless headphones", 149.99, "Electronics"),
            new Product(4, "Book - Java Programming", "Learn Java programming", 49.99, "Books"),
            new Product(5, "Coffee Maker", "Automatic coffee maker", 89.99, "Appliances")
        );
        
        return products.stream()
            .filter(p -> p.getId() == id)
            .findFirst()
            .orElse(null);
    }
}
