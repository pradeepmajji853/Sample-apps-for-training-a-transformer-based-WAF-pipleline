"""
Log Preprocessing Module
Handles parsing, normalization, and template extraction from HTTP access logs
Uses Drain algorithm for template mining
"""

import re
import json
import logging
from typing import Dict, List, Any, Optional, Tuple
from urllib.parse import parse_qs, urlparse
from datetime import datetime
import hashlib
from drain3 import TemplateMiner
from drain3.template_miner_config import TemplateMinerConfig

class HTTPLogParser:
    """Parser for HTTP access logs in various formats"""
    
    # Common log format patterns
    NGINX_COMBINED = re.compile(
        r'(?P<remote_addr>\S+) - (?P<remote_user>\S+) \[(?P<time_local>[^\]]+)\] '
        r'"(?P<request>[^"]*)" (?P<status>\d+) (?P<body_bytes_sent>\d+) '
        r'"(?P<http_referer>[^"]*)" "(?P<http_user_agent>[^"]*)"'
        r'(?:\s+(?P<request_time>[\d\.]+))?$'
    )
    
    APACHE_COMBINED = re.compile(
        r'(?P<remote_addr>\S+) - (?P<remote_user>\S+) \[(?P<time_local>[^\]]+)\] '
        r'"(?P<request>[^"]*)" (?P<status>\d+) (?P<body_bytes_sent>\d+) '
        r'"(?P<http_referer>[^"]*)" "(?P<http_user_agent>[^"]*)"'
        r'(?:\s+(?P<request_time>[\d\.]+))?$'
    )
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        
    def parse_log_line(self, log_line: str) -> Optional[Dict[str, Any]]:
        """Parse a single log line into structured data"""
        # Try different log formats
        for pattern in [self.NGINX_COMBINED, self.APACHE_COMBINED]:
            match = pattern.match(log_line)
            if match:
                parsed = match.groupdict()
                return self._enrich_parsed_log(parsed)
                
        # If no pattern matches, try to parse as JSON
        try:
            return json.loads(log_line)
        except json.JSONDecodeError:
            pass
            
        self.logger.warning(f"Could not parse log line: {log_line}")
        return None
        
    def _enrich_parsed_log(self, parsed: Dict[str, str]) -> Dict[str, Any]:
        """Enrich parsed log with additional fields"""
        enriched = parsed.copy()
        
        # Parse request line
        request = parsed.get('request', '')
        if request:
            request_parts = request.split(' ', 2)
            if len(request_parts) >= 2:
                enriched['method'] = request_parts[0]
                enriched['path'] = request_parts[1]
                if len(request_parts) == 3:
                    enriched['protocol'] = request_parts[2]
                    
        # Parse URL components
        path = enriched.get('path', '')
        if path:
            parsed_url = urlparse(path)
            enriched['path_only'] = parsed_url.path
            enriched['query_string'] = parsed_url.query
            enriched['query_params'] = parse_qs(parsed_url.query) if parsed_url.query else {}
            
        # Convert numeric fields
        for field in ['status', 'body_bytes_sent']:
            if field in enriched:
                try:
                    enriched[field] = int(enriched[field])
                except ValueError:
                    pass
                    
        # Add derived features
        enriched['is_error'] = enriched.get('status', 200) >= 400
        enriched['method_category'] = self._categorize_method(enriched.get('method', ''))
        enriched['path_depth'] = len([p for p in path.split('/') if p]) if path else 0
        
        return enriched
        
    def _categorize_method(self, method: str) -> str:
        """Categorize HTTP method"""
        method = method.upper()
        if method in ['GET', 'HEAD', 'OPTIONS']:
            return 'SAFE'
        elif method in ['POST', 'PUT', 'PATCH', 'DELETE']:
            return 'UNSAFE'
        else:
            return 'UNKNOWN'

class LogNormalizer:
    """Normalizes log entries by replacing dynamic values with placeholders"""
    
    def __init__(self):
        self.patterns = [
            # UUIDs
            (re.compile(r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}', re.IGNORECASE), '<UUID>'),
            # Session IDs (common patterns)
            (re.compile(r'JSESSIONID=[A-F0-9]{32}', re.IGNORECASE), 'JSESSIONID=<SESSION>'),
            (re.compile(r'sessionid=[a-z0-9]{20,40}', re.IGNORECASE), 'sessionid=<SESSION>'),
            # Numbers (IDs, timestamps, etc.)
            (re.compile(r'\b\d{10,13}\b'), '<TIMESTAMP>'),  # Unix timestamps
            (re.compile(r'\b\d{4,}\b'), '<ID>'),  # Large numbers (likely IDs)
            (re.compile(r'\b\d{1,3}\b'), '<NUM>'),  # Small numbers
            # Email addresses
            (re.compile(r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}'), '<EMAIL>'),
            # IP addresses
            (re.compile(r'\b(?:\d{1,3}\.){3}\d{1,3}\b'), '<IP>'),
            # Tokens and hashes
            (re.compile(r'[a-f0-9]{32,}', re.IGNORECASE), '<HASH>'),
            # CSRF tokens
            (re.compile(r'csrf_token=[a-zA-Z0-9+/=]{20,}'), 'csrf_token=<CSRF>'),
        ]
        
    def normalize(self, text: str) -> str:
        """Normalize text by replacing dynamic patterns with placeholders"""
        normalized = text
        for pattern, replacement in self.patterns:
            normalized = pattern.sub(replacement, normalized)
        return normalized

class TemplateMiningEngine:
    """Template mining using Drain algorithm"""
    
    def __init__(self, config_path: Optional[str] = None):
        # Configure Drain
        if (config_path):
            config = TemplateMinerConfig()
            config.load(config_path)
        else:
            config = TemplateMinerConfig()
            config.drain_depth = 4
            config.sim_th = 0.4
            config.max_children = 100
            config.max_clusters = 1000
            
        self.template_miner = TemplateMiner(config=config)
        self.logger = logging.getLogger(__name__)
        
    def extract_template(self, log_message: str) -> Tuple[str, int, int]:
        """Extract template from log message
        Supports Drain3 return structure (dict) and falls back to attribute-style if present.
        Returns (template, cluster_id, cluster_count).
        """
        result = self.template_miner.add_log_message(log_message)
        # Drain3 returns a Mapping with keys: change_type, cluster_id, cluster_size, template_mined, cluster_count
        try:
            if isinstance(result, dict):
                template = result.get("template_mined") or result.get("template") or ""
                cluster_id = int(result.get("cluster_id")) if result.get("cluster_id") is not None else -1
                cluster_count = int(result.get("cluster_count")) if result.get("cluster_count") is not None else 0
                return template, cluster_id, cluster_count
            else:
                # Fallback for potential attribute-style objects
                template = getattr(result, "template", "") or getattr(result, "template_mined", "")
                cluster_id = int(getattr(result, "cluster_id", -1))
                cluster_count = int(getattr(result, "cluster_count", 0))
                return template, cluster_id, cluster_count
        except Exception as e:
            # Log and provide safe defaults
            self.logger.error(f"Template extraction error: {e}")
            return "", -1, 0
        
    def get_templates(self) -> Dict[int, str]:
        """Get all discovered templates"""
        templates = {}
        for cluster_id, cluster in self.template_miner.drain.clusters.items():
            templates[cluster_id] = cluster.get_template()
        return templates

class LogPreprocessor:
    """Main preprocessing pipeline"""
    
    def __init__(self):
        self.parser = HTTPLogParser()
        self.normalizer = LogNormalizer()
        self.template_miner = TemplateMiningEngine()
        self.logger = logging.getLogger(__name__)
        
    def process_log_entry(self, raw_log: str) -> Optional[Dict[str, Any]]:
        """Process a single log entry through the full pipeline"""
        try:
            # Parse the log line
            parsed = self.parser.parse_log_line(raw_log)
            if not parsed:
                return None
                
            # Create normalized request signature
            request_signature = self._create_request_signature(parsed)
            normalized_signature = self.normalizer.normalize(request_signature)
            
            # Extract template
            template, cluster_id, cluster_count = self.template_miner.extract_template(normalized_signature)
            
            # Create processed entry
            processed = {
                'parsed': parsed,
                'request_signature': request_signature,
                'normalized_signature': normalized_signature,
                'template': template,
                'template_id': cluster_id,
                'cluster_count': cluster_count,
                'features': self._extract_features(parsed)
            }
            
            return processed
            
        except Exception as e:
            self.logger.error(f"Error processing log entry: {e}")
            return None
            
    def _create_request_signature(self, parsed: Dict[str, Any]) -> str:
        """Create a signature string from parsed log entry"""
        method = parsed.get('method', 'UNKNOWN')
        path = parsed.get('path_only', '/')
        status = parsed.get('status', 0)
        user_agent = parsed.get('http_user_agent', '')
        
        # Include key query parameters (normalized)
        query_params = parsed.get('query_params', {})
        param_keys = sorted(query_params.keys()) if query_params else []
        param_signature = ','.join(param_keys) if param_keys else 'NO_PARAMS'
        
        return f"{method} {path} {status} {param_signature} {user_agent}"
        
    def _extract_features(self, parsed: Dict[str, Any]) -> Dict[str, Any]:
        """Extract features for ML model"""
        features = {
            'method': parsed.get('method', ''),
            'status_code': parsed.get('status', 0),
            'path_depth': parsed.get('path_depth', 0),
            'query_param_count': len(parsed.get('query_params', {})),
            'user_agent_length': len(parsed.get('http_user_agent', '')),
            'is_error': parsed.get('is_error', False),
            'method_category': parsed.get('method_category', 'UNKNOWN'),
            'has_referer': bool(parsed.get('http_referer', '') and parsed.get('http_referer') != '-'),
            'body_size': parsed.get('body_bytes_sent', 0),
        }
        
        # Security-related features
        path = parsed.get('path_only', '')
        query = parsed.get('query_string', '')
        user_agent = parsed.get('http_user_agent', '')
        
        features.update({
            'contains_script_tags': '<script' in (path + query).lower(),
            'contains_sql_keywords': any(keyword in (path + query).lower() 
                                       for keyword in ['union', 'select', 'insert', 'delete', 'drop']),
            'contains_xss_patterns': any(pattern in (path + query).lower() 
                                       for pattern in ['javascript:', 'vbscript:', 'onload=', 'onerror=']),
            'suspicious_user_agent': any(bot in user_agent.lower() 
                                       for bot in ['sqlmap', 'nmap', 'dirb', 'nikto']),
        })
        
        return features
        
    def get_template_stats(self) -> Dict[str, Any]:
        """Get template mining statistics"""
        templates = self.template_miner.get_templates()
        return {
            'total_templates': len(templates),
            'templates': templates
        }

if __name__ == "__main__":
    # Test the preprocessor
    preprocessor = LogPreprocessor()
    
    sample_logs = [
        '127.0.0.1 - - [23/Sep/2025:10:30:00 +0000] "GET /blog-cms/posts/123 HTTP/1.1" 200 1234 "-" "Mozilla/5.0"',
        '127.0.0.1 - - [23/Sep/2025:10:30:01 +0000] "POST /ecommerce/cart HTTP/1.1" 201 567 "http://localhost/ecommerce/" "Mozilla/5.0"',
        '127.0.0.1 - - [23/Sep/2025:10:30:02 +0000] "GET /rest-api/users?id=456 HTTP/1.1" 200 890 "-" "curl/7.68.0"',
    ]
    
    for log in sample_logs:
        processed = preprocessor.process_log_entry(log)
        if processed:
            print(f"Template ID: {processed['template_id']}")
            print(f"Template: {processed['template']}")
            print(f"Features: {json.dumps(processed['features'], indent=2)}")
            print("-" * 50)
