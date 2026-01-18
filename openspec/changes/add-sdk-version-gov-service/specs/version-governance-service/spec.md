## ADDED Requirements

### Requirement: Version Visibility & Analytics
The SVG service SHALL provide visibility into agent/SDK usage across environments, including per-version counts, rollout percentages, and ratios of new/old/deprecated versions.

#### Scenario: Adoption Curve
- **WHEN** querying for a client type and environment
- **THEN** the service SHALL return percentage rollout per version (e.g., 0%, 50%, 90%, 99%) and counts split by environment.

### Requirement: Lifecycle Policy Management
The SVG service SHALL manage lifecycle policies (announce → warn → enforce → remove) using min/recommended/deprecated versions and block flags.

#### Scenario: Phase Progression
- **GIVEN** a policy with timelines and min/deprecated versions
- **WHEN** the current date crosses the enforce date
- **THEN** the service SHALL mark the phase as enforce and surface enforcement guidance (e.g., block or warn).

### Requirement: Release Portal Sync
The SVG service SHALL sync version repository data daily from the release portal (serviceId=353) and update catalog/policies.

#### Scenario: Daily Sync
- **WHEN** the scheduled sync runs
- **THEN** it SHALL fetch portal data with auth token
- **AND** refresh catalog entries (min/max/recommended/deprecated, block flags) used by policies and visibility APIs.

### Requirement: Alerts & Notifications
The SVG service SHALL send aggregated alerts/notifications for old/deprecated versions with configurable channels and dedupe windows.

#### Scenario: Aggregated Alert
- **GIVEN** multiple obsolete-version signals for the same namespace/application/sdkVersion within a window
- **WHEN** alerts are emitted
- **THEN** the service SHALL send a single aggregated alert via configured channels (email/webhook) honoring allow/deny lists.

### Requirement: Catalog & Knowledge Hub
The SVG service SHALL expose catalog APIs for supported versions and provide access to release notes/tutorials.

#### Scenario: Catalog Query
- **WHEN** a client calls the catalog API
- **THEN** the service SHALL return supported/min/recommended/deprecated versions plus links to release notes/tutorials for that client type.
