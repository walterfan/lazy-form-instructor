#!/bin/bash

# Startup script for Lazy Form Workflow Web Application

set -e

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_info() {
    echo -e "${YELLOW}ℹ${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

cd "$(dirname "$0")"

print_header "Lazy Form Workflow Web Application Startup"

# Check if lazy-form-workflow is built
if [ ! -f "../lazy-form-workflow/target/lazy-form-workflow-1.0-SNAPSHOT.jar" ]; then
    print_info "Building lazy-form-workflow library..."
    cd ../lazy-form-workflow
    mvn clean install -DskipTests
    cd ../lazy-form-workflow-web
    print_success "Workflow library built"
fi

# Check if lazy-form-instructor is built
if [ ! -f "../lazy-form-instructor/target/lazy-form-instructor-0.0.1-SNAPSHOT.jar" ]; then
    print_info "Building lazy-form-instructor library..."
    cd ../lazy-form-instructor
    mvn clean install -DskipTests
    cd ../lazy-form-workflow-web
    print_success "Instructor library built"
fi

# Build backend
print_info "Building backend..."
mvn clean package -DskipTests
print_success "Backend built"

# Check if we should run in production mode or development mode
if [ "$1" == "prod" ]; then
    print_header "Production Mode"
    
    # Check if node_modules exists
    if [ ! -d "frontend/node_modules" ]; then
        print_info "Installing frontend dependencies..."
        cd frontend
        npm install
        cd ..
        print_success "Dependencies installed"
    fi
    
    # Build frontend
    print_info "Building frontend..."
    cd frontend
    npm run build
    cd ..
    print_success "Frontend built"
    
    # Copy to static resources
    print_info "Copying frontend to backend..."
    mkdir -p src/main/resources/static
    cp -r frontend/dist/* src/main/resources/static/
    print_success "Frontend copied"
    
    # Rebuild backend with frontend
    print_info "Rebuilding backend with frontend..."
    mvn package -DskipTests
    print_success "Backend rebuilt"
    
    print_header "Starting Application"
    print_info "Backend + Frontend will run at http://localhost:8009"
    echo ""
    
    mvn spring-boot:run
else
    print_header "Development Mode"
    print_info "Backend will run at http://localhost:8009"
    print_info "Frontend will run at http://0.0.0.0:5174"
    print_info "Access frontend from your browser at http://<your-host>:5174"
    echo ""
    print_info "Starting backend..."
    echo ""
    
    # Start backend in background
    mvn spring-boot:run &
    BACKEND_PID=$!
    
    # Wait for backend to start
    sleep 5
    
    # Check if frontend dependencies are installed
    if [ ! -d "frontend/node_modules" ]; then
        print_info "Installing frontend dependencies..."
        cd frontend
        npm install
        cd ..
        print_success "Dependencies installed"
    fi
    
    print_info "Starting frontend..."
    echo ""
    cd frontend
    npm run dev &
    FRONTEND_PID=$!
    cd ..
    
    print_success "Application started!"
    echo ""
    print_info "Press Ctrl+C to stop both servers"
    
    # Trap Ctrl+C to stop both processes
    trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT TERM
    
    # Wait for both processes
    wait
fi

