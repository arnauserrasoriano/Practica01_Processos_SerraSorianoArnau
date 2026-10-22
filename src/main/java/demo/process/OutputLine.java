package demo.process;

import java.time.Instant;

public record OutputLine(
        Instant timestamp,
        String source,
        String text
) {}