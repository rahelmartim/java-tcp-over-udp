import java.nio.file.Files;
import java.nio.file.Path;

/** Tests for the checks of the values that the user types. */
public class InputValidationTest {

    private static String client(String input) {
        return Programs.client(Programs.tempDir(), input, 15000);
    }

    private static String server(String input) {
        try (Programs.Running server = Programs.start("Servidor", Programs.tempDir())) {
            server.type(input);
            if (!server.waitForExit(10000)) throw new AssertionError("the server did not stop:\n" + server.output());
            return server.output();
        }
    }

    @Test
    public void clientNeedsThreeValues() {
        Assert.assertContains(client("localhost 5555"), Utils.cliente_poucos_parametros, "two values");
        Assert.assertContains(client("localhost 5555 a.txt extra"), Utils.cliente_muitos_parametros, "four values");
    }

    @Test
    public void clientRejectsAPortThatIsNotANumber() {
        Assert.assertContains(client("localhost abc a.txt"), Utils.formato_n_errado, "text port");
    }

    @Test
    public void clientRejectsReservedAndInvalidPorts() {
        Assert.assertContains(client("localhost 80 a.txt"), Utils.porta_reservada, "port 80");
        Assert.assertContains(client("localhost 70000 a.txt"), Utils.porta_invalida, "port 70000");
    }

    @Test
    public void clientRejectsAMissingFile() throws Exception {
        Path dir = Programs.tempDir();
        try (Programs.Running server = Programs.server(dir, "out")) {
            String output = Programs.client(dir, "localhost " + server.port + " missing.txt", 15000, Programs.FAST);
            Assert.assertContains(output, Utils.arquivo_inexistente, "missing file");
        }
        Assert.assertFalse(Files.exists(dir.resolve("out").resolve("1.file")), "a file was saved");
    }

    @Test
    public void clientRejectsAnUnknownHost() {
        Assert.assertContains(client("no-such-host.invalid 5555 a.txt"), Utils.hostname_invalido, "host");
    }

    @Test
    public void clientStopsWhenNoServerAnswers() {
        String output = Programs.client(Programs.tempDir(), "localhost " + Programs.freePort() + " a.txt", 20000,
                "-Dtcpow.tempo.servidor=1500", "-Dtcpow.tempo.ack=100");
        Assert.assertContains(output, Utils.tempo_esgotado, "no server");
    }

    @Test
    public void serverNeedsAPortAndAFolder() {
        Assert.assertContains(server("5555"), Utils.parametro_faltando, "one value");
        Assert.assertContains(server("5555 out extra"), Utils.muitos_parametros, "three values");
    }

    @Test
    public void serverRejectsBadPorts() {
        Assert.assertContains(server("80 out"), Utils.porta_reservada, "port 80");
        Assert.assertContains(server("70000 out"), Utils.porta_invalida, "port 70000");
        Assert.assertContains(server("abc out"), Utils.porta_invalida, "text port");
    }

    @Test
    public void serverRejectsFolderNamesWithPathCharacters() {
        for (String name : new String[] {"../out", "/tmp/out", "a:b", "a*b"}) {
            Assert.assertContains(server("5555 " + name), Utils.nome_diretorio_invalido, "folder " + name);
        }
    }
}
