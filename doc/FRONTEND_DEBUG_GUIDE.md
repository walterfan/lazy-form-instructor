# Frontend Form Fill Debugging Guide

## Issue
Backend returns a response, but the form is not rendered/filled.

## Potential Causes & Solutions

### 1. Check Console for Errors

Open browser DevTools (F12) and check the Console tab for:
- TypeScript compilation errors
- Vue warnings
- Network errors
- Data structure mismatches

### 2. Verify Data Flow

The data flows: Backend → API → App.vue → LeaveForm/TaskForm

**Check each step:**

```javascript
// In App.vue
console.log('Parsing result received:', result)
console.log('Fields:', result.fields)

// In LeaveForm.vue  
console.log('LeaveForm: parsingResult changed:', result)
console.log('LeaveForm: formData updated:', formData.value)
```

### 3. Common Issues

#### Issue A: Form Component Not Rendering

**Symptom**: The "Step 2: Review & Submit" section doesn't appear.

**Cause**: `parsingResult` is null or undefined.

**Check in App.vue**:
```vue
<div v-if="parsingResult" class="result-section">
  <!-- This won't show if parsingResult is null -->
</div>
```

**Solution**: Verify the API is returning data:
```bash
curl -X POST http://localhost:8080/api/forms/parse \
  -H "Content-Type: application/json" \
  -d '{
    "formType": "leave",
    "userInput": "I need a vacation from July 1st to July 10th"
  }'
```

#### Issue B: Fields Not Filling

**Symptom**: Form appears but fields are empty.

**Cause 1 - Field Name Mismatch**:
```javascript
// Backend returns: { "leaveType": "annual" }  // camelCase
// Frontend expects: { "leave_type": "annual" }  // snake_case
```

**Solution**: Check `SchemaGenerator.getFieldName()` - it should convert to snake_case.

**Cause 2 - Reactive Update Issue**:
The `watch` might not be triggering properly.

**Solution**: Check if the watch is firing:
```javascript
watch(() => props.parsingResult, (result) => {
  console.log('Watch triggered:', result)
  // ...
}, { immediate: true })
```

#### Issue C: v-model Not Binding

**Symptom**: Console shows data but form fields don't update.

**Cause**: Reactivity issue with nested properties.

**Solution**: Ensure formData is properly reactive:
```javascript
const formData = ref<LeaveFormData>({})  // ✅ Correct

// When updating, reassign completely:
formData.value = { ...newData }  // ✅ Better reactivity
```

### 4. Test API Response Format

**Expected Backend Response**:
```json
{
  "fields": {
    "leave_type": {
      "value": "annual",
      "confidence": 0.95,
      "reasoning": "...",
      "alternatives": []
    },
    "start_date": {
      "value": "2024-07-01",
      "confidence": 0.99,
      "reasoning": "...",
      "alternatives": []
    }
  },
  "errors": []
}
```

**If Backend Returns Different Format**:
Check if Jackson is serializing correctly. Add to `application.properties`:
```properties
spring.jackson.property-naming-strategy=SNAKE_CASE
```

### 5. Check Network Tab

In DevTools → Network tab:
1. Click "Auto-Fill Form"
2. Find the POST request to `/api/forms/parse`
3. Check the Response tab
4. Verify JSON structure matches expected format

### 6. Vue DevTools

Install Vue DevTools browser extension:
1. Open Vue DevTools
2. Select the LeaveForm component
3. Check `props.parsingResult`
4. Check `formData` state
5. Verify data is present

### 7. Quick Fix: Force Re-render

If data is there but not showing, try forcing a re-render:

```vue
<LeaveForm
  v-if="parsingResult"
  :key="JSON.stringify(parsingResult)"  <!-- Force re-render -->
  :parsing-result="parsingResult"
/>
```

### 8. Debug Mode

Enable debug logging in both frontend and backend:

**Backend (.env)**:
```bash
LLM_DEBUG=true
```

**Frontend (App.vue)**:
```javascript
const handleParse = async () => {
  console.log('=== PARSE START ===')
  console.log('Input:', userInput.value)
  console.log('Type:', formType.value)
  
  const result = await parseForm(...)
  
  console.log('=== PARSE RESULT ===')
  console.log('Result:', result)
  console.log('Fields:', result.fields)
  console.log('Field names:', Object.keys(result.fields))
  console.log('=== END ===')
}
```

### 9. Minimal Test

Create a minimal test in App.vue to verify reactivity:

```vue
<!-- Add this temporarily -->
<div v-if="parsingResult">
  <h3>Debug: Raw Result</h3>
  <pre>{{ JSON.stringify(parsingResult, null, 2) }}</pre>
</div>
```

This will show if the data is being received correctly.

### 10. Common Vue 3 Pitfalls

**Issue**: Using wrong ref syntax
```javascript
// ❌ Wrong
let formData = ref({})

// ✅ Correct
const formData = ref({})
```

**Issue**: Mutating ref directly
```javascript
// ❌ Wrong
formData.value.field = 'value'

// ✅ Better
formData.value = { ...formData.value, field: 'value' }
```

## Step-by-Step Debugging

1. **Open browser console** (F12)
2. **Click "Auto-Fill Form"**
3. **Check for logs**:
   - "Parsing result received: ..."
   - "Fields: ..."
   - "LeaveForm: parsingResult changed: ..."
   - "Mapping field: ..."
4. **If no logs appear**: Check network tab for API errors
5. **If logs show data**: Issue is in component rendering
6. **If logs show wrong format**: Issue is in backend serialization

## Quick Diagnostic Commands

```bash
# Test backend API directly
curl -X POST http://localhost:8080/api/forms/parse \
  -H "Content-Type: application/json" \
  -d '{"formType":"leave","userInput":"I need vacation next week"}' | jq

# Check if frontend is running
curl http://localhost:5173

# Check if backend is running
curl http://localhost:8080/api/forms/schema/leave
```

## Expected Behavior

1. ✅ User enters text
2. ✅ Clicks "Auto-Fill Form"
3. ✅ Loading spinner appears
4. ✅ API call is made
5. ✅ Response is received
6. ✅ "Step 2: Review & Submit" section appears
7. ✅ Form fields are populated
8. ✅ Confidence scores are displayed
9. ✅ Fields are highlighted with blue background

## Need More Help?

Share these in your response:
1. Browser console output
2. Network tab response JSON
3. Any Vue warnings
4. Which step fails (1-9 above)

