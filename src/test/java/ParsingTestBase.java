import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.util.Processor;
import in.twbs.pure.lang.file.PureFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class ParsingTestBase extends PsiTestCase {

    public void testExamples() {
        FileUtil.processFilesRecursively(new File(getTestDataPath() + "/passing"), new Processor<File>() {
            @Override
            public boolean process(File file) {
                if (file.isFile()) {
                    try {
                        if (file.getName().endsWith(".purs")) {
                            testExample(file, true);
                        }
                    } catch (Exception e) {
                        assertTrue("Failed to read file " + file.getAbsolutePath(), false);
                    }
                }
                return true;
            }
        });
        FileUtil.processFilesRecursively(new File(getTestDataPath() + "/failing"), new Processor<File>() {
            @Override
            public boolean process(File file) {
                if (file.isFile()) {
                    try {
                        if (file.getName().endsWith(".purs")) {
                            testExample(file, false);
                        }
                    } catch (Exception e) {
                        assertTrue("Failed to read file " + file.getAbsolutePath(), false);
                    }
                }
                return true;
            }
        });
    }

    public static String readFile(File file) throws IOException {
        String content = new String(FileUtil.loadFileText(file.getCanonicalFile()));
        assertNotNull(content);
        return content;
    }

    protected String getTestDataPath() {
        return "src/test/resources/purescript/examples";
    }

    private void testExample(@NotNull File fileName, final boolean passing) throws Exception {
        PureFile file = (PureFile) createFile(fileName.getName(), readFile(fileName));

        String psiTree = DebugUtil.psiToString(file, false);
        FileUtil.writeToFile(new File(fileName.getAbsolutePath() + ".psi"), psiTree);

        if (passing) {
            assertEquals(fileName.getName() + " failed.", true, !psiTree.contains("PsiErrorElement"));
        }
    }
}
