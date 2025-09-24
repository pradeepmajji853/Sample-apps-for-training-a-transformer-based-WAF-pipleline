"""
Realistic Traffic Generation for WAF Training
Uses Locust to generate diverse, realistic HTTP traffic patterns
"""

import random
import json
import time
import uuid
from datetime import datetime, timedelta
from locust import HttpUser, task, between
from faker import Faker

fake = Faker()

class BlogCMSUser(HttpUser):
    """User behavior for Blog CMS application"""
    
    wait_time = between(1, 5)
    host = "http://localhost"
    
    def on_start(self):
        """Initialize user session"""
        self.user_id = random.randint(1, 1000)
        self.session_id = str(uuid.uuid4())
        
    @task(10)
    def view_homepage(self):
        """View blog homepage"""
        self.client.get("/blog-cms/")
        
    @task(8)
    def browse_posts(self):
        """Browse blog posts"""
        post_id = random.randint(1, 100)
        self.client.get(f"/blog-cms/blog/post/{post_id}")
        
    @task(5)
    def search_posts(self):
        """Search for blog posts"""
        keywords = ['technology', 'programming', 'web', 'security', 'tutorial']
        query = random.choice(keywords)
        self.client.get(f"/blog-cms/search?q={query}&category=all")
        
    @task(3)
    def view_category(self):
        """View posts by category"""
        categories = ['tech', 'news', 'tutorials', 'reviews']
        category = random.choice(categories)
        self.client.get(f"/blog-cms/category/{category}")
        
    @task(2)
    def submit_comment(self):
        """Submit a comment on a post"""
        post_id = random.randint(1, 50)
        comment_data = {
            'author': fake.name(),
            'email': fake.email(),
            'content': fake.text(max_nb_chars=200),
            'post_id': post_id
        }
        self.client.post(
            f"/blog-cms/comments",
            json=comment_data,
            headers={'Content-Type': 'application/json'}
        )
        
    @task(1)
    def admin_login_attempt(self):
        """Legitimate admin login attempts"""
        credentials = {
            'username': 'admin',
            'password': 'password123'
        }
        self.client.post(
            "/blog-cms/admin/login",
            data=credentials,
            headers={'Content-Type': 'application/x-www-form-urlencoded'}
        )

class EcommerceUser(HttpUser):
    """User behavior for E-commerce application"""
    
    wait_time = between(1, 4)
    host = "http://localhost"
    
    def on_start(self):
        """Initialize shopping session"""
        self.cart_items = []
        self.user_id = random.randint(1000, 9999)
        
    @task(15)
    def browse_products(self):
        """Browse product catalog"""
        self.client.get("/ecommerce/products")
        
    @task(12)
    def view_product_details(self):
        """View specific product"""
        product_id = random.randint(1, 500)
        self.client.get(f"/ecommerce/products/{product_id}")
        
    @task(8)
    def search_products(self):
        """Search for products"""
        queries = ['laptop', 'phone', 'headphones', 'keyboard', 'mouse', 'monitor']
        query = random.choice(queries)
        min_price = random.choice([0, 100, 500])
        max_price = random.choice([1000, 2000, 5000])
        
        params = {
            'q': query,
            'minPrice': min_price,
            'maxPrice': max_price,
            'category': random.choice(['electronics', 'computers', 'accessories'])
        }
        
        self.client.get("/ecommerce/search", params=params)
        
    @task(5)
    def add_to_cart(self):
        """Add product to cart"""
        product_id = random.randint(1, 100)
        quantity = random.randint(1, 3)
        
        cart_data = {
            'productId': product_id,
            'quantity': quantity,
            'userId': self.user_id
        }
        
        response = self.client.post(
            "/ecommerce/cart",
            json=cart_data,
            headers={'Content-Type': 'application/json'}
        )
        
        if response.status_code == 200:
            self.cart_items.append(product_id)
            
    @task(3)
    def view_cart(self):
        """View shopping cart"""
        self.client.get(f"/ecommerce/cart?userId={self.user_id}")
        
    @task(2)
    def update_cart_item(self):
        """Update quantity of cart item"""
        if self.cart_items:
            product_id = random.choice(self.cart_items)
            new_quantity = random.randint(1, 5)
            
            update_data = {
                'productId': product_id,
                'quantity': new_quantity,
                'userId': self.user_id
            }
            
            self.client.put(
                f"/ecommerce/cart/{product_id}",
                json=update_data,
                headers={'Content-Type': 'application/json'}
            )
            
    @task(1)
    def checkout(self):
        """Attempt checkout process"""
        if self.cart_items:
            checkout_data = {
                'userId': self.user_id,
                'paymentMethod': 'credit_card',
                'shippingAddress': {
                    'street': fake.street_address(),
                    'city': fake.city(),
                    'zipCode': fake.zipcode(),
                    'country': 'USA'
                }
            }
            
            self.client.post(
                "/ecommerce/checkout",
                json=checkout_data,
                headers={'Content-Type': 'application/json'}
            )
            
    @task(1)
    def user_registration(self):
        """Register new user account"""
        user_data = {
            'username': fake.user_name(),
            'email': fake.email(),
            'password': fake.password(length=12),
            'firstName': fake.first_name(),
            'lastName': fake.last_name()
        }
        
        self.client.post(
            "/ecommerce/users",
            json=user_data,
            headers={'Content-Type': 'application/json'}
        )

class RestAPIUser(HttpUser):
    """User behavior for REST API application"""
    
    wait_time = between(0.5, 3)
    host = "http://localhost"
    
    def on_start(self):
        """Initialize API session"""
        self.auth_token = None
        self.user_id = random.randint(1, 100)
        
    @task(12)
    def get_tasks(self):
        """Retrieve task list"""
        headers = {}
        if self.auth_token:
            headers['Authorization'] = f'Bearer {self.auth_token}'
            
        self.client.get("/rest-api/api/tasks", headers=headers)
        
    @task(8)
    def get_users(self):
        """Retrieve user list"""
        self.client.get("/rest-api/api/users")
        
    @task(6)
    def get_projects(self):
        """Retrieve project list"""
        self.client.get("/rest-api/api/projects")
        
    @task(5)
    def create_task(self):
        """Create new task"""
        task_data = {
            'title': fake.sentence(nb_words=4),
            'description': fake.text(max_nb_chars=200),
            'priority': random.choice(['low', 'medium', 'high']),
            'assignee': random.randint(1, 50),
            'dueDate': (datetime.now() + timedelta(days=random.randint(1, 30))).isoformat()
        }
        
        headers = {'Content-Type': 'application/json'}
        if self.auth_token:
            headers['Authorization'] = f'Bearer {self.auth_token}'
            
        self.client.post("/rest-api/api/tasks", json=task_data, headers=headers)
        
    @task(4)
    def update_task(self):
        """Update existing task"""
        task_id = random.randint(1, 100)
        
        update_data = {
            'status': random.choice(['todo', 'in_progress', 'completed']),
            'priority': random.choice(['low', 'medium', 'high'])
        }
        
        headers = {'Content-Type': 'application/json'}
        if self.auth_token:
            headers['Authorization'] = f'Bearer {self.auth_token}'
            
        self.client.put(f"/rest-api/api/tasks/{task_id}", json=update_data, headers=headers)
        
    @task(3)
    def get_analytics(self):
        """Get analytics data"""
        params = {
            'startDate': (datetime.now() - timedelta(days=30)).isoformat(),
            'endDate': datetime.now().isoformat(),
            'metric': random.choice(['tasks', 'users', 'projects'])
        }
        
        self.client.get("/rest-api/api/analytics", params=params)
        
    @task(2)
    def authenticate(self):
        """Authenticate user"""
        auth_data = {
            'username': f'user{self.user_id}',
            'password': 'password123'
        }
        
        response = self.client.post(
            "/rest-api/api/auth/login",
            json=auth_data,
            headers={'Content-Type': 'application/json'}
        )
        
        if response.status_code == 200:
            try:
                token_data = response.json()
                self.auth_token = token_data.get('token')
            except:
                pass
                
    @task(1)
    def delete_task(self):
        """Delete a task"""
        task_id = random.randint(1, 50)
        
        headers = {}
        if self.auth_token:
            headers['Authorization'] = f'Bearer {self.auth_token}'
            
        self.client.delete(f"/rest-api/api/tasks/{task_id}", headers=headers)

# User classes for mixed traffic
class MixedTrafficUser(HttpUser):
    """User that randomly accesses different applications"""
    
    wait_time = between(1, 6)
    host = "http://localhost"
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        # Create instances of other user types
        self.blog_user = BlogCMSUser(self.environment)
        self.ecom_user = EcommerceUser(self.environment)
        self.api_user = RestAPIUser(self.environment)
        
        # Override their clients
        self.blog_user.client = self.client
        self.ecom_user.client = self.client
        self.api_user.client = self.client
        
    @task(1)
    def use_blog_cms(self):
        """Randomly execute blog CMS tasks"""
        tasks = [
            self.blog_user.view_homepage,
            self.blog_user.browse_posts,
            self.blog_user.search_posts,
            self.blog_user.view_category
        ]
        random.choice(tasks)()
        
    @task(1)
    def use_ecommerce(self):
        """Randomly execute e-commerce tasks"""
        tasks = [
            self.ecom_user.browse_products,
            self.ecom_user.view_product_details,
            self.ecom_user.search_products,
            self.ecom_user.view_cart
        ]
        random.choice(tasks)()
        
    @task(1)
    def use_rest_api(self):
        """Randomly execute REST API tasks"""
        tasks = [
            self.api_user.get_tasks,
            self.api_user.get_users,
            self.api_user.get_projects,
            self.api_user.get_analytics
        ]
        random.choice(tasks)()

class SlowUser(HttpUser):
    """Simulate slow/problematic clients"""
    
    wait_time = between(10, 30)  # Much slower
    host = "http://localhost"
    
    @task(1)
    def slow_request(self):
        """Make requests with delays"""
        time.sleep(random.uniform(1, 5))  # Random delay
        
        apps = ['/blog-cms/', '/ecommerce/', '/rest-api/']
        app = random.choice(apps)
        
        # Sometimes make incomplete requests or with unusual headers
        headers = {}
        if random.random() < 0.3:
            headers['User-Agent'] = 'SlowBot/1.0'
            
        self.client.get(app, headers=headers, timeout=30)

class RetryUser(HttpUser):
    """Simulate users with retry behavior"""
    
    wait_time = between(2, 8)
    host = "http://localhost"
    
    @task(1)
    def retry_requests(self):
        """Make requests with retry logic"""
        endpoints = [
            '/blog-cms/posts/999999',  # Likely 404
            '/ecommerce/products/999999',  # Likely 404
            '/rest-api/api/nonexistent'  # Likely 404
        ]
        
        endpoint = random.choice(endpoints)
        
        # Try request multiple times (simulating client retries)
        for i in range(random.randint(1, 3)):
            response = self.client.get(endpoint)
            if response.status_code == 200:
                break
            time.sleep(random.uniform(0.5, 2))

# Configuration for different traffic patterns
TRAFFIC_PATTERNS = {
    'normal': [
        {'user_class': BlogCMSUser, 'weight': 3},
        {'user_class': EcommerceUser, 'weight': 4},
        {'user_class': RestAPIUser, 'weight': 2},
        {'user_class': MixedTrafficUser, 'weight': 1}
    ],
    'heavy': [
        {'user_class': BlogCMSUser, 'weight': 5},
        {'user_class': EcommerceUser, 'weight': 6},
        {'user_class': RestAPIUser, 'weight': 4},
        {'user_class': MixedTrafficUser, 'weight': 2},
        {'user_class': SlowUser, 'weight': 1}
    ],
    'problematic': [
        {'user_class': BlogCMSUser, 'weight': 2},
        {'user_class': EcommerceUser, 'weight': 2},
        {'user_class': RestAPIUser, 'weight': 2},
        {'user_class': SlowUser, 'weight': 2},
        {'user_class': RetryUser, 'weight': 2}
    ]
}
