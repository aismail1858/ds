#!/bin/bash
# test.sh - Test script with different scenarios

echo "========================================="
echo "Testing Distributed Marketplace System"
echo "========================================="

# Function to modify config
modify_config() {
    local service=$1
    local property=$2
    local value=$3
    
    if [ "$service" == "marketplace" ]; then
        sed -i.bak "s/^${property}=.*/${property}=${value}/" marketplace/config.properties
    else
        sed -i.bak "s/^${property}=.*/${property}=${value}/" seller/config.properties
    fi
}

# Test 1: Normal operation
echo ""
echo "Test 1: Normal operation (low failure rates)"
modify_config "seller" "failure.no_response.probability" "0.01"
modify_config "seller" "failure.processing.probability" "0.02"
modify_config "seller" "failure.out_of_stock.probability" "0.05"

./run.sh
echo "Running normal operation test for 30 seconds..."
sleep 30

# Test 2: High failure rate
echo ""
echo "Test 2: High failure rate scenario"
modify_config "seller" "failure.no_response.probability" "0.20"
modify_config "seller" "failure.processing.probability" "0.25"
modify_config "seller" "failure.out_of_stock.probability" "0.30"

docker-compose down
./run.sh
echo "Running high failure test for 30 seconds..."
sleep 30

# Test 3: High load
echo ""
echo "Test 3: High load scenario"
modify_config "marketplace" "order.delay.ms" "500"
modify_config "seller" "failure.no_response.probability" "0.05"

docker-compose down
./run.sh
echo "Running high load test for 30 seconds..."
sleep 30

# Restore original configs
echo ""
echo "Restoring original configurations..."
mv marketplace/config.properties.bak marketplace/config.properties
mv seller/config.properties.bak seller/config.properties

./stop.sh

echo ""
echo "✓ Tests completed!"
