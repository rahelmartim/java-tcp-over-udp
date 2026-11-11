import java.util.Random;

import static java.lang.Math.min;

/** Tests for Utils.cria_pacote: the bytes on the wire and back. */
public class PacketCodecTest {

    private static Pacote packet(int seq, int ack, int id, boolean a, boolean s, boolean f, byte[] content) {
        Pacote p = new Pacote();
        p.setNumero_de_sequencia(seq);
        p.setNumero_ack(ack);
        p.setId((short) id);
        p.setACK(a);
        p.setSYN(s);
        p.setFIN(f);
        p.setConteudo(content);
        return p;
    }

    private static byte[] bytes(int n, long seed) {
        byte[] data = new byte[n];
        new Random(seed).nextBytes(data);
        return data;
    }

    @Test
    public void headerOnlyPacketHas12Bytes() {
        Assert.assertEquals(12, Utils.cria_pacote(packet(1, 2, 3, true, false, false, null)).length, "length");
    }

    @Test
    public void packetLengthIsHeaderPlusContent() {
        for (int n : new int[] {0, 1, 100, 511, 512}) {
            byte[] wire = Utils.cria_pacote(packet(0, 0, 0, false, false, false, new byte[n]));
            Assert.assertEquals(12 + n, wire.length, "length for " + n + " content bytes");
        }
    }

    @Test
    public void contentLongerThan512BytesIsCut() {
        byte[] wire = Utils.cria_pacote(packet(0, 0, 0, false, false, false, new byte[600]));
        Assert.assertEquals(524, wire.length, "length");
    }

    @Test
    public void sequenceAckAndIdAreBigEndian() {
        byte[] wire = Utils.cria_pacote(packet(12345, 0x01020304, 0x0A0B, false, false, false, null));
        byte[] expected = {0, 0, 0x30, 0x39, 1, 2, 3, 4, 0x0A, 0x0B, 0, 0};
        Assert.assertBytes(expected, wire, "header bytes");
    }

    @Test
    public void flagsUseTheLastThreeBitsOfTheHeader() {
        // Same layout as the original BitArray version: bit 2 = ACK, bit 1 = SYN, bit 0 = FIN.
        Assert.assertEquals(4, Utils.cria_pacote(packet(0, 0, 0, true, false, false, null))[11], "ACK");
        Assert.assertEquals(2, Utils.cria_pacote(packet(0, 0, 0, false, true, false, null))[11], "SYN");
        Assert.assertEquals(1, Utils.cria_pacote(packet(0, 0, 0, false, false, true, null))[11], "FIN");
        Assert.assertEquals(7, Utils.cria_pacote(packet(0, 0, 0, true, true, true, null))[11], "all flags");
        Assert.assertEquals(0, Utils.cria_pacote(packet(0, 0, 0, true, true, true, null))[10], "unused flag byte");
    }

    @Test
    public void everyFlagCombinationSurvivesARoundTrip() {
        for (int bits = 0; bits < 8; bits++) {
            boolean a = (bits & 4) != 0, s = (bits & 2) != 0, f = (bits & 1) != 0;
            Pacote back = Utils.cria_pacote(Utils.cria_pacote(packet(1, 1, 1, a, s, f, null)));
            Assert.assertEquals(a, back.isACK(), "ACK for " + bits);
            Assert.assertEquals(s, back.isSYN(), "SYN for " + bits);
            Assert.assertEquals(f, back.isFIN(), "FIN for " + bits);
        }
    }

    @Test
    public void numbersSurviveARoundTrip() {
        int[] values = {0, 1, 511, 512, 4321, 12345, 102400, Integer.MAX_VALUE, -1, Integer.MIN_VALUE};
        for (int value : values) {
            Pacote back = Utils.cria_pacote(Utils.cria_pacote(packet(value, value ^ 0x5555, 7, false, false, false, null)));
            Assert.assertEquals(value, back.getNumero_de_sequencia(), "sequence");
            Assert.assertEquals(value ^ 0x5555, back.getNumero_ack(), "ack");
        }
        for (short id : new short[] {0, 1, 2, Short.MAX_VALUE, -1, Short.MIN_VALUE}) {
            Pacote back = Utils.cria_pacote(Utils.cria_pacote(packet(0, 0, id, false, false, false, null)));
            Assert.assertEquals(id, back.getId(), "id");
        }
    }

    @Test
    public void contentSurvivesARoundTripForEverySize() {
        for (int n = 0; n <= 512; n++) {
            byte[] content = bytes(n, n);
            Pacote back = Utils.cria_pacote(Utils.cria_pacote(packet(9, 9, 9, false, false, false, content)));
            Assert.assertBytes(content, back.getConteudo(), "content of " + n + " bytes");
        }
    }

    @Test
    public void parsingUsesTheReceivedLengthAndIgnoresTheRestOfTheBuffer() {
        // A DatagramPacket buffer is larger than the packet, and can hold old bytes.
        byte[] content = bytes(100, 1);
        byte[] wire = Utils.cria_pacote(packet(5, 6, 7, true, false, false, content));
        byte[] buffer = bytes(Utils.tamanho_pacote * 2, 2);
        System.arraycopy(wire, 0, buffer, 0, wire.length);

        Pacote back = Utils.cria_pacote(buffer, wire.length);
        Assert.assertBytes(content, back.getConteudo(), "content");
        Assert.assertEquals(5, back.getNumero_de_sequencia(), "sequence");
    }

    @Test
    public void parsingAHeaderOnlyPacketGivesEmptyContent() {
        byte[] wire = Utils.cria_pacote(packet(1, 2, 3, true, false, false, null));
        byte[] buffer = new byte[Utils.tamanho_pacote];
        System.arraycopy(wire, 0, buffer, 0, wire.length);
        Assert.assertEquals(0, Utils.cria_pacote(buffer, wire.length).getConteudo().length, "content length");
    }

    @Test
    public void parsingNeverReadsMoreThan512ContentBytes() {
        byte[] buffer = bytes(Utils.tamanho_pacote * 2, 3);
        Assert.assertEquals(512, Utils.cria_pacote(buffer, buffer.length).getConteudo().length, "content length");
    }

    @Test
    public void lengthLargerThanTheBufferIsSafe() {
        byte[] wire = Utils.cria_pacote(packet(1, 2, 3, false, false, false, bytes(10, 4)));
        Assert.assertEquals(10, Utils.cria_pacote(wire, 10_000).getConteudo().length, "content length");
    }

    @Test
    public void oneArgumentParserUsesTheWholeArray() {
        byte[] wire = Utils.cria_pacote(packet(1, 2, 3, false, true, false, bytes(77, 5)));
        Assert.assertBytes(Utils.cria_pacote(wire, wire.length).getConteudo(), Utils.cria_pacote(wire).getConteudo(), "content");
    }

    @Test
    public void randomPacketsSurviveARoundTrip() {
        Random random = new Random(42);
        for (int i = 0; i < 2000; i++) {
            byte[] content = bytes(random.nextInt(513), i);
            Pacote p = packet(random.nextInt(), random.nextInt(), random.nextInt(), random.nextBoolean(),
                    random.nextBoolean(), random.nextBoolean(), content);
            Pacote back = Utils.cria_pacote(Utils.cria_pacote(p));
            Assert.assertEquals(p.toString(), back.toString(), "fields of packet " + i);
            Assert.assertBytes(content, back.getConteudo(), "content of packet " + i);
        }
    }

    @Test
    public void encodingDoesNotChangeThePacket() {
        byte[] content = bytes(min(50, 512), 6);
        Pacote p = packet(1, 2, 3, true, true, false, content.clone());
        Utils.cria_pacote(p);
        Assert.assertBytes(content, p.getConteudo(), "content");
    }
}
