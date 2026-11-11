import java.util.HashMap;
import java.util.Map;

/** Tests for the log messages in Utils. */
public class MessagesTest {

    private static Pacote packet(int seq, int ack, int id, boolean a, boolean s, boolean f) {
        Pacote p = new Pacote();
        p.setNumero_de_sequencia(seq);
        p.setNumero_ack(ack);
        p.setId((short) id);
        p.setACK(a);
        p.setSYN(s);
        p.setFIN(f);
        return p;
    }

    @Test
    public void handshakeMessage() {
        Assert.assertEquals("[Handshake 1° via] SEQ = '12345', ACK = '0', ID = '0', SYN.",
                Utils.handshake(1, packet(12345, 0, 0, false, true, false)), "first message");
        Assert.assertEquals("[Handshake 2° via] SEQ = '4321', ACK = '12345', ID = '1', SYN, ACK.",
                Utils.handshake(2, packet(4321, 12345, 1, true, true, false)), "second message");
    }

    @Test
    public void sentAndReceivedMessagesListTheFlags() {
        Assert.assertEquals("[Pacote enviado] SEQ = '1', ACK = '2', ID = '3', ACK, FIN.",
                Utils.pacote_enviado(packet(1, 2, 3, true, false, true)), "sent");
        Assert.assertEquals("[Pacote recebido] SEQ = '1', ACK = '2', ID = '3'.\n",
                Utils.pacote_recebido(packet(1, 2, 3, false, false, false)), "received");
    }

    @Test
    public void configurationMessages() {
        Assert.assertContains(Utils.dados_definidos("localhost", 5555, "photo.jpg"), "'localhost'", "client");
        Assert.assertContains(Utils.dados_definidos("localhost", 5555, "photo.jpg"), "'photo.jpg'", "client");
        Assert.assertEquals("Servidor iniciado na porta 5555", Utils.servidor_iniciado(5555), "server started");
        Assert.assertEquals("Conexão criada ID = '7'.", Utils.conexao_criada(7), "connection");
        Assert.assertEquals("Arquivo salvo em: out/1.file com sucesso.", Utils.arquivo_salvo("out/1.file"), "saved");
    }

    @Test
    public void windowMessageShowsEveryPacket() {
        Map<Integer, Estado> window = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            Estado e = new Estado();
            e.setNumero_sequencia(100 + i);
            window.put(i, e);
        }
        String text = Utils.janela(3, 1536, 10000, window);
        Assert.assertContains(text, "CWND =1536 SS_THRESH =10000", "window sizes");
        for (int i = 0; i < 3; i++) Assert.assertContains(text, "Seq:" + (100 + i), "packet " + i);
    }

    @Test
    public void expectedSequenceMessage() {
        Assert.assertEquals("Numero de sequencia esperado -->  10 / 20  <-- recebido.", Utils.seq_esperado(10, 20), "message");
    }
}
