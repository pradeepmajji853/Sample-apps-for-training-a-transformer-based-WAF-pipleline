#!/bin/bash

echo "=== WAF TRAINING - HTTP TRAFFIC GENERATION SCRIPT ==="
echo "Generating diverse HTTP traffic patterns for transformer-based WAF training..."
echo ""

# E-commerce Application Traffic
echo "1. TESTING E-COMMERCE APPLICATION:"
echo "   Base URL: http://localhost:8080/ecommerce/"

# Test main page
curl -s -o /dev/null -w "Main Page: %{http_code}\n" http://localhost:8080/ecommerce/

# Test product operations (GET, POST, PUT, DELETE)
echo "   Testing Product Operations:"
curl -s -o /dev/null -w "   GET Products: %{http_code}\n" http://localhost:8080/ecommerce/products
curl -s -o /dev/null -w "   GET Product by ID: %{http_code}\n" http://localhost:8080/ecommerce/products/1
curl -s -X POST -H "Content-Type: application/json" -d '{"name":"Test Product","price":99.99}' -o /dev/null -w "   POST Product: %{http_code}\n" http://localhost:8080/ecommerce/products
curl -s -X PUT -H "Content-Type: application/json" -d '{"id":1,"name":"Updated Product","price":149.99}' -o /dev/null -w "   PUT Product: %{http_code}\n" http://localhost:8080/ecommerce/products/1
curl -s -X DELETE -o /dev/null -w "   DELETE Product: %{http_code}\n" http://localhost:8080/ecommerce/products/1

# Test cart operations
echo "   Testing Cart Operations:"
curl -s -o /dev/null -w "   GET Cart: %{http_code}\n" http://localhost:8080/ecommerce/cart
curl -s -X POST -H "Content-Type: application/json" -d '{"productId":1,"quantity":2}' -o /dev/null -w "   Add to Cart: %{http_code}\n" http://localhost:8080/ecommerce/cart

# Test user operations
echo "   Testing User Operations:"
curl -s -o /dev/null -w "   GET Users: %{http_code}\n" http://localhost:8080/ecommerce/users
curl -s -X POST -H "Content-Type: application/json" -d '{"username":"testuser","email":"test@example.com"}' -o /dev/null -w "   POST User: %{http_code}\n" http://localhost:8080/ecommerce/users

# Test search functionality
echo "   Testing Search:"
curl -s -o /dev/null -w "   Search Products: %{http_code}\n" "http://localhost:8080/ecommerce/search?q=laptop&category=electronics&minPrice=100&maxPrice=1000"

echo ""

# REST API Application Traffic
echo "2. TESTING REST API APPLICATION:"
echo "   Base URL: http://localhost:8080/rest-api/"

# Test main page
curl -s -o /dev/null -w "Main Page: %{http_code}\n" http://localhost:8080/rest-api/

# Test various API endpoints
echo "   Testing API Endpoints:"
curl -s -o /dev/null -w "   GET /api/tasks: %{http_code}\n" http://localhost:8080/rest-api/api/tasks
curl -s -o /dev/null -w "   GET /api/users: %{http_code}\n" http://localhost:8080/rest-api/api/users  
curl -s -o /dev/null -w "   GET /api/projects: %{http_code}\n" http://localhost:8080/rest-api/api/projects
curl -s -o /dev/null -w "   GET /api/analytics: %{http_code}\n" http://localhost:8080/rest-api/api/analytics

# Test with different HTTP methods
curl -s -X POST -H "Content-Type: application/json" -d '{"title":"New Task","description":"Test task"}' -o /dev/null -w "   POST /api/tasks: %{http_code}\n" http://localhost:8080/rest-api/api/tasks
curl -s -X PUT -H "Content-Type: application/json" -d '{"id":1,"title":"Updated Task"}' -o /dev/null -w "   PUT /api/tasks/1: %{http_code}\n" http://localhost:8080/rest-api/api/tasks/1
curl -s -X DELETE -o /dev/null -w "   DELETE /api/tasks/1: %{http_code}\n" http://localhost:8080/rest-api/api/tasks/1

# Test authentication endpoints
curl -s -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"password"}' -o /dev/null -w "   POST /api/auth/login: %{http_code}\n" http://localhost:8080/rest-api/api/auth/login

echo ""

# Blog CMS Application Traffic (if working)
echo "3. TESTING BLOG CMS APPLICATION:"
echo "   Base URL: http://localhost:8080/blog-cms/"

curl -s -o /dev/null -w "Main Page: %{http_code}\n" http://localhost:8080/blog-cms/

if [ $? -eq 0 ]; then
    echo "   Testing Blog Operations:"
    curl -s -o /dev/null -w "   GET /blog: %{http_code}\n" http://localhost:8080/blog-cms/blog
    curl -s -o /dev/null -w "   GET /comments: %{http_code}\n" http://localhost:8080/blog-cms/comments
    curl -s -o /dev/null -w "   GET /search: %{http_code}\n" http://localhost:8080/blog-cms/search
    curl -s -o /dev/null -w "   GET /users: %{http_code}\n" http://localhost:8080/blog-cms/users
    curl -s -o /dev/null -w "   GET /content: %{http_code}\n" http://localhost:8080/blog-cms/content
else
    echo "   Blog CMS application needs troubleshooting"
fi

echo ""
echo "=== TRAFFIC GENERATION COMPLETE ==="
echo "Check Tomcat access logs at: /Users/majjipradeepkumar/Downloads/apache-tomcat-9.0.109/logs/localhost_access_log.*.txt"
echo "These logs contain diverse HTTP patterns suitable for WAF training data"
