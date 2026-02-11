package com.fanyamin.web.controller;

import com.fanyamin.LazyFormInstructor;
import com.fanyamin.instructor.api.ParsingRequest;
import com.fanyamin.instructor.api.ParsingResult;
import com.fanyamin.instructor.schema.SchemaGenerator;
import com.fanyamin.instructor.streaming.StreamingParseEvent;
import com.fanyamin.web.dto.LeaveRequestForm;
import com.fanyamin.web.dto.ParseRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/forms")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FormController {

    private final LazyFormInstructor instructor;
    private final SchemaGenerator schemaGenerator;

    public FormController(LazyFormInstructor instructor) {
        this.instructor = instructor;
        this.schemaGenerator = new SchemaGenerator();
    }

    @PostMapping("/parse")
    public ParsingResult parseForm(@RequestBody ParseRequest request) throws IOException {
        ParsingRequest parsingRequest = buildParsingRequest(request);
        return instructor.parse(parsingRequest);
    }

    /**
     * Streaming parse endpoint (Server-Sent Events).
     *
     * <p>Emits named SSE events:\n
     * <ul>\n
     *   <li>attemptStarted</li>\n
     *   <li>rawChunk</li>\n
     *   <li>snapshot</li>\n
     *   <li>attemptFailed</li>\n
     *   <li>finalResult</li>\n
     *   <li>error</li>\n
     * </ul>\n
     * </p>
     */
    @PostMapping(path = "/parse/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter parseFormStream(@RequestBody ParseRequest request) throws IOException {
        ParsingRequest parsingRequest = buildParsingRequest(request);

        // 0L = no timeout (browser will typically manage reconnect/timeout). You can set e.g. 60_000L if desired.
        SseEmitter emitter = new SseEmitter(0L);

        instructor.parseStreaming(parsingRequest).subscribe(
                evt -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(toEventName(evt))
                                .data(evt));
                    } catch (Exception sendEx) {
                        emitter.completeWithError(sendEx);
                    }
                },
                emitter::completeWithError,
                emitter::complete
        );

        return emitter;
    }

    @GetMapping("/schema/{formType}")
    public String getSchema(@PathVariable String formType) throws IOException {
        if ("leave".equals(formType)) {
            return schemaGenerator.generateSchemaWithAnnotations(LeaveRequestForm.class);
        } else if ("task".equals(formType)) {
            return loadResourceFile("task-request-schema.json");
        } else {
            throw new IllegalArgumentException("Unknown form type: " + formType);
        }
    }

    private String loadResourceFile(String filename) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + filename);
            }
            return new String(inputStream.readAllBytes());
        }
    }

    private ParsingRequest buildParsingRequest(ParseRequest request) throws IOException {
        String schema;

        if ("leave".equals(request.getFormType())) {
            // Generate schema from DTO
            schema = schemaGenerator.generateSchemaWithAnnotations(LeaveRequestForm.class);
        } else if ("task".equals(request.getFormType())) {
            // Load task schema from resources
            schema = loadResourceFile("task-request-schema.json");
        } else {
            throw new IllegalArgumentException("Unknown form type: " + request.getFormType());
        }

        // Create context with current time
        Map<String, Object> context = Map.of(
                "now", Instant.now().toString(),
                "locale", "en-US",
                "user", Map.of(
                        "role", "employee",
                        "managerId", "walter"
                )
        );

        return new ParsingRequest(schema, request.getUserInput(), context);
    }

    private static String toEventName(StreamingParseEvent evt) {
        if (evt instanceof StreamingParseEvent.AttemptStarted) return "attemptStarted";
        if (evt instanceof StreamingParseEvent.RawChunk) return "rawChunk";
        if (evt instanceof StreamingParseEvent.Snapshot) return "snapshot";
        if (evt instanceof StreamingParseEvent.AttemptFailed) return "attemptFailed";
        if (evt instanceof StreamingParseEvent.FinalResult) return "finalResult";
        if (evt instanceof StreamingParseEvent.Error) return "error";
        return "event";
    }
}

