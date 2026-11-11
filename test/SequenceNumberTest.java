/** Tests for Utils.proximo_sequencia. */
public class SequenceNumberTest {

    @Test
    public void addsOnePacketSize() {
        Assert.assertEquals(12346 + 512, Utils.proximo_sequencia(12346), "next");
    }

    @Test
    public void reachingTheMaximumIsAllowed() {
        Assert.assertEquals(102400, Utils.proximo_sequencia(102400 - 512), "maximum");
    }

    @Test
    public void wrapsAroundAfterTheMaximum() {
        Assert.assertEquals(112, Utils.proximo_sequencia(102000), "after the wrap");
        Assert.assertEquals(511, Utils.proximo_sequencia(102399), "just below the maximum");
    }

    @Test
    public void staysInRangeForManyPackets() {
        int seq = Utils.numero_sequencia_inicial_cliente;
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int i = 0; i < 200; i++) {
            seq = Utils.proximo_sequencia(seq);
            Assert.assertBetween(1, Utils.numero_max_seq, seq, "sequence number " + i);
            Assert.assertTrue(seen.add(seq), "sequence number " + seq + " repeats within one cycle");
        }
    }
}
