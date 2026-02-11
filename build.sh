#!/bin/bash

# Build script for LazyFormInstructor project
# This script builds both the library and the examples

set -e  # Exit on error

echo "========================================"
echo "LazyFormInstructor Build Script"
echo "========================================"
echo ""

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_step() {
    echo -e "${BLUE}==>${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed. Please install Maven first."
    exit 1
fi

print_success "Maven is installed"
echo ""

# Build the main library
print_step "Building LazyFormInstructor library..."
cd instructor

if mvn clean install -DskipTests; then
    print_success "LazyFormInstructor library built successfully"
else
    print_error "Failed to build LazyFormInstructor library"
    exit 1
fi

cd ..
echo ""

# Build the examples
print_step "Building LazyFormInstructor examples..."
cd example/cli-demo

if mvn clean compile; then
    print_success "LazyFormInstructor examples built successfully"
else
    print_error "Failed to build LazyFormInstructor examples"
    exit 1
fi

cd ..
echo ""

# Run tests
print_step "Running tests..."
cd instructor

if mvn test; then
    print_success "All tests passed"
else
    print_error "Some tests failed"
    exit 1
fi

cd ..
echo ""

echo "========================================"
print_success "Build completed successfully!"
echo "========================================"
echo ""
echo "You can now run the examples using ./run-demo.sh"
echo ""

