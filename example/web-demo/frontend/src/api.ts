import axios from 'axios'
import type { ParseRequest, ParsingResult } from './types'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
})

export const parseForm = async (request: ParseRequest): Promise<ParsingResult> => {
  const response = await api.post<ParsingResult>('/forms/parse', request)
  return response.data
}

export type StreamEventName =
  | 'attemptStarted'
  | 'rawChunk'
  | 'snapshot'
  | 'attemptFailed'
  | 'finalResult'
  | 'error'

export type StreamEventHandler = (eventName: StreamEventName, data: any) => void

/**
 * Streaming parse via SSE (POST + text/event-stream).
 *
 * Note: browsers' EventSource only supports GET, so we use fetch() and parse SSE manually.
 */
export const parseFormStream = async (
  request: ParseRequest,
  onEvent: StreamEventHandler,
  signal?: AbortSignal
): Promise<void> => {
  const resp = await fetch('/api/forms/parse/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
    signal
  })

  if (!resp.ok || !resp.body) {
    const text = await resp.text().catch(() => '')
    throw new Error(text || `Streaming request failed: ${resp.status}`)
  }

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')

  let buffer = ''
  let currentEvent: StreamEventName | null = null

  const emitDataLine = (dataLine: string) => {
    // "data:" lines can be split; Spring usually sends a single JSON line.
    const payload = dataLine.startsWith('data:') ? dataLine.slice(5).trim() : dataLine.trim()
    if (!payload) return
    try {
      const obj = JSON.parse(payload)
      if (currentEvent) {
        onEvent(currentEvent, obj)
      }
    } catch {
      // If payload isn't JSON (unexpected), forward as raw string
      if (currentEvent) {
        onEvent(currentEvent, payload)
      }
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })

    // SSE frames are separated by blank line
    let idx: number
    while ((idx = buffer.indexOf('\n\n')) !== -1) {
      const frame = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)

      const lines = frame.split('\n').map(l => l.replace(/\r$/, ''))
      currentEvent = null
      for (const line of lines) {
        if (line.startsWith('event:')) {
          const name = line.slice(6).trim() as StreamEventName
          currentEvent = name
        } else if (line.startsWith('data:')) {
          emitDataLine(line)
        }
      }
    }
  }
}

export const getSchema = async (formType: 'leave' | 'task'): Promise<string> => {
  const response = await api.get<string>(`/forms/schema/${formType}`)
  return response.data
}

