<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>REST API Testing Interface</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #333;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
        }
        
        .container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
        }
        
        .header {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            border-radius: 15px;
            padding: 30px;
            margin-bottom: 30px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.1);
            text-align: center;
        }
        
        .header h1 {
            color: #2c3e50;
            margin-bottom: 10px;
            font-size: 2.5em;
        }
        
        .header p {
            color: #7f8c8d;
            font-size: 1.2em;
        }
        
        .api-section {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            border-radius: 15px;
            padding: 25px;
            margin-bottom: 25px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.1);
        }
        
        .api-section h2 {
            color: #2c3e50;
            margin-bottom: 20px;
            padding-bottom: 10px;
            border-bottom: 3px solid #3498db;
            display: flex;
            align-items: center;
            gap: 10px;
        }
        
        .api-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 20px;
        }
        
        .endpoint-card {
            background: #f8f9fa;
            border-radius: 10px;
            padding: 20px;
            border-left: 4px solid #3498db;
            transition: all 0.3s ease;
        }
        
        .endpoint-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
        }
        
        .endpoint-card h3 {
            color: #2c3e50;
            margin-bottom: 10px;
            font-size: 1.3em;
        }
        
        .endpoint-card p {
            color: #7f8c8d;
            margin-bottom: 15px;
        }
        
        .method-tag {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 0.8em;
            font-weight: bold;
            margin-right: 8px;
            margin-bottom: 5px;
        }
        
        .method-get { background: #27ae60; color: white; }
        .method-post { background: #f39c12; color: white; }
        .method-put { background: #8e44ad; color: white; }
        .method-delete { background: #e74c3c; color: white; }
        .method-options { background: #95a5a6; color: white; }
        
        .test-form {
            background: #ecf0f1;
            border-radius: 8px;
            padding: 15px;
            margin-top: 15px;
        }
        
        .form-group {
            margin-bottom: 15px;
        }
        
        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
            color: #2c3e50;
        }
        
        .form-group input,
        .form-group select,
        .form-group textarea {
            width: 100%;
            padding: 10px;
            border: 1px solid #bdc3c7;
            border-radius: 5px;
            font-size: 14px;
        }
        
        .form-group textarea {
            resize: vertical;
            min-height: 100px;
        }
        
        .btn {
            background: #3498db;
            color: white;
            padding: 10px 20px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 14px;
            transition: background 0.3s ease;
        }
        
        .btn:hover {
            background: #2980b9;
        }
        
        .btn-success { background: #27ae60; }
        .btn-success:hover { background: #229954; }
        
        .btn-warning { background: #f39c12; }
        .btn-warning:hover { background: #d68910; }
        
        .btn-danger { background: #e74c3c; }
        .btn-danger:hover { background: #c0392b; }
        
        .response-area {
            background: #2c3e50;
            color: #ecf0f1;
            padding: 15px;
            border-radius: 5px;
            font-family: 'Courier New', monospace;
            margin-top: 15px;
            min-height: 100px;
            white-space: pre-wrap;
            overflow-x: auto;
        }
        
        .auth-section {
            background: #fff3cd;
            border: 1px solid #ffeaa7;
            border-radius: 8px;
            padding: 15px;
            margin-bottom: 20px;
        }
        
        .auth-section h4 {
            color: #856404;
            margin-bottom: 10px;
        }
        
        .auth-info {
            font-size: 14px;
            color: #856404;
        }
        
        .feature-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-top: 20px;
        }
        
        .feature-card {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 10px;
            text-align: center;
        }
        
        .feature-card h4 {
            margin-bottom: 10px;
            font-size: 1.2em;
        }
        
        .feature-card p {
            font-size: 0.9em;
            opacity: 0.9;
        }
        
        .emoji {
            font-size: 1.5em;
            margin-right: 8px;
        }
        
        .demo-btn {
            margin: 5px;
            padding: 8px 16px;
            border: 1px solid #3498db;
            background: white;
            color: #3498db;
            border-radius: 4px;
            cursor: pointer;
            font-size: 12px;
            transition: all 0.3s ease;
        }
        
        .demo-btn:hover {
            background: #3498db;
            color: white;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1><span class="emoji">🚀</span>REST API Testing Interface</h1>
            <p>Comprehensive REST API for task management, user authentication, and analytics</p>
        </div>
        
        <div class="auth-section">
            <h4><span class="emoji">🔐</span>Authentication Information</h4>
            <div class="auth-info">
                <p><strong>Demo Credentials:</strong></p>
                <ul style="margin-left: 20px;">
                    <li>Username: <code>admin</code>, Password: <code>admin123</code> (Admin Role)</li>
                    <li>Username: <code>john_doe</code>, Password: <code>user123</code> (User Role)</li>
                </ul>
                <p><strong>Demo API Keys:</strong></p>
                <ul style="margin-left: 20px;">
                    <li>Admin Key: <code>demo-api-key-admin</code></li>
                    <li>User Key: <code>demo-api-key-user</code></li>
                </ul>
            </div>
        </div>
        
        <!-- Authentication API -->
        <div class="api-section">
            <h2><span class="emoji">🔑</span>Authentication API</h2>
            <div class="api-grid">
                <div class="endpoint-card">
                    <h3>Login</h3>
                    <p>Authenticate user and get session token</p>
                    <span class="method-tag method-post">POST</span>
                    <code>/api/auth/login</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>Request Body:</label>
                            <textarea id="loginBody">{"username": "admin", "password": "admin123"}</textarea>
                        </div>
                        <button class="btn btn-warning" onclick="testLogin()">Test Login</button>
                        <div id="loginResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
                
                <div class="endpoint-card">
                    <h3>Register</h3>
                    <p>Register new user account</p>
                    <span class="method-tag method-post">POST</span>
                    <code>/api/auth/register</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>Request Body:</label>
                            <textarea id="registerBody">{"username": "newuser", "password": "password123", "email": "new@example.com"}</textarea>
                        </div>
                        <button class="btn btn-warning" onclick="testRegister()">Test Register</button>
                        <div id="registerResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
                
                <div class="endpoint-card">
                    <h3>Validate Token</h3>
                    <p>Validate authentication token</p>
                    <span class="method-tag method-post">POST</span>
                    <code>/api/auth/validate</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>Authorization Header:</label>
                            <input type="text" id="validateToken" placeholder="Bearer token_admin_..." />
                        </div>
                        <button class="btn btn-warning" onclick="testValidate()">Test Validate</button>
                        <div id="validateResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Tasks API -->
        <div class="api-section">
            <h2><span class="emoji">📋</span>Tasks API</h2>
            <div class="api-grid">
                <div class="endpoint-card">
                    <h3>List Tasks</h3>
                    <p>Get all tasks with filtering and pagination</p>
                    <span class="method-tag method-get">GET</span>
                    <code>/api/tasks</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>Query Parameters:</label>
                            <input type="text" id="tasksQuery" placeholder="status=IN_PROGRESS&priority=HIGH&page=1" />
                        </div>
                        <button class="btn btn-success" onclick="testListTasks()">Test List Tasks</button>
                        <div id="tasksResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
                
                <div class="endpoint-card">
                    <h3>Create Task</h3>
                    <p>Create a new task</p>
                    <span class="method-tag method-post">POST</span>
                    <code>/api/tasks</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>Request Body:</label>
                            <textarea id="createTaskBody">{"title": "New API Task", "description": "Test task from API", "priority": "HIGH", "assignee": "john_doe"}</textarea>
                        </div>
                        <button class="btn btn-warning" onclick="testCreateTask()">Test Create</button>
                        <div id="createTaskResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
                
                <div class="endpoint-card">
                    <h3>Update Task</h3>
                    <p>Update existing task</p>
                    <span class="method-tag method-put">PUT</span>
                    <code>/api/tasks/{id}</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>Task ID:</label>
                            <input type="text" id="updateTaskId" value="1" />
                        </div>
                        <div class="form-group">
                            <label>Request Body:</label>
                            <textarea id="updateTaskBody">{"status": "COMPLETED", "priority": "LOW"}</textarea>
                        </div>
                        <button class="btn btn-success" onclick="testUpdateTask()">Test Update</button>
                        <div id="updateTaskResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Users API -->
        <div class="api-section">
            <h2><span class="emoji">👥</span>Users API</h2>
            <div class="api-grid">
                <div class="endpoint-card">
                    <h3>List Users</h3>
                    <p>Get all users with filtering</p>
                    <span class="method-tag method-get">GET</span>
                    <code>/api/users</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>Query Parameters:</label>
                            <input type="text" id="usersQuery" placeholder="role=USER&status=active&search=john" />
                        </div>
                        <button class="btn btn-success" onclick="testListUsers()">Test List Users</button>
                        <div id="usersResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
                
                <div class="endpoint-card">
                    <h3>Create User</h3>
                    <p>Create new user</p>
                    <span class="method-tag method-post">POST</span>
                    <code>/api/users</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>Request Body:</label>
                            <textarea id="createUserBody">{"username": "apiuser", "email": "api@example.com", "fullName": "API Test User", "password": "test123"}</textarea>
                        </div>
                        <button class="btn btn-warning" onclick="testCreateUser()">Test Create User</button>
                        <div id="createUserResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
                
                <div class="endpoint-card">
                    <h3>Get User Details</h3>
                    <p>Get specific user by ID</p>
                    <span class="method-tag method-get">GET</span>
                    <code>/api/users/{id}</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>User ID:</label>
                            <input type="text" id="getUserId" value="1" />
                        </div>
                        <button class="btn btn-success" onclick="testGetUser()">Test Get User</button>
                        <div id="getUserResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Projects API -->
        <div class="api-section">
            <h2><span class="emoji">🏗️</span>Projects API</h2>
            <div class="api-grid">
                <div class="endpoint-card">
                    <h3>List Projects</h3>
                    <p>Get all projects with sorting and filtering</p>
                    <span class="method-tag method-get">GET</span>
                    <code>/api/projects</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>Query Parameters:</label>
                            <input type="text" id="projectsQuery" placeholder="status=IN_PROGRESS&sortBy=priority&sortOrder=desc" />
                        </div>
                        <button class="btn btn-success" onclick="testListProjects()">Test List Projects</button>
                        <div id="projectsResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
                
                <div class="endpoint-card">
                    <h3>Create Project</h3>
                    <p>Create new project</p>
                    <span class="method-tag method-post">POST</span>
                    <code>/api/projects</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>Request Body:</label>
                            <textarea id="createProjectBody">{"name": "API Test Project", "description": "Project created via API", "priority": "HIGH", "owner": "admin"}</textarea>
                        </div>
                        <button class="btn btn-warning" onclick="testCreateProject()">Test Create Project</button>
                        <div id="createProjectResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Files API -->
        <div class="api-section">
            <h2><span class="emoji">📁</span>Files API</h2>
            <div class="api-grid">
                <div class="endpoint-card">
                    <h3>List Files</h3>
                    <p>Get uploaded files with filtering</p>
                    <span class="method-tag method-get">GET</span>
                    <code>/api/files</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>Query Parameters:</label>
                            <input type="text" id="filesQuery" placeholder="category=image&uploadedBy=admin" />
                        </div>
                        <button class="btn btn-success" onclick="testListFiles()">Test List Files</button>
                        <div id="filesResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
                
                <div class="endpoint-card">
                    <h3>Upload File</h3>
                    <p>Upload new file (multipart/form-data)</p>
                    <span class="method-tag method-post">POST</span>
                    <code>/api/files/upload</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>File:</label>
                            <input type="file" id="fileUpload" />
                        </div>
                        <div class="form-group">
                            <label>Category:</label>
                            <select id="fileCategory">
                                <option value="document">Document</option>
                                <option value="image">Image</option>
                                <option value="general">General</option>
                            </select>
                        </div>
                        <button class="btn btn-warning" onclick="testFileUpload()">Test Upload</button>
                        <div id="fileUploadResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
                
                <div class="endpoint-card">
                    <h3>File Info</h3>
                    <p>Get file information</p>
                    <span class="method-tag method-get">GET</span>
                    <code>/api/files/info/{id}</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>File ID:</label>
                            <input type="text" id="fileInfoId" value="1" />
                        </div>
                        <button class="btn btn-success" onclick="testFileInfo()">Test File Info</button>
                        <div id="fileInfoResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Analytics API -->
        <div class="api-section">
            <h2><span class="emoji">📊</span>Analytics API</h2>
            <div class="api-grid">
                <div class="endpoint-card">
                    <h3>Overview Dashboard</h3>
                    <p>Get comprehensive analytics overview</p>
                    <span class="method-tag method-get">GET</span>
                    <code>/api/analytics</code>
                    
                    <div class="test-form">
                        <button class="btn btn-success" onclick="testAnalyticsOverview()">Test Overview</button>
                        <div id="analyticsOverviewResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
                
                <div class="endpoint-card">
                    <h3>API Usage Stats</h3>
                    <p>Get API usage statistics</p>
                    <span class="method-tag method-get">GET</span>
                    <code>/api/analytics/api-usage</code>
                    
                    <div class="test-form">
                        <div class="form-group">
                            <label>Query Parameters:</label>
                            <input type="text" id="apiUsageQuery" placeholder="timeRange=week&endpoint=/api/tasks" />
                        </div>
                        <button class="btn btn-success" onclick="testApiUsage()">Test API Usage</button>
                        <div id="apiUsageResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
                
                <div class="endpoint-card">
                    <h3>Real-time Metrics</h3>
                    <p>Get live system metrics</p>
                    <span class="method-tag method-get">GET</span>
                    <code>/api/analytics/real-time</code>
                    
                    <div class="test-form">
                        <button class="btn btn-success" onclick="testRealTimeMetrics()">Test Real-time</button>
                        <div id="realTimeResponse" class="response-area" style="display:none;"></div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- API Features -->
        <div class="api-section">
            <h2><span class="emoji">✨</span>API Features</h2>
            <div class="feature-grid">
                <div class="feature-card">
                    <h4><span class="emoji">🔐</span>Multi-Auth Support</h4>
                    <p>Bearer tokens, API keys, and session-based authentication</p>
                </div>
                <div class="feature-card">
                    <h4><span class="emoji">🌐</span>CORS Enabled</h4>
                    <p>Cross-origin requests supported with proper headers</p>
                </div>
                <div class="feature-card">
                    <h4><span class="emoji">📄</span>Comprehensive Logging</h4>
                    <p>Detailed access logs for WAF training and analysis</p>
                </div>
                <div class="feature-card">
                    <h4><span class="emoji">⚡</span>RESTful Design</h4>
                    <p>Standard HTTP methods and status codes</p>
                </div>
                <div class="feature-card">
                    <h4><span class="emoji">🔍</span>Advanced Filtering</h4>
                    <p>Search, sort, and paginate through all resources</p>
                </div>
                <div class="feature-card">
                    <h4><span class="emoji">📊</span>Built-in Analytics</h4>
                    <p>Track API usage, performance, and security metrics</p>
                </div>
            </div>
        </div>
        
        <!-- Demo Actions -->
        <div class="api-section">
            <h2><span class="emoji">🎮</span>Quick Demo Actions</h2>
            <p>Generate various HTTP traffic patterns for WAF training:</p>
            <div style="margin-top: 20px;">
                <button class="demo-btn" onclick="runTrafficDemo()">Generate Mixed Traffic</button>
                <button class="demo-btn" onclick="runErrorDemo()">Generate Error Patterns</button>
                <button class="demo-btn" onclick="runAuthDemo()">Generate Auth Patterns</button>
                <button class="demo-btn" onclick="runFileDemo()">Generate File Operations</button>
                <button class="demo-btn" onclick="runAnalyticsDemo()">Generate Analytics Calls</button>
            </div>
            <div id="demoResponse" class="response-area" style="display:none; margin-top: 20px;"></div>
        </div>
    </div>
    
    <script>
        // Base API URL
        const API_BASE = '/api';
        
        // Utility function to make API calls
        async function apiCall(method, url, body = null, headers = {}) {
            const config = {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    ...headers
                }
            };
            
            if (body) {
                config.body = body instanceof FormData ? body : JSON.stringify(body);
                if (body instanceof FormData) {
                    delete config.headers['Content-Type'];
                }
            }
            
            try {
                const response = await fetch(url, config);
                const responseText = await response.text();
                
                return {
                    status: response.status,
                    statusText: response.statusText,
                    data: responseText ? JSON.parse(responseText) : null
                };
            } catch (error) {
                return {
                    status: 0,
                    statusText: 'Network Error',
                    data: { error: error.message }
                };
            }
        }
        
        // Display response in UI
        function displayResponse(elementId, response) {
            const element = document.getElementById(elementId);
            element.style.display = 'block';
            element.textContent = `Status: ${response.status} ${response.statusText}\\n\\n${JSON.stringify(response.data, null, 2)}`;
        }
        
        // Authentication tests
        async function testLogin() {
            const body = JSON.parse(document.getElementById('loginBody').value);
            const response = await apiCall('POST', `${API_BASE}/auth/login`, body);
            displayResponse('loginResponse', response);
        }
        
        async function testRegister() {
            const body = JSON.parse(document.getElementById('registerBody').value);
            const response = await apiCall('POST', `${API_BASE}/auth/register`, body);
            displayResponse('registerResponse', response);
        }
        
        async function testValidate() {
            const token = document.getElementById('validateToken').value;
            const headers = token ? { 'Authorization': token } : {};
            const response = await apiCall('POST', `${API_BASE}/auth/validate`, null, headers);
            displayResponse('validateResponse', response);
        }
        
        // Tasks tests
        async function testListTasks() {
            const query = document.getElementById('tasksQuery').value;
            const url = query ? `${API_BASE}/tasks?${query}` : `${API_BASE}/tasks`;
            const response = await apiCall('GET', url);
            displayResponse('tasksResponse', response);
        }
        
        async function testCreateTask() {
            const body = JSON.parse(document.getElementById('createTaskBody').value);
            const response = await apiCall('POST', `${API_BASE}/tasks`, body);
            displayResponse('createTaskResponse', response);
        }
        
        async function testUpdateTask() {
            const taskId = document.getElementById('updateTaskId').value;
            const body = JSON.parse(document.getElementById('updateTaskBody').value);
            const response = await apiCall('PUT', `${API_BASE}/tasks/${taskId}`, body);
            displayResponse('updateTaskResponse', response);
        }
        
        // Users tests
        async function testListUsers() {
            const query = document.getElementById('usersQuery').value;
            const url = query ? `${API_BASE}/users?${query}` : `${API_BASE}/users`;
            const response = await apiCall('GET', url);
            displayResponse('usersResponse', response);
        }
        
        async function testCreateUser() {
            const body = JSON.parse(document.getElementById('createUserBody').value);
            const response = await apiCall('POST', `${API_BASE}/users`, body);
            displayResponse('createUserResponse', response);
        }
        
        async function testGetUser() {
            const userId = document.getElementById('getUserId').value;
            const response = await apiCall('GET', `${API_BASE}/users/${userId}`);
            displayResponse('getUserResponse', response);
        }
        
        // Projects tests
        async function testListProjects() {
            const query = document.getElementById('projectsQuery').value;
            const url = query ? `${API_BASE}/projects?${query}` : `${API_BASE}/projects`;
            const response = await apiCall('GET', url);
            displayResponse('projectsResponse', response);
        }
        
        async function testCreateProject() {
            const body = JSON.parse(document.getElementById('createProjectBody').value);
            const response = await apiCall('POST', `${API_BASE}/projects`, body);
            displayResponse('createProjectResponse', response);
        }
        
        // Files tests
        async function testListFiles() {
            const query = document.getElementById('filesQuery').value;
            const url = query ? `${API_BASE}/files?${query}` : `${API_BASE}/files`;
            const response = await apiCall('GET', url);
            displayResponse('filesResponse', response);
        }
        
        async function testFileUpload() {
            const fileInput = document.getElementById('fileUpload');
            const category = document.getElementById('fileCategory').value;
            
            if (!fileInput.files[0]) {
                alert('Please select a file to upload');
                return;
            }
            
            const formData = new FormData();
            formData.append('file', fileInput.files[0]);
            formData.append('category', category);
            formData.append('uploadedBy', 'demo-user');
            
            const response = await apiCall('POST', `${API_BASE}/files/upload`, formData);
            displayResponse('fileUploadResponse', response);
        }
        
        async function testFileInfo() {
            const fileId = document.getElementById('fileInfoId').value;
            const response = await apiCall('GET', `${API_BASE}/files/info/${fileId}`);
            displayResponse('fileInfoResponse', response);
        }
        
        // Analytics tests
        async function testAnalyticsOverview() {
            const response = await apiCall('GET', `${API_BASE}/analytics`);
            displayResponse('analyticsOverviewResponse', response);
        }
        
        async function testApiUsage() {
            const query = document.getElementById('apiUsageQuery').value;
            const url = query ? `${API_BASE}/analytics/api-usage?${query}` : `${API_BASE}/analytics/api-usage`;
            const response = await apiCall('GET', url);
            displayResponse('apiUsageResponse', response);
        }
        
        async function testRealTimeMetrics() {
            const response = await apiCall('GET', `${API_BASE}/analytics/real-time`);
            displayResponse('realTimeResponse', response);
        }
        
        // Demo traffic generators
        async function runTrafficDemo() {
            const demoElement = document.getElementById('demoResponse');
            demoElement.style.display = 'block';
            demoElement.textContent = 'Generating mixed traffic patterns...\\n\\n';
            
            const endpoints = [
                { method: 'GET', url: '/api/tasks' },
                { method: 'GET', url: '/api/users' },
                { method: 'GET', url: '/api/projects' },
                { method: 'POST', url: '/api/auth/login', body: {username: 'admin', password: 'admin123'} },
                { method: 'GET', url: '/api/analytics' },
                { method: 'GET', url: '/api/files' }
            ];
            
            for (let endpoint of endpoints) {
                const response = await apiCall(endpoint.method, endpoint.url, endpoint.body);
                demoElement.textContent += `${endpoint.method} ${endpoint.url} -> ${response.status}\\n`;
                await new Promise(resolve => setTimeout(resolve, 500)); // Wait 500ms between requests
            }
        }
        
        async function runErrorDemo() {
            const demoElement = document.getElementById('demoResponse');
            demoElement.style.display = 'block';
            demoElement.textContent = 'Generating error patterns...\\n\\n';
            
            const errorEndpoints = [
                { method: 'GET', url: '/api/tasks/999999' }, // 404
                { method: 'POST', url: '/api/auth/login', body: {username: 'invalid', password: 'wrong'} }, // 401
                { method: 'POST', url: '/api/users', body: {username: 'admin'} }, // 400 - missing fields
                { method: 'PUT', url: '/api/projects/999999', body: {name: 'test'} }, // 404
                { method: 'DELETE', url: '/api/files/999999' } // 404
            ];
            
            for (let endpoint of errorEndpoints) {
                const response = await apiCall(endpoint.method, endpoint.url, endpoint.body);
                demoElement.textContent += `${endpoint.method} ${endpoint.url} -> ${response.status}\\n`;
                await new Promise(resolve => setTimeout(resolve, 300));
            }
        }
        
        async function runAuthDemo() {
            const demoElement = document.getElementById('demoResponse');
            demoElement.style.display = 'block';
            demoElement.textContent = 'Generating authentication patterns...\\n\\n';
            
            const authPatterns = [
                { method: 'POST', url: '/api/auth/login', body: {username: 'admin', password: 'admin123'} },
                { method: 'POST', url: '/api/auth/validate', headers: {'X-API-Key': 'demo-api-key-admin'} },
                { method: 'GET', url: '/api/auth/profile', headers: {'Authorization': 'Bearer token_admin_123456'} },
                { method: 'POST', url: '/api/auth/logout' },
                { method: 'POST', url: '/api/auth/refresh', headers: {'Authorization': 'Bearer token_admin_123456'} }
            ];
            
            for (let pattern of authPatterns) {
                const response = await apiCall(pattern.method, pattern.url, pattern.body, pattern.headers);
                demoElement.textContent += `${pattern.method} ${pattern.url} -> ${response.status}\\n`;
                await new Promise(resolve => setTimeout(resolve, 400));
            }
        }
        
        async function runFileDemo() {
            const demoElement = document.getElementById('demoResponse');
            demoElement.style.display = 'block';
            demoElement.textContent = 'Generating file operation patterns...\\n\\n';
            
            const fileOperations = [
                { method: 'GET', url: '/api/files' },
                { method: 'GET', url: '/api/files?category=image' },
                { method: 'GET', url: '/api/files/info/1' },
                { method: 'GET', url: '/api/files/download/1' },
                { method: 'DELETE', url: '/api/files/999' } // 404
            ];
            
            for (let operation of fileOperations) {
                const response = await apiCall(operation.method, operation.url);
                demoElement.textContent += `${operation.method} ${operation.url} -> ${response.status}\\n`;
                await new Promise(resolve => setTimeout(resolve, 350));
            }
        }
        
        async function runAnalyticsDemo() {
            const demoElement = document.getElementById('demoResponse');
            demoElement.style.display = 'block';
            demoElement.textContent = 'Generating analytics call patterns...\\n\\n';
            
            const analyticsEndpoints = [
                { method: 'GET', url: '/api/analytics' },
                { method: 'GET', url: '/api/analytics/api-usage?timeRange=week' },
                { method: 'GET', url: '/api/analytics/users' },
                { method: 'GET', url: '/api/analytics/performance' },
                { method: 'GET', url: '/api/analytics/security' },
                { method: 'GET', url: '/api/analytics/real-time' },
                { method: 'POST', url: '/api/analytics/report', body: {type: 'comprehensive', timeRange: 'week'} }
            ];
            
            for (let endpoint of analyticsEndpoints) {
                const response = await apiCall(endpoint.method, endpoint.url, endpoint.body);
                demoElement.textContent += `${endpoint.method} ${endpoint.url} -> ${response.status}\\n`;
                await new Promise(resolve => setTimeout(resolve, 600));
            }
        }
    </script>
</body>
</html>
