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

**Virtual Room (WhatsApp queue)**

- Walk-in queue (`/api/v1/queue`): per-day tokens, position + ETA (service durations / active staff), statuses WAITING → CALLED → IN_SERVICE → COMPLETED / SKIPPED / CANCELLED / EXPIRED
- Skip moves a ticket back `crm.virtual-room.requeue-positions` places; the `max-skips`-th skip cancels it (NO_SHOW)
- "You're next" notification when a waiting ticket reaches `notify-at-position`
- WhatsApp bot (`WhatsAppConversationService`): menu-driven join/status/pay/review/leave over `POST /api/v1/whatsapp/webhook` (Meta Cloud payload) and `POST /api/v1/whatsapp/simulate` (dev)
- Bills: `finish` creates a Visit, sets payment PENDING and a UPI intent link (`upi://pay?...`); `markPaid` is idempotent and sends a receipt; review prompt on payment (or after `review-delay-minutes`)
- Public mobile page `/q/:id` (`/api/v1/public/queue/{id}`): live position/ETA, leave queue, pay via UPI, leave a rating — client phone masked
- Queue board UI at `/queue` (polls 10s), WhatsApp simulator UI at `/whatsapp` (polls outbox 5s)

Config keys (all under `crm.virtual-room.*`, env overrides in `application.yml`):
`salon-name`, `notify-at-position`, `requeue-positions`, `max-skips`, `presence-timeout-minutes`,
`review-delay-minutes`, `google-review-url`, `public-base-url`, `payments.upi-id`,
`payments.payee-name`, `whatsapp.provider` (LOG|META), `whatsapp.access-token`,
`whatsapp.phone-number-id`, `whatsapp.verify-token`.

**Connecting real WhatsApp**

Set `WHATSAPP_PROVIDER=META`, `WHATSAPP_ACCESS_TOKEN`, and `WHATSAPP_PHONE_NUMBER_ID` from your
Meta app (WhatsApp Business Cloud API), then point the Meta webhook at
`POST /api/v1/whatsapp/webhook` and verify it with `GET /api/v1/whatsapp/webhook` using
`WHATSAPP_VERIFY_TOKEN` (default `salon-verify`). With the default `LOG` provider, outbound
messages are just written to the `outbound_messages` table (browse them in the simulator page
or via `GET /api/v1/whatsapp/messages`).

## Tests

```bash
cd backend && ./mvnw test
cd frontend && npm run build && npm run lint
```
