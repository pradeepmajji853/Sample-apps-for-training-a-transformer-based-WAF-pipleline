#!/usr/bin/env python3
"""
Simplified WAF Service for Testing
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn
import logging
import random
import time
from typing import Dict, List, Optional

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="WAF ML Service", 
    description="Transformer-based Web Application Firewall ML Service",
    version="1.0.0"
)

class RequestData(BaseModel):
    method: str = "GET"
    path: str = "/"
    query_string: str = ""
    headers: Dict[str, str] = {}
    remote_addr: str = "127.0.0.1"
    user_agent: str = ""
    body: Optional[str] = None
    timestamp: Optional[float] = None

class WAFResponse(BaseModel):
    is_malicious: bool
    confidence: float
    threat_type: Optional[str] = None
    explanation: str
    processing_time: float

class WAFService:
    def __init__(self):
        self.request_count = 0
        self.threat_patterns = [
            "union select", "drop table", "script>", "javascript:", 
            "eval(", "onclick=", "onerror=", "../", "etc/passwd",
            "cmd.exe", "whoami", "nc -e", "wget http"
        ]
        logger.info("WAF Service initialized")
    
    def analyze_request(self, request_data: RequestData) -> WAFResponse:
        """Simple rule-based analysis for testing"""
        start_time = time.time()
        self.request_count += 1
        
        # Simple threat detection logic
        full_request = f"{request_data.method} {request_data.path} {request_data.query_string} {request_data.body or ''}"
        full_request = full_request.lower()
        
        is_malicious = False
        threat_type = None
        confidence = 0.1
        
        for pattern in self.threat_patterns:
            if pattern in full_request:
                is_malicious = True
                confidence = min(0.9, confidence + 0.3)
                threat_type = "injection_attack"
                break
        
        # Add some randomness for testing
        if not is_malicious:
            confidence = random.uniform(0.05, 0.15)
            # Occasionally flag benign requests as suspicious
            if random.random() < 0.02:
                is_malicious = True
                confidence = random.uniform(0.6, 0.8)
                threat_type = "anomalous_behavior"
        
        processing_time = time.time() - start_time
        
        explanation = f"Analyzed {len(full_request)} characters"
        if is_malicious:
            explanation += f" - Detected {threat_type} with confidence {confidence:.2f}"
        
        return WAFResponse(
            is_malicious=is_malicious,
            confidence=confidence,
            threat_type=threat_type,
            explanation=explanation,
            processing_time=processing_time
        )

# Create service instance
waf_service = WAFService()

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "WAF ML Service",
        "uptime": time.time(),
        "requests_processed": waf_service.request_count
    }

@app.post("/analyze")
async def analyze_request(request_data: RequestData):
    """Analyze a request for threats"""
    try:
        result = waf_service.analyze_request(request_data)
        
        # Log the analysis
        logger.info(f"Analyzed request: {request_data.method} {request_data.path} -> "
                   f"Malicious: {result.is_malicious}, Confidence: {result.confidence:.2f}")
        
        return result
        
    except Exception as e:
        logger.error(f"Error analyzing request: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/stats")
async def get_stats():
    """Get service statistics"""
    return {
        "total_requests": waf_service.request_count,
        "service_status": "running",
        "version": "1.0.0-simplified"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {"message": "WAF ML Service is running", "version": "1.0.0"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8082, log_level="info")
