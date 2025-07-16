#!/bin/bash

# Enhanced Distributed System Test Script
# This script demonstrates the improved reliability features

echo "🚀 Enhanced Distributed E-commerce System Test"
echo "============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create directories for logs and state
mkdir -p logs
mkdir -p saga-states

echo -e "${BLUE}📁 Created directories for logs and saga states${NC}"

# Function to start a seller with enhanced configuration
start_seller() {
    local seller_id=$1
    local port=$2
    
    echo -e "${BLUE}🏪 Starting Enhanced Seller: $seller_id on port $port${NC}"
    
    cd seller
    SELLER_ID=$seller_id MARKETPLACE_ENDPOINT=tcp://localhost:5555 \
    java -Dconfig.file=enhanced-config.properties \
         -jar target/seller.jar > ../logs/seller-${seller_id}.log 2>&1 &
    
    echo $! > ../logs/seller-${seller_id}.pid
    cd ..
    
    echo -e "${GREEN}✅ Seller $seller_id started (PID: $(cat logs/seller-${seller_id}.pid))${NC}"
}

# Function to start the marketplace with enhanced configuration
start_marketplace() {
    echo -e "${BLUE}🏬 Starting Enhanced Marketplace${NC}"
    
    cd marketplace
    java -Dconfig.file=enhanced-config.properties \
         -jar target/marketplace.jar > ../logs/marketplace.log 2>&1 &
    
    echo $! > ../logs/marketplace.pid
    cd ..
    
    echo -e "${GREEN}✅ Marketplace started (PID: $(cat logs/marketplace.pid))${NC}"
}

# Function to check if a process is running
check_process() {
    local pid_file=$1
    if [ -f "$pid_file" ]; then
        local pid=$(cat $pid_file)
        if kill -0 $pid 2>/dev/null; then
            return 0
        fi
    fi
    return 1
}

# Function to stop all processes
stop_all() {
    echo -e "${YELLOW}🛑 Stopping all processes...${NC}"
    
    # Stop sellers
    for i in {1..5}; do
        if check_process "logs/seller-seller${i}.pid"; then
            kill $(cat logs/seller-seller${i}.pid) 2>/dev/null
            echo -e "${YELLOW}🏪 Stopped seller${i}${NC}"
        fi
    done
    
    # Stop marketplace
    if check_process "logs/marketplace.pid"; then
        kill $(cat logs/marketplace.pid) 2>/dev/null
        echo -e "${YELLOW}🏬 Stopped marketplace${NC}"
    fi
    
    # Clean up pid files
    rm -f logs/*.pid
    
    echo -e "${GREEN}✅ All processes stopped${NC}"
}

# Function to show system status
show_status() {
    echo -e "${BLUE}📊 System Status${NC}"
    echo "================"
    
    # Check marketplace
    if check_process "logs/marketplace.pid"; then
        echo -e "${GREEN}✅ Marketplace: RUNNING${NC}"
    else
        echo -e "${RED}❌ Marketplace: STOPPED${NC}"
    fi
    
    # Check sellers
    for i in {1..5}; do
        if check_process "logs/seller-seller${i}.pid"; then
            echo -e "${GREEN}✅ Seller${i}: RUNNING${NC}"
        else
            echo -e "${RED}❌ Seller${i}: STOPPED${NC}"
        fi
    done
    
    echo ""
    echo -e "${BLUE}📈 Recent Activity:${NC}"
    echo "==================="
    
    # Show recent log entries
    if [ -f "logs/marketplace.log" ]; then
        echo -e "${BLUE}Marketplace (last 5 lines):${NC}"
        tail -n 5 logs/marketplace.log
        echo ""
    fi
    
    # Show saga states
    if [ -d "saga-states" ]; then
        local saga_count=$(ls -1 saga-states/*.json 2>/dev/null | wc -l)
        echo -e "${BLUE}Active Sagas: $saga_count${NC}"
        if [ $saga_count -gt 0 ]; then
            ls -la saga-states/
        fi
    fi
}

# Function to run stress test
run_stress_test() {
    echo -e "${YELLOW}🔥 Running Stress Test with Enhanced Features${NC}"
    echo "=============================================="
    
    # Start all services
    start_marketplace
    sleep 2
    
    for i in {1..5}; do
        start_seller "seller${i}" $((6000 + i))
        sleep 1
    done
    
    echo -e "${GREEN}✅ All services started${NC}"
    sleep 5
    
    # Monitor the system
    echo -e "${BLUE}🔍 Monitoring system for 60 seconds...${NC}"
    for i in {1..12}; do
        echo -e "${BLUE}--- Check $i/12 ---${NC}"
        show_status
        sleep 5
    done
    
    echo -e "${GREEN}✅ Stress test completed${NC}"
}

# Function to demonstrate failure recovery
demonstrate_failure_recovery() {
    echo -e "${YELLOW}🔧 Demonstrating Failure Recovery${NC}"
    echo "=================================="
    
    # Start marketplace
    start_marketplace
    sleep 2
    
    # Start sellers
    for i in {1..3}; do
        start_seller "seller${i}" $((6000 + i))
        sleep 1
    done
    
    echo -e "${GREEN}✅ System started with 3 sellers${NC}"
    sleep 5
    
    # Simulate seller failure
    echo -e "${RED}💥 Simulating seller1 failure...${NC}"
    if check_process "logs/seller1.pid"; then
        kill $(cat logs/seller1.pid)
        rm -f logs/seller1.pid
    fi
    
    echo -e "${BLUE}🔄 System running with 2 sellers for 30 seconds...${NC}"
    sleep 30
    
    # Restart failed seller
    echo -e "${GREEN}🔄 Restarting seller1...${NC}"
    start_seller "seller1" 6001
    
    echo -e "${BLUE}🔍 Monitoring recovery for 30 seconds...${NC}"
    for i in {1..6}; do
        echo -e "${BLUE}--- Recovery check $i/6 ---${NC}"
        show_status
        sleep 5
    done
    
    echo -e "${GREEN}✅ Failure recovery demonstration completed${NC}"
}

# Function to show enhanced features
show_enhanced_features() {
    echo -e "${BLUE}🚀 Enhanced Features Demonstration${NC}"
    echo "=================================="
    
    echo -e "${GREEN}✅ Implemented Features:${NC}"
    echo "• Fixed ZMQ ROUTER-DEALER messaging pattern"
    echo "• Added idempotency management for exactly-once processing"
    echo "• Implemented retry logic with exponential backoff"
    echo "• Added saga state persistence and recovery"
    echo "• Enhanced inventory with proper concurrency control"
    echo "• Implemented circuit breaker pattern"
    echo "• Added advanced failure simulation with realistic patterns"
    echo "• Improved error handling and compensation logic"
    echo ""
    
    echo -e "${GREEN}🔧 Configuration Features:${NC}"
    echo "• Cascading failure patterns"
    echo "• Periodic maintenance window simulation"
    echo "• Burst failure patterns"
    echo "• Recovery patterns with gradual improvement"
    echo "• Configurable timeouts and retry policies"
    echo "• Comprehensive monitoring and logging"
    echo ""
    
    echo -e "${GREEN}📊 Reliability Improvements:${NC}"
    echo "• Exactly-once message processing"
    echo "• Automatic retry with backoff"
    echo "• Circuit breaker prevents cascading failures"
    echo "• Saga state persistence enables recovery"
    echo "• Enhanced inventory prevents race conditions"
    echo "• Sophisticated failure simulation for testing"
    echo ""
}

# Main script logic
case "$1" in
    "start")
        echo -e "${BLUE}🚀 Starting Enhanced System${NC}"
        start_marketplace
        sleep 2
        for i in {1..5}; do
            start_seller "seller${i}" $((6000 + i))
            sleep 1
        done
        echo -e "${GREEN}✅ All services started${NC}"
        ;;
    "stop")
        stop_all
        ;;
    "status")
        show_status
        ;;
    "stress")
        run_stress_test
        ;;
    "recovery")
        demonstrate_failure_recovery
        ;;
    "features")
        show_enhanced_features
        ;;
    "clean")
        echo -e "${YELLOW}🧹 Cleaning up logs and state${NC}"
        stop_all
        rm -rf logs/*
        rm -rf saga-states/*
        echo -e "${GREEN}✅ Cleanup completed${NC}"
        ;;
    *)
        echo "Enhanced Distributed System Test Script"
        echo "Usage: $0 {start|stop|status|stress|recovery|features|clean}"
        echo ""
        echo "Commands:"
        echo "  start     - Start all services with enhanced configuration"
        echo "  stop      - Stop all services"
        echo "  status    - Show system status and recent activity"
        echo "  stress    - Run stress test with monitoring"
        echo "  recovery  - Demonstrate failure recovery capabilities"
        echo "  features  - Show enhanced features list"
        echo "  clean     - Clean up logs and state files"
        echo ""
        echo "Examples:"
        echo "  $0 start    # Start the enhanced system"
        echo "  $0 stress   # Run comprehensive stress test"
        echo "  $0 recovery # Demonstrate failure recovery"
        echo "  $0 features # Show enhanced features"
        ;;
esac