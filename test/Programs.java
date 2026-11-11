import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Starts the real Servidor and Cliente programs in separate JVMs, for the tests. */
final class Programs {
    /** Short timeouts, so that the transfers with loss finish quickly. */
    static final String[] FAST = {
            "-Dtcpow.tempo.ack=150",
            "-Dtcpow.tempo.estouro=100",
            "-Dtcpow.tempo.finack=400",
            "-Dtcpow.intervalo.envio=1",
    };

    private Programs() {
    }

    static int freePort() {
        try (DatagramSocket socket = new DatagramSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static Path tempDir() {
        try {
            return Files.createTempDirectory("tcpow-test");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A running program with its output collected in the background. */
    static final class Running implements AutoCloseable {
        final Process process;
        /** The port of a server, or 0. */
        int port;
        private final StringBuffer output = new StringBuffer();

        Running(Process process) {
            this.process = process;
            Thread reader = new Thread(() -> {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) output.append(line).append('\n');
                } catch (IOException ignored) {
                    // The process ended.
                }
            });
            reader.setDaemon(true);
            reader.start();
        }

        String output() {
            return output.toString();
        }

        void type(String line) {
            try {
                OutputStream in = process.getOutputStream();
                in.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                in.flush();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        boolean waitFor(String text, long timeoutMs) {
            long end = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < end) {
                if (output().contains(text)) return true;
                if (!process.isAlive() && output().contains(text)) return true;
                sleep(20);
            }
            return output().contains(text);
        }

        boolean waitForExit(long timeoutMs) {
            try {
                boolean done = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                sleep(50); // Let the reader thread take the last lines.
                return done;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        public void close() {
            process.destroyForcibly();
        }
    }

    static Running start(String mainClass, Path dir, String... properties) {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        for (String property : properties) command.add(property);
        command.add("-cp");
        command.add(Path.of(System.getProperty("tcpow.classes", "out")).toAbsolutePath().toString());
        command.add(mainClass);
        try {
            Process process = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
            return new Running(process);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Starts a server on a free port and waits until it listens. Another process
     * can take the free port before the server binds it, so this tries again with
     * a new port.
     */
    static Running server(Path dir, String outputFolder, String... properties) {
        String output = "";
        for (int attempt = 0; attempt < 5; attempt++) {
            int port = freePort();
            Running server = start("Servidor", dir, properties);
            server.type(port + " " + outputFolder);
            long end = System.currentTimeMillis() + 10000;
            while (System.currentTimeMillis() < end) {
                if (server.output().contains(Utils.servidor_iniciado(port))) {
                    server.port = port;
                    return server;
                }
                if (server.output().contains("BindException")) break;
                sleep(20);
            }
            output = server.output();
            server.close();
            if (!output.contains("BindException")) break;
        }
        throw new AssertionError("the server did not start:\n" + output);
    }

    /** Runs a client to the end and returns its output. */
    static String client(Path dir, String input, long timeoutMs, String... properties) {
        try (Running client = start("Cliente", dir, properties)) {
            client.type(input);
            if (!client.waitForExit(timeoutMs)) {
                throw new AssertionError("the client did not finish in " + timeoutMs + " ms:\n" + tail(client.output()));
            }
            return client.output();
        }
    }

    static String tail(String text) {
        String[] lines = text.split("\n");
        StringBuilder out = new StringBuilder();
        for (int i = Math.max(0, lines.length - 15); i < lines.length; i++) out.append(lines[i]).append('\n');
        return out.toString();
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
