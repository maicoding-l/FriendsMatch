# 功能更新文档

> 更新日期：2026-03-19

本文档记录了推荐系统过滤优化和短评功能的实现细节、技术要点和注意事项。

---

## 📋 目录

- [功能一：推荐系统过滤优化](#功能一推荐系统过滤优化)
- [功能二：物品短评系统](#功能二物品短评系统)
- [关键技术要点](#关键技术要点)
- [最佳实践](#最佳实践)

---

## 功能一：推荐系统过滤优化

### 1.1 功能概述

实现了双重过滤机制，确保推荐结果不包含用户已交互的内容：
- **物品推荐**：过滤掉已标记（收藏/打分/看过）的物品
- **用户推荐**：过滤掉已添加的好友

### 1.2 双重过滤机制

#### 📌 为什么需要双重过滤？

```
场景：用户在凌晨2点缓存生成后，上午10点添加了新好友或标记了新物品
问题：如果只有缓存时过滤，上午10点的推荐仍会包含这些内容（使用的是凌晨的缓存）
解决：在读取缓存时再次过滤，确保推荐准确性
```

#### 过滤时机

**第一层：缓存写入时过滤**
- 位置：`RecommendCacheJob.java`（定时任务）
- 时机：每天凌晨2点执行
- 优点：减少缓存空间，提前排除无效推荐
- 代码位置：`friends-finder-backend/src/main/java/com/mai/friendsFinder/job/RecommendCacheJob.java`

```java
// 示例：物品推荐缓存时过滤
// 4.1.1 查询用户已标记的物品ID列表
QueryWrapper<UserItem> userItemQueryWrapper = new QueryWrapper<>();
userItemQueryWrapper.eq("user_id", userId);
userItemQueryWrapper.select("item_id");
List<Long> markedItemIds = userItemService.list(userItemQueryWrapper).stream()
        .map(UserItem::getItemId)
        .collect(Collectors.toList());

// 4.1.2 过滤掉已标记的物品
List<Item> recommendedItems = recommendedItemIds.stream()
        .filter(itemId -> !markedItemIds.contains(itemId))
        .map(itemService::getById)
        .filter(item -> item != null)
        .collect(Collectors.toList());
```

**第二层：缓存读取后过滤**
- 位置：`ItemServiceImpl.java` 和 `UserServiceImpl.java`
- 时机：用户请求推荐时
- 优点：处理缓存期间的增量变化，保证实时准确性
- 代码位置：
  - `friends-finder-backend/src/main/java/com/mai/friendsFinder/service/impl/ItemServiceImpl.java:191-212`
  - `friends-finder-backend/src/main/java/com/mai/friendsFinder/service/impl/UserServiceImpl.java:433-462`

```java
// 示例：从缓存读取后再次过滤
// 1.1 获取用户已标记的物品ID列表（缓存期间可能新增标记）
QueryWrapper<UserItem> userItemQueryWrapper = new QueryWrapper<>();
userItemQueryWrapper.eq("user_id", userId);
userItemQueryWrapper.select("item_id");
List<Long> markedItemIds = userItemService.list(userItemQueryWrapper).stream()
        .map(UserItem::getItemId)
        .collect(Collectors.toList());

// 1.2 从缓存结果中过滤掉已标记的物品
List<Item> filteredItems = cachedItems.stream()
        .filter(item -> !markedItemIds.contains(item.getId()))
        .collect(Collectors.toList());
```

### 1.3 关键技术点

#### 🔍 双向好友查询

好友关系是双向的，需要查询两个方向：

```java
// 查询用户的所有好友（双向）
QueryWrapper<Friend> friendQueryWrapper = new QueryWrapper<>();
friendQueryWrapper.eq("user_id", userId).or().eq("friend_id", userId);
List<Friend> friendList = friendMapper.selectList(friendQueryWrapper);

Set<Long> friendIds = new HashSet<>();
for (Friend friend : friendList) {
    // 添加好友ID（排除自己）
    if (!friend.getUserId().equals(userId)) {
        friendIds.add(friend.getUserId());
    }
    if (!friend.getFriendId().equals(userId)) {
        friendIds.add(friend.getFriendId());
    }
}
```

**要点：**
- 好友表存储的是单向关系（user_id → friend_id）
- 但业务上是双向的（A和B互为好友）
- 查询时必须同时检查 `user_id=A OR friend_id=A`

#### 📊 推荐数量的调整

由于过滤会减少推荐数量，需要增加初始计算量：

```java
// 之前
List<Long> recommendedUserIds = userRecommendUtils.getSimilarUsers(userId, 50);

// 现在
List<Long> recommendedUserIds = userRecommendUtils.getSimilarUsers(userId, 100);
```

**原因：**
- 如果用户有30个好友，从50个推荐中过滤，可能只剩20个
- 增加到100个，过滤后仍有充足的推荐候选

### 1.4 性能优化要点

#### 批量查询优化

```java
// ❌ 错误：循环查询数据库（N+1问题）
for (Long itemId : recommendedItemIds) {
    Item item = itemService.getById(itemId);
    items.add(item);
}

// ✅ 正确：一次性查询所有数据
QueryWrapper<User> queryWrapper = new QueryWrapper<>();
queryWrapper.in("id", recommendedUserIds);
Map<Long, List<User>> userMap = userService.list(queryWrapper).stream()
        .collect(Collectors.groupingBy(User::getId));
```

#### 只查询需要的字段

```java
// 只查询ID字段，减少数据传输
QueryWrapper<UserItem> queryWrapper = new QueryWrapper<>();
queryWrapper.eq("user_id", userId);
queryWrapper.select("item_id");  // 只查询item_id字段
```

### 1.5 缓存失效策略

当用户标记物品或添加好友时，主动清除相关缓存：

```java
// UserItemServiceImpl.java 中的缓存清除
private void clearUserRecommendCache(Long userId) {
    // 清除物品推荐缓存
    String itemRecommendKey = "mai:recommend:randomwalk:" + userId;
    redisTemplate.delete(itemRecommendKey);

    // 清除用户匹配缓存
    String userMatchKey = "mai:match:users:" + userId;
    redisTemplate.delete(userMatchKey);
}
```

**触发时机：**
- 用户收藏物品
- 用户打分
- 用户添加好友

**位置：**
- `friends-finder-backend/src/main/java/com/mai/friendsFinder/service/impl/UserItemServiceImpl.java:239-259`

---

## 功能二：物品短评系统

### 2.1 功能概述

用户可以对物品（书籍/电影/音乐）发表短评，其他用户可以为短评点赞。

**核心功能：**
- 发表短评（最多500字）
- 查看短评列表（按点赞数排序）
- 点赞/取消点赞短评
- 删除短评（仅作者或管理员）
- 每个用户对每个物品只能有一条短评

### 2.2 数据库设计

#### 表结构

**item_review 表**（物品短评表）

```sql
CREATE TABLE item_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '短评ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    item_id BIGINT NOT NULL COMMENT '物品ID',
    content TEXT NOT NULL COMMENT '短评内容',
    rating DOUBLE COMMENT '评分（1.0-5.0）',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除标志',
    INDEX idx_item_id (item_id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_user_item (user_id, item_id) COMMENT '用户-物品唯一索引'
) COMMENT '物品短评表';
```

**review_like 表**（短评点赞表）

```sql
CREATE TABLE review_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    review_id BIGINT NOT NULL COMMENT '短评ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_delete TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除标志',
    UNIQUE KEY uk_user_review (user_id, review_id) COMMENT '用户-短评唯一索引'
) COMMENT '短评点赞表';
```

#### 🔑 关键设计要点

**唯一索引的作用：**

1. **`uk_user_item (user_id, item_id)`**
   - 防止用户对同一物品发表多条短评
   - 数据库层面保证唯一性
   - 如果用户再次发表，业务逻辑会更新原有短评

2. **`uk_user_review (user_id, review_id)`**
   - 防止用户重复点赞同一条短评
   - 数据库层面保证唯一性
   - 如果再次点赞，会抛出异常（业务层先查询避免）

**为什么点赞数存在 item_review 表？**

```
优点：
1. 查询性能好 - 列表展示时无需JOIN或子查询
2. 排序方便 - 可以直接按like_count排序
3. 减少查询次数

缺点：
1. 数据冗余 - 需要同步更新
2. 一致性风险 - 需要事务保证

解决方案：
- 使用 @Transactional 保证原子性
- 点赞/取消点赞在同一事务中完成
```

### 2.3 后端架构

#### 分层结构

```
Controller（ItemReviewController.java）
    ↓
Service（ItemReviewService / ItemReviewServiceImpl）
    ↓
Mapper（ItemReviewMapper / ReviewLikeMapper）
    ↓
Database（item_review / review_like）
```

#### 核心方法详解

**addReview() - 添加短评**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Long addReview(ItemReviewAddRequest reviewAddRequest, HttpServletRequest request) {
    // 1. 参数校验
    if (content.length() > 500) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "短评内容不能超过500字");
    }

    // 2. 获取当前用户
    User loginUser = userService.getLoginUser(request);

    // 3. 检查是否已有短评
    QueryWrapper<ItemReview> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("user_id", loginUser.getId()).eq("item_id", itemId);
    ItemReview existingReview = this.getOne(queryWrapper);

    // 4. 更新或新增
    if (existingReview != null) {
        existingReview.setContent(content);
        existingReview.setRating(rating);
        this.updateById(existingReview);
        return existingReview.getId();
    } else {
        // 新增逻辑...
    }
}
```

**要点：**
- `@Transactional` 确保数据一致性
- `rollbackFor = Exception.class` 所有异常都回滚
- 先查询后更新，避免唯一索引冲突
- 用户每个物品只能有一条短评（更新模式）

**toggleLike() - 点赞切换**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Boolean toggleLike(Long reviewId, HttpServletRequest request) {
    // 1. 查询是否已点赞
    ReviewLike existingLike = reviewLikeMapper.selectOne(queryWrapper);

    if (existingLike != null) {
        // 取消点赞
        reviewLikeMapper.deleteById(existingLike.getId());
        review.setLikeCount(Math.max(0, review.getLikeCount() - 1));
        return false;
    } else {
        // 点赞
        ReviewLike like = new ReviewLike();
        like.setUserId(loginUser.getId());
        like.setReviewId(reviewId);
        reviewLikeMapper.insert(like);
        review.setLikeCount(review.getLikeCount() + 1);
        return true;
    }
}
```

**要点：**
- 事务保证点赞记录和点赞数同步更新
- `Math.max(0, ...)` 防止点赞数为负
- 返回布尔值表示最终状态（true=已点赞，false=未点赞）

**listReviewsByItemId() - 获取短评列表**

```java
@Override
public List<ItemReviewVO> listReviewsByItemId(Long itemId, HttpServletRequest request) {
    // 1. 查询短评列表（按点赞数和时间排序）
    QueryWrapper<ItemReview> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("item_id", itemId);
    queryWrapper.orderByDesc("like_count", "create_time");
    List<ItemReview> reviews = this.list(queryWrapper);

    // 2. 批量查询用户信息
    List<Long> userIds = reviews.stream()
            .map(ItemReview::getUserId)
            .distinct()
            .collect(Collectors.toList());
    Map<Long, User> userMap = userService.list(...).stream()
            .collect(Collectors.toMap(User::getId, user -> user));

    // 3. 查询当前用户的点赞状态
    if (loginUser != null) {
        Set<Long> likedReviewIds = reviewLikeMapper.selectList(...).stream()
                .map(ReviewLike::getReviewId)
                .collect(Collectors.toSet());
    }

    // 4. 构建VO对象
    // ...
}
```

**性能优化要点：**
- 批量查询用户信息（避免N+1）
- 批量查询点赞状态（一次查询所有）
- 使用Map缓存用户数据（O(1)查找）
- 使用Set缓存点赞ID（O(1)判断）

### 2.4 前端实现

#### API 调用规范

```typescript
// ❌ 错误：返回 res.data
export const addReview = async (reviewData: ItemReviewAddRequest) => {
  const res = await myAxios.post('/review/add', reviewData)
  return res.data  // 多剥了一层
}

// ✅ 正确：返回完整响应对象
export const addReview = async (reviewData: ItemReviewAddRequest) => {
  return await myAxios.post('/review/add', reviewData)
}
```

**使用方式：**

```typescript
const res = await addReview({ itemId: 1, content: '很好' })
const payload = res.data as any  // 现在能正确访问到 {code: 0, data: 2, ...}
if (payload?.code === 0) {
  // 处理成功
}
```

#### 智能时间显示

```typescript
const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
```

**显示规则：**
- 1分钟内 → "刚刚"
- 1-60分钟 → "x分钟前"
- 1-24小时 → "x小时前"
- 1-7天 → "x天前"
- 7天以上 → 完整日期（2026-03-19）

#### 乐观更新 UI

```typescript
// 点赞时立即更新UI，无需等待后端响应
const handleToggleLike = async (reviewId: number) => {
  const review = reviews.value.find(r => r.id === reviewId)
  if (review) {
    // 乐观更新
    review.isLiked = !review.isLiked
    review.likeCount += review.isLiked ? 1 : -1
  }

  try {
    const res = await toggleLikeReview(reviewId)
    // 如果失败，恢复状态
    if (res.data?.code !== 0) {
      review.isLiked = !review.isLiked
      review.likeCount += review.isLiked ? 1 : -1
    }
  } catch (error) {
    // 恢复状态
  }
}
```

**优点：**
- 即时反馈，用户体验好
- 减少等待时间
- 失败时可以回滚

---

## 关键技术要点

### 1. MyBatis-Plus 条件构造器

#### 基本用法

```java
// 等值查询
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.eq("user_id", userId);  // WHERE user_id = ?

// OR 条件
wrapper.eq("user_id", userId).or().eq("friend_id", userId);
// WHERE user_id = ? OR friend_id = ?

// 只查询指定字段
wrapper.select("item_id", "user_id");  // SELECT item_id, user_id FROM ...

// IN 查询
wrapper.in("id", Arrays.asList(1, 2, 3));  // WHERE id IN (1, 2, 3)

// 排序
wrapper.orderByDesc("like_count", "create_time");  // ORDER BY like_count DESC, create_time DESC
```

#### 注意事项

```java
// ❌ 错误：OR条件没有括号，逻辑可能不对
wrapper.eq("status", 1).eq("user_id", userId).or().eq("friend_id", userId);
// 生成: status = 1 AND user_id = ? OR friend_id = ?
// 等价于: (status = 1 AND user_id = ?) OR (friend_id = ?)

// ✅ 正确：使用 nested() 或 lambda 表达式
wrapper.eq("status", 1).and(w ->
    w.eq("user_id", userId).or().eq("friend_id", userId)
);
// 生成: status = 1 AND (user_id = ? OR friend_id = ?)
```

### 2. Java Stream API 最佳实践

#### 常用操作

```java
// filter - 过滤
list.stream()
    .filter(item -> item.getStatus() == 1)
    .collect(Collectors.toList());

// map - 转换
list.stream()
    .map(User::getId)
    .collect(Collectors.toList());

// distinct - 去重
list.stream()
    .map(UserItem::getUserId)
    .distinct()
    .collect(Collectors.toList());

// groupingBy - 分组
Map<Long, List<User>> userMap = users.stream()
    .collect(Collectors.groupingBy(User::getId));

// toMap - 转Map
Map<Long, User> userMap = users.stream()
    .collect(Collectors.toMap(User::getId, user -> user));
```

#### 性能注意事项

```java
// ❌ 性能差：每次filter都遍历markedItemIds
List<Item> result = items.stream()
    .filter(item -> !markedItemIds.contains(item.getId()))  // List.contains() 是O(n)
    .collect(Collectors.toList());

// ✅ 性能好：使用Set，contains是O(1)
Set<Long> markedItemIdSet = new HashSet<>(markedItemIds);
List<Item> result = items.stream()
    .filter(item -> !markedItemIdSet.contains(item.getId()))  // Set.contains() 是O(1)
    .collect(Collectors.toList());
```

### 3. Spring 事务管理

#### @Transactional 注解

```java
@Transactional(rollbackFor = Exception.class)
public Boolean toggleLike(Long reviewId) {
    // 所有数据库操作在同一事务中
    // 任何Exception都会导致回滚
}
```

**常见配置：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `rollbackFor` | 哪些异常回滚 | `RuntimeException` |
| `propagation` | 事务传播行为 | `REQUIRED` |
| `isolation` | 事务隔离级别 | `DEFAULT` |
| `timeout` | 超时时间（秒） | `-1`（无限制） |

**注意事项：**

```java
// ❌ 错误：private方法事务不生效
@Transactional
private void updateData() { }

// ✅ 正确：public方法
@Transactional
public void updateData() { }

// ❌ 错误：自调用事务不生效
public void methodA() {
    this.methodB();  // 直接调用，事务不生效
}

@Transactional
public void methodB() { }

// ✅ 正确：通过代理对象调用
@Autowired
private MyService self;

public void methodA() {
    self.methodB();  // 通过Spring代理，事务生效
}
```

### 4. Redis 缓存策略

#### 缓存模式

```java
// 1. 读取缓存
Object cachedData = redisTemplate.opsForValue().get(key);
if (cachedData != null) {
    return cachedData;
}

// 2. 缓存未命中，查询数据库
List<Item> items = itemService.list();

// 3. 写入缓存（设置过期时间）
redisTemplate.opsForValue().set(
    key,
    items,
    24 * 60 * 60 * 1000,  // 24小时
    TimeUnit.MILLISECONDS
);

return items;
```

#### 缓存失效策略

**被动失效（TTL）：**
```java
// 设置24小时后自动过期
redisTemplate.expire(key, 24, TimeUnit.HOURS);
```

**主动失效（删除）：**
```java
// 数据变更时，主动删除缓存
redisTemplate.delete(key);
```

**组合策略（推荐）：**
- 定时任务预热缓存（凌晨2点）
- 设置合理的TTL（24小时）
- 数据变更时主动清除
- 读取时二次过滤（处理增量变化）

### 5. 前端状态管理

#### ref vs reactive

```typescript
// ref - 基本类型和对象
const count = ref(0)
const user = ref<User | null>(null)
// 访问：count.value, user.value

// reactive - 对象
const state = reactive({
  count: 0,
  user: null
})
// 访问：state.count, state.user
```

**推荐：**
- 单个值用 `ref`
- 多个相关值用 `reactive`
- 避免混用导致响应式丢失

#### 异步状态处理

```typescript
// 推荐模式
const loading = ref(false)
const data = ref<Item[]>([])
const error = ref<string | null>(null)

const fetchData = async () => {
  loading.value = true
  error.value = null

  try {
    const res = await api.getData()
    data.value = res.data
  } catch (e) {
    error.value = '加载失败'
  } finally {
    loading.value = false
  }
}
```

---

## 最佳实践

### 1. 数据库设计

#### ✅ DO（推荐）

- 使用唯一索引防止重复数据
- 使用外键或索引优化查询
- 字段添加注释说明
- 使用逻辑删除（`is_delete`）
- 记录创建和更新时间

#### ❌ DON'T（不推荐）

- 不要在业务层保证唯一性（用数据库约束）
- 不要过度冗余字段
- 不要使用物理删除（用逻辑删除）

### 2. 后端开发

#### ✅ DO

- 参数校验放在Service层第一步
- 使用 `@Transactional` 保证数据一致性
- 批量查询替代循环查询
- 异常使用 `BusinessException` 统一处理
- 敏感数据脱敏（`getSafetyUser()`）
- 使用 Stream API 简化集合操作
- 添加详细的日志记录

#### ❌ DON'T

- 不要在Controller层写业务逻辑
- 不要忘记处理空值（`null`）
- 不要返回未脱敏的用户数据
- 不要在循环中查询数据库
- 不要忘记设置缓存过期时间

### 3. 前端开发

#### ✅ DO

- API统一封装在 `api/` 目录
- 类型定义统一放在接口文件或 `models/` 目录
- 使用 Loading 和 Error 状态
- 错误提示用户友好
- 列表数据使用 `key` 属性
- 时间格式化统一处理
- 空状态显示友好提示

#### ❌ DON'T

- 不要直接在组件中写API地址
- 不要忘记处理Loading状态
- 不要忽略错误处理
- 不要在模板中写复杂逻辑
- 不要忘记清理副作用（如定时器）

### 4. 性能优化

#### 数据库层面

```sql
-- ✅ 使用索引
CREATE INDEX idx_user_id ON item_review(user_id);
CREATE INDEX idx_item_id ON item_review(item_id);

-- ✅ 只查询需要的字段
SELECT id, user_id, content FROM item_review WHERE item_id = 1;

-- ❌ 避免 SELECT *
SELECT * FROM item_review;
```

#### 后端层面

```java
// ✅ 批量查询
List<Item> items = itemMapper.selectBatchIds(itemIds);

// ❌ 循环查询
for (Long itemId : itemIds) {
    Item item = itemMapper.selectById(itemId);
}

// ✅ 使用缓存
Object data = redisTemplate.opsForValue().get(key);
if (data == null) {
    data = loadFromDB();
    redisTemplate.opsForValue().set(key, data, 1, TimeUnit.HOURS);
}
```

#### 前端层面

```typescript
// ✅ 防抖处理
import { debounce } from 'lodash'
const search = debounce((keyword: string) => {
  // 搜索逻辑
}, 300)

// ✅ 虚拟列表（长列表优化）
import { useVirtualList } from '@vueuse/core'

// ✅ 懒加载
<img loading="lazy" :src="url" />
```

### 5. 安全注意事项

#### 后端安全

```java
// ✅ 权限校验
if (!userService.isAdmin(request) && !review.getUserId().equals(loginUser.getId())) {
    throw new BusinessException(ErrorCode.NO_AUTH);
}

// ✅ 参数校验
if (StringUtils.isBlank(content) || content.length() > 500) {
    throw new BusinessException(ErrorCode.PARAMS_ERROR);
}

// ✅ SQL注入防护（使用MyBatis-Plus参数化查询）
wrapper.eq("user_id", userId);  // 自动参数化

// ❌ 避免字符串拼接SQL
String sql = "SELECT * FROM user WHERE id = " + userId;  // 危险！
```

#### 前端安全

```typescript
// ✅ XSS防护（Vue自动转义）
<div>{{ userInput }}</div>

// ❌ 危险的HTML插入
<div v-html="userInput"></div>  // 用户输入可能包含脚本

// ✅ 敏感数据不存localStorage
// 使用httpOnly cookie存储token
```

---

## 📚 相关文档

- [MyBatis-Plus 官方文档](https://baomidou.com/)
- [Spring Framework 事务管理](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
- [Redis 最佳实践](https://redis.io/docs/manual/patterns/)
- [Vue 3 组合式API](https://cn.vuejs.org/guide/introduction.html)

---

## 🔄 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| v1.0 | 2026-03-19 | 初始版本：推荐过滤优化 + 短评系统 |

---

**维护者：** Claude Code
**最后更新：** 2026-03-19
