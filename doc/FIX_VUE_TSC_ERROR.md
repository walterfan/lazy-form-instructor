# Fix: vue-tsc "Search string not found" Error

## Problem

When running `npm run build`, you get this error:

```
Search string not found: "/supportedTSExtensions = .*(?=;)/"
```

This is caused by incompatibility between `vue-tsc` v1.8.x and TypeScript v5.3.x.

## Solution

### Quick Fix (On the Server)

Run these commands on your server where the build is failing:

```bash
cd lazy-form-web/frontend

# Remove old dependencies
rm -rf node_modules package-lock.json

# Reinstall with updated package.json
npm install

# Build
npm run build
```

### Step-by-Step Fix

1. **Navigate to frontend directory:**
   ```bash
   cd lazy-form-web/frontend
   ```

2. **Verify package.json has correct versions:**
   ```bash
   grep -A 5 '"devDependencies"' package.json
   ```
   
   Should show:
   ```json
   "devDependencies": {
     "@vitejs/plugin-vue": "^5.0.0",
     "typescript": "~5.3.3",
     "vite": "^5.0.8",
     "vue-tsc": "^2.0.0"
   }
   ```

3. **If package.json is outdated, update it:**
   
   If you see `"vue-tsc": "^1.8.27"`, update it to `"vue-tsc": "^2.0.0"`:
   
   ```bash
   # Option 1: Manual edit
   vi package.json  # or nano, vim, etc.
   # Change "vue-tsc": "^1.8.27" to "vue-tsc": "^2.0.0"
   
   # Option 2: Using sed
   sed -i 's/"vue-tsc": "^1.8.27"/"vue-tsc": "^2.0.0"/' package.json
   ```

4. **Clean and reinstall:**
   ```bash
   rm -rf node_modules package-lock.json
   npm install
   ```

5. **Verify the installed version:**
   ```bash
   npm list vue-tsc
   ```
   
   Should show version 2.x.x:
   ```
   smart-form-frontend@1.0.0 /path/to/frontend
   └── vue-tsc@2.0.6
   ```

6. **Build:**
   ```bash
   npm run build
   ```

## Why This Happens

- **Root Cause**: `vue-tsc` v1.8.x is incompatible with TypeScript v5.3.x
- **The Fix**: Upgrade to `vue-tsc` v2.0.0+ which supports newer TypeScript versions
- **Why Clean Install**: `npm install` alone might not update if `package-lock.json` has locked the old version

## Verification

After the fix, you should see:

```bash
$ npm run build

> smart-form-frontend@1.0.0 build
> vue-tsc && vite build

vite v5.4.21 building for production...
✓ 73 modules transformed.
✓ built in 448ms
```

And the `dist/` folder should be created:

```bash
$ ls -la dist/
total 24
drwxr-xr-x  4 user user 4096 Dec 12 10:00 .
drwxr-xr-x 10 user user 4096 Dec 12 10:00 ..
drwxr-xr-x  2 user user 4096 Dec 12 10:00 assets
-rw-r--r--  1 user user  470 Dec 12 10:00 index.html
```

## Alternative: Skip Type Checking (Not Recommended)

If you need to build quickly without fixing the dependencies (for testing only):

```bash
# Modify package.json temporarily
# Change: "build": "vue-tsc && vite build"
# To:     "build": "vite build"

npm run build
```

**Warning**: This skips TypeScript type checking and may hide errors.

## For CI/CD Pipelines

If you're running builds in CI/CD, make sure:

1. **Always use clean install:**
   ```bash
   npm ci  # Uses package-lock.json, faster and more reliable
   ```

2. **Or force clean:**
   ```bash
   rm -rf node_modules package-lock.json && npm install
   ```

3. **Cache node_modules properly:**
   - Include `package-lock.json` in cache key
   - Invalidate cache when `package.json` changes

## Complete Build Script (Server)

Create a build script for convenience:

```bash
#!/bin/bash
# build-frontend.sh

set -e

cd "$(dirname "$0")"

echo "=== Building Frontend ==="
echo ""

# Check if package.json exists
if [ ! -f "package.json" ]; then
    echo "Error: package.json not found"
    exit 1
fi

# Clean install if requested
if [ "$1" == "--clean" ] || [ "$1" == "-c" ]; then
    echo "Cleaning node_modules and package-lock.json..."
    rm -rf node_modules package-lock.json
fi

# Install dependencies
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Check vue-tsc version
echo ""
echo "Checking vue-tsc version..."
VUE_TSC_VERSION=$(npm list vue-tsc --depth=0 | grep vue-tsc | grep -oP '\d+\.\d+\.\d+')
echo "vue-tsc version: $VUE_TSC_VERSION"

if [[ "$VUE_TSC_VERSION" < "2.0.0" ]]; then
    echo "Warning: vue-tsc version is less than 2.0.0"
    echo "This may cause build errors. Please update package.json"
    echo "Run: npm install vue-tsc@^2.0.0 --save-dev"
fi

# Build
echo ""
echo "Building..."
npm run build

echo ""
echo "✓ Build completed successfully"
echo "Output: dist/"
```

Usage:
```bash
chmod +x build-frontend.sh

# Normal build
./build-frontend.sh

# Clean build (removes node_modules)
./build-frontend.sh --clean
```

## Troubleshooting

### Error persists after reinstall

```bash
# 1. Check Node.js version (should be 18+)
node --version

# 2. Check npm version
npm --version

# 3. Clear npm cache
npm cache clean --force

# 4. Try with different Node.js version
nvm install 20
nvm use 20
npm install
npm run build
```

### Permission errors

```bash
# Fix npm permissions
sudo chown -R $(whoami) ~/.npm
sudo chown -R $(whoami) node_modules

# Or reinstall with different permissions
sudo npm install -g npm@latest
```

### Network/proxy issues

```bash
# Check registry
npm config get registry

# Use different registry if needed
npm config set registry https://registry.npmjs.org/

# Or use a mirror registry if needed
npm config set registry https://registry.npmmirror.com/
```

## Prevention

To prevent this issue in the future:

1. **Commit package-lock.json** to version control
2. **Document Node.js version** in README (use `.nvmrc` file)
3. **Use exact versions** for critical dependencies
4. **Add pre-build check** in CI/CD to verify dependency versions
5. **Keep dependencies updated** regularly

## Related Files

- `package.json` - Dependency versions
- `package-lock.json` - Locked dependency tree (auto-generated)
- `tsconfig.json` - TypeScript configuration
- `vite.config.ts` - Build configuration

## Summary

This error occurs due to version incompatibility. The solution is straightforward:

1. Update `vue-tsc` to version 2.0.0+
2. Clean install dependencies
3. Rebuild

The fix has been applied to the repository. On any new machine, just run:

```bash
npm install && npm run build
```

