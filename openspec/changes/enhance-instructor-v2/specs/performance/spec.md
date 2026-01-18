## ADDED Requirements

### Requirement: Response Caching
The system SHALL cache LLM responses to avoid redundant API calls for identical requests.

#### Scenario: Cache Hit
- **GIVEN** a cache is configured
- **WHEN** the same parsing request is made twice
- **THEN** the first request MUST call the LLM
- **AND** the second request MUST return the cached response without calling the LLM
- **AND** cache hit is reported via hooks

#### Scenario: Cache Key Generation
- **GIVEN** a parsing request with specific prompt, model, schema, and temperature
- **WHEN** generating the cache key
- **THEN** the key MUST include all parameters that affect the response
- **AND** use a cryptographic hash (SHA-256) to ensure uniqueness
- **AND** include library version to invalidate on upgrades

#### Scenario: Cache Miss on Different Parameters
- **GIVEN** two requests with identical prompts but different temperatures
- **WHEN** cache lookup occurs
- **THEN** they MUST produce different cache keys
- **AND** result in separate LLM calls

#### Scenario: Cache Expiration
- **GIVEN** a cache with TTL of 1 hour
- **WHEN** a cached response is older than 1 hour
- **THEN** it MUST be considered stale
- **AND** a new LLM call is made

### Requirement: Cache Implementations
The system SHALL provide multiple cache implementation strategies.

#### Scenario: NoOpCache Default
- **GIVEN** no cache is configured
- **WHEN** parsing requests are made
- **THEN** the default `NoOpCache` MUST be used
- **AND** no caching overhead occurs

#### Scenario: InMemoryCache with LRU Eviction
- **GIVEN** an `InMemoryCache` with `maxSize=100`
- **WHEN** the 101st unique request is cached
- **THEN** the least recently used entry MUST be evicted
- **AND** cache size remains at 100

#### Scenario: FileCache Persistence
- **GIVEN** a `FileCache` with directory `/tmp/instructor-cache`
- **WHEN** a response is cached
- **THEN** it MUST be persisted to disk
- **AND** survive application restarts
- **AND** be available in subsequent runs

#### Scenario: Cache Warmup
- **GIVEN** a `FileCache` with existing entries
- **WHEN** the application starts
- **THEN** the cache MAY load entries into memory for faster access
- **AND** validate file integrity before loading

### Requirement: Cache Invalidation
The system SHALL support cache invalidation strategies.

#### Scenario: Manual Cache Clear
- **GIVEN** a cache instance
- **WHEN** `cache.clear()` is called
- **THEN** all cached entries MUST be removed
- **AND** subsequent requests result in LLM calls

#### Scenario: Selective Invalidation
- **GIVEN** a cache with multiple entries
- **WHEN** `cache.invalidate(keyPattern)` is called
- **THEN** only entries matching the pattern MUST be removed
- **AND** other entries remain intact

#### Scenario: TTL-Based Expiration
- **GIVEN** cached entries with different TTLs
- **WHEN** periodic cleanup runs
- **THEN** expired entries MUST be removed
- **AND** memory/disk space is reclaimed
