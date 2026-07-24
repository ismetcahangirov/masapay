# Flyway migrations

Database schema changes are managed exclusively through Flyway migrations in
this directory. Flyway runs automatically on application startup and the
application will not start if a migration fails (fail-fast).

## Conventions

- **Naming**: `V<version>__<snake_case_description>.sql`, e.g.
  `V2__add_waiter_assignment.sql`. Versions are integers and strictly
  increasing; do not reuse or skip-then-backfill a version.
- **Immutability**: never edit a migration that has already been merged/applied.
  Flyway stores a checksum in `flyway_schema_history` and a mismatch aborts
  startup (`validate-on-migrate: true`). Fix mistakes with a new migration.
- **Ordering**: out-of-order migrations are disabled (`out-of-order: false`), so
  always add the next sequential version.
- **Repeatable migrations** (views, functions): use the `R__<description>.sql`
  prefix; these re-run whenever their checksum changes.
- **One logical change per migration**, matching the project's small-commit rule.

## Safety

- `clean-disabled: true` — `flyway clean` is permanently disabled so the schema
  can never be dropped by Flyway, in any environment.
- Only `V*`/`R*` prefixed `.sql` files are picked up; this `README.md` is ignored.

## Local verification

Run a throwaway PostgreSQL 16 and let the app migrate on boot:

```
docker run -d --name masapay-pg -e POSTGRES_DB=masapay \
  -e POSTGRES_USER=masapay -e POSTGRES_PASSWORD=masapay -p 5433:5432 postgres:16-alpine
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/masapay ./gradlew bootRun
```

Applied migrations are recorded in the `flyway_schema_history` table.
