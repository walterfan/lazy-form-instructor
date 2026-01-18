# schema-parsing Specification

## Purpose
TBD - created by archiving change create-smart-form-instructor. Update Purpose after archive.
## Requirements
### Requirement: JSON Schema Support
The system SHALL support standard JSON Schema definitions for fields, including types, enums, and basic validation constraints.

#### Scenario: Enum Validation
- **WHEN** schema defines an enum ["A", "B"] and input implies "C"
- **THEN** the system should attempt to map "C" to "A" or "B" or return low confidence/error, not an invalid value.

