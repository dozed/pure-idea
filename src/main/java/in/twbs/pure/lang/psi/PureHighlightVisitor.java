package in.twbs.pure.lang.psi;

import com.intellij.codeInsight.daemon.impl.HighlightVisitor;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import in.twbs.pure.lang.file.PureFile;
import org.jetbrains.annotations.NotNull;

public class PureHighlightVisitor implements HighlightVisitor {
    @Override
    public boolean suitableForFile(@NotNull PsiFile file) {
        return file instanceof PureFile;
    }

    @Override
    public void visit(@NotNull PsiElement element) {

    }

    @Override
    public boolean analyze(@NotNull PsiFile file, boolean updateWholeFile, @NotNull HighlightInfoHolder holder, @NotNull Runnable action) {
        return true;
    }

    @NotNull
    @Override
    public HighlightVisitor clone() {
        return new PureHighlightVisitor();
    }

    @Override
    public int order() {
        return 0;
    }
}
