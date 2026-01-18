#!/bin/bash

# Quick test script to verify the backend serves frontend correctly

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Testing Backend Serves Frontend${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if dist folder exists
if [ ! -d "frontend/dist" ]; then
    echo -e "${RED}✗ frontend/dist not found. Building frontend...${NC}"
    cd frontend
    npm run build
    cd ..
    echo -e "${GREEN}✓ Frontend built${NC}"
else
    echo -e "${GREEN}✓ frontend/dist exists${NC}"
fi

# Check if backend is built
if [ ! -f "target/lazy-form-web-1.0-SNAPSHOT.jar" ]; then
    echo -e "${RED}✗ Backend JAR not found. Building...${NC}"
    mvn clean package -DskipTests
    echo -e "${GREEN}✓ Backend built${NC}"
else
    echo -e "${GREEN}✓ Backend JAR exists${NC}"
fi

# Check if files were copied to target
if [ ! -f "target/classes/static/index.html" ]; then
    echo -e "${RED}✗ Static files not copied. Rebuilding...${NC}"
    mvn package -DskipTests
fi

if [ -f "target/classes/static/index.html" ]; then
    echo -e "${GREEN}✓ Static files copied to target/classes/static/${NC}"
    echo "  Files:"
    ls -la target/classes/static/
else
    echo -e "${RED}✗ Static files not found in target${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}Starting application for testing...${NC}"
echo ""

# Kill any existing process on port 8008
lsof -ti:8008 | xargs kill -9 2>/dev/null || true

# Start the application
mvn spring-boot:run > /tmp/spring-boot-test.log 2>&1 &
SPRING_PID=$!

echo "Waiting for application to start (PID: $SPRING_PID)..."
sleep 12

if ! ps -p $SPRING_PID > /dev/null; then
    echo -e "${RED}✗ Application failed to start${NC}"
    cat /tmp/spring-boot-test.log | tail -50
    exit 1
fi

echo -e "${GREEN}✓ Application started${NC}"
echo ""

# Run tests
FAILED=0

echo "Test 1: Root path serves index.html"
if curl -s http://localhost:8008/ | grep -q "Lazy Form Instructor"; then
    echo -e "${GREEN}✓ PASS${NC}"
else
    echo -e "${RED}✗ FAIL${NC}"
    FAILED=$((FAILED + 1))
fi

echo "Test 2: SPA routing (unknown paths return index.html)"
if curl -s http://localhost:8008/unknown-page | grep -q "Lazy Form Instructor"; then
    echo -e "${GREEN}✓ PASS${NC}"
else
    echo -e "${RED}✗ FAIL${NC}"
    FAILED=$((FAILED + 1))
fi

echo "Test 3: API endpoint works"
if curl -s http://localhost:8008/api/forms/schema/leave | grep -q "schema"; then
    echo -e "${GREEN}✓ PASS${NC}"
else
    echo -e "${RED}✗ FAIL${NC}"
    FAILED=$((FAILED + 1))
fi

echo "Test 4: JavaScript assets are accessible"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8008/assets/index-ChyXKtb2.js)
if [ "$STATUS" = "200" ]; then
    echo -e "${GREEN}✓ PASS (HTTP $STATUS)${NC}"
else
    echo -e "${RED}✗ FAIL (HTTP $STATUS)${NC}"
    FAILED=$((FAILED + 1))
fi

echo "Test 5: CSS assets are accessible"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8008/assets/index-DLFNRjSR.css)
if [ "$STATUS" = "200" ]; then
    echo -e "${GREEN}✓ PASS (HTTP $STATUS)${NC}"
else
    echo -e "${RED}✗ FAIL (HTTP $STATUS)${NC}"
    FAILED=$((FAILED + 1))
fi

# Cleanup
echo ""
echo "Stopping application..."
kill $SPRING_PID 2>/dev/null
wait $SPRING_PID 2>/dev/null || true

echo ""
echo -e "${BLUE}========================================${NC}"
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    echo -e "${GREEN}Backend successfully serves frontend${NC}"
else
    echo -e "${RED}$FAILED test(s) failed${NC}"
    exit 1
fi
echo -e "${BLUE}========================================${NC}"

