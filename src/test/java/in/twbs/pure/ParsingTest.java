package in.twbs.pure;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.util.Processor;
import in.twbs.pure.lang.file.PureFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class ParsingTest extends PsiTestCase {

    private Processor<File> processor(final boolean passing) {
        return new Processor<File>() {
            @Override
            public boolean process(File file) {
                if (file.isFile()) {
                    try {
                        if (file.getName().endsWith(".purs")) {
                            testExample(file, passing);
                        }
                    } catch (Exception e) {
                        assertTrue("Failed to read file " + file.getAbsolutePath(), false);
                    }
                }
                return true;
            }
        };
    }

    public void testExamples() {
        String testDataPath = "src/test/resources/purescript_examples";
        FileUtil.processFilesRecursively(new File(testDataPath + "/passing"), processor(true));
        FileUtil.processFilesRecursively(new File(testDataPath + "/manual/passing"), processor(true));
        FileUtil.processFilesRecursively(new File(testDataPath + "/failing"), processor(false));
        FileUtil.processFilesRecursively(new File(testDataPath + "/manual/failing"), processor(false));

        String additionalTests = "src/test/resources/additional";
        FileUtil.processFilesRecursively(new File(additionalTests + "/passing"), processor(true));
    }

    public static String readFile(File file) throws IOException {
        String content = new String(FileUtil.loadFileText(file.getCanonicalFile()));
        assertNotNull(content);
        return content;
    }

    private void testExample(@NotNull File fileName, final boolean passing) throws Exception {
        PureFile file = (PureFile) createFile(fileName.getName(), readFile(fileName));

        String psiTree = DebugUtil.psiToString(file, false);
        File expectedFile = new File(fileName.getAbsolutePath() + ".psi");
        if (expectedFile.isFile()) {
            String expectedTree = FileUtil.loadFile(expectedFile);
            assertEquals(fileName.getName() + " failed.", expectedTree, psiTree);
        } else {
            assert false;  // Only manually.
            FileUtil.writeToFile(new File(fileName.getAbsolutePath() + ".psi"), psiTree);
        }

        if (passing) {
            assertEquals(fileName.getName() + " failed.", true, !psiTree.contains("PsiErrorElement"));
        } else {
            // TODO: type checker.
            // assertEquals(fileName.getName() + " failed.", true, psiTree.contains("PsiErrorElement"));
        }
    }
}
