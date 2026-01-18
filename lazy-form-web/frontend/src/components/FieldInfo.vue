<template>
  <div class="field-info">
    <div class="confidence-bar">
      <div
        class="confidence-fill"
        :style="{ width: `${field.confidence * 100}%` }"
        :class="confidenceClass"
      ></div>
    </div>
    <div class="info-details">
      <span class="confidence-text" :class="confidenceClass">
        {{ confidenceIcon }} {{ (field.confidence * 100).toFixed(0) }}% confident
      </span>
      <p class="reasoning">{{ field.reasoning }}</p>
      <div v-if="field.alternatives && field.alternatives.length > 0" class="alternatives">
        <strong>Alternatives:</strong> {{ field.alternatives.join(', ') }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FieldResult } from '../types'

const props = defineProps<{
  field: FieldResult
}>()

const confidenceClass = computed(() => {
  if (props.field.confidence >= 0.9) return 'high'
  if (props.field.confidence >= 0.7) return 'medium'
  return 'low'
})

const confidenceIcon = computed(() => {
  if (props.field.confidence >= 0.9) return '🟢'
  if (props.field.confidence >= 0.7) return '🟡'
  return '🔴'
})
</script>

<style scoped>
.field-info {
  margin-top: 0.5rem;
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 0.85rem;
}

.confidence-bar {
  height: 4px;
  background: #e9ecef;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 0.5rem;
}

.confidence-fill {
  height: 100%;
  transition: width 0.3s ease;
}

.confidence-fill.high {
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
}

.confidence-fill.medium {
  background: linear-gradient(90deg, #f59e0b 0%, #d97706 100%);
}

.confidence-fill.low {
  background: linear-gradient(90deg, #ef4444 0%, #dc2626 100%);
}

.info-details {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.confidence-text {
  font-weight: 600;
  font-size: 0.8rem;
}

.confidence-text.high {
  color: #059669;
}

.confidence-text.medium {
  color: #d97706;
}

.confidence-text.low {
  color: #dc2626;
}

.reasoning {
  color: #6c757d;
  line-height: 1.4;
  margin: 0;
}

.alternatives {
  color: #6c757d;
  font-size: 0.8rem;
  padding-top: 0.25rem;
  border-top: 1px solid #dee2e6;
  margin-top: 0.25rem;
}
</style>

