<template>
  <div class="task-form">
    <div class="form-grid">
      <div class="form-field full-width">
        <label>Task Name *</label>
        <input
          type="text"
          v-model="formData.name"
          :class="{ filled: hasValue('name') }"
        />
        <FieldInfo v-if="getField('name')" :field="getField('name')!" />
      </div>

      <div class="form-field full-width">
        <label>Description</label>
        <textarea
          v-model="formData.description"
          rows="2"
          :class="{ filled: hasValue('description') }"
        ></textarea>
        <FieldInfo v-if="getField('description')" :field="getField('description')!" />
      </div>

      <div class="form-field">
        <label>Priority</label>
        <select v-model="formData.priority" :class="{ filled: hasValue('priority') }">
          <option value="">Select...</option>
          <option value="1">1 - Low</option>
          <option value="2">2</option>
          <option value="3">3 - Medium</option>
          <option value="4">4</option>
          <option value="5">5 - High</option>
        </select>
        <FieldInfo v-if="getField('priority')" :field="getField('priority')!" />
      </div>

      <div class="form-field">
        <label>Difficulty</label>
        <select v-model="formData.difficulty" :class="{ filled: hasValue('difficulty') }">
          <option value="">Select...</option>
          <option value="1">1 - Easy</option>
          <option value="2">2</option>
          <option value="3">3 - Medium</option>
          <option value="4">4</option>
          <option value="5">5 - Hard</option>
        </select>
        <FieldInfo v-if="getField('difficulty')" :field="getField('difficulty')!" />
      </div>

      <div class="form-field">
        <label>Schedule Time</label>
        <input
          type="datetime-local"
          v-model="formData.schedule_time"
          :class="{ filled: hasValue('schedule_time') }"
        />
        <FieldInfo v-if="getField('schedule_time')" :field="getField('schedule_time')!" />
      </div>

      <div class="form-field">
        <label>Deadline</label>
        <input
          type="datetime-local"
          v-model="formData.deadline"
          :class="{ filled: hasValue('deadline') }"
        />
        <FieldInfo v-if="getField('deadline')" :field="getField('deadline')!" />
      </div>

      <div class="form-field">
        <label>Estimated Time (minutes)</label>
        <input
          type="number"
          v-model.number="formData.minutes"
          :class="{ filled: hasValue('minutes') }"
        />
        <FieldInfo v-if="getField('minutes')" :field="getField('minutes')!" />
      </div>

      <div class="form-field">
        <label>Status</label>
        <select v-model="formData.status" :class="{ filled: hasValue('status') }">
          <option value="">Select...</option>
          <option value="pending">Pending</option>
          <option value="in_progress">In Progress</option>
          <option value="completed">Completed</option>
          <option value="cancelled">Cancelled</option>
        </select>
        <FieldInfo v-if="getField('status')" :field="getField('status')!" />
      </div>

      <div class="form-field full-width">
        <label>Tags</label>
        <input
          type="text"
          v-model="formData.tags"
          placeholder="Comma-separated tags"
          :class="{ filled: hasValue('tags') }"
        />
        <FieldInfo v-if="getField('tags')" :field="getField('tags')!" />
      </div>
    </div>

    <button class="submit-btn" @click="handleSubmit">
      ✅ Create Task
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ParsingResult, TaskFormData, FieldResult } from '../types'
import FieldInfo from './FieldInfo.vue'

const props = defineProps<{
  parsingResult: ParsingResult | null
}>()

const formData = ref<TaskFormData>({})

// Initialize form data from parsing result
watch(() => props.parsingResult, (result) => {
  console.log('TaskForm: parsingResult changed:', result)
  if (result && result.fields) {
    formData.value = {}
    try {
      Object.keys(result.fields).forEach(key => {
        let value = result.fields[key].value
        
        console.log(`TaskForm: Mapping field: ${key} = ${value} (type: ${typeof value})`)
        
        // Convert ISO datetime to HTML datetime-local format
        if ((key === 'schedule_time' || key === 'deadline') && value) {
          value = convertToDatetimeLocal(value)
        }
        
        formData.value[key as keyof TaskFormData] = value
      })
      console.log('TaskForm: formData updated:', formData.value)
    } catch (error) {
      console.error('Error updating TaskForm data:', error)
    }
  }
}, { immediate: true })

const convertToDatetimeLocal = (isoString: any): string => {
  try {
    if (!isoString) {
      return ''
    }
    
    const str = String(isoString)
    
    // If it's already in the right format or doesn't have time component
    if (str.length < 16) {
      return str
    }
    
    // Convert "2023-10-30T09:00:00Z" to "2023-10-30T09:00"
    // or "2023-10-30T09:00:00+00:00" to "2023-10-30T09:00"
    const dateTimePart = str.substring(0, 16)
    
    // Validate it looks like a datetime
    if (dateTimePart.includes('T') && dateTimePart.length === 16) {
      return dateTimePart
    }
    
    return str
  } catch (error) {
    console.error('Error converting datetime:', error, 'value:', isoString)
    return String(isoString || '')
  }
}

const getField = (fieldName: string): FieldResult | null => {
  return props.parsingResult?.fields?.[fieldName] || null
}

const hasValue = (fieldName: string): boolean => {
  return formData.value[fieldName as keyof TaskFormData] != null &&
         formData.value[fieldName as keyof TaskFormData] !== ''
}

const handleSubmit = () => {
  console.log('Creating task:', formData.value)
  alert('Task created! (This is a demo)')
}
</script>

<style scoped>
.task-form {
  width: 100%;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field.full-width {
  grid-column: 1 / -1;
}

.form-field label {
  font-weight: 600;
  color: #333;
  font-size: 0.9rem;
}

.form-field input,
.form-field select,
.form-field textarea {
  padding: 0.75rem;
  border: 2px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.3s;
}

.form-field input:focus,
.form-field select:focus,
.form-field textarea:focus {
  outline: none;
  border-color: #667eea;
}

.form-field input.filled,
.form-field select.filled,
.form-field textarea.filled {
  background: linear-gradient(to right, #f0f9ff 0%, #e0f2fe 100%);
  border-color: #0ea5e9;
}

.submit-btn {
  width: 100%;
  padding: 1rem 2rem;
  font-size: 1.1rem;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}
</style>

