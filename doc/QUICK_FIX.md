# Quick Fix for vue-tsc Error

## The Error You're Seeing

```
Search string not found: "/supportedTSExtensions = .*(?=;)/"
```

## Quick Fix (3 commands)

**Run these commands:**

```bash
cd lazy-form-web/frontend
rm -rf node_modules package-lock.json
npm install
npm run build
```

## Even Quicker (1 command)

**Use the automated fix script:**

```bash
cd lazy-form-web/frontend
./fix-vue-tsc.sh
```

## What This Does

1. **Removes old dependencies** (`node_modules` and `package-lock.json`)
2. **Reinstalls with updated versions** (vue-tsc 2.0.0 instead of 1.8.27)
3. **Builds the frontend** (creates `dist/` folder)

## Why This Happens

- Your local machine and server have different `node_modules` installed
- The server has old vue-tsc v1.8.x (incompatible with TypeScript 5.3)
- The fix upgrades to vue-tsc v2.0.0 (compatible)

## After Running the Fix

You should see:

```
✓ 73 modules transformed.
✓ built in 448ms
```

And `dist/` folder will be created with your built frontend files.

## Then Build the Full Application

```bash
cd lazy-form-web
mvn clean package -DskipTests
```

This will:
1. Copy `frontend/dist/*` to `target/classes/static/`
2. Package everything into a single JAR
3. Ready to deploy!

## Verify Everything Works

```bash
# Start the application
./start.sh prod

# Or
java -jar target/lazy-form-web-1.0-SNAPSHOT.jar

# Access at http://localhost:8008
```

---

## Detailed Fix Documentation

For more details, see: `FIX_VUE_TSC_ERROR.md`

For deployment guide, see: `../DEPLOYMENT.md`
