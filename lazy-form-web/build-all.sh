#!/bin/bash

# Complete build script for the entire lazy-form-web application
# Handles frontend build, backend build, and verification

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_step() {
    echo -e "${YELLOW}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

print_header "Lazy Form Web - Complete Build"

# Parse command line arguments
CLEAN_BUILD=false
SKIP_TESTS=true
SKIP_FRONTEND=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --clean|-c)
            CLEAN_BUILD=true
            shift
            ;;
        --with-tests|-t)
            SKIP_TESTS=false
            shift
            ;;
        --skip-frontend|-s)
            SKIP_FRONTEND=true
            shift
            ;;
        --help|-h)
            echo "Usage: ./build-all.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --clean, -c           Clean build (remove node_modules and target)"
            echo "  --with-tests, -t      Run tests (default: skip tests)"
            echo "  --skip-frontend, -s   Skip frontend build (use existing dist/)"
            echo "  --help, -h            Show this help message"
            echo ""
            echo "Examples:"
            echo "  ./build-all.sh                    # Normal build"
            echo "  ./build-all.sh --clean            # Clean build"
            echo "  ./build-all.sh --with-tests       # Build with tests"
            echo "  ./build-all.sh --skip-frontend    # Only build backend"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Step 1: Check prerequisites
print_header "Step 1: Checking Prerequisites"

# Check Node.js
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    print_success "Node.js installed: $NODE_VERSION"
else
    print_error "Node.js not found. Please install Node.js 18+"
    exit 1
fi

# Check npm
if command -v npm &> /dev/null; then
    NPM_VERSION=$(npm --version)
    print_success "npm installed: $NPM_VERSION"
else
    print_error "npm not found. Please install npm"
    exit 1
fi

# Check Maven
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn --version | head -1)
    print_success "Maven installed: $MVN_VERSION"
else
    print_error "Maven not found. Please install Maven 3.6+"
    exit 1
fi

# Check Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1)
    print_success "Java installed: $JAVA_VERSION"
else
    print_error "Java not found. Please install Java 17+"
    exit 1
fi

# Step 2: Build Frontend
if [ "$SKIP_FRONTEND" = false ]; then
    print_header "Step 2: Building Frontend"
    
    cd frontend
    
    if [ "$CLEAN_BUILD" = true ]; then
        print_step "Cleaning frontend..."
        rm -rf node_modules package-lock.json dist
        print_success "Frontend cleaned"
    fi
    
    # Check if node_modules exists
    if [ ! -d "node_modules" ]; then
        print_step "Installing frontend dependencies..."
        npm install
        print_success "Dependencies installed"
    else
        print_success "Using existing node_modules"
    fi
    
    # Check vue-tsc version
    VUE_TSC_VERSION=$(npm list vue-tsc --depth=0 2>/dev/null | grep vue-tsc | grep -oP '\d+\.\d+\.\d+' || echo "unknown")
    if [[ "$VUE_TSC_VERSION" == 2.* ]]; then
        print_success "vue-tsc version: $VUE_TSC_VERSION"
    else
        print_error "vue-tsc version is not 2.x: $VUE_TSC_VERSION"
        print_step "Fixing vue-tsc version..."
        rm -rf node_modules package-lock.json
        npm install
        print_success "Dependencies reinstalled"
    fi
    
    # Build frontend
    print_step "Building frontend..."
    if npm run build; then
        print_success "Frontend build completed"
        
        # Verify dist folder
        if [ -d "dist" ] && [ -f "dist/index.html" ]; then
            DIST_SIZE=$(du -sh dist | cut -f1)
            print_success "dist/ folder created (size: $DIST_SIZE)"
            echo "  Files:"
            ls -lh dist/ | tail -n +2 | awk '{print "    " $9 " (" $5 ")"}'
        else
            print_error "dist/ folder not created properly"
            exit 1
        fi
    else
        print_error "Frontend build failed"
        exit 1
    fi
    
    cd ..
else
    print_header "Step 2: Skipping Frontend Build"
    
    # Check if dist exists
    if [ ! -d "frontend/dist" ]; then
        print_error "frontend/dist not found. Cannot skip frontend build."
        print_step "Run without --skip-frontend flag"
        exit 1
    fi
    print_success "Using existing frontend/dist/"
fi

# Step 3: Build lazy-form-instructor library
print_header "Step 3: Building lazy-form-instructor Library"

if [ ! -f "../lazy-form-instructor/target/lazy-form-instructor-0.0.1-SNAPSHOT.jar" ]; then
    print_step "Building lazy-form-instructor library..."
    cd ../lazy-form-instructor
    
    if [ "$SKIP_TESTS" = true ]; then
        mvn clean install -DskipTests
    else
        mvn clean install
    fi
    
    cd ../lazy-form-web
    print_success "Library built"
else
    print_success "Using existing lazy-form-instructor library"
fi

# Step 4: Build Backend
print_header "Step 4: Building Backend"

if [ "$CLEAN_BUILD" = true ]; then
    print_step "Cleaning backend..."
    mvn clean
    print_success "Backend cleaned"
fi

print_step "Building backend (includes copying frontend)..."

if [ "$SKIP_TESTS" = true ]; then
    mvn package -DskipTests
else
    mvn package
fi

print_success "Backend build completed"

# Step 5: Verification
print_header "Step 5: Verification"

# Check if JAR was created
if [ -f "target/lazy-form-web-1.0-SNAPSHOT.jar" ]; then
    JAR_SIZE=$(du -sh target/lazy-form-web-1.0-SNAPSHOT.jar | cut -f1)
    print_success "JAR created: lazy-form-web-1.0-SNAPSHOT.jar ($JAR_SIZE)"
else
    print_error "JAR file not created"
    exit 1
fi

# Check if frontend files were copied
if [ -f "target/classes/static/index.html" ]; then
    print_success "Frontend files copied to target/classes/static/"
    echo "  Files:"
    ls -lh target/classes/static/ | grep -v ^d | tail -n +2 | awk '{print "    " $9 " (" $5 ")"}'
    if [ -d "target/classes/static/assets" ]; then
        ASSET_COUNT=$(ls target/classes/static/assets/ | wc -l)
        echo "    assets/ ($ASSET_COUNT files)"
    fi
else
    print_error "Frontend files not copied properly"
    exit 1
fi

# Final summary
print_header "Build Complete!"

echo -e "${GREEN}✓ Frontend built successfully${NC}"
echo -e "${GREEN}✓ Backend built successfully${NC}"
echo -e "${GREEN}✓ Application ready to deploy${NC}"
echo ""
echo "Output:"
echo "  JAR file: target/lazy-form-web-1.0-SNAPSHOT.jar"
echo "  Size: $(du -sh target/lazy-form-web-1.0-SNAPSHOT.jar | cut -f1)"
echo ""
echo "To run the application:"
echo ""
echo -e "  ${BLUE}# Using Maven${NC}"
echo "  mvn spring-boot:run"
echo ""
echo -e "  ${BLUE}# Using JAR${NC}"
echo "  java -jar target/lazy-form-web-1.0-SNAPSHOT.jar"
echo ""
echo -e "  ${BLUE}# Using start script${NC}"
echo "  ./start.sh prod"
echo ""
echo "Access the application at: ${BLUE}http://localhost:8008${NC}"
echo ""

