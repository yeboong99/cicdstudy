# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Working Rules

- `.md` 파일을 제외한 모든 파일은 직접 생성/수정/삭제하지 않는다.
- 코드 변경이 필요하면 전체 코드 스니펫을 사용자에게 보여주고, 사용자가 직접 작성한다.

## Build & Development Commands

- `./gradlew bootRun` — Run the Spring Boot app (default profile: local)
- `./gradlew test` — Run JUnit test suite
- `./gradlew build` — Full build (compile + test + assemble JAR)
- `./gradlew clean` — Remove build artifacts
- `./gradlew bootJar` — Create executable JAR (used in Docker builds)
- `docker compose up --build` — Run full stack (MySQL + app) via Docker

## Environment Setup

The app requires MySQL. Database credentials are loaded from a `.env` file (see `.env.example`):
- `DB_URL` — JDBC connection string
- `DB_USERNAME` / `DB_PASSWORD` — Database credentials

Profiles: `local` (default, set in `ForcicdApplication.java`) and `prod`.

## Architecture

Spring Boot 4.0.2 / Java 17 / Gradle 9.3.0 — layered MVC application.

- **Base package:** `com.neulboong.forcicd`
- **Controller → Service** layering (e.g., `calculator/controller/`, `calculator/service/`)
- **CalculatorController** — REST API (`/cal/add`, returns JSON)
- **HomeController** — Thymeleaf web UI (`/`, renders `templates/home.html`)
- **CalculatorService** — Business logic, injected via Lombok `@RequiredArgsConstructor`

Config files: `application.yml` (base), `application-local.yml`, `application-prod.yml` — all YAML-based with env variable interpolation.

## Coding Conventions

- Java 17 toolchain; 4-space indentation; braces on same line
- Use Lombok annotations (`@RequiredArgsConstructor`, `@Getter`, etc.) to reduce boilerplate
- No linter/formatter configured — match existing file style
- Test classes use `*Tests` suffix, mirror main package structure in `src/test/java`

## Commit Convention

`[Type] Summary` format (e.g., `[Feature] Add user login`, `[Fix] Handle null input`)

## Deployment

Multi-stage Dockerfile (JDK 17 build → JRE 17 runtime). Docker Compose orchestrates MySQL 8.0 + app on port 8080. CI/CD via GitHub Actions to self-hosted runner is planned but not yet configured.
