#!/bin/bash

# Lazy Form Workflow Example Runner
# Demonstrates AI-driven workflow engine with leave request processing

set -e

echo "========================================================================"
echo "Lazy Form Workflow Engine - Leave Request Example"
echo "========================================================================"
echo ""

# Check if we're in the right directory
if [ ! -d "lazy-form-workflow" ]; then
    echo "Error: Please run this script from the project root directory"
    exit 1
fi

# Check for environment variables
if [ -z "$LLM_API_KEY" ] && [ ! -f ".env" ]; then
    echo "Warning: No LLM_API_KEY found in environment or .env file"
    echo "The example will fail without valid LLM configuration."
    echo ""
    echo "Please set environment variables:"
    echo "  export LLM_API_KEY=your-api-key"
    echo "  export LLM_BASE_URL=https://api.openai.com/v1"
    echo "  export LLM_MODEL=gpt-4"
    echo ""
    echo "Or create a .env file with these values."
    echo ""
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo "Building lazy-form-workflow library..."
cd lazy-form-workflow
mvn clean install -DskipTests -q
cd ..

echo "Building lazy-form-workflow-example..."
cd lazy-form-workflow-example
mvn clean compile -q

echo ""
echo "========================================================================"
echo "Running Leave Request Workflow Example"
echo "========================================================================"
echo ""

mvn exec:java -Dexec.mainClass="com.fanyamin.workflow.example.LeaveRequestWorkflowExample"

echo ""
echo "========================================================================"
echo "Example completed!"
echo "========================================================================"



