# SmartFormInstructor - Architecture Diagram

## High-Level System Overview

```mermaid
graph TB
    subgraph "User Layer"
        USER[👤 Application Developer]
        INPUT[📝 User Input<br/>Natural Language]
        SCHEMA[📋 JSON Schema<br/>Form Definition]
        CTX[🗂️ Context<br/>User/Environment Data]
    end

    subgraph "SmartFormInstructor Core"
        API[🎯 SmartFormInstructor<br/>Main Entry Point]
        PROMPT[📄 PromptManager<br/>Generate Prompts]
        VALIDATOR[✅ SchemaValidator<br/>JSON Schema Validation]
        MAPPER[🔄 ObjectMapper<br/>JSON Parsing]
    end

    subgraph "LLM Abstraction Layer"
        CLIENT[🔌 LlmClient Interface]
        SPRING[☁️ SpringAiLlmClient]
        OPENAI[🤖 OpenAiLlmClient]
        MOCK[🧪 MockLlmClient]
    end

    subgraph "External Services"
        LLM[🧠 LLM Provider<br/>OpenAI/Azure/etc]
    end

    subgraph "Data Models"
        REQ[ParsingRequest<br/>• schema<br/>• userInput<br/>• context]
        RES[ParsingResult<br/>• fields: Map<br/>• errors: List]
        FIELD[FieldResult<br/>• value<br/>• confidence<br/>• reasoning<br/>• alternatives]
    end

    USER -->|Provides| INPUT
    USER -->|Defines| SCHEMA
    USER -->|Provides| CTX
    
    INPUT --> REQ
    SCHEMA --> REQ
    CTX --> REQ
    
    REQ --> API
    API --> PROMPT
    API --> VALIDATOR
    API --> MAPPER
    API --> CLIENT
    
    PROMPT -->|System Prompt| CLIENT
    CLIENT --> SPRING
    CLIENT --> OPENAI
    CLIENT --> MOCK
    
    SPRING --> LLM
    OPENAI --> LLM
    
    LLM -->|JSON Response| CLIENT
    CLIENT -->|Raw JSON| MAPPER
    MAPPER -->|Parse| RES
    VALIDATOR -->|Validate| RES
    
    RES --> FIELD
    RES --> USER

    style API fill:#2196F3,stroke:#1976D2,stroke-width:3px,color:#fff
    style PROMPT fill:#9C27B0,stroke:#7B1FA2,stroke-width:2px,color:#fff
    style VALIDATOR fill:#4CAF50,stroke:#388E3C,stroke-width:2px,color:#fff
    style LLM fill:#F44336,stroke:#D32F2F,stroke-width:2px,color:#fff
```

## Core Processing Flow

```mermaid
sequenceDiagram
    participant App as 👤 Application
    participant API as SmartFormInstructor
    participant PM as PromptManager
    participant LC as LlmClient
    participant LLM as 🧠 LLM Provider
    participant SV as SchemaValidator
    participant OM as ObjectMapper

    App->>API: parse(ParsingRequest)
    
    rect rgb(230, 240, 255)
        Note over API,LLM: Attempt 1 (Initial)
        API->>PM: generateSystemPrompt(request)
        PM->>PM: Build prompt with<br/>schema + input + context
        PM-->>API: System prompt
        
        API->>LC: chat(prompt)
        LC->>LLM: API call
        LLM-->>LC: JSON response
        LC-->>API: JSON string
        
        API->>OM: readValue(json)
        OM-->>API: ParsingResult
        
        API->>API: extractValuesJson(result)
        API->>SV: validate(schema, valuesJson)
        SV-->>API: validation errors
    end
    
    alt Validation Success
        API-->>App: ✅ ParsingResult
    else Validation Failed (Retry)
        rect rgb(255, 240, 240)
            Note over API,LLM: Attempt 2-4 (Retry with errors)
            API->>API: generateRetryPrompt(prompt, errors)
            API->>LC: chat(retryPrompt)
            LC->>LLM: API call with error context
            LLM-->>LC: Corrected JSON
            LC-->>API: JSON string
            API->>OM: readValue(json)
            API->>SV: validate(schema, valuesJson)
        end
        
        alt Retry Success
            API-->>App: ✅ ParsingResult
        else Max Retries Exceeded
            API-->>App: ❌ ParsingResult with errors
        end
    end
```

## Data Flow Architecture

```mermaid
flowchart TD
    subgraph Input["📥 Input Layer"]
        I1[User Input<br/>'John is 30 years old']
        I2[JSON Schema<br/>name: string, age: int]
        I3[Context<br/>timestamp, location, user]
    end
    
    subgraph Processing["⚙️ Processing Layer"]
        P1[PromptManager<br/>Build System Prompt]
        P2[LlmClient<br/>Abstract LLM Call]
        P3[ObjectMapper<br/>Parse JSON Response]
        P4[SchemaValidator<br/>Validate Against Schema]
    end
    
    subgraph Retry["🔄 Retry Loop"]
        R1{Validation<br/>Pass?}
        R2[Generate Retry Prompt<br/>with Error Context]
        R3{Max Retries<br/>Reached?}
    end
    
    subgraph Output["📤 Output Layer"]
        O1[ParsingResult]
        O2[FieldResult Map<br/>value, confidence, reasoning]
        O3[ValidationError List]
    end

    I1 --> P1
    I2 --> P1
    I3 --> P1
    
    P1 -->|System Prompt| P2
    P2 -->|LLM Response| P3
    P3 -->|ParsingResult| P4
    
    P4 --> R1
    R1 -->|❌ Fail| R3
    R3 -->|No| R2
    R2 -->|Retry with errors| P2
    R3 -->|Yes| O3
    
    R1 -->|✅ Pass| O1
    O1 --> O2
    O1 --> O3

    style P1 fill:#9C27B0,color:#fff
    style P2 fill:#2196F3,color:#fff
    style P4 fill:#4CAF50,color:#fff
    style R1 fill:#FF9800,color:#fff
    style O1 fill:#00BCD4,color:#fff
```

## Component Architecture

```mermaid
classDiagram
    class SmartFormInstructor {
        -LlmClient llmClient
        -PromptManager promptManager
        -SchemaValidator schemaValidator
        -ObjectMapper objectMapper
        -int maxRetries
        +parse(ParsingRequest) ParsingResult
        -generateRetryPrompt() String
        -extractValuesJson() String
    }

    class ParsingRequest {
        +String schema
        +String userInput
        +Map context
    }

    class ParsingResult {
        +Map~String,FieldResult~ fields
        +List~ValidationError~ errors
    }

    class FieldResult {
        +Object value
        +double confidence
        +String reasoning
        +List~Object~ alternatives
    }

    class ValidationError {
        +String path
        +String message
        +String type
    }

    class PromptManager {
        -String SYSTEM_PROMPT_TEMPLATE
        +generateSystemPrompt(ParsingRequest) String
    }

    class SchemaValidator {
        -ObjectMapper objectMapper
        -JsonSchemaFactory schemaFactory
        +validate(String schema, String instance) List~ValidationError~
    }

    class LlmClient {
        <<interface>>
        +chat(String prompt) String
    }

    class SpringAiLlmClient {
        -ChatClient chatClient
        +chat(String prompt) String
    }

    class OpenAiLlmClient {
        -String apiKey
        -String model
        +chat(String prompt) String
    }

    class MockLlmClient {
        +chat(String prompt) String
    }

    SmartFormInstructor --> LlmClient : uses
    SmartFormInstructor --> PromptManager : uses
    SmartFormInstructor --> SchemaValidator : uses
    SmartFormInstructor --> ParsingRequest : consumes
    SmartFormInstructor --> ParsingResult : produces
    
    ParsingResult --> FieldResult : contains
    ParsingResult --> ValidationError : contains
    
    LlmClient <|.. SpringAiLlmClient : implements
    LlmClient <|.. OpenAiLlmClient : implements
    LlmClient <|.. MockLlmClient : implements
    
    PromptManager --> ParsingRequest : reads
```

## Retry Mechanism Flow

```mermaid
stateDiagram-v2
    [*] --> BuildPrompt: Start parse()
    
    BuildPrompt --> CallLLM: Initial prompt
    CallLLM --> ParseJSON: LLM response
    ParseJSON --> ValidateSchema: Parse to ParsingResult
    
    ValidateSchema --> CheckValidation: Run JSON Schema validation
    
    CheckValidation --> Success: ✅ Valid
    CheckValidation --> CheckRetryCount: ❌ Invalid
    
    CheckRetryCount --> BuildRetryPrompt: Retry count < maxRetries
    CheckRetryCount --> Failure: Retry count >= maxRetries
    
    BuildRetryPrompt --> CallLLM: Append error context
    
    Success --> [*]: Return ParsingResult
    Failure --> [*]: Return ParsingResult with errors
    
    note right of BuildRetryPrompt
        Retry prompt includes:
        - Original prompt
        - Previous response
        - Validation errors
        - Fix instructions
    end note
```

## Package Structure

```mermaid
graph TD
    ROOT[com.fanyamin]
    
    ROOT --> INSTRUCTOR[instructor/]
    ROOT --> MAIN[SmartFormInstructor.java]
    
    INSTRUCTOR --> API[api/]
    INSTRUCTOR --> EXCEPTION[exception/]
    INSTRUCTOR --> LLM[llm/]
    INSTRUCTOR --> SCHEMA[schema/]
    
    API --> API1[ParsingRequest.java]
    API --> API2[ParsingResult.java]
    API --> API3[FieldResult.java]
    API --> API4[ValidationError.java]
    
    EXCEPTION --> EX1[InstructorException.java]
    EXCEPTION --> EX2[ValidationFailedException.java]
    EXCEPTION --> EX3[RetryExhaustedException.java]
    EXCEPTION --> EX4[IncompleteOutputException.java]
    
    LLM --> LLM1[LlmClient.java]
    LLM --> LLM2[SpringAiLlmClient.java]
    LLM --> LLM3[OpenAiLlmClient.java]
    LLM --> LLM4[MockLlmClient.java]
    LLM --> LLM5[PromptManager.java]
    LLM --> LLM6[LlmClientFactory.java]
    
    SCHEMA --> SCH1[SchemaValidator.java]
    SCHEMA --> SCH2[SchemaGenerator.java]
    SCHEMA --> SCH3[Annotations...]

    style ROOT fill:#2196F3,color:#fff
    style INSTRUCTOR fill:#9C27B0,color:#fff
    style MAIN fill:#4CAF50,color:#fff
```

## Universal Smart Form Protocol (USFP)

```mermaid
mindmap
  root((USFP<br/>Protocol))
    Input
      JSON Schema
        Field types
        Constraints
        Dependencies
      User Input
        Natural language
        Partial data
        Ambiguous intent
      Context
        Timestamp
        User profile
        Environment
    Processing
      Prompt Generation
        Schema description
        Extraction rules
        Output format
      LLM Interaction
        API abstraction
        Provider agnostic
        Error handling
      Validation
        JSON Schema validation
        Type checking
        Constraint verification
    Output
      FieldResult
        Value
        Confidence 0.0-1.0
        Reasoning explanation
        Alternatives list
      ValidationErrors
        Path to field
        Error message
        Error type
```

## Key Features Visualization

```mermaid
flowchart LR
    subgraph Features["🌟 Key Features"]
        F1[📋 Schema-First<br/>JSON Schema driven]
        F2[🔄 Auto Retry<br/>Validation loop]
        F3[📊 Rich Output<br/>Confidence + Reasoning]
        F4[🔌 Provider Agnostic<br/>Multiple LLM support]
        F5[🧩 Complex Forms<br/>Nested + Arrays]
        F6[🎯 Context-Aware<br/>Environment data]
    end
    
    subgraph Benefits["✨ Benefits"]
        B1[🚀 Fast Development<br/>Schema = Contract]
        B2[🛡️ Type Safety<br/>Validated output]
        B3[📈 Transparency<br/>Explainable AI]
        B4[🔧 Flexibility<br/>Any LLM provider]
    end
    
    F1 --> B1
    F2 --> B2
    F3 --> B3
    F4 --> B4
    F5 --> B1
    F6 --> B3

    style F1 fill:#E1F5FE
    style F2 fill:#F3E5F5
    style F3 fill:#FFF3E0
    style F4 fill:#E8F5E9
    style B1 fill:#C8E6C9
    style B2 fill:#BBDEFB
    style B3 fill:#FFE0B2
    style B4 fill:#F8BBD0
```

## Example: Leave Request Processing

```mermaid
graph LR
    subgraph Input["📥 Input"]
        I1["User: 'I need sick leave tomorrow'"]
        I2["Schema: leave_type, start_date,<br/>end_date, reason"]
        I3["Context: today='2024-12-12'"]
    end
    
    subgraph Processing["⚙️ SmartFormInstructor"]
        P1[Build Prompt]
        P2[Call LLM]
        P3[Parse Response]
        P4[Validate]
    end
    
    subgraph Output["📤 Output"]
        O1["leave_type: 'sick'<br/>confidence: 1.0"]
        O2["start_date: '2024-12-13'<br/>confidence: 0.95<br/>reasoning: 'tomorrow = today+1'"]
        O3["end_date: '2024-12-13'<br/>confidence: 0.95"]
        O4["reason: null<br/>confidence: 0.0<br/>alternatives: ['illness', 'unwell']"]
    end

    I1 --> P1
    I2 --> P1
    I3 --> P1
    P1 --> P2
    P2 --> P3
    P3 --> P4
    P4 --> O1
    P4 --> O2
    P4 --> O3
    P4 --> O4

    style P1 fill:#9C27B0,color:#fff
    style P2 fill:#2196F3,color:#fff
    style P4 fill:#4CAF50,color:#fff
```

## LLM Client Abstraction

```mermaid
graph TB
    subgraph Application["Application Code"]
        APP[SmartFormInstructor]
    end
    
    subgraph Abstraction["LlmClient Interface"]
        INTERFACE[📡 LlmClient<br/>+ chat(String prompt)]
    end
    
    subgraph Implementations["Concrete Implementations"]
        SPRING[☁️ SpringAiLlmClient<br/>Uses Spring AI]
        OPENAI[🤖 OpenAiLlmClient<br/>Direct OpenAI API]
        MOCK[🧪 MockLlmClient<br/>For testing]
    end
    
    subgraph Providers["LLM Providers"]
        GPT[OpenAI GPT-4]
        AZURE[Azure OpenAI]
        OTHER[Claude, Gemini, etc.]
    end

    APP -->|depends on| INTERFACE
    INTERFACE -->|implemented by| SPRING
    INTERFACE -->|implemented by| OPENAI
    INTERFACE -->|implemented by| MOCK
    
    SPRING -->|calls| GPT
    SPRING -->|calls| AZURE
    SPRING -->|calls| OTHER
    OPENAI -->|calls| GPT

    style INTERFACE fill:#2196F3,color:#fff
    style APP fill:#4CAF50,color:#fff
```

## Prompt Engineering Flow

```mermaid
flowchart TD
    START([Start]) --> TEMPLATE[Load Prompt Template]
    
    TEMPLATE --> INJECT_CTX[Inject Context<br/>timestamp, user data]
    INJECT_CTX --> INJECT_SCHEMA[Inject JSON Schema<br/>field definitions]
    INJECT_SCHEMA --> INJECT_INPUT[Inject User Input<br/>natural language]
    
    INJECT_INPUT --> FORMAT[Format Instructions<br/>• Extraction rules<br/>• Output structure<br/>• Validation requirements]
    
    FORMAT --> SEND[Send to LLM]
    
    SEND --> RESPONSE{Valid<br/>Response?}
    
    RESPONSE -->|✅ Yes| RETURN[Return Prompt]
    RESPONSE -->|❌ No| RETRY_FORMAT[Append Error Context]
    
    RETRY_FORMAT --> ERRORS[Include:<br/>• Previous response<br/>• Validation errors<br/>• Fix instructions]
    
    ERRORS --> SEND
    
    RETURN --> END([End])

    style TEMPLATE fill:#E1F5FE
    style FORMAT fill:#F3E5F5
    style SEND fill:#2196F3,color:#fff
    style RESPONSE fill:#FF9800,color:#fff
```

## Design Principles

### 🎯 Core Principles

1. **Schema-First Design**
   - JSON Schema defines the contract
   - Business logic in schema, not code
   - Portable and language-agnostic

2. **Separation of Concerns**
   - LLM client abstracted via interface
   - Prompt management separate from logic
   - Validation decoupled from parsing

3. **Fail-Safe Retry**
   - Automatic retry with error feedback
   - Configurable retry limits
   - Graceful degradation

4. **Rich Metadata**
   - Not just values, but confidence + reasoning
   - Alternatives for ambiguous cases
   - Explainable AI decisions

5. **Provider Agnostic**
   - Works with any LLM provider
   - Easy to switch or test with mocks
   - No vendor lock-in

## Summary: 3 Key Components

```mermaid
graph LR
    A[1️⃣ SmartFormInstructor<br/>Orchestrator] --> B[2️⃣ PromptManager<br/>Prompt Engineering]
    B --> C[3️⃣ SchemaValidator<br/>Quality Assurance]
    
    A -.->|uses| D[LlmClient<br/>LLM Abstraction]
    
    E[📥 Input:<br/>Schema + Text + Context] --> A
    C --> F[📤 Output:<br/>Fields + Confidence]

    style A fill:#2196F3,color:#fff
    style B fill:#9C27B0,color:#fff
    style C fill:#4CAF50,color:#fff
    style D fill:#FF9800,color:#fff
```

**Result**: Transform unstructured natural language into validated, structured data with confidence scores! 🚀


