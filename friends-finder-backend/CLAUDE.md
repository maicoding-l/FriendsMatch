# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Friends Finder is a Spring Boot 3.5.3 backend application for matching users with similar interests. It uses session-based authentication with Redis, MyBatis-Plus for database operations, and implements a recommendation algorithm based on user tags (Jaccard similarity).

**Tech Stack:** Java 21, Spring Boot 3.5.3, MyBatis-Plus 3.5.12, MySQL, Redis, Redisson

## Common Commands

### Build and Run
```bash
# Build project (use mvnw wrapper on Windows, ./mvnw on Unix)
mvnw.cmd clean package

# Run application
mvnw.cmd spring-boot:run

# Run tests
mvnw.cmd test

# Run specific test class
mvnw.cmd test -Dtest=UserServiceTest
```

### Database
- Database name: `mai1`
- SQL schema: `sql/create_table.sql`
- Connection configured in `src/main/resources/application.yml`

### API Documentation
- Swagger UI available at: `http://localhost:8080/api/swagger-ui.html`
- All endpoints are prefixed with `/api`

## Architecture

### Package Structure
```
com.mai.friendsFinder/
├── common/          # BaseResponse, ErrorCode, ResultUtils, PageRequest
├── config/          # MyBatis-Plus, Redis, Redisson, Session configs
├── constant/        # UserConstant (USER_LOGIN_STATE, ADMIN_ROLE)
├── controller/      # REST endpoints (UserController, TeamController)
├── exception/       # BusinessException, GlobalExceptionHandler
├── mapper/          # MyBatis-Plus mappers (data access layer)
├── model/           # Entity classes, DTOs, VOs, request objects, enums
├── service/         # Business logic interfaces and implementations
└── utils/           # AlgorithmUtils (Jaccard similarity for tag matching)
```

### Core Design Patterns

**Session-Based Authentication**
- Login state stored in Redis via Spring Session
- Session key: `USER_LOGIN_STATE` stores user ID (Long)
- Use `userService.getLoginUser(request)` to get authenticated user
- Use `userService.isAdmin(request)` for admin authorization

**Response Wrapper**
- All API responses wrapped in `BaseResponse<T>`
- Use `ResultUtils.success(data)` for success responses
- Throw `BusinessException` with `ErrorCode` for errors

**User Safety**
- Never return raw User entities from controllers
- Always call `userService.getSafetyUser(user)` to strip sensitive fields (password)

**Tag-Based Matching**
- User tags stored as JSON strings in database
- Use `AlgorithmUtils.calcSimilarity()` for Jaccard similarity
- Match endpoint: `GET /api/user/match?num=N`

### Database Tables
- `user` - Users with tags stored as JSON
- `team` - Groups/teams with max_num, expire_time, status (0=public, 1=private, 2=encrypted)
- `user_team` - Many-to-many relationship between users and teams
- `tag` - Tag definitions
- `item` - Items (books, movies, music) for recommendation
- `user_item` - User-item interactions

### Key Services
- `UserService` - User CRUD, authentication, tag search, matching algorithm, recommendations with Redis caching
- `TeamService` - Team creation, joining, quitting, listing
- `UserTeamService` - User-team relationship management

### Configuration Notes
- Context path: `/api`
- Server port: `8080`
- Cross-origin enabled for `http://localhost:5173/`
- Session timeout: 86400 seconds (24 hours)
- Hot reload enabled via spring-boot-devtools

