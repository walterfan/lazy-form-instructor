package com.fanyamin.instructor.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify min/max range for numeric fields in JSON Schema.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SchemaRange {
    double min() default Double.MIN_VALUE;
    double max() default Double.MAX_VALUE;
}

