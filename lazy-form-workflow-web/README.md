# Smart Form Workflow Web

Web-based demonstration of the Smart Form Workflow Engine - AI-powered business process automation.

## Overview

This application showcases the `smart-form-workflow` engine capabilities through an interactive web interface. It demonstrates how natural language input can be parsed and executed through complex AI-driven workflows.

## Features

- **Interactive Web UI**: Vue.js-based interface for workflow execution
- **Real-time Execution Trace**: Visual timeline of workflow node execution
- **AI Decision Visualization**: See AI reasoning and confidence scores
- **Multiple Workflow Types**: Support for different business processes
- **Field Parsing Display**: Shows parsed form fields with confidence metrics
- **Beautiful UI**: Modern, gradient-based design with smooth animations

## Architecture

```
┌─────────────────────────────────────┐
│   Browser (http://localhost:8009)  │
└─────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│     Spring Boot Backend (8009)      │
│  ┌───────────────────────────────┐  │
│  │  Static Resources (/)         │  │  ← Vue.js SPA
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │  REST API (/api/workflow)     │  │  ← Workflow execution
│  │  - POST /execute              │  │
│  │  - GET /types                 │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│    Workflow Engine + LLM            │
│  - Parse natural language input     │
│  - Execute workflow graph (DAG)     │
│  - AI-powered decision nodes        │
│  - Deterministic action nodes       │
└─────────────────────────────────────┘
```

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Node.js 18+
- OpenAI API key (or compatible LLM endpoint)

### Environment Setup

Create a `.env` file or set environment variables:

```bash
OPENAI_API_KEY=your-api-key-here
OPENAI_MODEL=gpt-4
OPENAI_BASE_URL=https://api.openai.com/v1
```

### Development Mode (Hot Reload)

```bash
./start.sh
```

- Frontend: `http://localhost:5174` (Vite dev server)
- Backend API: `http://localhost:8009/api`

### Production Mode (Single Server)

```bash
./start.sh prod
```

- Application: `http://localhost:8009`

## Project Structure

```
smart-form-workflow-web/
├── frontend/                    # Vue.js frontend
│   ├── src/
│   │   ├── components/
│   │   │   └── ExecutionTrace.vue  # Workflow execution timeline
│   │   ├── App.vue             # Main application component
│   │   ├── api.ts              # API client
│   │   ├── types.ts            # TypeScript definitions
│   │   └── main.ts             # Application entry point
│   ├── package.json
│   └── vite.config.ts
│
├── src/main/
│   ├── java/com/fanyamin/workflow/web/
│   │   ├── controller/
│   │   │   └── WorkflowController.java    # REST API endpoints
│   │   ├── service/
│   │   │   └── WorkflowService.java       # Workflow business logic
│   │   ├── config/
│   │   │   ├── WebConfig.java             # Web configuration
│   │   │   └── WorkflowConfig.java        # Workflow beans
│   │   ├── dto/                           # Data transfer objects
│   │   └── WorkflowWebApplication.java
│   └── resources/
│       ├── application.properties
│       └── static/                        # Frontend build output
│
├── pom.xml
├── start.sh
└── README.md
```

## Available Workflows

### Leave Request Workflow

**Type**: `leave_request`

**Description**: AI-powered leave request validation and approval

**Example Input**:
```
I need annual leave from December 20 to December 25 for my wedding.
```

**Workflow Steps**:
1. **Parse Request**: Extract leave type, dates, and reason using SmartFormInstructor
2. **AI Validation**: Evaluate if reason is valid and reasonable
3. **Decision Routing**:
   - High confidence approve → Auto-approve
   - High confidence reject → Auto-reject
   - Low confidence or uncertain → Escalate to manager
4. **End**: Final state (APPROVED, REJECTED, or PENDING_APPROVAL)

**Workflow Graph**:
```
┌─────────┐
│  Start  │
└────┬────┘
     │
     ▼
┌──────────────┐
│ AI Validation│
└──────┬───────┘
       │
       ├─────── High Confidence Approve ──► Auto Approve ──┐
       │                                                    │
       ├─────── High Confidence Reject ───► Auto Reject ───┤
       │                                                    │
       └─────── Low Confidence/Escalate ─► Human Review ───┤
                                                            │
                                                            ▼
                                                         ┌─────┐
                                                         │ End │
                                                         └─────┘
```

## API Endpoints

### Execute Workflow

```http
POST /api/workflow/execute
Content-Type: application/json

{
  "userInput": "I need sick leave for 3 days starting tomorrow",
  "workflowType": "leave_request"
}
```

**Response**:
```json
{
  "status": "SUCCESS",
  "message": "Workflow completed successfully",
  "parsedFields": {
    "leave_type": {
      "value": "sick",
      "confidence": 0.95,
      "reasoning": "User explicitly mentioned sick leave"
    },
    "start_date": {
      "value": "2023-12-13",
      "confidence": 0.98,
      "reasoning": "Tomorrow relative to current date"
    }
  },
  "executionTrace": [
    {
      "timestamp": "2023-12-12T10:00:00Z",
      "nodeId": "start",
      "nodeType": "StartNode",
      "status": "SUCCESS"
    },
    {
      "timestamp": "2023-12-12T10:00:02Z",
      "nodeId": "ai_validation",
      "nodeType": "AiDecisionNode",
      "outputPayload": {
        "decision": "APPROVE",
        "reasoning": "Valid medical reason",
        "confidence": 0.92
      },
      "status": "SUCCESS"
    }
  ],
  "finalContext": {
    "state": "APPROVED",
    "variables": {
      "user_id": "emp_123",
      "manager_id": "mgr_456"
    }
  }
}
```

### Get Workflow Types

```http
GET /api/workflow/types
```

**Response**:
```json
{
  "leave_request": {
    "name": "Leave Request Workflow",
    "description": "AI-powered leave request validation and approval workflow",
    "exampleInput": "I need annual leave from Dec 20 to Dec 25 for vacation"
  }
}
```

## Building from Source

### Full Build

```bash
# Build all dependencies
cd ../smart-form-instructor
mvn clean install -DskipTests

cd ../smart-form-workflow
mvn clean install -DskipTests

# Build this application
cd ../smart-form-workflow-web

# Build frontend
cd frontend
npm install
npm run build

# Build backend (includes copying frontend)
cd ..
mvn clean package -DskipTests
```

### Run JAR

```bash
java -jar target/smart-form-workflow-web-1.0-SNAPSHOT.jar
```

## Development

### Backend Development

```bash
mvn spring-boot:run
```

- Runs on port 8009
- Spring Boot DevTools enabled for hot reload
- API accessible at `/api/workflow/*`

### Frontend Development

```bash
cd frontend
npm install
npm run dev
```

- Runs on port 5174
- Hot Module Replacement (HMR) enabled
- Proxies API requests to backend on port 8009

### Adding New Workflows

1. **Create workflow builder method** in `WorkflowService`:
   ```java
   private WorkflowGraph buildMyWorkflow() {
       // Define nodes and edges
   }
   ```

2. **Add execution method**:
   ```java
   public WorkflowResult executeMyWorkflow(String userInput) {
       // Parse input and execute workflow
   }
   ```

3. **Register in controller**:
   ```java
   case "my_workflow":
       result = workflowService.executeMyWorkflow(request.getUserInput());
       break;
   ```

4. **Add workflow type info** in `getWorkflowTypes()`:
   ```java
   types.put("my_workflow", new WorkflowTypeInfo(
       "My Workflow",
       "Description",
       "Example input"
   ));
   ```

## Configuration

### application.properties

```properties
# Server port
server.port=8009

# Logging
logging.level.com.fanyamin=INFO

# Static resources
spring.web.resources.static-locations=classpath:/static/
```

### LLM Configuration

The application uses environment variables for LLM configuration:

- `OPENAI_API_KEY`: Your API key
- `OPENAI_MODEL`: Model to use (default: gpt-4)
- `OPENAI_BASE_URL`: API endpoint
- `DISABLE_SSL_VERIFICATION`: Set to `true` for self-signed certificates

## UI Features

### Workflow Execution View

- **Workflow Type Selector**: Choose from available workflows
- **Input Area**: Enter natural language request
- **Execute Button**: Trigger workflow execution
- **Status Badge**: Visual indicator of workflow result
- **Parsed Fields**: Shows extracted data with confidence bars
- **Execution Trace**: Timeline visualization of each node
- **Final Context**: Shows workflow end state and variables

### Execution Trace Timeline

Each workflow node execution is displayed with:
- Sequential numbering
- Node ID and type
- Status indicator (success/failure/waiting)
- Output payload (including AI reasoning)
- Timestamp

Color coding:
- 🟢 Green: Success
- 🔴 Red: Failure
- 🟡 Yellow: Waiting

## Troubleshooting

### Frontend Build Errors

If you encounter `vue-tsc` errors:

```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run build
```

### Backend Startup Errors

**Port already in use**:
```bash
lsof -ti:8009 | xargs kill -9
```

**Missing dependencies**:
```bash
# Rebuild workflow libraries
cd ../smart-form-workflow
mvn clean install -DskipTests

cd ../smart-form-instructor
mvn clean install -DskipTests
```

### LLM Connection Issues

- Verify `OPENAI_API_KEY` is set
- Check API endpoint is accessible
- For self-signed certificates, set `DISABLE_SSL_VERIFICATION=true`

## Comparison with smart-form-web

| Feature | smart-form-web | smart-form-workflow-web |
|---------|----------------|-------------------------|
| Purpose | Form parsing only | Complete workflow execution |
| Output | Parsed fields | Parsed fields + execution trace |
| Visualization | Field results | Timeline + decision flow |
| Backend Logic | Simple parsing | Complex DAG execution |
| Port | 8008 | 8009 |
| Use Case | Form filling | Process automation |

## Future Enhancements

- [ ] Multiple workflow examples (expense approval, task routing)
- [ ] Workflow graph visualization (D3.js/Mermaid)
- [ ] Pause/resume workflow support
- [ ] Human task nodes with UI interaction
- [ ] Workflow history and persistence
- [ ] Real-time execution updates (WebSocket)
- [ ] Workflow designer UI
- [ ] Export execution reports

## Related Projects

- `smart-form-instructor`: Core form parsing library
- `smart-form-workflow`: Workflow engine library
- `smart-form-web`: Simple form parsing demo
- `smart-form-workflow-example`: Command-line workflow example

## License

See parent project for license information.

## Contributing

Contributions are welcome! Please see the main project README for contribution guidelines.

