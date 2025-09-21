package com.ecommerce.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class SearchServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String query = request.getParameter("q");
        String category = request.getParameter("category");
        String minPrice = request.getParameter("min_price");
        String maxPrice = request.getParameter("max_price");
        String sortBy = request.getParameter("sort");
        
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>Search Results</title></head><body>");
        out.println("<h1>Search Results</h1>");
        
        if (query != null && !query.isEmpty()) {
            out.println("<p>Search results for: <strong>" + query + "</strong></p>");
            
            // Advanced search form
            out.println("<h2>Refine Your Search</h2>");
            out.println("<form method='get'>");
            out.println("<input type='hidden' name='q' value='" + query + "'>");
            out.println("<p>Category: ");
            out.println("<select name='category'>");
            out.println("<option value=''>All Categories</option>");
            out.println("<option value='electronics'>Electronics</option>");
            out.println("<option value='books'>Books</option>");
            out.println("<option value='clothing'>Clothing</option>");
            out.println("</select></p>");
            out.println("<p>Price Range: $<input type='number' name='min_price' placeholder='Min'> - $<input type='number' name='max_price' placeholder='Max'></p>");
            out.println("<p>Sort by: ");
            out.println("<select name='sort'>");
            out.println("<option value='relevance'>Relevance</option>");
            out.println("<option value='price_low'>Price: Low to High</option>");
            out.println("<option value='price_high'>Price: High to Low</option>");
            out.println("<option value='rating'>Customer Rating</option>");
            out.println("</select></p>");
            out.println("<input type='submit' value='Refine Search'>");
            out.println("</form>");
            
            // Mock search results
            out.println("<div>");
            out.println("<h3>Found 5 results</h3>");
            for (int i = 1; i <= 5; i++) {
                out.println("<div style='border: 1px solid #ccc; margin: 10px; padding: 10px;'>");
                out.println("<h4>Product " + i + " matching '" + query + "'</h4>");
                out.println("<p>Description of product that matches your search criteria.</p>");
                out.println("<p>Price: $" + (99.99 + i * 50) + "</p>");
                out.println("<a href='/ecommerce/products/" + i + "'>View Details</a> | ");
                out.println("<a href='/ecommerce/cart?action=add&product_id=" + i + "'>Add to Cart</a>");
                out.println("</div>");
            }
            out.println("</div>");
        } else {
            out.println("<p>Please enter a search term.</p>");
        }
        
        out.println("<p><a href='/ecommerce/products'>Browse All Products</a></p>");
        out.println("</body></html>");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}
