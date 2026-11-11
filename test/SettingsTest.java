/** Tests for the settings that come from Java system properties. */
public class SettingsTest {

    @Test
    public void integerUsesTheDefaultWhenThePropertyIsMissing() {
        Assert.assertEquals(42, Utils.inteiro("tcpow.test.missing", 42), "missing");
    }

    @Test
    public void integerReadsAValidProperty() {
        System.setProperty("tcpow.test.int", "250");
        Assert.assertEquals(250, Utils.inteiro("tcpow.test.int", 42), "valid");
    }

    @Test
    public void integerRejectsNegativeAndInvalidValues() {
        System.setProperty("tcpow.test.negative", "-5");
        System.setProperty("tcpow.test.text", "fast");
        Assert.assertEquals(42, Utils.inteiro("tcpow.test.negative", 42), "negative");
        Assert.assertEquals(42, Utils.inteiro("tcpow.test.text", 42), "text");
    }

    @Test
    public void probabilityReadsValuesFrom0To1() {
        String[][] cases = {{"0", "0"}, {"1", "1"}, {"0.25", "0.25"}};
        for (String[] c : cases) {
            System.setProperty("tcpow.test.p", c[0]);
            Assert.assertEquals(Float.parseFloat(c[1]), Utils.probabilidade("tcpow.test.p", 0.2f), "value " + c[0]);
        }
    }

    @Test
    public void probabilityRejectsValuesOutOfRange() {
        for (String value : new String[] {"-0.1", "1.5", "abc", ""}) {
            System.setProperty("tcpow.test.bad", value);
            Assert.assertEquals(0.2f, Utils.probabilidade("tcpow.test.bad", 0.2f), "value '" + value + "'");
        }
        Assert.assertEquals(0.2f, Utils.probabilidade("tcpow.test.absent", 0.2f), "missing");
    }

    @Test
    public void constantsComeFromTheCommandLine() {
        // run-tests.sh passes short timeouts to the test JVM.
        Assert.assertEquals(Utils.inteiro("tcpow.tempo.estouro", 1500), Utils.tempo_estouro_pacote, "packet timer");
        Assert.assertEquals(Utils.inteiro("tcpow.tempo.servidor", 10000), Utils.tempo_espera_servidor, "server timeout");
    }

    @Test
    public void packetSizesAreConsistent() {
        Assert.assertEquals(Utils.tamanho_cabecalho + Utils.tamanho_util_pacote, Utils.tamanho_pacote, "packet size");
    }
}
