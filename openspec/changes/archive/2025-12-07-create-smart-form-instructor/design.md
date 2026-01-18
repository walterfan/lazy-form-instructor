# Design: SmartFormInstructor Architecture

## Context
The goal is to build a Java library similar to Python's Instructor but tailored for complex form filling ("Smart Form"). It needs to handle ambiguous user input, validate against a schema, and ask for confirmation when uncertain.

## Learnings from Python Instructor
The [Python Instructor library](https://github.com/567-labs/instructor) has established several successful patterns that we will adapt for the Java ecosystem:
1.  **Schema-First Extraction**: Just as Instructor uses Pydantic models to define expected output, we will strictly enforce structure using JSON Schema. This ensures the LLM knows exactly what format to produce.
2.  **Validation-Driven Retry Loop**: A core feature of Instructor is automatic retrying when validation fails. We will implement this by feeding validation errors (e.g., from JSON Schema or custom logic) back to the LLM in subsequent prompts to correct the output.
3.  **Type Safety**: Instructor bridges the gap between unstructured text and structured objects. Our library will focus on producing strongly-typed results (mapped to Java Records/POJOs via the JSON protocol) to provide a similar developer experience.
4.  **Provider Agnosticism**: Instructor works with many providers. By using Spring AI, we achieve this same goal, allowing the underlying model to be swapped without changing the form logic.

## Goals / Non-Goals
- **Goals**:
    - Decouple Form Schema (Business Logic) from NLP (AI Logic).
    - Standardize interaction via JSON.
    - Support complex validation and "confidence" scores.
    - Provide reasoning and alternatives for uncertain values.
- **Non-Goals**:
    - Building a new LLM.
    - Frontend UI implementation (only the protocol).

## Decisions
- **Decision**: Use JSON Schema for Form Definition.
    - **Why**: Standard, language-agnostic, rich validation support.
- **Decision**: Stateless Request/Response Model.
    - **Why**: Simplifies integration; context is passed in the request.
- **Decision**: Protocol-first design.
    - **Why**: Allows any backend (Java, Go) or Frontend to use the same logic if ported.
- **Decision**: Use Spring AI for LLM Integration.
    - **Why**: Provides a portable API across major AI providers (OpenAI, Azure, etc.), structured outputs, and seamless integration with the Spring ecosystem which fits the Java tech stack.

## Architecture
- **Input**:
    - `schema`: JSON Schema.
    - `user_input`: Natural language string.
    - `context`: JSON object (time, location, etc.).
- **Output**:
    - `fields`: Map of field names to `ParsingResult`.
        - `value`: Extracted value.
        - `confidence`: 0.0-1.0.
        - `reasoning`: Explanation.
        - `alternatives`: List of other possible values.

## Complex Form Modeling
### Form Schema Layer
- **Base**: JSON Schema (draft 2020-12 or compatible).
- **Extensions for complex forms**:
    - **Field groups / sections**: Logical grouping of fields into named sections (e.g., `requester`, `trip_segments`, `attachments`) with optional metadata such as display order and visibility rules.
    - **Nested objects**: Deeply nested structures to represent sub-forms (e.g., an array of `attendees`, each with `name`, `department`, `role`).
    - **Repeatable arrays**: Explicit modeling of repeatable blocks (e.g., multiple travel segments, multiple expense line items) using `type: "array"` plus item-level constraints.
    - **Cross-field constraints**: Declarative rules that span multiple fields (e.g., `start_date <= end_date`, `sum(line_items.amount) == total_amount`) expressed either via JSON Schema keywords (`allOf`, `anyOf`, `oneOf`, `if/then/else`) or an additional rules layer.
    - **Conditional fields / dependencies**: Rules such as:
        - `leave_type = "sick"` ⇒ `medical_certificate` is required.
        - `country = "US"` ⇒ `state` must be one of a specific enumeration.
      These dependencies SHOULD be machine-readable so both validation logic and prompt construction can use them.

### Runtime Input / Output
- **Input**:
    - `schema`: The extended JSON Schema-based Form Schema.
    - `user_input`: Natural language string.
    - `context`: JSON object (time, location, user profile, history, etc.).
- **Output**:
    - `fields`: Map of field identifiers (including nested paths like `attendees[0].name`) to `ParsingResult`.
        - `value`: Extracted value (primitive or structured object/array).
        - `confidence`: 0.0-1.0.
        - `reasoning`: Explanation.
        - `alternatives`: List of other possible values or structures for the same logical slot.
    - `errors`: Optional list of structural or business-rule violations detected after applying schema and policy rules.

### Multi-step / Wizard-style Forms
- **Step modeling**:
    - Each form MAY define an ordered list of `steps`, each referencing a subset of fields/sections.
    - Steps MAY depend on previous steps (e.g., `approval` step depends on `request_details` step).
- **Partial fills**:
    - SmartFormInstructor SHOULD be able to accept a `current_step` indicator and fill:
        - Fields belonging to the current step.
        - Fields in future steps when they can be confidently inferred from the same natural language input (e.g., \"I need sick leave next Monday for 3 days\" can determine both `basic_info` and `duration` steps).
- **Future-step hints**:
    - The result SHOULD mark fields in later steps that are already confidently determined so the UI can skip or pre-fill those steps.

### Ambiguity & Conflict Representation
- **Ambiguity per slot**:
    - A \"slot\" corresponds to a logical field or group instance (e.g., `travel_segments[0].destination_city`).
    - Each slot MAY contain:
        - a primary `value`,
        - an `alternatives` list with candidate values or structures,
        - a `confidence` score attached to each alternative.
- **Structural ambiguity**:
    - When it is unclear which array element a phrase refers to (e.g., \"Add another trip to Beijing\" when multiple segments exist), the system SHOULD:
        - Propose multiple candidate structures (e.g., new segment vs. modify last segment),
        - Mark the slot as low-confidence and explain the ambiguity in `reasoning`.
- **Cross-field conflicts**:
    - Examples: `start_date` after `end_date`, `total_amount` not matching the sum of line items.
    - These SHOULD be surfaced either as:
        - explicit `errors` entries, or
        - low-confidence values with `reasoning` describing the inconsistency and a recommendation.

## Intent Context & Business Rules
- **Intent Context usage**:
    - Context MAY include:
        - user profile (role, department, manager),
        - historical behavior (typical leave durations, common destinations),
        - environment (time zone, locale, working days).
    - Form Schema MAY reference context keys to define:
        - dynamic defaults (e.g., default `approver` = `managerId` from profile),
        - conditional visibility (e.g., extra fields for managers),
        - locale-aware parsing (e.g., date formats, public holidays).
- **Policy / rules layer**:
    - Strict business constraints (e.g., max leave days, approval chains, budget limits) SHOULD be enforced in a deterministic rules layer, not solely via LLM reasoning.
    - The pipeline becomes:
        1. LLM produces a candidate structured fill according to the Form Schema.
        2. Policy/rules layer validates and normalizes the candidate using context and organizational policies.
        3. Any violations are converted into `errors`, reduced `confidence`, or clarification prompts.

## Prompting & Retry Patterns for Complex Forms
- **Schema summarization**:
    - Prompts SHOULD summarize only the relevant parts of the Form Schema for the current request, especially when schemas are large.
    - Dependencies and cross-field constraints SHOULD be included in the system prompt so the LLM understands which combinations are valid.
- **Ask-don’t-guess policy**:
    - Prompts MUST instruct the LLM to:
        - avoid inventing values when essential information is missing,
        - explicitly mark low-confidence fields and suggest clarification questions.
- **Validation-aware retries**:
    - When validation fails (schema or policy layer), the retry prompt SHOULD:
        - include the previous invalid JSON,
        - list validation errors in a machine- and model-friendly way,
        - ask the LLM to only adjust the problematic fields while keeping valid parts unchanged.

## Worked Example: Multi-day Leave Request with Dependencies
- **Schema sketch**:
    - Fields:
        - `leave_type` (enum: `annual`, `sick`, `unpaid`)
        - `start_date`, `end_date`
        - `reason` (string)
        - `medical_certificate` (string, required when `leave_type = sick` and duration > N days)
        - `approver` (user reference, default from context)
    - Constraints:
        - `start_date <= end_date`
        - duration <= policy max for given `leave_type` and user role.
- **User input**:
    - \"I’m feeling really sick, I need a week off starting next Monday. Please send it to my manager.\"
- **Context**:
    - `now = 2023-10-27T10:00:00Z`, `user.role = employee`, `user.managerId = u_123`, locale = `en-US`.
- **Expected ParsingResult highlights**:
    - `leave_type`: `\"sick\"`, high confidence, reasoning: explicit \"sick\".
    - `start_date`: `2023-10-30`, high confidence, reasoning: \"next Monday\" from `now`.
    - `end_date`: computed as `2023-11-05`, confidence high if policy allows 7 days.
    - `approver`: `u_123` derived from context, with reasoning referencing manager.
    - `medical_certificate`: either:
        - missing with an `error` indicating it is required for sick leave > N days, or
        - low-confidence suggestion asking the user whether a certificate is available.

## Worked Example: New Task Request with Dependencies
- **Schema sketch**:
    - Core fields:
        - `id`, `realm_id`
        - `name`, `description`
        - `priority`, `difficulty`, `status`
        - `schedule_time` (when the user plans to work on the task)
        - `minutes` (estimated effort)
        - `deadline`
        - optional `start_time`, `end_time`, `tags`
    - Repeat fields:
        - `is_repeating`, `repeat_pattern`, `repeat_interval`,
        - `repeat_days_of_week`, `repeat_day_of_month`,
        - `repeat_end_date`, `repeat_count`
    - Reminder fields:
        - `generate_reminders`, `reminder_advance_minutes`,
        - `reminder_methods`, `reminder_targets`
    - Example dependencies:
        - `schedule_time` SHOULD be on or before `deadline`.
        - When `generate_reminders = true`, `reminder_advance_minutes` MUST be > 0.
- **User input**:
    - \"tomorrow is monday, I have to finish the sending alert feature ASAP, the release date is next friday\"
- **Context**:
    - `now = 2023-10-27T10:00:00Z` (Friday), locale = `en-US`.
- **Expected ParsingResult highlights**:
    - `name`: \"sending alert feature\", high confidence, extracted as the task name.
    - `priority`: high (e.g., mapped to an integer level) with reasoning referencing \"ASAP\".
    - `schedule_time`: computed as the coming Monday morning (e.g., `2023-10-30T09:00:00` in the user’s time zone), with reasoning from \"tomorrow is monday\" and context `now`.
    - `deadline`: computed as the next Friday after `schedule_time` (e.g., `2023-11-03T23:59:59`), with reasoning from \"release date is next friday\".
    - `minutes`: either:
        - inferred from historical context (if available, e.g., similar tasks usually take 120 minutes), or
        - left unset / low-confidence with a recommendation to ask the user for an estimate.
    - Repeat / reminder fields:
        - defaulted based on schema defaults and/or user/realm preferences in `context` (e.g., `generate_reminders = true`, `reminder_advance_minutes = 60`), with reasoning referencing defaults rather than hallucinated intent.

## Risks / Trade-offs
- **Risk**: LLM hallucination.
    - **Mitigation**: Strict schema validation and "confidence" scores.
- **Risk**: Complex schemas might confuse the LLM.
    - **Mitigation**: Hierarchical parsing or multi-step prompting (future).
- **Risk**: Spring AI is still evolving (v1.0.3).
    - **Mitigation**: Wrap Spring AI dependencies in an abstraction layer (as planned in `llm-integration` spec) to minimize impact of breaking changes.

## Open Questions
- None at this stage.

