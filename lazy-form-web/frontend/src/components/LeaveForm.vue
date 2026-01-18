<template>
  <div class="leave-form">
    <div class="form-grid">
      <div class="form-field">
        <label>Leave Type *</label>
        <select v-model="formData.leave_type" :class="{ filled: hasValue('leave_type') }">
          <option value="">Select...</option>
          <option value="annual">Annual</option>
          <option value="sick">Sick</option>
          <option value="unpaid">Unpaid</option>
          <option value="marriage">Marriage</option>
        </select>
        <FieldInfo v-if="getField('leave_type')" :field="getField('leave_type')!" />
      </div>

      <div class="form-field">
        <label>Start Date *</label>
        <input
          type="date"
          v-model="formData.start_date"
          :class="{ filled: hasValue('start_date') }"
        />
        <FieldInfo v-if="getField('start_date')" :field="getField('start_date')!" />
      </div>

      <div class="form-field">
        <label>End Date *</label>
        <input
          type="date"
          v-model="formData.end_date"
          :class="{ filled: hasValue('end_date') }"
        />
        <FieldInfo v-if="getField('end_date')" :field="getField('end_date')!" />
      </div>

      <div class="form-field full-width">
        <label>Reason *</label>
        <textarea
          v-model="formData.reason"
          rows="3"
          :class="{ filled: hasValue('reason') }"
        ></textarea>
        <FieldInfo v-if="getField('reason')" :field="getField('reason')!" />
      </div>

      <div class="form-field">
        <label>Approver</label>
        <input
          type="text"
          v-model="formData.approver"
          :class="{ filled: hasValue('approver') }"
        />
        <FieldInfo v-if="getField('approver')" :field="getField('approver')!" />
      </div>

      <div class="form-field">
        <label>Medical Certificate</label>
        <input
          type="text"
          v-model="formData.medical_certificate"
          placeholder="If applicable"
          :class="{ filled: hasValue('medical_certificate') }"
        />
        <FieldInfo v-if="getField('medical_certificate')" :field="getField('medical_certificate')!" />
      </div>
    </div>

    <button class="submit-btn" @click="handleSubmit">
      📝 Submit Leave Request
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ParsingResult, LeaveFormData, FieldResult } from '../types'
import FieldInfo from './FieldInfo.vue'

const props = defineProps<{
  parsingResult: ParsingResult | null
}>()

const formData = ref<LeaveFormData>({})

// Initialize form data from parsing result
watch(() => props.parsingResult, (result) => {
  console.log('LeaveForm: parsingResult changed:', result)
  if (result && result.fields) {
    formData.value = {}
    try {
      Object.keys(result.fields).forEach(key => {
        const value = result.fields[key].value
        console.log(`Mapping field: ${key} = ${value}`)
        formData.value[key as keyof LeaveFormData] = value
      })
      console.log('LeaveForm: formData updated:', formData.value)
    } catch (error) {
      console.error('Error updating LeaveForm data:', error)
    }
  }
}, { immediate: true })

const getField = (fieldName: string): FieldResult | null => {
  return props.parsingResult?.fields?.[fieldName] || null
}

const hasValue = (fieldName: string): boolean => {
  return formData.value[fieldName as keyof LeaveFormData] != null &&
         formData.value[fieldName as keyof LeaveFormData] !== ''
}

const handleSubmit = () => {
  console.log('Submitting leave request:', formData.value)
  alert('Leave request submitted! (This is a demo)')
}
</script>

<style scoped>
.leave-form {
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

