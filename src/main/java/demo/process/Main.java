
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
                List.of("bash", "-c", "echo \"Missatge d'error\" >&2"),
                "",
                Duration.ofSeconds(3)
        );

        System.out.println("stderr: " + error.stderr());

        // 4. Prova de timeout
        System.out.println("\n--- 4. TIMEOUT ---");
        ProcessResult timeout = runner.execute(
                List.of("sleep", "3"),
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

        // 7. Diagnosi d'errors: un worker acaba amb codi 7
        System.out.println("\n--- 7. DIAGNOSI D'ERRORS ---");

        List<List<String>> workerCommands = List.of(
                List.of("sh", "-c", "echo 'Worker correcte'; exit 0"),
                List.of("sh", "-c", "echo 'Worker amb error' >&2; exit 7"),
                List.of("sh", "-c", "echo 'Altre worker correcte'; exit 0")
        );

        List<ProcessResult> results = runner.runWorkers(
                workerCommands,
                Duration.ofSeconds(3)
        );

        for (int i = 0; i < results.size(); i++) {
            ProcessResult result = results.get(i);

            System.out.println(
                    "Worker " + (i + 1)
                            + " - codi: " + result.exitCode()
            );

            if (result.exitCode() != null && result.exitCode() != 0) {
                System.err.println(
                        "ERROR: el worker " + (i + 1)
                                + " ha acabat amb codi "
                                + result.exitCode()
                );
            }

            System.out.println("stdout: " + result.stdout());
            System.out.println("stderr: " + result.stderr());
        }

        // 8. Prova de cancel·lació
        System.out.println("\n--- 7. CANCEL·LACIÓ ---");
        Process process = runner.launchProcess(
                List.of("sleep", "30")
        );

        System.out.println("Procés iniciat, PID: " + process.pid());

        runner.cancelProcess(process);

        boolean finalitzat = process.waitFor(2, TimeUnit.SECONDS);
        System.out.println("Procés finalitzat: " + finalitzat);
        Process p = new ProcessBuilder("sleep", "30").start();

        Thread ioStdout = new Thread(() -> {
            try {
                p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }, "io-stdout");

        Thread ioStderr = new Thread(() -> {
            try {
                p.getErrorStream().transferTo(java.io.OutputStream.nullOutputStream());
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }, "io-stderr");

        // Crear dos hilos pool-* y mantenerlos activos.
        java.util.concurrent.CountDownLatch latch =
                new java.util.concurrent.CountDownLatch(1);

        java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool(2);

        pool.submit(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        pool.submit(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        ioStdout.start();
        ioStderr.start();

        System.out.println("PID de Java: " + ProcessHandle.current().pid());
        System.out.println("PID de sleep: " + p.pid());

        Thread.sleep(3000);

        latch.countDown();
        pool.shutdown();
        ioStdout.join();
        ioStderr.join();
        p.waitFor();

        // 9. WORKER QUE SUPERA EL LÍMIT TEMPORAL
        System.out.println("\n--- 9. TIMEOUT I FINALITZACIÓ DEL WORKER ---");

        ProcessResult result = runner.execute(
                List.of("sh", "-c", "echo 'Worker iniciat'; sleep 30"),
                "",
                Duration.ofSeconds(2)
        );

        System.out.println("Ha superat el temps: " + result.timedOut());
        System.out.println("Codi de sortida: " + result.exitCode());
        System.out.println("Sortida: " + result.stdout());
        System.out.println("Error: " + result.stderr());

    }


}
