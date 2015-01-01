package in.twbs.pure.lang.psi.cst;

import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.injected.StringLiteralEscaper;

public class PureStringLiteralEscaper extends StringLiteralEscaper<PsiLanguageInjectionHost> {
    public PureStringLiteralEscaper(PsiLanguageInjectionHost host) {
        super(host);
    }
}
