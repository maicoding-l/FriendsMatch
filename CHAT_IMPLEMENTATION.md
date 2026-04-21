# 聊天功能实现详解

## 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         浏览器                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  ChatPage.vue (聊天页面)                             │  │
│  │    - 显示消息列表                                     │  │
│  │    - 输入框和发送按钮                                 │  │
│  │    - 调用 useWebSocket composable                    │  │
│  └──────────────┬───────────────────────────────────────┘  │
│                 │                                            │
│  ┌──────────────▼───────────────────────────────────────┐  │
│  │  useWebSocket.ts (WebSocket 管理)                    │  │
│  │    - 建立 WebSocket 连接                             │  │
│  │    - 发送消息                                         │  │
│  │    - 接收消息                                         │  │
│  │    - 连接状态管理                                     │  │
│  └──────────────┬───────────────────────────────────────┘  │
└─────────────────┼───────────────────────────────────────────┘
                  │ WebSocket 连接 (ws://)
                  │ 持久化双向通信
┌─────────────────▼───────────────────────────────────────────┐
│                    Vite 代理服务器                           │
│                    (localhost:5173)                          │
│                    ws: true                                  │
└─────────────────┬───────────────────────────────────────────┘
                  │ 转发
┌─────────────────▼───────────────────────────────────────────┐
│               Spring Boot 后端 (localhost:8080)              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  WebSocketConfig                                      │  │
│  │    - 注册 WebSocket 端点: /ws/chat                    │  │
│  │    - 配置认证拦截器                                   │  │
│  │    - 配置允许的来源                                   │  │
│  └──────────────┬───────────────────────────────────────┘  │
│                 │                                            │
│  ┌──────────────▼───────────────────────────────────────┐  │
│  │  WebSocketAuthInterceptor (握手拦截器)               │  │
│  │    - 从 Cookie 提取 SESSION ID                       │  │
│  │    - 从 Redis 加载 Session                           │  │
│  │    - 验证用户登录状态                                 │  │
│  │    - 将 userId 传递给 Handler                        │  │
│  └──────────────┬───────────────────────────────────────┘  │
│                 │ 认证通过                                   │
│  ┌──────────────▼───────────────────────────────────────┐  │
│  │  ChatWebSocketHandler (消息处理器)                   │  │
│  │    - 管理用户会话 (userId → WebSocketSession)        │  │
│  │    - 接收客户端消息                                   │  │
│  │    - 调用 MessageService 保存消息                    │  │
│  │    - 转发消息给接收者                                 │  │
│  │    - 发送确认给发送者                                 │  │
│  └──────────────┬───────────────────────────────────────┘  │
│                 │                                            │
│  ┌──────────────▼───────────────────────────────────────┐  │
│  │  MessageService (业务逻辑)                           │  │
│  │    - 保存消息到数据库                                 │  │
│  │    - 查询历史消息                                     │  │
│  │    - 构建消息 VO                                      │  │
│  └──────────────┬───────────────────────────────────────┘  │
│                 │                                            │
│  ┌──────────────▼───────────────────────────────────────┐  │
│  │  MySQL 数据库                                         │  │
│  │    message 表                                         │  │
│  │    - id, fromUserId, toUserId                        │  │
│  │    - content, type, status                           │  │
│  │    - createTime                                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Redis                                                │  │
│  │    Spring Session 存储                                │  │
│  │    - myapp:session:{sessionId}                       │  │
│  │      → USER_LOGIN_STATE: userId                      │  │
│  └──────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

## 核心流程：发送一条消息的完整过程

让我详细讲解从点击"发送"到对方收到消息的每一步：

### 1️⃣ 用户点击发送按钮

**前端代码（ChatPage.vue）：**
```vue
<van-button @click="sendMessage">发送</van-button>

<script>
const sendMessage = () => {
  // 1.1 验证输入
  if (!inputMessage.value.trim()) return

  // 1.2 检查连接状态
  if (!isConnected.value) {
    showToast('连接已断开，请刷新页面')
    return
  }

  try {
    // 1.3 调用 WebSocket 发送方法
    wsSendMessage({
      toUserId: targetUserId.value,      // 接收者 ID
      content: inputMessage.value,        // 消息内容
      type: 0                             // 消息类型（0=文本）
    })

    // 1.4 清空输入框
    inputMessage.value = ''
    scrollToBottom()
  } catch (error) {
    showToast('发送失败')
  }
}
</script>
```

**知识点：**
- ✅ 前端验证（防止发送空消息）
- ✅ 连接状态检查（提升用户体验）
- ✅ 错误处理（网络问题时的降级方案）

---

### 2️⃣ useWebSocket 发送 JSON 消息

**前端代码（useWebSocket.ts）：**
```typescript
const sendMessage = (message: {
  toUserId: number;
  content: string;
  type?: number
}) => {
  // 2.1 检查 WebSocket 状态
  if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
    throw new Error('WebSocket 未连接')
  }

  // 2.2 序列化为 JSON 并发送
  ws.value.send(JSON.stringify(message))
}
```

**发送的数据（通过 WebSocket 传输）：**
```json
{
  "toUserId": 62,
  "content": "你好，在吗？",
  "type": 0
}
```

**知识点：**
- ✅ **WebSocket.send()** 只能发送字符串或二进制数据
- ✅ 使用 **JSON.stringify()** 序列化对象
- ✅ **WebSocket.readyState** 连接状态：
  - `0` CONNECTING - 正在连接
  - `1` OPEN - 已连接
  - `2` CLOSING - 正在关闭
  - `3` CLOSED - 已关闭

---

### 3️⃣ 通过 Vite 代理转发

**Vite 配置（vite.config.ts）：**
```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      ws: true,  // ← 关键：启用 WebSocket 代理
    }
  }
}
```

**转发过程：**
```
前端 WebSocket: ws://localhost:5173/api/ws/chat
                      ↓ Vite 代理转发
后端 WebSocket: ws://localhost:8080/api/ws/chat
```

**知识点：**
- ✅ WebSocket 使用 `ws://` 协议（对应 HTTPS 是 `wss://`）
- ✅ Vite 代理需要 `ws: true` 才能处理 WebSocket
- ✅ 代理会保持连接状态，不会断开

---

### 4️⃣ 后端 WebSocket 握手认证

**第一次连接时执行（WebSocketAuthInterceptor.java）：**
```java
@Override
public boolean beforeHandshake(
    ServerHttpRequest request,
    ServerHttpResponse response,
    WebSocketHandler wsHandler,
    Map<String, Object> attributes
) throws Exception {
    ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

    // 4.1 从 Cookie 中提取 SESSION
    Cookie[] cookies = servletRequest.getServletRequest().getCookies();
    for (Cookie cookie : cookies) {
        if ("SESSION".equals(cookie.getName())) {
            String sessionId = cookie.getValue();

            // 4.2 从 Redis 加载完整的 Session
            Session session = sessionRepository.findById(sessionId);

            if (session != null) {
                // 4.3 获取用户 ID
                Object userId = session.getAttribute(UserConstant.USER_LOGIN_STATE);

                if (userId != null) {
                    // 4.4 将 userId 存入 WebSocket 会话属性
                    attributes.put(UserConstant.USER_LOGIN_STATE, userId);
                    return true;  // ✅ 允许连接
                }
            }
        }
    }

    return false;  // ❌ 拒绝连接
}
```

**知识点：**
- ✅ **HandshakeInterceptor** 在 WebSocket 握手阶段执行
- ✅ **Spring Session** 将 Session 存储在 Redis 中
- ✅ Cookie 中的 `SESSION` 是 Session ID，不是完整数据
- ✅ `attributes` 参数会传递给后续的 Handler

**Redis 中的 Session 数据结构：**
```
Key: myapp:session:sessions:MDdjNDU1MDUtNzE0OS00YTBkLWFkNjQtOTg2MmEzZTUwNWRl
Value: {
  "USER_LOGIN_STATE": 61,
  "creationTime": 1710676800000,
  "lastAccessedTime": 1710680400000,
  "maxInactiveInterval": 86400
}
```

---

### 5️⃣ 后端接收并处理消息

**消息处理器（ChatWebSocketHandler.java）：**
```java
@Override
protected void handleTextMessage(
    WebSocketSession session,
    TextMessage message
) throws Exception {
    // 5.1 从会话属性中获取发送者 ID
    Long fromUserId = (Long) session.getAttributes()
        .get(UserConstant.USER_LOGIN_STATE);

    if (fromUserId == null) {
        return;  // 未认证，忽略消息
    }

    // 5.2 解析 JSON 消息
    String payload = message.getPayload();
    ChatMessage chatMessage = gson.fromJson(payload, ChatMessage.class);
    // chatMessage = {toUserId: 62, content: "你好，在吗？", type: 0}

    // 5.3 调用 MessageService 保存到数据库
    Message savedMessage = messageService.sendMessage(
        fromUserId,                   // 发送者 ID（从 session 获取）
        chatMessage.getToUserId(),    // 接收者 ID
        chatMessage.getContent(),     // 消息内容
        chatMessage.getType()         // 消息类型
    );

    // 5.4 构建返回的消息 VO（包含用户信息）
    MessageVO messageVO = buildMessageVO(savedMessage);
    // messageVO = {
    //   id: 123,
    //   fromUserId: 61,
    //   fromUsername: "张三",
    //   fromUserAvatar: "http://...",
    //   toUserId: 62,
    //   toUsername: "李四",
    //   content: "你好，在吗？",
    //   type: 0,
    //   createTime: "2024-03-18 20:30:00"
    // }

    // 5.5 发送给接收者（如果在线）
    Long toUserId = chatMessage.getToUserId();
    WebSocketSession toUserSession = userSessions.get(toUserId);
    if (toUserSession != null && toUserSession.isOpen()) {
        String messageJson = gson.toJson(messageVO);
        toUserSession.sendMessage(new TextMessage(messageJson));
    }

    // 5.6 发送确认给发送者（消息已送达）
    String messageJson = gson.toJson(messageVO);
    session.sendMessage(new TextMessage(messageJson));
}
```

**知识点：**
- ✅ **TextWebSocketHandler** 是 Spring 提供的 WebSocket 处理器基类
- ✅ **userSessions** 是一个 `ConcurrentHashMap<Long, WebSocketSession>` 存储在线用户
- ✅ 发送者和接收者都会收到消息（实现消息同步）
- ✅ 使用 **Gson** 序列化/反序列化 JSON

---

### 6️⃣ 保存消息到数据库

**MessageService 实现：**
```java
@Override
public Message sendMessage(
    Long fromUserId,
    Long toUserId,
    String content,
    Integer type
) {
    // 6.1 创建消息实体
    Message message = new Message();
    message.setFromUserId(fromUserId);
    message.setToUserId(toUserId);
    message.setContent(content);
    message.setType(type != null ? type : 0);
    message.setStatus(0);  // 0=未读
    message.setCreateTime(new Date());

    // 6.2 保存到数据库
    boolean result = this.save(message);

    if (!result) {
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息发送失败");
    }

    return message;
}
```

**数据库表结构（message）：**
```sql
CREATE TABLE message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fromUserId BIGINT NOT NULL COMMENT '发送者ID',
    toUserId BIGINT NOT NULL COMMENT '接收者ID',
    content TEXT NOT NULL COMMENT '消息内容',
    type INT DEFAULT 0 COMMENT '消息类型：0-文本 1-图片 2-语音',
    status INT DEFAULT 0 COMMENT '状态：0-未读 1-已读',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_from_to (fromUserId, toUserId, createTime),
    INDEX idx_to_status (toUserId, status)
);
```

**知识点：**
- ✅ **MyBatis-Plus** 提供了 `save()` 方法，自动生成 INSERT 语句
- ✅ 索引设计：
  - `idx_from_to`: 查询两人聊天记录
  - `idx_to_status`: 查询未读消息数量
- ✅ 消息状态管理（未读/已读）

---

### 7️⃣ 前端接收消息并显示

**useWebSocket 接收消息：**
```typescript
ws.value.onmessage = (event) => {
  try {
    // 7.1 解析 JSON
    const message = JSON.parse(event.data)
    // message = {
    //   id: 123,
    //   fromUserId: 61,
    //   toUserId: 62,
    //   content: "你好，在吗？",
    //   createTime: "2024-03-18T20:30:00"
    // }

    // 7.2 调用所有已注册的消息处理器
    messageHandlers.forEach(handler => handler(message))
  } catch (error) {
    console.error('解析消息失败:', error)
    errorHandlers.forEach(handler => handler(error))
  }
}
```

**ChatPage 处理接收到的消息：**
```vue
<script>
onMounted(() => {
  // 注册消息处理器
  onMessage((message: ChatMessage) => {
    // 7.3 只接收与当前聊天对象相关的消息
    if (
      (message.fromUserId === targetUserId.value &&
       message.toUserId === currentUserId.value) ||
      (message.fromUserId === currentUserId.value &&
       message.toUserId === targetUserId.value)
    ) {
      // 7.4 添加到消息列表
      messages.value.push(message)

      // 7.5 滚动到底部
      scrollToBottom()
    }
  })
})
</script>
```

**知识点：**
- ✅ **onmessage** 事件在收到消息时触发
- ✅ 使用**观察者模式**：可以注册多个消息处理器
- ✅ **消息过滤**：只显示当前聊天对象的消息

---

## 关键技术知识点详解

### 1. WebSocket 协议

**与 HTTP 的区别：**

| 特性 | HTTP | WebSocket |
|------|------|-----------|
| 连接方式 | 短连接（请求-响应） | 长连接（持久化） |
| 通信方向 | 单向（客户端主动） | 双向（服务器可主动推送） |
| 开销 | 每次请求都有完整的 HTTP 头 | 握手后只传输数据 |
| 实时性 | 需要轮询 | 真正实时 |
| 协议 | `http://` `https://` | `ws://` `wss://` |

**WebSocket 握手过程：**

```
1. 客户端发起 HTTP 升级请求：
   GET /ws/chat HTTP/1.1
   Host: localhost:8080
   Upgrade: websocket                    ← 请求升级协议
   Connection: Upgrade
   Sec-WebSocket-Key: dGhlIHNhbXBsZQ==  ← 随机密钥
   Sec-WebSocket-Version: 13

2. 服务器同意升级：
   HTTP/1.1 101 Switching Protocols      ← 状态码 101
   Upgrade: websocket
   Connection: Upgrade
   Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=  ← 根据密钥计算

3. 连接升级完成，可以双向通信
```

---

### 2. 会话管理（用户在线状态）

**userSessions 的作用：**

```java
// ConcurrentHashMap: 线程安全的哈希表
private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

// 用户连接时
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    Long userId = (Long) session.getAttributes().get(UserConstant.USER_LOGIN_STATE);
    userSessions.put(userId, session);  // 存储会话
}

// 用户断开时
@Override
public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    Long userId = (Long) session.getAttributes().get(UserConstant.USER_LOGIN_STATE);
    userSessions.remove(userId);  // 移除会话
}

// 发送消息时
WebSocketSession targetSession = userSessions.get(toUserId);
if (targetSession != null && targetSession.isOpen()) {
    targetSession.sendMessage(new TextMessage(json));  // 直接推送
}
```

**知识点：**
- ✅ **ConcurrentHashMap** 支持高并发读写
- ✅ **WebSocketSession** 代表一个连接
- ✅ 用户离线时消息不会丢失（已保存到数据库）

---

### 3. 消息可靠性保证

**三层保证：**

```
1. 数据库持久化
   ↓ 所有消息都保存到 MySQL
   ↓ 即使接收者离线，下次登录也能看到

2. 双向确认
   ↓ 发送者收到自己的消息（确认已送达服务器）
   ↓ 接收者收到消息（如果在线）

3. 历史消息加载
   ↓ 进入聊天页面时，从数据库加载历史
   ↓ WebSocket 只负责增量消息
```

**加载历史消息（HTTP API）：**

```typescript
// ChatPage.vue
onMounted(async () => {
  // 通过 HTTP 请求加载历史
  messages.value = await getChatHistory(targetUserId.value)

  // 然后连接 WebSocket 接收新消息
  connect()
})
```

```java
// MessageController.java
@GetMapping("/history")
public BaseResponse<List<MessageVO>> getChatHistory(
    @RequestParam Long targetUserId,
    HttpServletRequest request
) {
    User loginUser = userService.getLoginUser(request);
    List<MessageVO> history = messageService.getChatHistory(
        loginUser.getId(),
        targetUserId
    );
    return ResultUtils.success(history);
}
```

---

### 4. Vue 3 Composition API

**useWebSocket.ts 的设计模式：**

```typescript
export function useWebSocket() {
  // 响应式状态
  const ws = ref<WebSocket | null>(null)
  const isConnected = ref(false)

  // 事件处理器数组（观察者模式）
  const messageHandlers: MessageHandler[] = []

  // 连接方法
  const connect = () => { /* ... */ }

  // 断开方法
  const disconnect = () => { /* ... */ }

  // 发送消息
  const sendMessage = (msg) => { /* ... */ }

  // 注册消息处理器
  const onMessage = (handler: MessageHandler) => {
    messageHandlers.push(handler)
  }

  // 生命周期：组件卸载时自动断开
  onUnmounted(() => {
    disconnect()
  })

  // 返回公开的 API
  return {
    isConnected,
    connect,
    disconnect,
    sendMessage,
    onMessage,
  }
}
```

**知识点：**
- ✅ **Composable** 是 Vue 3 的代码复用方式
- ✅ **ref** 创建响应式变量
- ✅ **onUnmounted** 组件销毁时清理资源
- ✅ 封装复杂逻辑，暴露简单接口

---

### 5. 并发安全

**Spring WebSocket 的线程模型：**

```java
// 每个 WebSocket 连接在独立的线程中处理
@Override
protected void handleTextMessage(
    WebSocketSession session,
    TextMessage message
) {
    // ⚠️ 多个用户同时发消息 = 多线程并发

    // ✅ ConcurrentHashMap 是线程安全的
    userSessions.put(userId, session);

    // ✅ MyBatis-Plus 的 save() 使用数据库事务
    messageService.save(message);

    // ⚠️ 如果有共享状态，需要加锁
}
```

**知识点：**
- ✅ 使用线程安全的集合（ConcurrentHashMap）
- ✅ 数据库操作天然支持并发（事务隔离）
- ✅ 避免共享可变状态

---

## 与传统 HTTP 轮询的对比

### ❌ HTTP 轮询方案（老方法）

```javascript
// 前端每隔 3 秒请求一次
setInterval(() => {
  fetch('/api/message/new')
    .then(res => res.json())
    .then(messages => {
      // 显示新消息
    })
}, 3000)
```

**缺点：**
1. **延迟高**：最坏情况延迟 3 秒
2. **浪费资源**：99% 的请求都是无效的（没有新消息）
3. **服务器压力大**：1000 个在线用户 = 每秒 333 个请求
4. **不实时**：无法做到毫秒级推送

### ✅ WebSocket 方案（现代方法）

```javascript
// 建立一次连接，服务器主动推送
const ws = new WebSocket('ws://...')
ws.onmessage = (event) => {
  // 收到消息立即显示
}
```

**优点：**
1. **真正实时**：毫秒级延迟
2. **节省资源**：只在有消息时传输
3. **低开销**：握手后只传输数据，无 HTTP 头
4. **双向通信**：服务器可主动推送

---

## 扩展知识

### 1. 如何实现群聊？

```java
// 广播给多个用户
Set<Long> groupMemberIds = getGroupMembers(groupId);
for (Long memberId : groupMemberIds) {
    WebSocketSession session = userSessions.get(memberId);
    if (session != null && session.isOpen()) {
        session.sendMessage(new TextMessage(json));
    }
}
```

### 2. 如何实现"对方正在输入..."？

```javascript
// 前端：输入时发送特殊消息
inputElement.oninput = () => {
  wsSendMessage({ type: 'typing', toUserId: targetUserId })
}

// 后端：不保存到数据库，直接转发
if (message.type === 'typing') {
    // 只转发，不保存
    forwardToUser(message.toUserId, message)
}
```

### 3. 如何实现消息已读回执？

```javascript
// 用户打开聊天页面时
markAsRead(targetUserId)

// 后端更新数据库
UPDATE message
SET status = 1
WHERE toUserId = ? AND fromUserId = ? AND status = 0
```

### 4. 如何支持表情、图片、文件？

```javascript
// 文本消息
{ type: 0, content: "你好" }

// 表情
{ type: 1, content: "😊" }

// 图片（先上传，获得 URL）
{ type: 2, content: "http://cdn.com/img.jpg" }

// 文件
{ type: 3, content: "http://cdn.com/file.pdf", filename: "文档.pdf" }
```

---

## 需要掌握的核心知识点清单

### 前端（JavaScript/TypeScript）
- ✅ WebSocket API（连接、发送、接收、关闭）
- ✅ Vue 3 Composition API（ref、onMounted、onUnmounted）
- ✅ Promise/Async-Await（异步处理）
- ✅ JSON 序列化/反序列化

### 后端（Java/Spring）
- ✅ Spring WebSocket 配置
- ✅ WebSocket 生命周期（握手、消息、关闭）
- ✅ Spring Session + Redis
- ✅ MyBatis-Plus CRUD 操作
- ✅ 并发安全（ConcurrentHashMap、线程安全）

### 网络协议
- ✅ WebSocket 协议原理
- ✅ HTTP 协议升级机制
- ✅ 同源策略与 CORS
- ✅ Cookie 与 Session 认证

### 数据库
- ✅ 表结构设计
- ✅ 索引优化
- ✅ 查询历史消息

### 架构设计
- ✅ 前后端分离架构
- ✅ 代理服务器（Vite）
- ✅ 会话管理（在线用户）
- ✅ 消息可靠性保证

---

## 总结

这个聊天功能涉及的核心流程：

```
用户输入 → 前端验证 → WebSocket.send()
    ↓
Vite 代理转发 → 后端接收 → 认证验证
    ↓
保存数据库 → 推送给接收者 → 前端显示
```

**最重要的 3 个概念：**
1. **WebSocket = 持久化双向连接**（与 HTTP 的本质区别）
2. **会话管理 = 维护在线用户映射**（userId → WebSocketSession）
3. **消息可靠性 = 数据库持久化 + 实时推送**（离线可查，在线即达）
