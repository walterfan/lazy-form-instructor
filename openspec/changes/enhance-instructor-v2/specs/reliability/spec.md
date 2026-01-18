## ADDED Requirements

### Requirement: Configurable Retry Strategy
The system SHALL support configurable retry strategies with exponential backoff and jitter.

#### Scenario: Exponential Backoff Configuration
- **GIVEN** a `RetryConfig` with `backoff=EXPONENTIAL`, `initialInterval=1s`, `multiplier=2.0`
- **WHEN** retries are triggered
- **THEN** retry delays MUST follow the pattern: 1s, 2s, 4s, 8s, ...
- **AND** respect the configured `maxAttempts`

#### Scenario: Jitter Application
- **GIVEN** a `RetryConfig` with `jitterFactor=0.1`
- **WHEN** calculating retry delay
- **THEN** a random jitter between -10% and +10% MUST be applied
- **AND** prevent thundering herd when multiple clients retry simultaneously

#### Scenario: Fixed Backoff Configuration
- **GIVEN** a `RetryConfig` with `backoff=FIXED`, `initialInterval=2s`
- **WHEN** retries are triggered
- **THEN** all retry delays MUST be exactly 2 seconds

#### Scenario: Exception-Specific Retry
- **GIVEN** a `RetryConfig` with `retryOn=[RateLimitException.class, ValidationException.class]`
- **WHEN** a `RateLimitException` occurs
- **THEN** the system MUST retry
- **WHEN** an `IncompleteOutputException` occurs
- **THEN** the system MUST NOT retry (not in the list)

### Requirement: Circuit Breaking
The system SHALL implement circuit breaking to prevent cascading failures when the LLM provider is consistently failing.

#### Scenario: Circuit Open After Threshold
- **GIVEN** a circuit breaker with `failureThreshold=5`, `timeout=30s`
- **WHEN** 5 consecutive failures occur
- **THEN** the circuit MUST open
- **AND** subsequent requests MUST fail fast without calling the LLM

#### Scenario: Circuit Half-Open State
- **GIVEN** an open circuit after the timeout period
- **WHEN** the next request arrives
- **THEN** the circuit enters half-open state
- **AND** allows one probe request through
- **AND** closes if successful, or reopens if failed

#### Scenario: Circuit Reset on Success
- **GIVEN** a circuit in half-open state
- **WHEN** a probe request succeeds
- **THEN** the circuit MUST close
- **AND** the failure counter resets to zero

### Requirement: Retry Exhaustion Handling
The system SHALL provide detailed information when retries are exhausted.

#### Scenario: Retry Exhaustion Exception
- **GIVEN** a `RetryConfig` with `maxAttempts=3`
- **WHEN** all 3 attempts fail
- **THEN** the system MUST throw `RetryExhaustedException`
- **AND** include a list of errors from each attempt
- **AND** include the total number of attempts made

#### Scenario: Partial Results on Exhaustion
- **GIVEN** retry exhaustion occurs
- **WHEN** the last attempt produced partial valid fields
- **THEN** the exception MAY include a `ParsingResult` with those fields
- **AND** clearly mark validation errors for failed fields

### Requirement: Timeout Management
The system SHALL enforce request timeouts to prevent indefinite hangs.

#### Scenario: Overall Request Timeout
- **GIVEN** a `RetryConfig` with `overallTimeout=60s`
- **WHEN** the total parsing time (including retries) exceeds 60 seconds
- **THEN** the system MUST abort and throw `TimeoutException`
- **AND** include how many attempts were completed

#### Scenario: Per-Attempt Timeout
- **GIVEN** a `RetryConfig` with `perAttemptTimeout=15s`
- **WHEN** a single LLM call exceeds 15 seconds
- **THEN** that attempt MUST be aborted
- **AND** the retry logic proceeds to the next attempt if within limits

### Requirement: Backpressure Handling
The system SHALL apply backpressure when the LLM provider signals capacity issues.

#### Scenario: Rate Limit Respect
- **GIVEN** the LLM provider returns `Retry-After: 30s` header
- **WHEN** a `RateLimitException` is thrown
- **THEN** the system MUST wait at least 30 seconds before retry
- **AND** override the calculated exponential backoff if longer

#### Scenario: Adaptive Backoff
- **GIVEN** repeated rate limit errors
- **WHEN** retries continue to hit rate limits
- **THEN** the system MAY increase the backoff multiplier adaptively
- **AND** log warnings about persistent rate limiting

