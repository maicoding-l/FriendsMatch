import { ref, onUnmounted } from 'vue'

export interface ChatMessage {
  id?: number
  fromUserId?: number
  fromUsername?: string
  fromUserAvatar?: string
  toUserId?: number
  toUsername?: string
  toUserAvatar?: string
  content: string
  type?: number
  status?: number
  createTime?: Date
}

type MessageHandler = (message: ChatMessage) => void
type ErrorHandler = (error: any) => void
type ConnectionHandler = () => void

export function useWebSocket() {
  const ws = ref<WebSocket | null>(null)
  const isConnected = ref(false)
  const messageHandlers: MessageHandler[] = []
  const errorHandlers: ErrorHandler[] = []
  const connectionHandlers: ConnectionHandler[] = []
  const disconnectionHandlers: ConnectionHandler[] = []

  const connect = () => {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      return
    }

    // 使用相对路径，让浏览器自动选择 ws:// 或 wss://
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/api/ws/chat`

    ws.value = new WebSocket(wsUrl)

    ws.value.onopen = () => {
      console.log('WebSocket 已连接')
      isConnected.value = true
      connectionHandlers.forEach(handler => handler())
    }

    ws.value.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        messageHandlers.forEach(handler => handler(message))
      } catch (error) {
        console.error('解析消息失败:', error)
        errorHandlers.forEach(handler => handler(error))
      }
    }

    ws.value.onerror = (error) => {
      console.error('WebSocket 错误:', error)
      errorHandlers.forEach(handler => handler(error))
    }

    ws.value.onclose = () => {
      console.log('WebSocket 已断开')
      isConnected.value = false
      disconnectionHandlers.forEach(handler => handler())
    }
  }

  const disconnect = () => {
    if (ws.value) {
      ws.value.close()
      ws.value = null
      isConnected.value = false
    }
  }

  const sendMessage = (message: { toUserId: number; content: string; type?: number }) => {
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket 未连接')
    }

    ws.value.send(JSON.stringify(message))
  }

  const onMessage = (handler: MessageHandler) => {
    messageHandlers.push(handler)
  }

  const onError = (handler: ErrorHandler) => {
    errorHandlers.push(handler)
  }

  const onConnect = (handler: ConnectionHandler) => {
    connectionHandlers.push(handler)
  }

  const onDisconnect = (handler: ConnectionHandler) => {
    disconnectionHandlers.push(handler)
  }

  // 组件卸载时自动断开连接
  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected,
    connect,
    disconnect,
    sendMessage,
    onMessage,
    onError,
    onConnect,
    onDisconnect
  }
}
