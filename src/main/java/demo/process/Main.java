
package demo.process;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        ProcessRunner runner = new ProcessRunner();

        // 1. Prova d'èxit
        System.out.println("\n--- 1. ÈXIT ---");
        ProcessResult exit = runner.execute(
                List.of("echo", "Hola món"),
                "",
                Duration.ofSeconds(3)
        );

        System.out.println("Codi: " + exit.exitCode());
        System.out.println("Sortida: " + exit.stdout());

        // 2. Ordre inexistent
        System.out.println("\n--- 2. ORDRE INEXISTENT ---");
        try {
            runner.launchProcess(
                    List.of("ordre_que_no_existeix_123")
            );
            System.out.println("ERROR: s'esperava una excepció");
        } catch (IOException e) {
            System.out.println("Correcte: ordre no trobada");
        }

        // 3. Prova de stderr
        System.out.println("\n--- 3. STDERR ---");
        ProcessResult error = runner.execute(
                List.of("bash", "-c", "echo 'Missatge d'error' >&2"),
                "",
                Duration.ofSeconds(3)
        );

        System.out.println("stderr: " + error.stderr());

        // 4. Prova de timeout
        System.out.println("\n--- 4. TIMEOUT ---");
        ProcessResult timeout = runner.execute(
                List.of("sleep", "5"),
                "",
                Duration.ofMillis(500)
        );

        System.out.println("Ha superat el temps: " + timeout.timedOut());

        // 5. Prova de stdin
        System.out.println("\n--- 5. STDIN ---");
        ProcessResult input = runner.execute(
                List.of("cat"),
                "Text enviat per stdin",
                Duration.ofSeconds(3)
        );

        System.out.println("Resposta: " + input.stdout());

        // 6. Prova de pipe manual
        System.out.println("\n--- 6. PIPE ---");
        PipelineResult pipe = runner.connectProcesses(
                List.of("printf", "poma\npera\nplatan\n"),
                List.of("grep", "pera")
        );

        System.out.println("Sortida del consumidor: "
                + pipe.consumerOutput());
        System.out.println("Codi productor: " + pipe.producerExitCode());
        System.out.println("Codi consumidor: " + pipe.consumerExitCode());

        // 7. Prova de cancel·lació
        System.out.println("\n--- 7. CANCEL·LACIÓ ---");
        Process process = runner.launchProcess(
                List.of("sleep", "30")
        );

        System.out.println("Procés iniciat, PID: " + process.pid());

        runner.cancelProcess(process);

        boolean finalitzat = process.waitFor(2, TimeUnit.SECONDS);
        System.out.println("Procés finalitzat: " + finalitzat);
    }
}
