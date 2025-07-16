#!/bin/bash
echo "========================================="
echo "Starting Distributed Marketplace System"
echo "========================================="

# Stoppe alte Container falls vorhanden
echo "Cleaning up old containers..."
docker-compose down 2>/dev/null

# Starte System
echo "Starting services..."
docker-compose build --no-cache
docker-compose up -d

# Warte kurz
sleep 3

# Zeige Status
echo ""
echo "System status:"
docker-compose ps

echo ""
echo "✓ System started!"
echo ""
echo "View logs with:"
echo "  - All services: docker-compose logs -f"
echo "  - Specific service: docker-compose logs -f marketplace1"
echo ""
echo "Stop system with: ./stop.sh"
