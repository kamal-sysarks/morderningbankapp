# Modern Banking App

A modernized web application rebuilt from the Legacy Banking Transaction Management System.

**Stack:** React 19 + TypeScript · Java 21 + Spring Boot 3.3 · SQLite

---

## Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| JDK | 21 | `java -version` |
| Maven | 3.6+ | `mvn -version` |
| Node.js | 18+ LTS | `node --version` |
| npm | 9+ | `npm --version` |
| sqlite3 | any | `sqlite3 --version` |
| Docker (optional) | recent | `docker compose version` |

---

## Quick Start (local)

### Step 1 — Initialize the database

From the **repo root**:

```bash
sh modern/database/init.sh
```

This creates `legacybank.db` in the repo root (or at the path set by `DB_PATH`).

### Step 2 — Start the backend

```bash
cd modern/backend
mvn spring-boot:run
```

Backend starts on **http://localhost:8080**. Verify: `curl http://localhost:8080/actuator/health`

### Step 3 — Start the frontend

In a **second terminal**:

```bash
cd modern/frontend
npm install
npm run dev
```

Frontend starts on **http://localhost:5173**

---

## Demo credentials

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `admin123` |

---

## Run tests

**Backend** (requires Java 21 + Maven):

```bash
cd modern/backend
mvn test
```

**Frontend:**

```bash
cd modern/frontend
npm test
```

---

## Docker Compose (optional)

```bash
cd modern
docker compose up --build
```

Both services start. Frontend at **http://localhost:5173**.

To stop:

```bash
docker compose down
```

---

## Verify legacy files are unchanged

```bash
# Generate baseline (run once after first setup)
sh modern/scripts/check-legacy-integrity.sh --generate

# Verify (run any time)
sh modern/scripts/check-legacy-integrity.sh
```

---

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_PATH` | `../../../legacybank.db` (relative to backend) | Path to the SQLite database file |
| `JWT_SECRET` | built-in default (dev only) | HS256 JWT signing secret — **change in production** |
| `CORS_ORIGINS` | `http://localhost:5173` | Comma-separated allowed origins |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `java`, `mvn`, `node` not found | Install the tool, add to `PATH`, open a new terminal |
| Backend fails to start: DB not found | Run `sh modern/database/init.sh` first; check `DB_PATH` |
| Frontend can't reach backend (`Network Error`) | Confirm backend is running on port 8080; check Vite proxy config in `vite.config.ts` |
| Login returns 401 | Run `sh modern/database/init.sh` to ensure BCrypt passwords are applied |
| Port conflict (8080 or 5173) | Change `server.port` in `application.properties` or `--port` in Vite |
| `sqlite3: command not found` | Install sqlite3: `brew install sqlite` (macOS) |

---

## Project structure

```
modern/
├── database/
│   ├── init.sh               # DB initialization script
│   └── CONSTRAINTS.md        # Data-integrity audit
├── backend/
│   ├── pom.xml               # Spring Boot 3.3 Maven project
│   └── src/
│       ├── main/java/com/modernbank/
│       └── test/java/com/modernbank/
├── frontend/
│   ├── package.json          # Vite + React 19 project
│   └── src/
│       ├── api/              # Axios API clients
│       ├── context/          # Auth context
│       ├── pages/            # All page components
│       └── test/             # Vitest tests
├── scripts/
│   └── check-legacy-integrity.sh
├── docker-compose.yml
└── README.md                 # This file
```

---

## Guardrails

- All new code lives under `modern/`
- Legacy `src/`, `pom.xml`, and `legacybank.db` are **never modified**
- Run `sh modern/scripts/check-legacy-integrity.sh` to verify
