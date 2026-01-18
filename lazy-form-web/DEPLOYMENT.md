# Smart Form Web - Deployment Guide

This guide explains how to deploy the Smart Form Web application, which consists of a Vue.js frontend and Spring Boot backend.

## Architecture

The application can run in two modes:

1. **Development Mode**: Frontend (Vite dev server) and backend run separately
   - Frontend: `http://localhost:5173` (or `http://<your-host>:5173`)
   - Backend API: `http://localhost:8008/api`
   - Frontend proxies API requests to backend

2. **Production Mode**: Frontend is built and served by the Spring Boot backend
   - Single endpoint: `http://localhost:8008`
   - Frontend static files served from `/`
   - API available at `/api`

## Quick Start

### Development Mode

```bash
# Start both frontend and backend in development mode
./start.sh
```

This will:
- Build and start the Spring Boot backend on port 8008
- Start Vite dev server on port 5173
- Enable hot-reload for frontend development

Access the application at: `http://localhost:5173` (or `http://<your-host>:5173` for remote access)

### Production Mode

```bash
# Build and run in production mode
./start.sh prod
```

This will:
1. Build the frontend (`npm run build`)
2. Copy frontend dist files to backend resources
3. Build the backend with embedded frontend
4. Start the Spring Boot application

Access the application at: `http://localhost:8008`

## Manual Build & Deployment

### Step 1: Build Frontend

```bash
cd frontend
npm install
npm run build
```

This creates the production build in `frontend/dist/`

### Step 2: Build Backend (includes copying frontend)

```bash
cd ..
mvn clean package -DskipTests
```

The Maven build automatically:
- Copies `frontend/dist/*` to `target/classes/static/`
- Packages everything into a single JAR file

### Step 3: Run the Application

```bash
# Run the packaged JAR
java -jar target/smart-form-web-1.0-SNAPSHOT.jar

# Or use Maven
mvn spring-boot:run
```

## How It Works

### Maven Build Process

The `pom.xml` includes a Maven Resources Plugin configuration:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-frontend-dist</id>
            <phase>process-resources</phase>
            <goals>
                <goal>copy-resources</goal>
            </goals>
            <configuration>
                <outputDirectory>${project.build.outputDirectory}/static</outputDirectory>
                <resources>
                    <resource>
                        <directory>${project.basedir}/frontend/dist</directory>
                        <filtering>false</filtering>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

During the `process-resources` phase, this plugin:
1. Looks for `frontend/dist/` directory
2. Copies all files to `target/classes/static/`
3. Spring Boot automatically serves files from `static/` at the root path

### Spring Boot Configuration

**WebConfig.java** configures:
- Static resource handling from `classpath:/static/`
- SPA routing support (returns `index.html` for non-API routes)
- CORS configuration for API endpoints

**application.properties** sets:
```properties
spring.web.resources.static-locations=classpath:/static/
spring.web.resources.add-mappings=true
```

### SPA Routing Support

The `WebConfig` includes a custom `PathResourceResolver`:
- If a file exists, it's served directly
- If a file doesn't exist and the path doesn't start with `/api`, return `index.html`
- This allows Vue Router to handle client-side routing

## File Structure

```
smart-form-web/
├── frontend/              # Vue.js frontend
│   ├── dist/             # Production build (generated)
│   ├── src/              # Source code
│   └── package.json
├── src/main/
│   ├── java/             # Spring Boot backend
│   └── resources/
│       ├── static/       # Frontend files (copied during build)
│       └── application.properties
├── target/
│   ├── classes/static/   # Frontend in compiled output
│   └── smart-form-web-1.0-SNAPSHOT.jar
├── pom.xml
└── start.sh
```

## Production Deployment Checklist

1. **Build Frontend**
   ```bash
   cd frontend && npm run build
   ```

2. **Build Backend**
   ```bash
   cd .. && mvn clean package -DskipTests
   ```

3. **Verify Static Files**
   ```bash
   # Check that files were copied
   ls -la target/classes/static/
   ```

4. **Set Environment Variables**
   ```bash
   export OPENAI_API_KEY=your-api-key
   export OPENAI_MODEL=gpt-4
   # Or use .env file
   ```

5. **Run the Application**
   ```bash
   java -jar target/smart-form-web-1.0-SNAPSHOT.jar
   ```

6. **Test**
   - Open browser to `http://localhost:8008`
   - Verify frontend loads
   - Test form parsing functionality
   - Check API endpoints at `http://localhost:8008/api/forms/schema/leave`

## Troubleshooting

### Frontend Not Loading

**Problem**: 404 errors or blank page

**Solutions**:
- Verify frontend was built: `ls frontend/dist/`
- Check files copied to target: `ls target/classes/static/`
- Rebuild: `cd frontend && npm run build && cd .. && mvn package`
- Check browser console for errors

### API Requests Failing

**Problem**: API calls return 404 or CORS errors

**Solutions**:
- Verify API base URL in `frontend/src/api.ts` is `/api` (relative path)
- Check CORS configuration in `WebConfig.java`
- Verify backend is running: `curl http://localhost:8008/api/forms/schema/leave`

### Static Files Not Updated

**Problem**: Old version of frontend showing after rebuild

**Solutions**:
- Clear browser cache (Ctrl+Shift+R or Cmd+Shift+R)
- Delete target directory: `mvn clean`
- Rebuild everything: `mvn clean package`

### SPA Routing Not Working

**Problem**: Direct URLs return 404 (e.g., `http://localhost:8008/some-page`)

**Solutions**:
- Verify `WebConfig.java` has the custom `PathResourceResolver`
- Check that non-API paths return `index.html`
- Test: `curl http://localhost:8008/nonexistent` should return HTML

## Docker Deployment (Optional)

Create a `Dockerfile`:

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Build frontend
COPY frontend/package*.json frontend/
RUN cd frontend && npm install
COPY frontend/ frontend/
RUN cd frontend && npm run build

# Build backend
COPY pom.xml .
COPY src/ src/
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/smart-form-web-1.0-SNAPSHOT.jar app.jar
EXPOSE 8008
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:
```bash
docker build -t smart-form-web .
docker run -p 8008:8008 -e OPENAI_API_KEY=your-key smart-form-web
```

## Performance Optimization

1. **Enable Gzip Compression** (application.properties):
   ```properties
   server.compression.enabled=true
   server.compression.mime-types=text/html,text/css,application/javascript,application/json
   ```

2. **Cache Static Resources**:
   ```properties
   spring.web.resources.cache.cachecontrol.max-age=31536000
   ```

3. **Frontend Build Optimization**:
   - Already configured in Vite for optimal production builds
   - Assets are hashed for cache-busting
   - Tree-shaking and minification enabled

## Environment-Specific Configuration

Use Spring profiles for different environments:

```bash
# application-prod.properties
server.port=80
logging.level.com.fanyamin=WARN

# Run with profile
java -jar app.jar --spring.profiles.active=prod
```

## Monitoring & Health Checks

Add Spring Boot Actuator for monitoring:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Health check endpoint: `http://localhost:8008/actuator/health`

