import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Sends real files from Cliente to Servidor over UDP on localhost and checks every byte. */
public class EndToEndTest {

    private static byte[] random(int n, long seed) {
        byte[] data = new byte[n];
        new Random(seed).nextBytes(data);
        return data;
    }

    private static String[] properties(String... extra) {
        String[] all = new String[Programs.FAST.length + extra.length];
        System.arraycopy(Programs.FAST, 0, all, 0, Programs.FAST.length);
        System.arraycopy(extra, 0, all, Programs.FAST.length, extra.length);
        return all;
    }

    /** Sends one file and returns the bytes that the server saved. */
    private static byte[] transfer(byte[] file, String... extra) throws IOException {
        Path dir = Programs.tempDir();
        Files.write(dir.resolve("input.bin"), file);
        String[] props = properties(extra);
        try (Programs.Running server = Programs.server(dir, "recebidos", props)) {
            String client = Programs.client(dir, "localhost " + server.port + " input.bin", 120_000, props);
            Assert.assertContains(client, Utils.fim_de_conexao, "the client did not close the connection");
            Path saved = dir.resolve("recebidos").resolve("1.file");
            Assert.assertTrue(server.waitFor(Utils.arquivo_salvo("recebidos/1.file"), 10000),
                    "the server did not save the file:\n" + Programs.tail(server.output()));
            return Files.readAllBytes(saved);
        }
    }

    private static void assertTransfer(byte[] file, String... extra) throws IOException {
        Assert.assertBytes(file, transfer(file, extra), "received file of " + file.length + " bytes");
    }

    private static final String NO_LOSS = "-Dtcpow.perda.cliente=0";

    @Test
    public void oneByte() throws IOException {
        assertTransfer(new byte[] {42}, NO_LOSS);
    }

    @Test
    public void emptyFile() throws IOException {
        assertTransfer(new byte[0], NO_LOSS);
    }

    @Test
    public void sizesAroundOnePacket() throws IOException {
        for (int n : new int[] {511, 512, 513}) assertTransfer(random(n, n), NO_LOSS);
    }

    @Test
    public void exactMultipleOfThePacketSize() throws IOException {
        // Regression: the old code added 512 spaces to these files.
        assertTransfer(random(4096, 4), NO_LOSS);
    }

    @Test
    public void textFileKeepsItsLastLine() throws IOException {
        // Regression: the old code padded the last packet with spaces.
        byte[] text = "first line\nsecond line\nlast line without newline".getBytes(StandardCharsets.UTF_8);
        assertTransfer(text, NO_LOSS);
    }

    @Test
    public void samplePhotoWithTheDefaultLoss() throws IOException {
        // 20% of the client packets are lost on purpose.
        byte[] photo = Files.readAllBytes(Path.of("photo.jpg").toAbsolutePath());
        assertTransfer(photo);
    }

    @Test
    public void binaryFileWithHalfOfThePacketsLost() throws IOException {
        assertTransfer(random(20_000, 50), "-Dtcpow.perda.cliente=0.5");
    }

    @Test
    public void largeFileWrapsTheSequenceNumbers() throws IOException {
        // The sequence numbers wrap around after 102400 bytes.
        assertTransfer(random(300_000, 300), "-Dtcpow.perda.cliente=0.05");
    }

    @Test
    public void serverLosesSomeOfItsPacketsToo() throws IOException {
        assertTransfer(random(30_000, 7), "-Dtcpow.perda.cliente=0.1", "-Dtcpow.perda.servidor=0.1");
    }

    @Test
    public void handshakeSurvivesLostAcks() throws IOException {
        // Regression: the client sent the third handshake packet again with a new
        // sequence number each time, so the server never accepted it.
        for (int i = 0; i < 4; i++) assertTransfer(random(1500, 60 + i), "-Dtcpow.perda.cliente=0", "-Dtcpow.perda.servidor=0.4");
    }

    @Test
    public void retransmittedPacketsAreSavedOnlyOnce() throws IOException {
        // Regression: two server threads could accept a packet and its retransmission.
        for (int i = 0; i < 3; i++) assertTransfer(random(20_000, 70 + i), "-Dtcpow.perda.cliente=0.3", "-Dtcpow.perda.servidor=0.3");
    }

    @Test
    public void manyRandomSizesWithLoss() throws IOException {
        Random random = new Random(2017);
        for (int i = 0; i < 6; i++) {
            int n = 1 + random.nextInt(12_000);
            assertTransfer(random(n, i), "-Dtcpow.perda.cliente=0.2");
        }
    }

    @Test
    public void twoClientsInARowGetTheirOwnFiles() throws IOException {
        Path dir = Programs.tempDir();
        byte[] first = random(3000, 1);
        byte[] second = random(5000, 2);
        Files.write(dir.resolve("a.bin"), first);
        Files.write(dir.resolve("b.bin"), second);
        String[] props = properties(NO_LOSS);
        try (Programs.Running server = Programs.server(dir, "out", props)) {
            Programs.client(dir, "localhost " + server.port + " a.bin", 60_000, props);
            Programs.client(dir, "localhost " + server.port + " b.bin", 60_000, props);
            Assert.assertTrue(server.waitFor(Utils.arquivo_salvo("out/2.file"), 10000), "second file not saved");
        }
        Assert.assertBytes(first, Files.readAllBytes(dir.resolve("out/1.file")), "first file");
        Assert.assertBytes(second, Files.readAllBytes(dir.resolve("out/2.file")), "second file");
    }

    @Test
    public void severalClientsAtTheSameTime() throws Exception {
        // Regression: the server shared one receive buffer between its threads.
        Path dir = Programs.tempDir();
        int clients = 4;
        List<byte[]> files = new ArrayList<>();
        for (int i = 0; i < clients; i++) {
            byte[] file = random(8000 + 1000 * i, 100 + i);
            files.add(file);
            Files.write(dir.resolve("f" + i + ".bin"), file);
        }
        String[] props = properties("-Dtcpow.perda.cliente=0.1");
        ExecutorService pool = Executors.newFixedThreadPool(clients);
        try (Programs.Running server = Programs.server(dir, "out", props)) {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < clients; i++) {
                String input = "localhost " + server.port + " f" + i + ".bin";
                results.add(pool.submit(() -> Programs.client(dir, input, 120_000, props)));
            }
            for (Future<String> result : results) result.get();
            for (int id = 1; id <= clients; id++) {
                Assert.assertTrue(server.waitFor(Utils.arquivo_salvo("out/" + id + ".file"), 10000), "file " + id + " not saved");
            }
        } finally {
            pool.shutdownNow();
        }
        // Each client gets its own ID, so match the saved files by content.
        Set<String> expected = new HashSet<>();
        for (byte[] file : files) expected.add(java.util.Arrays.toString(file));
        Set<String> saved = new HashSet<>();
        for (int id = 1; id <= clients; id++) saved.add(java.util.Arrays.toString(Files.readAllBytes(dir.resolve("out/" + id + ".file"))));
        Assert.assertEquals(expected, saved, "saved files");
    }

    @Test
    public void handshakeUsesTheDocumentedSequenceNumbers() throws IOException {
        Path dir = Programs.tempDir();
        Files.write(dir.resolve("x.bin"), new byte[] {1, 2, 3});
        String[] props = properties(NO_LOSS);
        try (Programs.Running server = Programs.server(dir, "out", props)) {
            String output = Programs.client(dir, "localhost " + server.port + " x.bin", 60_000, props);
            Assert.assertContains(output, "[Handshake 1° via] SEQ = '12345', ACK = '0', ID = '0', SYN.", "SYN");
            Assert.assertContains(output, "[Handshake 2° via] SEQ = '4321', ACK = '12345', ID = '1', SYN, ACK.", "SYN-ACK");
            Assert.assertContains(output, "[Handshake 3° via] SEQ = '12346', ACK = '4322', ID = '1', ACK.", "ACK");
        }
    }

    @Test
    public void windowGrowsDuringTheTransfer() throws IOException {
        Path dir = Programs.tempDir();
        Files.write(dir.resolve("x.bin"), random(40_000, 9));
        String[] props = properties(NO_LOSS);
        try (Programs.Running server = Programs.server(dir, "out", props)) {
            String output = Programs.client(dir, "localhost " + server.port + " x.bin", 60_000, props);
            // Slow start reaches the threshold, and congestion avoidance keeps growing above it.
            Assert.assertContains(output, "CWND =10240", "slow start");
            int largest = 0;
            for (String line : output.split("\n")) {
                int at = line.indexOf("CWND =");
                if (at >= 0) largest = Math.max(largest, Integer.parseInt(line.substring(at + 6, line.indexOf(' ', at + 6))));
            }
            Assert.assertTrue(largest > 10240, "the window never grew above the threshold: " + largest);
        }
    }
}
