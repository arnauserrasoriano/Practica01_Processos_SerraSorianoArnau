
package demo.process;

import java.time.Duration;
import java.util.List;

public record ProcessResult(
        Integer exitCode,
        Duration duration,
        List<OutputLine> stdout,
        List<OutputLine> stderr,
        boolean timedOut,
        TerminationCause terminationCause
) {
    public ProcessResult {
        stdout = List.copyOf(stdout);
        stderr = List.copyOf(stderr);
    }
}
