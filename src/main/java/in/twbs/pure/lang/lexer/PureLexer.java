package in.twbs.pure.lang.lexer;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import in.twbs.pure.lang.psi.PureTokens;

import java.io.Reader;

public final class PureLexer extends LookAheadLexer {
    public PureLexer() {
        super(new MergedPureLexer(), 64);
    }

    private static final class MergedPureLexer extends MergingLexerAdapterBase {
        public MergedPureLexer() {
            super(new FlexAdapter(new _PureLexer((Reader) null)));
        }

        private static final MergeFunction mergeFunction = new MergeFunction() {
            @Override
            public IElementType merge(IElementType type, Lexer originalLexer) {
                if (type == PureTokens.STRING) {
                    while (true) {
                        final IElementType tokenType = originalLexer.getTokenType();
                        if (tokenType != PureTokens.STRING && tokenType != PureTokens.STRING_ESCAPED && tokenType != PureTokens.STRING_GAP)
                            break;
                        originalLexer.advance();
                    }
                } else if (type == PureTokens.MLCOMMENT || type == PureTokens.SLCOMMENT || type == PureTokens.WS) {
                    while (true) {
                        type = originalLexer.getTokenType();
                        if (type == PureTokens.MLCOMMENT || type == PureTokens.SLCOMMENT || type == PureTokens.WS) {
                            originalLexer.advance();
                        } else {
                            break;
                        }
                    }
                    type = PureTokens.WS;
                } else if (type == PureTokens.PIPE) {
                    type = PureTokens.OPERATOR;
                }
                return type;
            }
        };

        @Override
        public MergeFunction getMergeFunction() {
            return mergeFunction;
        }
    }
}
