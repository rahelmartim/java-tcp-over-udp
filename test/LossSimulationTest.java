/** Tests for Utils.vai_falhar, the simulated packet loss. */
public class LossSimulationTest {

    private static double rate(float p, int samples) {
        int lost = 0;
        for (int i = 0; i < samples; i++) if (Utils.vai_falhar(p)) lost++;
        return (double) lost / samples;
    }

    @Test
    public void zeroNeverLoses() {
        Assert.assertEquals(0.0, rate(0f, 20000), "rate");
    }

    @Test
    public void oneAlwaysLoses() {
        Assert.assertEquals(1.0, rate(1f, 20000), "rate");
    }

    @Test
    public void defaultClientRateIsAbout20Percent() {
        Assert.assertBetween(0.18, 0.22, rate(0.2f, 50000), "rate");
    }

    @Test
    public void halfLosesAboutHalf() {
        Assert.assertBetween(0.48, 0.52, rate(0.5f, 50000), "rate");
    }

    @Test
    public void safeFromManyThreads() throws Exception {
        Thread[] threads = new Thread[8];
        int[] lost = new int[threads.length];
        for (int t = 0; t < threads.length; t++) {
            int index = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 10000; i++) if (Utils.vai_falhar(0.3f)) lost[index]++;
            });
            threads[t].start();
        }
        int total = 0;
        for (int t = 0; t < threads.length; t++) {
            threads[t].join();
            total += lost[t];
        }
        Assert.assertBetween(0.28, 0.32, total / 80000.0, "rate");
    }
}
