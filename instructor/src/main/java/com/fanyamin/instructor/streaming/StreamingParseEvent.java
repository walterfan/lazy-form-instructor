package com.fanyamin.instructor.streaming;

import com.fanyamin.instructor.api.ParsingResult;
import com.fanyamin.instructor.api.ValidationError;

import java.util.List;

/**
 * Events emitted by LazyFormInstructor streaming parsing.
 *
 * <p>Semantics:
 * <ul>
 *   <li>{@link Snapshot} is best-effort JSON parsing only (not schema-validated).</li>
 *   <li>Schema validation is performed only at the end of an attempt and surfaced via {@link AttemptFailed}
 *       and {@link FinalResult}.</li>
 * </ul>
 */
public interface StreamingParseEvent {

    /**
     * Attempt number starting from 1.
     */
    int attempt();

    record AttemptStarted(int attempt) implements StreamingParseEvent {}

    record RawChunk(String chunk, int attempt) implements StreamingParseEvent {}

    /**
     * Best-effort parsed snapshot (may fail to parse for partial JSON and thus not emitted).
     * This snapshot is not schema-validated.
     */
    record Snapshot(ParsingResult partial, int attempt) implements StreamingParseEvent {}

    /**
     * An attempt finished but did not pass schema validation.
     */
    record AttemptFailed(List<ValidationError> schemaErrors, int attempt) implements StreamingParseEvent {}

    /**
     * Final result (schemaErrors empty means schema validation passed).
     */
    record FinalResult(ParsingResult result, List<ValidationError> schemaErrors, int attempt) implements StreamingParseEvent {}

    record Error(Throwable error, int attempt) implements StreamingParseEvent {}
}



