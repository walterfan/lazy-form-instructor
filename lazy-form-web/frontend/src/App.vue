<template>
  <div class="app">
    <header class="header">
      <h1>🤖 Smart Form Instructor</h1>
      <p class="subtitle">Intelligent Form Auto-Fill with Natural Language</p>
    </header>

    <div class="form-selector">
      <button
        :class="['form-type-btn', { active: formType === 'leave' }]"
        @click="formType = 'leave'"
      >
        📅 Leave Request
      </button>
      <button
        :class="['form-type-btn', { active: formType === 'task' }]"
        @click="formType = 'task'"
      >
        ✅ Task Request
      </button>
    </div>

    <div class="main-content">
      <div class="input-section">
        <h2>Step 1: Describe Your Request</h2>
        <p class="section-hint">Enter a natural language description, then click "Auto-Fill Form" to populate the fields automatically.</p>
        <textarea
          v-model="userInput"
          :placeholder="getPlaceholder()"
          rows="4"
          class="user-input"
        ></textarea>
        
        <button
          class="parse-btn"
          @click="handleParse"
          :disabled="loading || !userInput.trim()"
        >
          {{ loading ? '🔄 Processing...' : '✨ Auto-Fill Form' }}
        </button>

        <div v-if="loading && streamingEnabled" class="streaming-section">
          <h3>Streaming Output</h3>
          <p class="section-hint">
            Live LLM output (raw JSON) + final JSON Schema validation.
          </p>
          <pre class="streaming-output">{{ rawStreamText }}</pre>

          <div v-if="finalSchemaOk === true" class="schema-ok">
            ✅ Final schema validation: OK
          </div>
          <div v-else-if="finalSchemaOk === false" class="schema-fail">
            ❌ Final schema validation failed ({{ finalSchemaErrors.length }} errors)
            <div class="schema-errors">
              <div v-for="(err, idx) in finalSchemaErrors" :key="idx" class="schema-error-item">
                <strong>{{ err.path }}:</strong> {{ err.message }}
                <span class="error-type">({{ err.type }})</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-spinner">
          <div class="spinner"></div>
          <p>Analyzing your request with AI...</p>
        </div>

        <div v-if="error" class="error-message">
          ❌ {{ error }}
        </div>

        <!-- Examples section moved here -->
        <div class="examples">
          <h3>💡 Try these examples:</h3>
          <div class="example-list">
            <button v-if="formType === 'leave'" class="example-btn" @click="userInput = 'I\'m getting married next month! I\'d like to take a week off starting from December 15th.'">
              Marriage leave example
            </button>
            <button v-if="formType === 'leave'" class="example-btn" @click="userInput = 'I need sick leave from next Monday for 3 days. I have a doctor\'s appointment.'">
              Sick leave example
            </button>
            <button v-if="formType === 'task'" class="example-btn" @click="userInput = 'tomorrow is monday, I have to finish the sending alert feature ASAP, the release date is next friday'">
              Urgent task example
            </button>
            <button v-if="formType === 'task'" class="example-btn" @click="userInput = 'Need to write documentation for the new API by end of this week, should take about 2 hours'">
              Documentation task example
            </button>
          </div>
        </div>
      </div>

      <div class="result-section">
        <h2>Step 2: Review & Submit</h2>
        <p class="section-hint">{{ parsingResult ? 'Review the auto-filled fields and submit.' : 'Fill the form manually or use Step 1 to auto-fill.' }}</p>
        
        <!-- Debug info - only show if we have parsing results -->
        <details class="debug-section" v-if="parsingResult">
          <summary>🐛 Debug Info (click to expand)</summary>
          <div class="debug-content">
            <h4>Field Names Received:</h4>
            <pre>{{ Object.keys(parsingResult.fields) }}</pre>
            <h4>Full Response:</h4>
            <pre>{{ JSON.stringify(parsingResult, null, 2) }}</pre>
          </div>
        </details>
        
        <LeaveForm
          v-if="formType === 'leave'"
          :parsing-result="parsingResult"
        />
        
        <TaskForm
          v-else
          :parsing-result="parsingResult"
        />

        <div v-if="parsingResult && parsingResult.errors && parsingResult.errors.length > 0" class="validation-errors">
          <h3>⚠️ Validation Errors:</h3>
          <div v-for="(err, index) in parsingResult.errors" :key="index" class="error-item">
            <strong>{{ err.path }}:</strong> {{ err.message }}
            <span class="error-type">({{ err.type }})</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { parseForm, parseFormStream } from './api'
import type { ParsingResult, ValidationError } from './types'
import LeaveForm from './components/LeaveForm.vue'
import TaskForm from './components/TaskForm.vue'

const formType = ref<'leave' | 'task'>('leave')
const userInput = ref('')
const loading = ref(false)
const error = ref('')
const parsingResult = ref<ParsingResult | null>(null)

// Streaming UI state
const streamingEnabled = ref(true)
const rawStreamText = ref('')
const finalSchemaOk = ref<boolean | null>(null)
const finalSchemaErrors = ref<ValidationError[]>([])
let abortController: AbortController | null = null

const getPlaceholder = () => {
  if (formType.value === 'leave') {
    return 'Example: I\'m getting married next month! I\'d like to take a week off starting from December 15th.'
  } else {
    return 'Example: tomorrow is monday, I have to finish the sending alert feature ASAP, the release date is next friday'
  }
}

const handleParse = async () => {
  if (!userInput.value.trim()) return

  // Cancel any previous stream
  if (abortController) {
    abortController.abort()
  }
  abortController = new AbortController()

  loading.value = true
  error.value = ''
  parsingResult.value = null
  rawStreamText.value = ''
  finalSchemaOk.value = null
  finalSchemaErrors.value = []

  try {
    const req = { formType: formType.value, userInput: userInput.value }

    if (streamingEnabled.value) {
      await parseFormStream(
        req,
        (eventName, data) => {
          if (eventName === 'rawChunk' && data && typeof data.chunk === 'string') {
            rawStreamText.value += data.chunk
          } else if (eventName === 'finalResult') {
            // Server sends StreamingParseEvent.FinalResult { result, schemaErrors, attempt }
            if (data && data.result) {
              parsingResult.value = data.result as ParsingResult
            }
            const errs = (data && data.schemaErrors) ? (data.schemaErrors as ValidationError[]) : []
            finalSchemaErrors.value = errs
            finalSchemaOk.value = errs.length === 0
          } else if (eventName === 'attemptFailed') {
            // Optional: show current attempt's schema errors (still waiting for retry)
            const errs = (data && data.schemaErrors) ? (data.schemaErrors as ValidationError[]) : []
            finalSchemaErrors.value = errs
            finalSchemaOk.value = false
          } else if (eventName === 'error') {
            error.value = (data && data.error && data.error.message) ? data.error.message : 'Streaming error'
          }
        },
        abortController.signal
      )
    } else {
      const result = await parseForm(req)
      parsingResult.value = result
    }
  } catch (err: any) {
    error.value = err.response?.data?.message || err.message || 'Failed to parse form'
    console.error('Parse error:', err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.app {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.header {
  text-align: center;
  margin-bottom: 3rem;
}

.header h1 {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: #666;
  font-size: 1.1rem;
}

.form-selector {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-bottom: 2rem;
}

.form-type-btn {
  padding: 0.8rem 2rem;
  font-size: 1.1rem;
  border-radius: 12px;
  border: 2px solid #ddd;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.form-type-btn:hover {
  border-color: #667eea;
  transform: translateY(-2px);
}

.form-type-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
}

.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
  margin-bottom: 3rem;
}

.input-section, .result-section {
  background: white;
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.input-section h2, .result-section h2 {
  margin-bottom: 0.5rem;
  color: #333;
}

.section-hint {
  margin-bottom: 1.5rem;
  color: #666;
  font-size: 0.95rem;
  line-height: 1.4;
}

.user-input {
  width: 100%;
  padding: 1rem;
  border: 2px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  margin-bottom: 1rem;
  resize: vertical;
  transition: border-color 0.3s;
}

.user-input:focus {
  outline: none;
  border-color: #667eea;
}

.parse-btn {
  width: 100%;
  padding: 1rem 2rem;
  font-size: 1.1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.parse-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.parse-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-top: 1.5rem;
  padding: 1rem;
  gap: 1rem;
}

.loading-spinner p {
  color: #667eea;
  font-size: 0.95rem;
  margin: 0;
  animation: pulse 1.5s ease-in-out infinite;
}

.streaming-section {
  margin-top: 1.5rem;
  padding: 1rem;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  background: #f8f9fa;
}

.streaming-output {
  margin-top: 0.75rem;
  padding: 1rem;
  border-radius: 6px;
  background: #0b1020;
  color: #d1e7ff;
  font-size: 0.9rem;
  max-height: 240px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.schema-ok {
  margin-top: 0.75rem;
  color: #1b7f3a;
  font-weight: 600;
}

.schema-fail {
  margin-top: 0.75rem;
  color: #b42318;
  font-weight: 600;
}

.schema-errors {
  margin-top: 0.5rem;
}

.schema-error-item {
  margin-top: 0.25rem;
  padding: 0.5rem;
  background: white;
  border-radius: 6px;
  border: 1px solid #f1c2c2;
  color: #7a271a;
  font-weight: 400;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e0e0e0;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.error-message {
  margin-top: 1rem;
  padding: 1rem;
  background: #fee;
  border: 1px solid #fcc;
  border-radius: 8px;
  color: #c33;
}

.validation-errors {
  margin-top: 2rem;
  padding: 1rem;
  background: #fff3cd;
  border: 1px solid #ffc107;
  border-radius: 8px;
}

.error-item {
  margin: 0.5rem 0;
  padding: 0.5rem;
  background: white;
  border-radius: 4px;
}

.error-type {
  color: #666;
  font-size: 0.9rem;
  margin-left: 0.5rem;
}

.debug-section {
  margin-bottom: 2rem;
  padding: 1rem;
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 8px;
}

.debug-section summary {
  cursor: pointer;
  font-weight: 600;
  color: #667eea;
  user-select: none;
}

.debug-section summary:hover {
  color: #764ba2;
}

.debug-content {
  margin-top: 1rem;
}

.debug-content h4 {
  margin: 1rem 0 0.5rem 0;
  color: #333;
}

.debug-content pre {
  background: white;
  padding: 1rem;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 0.9rem;
}

.examples {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid #e0e0e0;
}

.examples h3 {
  margin-bottom: 1rem;
  color: #333;
  font-size: 1rem;
  text-align: left;
}

.example-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.example-btn {
  padding: 0.75rem 1rem;
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  text-align: left;
  font-size: 0.95rem;
  color: #495057;
}

.example-btn:hover {
  background: #e9ecef;
  border-color: #667eea;
  color: #667eea;
  transform: translateX(4px);
}

@media (max-width: 768px) {
  .main-content {
    grid-template-columns: 1fr;
  }
}
</style>

