# Repository Guidelines

## Project Structure & Module Organization
This repository is a multi-module flash sale platform. Backend services live in `flash-sale-*`, shared libraries in `flash-sale-common/`, and the Vue 3 client in `frontend/`. Deployment assets and seed SQL live in `deploy/`, helper scripts in `scripts/`, and longer-form docs in `docs/`.

Use the standard Maven layout in each backend module: `src/main/java`, `src/main/resources`, and `src/test/java`. Keep new code inside the existing service boundaries.

## Build, Test, and Development Commands
Run backend commands from the repository root unless you are targeting a single module:

- `mvn -q -DskipTests install` - build and install all Java modules locally
- `mvn -q test` - run all backend tests
- `mvn -q -pl flash-sale-user-service test` - run one module's tests
- `mvn -q -pl flash-sale-gateway spring-boot:run` - start one backend service

Run frontend commands from `frontend/`:

- `npm install` - install dependencies
- `npm run dev` - start the Vite dev server
- `npm run build` - type-check and build production assets
- `npm test` - run Vitest once

## Coding Style & Naming Conventions
Java code uses 4-space indentation, layered packages (`controller`, `service`, `mapper`, `dto`, `domain`), and `UpperCamelCase` type names. Keep REST paths consistent with existing `/api/...` patterns.

Frontend code follows the current Vue + TypeScript style: `PascalCase` for components and views, `camelCase` for utilities and stores, and colocated API helpers under `frontend/src/api/`. Match the existing semicolon-light formatting and import style.

## Testing Guidelines
Backend tests use JUnit with Spring Boot test support and live under `src/test/java`; test resources belong in `src/test/resources`. Name test classes after the target class or workflow, such as `PaymentControllerTest`.

Frontend tests use Vitest from `frontend/src/test/`. Add focused tests for changed behavior, especially controller endpoints, service logic, router guards, and API wrappers.

## Commit & Pull Request Guidelines
Recent history uses Conventional Commit-style prefixes such as `feat:`. Follow that pattern with concise, scoped summaries, for example `fix: handle duplicate seckill requests`.

Pull requests should describe the affected module(s), summarize behavior changes, list verification commands you ran, and include screenshots for UI work.

## Security & Configuration Tips
Copy `.env.example` to `.env` for local setup and never commit secrets, local logs, or generated exports. Treat `logs/`, temporary test artifacts, and environment-specific overrides as local-only unless they are intentionally part of repository setup.
