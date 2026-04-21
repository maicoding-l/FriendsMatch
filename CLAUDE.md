# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Friends Finder is a full-stack social application for matching users based on shared interests (tags) and item preferences (books, movies, music). The system uses two recommendation algorithms:
1. **Tag-based user matching** - Jaccard similarity on user tags
2. **Item-based recommendations** - Random walk (PersonalRank) algorithm on user-item bipartite graph

**Architecture:** Spring Boot 3.5.3 backend + Vue 3 + Vite frontend monorepo

## Tech Stack

**Backend:**
- Java 21, Spring Boot 3.5.3
- MyBatis-Plus 3.5.12 (ORM)
- MySQL (persistence)
- Redis + Spring Session (authentication & caching)
- Redisson (distributed locks)

**Frontend:**
- Vue 3 + TypeScript
- Vite 7 (build tool)
- Vant 4 (mobile UI library)
- Vue Router 4
- Axios (HTTP client)

## Common Commands

### Backend (friends-finder-backend)

```bash
# Build and run
mvnw.cmd clean package              # Build JAR (Windows)
mvnw.cmd spring-boot:run            # Run dev server on :8080

# Testing
mvnw.cmd test                       # Run all tests
mvnw.cmd test -Dtest=UserServiceTest  # Run specific test

# Database setup
# 1. Create database: mai1
# 2. Run SQL: friends-finder-backend/sql/create_table.sql
```

### Frontend (friends-finder-frontend)

```bash
# Install and run
pnpm install                        # Install dependencies (once)
pnpm dev                           # Dev server on :5173
pnpm build                         # Production build
pnpm preview                       # Preview production build

# Code quality
pnpm lint                          # Check code style
pnpm lint:fix                      # Auto-fix linting issues
pnpm format                        # Format with Prettier
pnpm beautify                      # Run both lint:fix and format
```

### Development Workflow

1. Start backend: `cd friends-finder-backend && mvnw.cmd spring-boot:run`
2. Start frontend: `cd friends-finder-frontend && pnpm dev`
3. Access app: http://localhost:5173 (Vite proxies `/api` to backend)
4. API docs: http://localhost:8080/api/swagger-ui.html

## Architecture & Design Patterns

### Backend Architecture

**Package Structure:**
```
com.mai.friendsFinder/
├── common/          # BaseResponse, ErrorCode, ResultUtils, PageRequest
├── config/          # MyBatis-Plus, Redis, Redisson, Session, CORS
├── constant/        # UserConstant (USER_LOGIN_STATE, ADMIN_ROLE)
├── controller/      # REST endpoints (UserController, ItemController, TeamController)
├── exception/       # BusinessException, GlobalExceptionHandler
├── job/             # Scheduled tasks (RecommendCacheJob)
├── mapper/          # MyBatis-Plus data access layer
├── model/           # Entities, DTOs, VOs, request objects, enums
├── service/         # Business logic interfaces and implementations
└── utils/           # RecommendUtils, UserRecommendUtils, AlgorithmUtils
```

**Core Design Patterns:**

**Session-Based Authentication**
- Login state stored in Redis via Spring Session
- Session key: `USER_LOGIN_STATE` contains user ID (Long)
- Get current user: `userService.getLoginUser(request)`
- Check admin: `userService.isAdmin(request)`

**Response Wrapper Pattern**
- All API responses wrapped in `BaseResponse<T>`
- Success: `ResultUtils.success(data)`
- Error: Throw `BusinessException` with `ErrorCode`

**User Safety**
- NEVER return raw User entities from controllers
- Always call `userService.getSafetyUser(user)` to strip sensitive fields (password, etc.)

**Recommendation Caching**
- Scheduled job runs daily at 2 AM (RecommendCacheJob)
- Pre-computes item recommendations and user matches for all active users
- Uses Redisson distributed locks to prevent duplicate execution
- Redis keys:
  - `mai:recommend:randomwalk:{userId}` - Item recommendations (24h TTL)
  - `mai:match:users:{userId}` - User matches with common items (24h TTL)

### Frontend Architecture

**Directory Structure:**
```
src/
├── api/             # HTTP clients (user.ts, item.ts, team.ts)
├── components/      # Reusable Vue components (UserCardsList, TeamCardsList)
├── constants/       # App constants and enums
├── layouts/         # Layout components (BasicLayout)
├── models/          # TypeScript types/interfaces
├── pages/           # Route pages (Index, User, Team, Graph, ItemDetail)
├── router/          # Vue Router configuration
├── request.ts       # Axios instance with interceptors
└── main.ts          # App entry point
```

**Key Pages:**
- `Index.vue` - Home page with recommendations
- `Graph.vue` - Social graph visualization
- `ItemDetail.vue` - Item (book/movie/music) details
- `User.vue` - User profile and matching
- `Team.vue` - Team management

**API Integration:**
- Vite dev server proxies `/api` to `http://localhost:8080`
- Axios instance in `request.ts` handles auth headers and error handling
- API clients in `src/api/` wrap backend endpoints

### Database Schema

**Core Tables:**
- `user` - Users with tags stored as JSON strings
- `item` - Items (books, movies, music) for recommendation
- `user_item` - User-item interactions (ratings, likes)
- `team` - Groups/teams (max_num, expire_time, status: 0=public, 1=private, 2=encrypted)
- `user_team` - Many-to-many user-team relationships
- `tag` - Tag definitions

### Key Services & Algorithms

**UserService** (`friends-finder-backend/src/main/java/com/mai/friendsFinder/service/UserService.java`)
- User CRUD operations
- Authentication (login/logout)
- Tag-based search
- Tag-based matching: `GET /api/user/match?num=N` (Jaccard similarity via `AlgorithmUtils.calcSimilarity()`)
- User matching with common items: `GET /api/user/match/withCommonItems?num=N` (uses `UserRecommendUtils`)

**ItemService** (`friends-finder-backend/src/main/java/com/mai/friendsFinder/service/ItemService.java`)
- Item CRUD operations
- Item recommendations: Uses `RecommendUtils.getRecommendation(userId, topN)`
- Based on PersonalRank random walk algorithm on user-item bipartite graph

**TeamService** (`friends-finder-backend/src/main/java/com/mai/friendsFinder/service/TeamService.java`)
- Team creation, joining, quitting, listing
- Handles team status (public/private/encrypted)

**UserItemService** (`friends-finder-backend/src/main/java/com/mai/friendsFinder/service/UserItemService.java`)
- User-item interaction tracking
- Provides data for recommendation algorithms

**Recommendation Algorithms:**
- `AlgorithmUtils` - Jaccard similarity for tag-based user matching
- `RecommendUtils` - PersonalRank random walk for item recommendations
- `UserRecommendUtils` - Random walk for finding similar users based on shared items
- Both utils construct bipartite graphs from `user_item` data and run iterative PageRank-style algorithms

### Configuration

**Backend (`application.yml`):**
- Context path: `/api`
- Server port: `8080`
- CORS: `http://localhost:5173/`
- Session timeout: 86400 seconds (24 hours)
- Database: `mai1` on MySQL
- Redis: Session storage + caching

**Frontend (`vite.config.ts`):**
- Dev server: Port 5173
- Proxy: `/api` → `http://localhost:8080`
- Auto-imports: Vant components via `unplugin-vue-components`

## Working with the Codebase

### Adding New Features

**Backend:**
1. Define request/response DTOs in `model/request/` and `model/vo/`
2. Add service interface in `service/` and implementation in `service/impl/`
3. Create controller endpoint in `controller/`
4. Use `ResultUtils.success()` for success responses
5. Throw `BusinessException(ErrorCode.XXX)` for errors
6. Update SQL schema in `sql/create_table.sql` if needed

**Frontend:**
1. Add TypeScript types in `src/models/`
2. Create API client function in `src/api/`
3. Add page component in `src/pages/` or reusable component in `src/components/`
4. Register route in `src/router/index.ts`
5. Use Vant UI components (auto-imported)

### Tag-Based User Matching

- User tags stored as JSON strings in `user.tags` column
- Example: `["Java", "Python", "Vue"]`
- Match endpoint calculates Jaccard similarity between current user's tags and all other users
- Returns top N matches sorted by similarity score

### Item Recommendation System

- Uses PersonalRank random walk algorithm
- Constructs bipartite graph: Users ↔ Items
- Iteratively propagates probability scores (default 20 iterations)
- Filters out items user already interacted with
- Returns top N recommended items
- Pre-computed daily and cached in Redis for performance

### Session Management

- Spring Session stores sessions in Redis
- Login sets `USER_LOGIN_STATE` in session
- All protected endpoints call `userService.getLoginUser(request)` to verify authentication
- Frontend sends session cookie with each request (handled by Axios)

## Testing

**Backend:**
- Tests in `src/test/java/com/mai/friendsFinder/`
- Use `@SpringBootTest` for integration tests
- Example test classes: `UserServiceTest`, `RecommendServiceTest`, `ImportDataTest`
- Run with: `mvnw.cmd test`

**Frontend:**
- No test framework currently configured
- Rely on ESLint/Prettier for code quality
- Manual testing via `pnpm dev`

## Important Notes

- Main branch: `main` (for PRs)
- Current branch: `master`
- When making PRs, target `main` branch
- Always use `getSafetyUser()` before returning user data from controllers
- Tag matching uses Jaccard similarity; item recommendations use random walk
- Scheduled jobs use Redisson locks to prevent duplicate execution in multi-instance deployments
- Frontend is optimized for mobile (Vant UI library)
