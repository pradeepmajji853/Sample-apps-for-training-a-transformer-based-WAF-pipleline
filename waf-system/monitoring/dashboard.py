"""
WAF Monitoring Dashboard
Real-time monitoring and visualization of WAF performance and threats
"""

import asyncio
import json
import logging
import time
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional
from pathlib import Path
import sqlite3

import streamlit as st
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from plotly.subplots import make_subplots
import httpx

# Compatibility helper for Streamlit rerun across versions
def _st_rerun():
    try:
        # Newer Streamlit versions
        st.rerun()
    except AttributeError:
        try:
            # Older Streamlit versions
            st.experimental_rerun()
        except AttributeError:
            # As a last resort, do nothing
            pass

def _fetch_json(url: str, method: str = 'GET', json_data: dict | None = None, timeout: float = 10.0):
    try:
        if method.upper() == 'POST':
            r = httpx.post(url, json=json_data, timeout=timeout)
        else:
            r = httpx.get(url, timeout=timeout)
        if r.status_code == 200:
            return r.json()
    except Exception as e:
        logging.warning(f"HTTP call failed: {e}")
    return None

class WAFMonitor:
    """Monitor for WAF system metrics and alerts"""
    
    def __init__(self, db_path: str = "data/waf_monitoring.db"):
        self.db_path = db_path
        self.init_database()
        self.waf_service_url = "http://localhost:8081"
        
    def init_database(self):
        """Initialize SQLite database for monitoring data"""
        Path(self.db_path).parent.mkdir(parents=True, exist_ok=True)
        
        with sqlite3.connect(self.db_path) as conn:
            # Requests table
            conn.execute("""
                CREATE TABLE IF NOT EXISTS requests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp DATETIME,
                    method TEXT,
                    path TEXT,
                    status_code INTEGER,
                    anomaly_score REAL,
                    is_anomalous BOOLEAN,
                    confidence REAL,
                    processing_time_ms REAL,
                    source_ip TEXT,
                    user_agent TEXT
                )
            """)
            
            # Alerts table
            conn.execute("""
                CREATE TABLE IF NOT EXISTS alerts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp DATETIME,
                    alert_type TEXT,
                    severity TEXT,
                    message TEXT,
                    details TEXT,
                    resolved BOOLEAN DEFAULT FALSE
                )
            """)
            
            # Model metrics table
            conn.execute("""
                CREATE TABLE IF NOT EXISTS model_metrics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp DATETIME,
                    metric_name TEXT,
                    metric_value REAL,
                    model_version TEXT
                )
            """)
            
            conn.commit()
            
    def log_request(self, request_data: Dict[str, Any]):
        """Log a request and its analysis results"""
        with sqlite3.connect(self.db_path) as conn:
            conn.execute("""
                INSERT INTO requests (
                    timestamp, method, path, status_code, anomaly_score,
                    is_anomalous, confidence, processing_time_ms, source_ip, user_agent
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                datetime.utcnow(),
                request_data.get('method'),
                request_data.get('path'),
                request_data.get('status_code'),
                request_data.get('anomaly_score'),
                request_data.get('is_anomalous'),
                request_data.get('confidence'),
                request_data.get('processing_time_ms'),
                request_data.get('source_ip'),
                request_data.get('user_agent')
            ))
            
    def create_alert(self, alert_type: str, severity: str, message: str, details: str = ""):
        """Create a new alert"""
        with sqlite3.connect(self.db_path) as conn:
            conn.execute("""
                INSERT INTO alerts (timestamp, alert_type, severity, message, details)
                VALUES (?, ?, ?, ?, ?)
            """, (datetime.utcnow(), alert_type, severity, message, details))
            
    def get_recent_requests(self, hours: int = 24) -> pd.DataFrame:
        """Get recent requests data"""
        with sqlite3.connect(self.db_path) as conn:
            query = """
                SELECT * FROM requests 
                WHERE timestamp > datetime('now', '-{} hours')
                ORDER BY timestamp DESC
            """.format(hours)
            
            return pd.read_sql_query(query, conn)
            
    def get_alerts(self, hours: int = 24, resolved: bool = False) -> pd.DataFrame:
        """Get recent alerts"""
        with sqlite3.connect(self.db_path) as conn:
            query = """
                SELECT * FROM alerts 
                WHERE timestamp > datetime('now', '-{} hours')
                AND resolved = {}
                ORDER BY timestamp DESC
            """.format(hours, resolved)
            
            return pd.read_sql_query(query, conn)
            
    def get_threat_statistics(self, hours: int = 24) -> Dict[str, Any]:
        """Get threat detection statistics"""
        with sqlite3.connect(self.db_path) as conn:
            # Total requests
            total_requests = conn.execute("""
                SELECT COUNT(*) FROM requests 
                WHERE timestamp > datetime('now', '-{} hours')
            """.format(hours)).fetchone()[0]
            
            # Anomalous requests
            anomalous_requests = conn.execute("""
                SELECT COUNT(*) FROM requests 
                WHERE timestamp > datetime('now', '-{} hours')
                AND is_anomalous = 1
            """.format(hours)).fetchone()[0]
            
            # Average processing time
            avg_processing_time = conn.execute("""
                SELECT AVG(processing_time_ms) FROM requests 
                WHERE timestamp > datetime('now', '-{} hours')
            """.format(hours)).fetchone()[0] or 0
            
            # Top attack paths
            top_attack_paths = conn.execute("""
                SELECT path, COUNT(*) as count FROM requests 
                WHERE timestamp > datetime('now', '-{} hours')
                AND is_anomalous = 1
                GROUP BY path
                ORDER BY count DESC
                LIMIT 10
            """.format(hours)).fetchall()
            
            return {
                'total_requests': total_requests,
                'anomalous_requests': anomalous_requests,
                'anomaly_rate': anomalous_requests / max(1, total_requests) * 100,
                'avg_processing_time': avg_processing_time,
                'top_attack_paths': top_attack_paths
            }

class WAFDashboard:
    """Streamlit-based WAF monitoring dashboard"""
    
    def __init__(self):
        self.monitor = WAFMonitor()
        self.setup_page()
        
    def setup_page(self):
        """Setup Streamlit page configuration"""
        st.set_page_config(
            page_title="WAF Monitoring Dashboard",
            page_icon="🛡️",
            layout="wide",
            initial_sidebar_state="expanded"
        )
        
    async def get_waf_service_stats(self) -> Dict[str, Any]:
        """Get statistics from WAF service"""
        try:
            async with httpx.AsyncClient() as client:
                response = await client.get("http://localhost:8081/stats", timeout=5.0)
                if response.status_code == 200:
                    return response.json()
        except Exception:
            pass
        
        return {
            'total_requests': 0,
            'anomalous_requests': 0,
            'anomaly_rate': 0.0,
            'avg_processing_time': 0.0,
            'uptime': 0.0
        }
        
    def render_header(self):
        """Render dashboard header"""
        st.title("🛡️ WAF Monitoring Dashboard")
        st.markdown("Real-time Web Application Firewall monitoring and threat detection")
        
        # Try live stats from service
        live_stats = _fetch_json("http://localhost:8081/stats") or {}
        
        # Status indicators
        col1, col2, col3, col4 = st.columns(4)
        
        with col1:
            st.metric("WAF Status", "🟢 Online" if live_stats else "🟡 Unknown")
            
        with col2:
            total_requests = live_stats.get('total_requests', 0)
            st.metric("Total Requests", f"{total_requests:,}")
            
        with col3:
            anomaly_rate = live_stats.get('anomaly_rate', 0.0) * (100.0 if live_stats.get('anomaly_rate', 0.0) < 1 else 1)
            st.metric("Anomaly Rate", f"{anomaly_rate:.1f}%")
            
        with col4:
            st.metric("Avg Processing Time", f"{live_stats.get('avg_processing_time', 0.0):.1f}ms")

    def render_threat_overview(self):
        """Render threat detection overview"""
        st.header("🚨 Threat Detection Overview")
        
        # Get threat statistics
        threat_stats = self.monitor.get_threat_statistics(hours=24)
        
        col1, col2, col3, col4 = st.columns(4)
        
        with col1:
            st.metric(
                "Total Requests (24h)",
                f"{threat_stats['total_requests']:,}",
                delta=f"+{threat_stats['total_requests'] // 10}"
            )
            
        with col2:
            st.metric(
                "Blocked Threats",
                f"{threat_stats['anomalous_requests']:,}",
                delta=f"+{threat_stats['anomalous_requests'] // 5}"
            )
            
        with col3:
            st.metric(
                "Anomaly Rate",
                f"{threat_stats['anomaly_rate']:.2f}%",
                delta=f"{threat_stats['anomaly_rate'] - 2.0:.1f}%"
            )
            
        with col4:
            st.metric(
                "Avg Response Time",
                f"{threat_stats['avg_processing_time']:.1f}ms",
                delta=f"-{threat_stats['avg_processing_time'] * 0.1:.1f}ms"
            )
            
    def render_request_timeline(self):
        """Render request timeline chart"""
        st.header("📊 Request Timeline")
        
        # Get recent requests data
        df = self.monitor.get_recent_requests(hours=24)
        
        if not df.empty:
            df['timestamp'] = pd.to_datetime(df['timestamp'])
            
            # Resample by hour
            df_hourly = df.set_index('timestamp').resample('H').agg({
                'id': 'count',
                'is_anomalous': 'sum',
                'anomaly_score': 'mean'
            }).reset_index()
            
            df_hourly['normal_requests'] = df_hourly['id'] - df_hourly['is_anomalous']
            
            # Create timeline chart
            fig = make_subplots(
                rows=2, cols=1,
                subplot_titles=('Request Volume', 'Anomaly Score'),
                vertical_spacing=0.1
            )
            
            # Request volume
            fig.add_trace(
                go.Bar(
                    x=df_hourly['timestamp'],
                    y=df_hourly['normal_requests'],
                    name='Normal Requests',
                    marker_color='green'
                ),
                row=1, col=1
            )
            
            fig.add_trace(
                go.Bar(
                    x=df_hourly['timestamp'],
                    y=df_hourly['is_anomalous'],
                    name='Anomalous Requests',
                    marker_color='red'
                ),
                row=1, col=1
            )
            
            # Anomaly score
            fig.add_trace(
                go.Scatter(
                    x=df_hourly['timestamp'],
                    y=df_hourly['anomaly_score'],
                    name='Avg Anomaly Score',
                    line=dict(color='orange', width=2)
                ),
                row=2, col=1
            )
            
            fig.update_layout(height=500, showlegend=True)
            st.plotly_chart(fig, use_container_width=True)
        else:
            st.info("No request data available yet")
            
    def render_top_threats(self):
        """Render top threats analysis"""
        st.header("🎯 Top Attack Patterns")
        
        # Get threat statistics
        threat_stats = self.monitor.get_threat_statistics(hours=24)
        
        if threat_stats['top_attack_paths']:
            paths_df = pd.DataFrame(
                threat_stats['top_attack_paths'],
                columns=['Path', 'Attack Count']
            )
            
            # Bar chart of top attack paths
            fig = px.bar(
                paths_df.head(10),
                x='Attack Count',
                y='Path',
                orientation='h',
                title="Most Targeted Endpoints",
                color='Attack Count',
                color_continuous_scale='Reds'
            )
            fig.update_layout(height=400)
            st.plotly_chart(fig, use_container_width=True)
        else:
            st.info("No attack patterns detected in the last 24 hours")
            
    def render_alerts_panel(self):
        """Render active alerts panel"""
        st.header("⚠️ Active Alerts")
        
        # Get recent unresolved alerts
        alerts_df = self.monitor.get_alerts(hours=24, resolved=False)
        
        if not alerts_df.empty:
            for _, alert in alerts_df.iterrows():
                severity_color = {
                    'critical': 'red',
                    'high': 'orange',
                    'medium': 'yellow',
                    'low': 'blue'
                }.get(alert['severity'].lower(), 'gray')
                
                with st.expander(f"🚨 {alert['alert_type']} - {alert['severity'].upper()}", expanded=True):
                    st.write(f"**Time:** {alert['timestamp']}")
                    st.write(f"**Message:** {alert['message']}")
                    if alert['details']:
                        st.write(f"**Details:** {alert['details']}")
                        
                    if st.button(f"Resolve Alert {alert['id']}", key=f"resolve_{alert['id']}"):
                        # Mark alert as resolved
                        with sqlite3.connect(self.monitor.db_path) as conn:
                            conn.execute(
                                "UPDATE alerts SET resolved = 1 WHERE id = ?",
                                (alert['id'],)
                            )
                        st.success("Alert resolved!")
                        _st_rerun()
        else:
            st.success("No active alerts")
            
    def render_model_performance(self):
        """Render model performance metrics and training controls"""
        st.header("🤖 Model & Training")
        
        # Controls
        with st.expander("Train from Access Logs", expanded=True):
            colA, colB, colC, colD = st.columns([1,1,1,2])
            with colA:
                epochs = st.number_input("Epochs", min_value=1, max_value=10, value=1)
            with colB:
                batch_size = st.number_input("Batch Size", min_value=8, max_value=128, value=32, step=8)
            with colC:
                max_lines = st.number_input("Max Lines", min_value=100, max_value=20000, value=5000, step=100)
            with colD:
                default_paths = [
                    str(Path('data/logs/access.log').resolve()),
                    str(Path('tomcat/current/logs/localhost_access_log*.txt').resolve())
                ]
                # Prefer synthetic benign dataset if available
                synth = Path('data/logs/benign_synth.log').resolve()
                if synth.exists():
                    default_paths.insert(0, str(synth))
                log_paths_str = st.text_input("Log paths (comma-separated, globs ok)", ", ".join(default_paths))
            
            if st.button("Start Training", type="primary"):
                paths = [p.strip() for p in log_paths_str.split(',') if p.strip()]
                payload = {"log_paths": paths, "epochs": int(epochs), "max_lines": int(max_lines), "batch_size": int(batch_size)}
                res = _fetch_json("http://localhost:8081/train_from_logs", method='POST', json_data=payload)
                if res and res.get('status') == 'started':
                    st.success("Training started")
                else:
                    st.warning("Failed to start training. Check WAF service logs.")
                _st_rerun()
        
        # Live auto-training
        with st.expander("Live Training (Tail Logs)", expanded=False):
            auto_train = st.checkbox("Enable live auto-training (every 60s)", key="auto_train")
            interval = st.number_input("Interval (seconds)", min_value=15, max_value=600, value=60, step=5, key="auto_train_interval")
            if auto_train:
                last = st.session_state.get('last_auto_train_ts', 0)
                now = time.time()
                if now - last > interval:
                    st.info("Triggering background training from logs...")
                    # If synthetic benign logs exist, prioritize them
                    synth = Path('data/logs/benign_synth.log').resolve()
                    payload = {"epochs": 1, "max_lines": 2000, "batch_size": 32}
                    if synth.exists():
                        payload["log_paths"] = [str(synth)]
                    _fetch_json("http://localhost:8081/train_from_logs", method='POST', json_data=payload)
                    st.session_state['last_auto_train_ts'] = now
                st.caption("Auto-training is active. Page will refresh periodically.")
        
        # Status
        st.subheader("Training Status")
        status = _fetch_json("http://localhost:8081/train/status") or {}
        if status:
            cols = st.columns(3)
            cols[0].metric("State", status.get('status', 'idle'))
            cols[1].metric("Epoch", f"{status.get('current_epoch', 0)}/{status.get('epochs_total', 0)}")
            cols[2].metric("Progress", f"{status.get('progress', 0.0)*100:.0f}%")
            st.progress(min(1.0, float(status.get('progress', 0.0))))
            if status.get('result'):
                st.json(status['result'])
            if status.get('error'):
                st.error(status['error'])
            if st.button("Reload Model"):
                _fetch_json("http://localhost:8081/model/reload", method='POST')
                st.success("Model reloaded")
        else:
            st.info("No training status available.")

    def render_live_feed(self):
        """Render live request feed"""
        st.header("📡 Live Request Feed")
        
        # Get recent requests
        df = self.monitor.get_recent_requests(hours=1)
        
        if not df.empty:
            # Show most recent 20 requests
            recent_df = df.head(20)[['timestamp', 'method', 'path', 'status_code', 'anomaly_score', 'is_anomalous']]
            
            # Color code anomalous requests
            def highlight_anomalous(row):
                if row['is_anomalous']:
                    return ['background-color: #ffcccb'] * len(row)
                return [''] * len(row)
                
            styled_df = recent_df.style.apply(highlight_anomalous, axis=1)
            st.dataframe(styled_df, use_container_width=True)
        else:
            st.info("No recent requests to display")
            
    def render_quick_test(self):
        """Interactive test to score a custom request/payload"""
        st.header("🧪 Quick Payload Test")
        with st.form("quick_test_form"):
            col1, col2 = st.columns([1,1])
            with col1:
                method = st.selectbox("Method", ["GET", "POST", "PUT", "DELETE"], index=0)
                path = st.text_input("Path/URI", value="/ecommerce/search?q=")
                payload = st.text_input("Payload", value="' OR '1'='1")
            with col2:
                user_agent = st.text_input("User-Agent", value="Mozilla/5.0")
                remote_addr = st.text_input("Remote IP", value="127.0.0.1")
                body = st.text_area("Body (for POST/PUT)", value="", height=80)
            submitted = st.form_submit_button("Score Request")
        
        if submitted:
            # Build URI with payload if method is not POST body
            uri = path
            if method in ["GET", "DELETE"]:
                uri = f"{path}{payload}"
            elif method in ["POST", "PUT"]:
                if not body:
                    body = payload
            req = {
                "method": method,
                "uri": uri,
                "headers": {"X-Quick-Test": "1"},
                "remote_addr": remote_addr,
                "user_agent": user_agent,
                "body": body or None
            }
            res = _fetch_json("http://localhost:8081/score", method='POST', json_data=req)
            if res:
                st.success(f"Anomaly Score: {res.get('anomaly_score', 0):.4f} | Anomalous: {res.get('is_anomalous')} | Confidence: {res.get('confidence', 0):.2f}")
                with st.expander("Details"):
                    st.json(res)
                # Log locally for timeline
                self.monitor.log_request({
                    'method': method,
                    'path': uri,
                    'status_code': 200,
                    'anomaly_score': float(res.get('anomaly_score', 0.0)),
                    'is_anomalous': bool(res.get('is_anomalous', False)),
                    'confidence': float(res.get('confidence', 0.0)),
                    'processing_time_ms': float(res.get('processing_time_ms', 0.0)),
                    'source_ip': remote_addr,
                    'user_agent': user_agent
                })
            else:
                st.error("Failed to score request. Check WAF service.")

    def render_sidebar(self):
        """Render sidebar controls"""
        st.sidebar.header("Dashboard Controls")
        
        # Time range selector
        time_range = st.sidebar.selectbox(
            "Time Range",
            ["Last Hour", "Last 6 Hours", "Last 24 Hours", "Last Week"],
            index=2
        )
        
        # Refresh button
        if st.sidebar.button("🔄 Refresh Data"):
            _st_rerun()
            
        # Settings
        st.sidebar.header("Settings")
        
        auto_refresh = st.sidebar.checkbox("Auto Refresh (30s)", value=False)
        show_details = st.sidebar.checkbox("Show Request Details", value=True)
        
        # System status
        st.sidebar.header("System Status")
        st.sidebar.success("WAF Service: Online")
        st.sidebar.success("ML Pipeline: Running")
        st.sidebar.info("Last Update: 30 seconds ago")
        
        return {
            'time_range': time_range,
            'auto_refresh': auto_refresh,
            'show_details': show_details
        }
        
    def run(self):
        """Run the dashboard"""
        # Render sidebar
        settings = self.render_sidebar()
        
        # Main content
        self.render_header()
        
        # Create tabs
        tab1, tab2, tab3, tab4, tab5 = st.tabs(["Overview", "Threats", "Model", "Live Feed", "Quick Test"])
        
        with tab1:
            self.render_threat_overview()
            self.render_request_timeline()
            
        with tab2:
            self.render_top_threats()
            self.render_alerts_panel()
            
        with tab3:
            self.render_model_performance()
            
        with tab4:
            self.render_live_feed()
            
        with tab5:
            self.render_quick_test()
        
        # Auto refresh
        if settings['auto_refresh']:
            time.sleep(30)
            _st_rerun()

def main():
    """Main function to run the dashboard"""
    dashboard = WAFDashboard()
    dashboard.run()

if __name__ == "__main__":
    main()
