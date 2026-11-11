/** Tests for Utils.proximo_cwnd: slow start and congestion avoidance. */
public class CongestionWindowTest {

    @Test
    public void slowStartAddsOnePacketPerAck() {
        Assert.assertEquals(1024, Utils.proximo_cwnd(512, 10000), "from 512");
        Assert.assertEquals(9728, Utils.proximo_cwnd(9216, 10000), "just below the threshold");
    }

    @Test
    public void slowStartDoublesTheWindowEveryRoundTrip() {
        int cwnd = 512;
        for (int round = 0; round < 4; round++) {
            int acks = cwnd / 512;
            for (int i = 0; i < acks; i++) cwnd = Utils.proximo_cwnd(cwnd, 1_000_000);
        }
        Assert.assertEquals(512 * 16, cwnd, "window after 4 round trips");
    }

    @Test
    public void congestionAvoidanceGrowsTheWindow() {
        // Regression: the old code set CWND = 512 * 512 / CWND, which shrank the window to one packet.
        int cwnd = 10240;
        int next = Utils.proximo_cwnd(cwnd, 10000);
        Assert.assertTrue(next > cwnd, "the window must grow, but went from " + cwnd + " to " + next);
        Assert.assertEquals(cwnd + 512 * 512 / cwnd, next, "increase");
    }

    @Test
    public void congestionAvoidanceAddsAboutOnePacketPerRoundTrip() {
        int start = 20480;
        int cwnd = start;
        for (int i = 0; i < start / 512; i++) cwnd = Utils.proximo_cwnd(cwnd, 10000);
        Assert.assertBetween(start + 400, start + 512, cwnd, "window after one round trip of ACKs");
    }

    @Test
    public void thresholdItselfUsesCongestionAvoidance() {
        Assert.assertEquals(10000 + 512 * 512 / 10000, Utils.proximo_cwnd(10000, 10000), "at the threshold");
    }

    @Test
    public void windowNeverShrinksOnAnAck() {
        for (int thresh : new int[] {512, 10000, 100000}) {
            int cwnd = 512;
            for (int i = 0; i < 5000; i++) {
                int next = Utils.proximo_cwnd(cwnd, thresh);
                Assert.assertTrue(next > cwnd, "window shrank from " + cwnd + " with threshold " + thresh);
                cwnd = next;
            }
        }
    }

    @Test
    public void hugeWindowsStillGrowByAtLeastOneByte() {
        Assert.assertEquals(1_000_001, Utils.proximo_cwnd(1_000_000, 10000), "huge window");
    }

    @Test
    public void defaultsMatchTheReadme() {
        Assert.assertEquals(512, Utils.CWND_inicial, "first window");
        Assert.assertEquals(10000, Utils.SS_THRESH_inicial, "first threshold");
    }
}
