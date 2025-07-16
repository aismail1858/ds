#!/bin/bash
# logs.sh - View logs for the distributed marketplace system

# Check if service name is provided
if [ $# -eq 0 ]; then
    echo "Showing logs for all services..."
    docker-compose logs -f
else
    echo "Showing logs for: $@"
    docker-compose logs -f $@
fi
