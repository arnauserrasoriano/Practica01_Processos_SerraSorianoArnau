
package demo.process;

public record PipelineResult(
        int producerExitCode,
        int consumerExitCode,
        String consumerOutput
) {}