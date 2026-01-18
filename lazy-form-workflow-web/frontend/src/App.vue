<template>
  <div class="app-container">
    <header class="app-header">
      <h1>🔄 Smart Form Workflow Engine Demo</h1>
      <p class="subtitle">AI-Powered Business Process Automation</p>
    </header>

    <div class="main-content">
      <!-- Workflow Type Selector -->
      <div class="workflow-selector">
        <label>Select Workflow Type:</label>
        <select v-model="selectedWorkflowType" @change="onWorkflowTypeChange">
          <option value="">Choose a workflow...</option>
          <option v-for="(info, key) in workflowTypes" :key="key" :value="key">
            {{ info.name }}
          </option>
        </select>
        <div v-if="selectedWorkflowInfo" class="workflow-info">
          <p>{{ selectedWorkflowInfo.description }}</p>
          <p class="example"><strong>Example:</strong> {{ selectedWorkflowInfo.exampleInput }}</p>
        </div>
      </div>

      <!-- Workflow Diagram -->
      <WorkflowDiagram v-if="selectedWorkflowType" :workflowType="selectedWorkflowType" />

      <!-- User Input -->
      <div class="input-section">
        <label>Enter Your Request:</label>
        <textarea
          v-model="userInput"
          placeholder="Type your request in natural language..."
          rows="3"
          :disabled="!selectedWorkflowType"
        ></textarea>
        <button
          @click="handleExecuteWorkflow"
          :disabled="!userInput || !selectedWorkflowType || isExecuting"
          class="execute-btn"
        >
          {{ isExecuting ? '⏳ Executing...' : '▶️ Execute Workflow' }}
        </button>
      </div>

      <!-- Execution Progress -->
      <ExecutionProgress 
        v-if="isExecuting || result"
        :currentStep="currentStep"
        :parsedFields="progressParsedFields"
        :executionTrace="progressTrace"
        :finalState="progressFinalState"
      />

      <!-- Workflow Trail -->
      <WorkflowTrail 
        v-if="result && result.executionTrace.length > 0"
        :trace="result.executionTrace"
      />

      <!-- Results Section -->
      <div v-if="result" class="results-section">
        <h2>Execution Results</h2>
        
        <!-- Status Badge -->
        <div class="status-badge" :class="result.status.toLowerCase()">
          <span class="status-icon">{{ getStatusIcon(result.status) }}</span>
          <span class="status-text">{{ result.status }}</span>
        </div>
        
        <p class="message">{{ result.message }}</p>

        <!-- Parsed Fields -->
        <div v-if="result.parsedFields" class="parsed-fields">
          <h3>📝 Parsed Request Fields</h3>
          <div class="fields-grid">
            <div v-for="(field, key) in result.parsedFields" :key="key" class="field-item">
              <div class="field-name">{{ key }}</div>
              <div class="field-value">{{ formatValue(field.value) }}</div>
              <div class="field-confidence">
                <div class="confidence-bar">
                  <div
                    class="confidence-fill"
                    :style="{ width: `${field.confidence * 100}%` }"
                    :class="getConfidenceClass(field.confidence)"
                  ></div>
                </div>
                <span>{{ (field.confidence * 100).toFixed(0) }}% confident</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Execution Trace -->
        <ExecutionTrace :trace="result.executionTrace" />

        <!-- Final Context -->
        <div v-if="result.finalContext" class="final-context">
          <h3>🎯 Final State</h3>
          <div class="context-item">
            <strong>State:</strong> 
            <span class="state-value">{{ result.finalContext.state }}</span>
          </div>
          <div v-if="Object.keys(result.finalContext.variables).length > 0" class="context-item">
            <strong>Variables:</strong>
            <pre>{{ JSON.stringify(result.finalContext.variables, null, 2) }}</pre>
          </div>
        </div>
      </div>

      <!-- Error Display -->
      <div v-if="error" class="error-message">
        <h3>❌ Error</h3>
        <p>{{ error }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { WorkflowExecutionResponse, WorkflowTypeInfo } from './types'
import { WorkflowStatus } from './types'
import { executeWorkflow, getWorkflowTypes } from './api'
import ExecutionTrace from './components/ExecutionTrace.vue'
import WorkflowDiagram from './components/WorkflowDiagram.vue'
import ExecutionProgress from './components/ExecutionProgress.vue'
import WorkflowTrail from './components/WorkflowTrail.vue'

const selectedWorkflowType = ref('')
const userInput = ref('')
const workflowTypes = ref<Record<string, WorkflowTypeInfo>>({})
const result = ref<WorkflowExecutionResponse | null>(null)
const error = ref('')
const isExecuting = ref(false)

// Progress tracking
const currentStep = ref(0)
const progressParsedFields = ref<any>(null)
const progressTrace = ref<any[]>([])
const progressFinalState = ref('')

const selectedWorkflowInfo = computed(() => {
  return selectedWorkflowType.value ? workflowTypes.value[selectedWorkflowType.value] : null
})

onMounted(async () => {
  try {
    workflowTypes.value = await getWorkflowTypes()
  } catch (e) {
    error.value = 'Failed to load workflow types'
    console.error(e)
  }
})

const onWorkflowTypeChange = () => {
  // Set example input when workflow type changes
  if (selectedWorkflowInfo.value) {
    userInput.value = selectedWorkflowInfo.value.exampleInput
  }
  result.value = null
  error.value = ''
  currentStep.value = 0
  progressParsedFields.value = null
  progressTrace.value = []
  progressFinalState.value = ''
}

const handleExecuteWorkflow = async () => {
  if (!userInput.value || !selectedWorkflowType.value) return

  isExecuting.value = true
  error.value = ''
  result.value = null
  currentStep.value = 0
  progressParsedFields.value = null
  progressTrace.value = []
  progressFinalState.value = ''

  try {
    // Step 1: Parse Request (simulate delay)
    currentStep.value = 1
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // Step 2: Build Workflow (simulate delay)
    currentStep.value = 2
    await new Promise(resolve => setTimeout(resolve, 300))
    
    // Step 3: Execute Workflow (make actual API call)
    currentStep.value = 3
    const response = await executeWorkflow({
      userInput: userInput.value,
      workflowType: selectedWorkflowType.value
    })
    
    // Update progress with parsed fields
    progressParsedFields.value = response.parsedFields
    progressTrace.value = response.executionTrace
    
    // Simulate showing execution progress
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // Step 4: Complete
    currentStep.value = 4
    progressFinalState.value = response.finalContext.state
    await new Promise(resolve => setTimeout(resolve, 300))
    
    result.value = response
  } catch (e: any) {
    error.value = e.response?.data?.message || e.message || 'Failed to execute workflow'
    console.error(e)
    currentStep.value = 0
  } finally {
    isExecuting.value = false
  }
}

const getStatusIcon = (status: WorkflowStatus): string => {
  switch (status) {
    case WorkflowStatus.SUCCESS:
      return '✅'
    case WorkflowStatus.FAILED:
      return '❌'
    case WorkflowStatus.WAITING:
      return '⏸️'
    default:
      return '❓'
  }
}

const getConfidenceClass = (confidence: number): string => {
  if (confidence >= 0.9) return 'high'
  if (confidence >= 0.7) return 'medium'
  return 'low'
}

const formatValue = (value: any): string => {
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2rem;
}

.app-header {
  text-align: center;
  color: white;
  margin-bottom: 2rem;
}

.app-header h1 {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
}

.subtitle {
  font-size: 1.2rem;
  opacity: 0.9;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  padding: 2rem;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.workflow-selector {
  margin-bottom: 2rem;
}

.workflow-selector label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #333;
}

.workflow-selector select {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  margin-bottom: 1rem;
}

.workflow-info {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 8px;
  border-left: 4px solid #667eea;
}

.workflow-info p {
  margin: 0.5rem 0;
  color: #555;
}

.example {
  font-style: italic;
  color: #666;
}

.input-section {
  margin-bottom: 2rem;
}

.input-section label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #333;
}

.input-section textarea {
  width: 100%;
  padding: 1rem;
  border: 2px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  font-family: inherit;
  resize: vertical;
  margin-bottom: 1rem;
}

.input-section textarea:focus {
  outline: none;
  border-color: #667eea;
}

.input-section textarea:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.execute-btn {
  width: 100%;
  padding: 1rem 2rem;
  font-size: 1.1rem;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.execute-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
}

.execute-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.results-section {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 2px solid #eee;
}

.results-section h2 {
  color: #333;
  margin-bottom: 1rem;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 600;
  margin-bottom: 1rem;
}

.status-badge.success {
  background: #d4edda;
  color: #155724;
}

.status-badge.failed {
  background: #f8d7da;
  color: #721c24;
}

.status-badge.waiting {
  background: #fff3cd;
  color: #856404;
}

.message {
  font-size: 1.1rem;
  color: #555;
  margin: 1rem 0;
}

.parsed-fields {
  margin: 2rem 0;
}

.parsed-fields h3 {
  color: #333;
  margin-bottom: 1rem;
}

.fields-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1rem;
}

.field-item {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 8px;
  border-left: 4px solid #667eea;
}

.field-name {
  font-weight: 600;
  color: #555;
  font-size: 0.9rem;
  margin-bottom: 0.25rem;
}

.field-value {
  font-size: 1.1rem;
  color: #333;
  margin-bottom: 0.5rem;
}

.field-confidence {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: #666;
}

.confidence-bar {
  flex: 1;
  height: 4px;
  background: #e9ecef;
  border-radius: 2px;
  overflow: hidden;
}

.confidence-fill {
  height: 100%;
  transition: width 0.3s ease;
}

.confidence-fill.high {
  background: #28a745;
}

.confidence-fill.medium {
  background: #ffc107;
}

.confidence-fill.low {
  background: #dc3545;
}

.final-context {
  margin: 2rem 0;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.final-context h3 {
  color: #333;
  margin-bottom: 1rem;
}

.context-item {
  margin: 0.75rem 0;
}

.state-value {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: #667eea;
  color: white;
  border-radius: 4px;
  font-weight: 600;
  margin-left: 0.5rem;
}

.context-item pre {
  background: white;
  padding: 1rem;
  border-radius: 4px;
  overflow-x: auto;
  margin-top: 0.5rem;
}

.error-message {
  margin: 2rem 0;
  padding: 1rem;
  background: #f8d7da;
  border-left: 4px solid #dc3545;
  border-radius: 8px;
}

.error-message h3 {
  color: #721c24;
  margin-bottom: 0.5rem;
}

.error-message p {
  color: #721c24;
}
</style>

