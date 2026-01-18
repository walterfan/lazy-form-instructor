<template>
  <div class="workflow-diagram">
    <h3>📊 Workflow Diagram</h3>
    
    <div v-if="workflowType === 'leave_request'" class="diagram-container">
      <div class="diagram-title">Leave Request Approval Workflow</div>
      
      <!-- Workflow visualization -->
      <div class="flow-diagram">
        <!-- Start Node -->
        <div class="flow-node start-node">
          <div class="node-icon">🚀</div>
          <div class="node-label">Start</div>
          <div class="node-desc">Initialize workflow</div>
        </div>

        <div class="flow-arrow">↓</div>

        <!-- Parse Node -->
        <div class="flow-node process-node">
          <div class="node-icon">🤖</div>
          <div class="node-label">Parse Request</div>
          <div class="node-desc">Extract leave details using AI</div>
        </div>

        <div class="flow-arrow">↓</div>

        <!-- AI Validation Node -->
        <div class="flow-node ai-node">
          <div class="node-icon">🧠</div>
          <div class="node-label">AI Validation</div>
          <div class="node-desc">Evaluate request validity & reasoning</div>
        </div>

        <div class="flow-arrow">↓</div>

        <!-- Decision Point -->
        <div class="flow-decision">
          <div class="decision-node">
            <div class="node-icon">🔀</div>
            <div class="node-label">Decision Router</div>
            <div class="node-desc">Based on AI confidence & decision</div>
          </div>

          <!-- Three branches -->
          <div class="branches">
            <div class="branch">
              <div class="branch-label high-confidence">
                ✅ High Confidence<br/>APPROVE (≥85%)
              </div>
              <div class="flow-arrow-small">↓</div>
              <div class="flow-node approve-node">
                <div class="node-icon">👍</div>
                <div class="node-label">Auto Approve</div>
                <div class="node-desc">Request approved</div>
              </div>
            </div>

            <div class="branch">
              <div class="branch-label low-confidence">
                ⚠️ Low Confidence<br/>or ESCALATE
              </div>
              <div class="flow-arrow-small">↓</div>
              <div class="flow-node review-node">
                <div class="node-icon">👤</div>
                <div class="node-label">Human Review</div>
                <div class="node-desc">Send to manager</div>
              </div>
            </div>

            <div class="branch">
              <div class="branch-label high-confidence-reject">
                ❌ High Confidence<br/>REJECT (≥85%)
              </div>
              <div class="flow-arrow-small">↓</div>
              <div class="flow-node reject-node">
                <div class="node-icon">🚫</div>
                <div class="node-label">Auto Reject</div>
                <div class="node-desc">Request rejected</div>
              </div>
            </div>
          </div>

          <!-- Converge arrows -->
          <div class="converge-arrows">
            <div class="converge-line left"></div>
            <div class="converge-line center"></div>
            <div class="converge-line right"></div>
          </div>
        </div>

        <div class="flow-arrow">↓</div>

        <!-- End Node -->
        <div class="flow-node end-node">
          <div class="node-icon">🏁</div>
          <div class="node-label">End</div>
          <div class="node-desc">Workflow complete</div>
        </div>
      </div>

      <!-- Legend -->
      <div class="diagram-legend">
        <h4>Legend:</h4>
        <div class="legend-items">
          <div class="legend-item">
            <div class="legend-box start-node"></div>
            <span>Start/End</span>
          </div>
          <div class="legend-item">
            <div class="legend-box process-node"></div>
            <span>Process</span>
          </div>
          <div class="legend-item">
            <div class="legend-box ai-node"></div>
            <span>AI Decision</span>
          </div>
          <div class="legend-item">
            <div class="legend-box decision-node"></div>
            <span>Router</span>
          </div>
          <div class="legend-item">
            <div class="legend-box approve-node"></div>
            <span>Approve</span>
          </div>
          <div class="legend-item">
            <div class="legend-box review-node"></div>
            <span>Review</span>
          </div>
          <div class="legend-item">
            <div class="legend-box reject-node"></div>
            <span>Reject</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="no-diagram">
      <p>Select a workflow type to view its diagram</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  workflowType: string
}>()
</script>

<style scoped>
.workflow-diagram {
  margin: 2rem 0;
  padding: 2rem;
  background: #f8f9fa;
  border-radius: 12px;
  border: 2px solid #e9ecef;
}

.workflow-diagram h3 {
  color: #333;
  margin-bottom: 1.5rem;
  text-align: center;
}

.diagram-container {
  background: white;
  padding: 2rem;
  border-radius: 8px;
}

.diagram-title {
  text-align: center;
  font-size: 1.3rem;
  font-weight: 600;
  color: #667eea;
  margin-bottom: 2rem;
}

.flow-diagram {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.flow-node {
  min-width: 200px;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease;
}

.flow-node:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.node-icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.node-label {
  font-weight: 600;
  font-size: 1.1rem;
  color: #333;
  margin-bottom: 0.25rem;
}

.node-desc {
  font-size: 0.85rem;
  color: #666;
  font-style: italic;
}

.start-node, .end-node {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 50%;
  width: 200px;
  height: 200px;
  padding: 2rem 1rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-width: 0;
}

.start-node .node-label,
.start-node .node-desc,
.end-node .node-label,
.end-node .node-desc {
  color: white;
}

.process-node {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.process-node .node-label,
.process-node .node-desc {
  color: white;
}

.ai-node {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: white;
}

.ai-node .node-label,
.ai-node .node-desc {
  color: white;
}

.decision-node {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  border: 3px solid #667eea;
  width: 180px;
  height: 180px;
  transform: rotate(45deg);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.decision-node > * {
  transform: rotate(-45deg);
}

.decision-node .node-icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.decision-node .node-label {
  font-weight: 600;
  font-size: 1rem;
  color: #333;
  margin-bottom: 0.25rem;
}

.decision-node .node-desc {
  font-size: 0.75rem;
  color: #666;
  font-style: italic;
  max-width: 120px;
  line-height: 1.2;
}

.approve-node {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
}

.approve-node .node-label,
.approve-node .node-desc {
  color: white;
}

.review-node {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.review-node .node-label,
.review-node .node-desc {
  color: white;
}

.reject-node {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
  color: white;
}

.reject-node .node-label,
.reject-node .node-desc {
  color: white;
}

.flow-arrow {
  font-size: 2rem;
  color: #667eea;
  font-weight: bold;
  line-height: 1;
}

.flow-arrow-small {
  font-size: 1.5rem;
  color: #667eea;
  font-weight: bold;
  line-height: 1;
}

.flow-decision {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 1rem 0;
}

.flow-decision .decision-node {
  margin-bottom: 1rem;
}

.branches {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2rem;
  width: 100%;
  max-width: 900px;
  margin: 1rem 0;
}

.branch {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.branch-label {
  padding: 0.75rem 1rem;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  text-align: center;
  white-space: nowrap;
  line-height: 1.4;
}

.branch-label.high-confidence {
  background: #d4edda;
  color: #155724;
  border: 2px solid #28a745;
}

.branch-label.low-confidence {
  background: #fff3cd;
  color: #856404;
  border: 2px solid #ffc107;
}

.branch-label.high-confidence-reject {
  background: #f8d7da;
  color: #721c24;
  border: 2px solid #dc3545;
}

.converge-arrows {
  display: flex;
  justify-content: center;
  gap: 2rem;
  width: 100%;
  max-width: 900px;
  margin: 0.5rem 0;
}

.converge-line {
  width: 2px;
  height: 40px;
  background: #667eea;
  position: relative;
}

.converge-line::after {
  content: '↓';
  position: absolute;
  bottom: -20px;
  left: 50%;
  transform: translateX(-50%);
  color: #667eea;
  font-size: 1.5rem;
  font-weight: bold;
}

.converge-line.center::after {
  bottom: -10px;
}

.diagram-legend {
  margin-top: 2rem;
  padding: 1.5rem;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #dee2e6;
}

.diagram-legend h4 {
  margin-bottom: 1rem;
  color: #333;
  font-size: 1rem;
}

.legend-items {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.legend-box {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.legend-box.start-node,
.legend-box.end-node {
  border-radius: 50%;
}

.legend-box.decision-node {
  transform: rotate(45deg);
  border-radius: 0;
}

.no-diagram {
  text-align: center;
  padding: 3rem;
  color: #999;
  font-style: italic;
}

@media (max-width: 768px) {
  .branches {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .converge-arrows {
    display: none;
  }

  .flow-node {
    min-width: 150px;
    padding: 1rem;
  }

  .start-node, .end-node {
    width: 160px;
    height: 160px;
    padding: 1.5rem 0.5rem;
  }

  .decision-node {
    width: 140px;
    height: 140px;
  }

  .decision-node .node-label {
    font-size: 0.9rem;
  }

  .decision-node .node-desc {
    font-size: 0.7rem;
    max-width: 100px;
  }

  .node-icon {
    font-size: 1.5rem;
  }

  .node-label {
    font-size: 1rem;
  }

  .node-desc {
    font-size: 0.75rem;
  }
}
</style>

