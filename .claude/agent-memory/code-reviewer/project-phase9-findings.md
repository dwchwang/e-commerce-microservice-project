---
name: Phase 9 Key Issues
description: Critical and major bugs found in Phase 9 code review - observability, OpenAPI, tests
type: project
---

Key issues found in Phase 9 review (2026-05-02):

1. **Grafana datasource UID mismatch (Major):** datasource.yml has no explicit `uid` field — Grafana auto-generates a random UID. All 3 dashboards hardcode `"uid": "Prometheus"` for the datasource reference. Fix: add `uid: prometheus` to datasource.yml and update dashboard references.

2. **jvm_gc_pause_seconds metric removed (Major):** Spring Boot 3.5 uses Micrometer 1.14. The metric `jvm_gc_pause_seconds_sum` was replaced by `jvm_gc_duration_seconds_sum` in Micrometer 1.12+. The JVM overview dashboard will show empty panels.

3. **springdoc not configured in identity, user, cart, voucher config-server YAMLs (Minor):** These 4 services have OpenApiConfig.java but no `springdoc.swagger-ui.path` in their config-server config files. Swagger UI still works at default path `/swagger-ui/index.html`.

4. **X-Forwarded-For header not sanitized (security concern, Minor in dev):** RateLimiterConfig trusts `X-Forwarded-For` without configuring a trusted proxy list. A client can spoof this header to bypass per-IP rate limiting. Acceptable for dev/thesis but would be Major in production.

5. **show-details: always on all services (informational):** Exposes DB connection info in /actuator/health. Services are behind gateway with /actuator/** whitelisted. Not blocked in this architecture.

**Why:** Findings from reviewing all Phase 9 new/modified files.
**How to apply:** If Phase 10 adds more observability, verify the Grafana datasource UID fix was applied.
