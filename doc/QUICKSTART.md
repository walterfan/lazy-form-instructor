# Quick Start Guide - Smart Form Workflow Web

## 1-Minute Quick Start

```bash
# Set your OpenAI API key
export OPENAI_API_KEY=your-key-here

# Start the application
cd smart-form-workflow-web
./start.sh

# Access at http://localhost:5174 (development)
# or
./start.sh prod  # Access at http://localhost:8009 (production)
```

## What You'll See

1. **Workflow Type Selector**: Choose "Leave Request Workflow"
2. **Input Area**: Pre-filled with example text
3. **Execute Button**: Click to run the workflow
4. **Results Display**:
   - Status badge (SUCCESS/FAILED/WAITING)
   - Parsed fields with confidence scores
   - Execution trace timeline
   - Final workflow state

## Example Workflow

**Input:**
```
I need annual leave from December 20 to December 25 for my wedding.
```

**What Happens:**
1. **Parsing**: Extracts leave_type, start_date, end_date, reason
2. **AI Validation**: LLM evaluates if request is valid
3. **Decision**: Routes to approve/reject/review based on AI confidence
4. **Result**: Shows final approval state

## Workflow Execution Flow

```
User Input
    ↓
Parse with AI → Extract structured data
    ↓
AI Validation → Evaluate request validity
    ↓
Decision Logic → Confidence-based routing
    ↓
Final Action → Approve/Reject/Review
    ↓
Result Display → Show execution trace
```

## Try Different Inputs

**Valid Request (likely auto-approved):**
```
I need 3 days sick leave starting tomorrow for medical treatment
```

**Questionable Request (likely escalated):**
```
I need 2 weeks vacation starting Monday because I feel like it
```

**Invalid Request (likely rejected):**
```
I need leave from yesterday to last week for no reason
```

## Understanding the UI

### Execution Trace Timeline

Each node shows:
- **Number**: Execution order
- **Node ID**: Unique identifier (e.g., "start", "ai_validation")
- **Node Type**: Type of node (StartNode, AiDecisionNode, ActionNode)
- **Status**: SUCCESS ✅ / FAILURE ❌ / WAITING ⏸️
- **Output**: Node result (especially AI reasoning)
- **Timestamp**: When node executed

### AI Decision Output

When the AI validates a request, you'll see:
- **Decision**: APPROVE / REJECT / ESCALATE
- **Reasoning**: Why the AI made this decision
- **Confidence**: How confident the AI is (0-100%)

### Confidence Bars

Color indicates confidence level:
- 🟢 Green (≥90%): High confidence
- 🟡 Yellow (70-89%): Medium confidence
- 🔴 Red (<70%): Low confidence

## Development vs Production Mode

### Development Mode (`./start.sh`)
- **Frontend**: Port 5174 (Vite dev server)
- **Backend**: Port 8009 (Spring Boot)
- **Hot Reload**: Changes reflect immediately
- **Use For**: Development and testing

### Production Mode (`./start.sh prod`)
- **Single Server**: Port 8009
- **Optimized**: Minified and bundled
- **Single JAR**: Everything in one file
- **Use For**: Deployment and demos

## Troubleshooting

### "Cannot connect to backend"
- Ensure backend is running on port 8009
- Check `OPENAI_API_KEY` is set
- View backend logs for errors

### "Workflow execution failed"
- Verify API key has credits
- Check network connectivity to OpenAI
- Look at error message for details

### Frontend not loading
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run build
```

### Backend won't start
```bash
# Kill any process on port 8009
lsof -ti:8009 | xargs kill -9

# Rebuild dependencies
cd ../smart-form-workflow
mvn clean install -DskipTests

cd ../smart-form-instructor
mvn clean install -DskipTests
```

## Next Steps

1. **Explore the Code**:
   - Backend: `src/main/java/com/fanyamin/workflow/web/`
   - Frontend: `frontend/src/`

2. **Add a New Workflow**:
   - Follow the guide in README.md
   - Copy the leave request workflow as template

3. **Customize the UI**:
   - Edit `frontend/src/App.vue`
   - Modify styles and layout

4. **Deploy**:
   - Build with `./start.sh prod`
   - Deploy `target/smart-form-workflow-web-1.0-SNAPSHOT.jar`

## Architecture Overview

```
┌──────────────────┐
│   Vue.js UI      │  User interacts with form
│   (Frontend)     │  Enters natural language
└────────┬─────────┘
         │
         ↓ HTTP POST /api/workflow/execute
┌──────────────────┐
│ Spring Boot API  │  Receives request
│ (Backend)        │  Coordinates execution
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│ SmartFormInstr   │  Parses natural language
│ (Parser)         │  Extracts structured data
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│ Workflow Engine  │  Executes DAG
│ (Orchestrator)   │  Runs AI + logic nodes
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│    OpenAI LLM    │  Makes decisions
│   (AI Brain)     │  Validates requests
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│   Result + Trace │  Returns to frontend
│   (Response)     │  Shows execution flow
└──────────────────┘
```

## Key Concepts

### Workflow Graph (DAG)
- Directed Acyclic Graph
- Nodes: Actions to perform
- Edges: Connections with conditions
- No cycles allowed (prevents infinite loops)

### Node Types
- **StartNode**: Entry point
- **AiDecisionNode**: Uses LLM to decide
- **LogicDecisionNode**: Code-based decision
- **ActionNode**: Performs operation
- **EndNode**: Terminal point

### Workflow Context
- Shared data structure
- Passes between nodes
- Contains parsed fields
- Stores state and variables

### Execution Trace
- Log of each node execution
- Includes timing and output
- Used for debugging
- Displayed in UI timeline

## Tips

1. **Start Simple**: Use the default example first
2. **Watch the Trace**: Understanding the flow helps debug
3. **Check Confidence**: Low confidence often means unclear input
4. **Iterate**: Refine prompts based on results
5. **Read Logs**: Backend logs show detailed information

## Support

For issues or questions:
1. Check README.md for detailed documentation
2. Review backend logs: `mvn spring-boot:run`
3. Check frontend console: Browser DevTools
4. Examine `application.properties` for configuration

## Success Checklist

- ✅ OpenAI API key is set
- ✅ Dependencies are built (workflow + instructor libraries)
- ✅ Backend starts without errors
- ✅ Frontend loads in browser
- ✅ Can select workflow type
- ✅ Can execute workflow successfully
- ✅ See execution trace in UI
- ✅ Results show parsed fields and AI reasoning

**You're ready to go!** 🚀

