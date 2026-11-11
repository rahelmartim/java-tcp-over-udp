/** Tests for conexao, one client connection on the server. */
public class ConexaoTest {

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void newConnectionIsActiveAndEmpty() {
        conexao c = new conexao();
        Assert.assertTrue(c.isAtiva(), "active");
        Assert.assertEquals(null, c.getArquivo(), "file");
    }

    @Test
    public void partsAreJoinedInOrder() {
        conexao c = new conexao();
        c.add_parte(new byte[] {1, 2});
        c.add_parte(new byte[0]);
        c.add_parte(new byte[] {3});
        c.add_parte(new byte[] {4, 5, 6});
        Assert.assertBytes(new byte[] {1, 2, 3, 4, 5, 6}, c.getArquivo(), "file");
    }

    @Test
    public void manyFullPartsKeepTheirSize() {
        conexao c = new conexao();
        for (int i = 0; i < 100; i++) {
            byte[] part = new byte[512];
            java.util.Arrays.fill(part, (byte) i);
            c.add_parte(part);
        }
        byte[] file = c.getArquivo();
        Assert.assertEquals(51200, file.length, "size");
        Assert.assertEquals(99, file[51199], "last byte");
    }

    @Test
    public void expectedSequenceNumber() {
        conexao c = new conexao();
        c.setProx_esperado(12346);
        Assert.assertEquals(12346, c.getProx_esperado(), "expected sequence");
    }

    @Test
    public void connectionExpiresWithoutPackets() {
        conexao c = new conexao();
        sleep(Utils.tempo_espera_servidor + 300);
        Assert.assertFalse(c.isAtiva(), "still active after the timeout");
    }

    @Test
    public void packetsKeepTheConnectionActive() {
        conexao c = new conexao();
        for (int i = 0; i < 4; i++) {
            sleep(Utils.tempo_espera_servidor / 2);
            c.desativa_timer(); // The server calls this for every packet.
        }
        Assert.assertTrue(c.isAtiva(), "expired while packets arrived");
    }
}
