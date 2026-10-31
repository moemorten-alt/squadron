# Squadron Project

This repository contains a full-stack application called **Squadron**. It consists of two main parts:

1. **backend** (Java/Spring Boot)
2. **frontend** (Next.js/React + TypeScript)

The goal of this document is to give a newcomer a simple overview of how the project is structured, how to run it, and where to make changes.

---

## 📁 Project Structure

- `backend/`: Java-based service using Spring Boot. Contains business logic, database migrations, and REST controllers.
  - `src/main/java/com/squadron/`: main packages (config, controller, dto, entity, exception, repository, security, service).
  - `resources/`: Spring properties, SQL scripts, Flyway migrations.
  - `pom.xml`: Maven configuration.

- `frontend/`: Next.js application with React components and context for authentication and UI.
  - `src/app`: page/routes structure.
  - `src/components`: UI components organized by feature.
  - `src/context`: React context providers.
  - `src/lib`: utilities and API wrapper.
  - `src/types`: shared TypeScript types.
  - `package.json`, `tsconfig.json`, etc.

---

## 🚀 Running the Services

### Prerequisites
- Java 17+ and Maven (for `backend`)
- Node.js 18+ and npm/yarn (for `frontend`)
- A local database (PostgreSQL) configured according to each service's configuration.

### Backend (Java)
```powershell
cd backend
mvn spring-boot:run
```
The service listens on default port 8080. Configuration lives in `application.yml` (overridden by `application-local.yml`).

### Frontend (Next.js)
```powershell
cd frontend
npm install
npm run dev
```
Visits `http://localhost:3000`. API calls are proxied or configured via `src/lib/api.ts`.

---

## 🔧 Development Notes

- **Adding a new backend endpoint**: implement the controller in Java, then update the DTO and service layer.
- **Database migrations**: Flyway SQL files under `backend/src/main/resources/db/migration`.
- **Frontend routing**: pages are under `src/app`. Data fetching is done via the API wrapper and React contexts manage auth.
- **Authentication**: handled with JWT; see `backend/src/main/java/com/squadron/security/` for details.

---

## ✅ Useful Commands

| Component    | Command                              | Purpose                         |
|--------------|--------------------------------------|---------------------------------|
| backend      | `mvn clean package`                  | Build jar                       |
| backend      | `mvn test`                           | Run the Java test suite         |
| frontend     | `npm run lint` / `npm test`          | Lint or run the frontend tests  |

---

## 📌 Contact / Collaboration
If you have questions or want to add features, feel free to reach out! Start by exploring the relevant directories above and running the services locally. Keep things simple and follow the existing patterns.

Happy coding!