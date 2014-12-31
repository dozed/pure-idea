package in.twbs.pure.lang.psi.cst;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.tree.IElementType;
import in.twbs.pure.lang.psi.PureElements;
import org.jetbrains.annotations.NotNull;

public class PureASTWrapperElement extends ASTWrapperPsiElement implements PsiLanguageInjectionHost {
    public PureASTWrapperElement(ASTNode astNode) {
        super(astNode);
    }

    public boolean isString() {
        final IElementType type = getNode().getElementType();
        return type.equals(PureElements.JSRaw) || type.equals(PureElements.StringLiteral);
    }

    public boolean isBlockString() {
        return getStringText().startsWith("\"\"\"");
    }

    public boolean isMultilineString() {
        return getStringText().indexOf('\n') != -1;
    }

    /**
     * Returns the text of a string element, including its quotes.
     */
    @NotNull
    public String getStringText() {
        return isString() ? getNode().getFirstChildNode().getText() : "";
    }

    @Override
    public boolean isValidHost() {
        // Only supports block-strings or single-line strings.
        return isString() && (isBlockString() || !isMultilineString());
    }

    @Override
    public PureASTWrapperElement updateText(@NotNull String s) {
        final ASTNode valueNode = getNode().getFirstChildNode();
        assert valueNode instanceof LeafElement;
        ((LeafElement)valueNode).replaceWithText(s);
        return this;
    }

    @NotNull
    @Override
    public PureStringLiteralEscaper createLiteralTextEscaper() {
        return new PureStringLiteralEscaper(this);
    }
}
