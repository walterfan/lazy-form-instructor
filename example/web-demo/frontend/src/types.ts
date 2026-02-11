export interface ParseRequest {
  formType: 'leave' | 'task'
  userInput: string
}

export interface FieldResult {
  value: any
  confidence: number
  reasoning: string
  alternatives?: any[]
}

export interface ValidationError {
  path: string
  message: string
  type: string
}

export interface ParsingResult {
  fields: Record<string, FieldResult>
  errors: ValidationError[]
}

export interface LeaveFormData {
  leave_type?: string
  start_date?: string
  end_date?: string
  reason?: string
  medical_certificate?: string
  approver?: string
}

export interface TaskFormData {
  name?: string
  description?: string
  priority?: string
  difficulty?: string
  status?: string
  schedule_time?: string
  minutes?: number
  deadline?: string
  start_time?: string
  end_time?: string
  tags?: string
}

