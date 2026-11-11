/** Tests for Utils.diretorio_valido, which checks the server output folder name. */
public class DirectoryNameTest {

    @Test
    public void acceptsSimpleNames() {
        for (String name : new String[] {"recebidos", "saida_1", "files-2017", "a b", "ção"}) {
            Assert.assertTrue(Utils.diretorio_valido(name), name);
        }
    }

    @Test
    public void rejectsEveryForbiddenCharacter() {
        for (String c : new String[] {"/", "\\", "|", "*", "?", "\"", "<", ">", ":"}) {
            Assert.assertFalse(Utils.diretorio_valido("bad" + c + "name"), "name with " + c);
            Assert.assertFalse(Utils.diretorio_valido(c), "only " + c);
        }
    }

    @Test
    public void rejectsPaths() {
        Assert.assertFalse(Utils.diretorio_valido("../etc"), "parent path");
        Assert.assertFalse(Utils.diretorio_valido("/tmp/out"), "absolute path");
        Assert.assertFalse(Utils.diretorio_valido("C:\\out"), "Windows path");
    }
}
