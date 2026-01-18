#!/bin/bash

# Quick fix script for vue-tsc error on server
# Run this on the server where npm run build is failing

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}Vue-tsc Error Fix Script${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Check if we're in the frontend directory
if [ ! -f "package.json" ]; then
    echo -e "${RED}Error: package.json not found${NC}"
    echo "Please run this script from the frontend directory:"
    echo "  cd lazy-form-web/frontend"
    echo "  ./fix-vue-tsc.sh"
    exit 1
fi

echo -e "${YELLOW}Step 1: Checking current configuration...${NC}"

# Check current vue-tsc version in package.json
if grep -q '"vue-tsc": "^1.8' package.json; then
    echo -e "${YELLOW}⚠ Found old vue-tsc version (1.8.x) in package.json${NC}"
    echo "Updating to vue-tsc ^2.0.0..."
    
    # Update package.json
    sed -i 's/"vue-tsc": "^1\.8\.[0-9]*"/"vue-tsc": "^2.0.0"/' package.json
    echo -e "${GREEN}✓ package.json updated${NC}"
elif grep -q '"vue-tsc": "^2.0' package.json; then
    echo -e "${GREEN}✓ package.json already has vue-tsc ^2.0.0${NC}"
else
    echo -e "${RED}✗ Unexpected vue-tsc version in package.json${NC}"
    echo "Please check package.json manually"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 2: Cleaning old dependencies...${NC}"

# Remove old node_modules and package-lock.json
if [ -d "node_modules" ]; then
    echo "Removing node_modules..."
    rm -rf node_modules
    echo -e "${GREEN}✓ node_modules removed${NC}"
fi

if [ -f "package-lock.json" ]; then
    echo "Removing package-lock.json..."
    rm -f package-lock.json
    echo -e "${GREEN}✓ package-lock.json removed${NC}"
fi

echo ""
echo -e "${YELLOW}Step 3: Installing dependencies...${NC}"

# Install dependencies
npm install

echo -e "${GREEN}✓ Dependencies installed${NC}"

echo ""
echo -e "${YELLOW}Step 4: Verifying vue-tsc version...${NC}"

# Check installed version
VUE_TSC_VERSION=$(npm list vue-tsc --depth=0 2>/dev/null | grep vue-tsc | grep -oP '\d+\.\d+\.\d+' || echo "unknown")
echo "Installed vue-tsc version: $VUE_TSC_VERSION"

if [[ "$VUE_TSC_VERSION" == 2.* ]]; then
    echo -e "${GREEN}✓ vue-tsc 2.x installed successfully${NC}"
else
    echo -e "${RED}✗ Unexpected vue-tsc version: $VUE_TSC_VERSION${NC}"
    echo "Expected version 2.x"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 5: Testing build...${NC}"

# Try to build
if npm run build; then
    echo ""
    echo -e "${GREEN}======================================${NC}"
    echo -e "${GREEN}✓ SUCCESS! Build completed${NC}"
    echo -e "${GREEN}======================================${NC}"
    echo ""
    echo "Output directory: dist/"
    echo ""
    echo "You can now run the backend build:"
    echo "  cd .."
    echo "  mvn clean package -DskipTests"
else
    echo ""
    echo -e "${RED}======================================${NC}"
    echo -e "${RED}✗ Build failed${NC}"
    echo -e "${RED}======================================${NC}"
    echo ""
    echo "Please check the error messages above."
    echo "You may need to:"
    echo "  1. Check Node.js version: node --version (should be 18+)"
    echo "  2. Clear npm cache: npm cache clean --force"
    echo "  3. Check for TypeScript errors in the code"
    exit 1
fi

echo "Next steps:"
echo "  1. Build the backend: cd .. && mvn clean package"
echo "  2. Run the application: ./start.sh prod"

