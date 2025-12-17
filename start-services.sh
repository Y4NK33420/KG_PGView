#!/bin/bash

# Knowledge Graph System - Startup Script
# This script starts all required services for the application

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  Starting Knowledge Graph System Services                    ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Load environment variables from .env if it exists
if [ -f .env ]; then
    echo "✓ Loading environment variables from .env"
    export $(cat .env | grep -v '^#' | xargs)
else
    echo "⚠  No .env file found (GEMINI_API_KEY may be required for Graph RAG)"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  1. Checking PostgreSQL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check if PostgreSQL is running
if pg_isready -q; then
    echo -e "${GREEN}✓${NC} PostgreSQL is running"
else
    echo -e "${YELLOW}⚠${NC} PostgreSQL is not running"
    echo "  Starting PostgreSQL..."
    sudo service postgresql start
    sleep 2
    if pg_isready -q; then
        echo -e "${GREEN}✓${NC} PostgreSQL started successfully"
    else
        echo -e "${RED}✗${NC} Failed to start PostgreSQL"
        exit 1
    fi
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  2. Starting API Server (Port 7070)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Kill existing API server if running
if pgrep -f "exec:java@api" > /dev/null; then
    echo "  Stopping existing API server..."
    pkill -9 -f "exec:java@api"
    sleep 2
fi

# Start API server
echo "  Starting API server..."
nohup mvn -q exec:java@api > /tmp/pgview_api.log 2>&1 &
API_PID=$!

# Wait for API server to be ready
echo -n "  Waiting for API server"
for i in {1..30}; do
    sleep 1
    if curl -s http://localhost:7070/health > /dev/null 2>&1; then
        echo ""
        echo -e "${GREEN}✓${NC} API server ready (PID: $API_PID)"
        break
    fi
    echo -n "."
    if [ $i -eq 30 ]; then
        echo ""
        echo -e "${RED}✗${NC} API server failed to start"
        echo "  Check logs: tail -f /tmp/pgview_api.log"
        exit 1
    fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  3. Starting Web UI Server (Port 8080)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Kill existing web server if running
if pgrep -f "http.server 8080" > /dev/null; then
    echo "  Stopping existing web server..."
    pkill -9 -f "http.server 8080"
    sleep 1
fi

# Start web server
cd web-ui
echo "  Starting web server..."
nohup python3 -m http.server 8080 > /tmp/pgview_web.log 2>&1 &
WEB_PID=$!
cd ..

# Wait for web server to be ready
sleep 2
if curl -s http://localhost:8080 > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Web server ready (PID: $WEB_PID)"
else
    echo -e "${RED}✗${NC} Web server failed to start"
    exit 1
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  4. System Status"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "  Service              Status    URL"
echo "  ──────────────────────────────────────────────────────────"
echo -e "  PostgreSQL          ${GREEN}✓ Running${NC}  localhost:5432"
echo -e "  API Server          ${GREEN}✓ Running${NC}  http://localhost:7070"
echo -e "  Web UI              ${GREEN}✓ Running${NC}  http://localhost:8080"

if [ -n "$GEMINI_API_KEY" ]; then
    echo -e "  Graph RAG           ${GREEN}✓ Enabled${NC}  API key configured"
else
    echo -e "  Graph RAG           ${YELLOW}⚠ Disabled${NC} Set GEMINI_API_KEY in .env"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo -e "${GREEN}✓ All services started successfully!${NC}"
echo ""
echo "  🌐 Open your browser to: ${GREEN}http://localhost:8080${NC}"
echo ""
echo "  📝 Logs:"
echo "     API Server: tail -f /tmp/pgview_api.log"
echo "     Web Server: tail -f /tmp/pgview_web.log"
echo ""
echo "  🛑 To stop all services: ./stop-services.sh"
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  Ready to use! 🚀                                            ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""


