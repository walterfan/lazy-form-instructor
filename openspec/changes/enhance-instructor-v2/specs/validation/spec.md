## ADDED Requirements

### Requirement: Custom Validation Framework
The system SHALL support custom Java-based validators for business logic beyond JSON Schema validation.

#### Scenario: Annotation-Based Validator
- **GIVEN** a DTO field annotated with `@SchemaValidate(validator = DateRangeValidator.class)`
- **WHEN** parsing completes and passes schema validation
- **THEN** the custom validator MUST be invoked with the field value
- **AND** validation errors MUST be added to the `ParsingResult`

#### Scenario: Cross-Field Validation
- **GIVEN** a `DateRangeValidator` that checks start date < end date
- **WHEN** the validator is invoked
- **THEN** it MUST have access to the full object via `ValidationContext`
- **AND** can read other fields for cross-field validation logic

#### Scenario: Custom Validator Failure
- **GIVEN** a custom validator that returns `ValidationResult.invalid("error message")`
- **WHEN** validation runs
- **THEN** the error MUST be included in the `ParsingResult.errors()`
- **AND** if configured, trigger an LLM retry with the validation error

### Requirement: Validation Context
The system SHALL provide validators with access to the full object and metadata.

#### Scenario: Context Access
- **GIVEN** a custom validator
- **WHEN** the `validate` method is called
- **THEN** the `ValidationContext` MUST include:
  - The full object being validated
  - The field name being validated
  - The original user input
  - The intent context (if provided)

#### Scenario: Nested Object Validation
- **GIVEN** a DTO with nested objects (e.g., `LeaveRequest.attendees[]`)
- **WHEN** a validator is applied to a nested field
- **THEN** the context MUST provide path to the nested field
- **AND** access to parent object for hierarchical validation

### Requirement: Validator Composition
The system SHALL support composing multiple validators for a single field.

#### Scenario: Multiple Validators on One Field
- **GIVEN** a field with `@SchemaValidate(validators = {RangeValidator.class, BusinessHoursValidator.class})`
- **WHEN** validation runs
- **THEN** all validators MUST execute in order
- **AND** all validation errors MUST be collected

#### Scenario: Short-Circuit on Failure
- **GIVEN** a validation chain with `failFast=true`
- **WHEN** the first validator fails
- **THEN** subsequent validators MUST be skipped
- **AND** only the first error is reported

### Requirement: Validation Retry Integration
The system SHALL integrate custom validation errors into the retry loop.

#### Scenario: Retry on Custom Validation Failure
- **GIVEN** custom validation fails after schema validation passes
- **WHEN** `RetryConfig.retryOnCustomValidation=true`
- **THEN** the system MUST retry with the LLM
- **AND** include the custom validation error in the retry prompt

#### Scenario: Validation Error Formatting for LLM
- **GIVEN** a custom validator returns an error "Start date must be before end date"
- **WHEN** a retry is triggered
- **THEN** the error message MUST be included in the LLM prompt
- **AND** formatted for LLM understanding (e.g., "Field 'start_date' has error: ...")

### Requirement: Built-in Validators
The system SHALL provide common validators out-of-the-box.

#### Scenario: Date Range Validator
- **GIVEN** a `DateRangeValidator` configured with `startField="start_date"`, `endField="end_date"`
- **WHEN** the end date is before the start date
- **THEN** validation MUST fail with a clear error message

#### Scenario: Enum Value Validator
- **GIVEN** an `EnumValidator` with allowed values ["annual", "sick", "personal"]
- **WHEN** the field value is "vacation"
- **THEN** validation MUST fail suggesting closest matches

#### Scenario: Pattern Validator
- **GIVEN** a `PatternValidator` with regex `^[A-Z]{2}\\d{6}$` for employee ID
- **WHEN** the field value doesn't match the pattern
- **THEN** validation MUST fail with a format hint

### Requirement: Validation Performance
The system SHALL ensure custom validation does not significantly impact performance.

#### Scenario: Validation Timeout
- **GIVEN** a custom validator takes longer than 5 seconds
- **WHEN** validation runs
- **THEN** the validator MUST be interrupted
- **AND** a `ValidationTimeoutException` thrown

#### Scenario: Parallel Validation
- **GIVEN** multiple independent field validators
- **WHEN** validation runs
- **THEN** validators MAY execute in parallel
- **AND** all results are collected before returning

