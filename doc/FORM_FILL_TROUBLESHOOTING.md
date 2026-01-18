# Form Fill Issue - Troubleshooting Steps

## Changes Made to Debug the Issue

### 1. Added Console Logging

**App.vue** - Added logging to track API responses:
```javascript
console.log('Parsing result received:', result)
console.log('Fields:', result.fields)
```

**LeaveForm.vue** - Added logging to track field mapping:
```javascript
console.log('LeaveForm: parsingResult changed:', result)
console.log(`Mapping field: ${key} = ${result.fields[key].value}`)
console.log('LeaveForm: formData updated:', formData.value)
```

### 2. Added Debug Panel in UI

Added a collapsible debug section in `App.vue` that shows:
- Field names received from backend
- Full JSON response

This helps identify:
- If data is being received
- If field names match expectations
- If data structure is correct

### 3. Created Test Files

**test-api.html** - Standalone HTML page to test backend API:
- Tests `/api/forms/parse` endpoint
- Shows raw JSON response
- Displays field names
- Works independently of Vue frontend

**FRONTEND_DEBUG_GUIDE.md** - Comprehensive debugging guide covering:
- Common issues and solutions
- Step-by-step debugging process
- Console commands for testing
- Expected behavior

## How to Use These Debugging Tools

### Step 1: Test Backend Directly

```bash
# Test backend API
curl -X POST http://localhost:8080/api/forms/parse \
  -H "Content-Type: application/json" \
  -d '{
    "formType": "leave",
    "userInput": "I need vacation next week"
  }'
```

Expected response:
```json
{
  "fields": {
    "leave_type": { "value": "...", "confidence": 0.9, ... },
    "start_date": { "value": "...", "confidence": 0.9, ... }
  },
  "errors": []
}
```

### Step 2: Use test-api.html

1. Make sure backend is running on port 8080
2. Open `smart-form-web/test-api.html` in browser
3. Click "Test Leave Request"
4. Check if response shows proper data structure

### Step 3: Check Frontend Console

1. Start the full application (`./start.sh`)
2. Open browser DevTools (F12)
3. Go to Console tab
4. Click "Auto-Fill Form" in the UI
5. Look for these logs:
   ```
   Parsing result received: {fields: {...}, errors: []}
   Fields: {leave_type: {...}, start_date: {...}}
   LeaveForm: parsingResult changed: {fields: {...}, errors: []}
   Mapping field: leave_type = annual
   Mapping field: start_date = 2024-12-15
   LeaveForm: formData updated: {leave_type: "annual", start_date: "2024-12-15"}
   ```

### Step 4: Use Debug Panel in UI

1. After clicking "Auto-Fill Form"
2. Look for "🐛 Debug Info" section
3. Click to expand
4. Check:
   - Are field names correct? (should be snake_case: `leave_type`, not `leaveType`)
   - Is the data structure correct?
   - Are there any null values?

## Common Issues and Fixes

### Issue 1: Form Not Appearing

**Symptom**: "Step 2: Review & Submit" section doesn't show up.

**Possible Causes**:
1. `parsingResult` is null
2. API request failed
3. Network error

**Debug**:
- Check console for error messages
- Check Network tab in DevTools
- Verify backend is running

### Issue 2: Form Appears But Empty

**Symptom**: Form section shows but all fields are empty.

**Possible Causes**:
1. Field name mismatch (camelCase vs snake_case)
2. Data not being mapped correctly
3. Vue reactivity issue

**Debug**:
- Check "Debug Info" panel - are field names correct?
- Check console logs - is `formData` being updated?
- Inspect Vue DevTools - check component data

**Common Fix**: Ensure backend uses snake_case field names:
```java
// SchemaGenerator should convert:
"leaveType" → "leave_type"  ✅
```

### Issue 3: Some Fields Fill, Others Don't

**Symptom**: Only some fields are populated.

**Possible Causes**:
1. Backend didn't extract all fields
2. Some field names don't match
3. Confidence threshold filtering

**Debug**:
- Check "Debug Info" - which fields are missing?
- Check backend logs - did LLM return those fields?
- Check schema - are field names consistent?

### Issue 4: TypeScript/Build Errors

**Symptom**: Console shows TypeScript errors.

**Fix**:
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run dev
```

## Verification Checklist

- [ ] Backend is running on port 8080
- [ ] Frontend is running on port 5173
- [ ] No CORS errors in console
- [ ] API returns 200 status
- [ ] Response has `fields` object
- [ ] Field names are snake_case
- [ ] Debug panel shows data
- [ ] Console logs show data flow
- [ ] No Vue warnings in console

## Expected Console Output (Success)

```
// When clicking "Auto-Fill Form":

Parsing result received: {
  fields: {
    leave_type: {value: "annual", confidence: 0.95, ...},
    start_date: {value: "2024-12-15", confidence: 0.99, ...},
    end_date: {value: "2024-12-22", confidence: 0.95, ...}
  },
  errors: []
}

Fields: {leave_type: {...}, start_date: {...}, end_date: {...}}

LeaveForm: parsingResult changed: {fields: {...}, errors: []}

Mapping field: leave_type = annual
Mapping field: start_date = 2024-12-15
Mapping field: end_date = 2024-12-22

LeaveForm: formData updated: {
  leave_type: "annual",
  start_date: "2024-12-15",
  end_date: "2024-12-22"
}
```

## Next Steps

1. **Start the application**:
   ```bash
   cd smart-form-web
   ./start.sh
   ```

2. **Open browser to** `http://localhost:5173`

3. **Enter test input**:
   ```
   I'm getting married next month! I'd like to take a week off starting from December 15th.
   ```

4. **Click "Auto-Fill Form"**

5. **Check**:
   - Does "Step 2" section appear?
   - Does debug panel show data?
   - Are fields filled?
   - Check console for logs

6. **If still not working**, share:
   - Console output
   - Debug panel content
   - Network tab response
   - Any error messages

The debug tools are now in place to quickly identify where the issue is occurring!

