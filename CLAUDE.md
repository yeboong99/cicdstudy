# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

Multi-stage Dockerfile (JDK 17 build → JRE 17 runtime). Docker Compose orchestrates MySQL 8.0 + app on port 8080.

Production delivery uses `.github/workflows/deploy.yml` (GitHub-hosted runner):
- build (`./gradlew build`)
- tag/release creation
- GHCR image push
- AWS SSM-based deployment to EC2

- Deploy job copies `docker-compose.prod.yml` to `~/app/` on the server, then runs from there.
- Environment variables are managed directly in `~/app/.env` on the server (not via GitHub Secrets).
- `restart: unless-stopped` policy ensures containers auto-restart after server reboot.
- Manual server management: `cd ~/app && docker compose -f docker-compose.prod.yml down/up -d/logs -f`

## PR and Merge Flow

PR review/merge decisions are manual (PL/user-driven). No repository-level auto-merge orchestration workflow is configured.
