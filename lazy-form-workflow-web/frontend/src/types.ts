export interface WorkflowExecutionRequest {
  userInput: string
  workflowType: string
}

export interface FieldResult {
  value: any
  confidence: number
  reasoning: string
  alternatives?: any[]
}

export interface TraceEntry {
  timestamp: string
  nodeId: string
  nodeType: string
  inputSummary?: string
  outputPayload?: any
  status: NodeStatus
}

export enum NodeStatus {
  SUCCESS = 'SUCCESS',
  FAILURE = 'FAILURE',
  WAITING = 'WAITING'
}

export enum WorkflowStatus {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  WAITING = 'WAITING'
}

export interface WorkflowExecutionResponse {
  status: WorkflowStatus
  message: string
  parsedFields: Record<string, FieldResult> | null
  executionTrace: TraceEntry[]
  finalContext: {
    state: string
    variables: Record<string, any>
  }
}

export interface WorkflowTypeInfo {
  name: string
  description: string
  exampleInput: string
}

