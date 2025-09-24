#!/bin/bash

# WAF System Setup Script
# This script sets up the complete WAF environment including Tomcat, Nginx, and ML components

set -e

echo "=== WAF System Setup ==="
echo "Setting up complete WAF pipeline with Tomcat, Nginx, and ML components..."

PROJECT_ROOT="/Users/majjipradeepkumar/Downloads/WAF/Sample-apps-for-training-a-transformer-based-WAF-pipleline"
WAF_ROOT="$PROJECT_ROOT/waf-system"
TOMCAT_VERSION="9.0.82"
NGINX_VERSION="1.24.0"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Create necessary directories
print_status "Creating directory structure..."
mkdir -p "$WAF_ROOT"/{tomcat,nginx,python-env,data/{logs,models,training},config}

cd "$PROJECT_ROOT"

# Check and install Homebrew if on macOS
if [[ "$OSTYPE" == "darwin"* ]]; then
    if ! command_exists brew; then
        print_status "Installing Homebrew..."
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    fi
fi

# Install system dependencies
print_status "Installing system dependencies..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    if ! command_exists python3; then
        brew install python@3.11
    fi
    if ! command_exists java; then
        brew install openjdk@11
    fi
    if ! command_exists nginx; then
        brew install nginx
    fi
    if ! command_exists maven; then
        brew install maven
    fi
    if ! command_exists wget; then
        brew install wget
    fi
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    sudo apt-get update
    sudo apt-get install -y python3 python3-pip python3-venv openjdk-11-jdk maven nginx wget curl
fi

# Download and setup Tomcat
print_status "Setting up Apache Tomcat $TOMCAT_VERSION..."
TOMCAT_DIR="$WAF_ROOT/tomcat"
if [ ! -d "$TOMCAT_DIR/apache-tomcat-$TOMCAT_VERSION" ]; then
    cd "$TOMCAT_DIR"
    wget -q "https://archive.apache.org/dist/tomcat/tomcat-9/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz"
    tar -xzf "apache-tomcat-$TOMCAT_VERSION.tar.gz"
    rm "apache-tomcat-$TOMCAT_VERSION.tar.gz"
    
    # Make scripts executable
    chmod +x "apache-tomcat-$TOMCAT_VERSION/bin/"*.sh
    
    # Create symbolic link for easier access
    ln -sf "apache-tomcat-$TOMCAT_VERSION" current
fi

# Deploy WAR files to Tomcat
print_status "Deploying WAR files to Tomcat..."
TOMCAT_WEBAPPS="$TOMCAT_DIR/current/webapps"
cp "$PROJECT_ROOT"/*/target/*.war "$TOMCAT_WEBAPPS/"

# Setup Python environment for ML pipeline
print_status "Setting up Python environment for ML pipeline..."
cd "$WAF_ROOT"
if [ ! -d "python-env" ]; then
    python3 -m venv python-env
fi

# Activate virtual environment and install dependencies
source python-env/bin/activate
pip install --upgrade pip
pip install -r requirements.txt

print_status "Installing additional ML dependencies..."
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu

# Create Nginx configuration
print_status "Creating Nginx configuration..."
cat > "$WAF_ROOT/nginx/nginx.conf" << 'EOF'
events {
    worker_connections 1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;
    
    # Custom log format for WAF training
    log_format waf_format '$remote_addr - $remote_user [$time_local] '
                          '"$request" $status $body_bytes_sent '
                          '"$http_referer" "$http_user_agent" '
                          '"$http_x_forwarded_for" $request_time '
                          '"$request_body"';
    
    access_log /Users/majjipradeepkumar/Downloads/WAF/Sample-apps-for-training-a-transformer-based-WAF-pipleline/waf-system/data/logs/access.log waf_format;
    error_log /Users/majjipradeepkumar/Downloads/WAF/Sample-apps-for-training-a-transformer-based-WAF-pipleline/waf-system/data/logs/error.log;
    
    upstream tomcat_backend {
        server localhost:8080;
    }
    
    upstream waf_ml_service {
        server localhost:8081;
    }
    
    server {
        listen 80;
        server_name localhost;
        
        # WAF ML Service endpoint
        location /waf-api/ {
            proxy_pass http://waf_ml_service/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }
        
        # Main application proxy with ML scoring
        location / {
            # Send request to ML service for scoring (non-blocking)
            access_by_lua_block {
                local http = require "resty.http"
                local httpc = http.new()
                
                -- Prepare request data for ML service
                local request_data = {
                    method = ngx.var.request_method,
                    uri = ngx.var.request_uri,
                    headers = ngx.req.get_headers(),
                    remote_addr = ngx.var.remote_addr,
                    user_agent = ngx.var.http_user_agent
                }
                
                -- Send to ML service asynchronously
                local res, err = httpc:request_uri("http://localhost:8081/score", {
                    method = "POST",
                    body = require("cjson").encode(request_data),
                    headers = {
                        ["Content-Type"] = "application/json",
                    }
                })
                
                -- Log the ML score but don't block request
                if res then
                    ngx.log(ngx.INFO, "ML Score: " .. (res.body or "unknown"))
                end
            }
            
            proxy_pass http://tomcat_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }
    }
}
EOF

# Create startup script
print_status "Creating startup script..."
cat > "$WAF_ROOT/start_waf_system.sh" << 'EOF'
#!/bin/bash

PROJECT_ROOT="/Users/majjipradeepkumar/Downloads/WAF/Sample-apps-for-training-a-transformer-based-WAF-pipleline"
WAF_ROOT="$PROJECT_ROOT/waf-system"

echo "=== Starting WAF System ==="

# Start Tomcat
echo "Starting Tomcat..."
cd "$WAF_ROOT/tomcat/current"
./bin/startup.sh

# Wait for Tomcat to start
sleep 10

# Start ML Pipeline
echo "Starting ML Pipeline..."
cd "$WAF_ROOT"
source python-env/bin/activate
python ml-pipeline/inference/waf_service.py &

# Start Nginx
echo "Starting Nginx..."
sudo nginx -c "$WAF_ROOT/nginx/nginx.conf"

echo "=== WAF System Started ==="
echo "Applications available at:"
echo "  - Blog CMS: http://localhost/blog-cms/"
echo "  - E-commerce: http://localhost/ecommerce/"
echo "  - REST API: http://localhost/rest-api/"
echo "  - WAF API: http://localhost/waf-api/"
EOF

chmod +x "$WAF_ROOT/start_waf_system.sh"

# Create stop script
cat > "$WAF_ROOT/stop_waf_system.sh" << 'EOF'
#!/bin/bash

PROJECT_ROOT="/Users/majjipradeepkumar/Downloads/WAF/Sample-apps-for-training-a-transformer-based-WAF-pipleline"
WAF_ROOT="$PROJECT_ROOT/waf-system"

echo "=== Stopping WAF System ==="

# Stop Nginx
echo "Stopping Nginx..."
sudo nginx -s stop

# Stop ML Pipeline
echo "Stopping ML Pipeline..."
pkill -f "waf_service.py"

# Stop Tomcat
echo "Stopping Tomcat..."
cd "$WAF_ROOT/tomcat/current"
./bin/shutdown.sh

echo "=== WAF System Stopped ==="
EOF

chmod +x "$WAF_ROOT/stop_waf_system.sh"

print_status "WAF System setup complete!"
print_status "Run '$WAF_ROOT/start_waf_system.sh' to start the system"
print_status "Run '$WAF_ROOT/stop_waf_system.sh' to stop the system"
