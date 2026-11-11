import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Random;

/** Tests for Utils.divide_em_pacotes, which splits a file into packets. */
public class SplitFileTest {

    private static byte[] join(List<byte[]> parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) out.write(part, 0, part.length);
        return out.toByteArray();
    }

    @Test
    public void partsJoinBackToTheFileForEverySizeUpTo3000Bytes() {
        Random random = new Random(1);
        for (int n = 0; n <= 3000; n++) {
            byte[] file = new byte[n];
            random.nextBytes(file);
            Assert.assertBytes(file, join(Utils.divide_em_pacotes(file)), "file of " + n + " bytes");
        }
    }

    @Test
    public void numberOfPartsIsTheSizeDividedBy512RoundedUp() {
        int[][] cases = {{1, 1}, {511, 1}, {512, 1}, {513, 2}, {1024, 2}, {1025, 3}, {32172, 63}, {102400, 200}};
        for (int[] c : cases) {
            Assert.assertEquals(c[1], Utils.divide_em_pacotes(new byte[c[0]]).size(), "parts for " + c[0] + " bytes");
        }
    }

    @Test
    public void everyPartExceptTheLastHas512Bytes() {
        List<byte[]> parts = Utils.divide_em_pacotes(new byte[5000]);
        for (int i = 0; i < parts.size() - 1; i++) Assert.assertEquals(512, parts.get(i).length, "part " + i);
        Assert.assertEquals(5000 - 9 * 512, parts.get(parts.size() - 1).length, "last part");
    }

    @Test
    public void lastPartIsNotPadded() {
        // Regression: the old client filled the last packet with spaces up to 512 bytes.
        List<byte[]> parts = Utils.divide_em_pacotes("hello".getBytes());
        Assert.assertEquals(1, parts.size(), "parts");
        Assert.assertBytes("hello".getBytes(), parts.get(0), "only part");
    }

    @Test
    public void exactMultipleOf512HasNoExtraPart() {
        // Regression: the old client added a packet of 512 spaces to such files.
        Assert.assertEquals(4, Utils.divide_em_pacotes(new byte[2048]).size(), "parts");
    }

    @Test
    public void emptyFileBecomesOneEmptyPart() {
        List<byte[]> parts = Utils.divide_em_pacotes(new byte[0]);
        Assert.assertEquals(1, parts.size(), "parts");
        Assert.assertEquals(0, parts.get(0).length, "part length");
    }

    @Test
    public void partsAreCopies() {
        byte[] file = {1, 2, 3};
        List<byte[]> parts = Utils.divide_em_pacotes(file);
        file[0] = 99;
        Assert.assertEquals(1, parts.get(0)[0], "first byte");
    }
}
