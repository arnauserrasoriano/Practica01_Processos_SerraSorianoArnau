
package demo.process;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        ProcessRunner runner = new ProcessRunner();

        String opcio = args.length > 0 ? args[0] : "all";

        switch (opcio) {
            case "launch" -> provaLaunchProcess(runner);
            case "capture" -> provaCaptureOutput(runner);
            case "input" -> provaSendInput(runner);
            case "cancel" -> provaCancelProcess(runner);
            case "workers" -> provaRunWorkers(runner);
            case "pipe" -> provaConnectProcesses(runner);
            case "execute" -> provaExecute(runner);
            case "timeout" -> provaTimeout(runner);
            case "all" -> {
                provaLaunchProcess(runner);
                provaCaptureOutput(runner);
                provaSendInput(runner);
                provaCancelProcess(runner);
                provaRunWorkers(runner);
                provaConnectProcesses(runner);
                provaExecute(runner);
                provaTimeout(runner);
            }
            default -> {
                System.err.println("Opció desconeguda: " + opcio);
                System.out.println(
                        "Opcions: all, launch, capture, input, cancel, "
                                + "workers, pipe, execute, timeout"
                );
            }
        }
    }

    // 1. Prova launchProcess()
    private static void provaLaunchProcess(ProcessRunner runner)
            throws Exception {
        System.out.println("\n--- 1. LAUNCH PROCESS ---");

        Process process = runner.launchProcess(
                List.of("echo", "Hola món")
        );

        System.out.println("PID: " + process.pid());

        List<OutputLine> output = runner.captureOutput(process);
        process.waitFor();

        System.out.println("Sortida: " + output);
        System.out.println("Codi: " + process.exitValue());
    }

    // 2. Prova captureOutput()
    private static void provaCaptureOutput(ProcessRunner runner)
            throws Exception {
        System.out.println("\n--- 2. CAPTURE OUTPUT ---");

        Process process = runner.launchProcess(
                List.of("sh", "-c",
                        "echo 'Sortida normal'; "
                                + "echo 'Missatge d error' >&2")
        );

        List<OutputLine> output = runner.captureOutput(process);
        process.waitFor();

        for (OutputLine line : output) {
            System.out.println(
                    "[" + line.source() + "] " + line.text()
            );
        }

        System.out.println("Codi: " + process.exitValue());
    }

    // 3. Prova sendInput()
    private static void provaSendInput(ProcessRunner runner)
            throws Exception {
        System.out.println("\n--- 3. SEND INPUT ---");

        Process process = runner.launchProcess(List.of("cat"));

        runner.sendInput(process, "Text enviat per stdin");

        List<OutputLine> output = runner.captureOutput(process);
        process.waitFor();

        System.out.println("Resposta: " + output);
        System.out.println("Codi: " + process.exitValue());
    }

    // 4. Prova cancelProcess()
    private static void provaCancelProcess(ProcessRunner runner)
            throws Exception {
        System.out.println("\n--- 4. CANCEL PROCESS ---");

        Process process = runner.launchProcess(
                List.of("sleep", "30")
        );

        System.out.println("Procés iniciat. PID: " + process.pid());

        runner.cancelProcess(process);

        boolean finalitzat = process.waitFor(2, TimeUnit.SECONDS);

        System.out.println("Procés finalitzat: " + finalitzat);
        System.out.println("Procés actiu: " + process.isAlive());
    }

    // 5. Prova runWorkers()
    private static void provaRunWorkers(ProcessRunner runner)
            throws Exception {
        System.out.println("\n--- 5. RUN WORKERS ---");

        List<List<String>> commands = List.of(
                List.of("sh", "-c",
                        "echo 'Worker 1 correcte'; exit 0"),
                List.of("sh", "-c",
                        "echo 'Worker 2 amb error' >&2; exit 7"),
                List.of("sh", "-c",
                        "echo 'Worker 3 correcte'; exit 0")
        );

        List<ProcessResult> results = runner.runWorkers(
                commands,
                Duration.ofSeconds(2)
        );

        for (int i = 0; i < results.size(); i++) {
            ProcessResult result = results.get(i);

            System.out.println("\nWorker " + (i + 1));
            System.out.println("Codi: " + result.exitCode());
            System.out.println("Timeout: " + result.timedOut());
            System.out.println("Causa: " + result.terminationCause());
            System.out.println("stdout: " + result.stdout());
            System.out.println("stderr: " + result.stderr());
        }
    }

    // 6. Prova connectProcesses()
    private static void provaConnectProcesses(ProcessRunner runner)
            throws Exception {
        System.out.println("\n--- 6. CONNECT PROCESSES ---");

        PipelineResult result = runner.connectProcesses(
                List.of("printf", "Anna\nBiel\nCarla\nDavid\nEmma\n"),
                List.of(
                        "sh", "-c",
                        "awk '{ n++; chars += length($0) } "
                                + "END { printf \"Linies: %d\\nCaracters: %d\\n\", "
                                + "n, chars }'"
                )
        );

        System.out.println("Sortida del consumidor:");
        System.out.println(result.consumerOutput());

        System.out.println("Codi productor: "
                + result.producerExitCode());
        System.out.println("Codi consumidor: "
                + result.consumerExitCode());
    }

    // 7. Prova execute() amb una execució normal.
    private static void provaExecute(ProcessRunner runner)
            throws Exception {
        System.out.println("\n--- 7. EXECUTE ---");

        ProcessResult result = runner.execute(
                List.of("echo", "Execució correcta"),
                "",
                Duration.ofSeconds(3)
        );

        System.out.println("Codi: " + result.exitCode());
        System.out.println("Durada: " + result.duration());
        System.out.println("stdout: " + result.stdout());
        System.out.println("stderr: " + result.stderr());
        System.out.println("Timeout: " + result.timedOut());
        System.out.println("Causa: " + result.terminationCause());
    }

    // 8. Prova execute() amb timeout.
    private static void provaTimeout(ProcessRunner runner)
            throws Exception {
        System.out.println("\n--- 8. TIMEOUT ---");

        ProcessResult result = runner.execute(
                List.of("sh", "-c",
                        "echo 'Worker iniciat'; sleep 30"),
                "",
                Duration.ofSeconds(2)
        );

        System.out.println("Ha superat el temps: " + result.timedOut());
        System.out.println("Codi: " + result.exitCode());
        System.out.println("Causa: " + result.terminationCause());
        System.out.println("stdout: " + result.stdout());
        System.out.println("stderr: " + result.stderr());
    }
}
