/** Tests for the Pacote class. */
public class PacoteTest {

    @Test
    public void newPacketHasNoFlagsAndNoContent() {
        Pacote p = new Pacote();
        Assert.assertFalse(p.isACK() || p.isSYN() || p.isFIN(), "flags");
        Assert.assertEquals(0, p.getNumero_de_sequencia(), "sequence");
        Assert.assertEquals(0, p.getNumero_ack(), "ack");
        Assert.assertEquals(0, p.getId(), "id");
        Assert.assertEquals(null, p.getConteudo(), "content");
    }

    @Test
    public void settersAndGettersKeepTheValues() {
        Pacote p = new Pacote();
        byte[] content = {1, 2, 3};
        p.setNumero_de_sequencia(12345);
        p.setNumero_ack(4321);
        p.setId((short) 9);
        p.setACK(true);
        p.setSYN(true);
        p.setFIN(true);
        p.setConteudo(content);
        Assert.assertEquals(12345, p.getNumero_de_sequencia(), "sequence");
        Assert.assertEquals(4321, p.getNumero_ack(), "ack");
        Assert.assertEquals(9, p.getId(), "id");
        Assert.assertTrue(p.isACK() && p.isSYN() && p.isFIN(), "flags");
        Assert.assertBytes(content, p.getConteudo(), "content");
    }

    @Test
    public void toStringShowsTheFields() {
        Pacote p = new Pacote();
        p.setNumero_de_sequencia(10);
        p.setNumero_ack(20);
        p.setId((short) 3);
        p.setSYN(true);
        Assert.assertEquals("ID: 3| SEQ: 10 | ACK n: 20 | A : false | S: true | F: false", p.toString(), "toString");
    }
}
