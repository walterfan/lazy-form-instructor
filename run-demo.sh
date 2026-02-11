#!/bin/bash

# Demo runner script for LazyFormInstructor examples
# This script runs the demo programs

set -e  # Exit on error

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
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

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Function to display menu
show_menu() {
    clear
    print_header "LazyFormInstructor Demo Runner"
    echo "Please select a demo to run:"
    echo ""
    echo "  1) Leave Request Example"
    echo "     (Multi-day leave with dependencies)"
    echo ""
    echo "  2) Task Request Example"
    echo "     (Task creation with priority inference)"
    echo ""
    echo "  3) Run All Examples"
    echo ""
    echo "  4) Exit"
    echo ""
}

# Function to run leave request example
run_leave_request() {
    print_header "Leave Request Example"
    print_info "Running: LeaveRequestExample"
    echo ""
    
    cd example/cli-demo
    mvn exec:java -Dexec.mainClass="com.fanyamin.example.LeaveRequestExample" -q
    local exit_code=$?
    cd ..
    
    if [ $exit_code -ne 0 ]; then
        print_error "Leave Request Example failed"
        return $exit_code
    fi
    
    echo ""
    print_success "Example completed"
    echo ""
    if [ "${NON_INTERACTIVE}" != "true" ]; then
        read -p "Press Enter to continue..."
    fi
}

# Function to run task request example
run_task_request() {
    print_header "Task Request Example"
    print_info "Running: TaskRequestExample"
    echo ""

    cd example/cli-demo
    mvn exec:java -Dexec.mainClass="com.fanyamin.example.TaskRequestExample" -q
    local exit_code=$?
    cd ..
    
    if [ $exit_code -ne 0 ]; then
        print_error "Task Request Example failed"
        return $exit_code
    fi
    
    echo ""
    print_success "Example completed"
    echo ""
    if [ "${NON_INTERACTIVE}" != "true" ]; then
        read -p "Press Enter to continue..."
    fi
}

# Function to run all examples
run_all() {
    run_leave_request
    run_task_request
}

# Check if project is built
if [ ! -f "instructor/target/lazy-form-instructor-0.0.1-SNAPSHOT.jar" ]; then
    print_error "Project not built. Please run ./build.sh first."
    exit 1
fi

# Check for command-line arguments
if [ $# -gt 0 ]; then
    NON_INTERACTIVE=true
    case "$1" in
        all|--all)
            run_leave_request || exit 1
            run_task_request || exit 1
            print_success "All demos completed successfully!"
            exit 0
            ;;
        leave|--leave)
            run_leave_request
            exit $?
            ;;
        task|--task)
            run_task_request
            exit $?
            ;;
        *)
            echo "Usage: $0 [all|leave|task]"
            echo "  all   - Run all examples"
            echo "  leave - Run leave request example"
            echo "  task  - Run task request example"
            echo "  (no args) - Interactive menu"
            exit 1
            ;;
    esac
fi

# Main loop
while true; do
    show_menu
    read -p "Enter your choice [1-4]: " choice
    
    case $choice in
        1)
            run_leave_request
            ;;
        2)
            run_task_request
            ;;
        3)
            run_all
            ;;
        4)
            clear
            print_success "Goodbye!"
            echo ""
            exit 0
            ;;
        *)
            print_error "Invalid option. Please choose 1-4."
            sleep 2
            ;;
    esac
done

