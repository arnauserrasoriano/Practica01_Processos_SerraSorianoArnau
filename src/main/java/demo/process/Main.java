
package demo.process;

import java.time.Duration;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        ProcessRunner runner = new ProcessRunner();

        ProcessResult result = runner.execute(
                List.of(
                        "bash", "-c",
                        "read line; echo \"Has escrit: $line\"; " +
                                "echo \"Missatge de prova\" >&2"
                ),
                "Hola des de Java",
                Duration.ofSeconds(5)
        );

        System.out.println("Codi de sortida: " + result.exitCode());
        System.out.println("Durada: " + result.duration());
        System.out.println("Timeout: " + result.timedOut());

        System.out.println("--- STDOUT ---");
        for (OutputLine line : result.stdout()) {
            System.out.println(line.text());
        }

        System.out.println("--- STDERR ---");
        for (OutputLine line : result.stderr()) {
            System.out.println(line.text());
        }
    }
}
