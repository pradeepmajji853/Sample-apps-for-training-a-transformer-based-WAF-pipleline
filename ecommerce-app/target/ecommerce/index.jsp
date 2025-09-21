<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>E-commerce Demo - WAF Training App</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
        .header { background-color: #333; color: white; padding: 20px; text-align: center; }
        .nav { background-color: #555; padding: 10px; }
        .nav a { color: white; text-decoration: none; margin: 0 15px; }
        .nav a:hover { text-decoration: underline; }
        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
        .card { background: white; border-radius: 8px; padding: 20px; margin: 10px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .search-box { width: 100%; max-width: 400px; padding: 10px; border: 1px solid #ddd; border-radius: 4px; }
        .btn { background-color: #007bff; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; }
        .btn:hover { background-color: #0056b3; }
        .feature-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>E-commerce Demo Application</h1>
        <p>Sample Web Application for WAF Training & Testing</p>
    </div>
    
    <div class="nav">
        <a href="/ecommerce/">Home</a>
        <a href="/ecommerce/products">Products</a>
        <a href="/ecommerce/search">Search</a>
        <a href="/ecommerce/cart">Cart</a>
        <a href="/ecommerce/user">Account</a>
        <a href="/ecommerce/admin">Admin</a>
    </div>
    
    <div class="container">
        <div class="card">
            <h2>Welcome to the E-commerce Demo</h2>
            <p>This is a sample e-commerce web application designed for transformer-based WAF training and testing.</p>
            
            <div style="text-align: center; margin: 20px 0;">
                <form action="/ecommerce/search" method="get">
                    <input type="text" name="q" class="search-box" placeholder="Search for products...">
                    <button type="submit" class="btn">Search</button>
                </form>
            </div>
        </div>
        
        <div class="feature-grid">
            <div class="card">
                <h3>🛍️ Product Catalog</h3>
                <p>Browse through various product categories including Electronics, Books, and Appliances.</p>
                <a href="/ecommerce/products" class="btn">Browse Products</a>
            </div>
            
            <div class="card">
                <h3>🛒 Shopping Cart</h3>
                <p>Add products to your cart and proceed to checkout with our secure payment system.</p>
                <a href="/ecommerce/cart" class="btn">View Cart</a>
            </div>
            
            <div class="card">
                <h3>👤 User Account</h3>
                <p>Create an account, login, and manage your profile and order history.</p>
                <a href="/ecommerce/user" class="btn">My Account</a>
            </div>
            
            <div class="card">
                <h3>🔍 Advanced Search</h3>
                <p>Search for products with filters, sorting, and advanced query options.</p>
                <a href="/ecommerce/search" class="btn">Search Products</a>
            </div>
            
            <div class="card">
                <h3>📋 Order Management</h3>
                <p>Track your orders, view order history, and manage shipping information.</p>
                <a href="/ecommerce/orders" class="btn">My Orders</a>
            </div>
            
            <div class="card">
                <h3>⚙️ Admin Panel</h3>
                <p>Admin interface for managing products, orders, and users (admin login required).</p>
                <a href="/ecommerce/admin" class="btn">Admin Panel</a>
            </div>
        </div>
        
        <div class="card">
            <h3>🤖 WAF Training Information</h3>
            <p>This application is designed to generate diverse HTTP traffic patterns for training transformer-based Web Application Firewalls:</p>
            <ul>
                <li><strong>GET Requests:</strong> Product browsing, search queries, pagination</li>
                <li><strong>POST Requests:</strong> User registration, login, cart operations, order processing</li>
                <li><strong>Form Submissions:</strong> Contact forms, checkout process, user profile updates</li>
                <li><strong>AJAX Requests:</strong> Dynamic content loading, search suggestions</li>
                <li><strong>File Uploads:</strong> Profile pictures, product images</li>
                <li><strong>Session Management:</strong> Login/logout, shopping cart persistence</li>
                <li><strong>Parameter Variations:</strong> Query strings, form data, JSON payloads</li>
            </ul>
            <p>The application includes various endpoints that accept different types of input to create comprehensive training data for anomaly detection models.</p>
        </div>
        
        <div class="card">
            <h3>🔗 Quick Links for Testing</h3>
            <p>Common endpoints for generating traffic:</p>
            <ul>
                <li><a href="/ecommerce/products?category=Electronics&sort=price_asc">Filtered Product Listing</a></li>
                <li><a href="/ecommerce/search?q=laptop&category=electronics&min_price=500">Advanced Search</a></li>
                <li><a href="/ecommerce/products/1">Product Details</a></li>
                <li><a href="/ecommerce/user/login">Login Page</a></li>
                <li><a href="/ecommerce/user/register">Registration Page</a></li>
                <li><a href="/ecommerce/cart?action=add&product_id=1&quantity=2">Add to Cart</a></li>
            </ul>
        </div>
    </div>
    
    <div style="text-align: center; margin-top: 40px; color: #666;">
        <p>&copy; 2025 E-commerce Demo - WAF Training Application</p>
    </div>
</body>
</html>
