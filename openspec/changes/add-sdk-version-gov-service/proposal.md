# Change: Add SDK Version Governance Service

## Why
- Need centralized visibility of agent/SDK versions across environments to reduce outages from outdated clients.
- Require lifecycle control (announce → warn → enforce → remove) with policy sync from release portal and enforcement/alerting.
- Support supportability and product analytics (adoption curves, ratios of new/old/deprecated versions).

## What Changes
- Introduce a dedicated SDK Version Governance (SVG) service: version visibility dashboards, lifecycle policy management, enforcement/alerting.
- Integrate with release portal API for version repository sync; manage policies (min/recommended/deprecated, block flags, timelines).
- Provide notifications/announcements (email/webhook) and aggregated alerts; expose catalog APIs for versions, release notes, tutorials.
- Integrate with existing telemetry storage (Elasticsearch/Cube) for usage and drill-down; optional MMS/dashboard integration.

## Impact
- Affected specs: new `version-governance-service` capability.
- Affected code: new SVG service (API, portal sync, policy engine, alert/notification modules, UI/dashboard integration points).
