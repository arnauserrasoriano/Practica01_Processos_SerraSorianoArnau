
package demo.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ProcessRunner {

    public Process launchProcess(List<String> command)
            throws IOException {

        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("La llista de comandes no pot estar buida.");
        }

        return new ProcessBuilder(command).start();
    }

    public List<OutputLine> captureOutput(Process process) throws InterruptedException, ExecutionException {

        ConcurrentLinkedQueue<OutputLine> output = new ConcurrentLinkedQueue<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> stdout = executor.submit(
                    () -> readStream(process.getInputStream(), "stdout", output));

            Future<?> stderr = executor.submit(
                    () -> readStream(process.getErrorStream(), "stderr", output));

            stdout.get();
            stderr.get();

            return output.stream().sorted((a, b) -> a.timestamp().compareTo(b.timestamp())).toList();

        } finally {
            executor.shutdownNow();
        }
    }



    public void sendInput(Process process, String input) throws IOException {
        try (var writer = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(
                        process.getOutputStream(),
                        java.nio.charset.StandardCharsets.UTF_8))) {

            if (input != null && !input.isEmpty()) {
                writer.write(input);
                writer.newLine();
            }
        }
    }


    private void readStream(java.io.InputStream stream, String source, ConcurrentLinkedQueue<OutputLine> output) {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                output.add(new OutputLine(Instant.now(), source, line));
            }

        } catch (IOException e) {
            throw new RuntimeException("Error llegint " + source, e);
        }
    }

    private static final Logger LOGGER =
            Logger.getLogger(ProcessRunner.class.getName());

    private void stopProcess(
            Process process,
            TerminationCause cause) {

        // Obtenir els processos fills abans d'aturar el principal.
        List<ProcessHandle> descendants =
                process.toHandle().descendants().toList();

        LOGGER.info("Tancant el procés. Causa: " + cause);

        // Intentar aturar primer els processos fills.
        for (ProcessHandle child : descendants) {
            child.destroy();
        }

        // Intentar aturar el procés principal.
        process.destroy();

        try {
            // Donar 500 ms perquè el procés principal s'aturi.
            process.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS);

            // Si algun fill continua actiu, forçar-ne el tancament.
            for (ProcessHandle child : descendants) {
                if (child.isAlive()) {
                    child.destroyForcibly();
                }
            }

            // Si el principal continua actiu, forçar-ne el tancament.
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor();
            }

        } catch (InterruptedException e) {
            // Si s'interromp aquest fil, forçar l'aturada.
            for (ProcessHandle child : descendants) {
                if (child.isAlive()) {
                    child.destroyForcibly();
                }
            }

            if (process.isAlive()) {
                process.destroyForcibly();
            }

            Thread.currentThread().interrupt();
        }
    }

    public void cancelProcess(Process process) {
        if (process != null && process.isAlive()) {
            stopProcess(process, TerminationCause.CANCELLED);
        }
    }


    public List<ProcessResult> runWorkers(
            List<List<String>> workerCommands,
            java.time.Duration timeout)
            throws InterruptedException,
            java.util.concurrent.ExecutionException {

        // Com a màxim dos workers executant-se alhora.
        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            List<Future<ProcessResult>> futures =
                    new java.util.ArrayList<>();

            // Enviar tots els workers a la cua d'execució.
            for (List<String> command : workerCommands) {
                Future<ProcessResult> future = executor.submit(
                        () -> execute(command, "", timeout)
                );

                futures.add(future);
            }

            // Recollir i agregar tots els resultats.
            List<ProcessResult> results =
                    new java.util.ArrayList<>();

            for (Future<ProcessResult> future : futures) {
                results.add(future.get());
            }

            return List.copyOf(results);

        } finally {
            executor.shutdownNow();
        }
    }

    public PipelineResult connectProcesses(
            List<String> producerCommand,
            List<String> consumerCommand)
            throws IOException, InterruptedException,
            java.util.concurrent.ExecutionException {

        Process producer =
                new ProcessBuilder(producerCommand).start();

        Process consumer =
                new ProcessBuilder(consumerCommand).start();

        ExecutorService executor =
                Executors.newFixedThreadPool(4);

        try {
            // 1. Connectar stdout del productor amb stdin del consumidor.
            Future<?> transfer = executor.submit(() -> {
                try (
                        var input = producer.getInputStream();
                        var output = consumer.getOutputStream()
                ) {
                    input.transferTo(output);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // 2. Llegir stdout del co+nsumidor.
            Future<String> consumerOutput = executor.submit(() ->
                    new String(
                            consumer.getInputStream().readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8
                    )
            );

            // 3. Llegir stderr dels dos processos.
            Future<String> producerError = executor.submit(() ->
                    new String(
                            producer.getErrorStream().readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8
                    )
            );

            Future<String> consumerError = executor.submit(() ->
                    new String(
                            consumer.getErrorStream().readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8
                    )
            );

            // 4. Esperar que acabin els processos.
            int producerExitCode = producer.waitFor();
            transfer.get();

            int consumerExitCode = consumer.waitFor();

            String output = consumerOutput.get();
            String error1 = producerError.get();
            String error2 = consumerError.get();

            // 5. Registrar els errors per a la diagnosi.
            if (!error1.isEmpty()) {
                System.err.println("Error del productor: " + error1);
            }

            if (!error2.isEmpty()) {
                System.err.println("Error del consumidor: " + error2);
            }

            // 6. Retornar els dos codis de sortida.
            return new PipelineResult(
                    producerExitCode,
                    consumerExitCode,
                    output
            );

        } finally {
            executor.shutdownNow();
        }
    }

    public ProcessResult execute(
            List<String> command,
            String input,
            Duration timeout)
            throws IOException, InterruptedException,
            java.util.concurrent.ExecutionException {

        long start = System.nanoTime();
        Process process = launchProcess(command);

        ConcurrentLinkedQueue<OutputLine> output =
                new ConcurrentLinkedQueue<>();

        ExecutorService executor =
                Executors.newFixedThreadPool(3);

        try {
            // Llegir stdout i stderr al mateix temps.
            Future<?> stdoutFuture = executor.submit(
                    () -> readStream(
                            process.getInputStream(),
                            "stdout",
                            output));

            Future<?> stderrFuture = executor.submit(
                    () -> readStream(
                            process.getErrorStream(),
                            "stderr",
                            output));

            // Enviar l'entrada en una tasca separada i tancar stdin.

            Future<?> inputFuture = executor.submit(() -> {
                try {
                    sendInput(process, input);
                } catch (IOException e) {
                    // Si el procés ja ha acabat, pot haver tancat stdin.
                    if (process.isAlive()) {
                        throw new RuntimeException(e);
                    }
                }
            });

            // Esperar fins al límit de temps.

            boolean finished = process.waitFor(
                    timeout.toMillis(),
                    java.util.concurrent.TimeUnit.MILLISECONDS);

            boolean timedOut = !finished;

            TerminationCause cause;

            if (timedOut) {
                cause = TerminationCause.TIMEOUT;
                stopProcess(process, cause);
            } else {
                cause = TerminationCause.NORMAL;
                LOGGER.info("El procés ha acabat normalment.");
            }

            // Esperar que acabin les tasques d'entrada i de lectura.
            try {
                inputFuture.get();
            } catch (java.util.concurrent.ExecutionException e) {
                if (!timedOut) {
                    throw e;
                }
            }

            stdoutFuture.get();
            stderrFuture.get();

            long elapsedNanos = System.nanoTime() - start;
            Duration duration = Duration.ofNanos(elapsedNanos);

            List<OutputLine> stdout = new ArrayList<>();
            List<OutputLine> stderr = new ArrayList<>();

            for (OutputLine line : output) {
                if (line.source().equals("stdout")) {
                    stdout.add(line);
                } else if (line.source().equals("stderr")) {
                    stderr.add(line);
                }
            }

            Integer exitCode = timedOut
                    ? null
                    : process.exitValue();

            return new ProcessResult(
                    exitCode,
                    duration,
                    stdout,
                    stderr,
                    timedOut,
                    cause
            );

        } finally {
            executor.shutdownNow();
        }
    }


}
