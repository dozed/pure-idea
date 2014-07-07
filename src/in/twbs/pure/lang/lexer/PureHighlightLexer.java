package in.twbs.pure.lang.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.LookAheadLexer;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.psi.tree.TokenSet;
import in.twbs.pure.lang.psi.PureTokens;

import java.io.Reader;

public final class PureHighlightLexer extends LookAheadLexer {
    public PureHighlightLexer() {
        super(new MergingLexerAdapter(new FlexAdapter(new _PureLexer((Reader) null)), TokenSet.create(PureTokens.MLCOMMENT, PureTokens.WS, PureTokens.STRING)), 10);
    }
}
