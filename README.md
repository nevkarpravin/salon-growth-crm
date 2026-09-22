# Salon Growth CRM — Core CRM Module

Monorepo for an AI-driven salon growth CRM. This phase delivers the Core CRM module:
client profiles, visit history, formula cards, consent/preferences, tags, lifecycle
segments, CSV import, and a responsive React admin UI.

## Layout

- `backend/` — Spring Boot 3.3 (Java 17, Maven wrapper), H2 by default, PostgreSQL via `postgres` profile
- `frontend/` — React 18 + Vite + TypeScript + Tailwind CSS + react-router-dom

## Run the backend

```bash
cd backend
./mvnw spring-boot:run
```

- API base: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 console: `http://localhost:8080/h2-console` (jdbc url `jdbc:h2:mem:saloncrm`, user `sa`)
- Seeds ~15 Indian-salon demo clients (set `crm.seed=false` to disable)

PostgreSQL:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/saloncrm \
SPRING_DATASOURCE_USERNAME=user \
SPRING_DATASOURCE_PASSWORD=pass \
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Run the frontend

```bash
cd frontend
npm i
npm run dev
```

Dev server at `http://localhost:5173`, proxying `/api` to `localhost:8080`.
Requires backend running.

## Features

**Checkout & Sales (POS)**
- Product catalog with SKU, stock tracking, low-stock thresholds
- POS checkout: service/product lines, qty & line discounts, order discount, tip, GST (crm.tax.rate), split payments
- Pay finalizes: `INV-<yyyy>-<seq>` invoice, stock decrement, low-stock warnings, appointment → COMPLETED + single synced Visit
- `POST /sales/from-appointment/{id}` pre-fills a draft from an appointment; void/refund restore stock
- Sales list with date filters + summary endpoint (revenue, avg ticket, service vs retail, by payment method)
- Printable invoice view; mobile POS = catalogue + sticky cart bottom sheet

**Appointments & Scheduling**
- Staff CRUD with roles, colors, and per-weekday working hours
- Service catalog with duration + processing time and pricing
- Appointment booking with overlap (409) and working-hours (400) validation
- Status lifecycle BOOKED → CONFIRMED/COMPLETED/CANCELLED/NO_SHOW; completing books a client Visit
- Availability endpoint (`/api/v1/appointments/availability`) — 15-min slot grid
- Calendar UI: day time-grid per staff on desktop, grouped list on mobile; booking & reschedule sheets

**Core CRM**

- Client CRUD with soft-delete (ARCHIVED), unique phone, validation via RFC-7807 ProblemDetail
- Tags (replace-set endpoint), consent & channel preferences
- Visits (services/products/amount/stylist) and Formula Cards per client
- Merged timeline (visits + formulas, desc by date)
- Auto-computed segments: NEW, AT_RISK, LAPSED, VIP, BIRTHDAY_THIS_MONTH
  (thresholds configurable under `crm.segments.*`)
- Client search (name/phone/email), tag & segment filters, pagination
- CSV import (multipart, per-row error report, duplicate-phone skip) + template download
- `/api/v1/segments` counts and `/api/v1/tags` counts
- Responsive UI: sidebar on desktop, top bar + bottom tabs on mobile (works down to 360px)

## Tests

```bash
cd backend && ./mvnw test
cd frontend && npm run build && npm run lint
```
