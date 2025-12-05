# Repository Guidelines

## Project Structure & Module Organization
- Monorepo: Spring Boot backend in `friends-finder-backend`, Vue 3 + Vite frontend in `friends-finder-frontend`.
- Backend: code under `src/main/java`, config in `src/main/resources`, SQL seeds in `sql/`, tests in `src/test/java`, build output in `target/`.
- Frontend: app code in `src` (views, router, composables, Vant UI), static assets in `public/`; generated type stubs live in `components.d.ts` and `auto-imports.d.ts`.

## Build, Test, and Development Commands
- Backend: `cd friends-finder-backend && ./mvnw spring-boot:run` (local API), `./mvnw clean test` (unit/integration), `./mvnw package` (jar in `target/`).
- Frontend: `cd friends-finder-frontend && pnpm install` (once), `pnpm dev` (Vite dev server), `pnpm build` (production bundle), `pnpm preview` (serve built assets), `pnpm lint` / `pnpm lint:fix` (eslint + prettier).
- Run backend and frontend together when touching contracts; update shared DTOs/clients in the same PR.

## Coding Style & Naming Conventions
- Java: 4-space indent, `PascalCase` classes, `camelCase` members, prefer constructor injection, keep controllers thin with DTOs and services in dedicated packages.
- TypeScript/Vue: Prettier/ESLint enforce 2-space indent, semicolon-free, single quotes. Components in `PascalCase.vue`, composables `useX.ts`, route paths kebab-case. Keep API clients under a `services` or `api` directory.
- Run `pnpm lint` and format before pushing; for Java, rely on IDE defaults or Spring formatting.

## Testing Guidelines
- Backend: tests end with `*Test.java` in `src/test/java`; use `@SpringBootTest` for integration and slices for controllers/repos. Cover happy/error paths for endpoints and services.
- Frontend: no bundled test runner yet; add Vitest + `*.spec.ts` near components when adding features. Until then, lean on linting and manual verification in `pnpm dev`.

## Commit & Pull Request Guidelines
- Commits: short imperative subject (`Add friend search API`), keep scope tight, reference issue IDs when applicable.
- PRs: include summary, test commands run (mvn, pnpm lint/build), note schema/API changes, and attach UI screenshots or GIFs. Seek review from owners of the module you changed.








