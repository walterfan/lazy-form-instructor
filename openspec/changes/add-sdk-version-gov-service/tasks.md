## 1. Scaffolding
- [ ] Create service skeleton (API module, portal-sync module, policy module).
- [ ] Define config schema (portal endpoints, auth, alert channels, dashboards).

## 2. Data & Ingestion
- [ ] Integrate with existing version telemetry storage (Elasticsearch/Cube) for reads.
- [ ] Define aggregation jobs/queries for version distributions and adoption curves.

## 3. Policy & Lifecycle
- [ ] Implement policy model (min/recommended/deprecated, block flags, timelines).
- [ ] Wire release-portal sync to refresh policies/catalog daily.
- [ ] Add enforcement/alert rules (announce → warn → enforce → remove) with intervals.

## 4. APIs & Visibility
- [ ] Expose catalog APIs (versions, release notes, tutorials, capabilities).
- [ ] Expose visibility APIs/dashboards for ratios, per-env usage, rollout percentages.
- [ ] Integrate optional MMS/dashboard surface.

## 5. Notifications & Alerts
- [ ] Implement alert aggregation and routing (webhook/email) with dedupe windows.
- [ ] Add configuration for recipients and per-namespace/application scoping.

## 6. Testing & Validation
- [ ] Acceptance tests for portal sync, policy evaluation, alert aggregation.
- [ ] Validate API responses and dashboard metrics against sample datasets.
