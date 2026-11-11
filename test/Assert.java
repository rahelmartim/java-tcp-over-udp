import java.util.Arrays;
import java.util.Objects;

/** Small assertion helpers. A failed assertion throws an AssertionError. */
final class Assert {
    private Assert() {
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    static void assertBytes(byte[] expected, byte[] actual, String message) {
        if (!Arrays.equals(expected, actual)) {
            int length = Math.min(expected.length, actual.length);
            int first = 0;
            while (first < length && expected[first] == actual[first]) first++;
            throw new AssertionError(message + ": expected " + expected.length + " bytes, got " + actual.length
                    + ", first difference at byte " + first);
        }
    }

    static void assertContains(String text, String part, String message) {
        if (text == null || !text.contains(part)) {
            throw new AssertionError(message + ": <" + part + "> not found in output:\n" + text);
        }
    }

    static void assertBetween(double low, double high, double actual, String message) {
        if (actual < low || actual > high) {
            throw new AssertionError(message + ": " + actual + " is not between " + low + " and " + high);
        }
    }

    static <T extends Throwable> void assertThrows(Class<T> type, Runnable code, String message) {
        try {
            code.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError(message + ": expected " + type.getSimpleName() + " but got " + t);
        }
        throw new AssertionError(message + ": expected " + type.getSimpleName() + ", but nothing was thrown");
    }
}
