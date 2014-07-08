package in.twbs.pure.lang.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.WhitespacesAndCommentsBinder;
import com.intellij.psi.tree.IElementType;
import in.twbs.pure.lang.psi.PureTokens;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ParserContext {
    @NotNull
    private final PsiBuilder builder;
    private int column;
    private int indentationLevel;

    public boolean eof() {
        return builder.eof();
    }

    private final class PureMarker implements PsiBuilder.Marker {
        private final int start;
        private final PsiBuilder.Marker marker;

        protected PureMarker(@NotNull PsiBuilder.Marker marker) {
            this.start = column;
            this.marker = marker;
        }

        public PureMarker(int start, PsiBuilder.Marker marker) {
            this.start = start;
            this.marker = marker;
        }

        @Override
        public PsiBuilder.Marker precede() {
            return new PureMarker(start, marker);
        }

        @Override
        public void drop() {
            marker.drop();
        }

        @Override
        public void rollbackTo() {
            column = start;
            marker.rollbackTo();
        }

        @Override
        public void done(IElementType type) {
            marker.done(type);
        }

        @Override
        public void collapse(IElementType type) {
            marker.collapse(type);
        }

        @Override
        public void doneBefore(IElementType type, PsiBuilder.Marker before) {
            marker.doneBefore(type, before);
        }

        @Override
        public void doneBefore(IElementType type, PsiBuilder.Marker before, String errorMessage) {
            marker.doneBefore(type, before, errorMessage);
        }

        @Override
        public void error(String message) {
            marker.error(message);
        }

        @Override
        public void errorBefore(String message, PsiBuilder.Marker before) {
            marker.errorBefore(message, before);
        }

        @Override
        public void setCustomEdgeTokenBinders(@Nullable WhitespacesAndCommentsBinder left, @Nullable WhitespacesAndCommentsBinder right) {
            marker.setCustomEdgeTokenBinders(left, right);
        }
    }

    public ParserContext(@NotNull PsiBuilder builder) {
        this.builder = builder;
    }

    public void whiteSpace() {
        while (!builder.eof()) {
            IElementType type = builder.getTokenType();
            if (type == PureTokens.NL || type == PureTokens.SLCOMMENT) {
                column = 0;
            } else if (type == PureTokens.TAB) {
                column = (column + 1) >> 3 << 3;
            } else if (type == PureTokens.WS) {
                column += 1;
            } else if (type == PureTokens.MLCOMMENT) {
                String text = builder.getTokenText();
                if (text != null) {
                    for (int i = 0; i < text.length(); i++) {
                        char ch = builder.getOriginalText().charAt(i);
                        if (ch == '\n') {
                            column = 0;
                        } else if (ch == '\t') {
                            column = (column + 1) >> 3 << 3;
                        }
                    }
                }
            } else {
                break;
            }
            builder.advanceLexer();
        }
    }

    public void advance() {
        String text = builder.getTokenText();
        if (text != null) {
            if (builder.getTokenType() == PureTokens.STRING) {
                for (int i = 0; i < text.length(); i++) {
                    char ch = text.charAt(i);
                    if (ch == '\n') {
                        column = 0;
                    } else if (ch == '\t') {
                        column = (column + 1) >> 3 << 3;
                    }
                }
            } else {
                column += text.length();
            }
        }
        builder.advanceLexer();
    }

    @NotNull
    public String text() {
        String text = builder.getTokenText();
        if (text == null) return "";
        return text;
    }

    public boolean match(@NotNull IElementType type) {
        return builder.getTokenType() == type;
    }

    public boolean eat(@NotNull IElementType type) {
        if (builder.getTokenType() == type) {
            advance();
            return true;
        }
        return false;
    }

    public boolean expect(@NotNull IElementType type) {
        PsiBuilder.Marker mark = builder.mark();
        if (builder.getTokenType() == type) {
            advance();
            mark.drop();
            return true;
        }
        mark.error(String.format("Expecting %s.", type.toString()));
        return false;
    }

    @NotNull
    public PsiBuilder.Marker start() {
        // Consume all the white spaces.
        builder.eof();
        return new PureMarker(builder.mark());
    }

    public int getPosition() {
        return builder.getCurrentOffset();
    }

    public int getColumn() {
        return column;
    }

    public int getIndentationLevel() {
        return indentationLevel;
    }

    public void setIndentationLevel(int indentationLevel) {
        this.indentationLevel = indentationLevel;
    }
}
