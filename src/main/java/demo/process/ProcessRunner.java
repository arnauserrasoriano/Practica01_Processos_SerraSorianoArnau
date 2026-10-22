
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

    public void sendInput(Process process, String input)
            throws IOException {

        try (var writer = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(
                        process.getOutputStream(),
                        java.nio.charset.StandardCharsets.UTF_8))) {

            writer.write(input);
            writer.newLine();
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
                    throw new RuntimeException(e);
                }
            });

            // Esperar fins al límit de temps.
            boolean finished = process.waitFor(
                    timeout.toMillis(), TimeUnit.MILLISECONDS);

            boolean timedOut = !finished;

            if (timedOut) {
                process.destroyForcibly();
                process.waitFor();
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
                    timedOut);

        } finally {
            executor.shutdownNow();
        }
    }


}
