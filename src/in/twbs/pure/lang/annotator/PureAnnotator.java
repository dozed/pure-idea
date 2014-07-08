package in.twbs.pure.lang.annotator;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import in.twbs.pure.lang.highlight.PureSyntaxHighlighter;
import in.twbs.pure.lang.psi.PureElements;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class PureAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (psiElement(PureElements.ModuleName).accepts(element)) {
            Annotation ann = holder.createInfoAnnotation(element, element.getText());
            ann.setTextAttributes(PureSyntaxHighlighter.MODULE_NAME);
        }
    }
}
