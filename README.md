# ProcessRunner — Supervisió de processos i comunicació IPC

Pràctica de Java sobre la creació, supervisió i comunicació entre processos del sistema operatiu.

## Descripció

Aquest projecte implementa una classe `ProcessRunner` per executar ordres externes i gestionar-ne l'entrada, la sortida, els errors i el temps d'execució. També inclou proves de concurrència amb workers i un pipeline manual entre dos processos.

## Funcionalitats

- **`launchProcess()`**: inicia un procés a partir d'una llista d'arguments.
- **`captureOutput()`**: llegeix simultàniament `stdout` i `stderr`, i retorna les línies amb la seva marca temporal.
- **`sendInput()`**: envia text a l'entrada estàndard (`stdin`) d'un procés.
- **`cancelProcess()`**: intenta aturar un procés i els processos fills que pugui tenir.
- **`runWorkers()`**: executa diverses ordres com a workers, amb un màxim de dos workers simultanis.
- **`connectProcesses()`**: connecta la sortida del productor amb l'entrada del consumidor i recull la sortida i els errors.
- **`execute()`**: combina l'execució d'un procés, l'enviament d'entrada, la captura de sortides i el control del timeout.

El projecte també utilitza les classes de suport `ProcessResult`, `OutputLine`, `PipelineResult` i `TerminationCause` per representar els resultats, les línies de sortida, el resultat del pipeline i la causa de finalització.

## Requisits

- JDK de Java compatible amb les característiques del codi utilitzades al projecte.
- Un entorn Unix/Linux o macOS per executar les ordres de les proves (`sh`, `sleep`, `awk`, `cat` i `echo`). A Windows es pot utilitzar WSL si està configurat.
- Un IDE Java o una eina de compilació com Maven/Gradle, si el projecte ja està configurat amb alguna d'aquestes eines.

## Estructura principal

```text
src/
└── demo/
    └── process/
        ├── Main.java
        ├── ProcessRunner.java
        ├── ProcessResult.java
        ├── OutputLine.java
        ├── PipelineResult.java
        └── TerminationCause.java
```

> L'arbre és orientatiu: adapta'l si l'estructura real del repositori és diferent.

## Execució

### Des d'un IDE

1. Obre el projecte a l'IDE.
2. Comprova que les classes estiguin al paquet `demo.process`.
3. Executa `Main`.
4. Si `Main` utilitza el `switch-case` per seleccionar proves, indica l'argument corresponent a la configuració d'execució.

### Seleccionar una prova

Si utilitzes la versió de `Main` amb `switch-case` que selecciona les proves, pots passar un d'aquests arguments:

| Argument | Prova |
|---|---|
| `all` | Executa totes les proves (també és l'opció per defecte). |
| `launch` | Prova `launchProcess()`. |
| `capture` | Prova la captura de `stdout` i `stderr`. |
| `input` | Prova l'enviament de dades per `stdin`. |
| `cancel` | Prova la cancel·lació d'un procés. |
| `workers` | Prova l'execució concurrent de workers. |
| `pipe` | Prova el pipeline productor-consumidor. |
| `execute` | Prova una execució normal amb `execute()`. |
| `timeout` | Prova el control del temps màxim d'execució. |

Per executar-lo des del terminal, adapta la comanda al directori de compilació del teu projecte. Per exemple, si les classes compilades són a `out`:

```bash
java -cp out demo.process.Main all
```

Per executar només els workers:

```bash
java -cp out demo.process.Main workers
```

Si no has configurat la compilació a `out`, utilitza el directori de sortida que generi el teu IDE o sistema de compilació.

## Exemples del que es comprova

- Una ordre que acaba correctament i retorna el codi de sortida.
- Una ordre inexistent, que provoca una `IOException`.
- La captura separada de `stdout` i `stderr`.
- L'enviament de text a un procés mitjançant `stdin`.
- Un procés que supera el timeout establert.
- Un pipeline que envia diverses línies d'un productor a un consumidor.
- Diversos workers, incloent-hi un worker que acaba amb codi d'error `7`.
- La cancel·lació d'un procés llarg.

## Conceptes treballats

- Creació i supervisió de processos.
- Comunicació mitjançant `stdin`, `stdout` i `stderr`.
- Concurrència amb `ExecutorService` i `Future`.
- Gestió de temps màxims d'execució (*timeouts*).
- Codis de sortida i diagnosi d'errors.
- Comunicació entre processos mitjançant un pipeline manual.

## Estat del projecte

Projecte acadèmic de pràctiques. Les ordres de prova i els arguments disponibles depenen de la versió actual de `Main.java`.

## Autor

Arnau Serra Soriano
