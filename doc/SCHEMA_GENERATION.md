# Schema Generation from Java DTOs

This guide shows how to generate JSON Schema from Java Data Transfer Objects (DTOs) instead of manually writing JSON files.

## Benefits

✅ **Type Safety**: Java compiler ensures your schema is valid
✅ **Refactoring**: IDE refactoring tools work seamlessly  
✅ **Documentation**: Annotations serve as inline documentation
✅ **Maintainability**: Single source of truth for form structure
✅ **Auto-completion**: IDE provides code completion for schema definition

## Quick Start

###  Step 1: Create a DTO with Annotations

```java
package com.example.dto;

import com.fanyamin.instructor.schema.*;

public class LeaveRequestForm {

    @SchemaRequired
    @SchemaEnum({"annual", "sick", "unpaid"})
    @SchemaDescription("Type of leave request")
    private String leaveType;

    @SchemaRequired
    @SchemaFormat("date")
    @SchemaDescription("Start date of leave")
    private String startDate;

    @SchemaRequired
    @SchemaFormat("date")
    @SchemaDescription("End date of leave")
    private String endDate;

    @SchemaRequired
    @SchemaDescription("Reason for leave")
    private String reason;

    @SchemaDescription("Medical certificate if required")
    private String medicalCertificate;

    @SchemaDescription("Person who will approve the leave")
    private String approver;

    // Getters and setters...
}
```

### Step 2: Generate Schema

```java
import com.fanyamin.instructor.schema.SchemaGenerator;

SchemaGenerator generator = new SchemaGenerator();
String schema = generator.generateSchemaWithAnnotations(LeaveRequestForm.class);
```

### Step 3: Use in SmartFormInstructor

```java
import com.fanyamin.SmartFormInstructor;
import com.fanyamin.instructor.api.ParsingRequest;
import com.fanyamin.instructor.api.ParsingResult;

// Generate schema
String schema = generator.generateSchemaWithAnnotations(LeaveRequestForm.class);

// Create parsing request
ParsingRequest request = new ParsingRequest(
    schema,
    "I need sick leave from next Monday for 3 days",
    Map.of("now", "2024-12-08T10:00:00Z")
);

// Parse
SmartFormInstructor instructor = new SmartFormInstructor(llmClient);
ParsingResult result = instructor.parse(request);
```

## Available Annotations

### @SchemaRequired

Marks a field as required in the JSON Schema.

```java
@SchemaRequired
private String name;
```

Generated schema:
```json
{
  "properties": {
    "name": { "type": "string" }
  },
  "required": ["name"]
}
```

### @SchemaDescription

Adds a description to the field.

```java
@SchemaDescription("User's full name")
private String name;
```

Generated schema:
```json
{
  "properties": {
    "name": {
      "type": "string",
      "description": "User's full name"
    }
  }
}
```

### @SchemaEnum

Specifies allowed values for a field.

```java
@SchemaEnum({"active", "inactive", "pending"})
private String status;
```

Generated schema:
```json
{
  "properties": {
    "status": {
      "type": "string",
      "enum": ["active", "inactive", "pending"]
    }
  }
}
```

### @SchemaFormat

Specifies the format of a string field (e.g., date, date-time, email, uri).

```java
@SchemaFormat("date")
private String birthDate;

@SchemaFormat("email")
private String email;

@SchemaFormat("date-time")
private String createdAt;
```

Generated schema:
```json
{
  "properties": {
    "birth_date": {
      "type": "string",
      "format": "date"
    },
    "email": {
      "type": "string",
      "format": "email"
    },
    "created_at": {
      "type": "string",
      "format": "date-time"
    }
  }
}
```

### @SchemaRange

Specifies min/max values for numeric fields.

```java
@SchemaRange(min = 0, max = 100)
private int age;

@SchemaRange(min = 1.0)
private double price;
```

Generated schema:
```json
{
  "properties": {
    "age": {
      "type": "integer",
      "minimum": 0,
      "maximum": 100
    },
    "price": {
      "type": "number",
      "minimum": 1.0
    }
  }
}
```

### @SchemaPattern

Specifies a regex pattern for string validation.

```java
@SchemaPattern("^[A-Z]{2}\\d{6}$")
private String employeeId;
```

Generated schema:
```json
{
  "properties": {
    "employee_id": {
      "type": "string",
      "pattern": "^[A-Z]{2}\\d{6}$"
    }
  }
}
```

## Complete Example

```java
package com.example.dto;

import com.fanyamin.instructor.schema.*;

public class TaskRequestForm {

    @SchemaRequired
    @SchemaDescription("Task name or title")
    private String name;

    @SchemaDescription("Detailed description of the task")
    private String description;

    @SchemaRequired
    @SchemaEnum({"1", "2", "3", "4", "5"})
    @SchemaDescription("Priority level (1=lowest, 5=highest)")
    private String priority;

    @SchemaRequired
    @SchemaFormat("date-time")
    @SchemaDescription("When to start the task")
    private String scheduleTime;

    @SchemaFormat("date-time")
    @SchemaDescription("Task deadline")
    private String deadline;

    @SchemaRange(min = 1)
    @SchemaDescription("Estimated time in minutes")
    private Integer minutes;

    @SchemaEnum({"pending", "in_progress", "completed"})
    @SchemaDescription("Current status of the task")
    private String status;

    // Getters and setters...
}
```

## Field Naming Convention

By default, Java camelCase field names are converted to snake_case in JSON Schema:

```java
private String leaveType;     // → "leave_type" in schema
private String startDate;     // → "start_date" in schema
private String createdAt;     // → "created_at" in schema
```

To override, use `@JsonProperty`:

```java
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonProperty("leaveType")  // Keep camelCase
private String leaveType;
```

## Type Mapping

Java types are automatically mapped to JSON Schema types:

| Java Type | JSON Schema Type |
|-----------|------------------|
| `String` | `"string"` |
| `int`, `Integer`, `long`, `Long` | `"integer"` |
| `double`, `Double`, `float`, `Float` | `"number"` |
| `boolean`, `Boolean` | `"boolean"` |
| Arrays, `Collection` | `"array"` |
| Other objects | `"object"` |

## Advanced Example: Task Request Form

```java
package com.fanyamin.example.dto;

import com.fanyamin.instructor.schema.*;

public class TaskRequestForm {

    @SchemaRequired
    @SchemaDescription("Task name")
    private String name;

    @SchemaDescription("Task description")
    private String description;

    @SchemaEnum({"1", "2", "3", "4", "5"})
    @SchemaDescription("Priority (1=low, 5=high)")
    private String priority;

    @SchemaEnum({"1", "2", "3"})
    @SchemaDescription("Difficulty (1=easy, 3=hard)")
    private String difficulty;

    @SchemaEnum({"pending", "in_progress", "completed", "cancelled"})
    @SchemaDescription("Task status")
    private String status;

    @SchemaFormat("date-time")
    @SchemaDescription("Scheduled start time")
    private String scheduleTime;

    @SchemaRange(min = 1)
    @SchemaDescription("Estimated minutes")
    private Integer minutes;

    @SchemaFormat("date-time")
    @SchemaDescription("Deadline")
    private String deadline;

    @SchemaFormat("date-time")
    @SchemaDescription("Actual start time")
    private String startTime;

    @SchemaFormat("date-time")
    @SchemaDescription("Actual end time")
    private String endTime;

    @SchemaDescription("Tags (comma-separated)")
    private String tags;

    // Repeat-related fields
    @SchemaDescription("Is this a repeating task?")
    private Boolean isRepeating;

    @SchemaEnum({"daily", "weekly", "monthly"})
    @SchemaDescription("Repeat pattern")
    private String repeatPattern;

    @SchemaRange(min = 1)
    @SchemaDescription("Repeat interval")
    private Integer repeatInterval;

    // Reminder fields
    @SchemaDescription("Generate reminders?")
    private Boolean generateReminders;

    @SchemaRange(min = 1)
    @SchemaDescription("Reminder advance time (minutes)")
    private Integer reminderAdvanceMinutes;

    // Getters and setters...
}
```

Usage:

```java
SchemaGenerator generator = new SchemaGenerator();
String schema = generator.generateSchemaWithAnnotations(TaskRequestForm.class);

System.out.println(schema);
```

Output:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "name": {
      "type": "string",
      "description": "Task name"
    },
    "priority": {
      "type": "string",
      "description": "Priority (1=low, 5=high)",
      "enum": ["1", "2", "3", "4", "5"]
    },
    "schedule_time": {
      "type": "string",
      "description": "Scheduled start time",
      "format": "date-time"
    },
    "minutes": {
      "type": "integer",
      "description": "Estimated minutes",
      "minimum": 1
    }
    // ... more fields
  },
  "required": ["name"]
}
```

## Comparison: DTO vs JSON File

### DTO Approach (Recommended)

**Pros:**
- ✅ Type-safe
- ✅ IDE support (auto-completion, refactoring)
- ✅ Compile-time validation
- ✅ Single source of truth
- ✅ Easy to maintain

**Cons:**
- ❌ Requires rebuilding when schema changes
- ❌ More verbose for simple schemas

**Example:**
```java
@SchemaRequired
@SchemaEnum({"annual", "sick"})
@SchemaDescription("Leave type")
private String leaveType;
```

### JSON File Approach

**Pros:**
- ✅ Can be modified without recompilation
- ✅ Simple for basic schemas
- ✅ Familiar JSON format

**Cons:**
- ❌ No compile-time validation
- ❌ Prone to typos
- ❌ No IDE assistance
- ❌ Hard to refactor

**Example:**
```json
{
  "leave_type": {
    "type": "string",
    "description": "Leave type",
    "enum": ["annual", "sick"]
  }
}
```

## Best Practices

1. **Use DTOs for complex forms** with many fields and validation rules
2. **Use JSON files for simple schemas** that rarely change
3. **Document fields** with `@SchemaDescription`
4. **Validate enums** with `@SchemaEnum` instead of free-text
5. **Specify formats** for dates, emails, URIs with `@SchemaFormat`
6. **Set constraints** with `@SchemaRange` and `@SchemaPattern`
7. **Mark required fields** explicitly with `@SchemaRequired`

## Testing Your Schema

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LeaveRequestFormTest {

    @Test
    void testSchemaGeneration() {
        SchemaGenerator generator = new SchemaGenerator();
        String schema = generator.generateSchemaWithAnnotations(LeaveRequestForm.class);
        
        // Verify schema structure
        assertTrue(schema.contains("\"$schema\""));
        assertTrue(schema.contains("\"type\" : \"object\""));
        assertTrue(schema.contains("\"properties\""));
        assertTrue(schema.contains("\"required\""));
        
        // Verify specific fields
        assertTrue(schema.contains("\"leave_type\""));
        assertTrue(schema.contains("\"enum\" : [ \"annual\", \"sick\", \"unpaid\" ]"));
    }
}
```

## See Also

- [QUICKSTART.md](QUICKSTART.md) - Quick start guide
- [README.md](README.md) - Main documentation
- Example: `smart-form-example/src/main/java/com/fanyamin/example/LeaveRequestExample.java`

