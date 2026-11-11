/** Tests for Estado, the state and retransmission timer of one packet. */
public class EstadoTest {

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void newStateIsNotAcknowledgedAndNotExpired() {
        Estado e = new Estado();
        Assert.assertFalse(e.isEstado(), "acknowledged");
        Assert.assertFalse(e.isEstouro(), "expired");
    }

    @Test
    public void settersKeepTheValues() {
        Estado e = new Estado();
        e.setEstouro(true);
        e.setNumero_sequencia(12858);
        Assert.assertTrue(e.isEstouro(), "expired");
        e.setEstado(true);
        Assert.assertTrue(e.isEstado(), "acknowledged");
        Assert.assertEquals(12858, e.getNumero_sequencia(), "sequence");
    }

    @Test
    public void acknowledgedPacketIsNeverExpired() {
        // A packet that has its ACK must not be sent again, even if its timer fired.
        Estado e = new Estado();
        e.setEstouro(true);
        e.setEstado(true);
        Assert.assertFalse(e.isEstouro(), "acknowledged packet reported as expired");
        e.setEstado(false);
        Assert.assertTrue(e.isEstouro(), "packet without ACK must be expired");
    }

    @Test
    public void timerExpiresAfterTheTimeout() {
        Estado e = new Estado();
        e.ativa_timer();
        sleep(Utils.tempo_estouro_pacote / 2);
        Assert.assertFalse(e.isEstouro(), "expired too early");
        sleep(Utils.tempo_estouro_pacote + 300);
        Assert.assertTrue(e.isEstouro(), "not expired after the timeout");
        e.desativa_timer();
    }

    @Test
    public void stoppedTimerDoesNotExpire() {
        Estado e = new Estado();
        e.ativa_timer();
        e.desativa_timer();
        sleep(Utils.tempo_estouro_pacote * 2 + 200);
        Assert.assertFalse(e.isEstouro(), "expired after the stop");
    }

    @Test
    public void stoppingATimerThatNeverStartedIsSafe() {
        new Estado().desativa_timer();
    }
}
