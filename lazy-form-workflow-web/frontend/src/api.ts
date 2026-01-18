import axios from 'axios'
import type { WorkflowExecutionRequest, WorkflowExecutionResponse, WorkflowTypeInfo } from './types'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
})

export const executeWorkflow = async (request: WorkflowExecutionRequest): Promise<WorkflowExecutionResponse> => {
  const response = await api.post<WorkflowExecutionResponse>('/workflow/execute', request)
  return response.data
}

export const getWorkflowTypes = async (): Promise<Record<string, WorkflowTypeInfo>> => {
  const response = await api.get<Record<string, WorkflowTypeInfo>>('/workflow/types')
  return response.data
}

