<template>
  <div class="execution-trace">
    <h3>📊 Execution Trace</h3>
    <div class="trace-timeline">
      <div v-for="(entry, index) in trace" :key="index" class="trace-entry">
        <div class="trace-marker" :class="getStatusClass(entry.status)">
          <span class="trace-number">{{ index + 1 }}</span>
        </div>
        <div class="trace-content">
          <div class="trace-header">
            <span class="node-id">{{ entry.nodeId }}</span>
            <span class="node-type">{{ entry.nodeType }}</span>
            <span class="status-badge" :class="getStatusClass(entry.status)">
              {{ entry.status }}
            </span>
          </div>
          <div v-if="entry.outputPayload" class="trace-payload">
            <strong>Output:</strong>
            <div class="payload-content">
              {{ formatPayload(entry.outputPayload) }}
            </div>
          </div>
          <div class="trace-timestamp">
            {{ formatTimestamp(entry.timestamp) }}
          </div>
        </div>
        <div v-if="index < trace.length - 1" class="trace-connector"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { TraceEntry } from '../types'
import { NodeStatus } from '../types'

defineProps<{
  trace: TraceEntry[]
}>()

const getStatusClass = (status: NodeStatus): string => {
  switch (status) {
    case NodeStatus.SUCCESS:
      return 'success'
    case NodeStatus.FAILURE:
      return 'failure'
    case NodeStatus.WAITING:
      return 'waiting'
    default:
      return ''
  }
}

const formatTimestamp = (timestamp: string): string => {
  try {
    const date = new Date(timestamp)
    return date.toLocaleString()
  } catch {
    return timestamp
  }
}

const formatPayload = (payload: any): string => {
  if (typeof payload === 'string') {
    return payload
  }
  
  if (typeof payload === 'object') {
    // Special handling for AI decision payload
    if (payload.decision) {
      let result = `Decision: ${payload.decision}`
      if (payload.reasoning) {
        result += `\nReasoning: ${payload.reasoning}`
      }
      if (payload.confidence !== undefined) {
        result += `\nConfidence: ${(payload.confidence * 100).toFixed(0)}%`
      }
      return result
    }
    
    return JSON.stringify(payload, null, 2)
  }
  
  return String(payload)
}
</script>

<style scoped>
.execution-trace {
  margin: 2rem 0;
}

.execution-trace h3 {
  color: #333;
  margin-bottom: 1.5rem;
}

.trace-timeline {
  position: relative;
}

.trace-entry {
  position: relative;
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.trace-marker {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  z-index: 1;
}

.trace-marker.success {
  background: #28a745;
  color: white;
}

.trace-marker.failure {
  background: #dc3545;
  color: white;
}

.trace-marker.waiting {
  background: #ffc107;
  color: #333;
}

.trace-number {
  font-size: 0.9rem;
}

.trace-content {
  flex: 1;
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 8px;
  border-left: 4px solid #667eea;
}

.trace-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}

.node-id {
  font-weight: 600;
  color: #333;
  font-size: 1rem;
}

.node-type {
  font-size: 0.85rem;
  color: #666;
  background: white;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.status-badge {
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-badge.success {
  background: #d4edda;
  color: #155724;
}

.status-badge.failure {
  background: #f8d7da;
  color: #721c24;
}

.status-badge.waiting {
  background: #fff3cd;
  color: #856404;
}

.trace-payload {
  margin: 0.75rem 0;
}

.trace-payload strong {
  color: #555;
  font-size: 0.9rem;
}

.payload-content {
  background: white;
  padding: 0.75rem;
  border-radius: 4px;
  margin-top: 0.5rem;
  font-family: 'Courier New', monospace;
  font-size: 0.9rem;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}

.trace-timestamp {
  font-size: 0.8rem;
  color: #999;
  margin-top: 0.5rem;
}

.trace-connector {
  position: absolute;
  left: 19px;
  top: 40px;
  width: 2px;
  height: calc(100% + 1rem);
  background: #ddd;
  z-index: 0;
}
</style>

