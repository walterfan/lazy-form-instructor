<template>
  <div class="execution-progress">
    <h3>⚡ Execution Progress</h3>
    
    <div class="progress-steps">
      <!-- Step 1: Parse Request -->
      <div class="progress-step" :class="getStepClass(1)">
        <div class="step-number">1</div>
        <div class="step-content">
          <div class="step-header">
            <span class="step-title">Parse Request</span>
            <span class="step-status">{{ getStepStatus(1) }}</span>
          </div>
          <div class="step-description">Extracting structured data using AI</div>
          
          <!-- Show parsed fields when complete -->
          <div v-if="currentStep >= 2 && parsedFields" class="step-result">
            <div class="parsed-summary">
              ✅ Parsed {{ Object.keys(parsedFields).length }} fields
              <div class="field-preview">
                <span v-for="(field, key) in parsedFields" :key="key" class="field-tag">
                  {{ key }}: {{ formatValue(field.value) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="step-connector" :class="{ active: currentStep >= 2 }"></div>

      <!-- Step 2: Build Workflow -->
      <div class="progress-step" :class="getStepClass(2)">
        <div class="step-number">2</div>
        <div class="step-content">
          <div class="step-header">
            <span class="step-title">Build Workflow</span>
            <span class="step-status">{{ getStepStatus(2) }}</span>
          </div>
          <div class="step-description">Constructing workflow graph (DAG)</div>
          
          <div v-if="currentStep >= 3" class="step-result">
            <div class="workflow-summary">
              ✅ Workflow graph ready with AI validation
            </div>
          </div>
        </div>
      </div>

      <div class="step-connector" :class="{ active: currentStep >= 3 }"></div>

      <!-- Step 3: Execute Workflow -->
      <div class="progress-step" :class="getStepClass(3)">
        <div class="step-number">3</div>
        <div class="step-content">
          <div class="step-header">
            <span class="step-title">Execute Workflow</span>
            <span class="step-status">{{ getStepStatus(3) }}</span>
          </div>
          <div class="step-description">Running nodes and making decisions</div>
          
          <!-- Show execution progress -->
          <div v-if="currentStep === 3 && executionTrace.length > 0" class="step-result">
            <div class="execution-summary">
              🔄 Executed {{ executionTrace.length }} nodes
              <div class="node-list">
                <div v-for="(trace, idx) in executionTrace" :key="idx" class="node-item">
                  <span class="node-icon">{{ getNodeIcon(trace.nodeType) }}</span>
                  <span class="node-name">{{ trace.nodeId }}</span>
                  <span class="node-status" :class="trace.status.toLowerCase()">
                    {{ trace.status }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="step-connector" :class="{ active: currentStep >= 4 }"></div>

      <!-- Step 4: Display Results -->
      <div class="progress-step" :class="getStepClass(4)">
        <div class="step-number">4</div>
        <div class="step-content">
          <div class="step-header">
            <span class="step-title">Complete</span>
            <span class="step-status">{{ getStepStatus(4) }}</span>
          </div>
          <div class="step-description">Workflow execution finished</div>
          
          <div v-if="currentStep >= 4 && finalState" class="step-result">
            <div class="final-summary" :class="finalState.toLowerCase()">
              {{ getFinalIcon(finalState) }} Final State: <strong>{{ finalState }}</strong>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Overall Progress Bar -->
    <div class="overall-progress">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: `${progressPercentage}%` }"></div>
      </div>
      <div class="progress-text">{{ progressPercentage }}% Complete</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FieldResult, TraceEntry } from '../types'

const props = defineProps<{
  currentStep: number
  parsedFields?: Record<string, FieldResult> | null
  executionTrace: TraceEntry[]
  finalState?: string
}>()

const progressPercentage = computed(() => {
  return Math.round((props.currentStep / 4) * 100)
})

const getStepClass = (step: number): string => {
  if (props.currentStep > step) return 'completed'
  if (props.currentStep === step) return 'active'
  return 'pending'
}

const getStepStatus = (step: number): string => {
  if (props.currentStep > step) return '✓ Done'
  if (props.currentStep === step) return '⏳ In Progress...'
  return '⏸ Pending'
}

const getNodeIcon = (nodeType: string): string => {
  const icons: Record<string, string> = {
    'StartNode': '🚀',
    'AiDecisionNode': '🧠',
    'LogicDecisionNode': '🔀',
    'ActionNode': '⚡',
    'EndNode': '🏁'
  }
  return icons[nodeType] || '📦'
}

const getFinalIcon = (state: string): string => {
  if (state === 'APPROVED') return '✅'
  if (state === 'REJECTED') return '❌'
  if (state === 'PENDING_APPROVAL') return '⏳'
  return '🎯'
}

const formatValue = (value: any): string => {
  if (typeof value === 'string' && value.length > 20) {
    return value.substring(0, 20) + '...'
  }
  return String(value)
}
</script>

<style scoped>
.execution-progress {
  margin: 2rem 0;
  padding: 2rem;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border-radius: 12px;
  border: 2px solid #667eea;
}

.execution-progress h3 {
  color: #667eea;
  margin-bottom: 2rem;
  text-align: center;
  font-size: 1.5rem;
}

.progress-steps {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.progress-step {
  display: flex;
  gap: 1.5rem;
  padding: 1.5rem;
  background: white;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.progress-step.pending {
  opacity: 0.6;
}

.progress-step.active {
  background: linear-gradient(135deg, #fff9e6 0%, #ffe6f0 100%);
  border-left: 4px solid #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.progress-step.completed {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  border-left: 4px solid #4caf50;
}

.step-number {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 1.2rem;
  background: #e0e0e0;
  color: #666;
}

.progress-step.active .step-number {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  animation: pulse 1.5s ease-in-out infinite;
}

.progress-step.completed .step-number {
  background: #4caf50;
  color: white;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.7);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 0 0 10px rgba(102, 126, 234, 0);
  }
}

.step-content {
  flex: 1;
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.step-title {
  font-weight: 600;
  font-size: 1.1rem;
  color: #333;
}

.step-status {
  font-size: 0.9rem;
  color: #666;
  font-weight: 500;
}

.step-description {
  color: #777;
  font-size: 0.9rem;
  margin-bottom: 0.75rem;
}

.step-result {
  margin-top: 1rem;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 6px;
  border-left: 3px solid #667eea;
}

.parsed-summary,
.workflow-summary,
.execution-summary {
  font-size: 0.95rem;
  color: #333;
}

.field-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.field-tag {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: #e3f2fd;
  color: #1976d2;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 500;
}

.node-list {
  margin-top: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  background: white;
  border-radius: 4px;
  font-size: 0.85rem;
}

.node-icon {
  font-size: 1.2rem;
}

.node-name {
  flex: 1;
  font-weight: 500;
  color: #555;
}

.node-status {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
}

.node-status.success {
  background: #d4edda;
  color: #155724;
}

.node-status.failure {
  background: #f8d7da;
  color: #721c24;
}

.final-summary {
  padding: 1rem;
  border-radius: 6px;
  font-size: 1.1rem;
  text-align: center;
}

.final-summary.approved {
  background: #d4edda;
  color: #155724;
}

.final-summary.rejected {
  background: #f8d7da;
  color: #721c24;
}

.final-summary.pending_approval {
  background: #fff3cd;
  color: #856404;
}

.step-connector {
  height: 30px;
  width: 2px;
  background: #e0e0e0;
  margin-left: calc(1.5rem + 19px);
  transition: background 0.3s ease;
}

.step-connector.active {
  background: linear-gradient(180deg, #4caf50 0%, #667eea 100%);
}

.overall-progress {
  margin-top: 2rem;
}

.progress-bar {
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #4caf50 100%);
  transition: width 0.5s ease;
}

.progress-text {
  text-align: center;
  margin-top: 0.5rem;
  color: #667eea;
  font-weight: 600;
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .execution-progress {
    padding: 1.5rem;
  }

  .progress-step {
    padding: 1rem;
  }

  .step-title {
    font-size: 1rem;
  }

  .field-preview {
    flex-direction: column;
  }
}
</style>

