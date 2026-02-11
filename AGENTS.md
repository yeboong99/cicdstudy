# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java` holds application source code (Spring Boot). Example: `com.neulboong.forcicd.calculator`.
- `src/main/resources` contains configuration and assets; `application.properties` is the primary config file.
- `src/main/resources/templates` is for Thymeleaf views; `src/main/resources/static` for static assets.
- `src/test/java` contains tests, currently under `com.neulboong.forcicd`.
- Build outputs go to `build/` (Gradle default).

## Build, Test, and Development Commands
- `./gradlew bootRun` runs the Spring Boot app locally.
- `./gradlew test` executes the JUnit test suite.
- `./gradlew build` compiles, runs tests, and assembles the app.
- `./gradlew clean` removes generated build artifacts.

## Coding Style & Naming Conventions
- Java 17 toolchain; standard Java conventions apply.
- Indentation: 4 spaces; braces on the same line.
- Package naming: lowercase, dot-delimited (e.g., `com.neulboong.forcicd`).
- Class naming: PascalCase; methods/fields: camelCase.
- Lombok is enabled; prefer Lombok annotations where it reduces boilerplate.
- No formatter or linter is configured in Gradle—keep formatting consistent with existing files.

## Testing Guidelines
- Testing uses JUnit Platform via Spring Boot test starters.
- Test classes follow `*Tests` naming (e.g., `ForcicdApplicationTests`).
- Place unit/integration tests in `src/test/java` mirroring main package structure.
- Run tests with `./gradlew test` before submitting changes.

## Commit & Pull Request Guidelines
- Git history is minimal; the only existing commit uses a bracketed prefix (`[Start] ...`).
- Until a convention is agreed, use short, descriptive commit messages; optional format: `[Type] Summary` (e.g., `[Fix] Handle null input`).
- PRs should include: a brief description, steps to test, and screenshots for UI changes (Thymeleaf/templates).
- Link related issues if applicable and note any config changes in `application.properties`.

## Configuration Tips
- Database settings (e.g., MySQL) should be managed in `src/main/resources/application.properties`.
- Avoid committing secrets; use environment variables or local overrides when needed.
