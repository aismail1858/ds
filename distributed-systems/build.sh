#!/bin/bash
echo "========================================="
echo "Building Distributed Marketplace System"
echo "========================================="

# Clean up old containers
echo "Cleaning up old containers..."
docker-compose down 2>/dev/null

# Build the system
echo "Building services..."
docker-compose build --no-cache

echo ""
echo "✓ System built successfully!"
echo ""
echo "Start system with: ./run.sh"
