import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Runs the tests without external libraries.
 *
 * Usage: java -cp out TestRunner [filter]
 * The optional filter runs only the tests whose "Class.method" name contains it.
 */
public final class TestRunner {
    private static final Class<?>[] TEST_CLASSES = {
            PacketCodecTest.class,
            PacoteTest.class,
            SplitFileTest.class,
            CongestionWindowTest.class,
            SequenceNumberTest.class,
            SettingsTest.class,
            LossSimulationTest.class,
            DirectoryNameTest.class,
            MessagesTest.class,
            EstadoTest.class,
            ConexaoTest.class,
            InputValidationTest.class,
            EndToEndTest.class,
    };

    public static void main(String[] args) {
        String filter = args.length > 0 ? args[0] : "";
        List<String> failures = new ArrayList<>();
        int passed = 0;
        long start = System.nanoTime();

        for (Class<?> type : TEST_CLASSES) {
            Method[] methods = type.getDeclaredMethods();
            Arrays.sort(methods, Comparator.comparing(Method::getName));
            for (Method method : methods) {
                if (!method.isAnnotationPresent(Test.class)) continue;
                String name = type.getSimpleName() + "." + method.getName();
                if (!name.contains(filter)) continue;
                long testStart = System.nanoTime();
                try {
                    Object instance = type.getDeclaredConstructor().newInstance();
                    method.setAccessible(true);
                    method.invoke(instance);
                    passed++;
                    System.out.printf("  PASS  %s (%d ms)%n", name, (System.nanoTime() - testStart) / 1_000_000);
                } catch (InvocationTargetException e) {
                    failures.add(name + "\n        " + e.getCause());
                    System.out.printf("  FAIL  %s%n        %s%n", name, e.getCause());
                } catch (ReflectiveOperationException e) {
                    failures.add(name + "\n        " + e);
                    System.out.printf("  FAIL  %s%n        %s%n", name, e);
                }
            }
        }

        long seconds = (System.nanoTime() - start) / 1_000_000_000;
        System.out.printf("%n%d passed, %d failed, in %d s%n", passed, failures.size(), seconds);
        if (!failures.isEmpty()) {
            System.out.println("\nFailures:");
            for (String failure : failures) System.out.println("  " + failure);
        }
        // The code under test starts timer threads that never end, so exit explicitly.
        System.exit(failures.isEmpty() && passed > 0 ? 0 : 1);
    }
}
