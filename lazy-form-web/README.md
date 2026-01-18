# Smart Form Web Application

A full-stack web application combining Vue.js frontend with Spring Boot backend for intelligent form parsing using LLMs.

## Quick Start

### Development Mode (Recommended for Development)

Start both frontend and backend with hot-reload:

```bash
./start.sh
```

- **Frontend**: `http://localhost:5173` (or `http://<your-host>:5173`)
- **Backend API**: `http://localhost:8008/api`

### Production Mode (Single Server)

Build and serve frontend from backend:

```bash
./start.sh prod
```

- **Application**: `http://localhost:8008`

## Architecture

```
┌─────────────────────────────────────┐
│   Browser (http://localhost:8008)  │
└─────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│     Spring Boot Backend (8008)      │
│  ┌───────────────────────────────┐  │
│  │  Static Resources (/)         │  │  ← Vue.js SPA
│  │  - index.html                 │  │
│  │  - /assets/*.js, *.css        │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │  REST API (/api/forms)        │  │  ← Form parsing API
│  │  - POST /parse                │  │
│  │  - GET /schema/{formType}     │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│        OpenAI API / LLM             │
└─────────────────────────────────────┘
```

## Features

- **Intelligent Form Parsing**: Natural language to structured form data
- **Multiple Form Types**: Leave requests, task creation, and more
- **Confidence Scoring**: Visual feedback on parsing accuracy
- **Single-Page Application**: Smooth client-side routing
- **Production Ready**: Optimized build with all assets bundled

## Project Structure

```
smart-form-web/
├── frontend/                    # Vue.js frontend
│   ├── src/
│   │   ├── components/         # Form components
│   │   │   ├── LeaveForm.vue
│   │   │   ├── TaskForm.vue
│   │   │   └── FieldInfo.vue
│   │   ├── api.ts             # API client
│   │   ├── types.ts           # TypeScript definitions
│   │   └── App.vue            # Main app component
│   ├── dist/                  # Production build (generated)
│   ├── package.json
│   └── vite.config.ts
│
├── src/main/
│   ├── java/com/fanyamin/web/
│   │   ├── controller/
│   │   │   └── FormController.java    # REST API endpoints
│   │   ├── config/
│   │   │   ├── WebConfig.java         # Web & static resource config
│   │   │   └── SmartFormConfig.java   # LLM configuration
│   │   ├── dto/                       # Data transfer objects
│   │   └── SmartFormWebApplication.java
│   └── resources/
│       ├── application.properties     # Server configuration
│       └── static/                    # Frontend files (auto-copied)
│
├── pom.xml                   # Maven configuration
├── start.sh                  # Startup script
└── DEPLOYMENT.md            # Detailed deployment guide
```

## API Endpoints

### Parse Form
```bash
POST /api/forms/parse
Content-Type: application/json

{
  "formType": "leave",
  "userInput": "I need annual leave from Dec 20 to Dec 25 for vacation"
}
```

Response:
```json
{
  "fields": {
    "leave_type": {
      "value": "annual",
      "confidence": 0.95,
      "reasoning": "User explicitly mentioned 'annual leave'"
    },
    "start_date": {
      "value": "2025-12-20",
      "confidence": 0.99,
      "reasoning": "Clear date range specified"
    }
  },
  "errors": []
}
```

### Get Schema
```bash
GET /api/forms/schema/{formType}
```

Returns JSON schema for the specified form type.

## Building from Source

### Prerequisites
- Java 17+
- Maven 3.6+
- Node.js 18+
- npm or yarn

### Build Steps

1. **Build Frontend**
   ```bash
   cd frontend
   npm install
   npm run build
   ```

2. **Build Backend** (automatically copies frontend)
   ```bash
   cd ..
   mvn clean package -DskipTests
   ```

3. **Run**
   ```bash
   java -jar target/smart-form-web-1.0-SNAPSHOT.jar
   ```

## Configuration

### Environment Variables

Create a `.env` file in the project root:

```bash
# OpenAI Configuration
OPENAI_API_KEY=your-api-key-here
OPENAI_MODEL=gpt-4
OPENAI_BASE_URL=https://api.openai.com/v1

# Optional: For self-signed certificates
DISABLE_SSL_VERIFICATION=true
```

### Application Properties

Edit `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8008

# Logging
logging.level.com.fanyamin=INFO
```

## Development Workflow

### Frontend Development

```bash
cd frontend
npm run dev
```

The Vite dev server provides:
- Hot Module Replacement (HMR)
- Fast refresh
- Proxy to backend API

### Backend Development

```bash
mvn spring-boot:run
```

Spring Boot DevTools provides:
- Automatic restart on code changes
- Live reload

### Full Stack Development

```bash
./start.sh
```

Both servers run simultaneously with auto-reload.

## Testing

### Manual Testing

1. Start the application
2. Open browser to `http://localhost:8008` (prod) or `http://localhost:5173` (dev)
3. Try parsing a leave request:
   - Input: "I need sick leave for 3 days starting tomorrow"
   - Verify form fields are populated correctly

### API Testing

```bash
# Test schema endpoint
curl http://localhost:8008/api/forms/schema/leave

# Test parsing
curl -X POST http://localhost:8008/api/forms/parse \
  -H "Content-Type: application/json" \
  -d '{"formType":"leave","userInput":"sick leave Dec 20-22"}'
```

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for comprehensive deployment guide including:
- Docker deployment
- Production optimization
- Environment-specific configuration
- Troubleshooting

### Quick Production Deployment

```bash
# Build everything
./start.sh prod

# Or manually
cd frontend && npm run build && cd ..
mvn clean package -DskipTests

# Deploy the JAR
java -jar target/smart-form-web-1.0-SNAPSHOT.jar
```

## How It Works

### Development Mode
1. Vite dev server runs on port 5173
2. Spring Boot backend runs on port 8008
3. Vite proxies `/api/*` requests to backend
4. Frontend accessed via Vite server

### Production Mode
1. `npm run build` creates optimized frontend in `frontend/dist/`
2. Maven copies `dist/*` to `target/classes/static/`
3. Spring Boot serves static files from `/static/` at root path
4. API endpoints remain at `/api/*`
5. Single server on port 8008 serves everything

### SPA Routing
- Direct file requests (JS, CSS, images) → served directly
- API requests (`/api/*`) → routed to Spring controllers
- Unknown paths → return `index.html` for Vue Router to handle

## Troubleshooting

### Port Already in Use
```bash
# Find and kill process on port 8008
lsof -ti:8008 | xargs kill -9

# Or use different port
SERVER_PORT=8009 mvn spring-boot:run
```

### Frontend Not Loading
```bash
# Rebuild frontend
cd frontend && npm run build

# Rebuild backend with new frontend
cd .. && mvn clean package
```

### API Not Working
- Check backend logs for errors
- Verify OpenAI API key is set
- Test API directly: `curl http://localhost:8008/api/forms/schema/leave`

## Technologies

**Frontend:**
- Vue.js 3 (Composition API)
- TypeScript
- Vite (build tool)
- Axios (HTTP client)

**Backend:**
- Spring Boot 3.2
- Java 17
- Maven
- OpenAI API integration

## License

[Add your license here]

## Contributing

[Add contribution guidelines here]

