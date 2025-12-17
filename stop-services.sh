#!/bin/bash

# Knowledge Graph System - Stop Script
# This script stops all running services

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  Stopping Knowledge Graph System Services                    ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Stop API server
if pgrep -f "exec:java@api" > /dev/null; then
    echo "  Stopping API server..."
    pkill -9 -f "exec:java@api"
    echo "  ✓ API server stopped"
else
    echo "  ℹ API server not running"
fi

# Stop web server
if pgrep -f "http.server 8080" > /dev/null; then
    echo "  Stopping web server..."
    pkill -9 -f "http.server 8080"
    echo "  ✓ Web server stopped"
else
    echo "  ℹ Web server not running"
fi

echo ""
echo "✓ All services stopped"
echo ""


