#!/usr/bin/env python3
"""
WAF Testing and Demo Script
Generates mixed traffic patterns to demonstrate WAF functionality
"""

import requests
import json
import time
import random
from typing import List, Dict

class WAFTester:
    def __init__(self, waf_url: str = "http://localhost:8082"):
        self.waf_url = waf_url
        self.app_urls = [
            "http://localhost:8080/blog-cms/",
            "http://localhost:8080/ecommerce/", 
            "http://localhost:8080/rest-api/"
        ]
        
    def create_benign_requests(self) -> List[Dict]:
        """Create normal, legitimate requests"""
        requests_data = [
            {
                "method": "GET",
                "path": "/blog-cms/",
                "query_string": "",
                "headers": {"User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"},
                "remote_addr": "192.168.1.100",
                "user_agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
            },
            {
                "method": "GET",
                "path": "/ecommerce/products",
                "query_string": "category=electronics&sort=price",
                "headers": {"User-Agent": "Chrome/91.0.4472.124"},
                "remote_addr": "192.168.1.101",
                "user_agent": "Chrome/91.0.4472.124"
            },
            {
                "method": "POST",
                "path": "/rest-api/users",
                "query_string": "",
                "headers": {"Content-Type": "application/json", "User-Agent": "curl/7.68.0"},
                "remote_addr": "192.168.1.102",
                "user_agent": "curl/7.68.0",
                "body": '{"name": "John Doe", "email": "john@example.com"}'
            },
            {
                "method": "GET",
                "path": "/blog-cms/posts",
                "query_string": "search=python tutorial",
                "headers": {"User-Agent": "Firefox/89.0"},
                "remote_addr": "192.168.1.103",
                "user_agent": "Firefox/89.0"
            }
        ]
        return requests_data
    
    def create_malicious_requests(self) -> List[Dict]:
        """Create malicious attack requests"""
        malicious_requests = [
            {
                "method": "GET",
                "path": "/blog-cms/search",
                "query_string": "q=1' UNION SELECT username,password FROM users--",
                "headers": {"User-Agent": "sqlmap/1.0"},
                "remote_addr": "10.0.0.5",
                "user_agent": "sqlmap/1.0"
            },
            {
                "method": "POST",
                "path": "/ecommerce/login",
                "query_string": "",
                "headers": {"User-Agent": "curl/7.68.0"},
                "remote_addr": "172.16.0.10",
                "user_agent": "curl/7.68.0",
                "body": "username=admin&password=' OR '1'='1"
            },
            {
                "method": "GET",
                "path": "/rest-api/files",
                "query_string": "file=../../../etc/passwd",
                "headers": {"User-Agent": "Mozilla/5.0"},
                "remote_addr": "203.0.113.15",
                "user_agent": "Mozilla/5.0"
            },
            {
                "method": "GET",
                "path": "/blog-cms/admin",
                "query_string": "cmd=whoami",
                "headers": {"User-Agent": "wget"},
                "remote_addr": "198.51.100.20",
                "user_agent": "wget"
            },
            {
                "method": "POST",
                "path": "/ecommerce/comment",
                "query_string": "",
                "headers": {"User-Agent": "Mozilla/5.0"},
                "remote_addr": "10.0.0.25",
                "user_agent": "Mozilla/5.0",
                "body": '<script>alert("XSS")</script>'
            }
        ]
        return malicious_requests
    
    def test_waf_analysis(self, request_data: Dict) -> Dict:
        """Send request to WAF for analysis"""
        try:
            response = requests.post(f"{self.waf_url}/analyze", json=request_data, timeout=5)
            if response.status_code == 200:
                return response.json()
            else:
                return {"error": f"HTTP {response.status_code}"}
        except Exception as e:
            return {"error": str(e)}
    
    def run_demo(self):
        """Run the complete WAF demonstration"""
        print("🛡️  WAF System Demonstration")
        print("=" * 50)
        print()
        
        # Test benign requests
        print("🟢 Testing Benign Requests:")
        print("-" * 30)
        benign_requests = self.create_benign_requests()
        benign_results = []
        
        for i, req in enumerate(benign_requests, 1):
            result = self.test_waf_analysis(req)
            benign_results.append(result)
            
            status = "✅ ALLOWED" if not result.get("is_malicious", True) else "❌ BLOCKED"
            confidence = result.get("confidence", 0)
            
            print(f"{i}. {req['method']} {req['path']}")
            print(f"   Status: {status}")
            print(f"   Confidence: {confidence:.3f}")
            print(f"   Processing: {result.get('processing_time', 0)*1000:.2f}ms")
            print()
            
            time.sleep(0.5)
        
        # Test malicious requests  
        print("🔴 Testing Malicious Requests:")
        print("-" * 30)
        malicious_requests = self.create_malicious_requests()
        malicious_results = []
        
        for i, req in enumerate(malicious_requests, 1):
            result = self.test_waf_analysis(req)
            malicious_results.append(result)
            
            status = "❌ BLOCKED" if result.get("is_malicious", False) else "⚠️  MISSED"
            confidence = result.get("confidence", 0)
            threat_type = result.get("threat_type", "unknown")
            
            print(f"{i}. {req['method']} {req['path']}")
            print(f"   Status: {status}")
            print(f"   Threat: {threat_type}")
            print(f"   Confidence: {confidence:.3f}")
            print(f"   Processing: {result.get('processing_time', 0)*1000:.2f}ms")
            print()
            
            time.sleep(0.5)
        
        # Summary statistics
        print("📊 Summary Statistics:")
        print("-" * 30)
        
        benign_blocked = sum(1 for r in benign_results if r.get("is_malicious", False))
        malicious_detected = sum(1 for r in malicious_results if r.get("is_malicious", False))
        
        print(f"Benign requests processed: {len(benign_results)}")
        print(f"False positives: {benign_blocked}")
        print(f"Malicious requests processed: {len(malicious_results)}")
        print(f"True positives: {malicious_detected}")
        print(f"Detection rate: {malicious_detected/len(malicious_results)*100:.1f}%")
        print(f"False positive rate: {benign_blocked/len(benign_results)*100:.1f}%")
        
        # WAF Statistics
        try:
            stats = requests.get(f"{self.waf_url}/stats").json()
            print(f"\nWAF Service Stats:")
            print(f"Total requests analyzed: {stats.get('total_requests', 0)}")
            print(f"Service status: {stats.get('service_status', 'unknown')}")
        except:
            print("Could not retrieve WAF statistics")

if __name__ == "__main__":
    tester = WAFTester()
    tester.run_demo()
