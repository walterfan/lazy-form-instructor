<template>
  <div class="workflow-trail">
    <h3>🛤️ Execution Trail</h3>
    <p class="trail-description">Path taken through the workflow</p>
    
    <div class="trail-container">
      <div class="trail-path">
        <div 
          v-for="(entry, index) in trace" 
          :key="index"
          class="trail-segment"
        >
          <!-- Node -->
          <div 
            class="trail-node"
            :class="[getNodeTypeClass(entry.nodeType), getStatusClass(entry.status)]"
            :title="`${entry.nodeId} (${entry.nodeType})`"
          >
            <div class="node-icon">{{ getNodeIcon(entry.nodeType) }}</div>
            <div class="node-label">{{ entry.nodeId }}</div>
            <div class="node-status-badge" :class="entry.status.toLowerCase()">
              {{ entry.status }}
            </div>
          </div>

          <!-- Arrow (if not last) -->
          <div v-if="index < trace.length - 1" class="trail-arrow">
            <svg width="60" height="40" viewBox="0 0 60 40">
              <defs>
                <marker
                  id="arrowhead"
                  markerWidth="10"
                  markerHeight="10"
                  refX="9"
                  refY="3"
                  orient="auto"
                >
                  <polygon points="0 0, 10 3, 0 6" fill="#667eea" />
                </marker>
              </defs>
              <line
                x1="5"
                y1="20"
                x2="55"
                y2="20"
                stroke="#667eea"
                stroke-width="3"
                marker-end="url(#arrowhead)"
                class="arrow-line"
              />
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- Trail Summary -->
    <div class="trail-summary">
      <div class="summary-item">
        <span class="summary-label">Total Nodes:</span>
        <span class="summary-value">{{ trace.length }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">Path:</span>
        <span class="summary-value path-text">{{ getPathString() }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">Duration:</span>
        <span class="summary-value">{{ getDuration() }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { TraceEntry, NodeStatus } from '../types'

const props = defineProps<{
  trace: TraceEntry[]
}>()

const getNodeIcon = (nodeType: string): string => {
  const icons: Record<string, string> = {
    'StartNode': '🚀',
    'EndNode': '🏁',
    'AiDecisionNode': '🧠',
    'LogicDecisionNode': '🔀',
    'ActionNode': '⚡',
    'AiProcessNode': '🤖',
    'HumanTaskNode': '👤'
  }
  return icons[nodeType] || '📦'
}

const getNodeTypeClass = (nodeType: string): string => {
  if (nodeType === 'StartNode') return 'start-node'
  if (nodeType === 'EndNode') return 'end-node'
  if (nodeType === 'AiDecisionNode') return 'ai-node'
  if (nodeType === 'LogicDecisionNode') return 'decision-node'
  if (nodeType === 'ActionNode') return 'action-node'
  return 'default-node'
}

const getStatusClass = (status: NodeStatus): string => {
  return `status-${status.toLowerCase()}`
}

const getPathString = (): string => {
  return props.trace.map(entry => entry.nodeId).join(' → ')
}

const getDuration = (): string => {
  if (props.trace.length < 2) return 'N/A'
  
  try {
    const start = new Date(props.trace[0].timestamp).getTime()
    const end = new Date(props.trace[props.trace.length - 1].timestamp).getTime()
    const duration = end - start
    
    if (duration < 1000) return `${duration}ms`
    return `${(duration / 1000).toFixed(2)}s`
  } catch {
    return 'N/A'
  }
}
</script>

<style scoped>
.workflow-trail {
  margin: 2rem 0;
  padding: 2rem;
  background: white;
  border-radius: 12px;
  border: 2px solid #e9ecef;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.workflow-trail h3 {
  color: #333;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.trail-description {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 1.5rem;
}

.trail-container {
  overflow-x: auto;
  padding: 1rem 0;
}

.trail-path {
  display: flex;
  align-items: center;
  min-width: min-content;
  padding: 1rem;
}

.trail-segment {
  display: flex;
  align-items: center;
}

.trail-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  border-radius: 12px;
  min-width: 120px;
  position: relative;
  transition: all 0.3s ease;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.trail-node:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

/* Node type styles */
.trail-node.start-node {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.trail-node.end-node {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.trail-node.ai-node {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: white;
}

.trail-node.decision-node {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #333;
}

.trail-node.action-node {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.trail-node.default-node {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

/* Status-based border */
.trail-node.status-success {
  border: 3px solid #4caf50;
}

.trail-node.status-failure {
  border: 3px solid #f44336;
}

.trail-node.status-waiting {
  border: 3px solid #ff9800;
}

.node-icon {
  font-size: 2rem;
}

.node-label {
  font-weight: 600;
  font-size: 0.9rem;
  text-align: center;
  word-break: break-word;
  max-width: 100px;
}

.node-status-badge {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
}

.node-status-badge.success {
  background: rgba(76, 175, 80, 0.9);
  color: white;
}

.node-status-badge.failure {
  background: rgba(244, 67, 54, 0.9);
  color: white;
}

.node-status-badge.waiting {
  background: rgba(255, 152, 0, 0.9);
  color: white;
}

.trail-arrow {
  margin: 0 0.5rem;
}

.arrow-line {
  animation: flowAnimation 2s linear infinite;
}

@keyframes flowAnimation {
  0% {
    stroke-dasharray: 0, 100;
    stroke-dashoffset: 0;
  }
  50% {
    stroke-dasharray: 50, 50;
    stroke-dashoffset: -25;
  }
  100% {
    stroke-dasharray: 0, 100;
    stroke-dashoffset: -50;
  }
}

.trail-summary {
  margin-top: 2rem;
  padding: 1.5rem;
  background: #f8f9fa;
  border-radius: 8px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.summary-label {
  font-size: 0.85rem;
  color: #666;
  font-weight: 500;
}

.summary-value {
  font-size: 1.1rem;
  color: #333;
  font-weight: 600;
}

.path-text {
  font-family: 'Courier New', monospace;
  font-size: 0.95rem;
  color: #667eea;
  word-break: break-all;
  line-height: 1.5;
}

/* Mobile responsive */
@media (max-width: 768px) {
  .trail-path {
    flex-direction: column;
    align-items: stretch;
  }

  .trail-segment {
    flex-direction: column;
    width: 100%;
  }

  .trail-node {
    width: 100%;
    min-width: 0;
  }

  .trail-arrow {
    margin: 0.5rem 0;
  }

  .trail-arrow svg {
    transform: rotate(90deg);
    width: 40px;
    height: 60px;
  }

  .trail-summary {
    grid-template-columns: 1fr;
  }
}

/* Print styles */
@media print {
  .workflow-trail {
    page-break-inside: avoid;
  }

  .trail-node:hover {
    transform: none;
  }

  .arrow-line {
    animation: none;
  }
}
</style>

