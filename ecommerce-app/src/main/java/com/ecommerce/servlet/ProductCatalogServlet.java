package com.ecommerce.servlet;

import com.ecommerce.model.Product;
import com.ecommerce.util.JsonUtil;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProductCatalogServlet extends HttpServlet {
    
    private static final List<Product> PRODUCTS = new ArrayList<>();
    private static final Gson gson = new Gson();
    
    static {
        // Initialize sample products
        PRODUCTS.add(new Product(1, "Laptop", "High-performance laptop", 999.99, "Electronics"));
        PRODUCTS.add(new Product(2, "Smartphone", "Latest smartphone", 699.99, "Electronics"));
        PRODUCTS.add(new Product(3, "Headphones", "Wireless headphones", 149.99, "Electronics"));
        PRODUCTS.add(new Product(4, "Book - Java Programming", "Learn Java programming", 49.99, "Books"));
        PRODUCTS.add(new Product(5, "Coffee Maker", "Automatic coffee maker", 89.99, "Appliances"));
        PRODUCTS.add(new Product(6, "Gaming Mouse", "RGB Gaming Mouse", 59.99, "Electronics"));
        PRODUCTS.add(new Product(7, "Mechanical Keyboard", "RGB Mechanical Keyboard", 129.99, "Electronics"));
        PRODUCTS.add(new Product(8, "Monitor", "27-inch 4K Monitor", 299.99, "Electronics"));
        PRODUCTS.add(new Product(9, "Backpack", "Laptop backpack", 79.99, "Accessories"));
        PRODUCTS.add(new Product(10, "Desk Lamp", "LED desk lamp", 39.99, "Home"));
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        String category = request.getParameter("category");
        String search = request.getParameter("search");
        String sort = request.getParameter("sort");
        String page = request.getParameter("page");
        String limit = request.getParameter("limit");
        String format = request.getParameter("format");
        
        List<Product> filteredProducts = new ArrayList<>(PRODUCTS);
        
        // Filter by category
        if (category != null && !category.isEmpty()) {
            filteredProducts = filteredProducts.stream()
                .filter(p -> p.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
        }
        
        // Search functionality
        if (search != null && !search.isEmpty()) {
            filteredProducts = filteredProducts.stream()
                .filter(p -> p.getName().toLowerCase().contains(search.toLowerCase()) ||
                           p.getDescription().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
        }
        
        // Sort functionality
        if (sort != null) {
            switch (sort) {
                case "price_asc":
                    filteredProducts.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                    break;
                case "price_desc":
                    filteredProducts.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                    break;
                case "name":
                    filteredProducts.sort((a, b) -> a.getName().compareTo(b.getName()));
                    break;
            }
        }
        
        // Handle specific product ID
        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                int productId = Integer.parseInt(pathInfo.substring(1));
                Product product = PRODUCTS.stream()
                    .filter(p -> p.getId() == productId)
                    .findFirst()
                    .orElse(null);
                    
                if (product != null) {
                    if ("json".equals(format)) {
                        response.setContentType("application/json");
                        response.getWriter().write(gson.toJson(product));
                    } else {
                        response.setContentType("text/html");
                        PrintWriter out = response.getWriter();
                        out.println("<html><body>");
                        out.println("<h1>Product Details</h1>");
                        out.println("<h2>" + product.getName() + "</h2>");
                        out.println("<p>" + product.getDescription() + "</p>");
                        out.println("<p>Price: $" + product.getPrice() + "</p>");
                        out.println("<p>Category: " + product.getCategory() + "</p>");
                        out.println("<a href='/ecommerce/products'>Back to Products</a>");
                        out.println("</body></html>");
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }
        
        // Return product list
        if ("json".equals(format)) {
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(filteredProducts));
        } else {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<html><head><title>Product Catalog</title></head><body>");
            out.println("<h1>Product Catalog</h1>");
            
            // Search form
            out.println("<form method='get'>");
            out.println("<input type='text' name='search' placeholder='Search products...' value='" + 
                       (search != null ? search : "") + "'>");
            out.println("<select name='category'>");
            out.println("<option value=''>All Categories</option>");
            out.println("<option value='Electronics'" + ("Electronics".equals(category) ? " selected" : "") + ">Electronics</option>");
            out.println("<option value='Books'" + ("Books".equals(category) ? " selected" : "") + ">Books</option>");
            out.println("<option value='Appliances'" + ("Appliances".equals(category) ? " selected" : "") + ">Appliances</option>");
            out.println("</select>");
            out.println("<select name='sort'>");
            out.println("<option value=''>Sort by</option>");
            out.println("<option value='name'" + ("name".equals(sort) ? " selected" : "") + ">Name</option>");
            out.println("<option value='price_asc'" + ("price_asc".equals(sort) ? " selected" : "") + ">Price (Low to High)</option>");
            out.println("<option value='price_desc'" + ("price_desc".equals(sort) ? " selected" : "") + ">Price (High to Low)</option>");
            out.println("</select>");
            out.println("<input type='submit' value='Search'>");
            out.println("</form>");
            
            out.println("<div>");
            for (Product product : filteredProducts) {
                out.println("<div style='border: 1px solid #ccc; margin: 10px; padding: 10px;'>");
                out.println("<h3><a href='/ecommerce/products/" + product.getId() + "'>" + product.getName() + "</a></h3>");
                out.println("<p>" + product.getDescription() + "</p>");
                out.println("<p>Price: $" + product.getPrice() + "</p>");
                out.println("<p>Category: " + product.getCategory() + "</p>");
                out.println("<a href='/ecommerce/cart?action=add&product_id=" + product.getId() + "'>Add to Cart</a>");
                out.println("</div>");
            }
            out.println("</div>");
            out.println("</body></html>");
        }
    }
}
