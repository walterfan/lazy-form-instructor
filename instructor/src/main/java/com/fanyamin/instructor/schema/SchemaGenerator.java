package com.fanyamin.instructor.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Utility to generate JSON Schema from Java classes.
 * Supports custom annotations for descriptions, enums, and constraints.
 */
public class SchemaGenerator {

    private final ObjectMapper objectMapper;

    public SchemaGenerator() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate JSON Schema with custom annotations support.
     */
    public String generateSchemaWithAnnotations(Class<?> clazz) {
        try {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
            schema.put("type", "object");
            
            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();
            
            for (Field field : getAllFields(clazz)) {
                field.setAccessible(true);
                
                String fieldName = getFieldName(field);
                Map<String, Object> fieldSchema = generateFieldSchema(field);
                properties.put(fieldName, fieldSchema);
                
                if (isRequired(field)) {
                    required.add(fieldName);
                }
            }
            
            schema.put("properties", properties);
            if (!required.isEmpty()) {
                schema.put("required", required);
            }
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate schema for " + clazz.getName(), e);
        }
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private String getFieldName(Field field) {
        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty != null && !jsonProperty.value().isEmpty()) {
            return jsonProperty.value();
        }
        // Convert camelCase to snake_case
        String name = field.getName();
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private Map<String, Object> generateFieldSchema(Field field) {
        Map<String, Object> fieldSchema = new LinkedHashMap<>();
        
        // Get type
        Class<?> fieldType = field.getType();
        String jsonType = getJsonType(fieldType);
        fieldSchema.put("type", jsonType);
        
        // Get description from @SchemaDescription
        SchemaDescription description = field.getAnnotation(SchemaDescription.class);
        if (description != null) {
            fieldSchema.put("description", description.value());
        }
        
        // Get enum values from @SchemaEnum
        SchemaEnum schemaEnum = field.getAnnotation(SchemaEnum.class);
        if (schemaEnum != null) {
            fieldSchema.put("enum", Arrays.asList(schemaEnum.value()));
        }
        
        // Get format from @SchemaFormat
        SchemaFormat format = field.getAnnotation(SchemaFormat.class);
        if (format != null) {
            fieldSchema.put("format", format.value());
        }
        
        // Get min/max from @SchemaRange
        SchemaRange range = field.getAnnotation(SchemaRange.class);
        if (range != null) {
            if (range.min() != Double.MIN_VALUE) {
                fieldSchema.put("minimum", range.min());
            }
            if (range.max() != Double.MAX_VALUE) {
                fieldSchema.put("maximum", range.max());
            }
        }
        
        // Get pattern from @SchemaPattern
        SchemaPattern pattern = field.getAnnotation(SchemaPattern.class);
        if (pattern != null) {
            fieldSchema.put("pattern", pattern.value());
        }
        
        return fieldSchema;
    }

    private boolean isRequired(Field field) {
        SchemaRequired required = field.getAnnotation(SchemaRequired.class);
        return required != null && required.value();
    }

    private String getJsonType(Class<?> javaType) {
        if (javaType == String.class) {
            return "string";
        } else if (javaType == Integer.class || javaType == int.class || 
                   javaType == Long.class || javaType == long.class) {
            return "integer";
        } else if (javaType == Double.class || javaType == double.class || 
                   javaType == Float.class || javaType == float.class) {
            return "number";
        } else if (javaType == Boolean.class || javaType == boolean.class) {
            return "boolean";
        } else if (javaType.isArray() || Collection.class.isAssignableFrom(javaType)) {
            return "array";
        } else {
            return "object";
        }
    }
}

