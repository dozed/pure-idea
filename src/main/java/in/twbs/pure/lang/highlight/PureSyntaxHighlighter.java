package in.twbs.pure.lang.highlight;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.ui.JBColor;
import in.twbs.pure.lang.lexer.PureHighlightLexer;
import in.twbs.pure.lang.psi.PureElements;
import in.twbs.pure.lang.psi.PureTokens;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class PureSyntaxHighlighter extends SyntaxHighlighterBase {
    private static final Map<IElementType, TextAttributesKey> ATTRIBUTES = new HashMap<IElementType, TextAttributesKey>();

    public static final TextAttributesKey LINE_COMMENT = createKey("pure.LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);

    public static final TextAttributesKey BLOCK_COMMENT = createKey("pure.BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);

    public static final TextAttributesKey KEYWORD = createKey("pure.KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);

    public static final TextAttributesKey STRING = createKey("pure.STRING", DefaultLanguageHighlighterColors.STRING);

    private static final TextAttributes STRING_GAP_ATTR;

    static {
        STRING_GAP_ATTR = STRING.getDefaultAttributes().clone();
        STRING_GAP_ATTR.setForegroundColor(JBColor.GRAY);
    }

    public static final TextAttributesKey STRING_GAP = createTextAttributesKey("pure.STRING_GAP", STRING_GAP_ATTR);

    public static final TextAttributesKey NUMBER = createKey("pure.NUMBER", DefaultLanguageHighlighterColors.NUMBER);

    public static final TextAttributesKey BRACKET = createKey("pure.BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);

    public static final TextAttributesKey OPERATOR = createKey("pure.OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);

    public static final TextAttributesKey TYPE_NAME = createKey("pure.TYPE_NAME", CodeInsightColors.ANNOTATION_NAME_ATTRIBUTES);

    public static final TextAttributesKey VARIABLE = createKey("pure.VARIABLE", CodeInsightColors.INSTANCE_FIELD_ATTRIBUTES);

    public static final TextAttributesKey MODULE_NAME = createKey("pure.MODULE_NAME", CodeInsightColors.ANNOTATION_NAME_ATTRIBUTES);

    public static final TextAttributesKey METHOD_DECLARATION = createKey("pure.METHOD_DECLARATION", CodeInsightColors.METHOD_CALL_ATTRIBUTES);

    static {
        fillMap(ATTRIBUTES, TokenSet.create(PureTokens.SLCOMMENT), LINE_COMMENT);
        fillMap(ATTRIBUTES, TokenSet.create(PureTokens.MLCOMMENT), BLOCK_COMMENT);
        fillMap(ATTRIBUTES, PureTokens.kKeywords, KEYWORD);
        fillMap(ATTRIBUTES, TokenSet.create(PureTokens.NATURAL), NUMBER);
        fillMap(ATTRIBUTES, PureTokens.kStrings, STRING);
        fillMap(ATTRIBUTES, TokenSet.create(PureTokens.LPAREN, PureTokens.RPAREN), BRACKET);
        fillMap(ATTRIBUTES, TokenSet.create(PureTokens.LBRACK, PureTokens.RBRACK), BRACKET);
        fillMap(ATTRIBUTES, TokenSet.create(PureTokens.LCURLY, PureTokens.RCURLY), BRACKET);
        fillMap(ATTRIBUTES, PureTokens.kOperators, OPERATOR);
        fillMap(ATTRIBUTES, TokenSet.create(PureTokens.IDENT, PureTokens.OPERATOR), VARIABLE);
        fillMap(ATTRIBUTES, TokenSet.create(PureTokens.PROPER_NAME), METHOD_DECLARATION);
        fillMap(ATTRIBUTES, TokenSet.create(PureElements.pModuleName), TYPE_NAME);
        fillMap(ATTRIBUTES, TokenSet.create(PureTokens.STRING_ESCAPED), KEYWORD);
        fillMap(ATTRIBUTES, TokenSet.create(PureTokens.STRING_GAP), STRING_GAP);
        fillMap(ATTRIBUTES, TokenSet.create(PureTokens.STRING_ERROR), CodeInsightColors.ERRORS_ATTRIBUTES);
        fillMap(ATTRIBUTES, TokenSet.create(PureTokens.ERROR), CodeInsightColors.ERRORS_ATTRIBUTES);
    }

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new PureHighlightLexer();
    }

    @NotNull
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return pack(ATTRIBUTES.get(tokenType));
    }

    private static TextAttributesKey createKey(String externalName, TextAttributesKey fallbackAttrs) {
        return createTextAttributesKey(externalName, fallbackAttrs);
    }
}
