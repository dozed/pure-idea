package in.twbs.pure.lang.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.LookAheadLexer;

import java.io.Reader;

public final class PureLexer extends LookAheadLexer {
    public PureLexer() {
        super(new FlexAdapter(new _PureLexer((Reader) null)), 10);
    }
}
