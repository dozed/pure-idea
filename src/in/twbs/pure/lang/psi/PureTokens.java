package in.twbs.pure.lang.psi;

import com.intellij.psi.tree.TokenSet;

public interface PureTokens {
    PureElementType ERROR = new PureElementType("error");
    PureElementType WS = new PureElementType("whitespace");
    PureElementType NL = new PureElementType("new line");
    PureElementType TAB = new PureElementType("tab");
    PureElementType MLCOMMENT = new PureElementType("block comment");
    PureElementType SLCOMMENT = new PureElementType("line comment");

    PureElementType DATA = new PureElementType("data");
    PureElementType TYPE = new PureElementType("type");
    PureElementType FOREIGN = new PureElementType("foreign");
    PureElementType IMPORT = new PureElementType("import");
    PureElementType INFIXL = new PureElementType("infixl");
    PureElementType INFIXR = new PureElementType("infixr");
    PureElementType INFIX = new PureElementType("infix");
    PureElementType CLASS = new PureElementType("class");
    PureElementType INSTANCE = new PureElementType("instance");
    PureElementType MODULE = new PureElementType("module");
    PureElementType CASE = new PureElementType("case");
    PureElementType OF = new PureElementType("of");
    PureElementType IF = new PureElementType("if");
    PureElementType THEN = new PureElementType("then");
    PureElementType ELSE = new PureElementType("else");
    PureElementType DO = new PureElementType("do");
    PureElementType LET = new PureElementType("let");
    PureElementType TRUE = new PureElementType("true");
    PureElementType FALSE = new PureElementType("false");
    PureElementType IN = new PureElementType("in");
    PureElementType WHERE = new PureElementType("where");
    PureElementType FORALL = new PureElementType("forall");
    PureElementType DARROW = new PureElementType("=>");
    PureElementType DERIVE = new PureElementType("<=");
    PureElementType ARROW = new PureElementType("->");
    PureElementType EQ = new PureElementType("=");
    PureElementType DOT = new PureElementType(".");
    PureElementType DDOT = new PureElementType("..");

    PureElementType SEMI = new PureElementType(";");
    PureElementType TICK = new PureElementType("`");
    PureElementType PIPE = new PureElementType("|");
    PureElementType COMMA = new PureElementType(",");
    PureElementType LPAREN = new PureElementType("(");
    PureElementType RPAREN = new PureElementType(")");
    PureElementType LBRACK = new PureElementType("[");
    PureElementType RBRACK = new PureElementType("]");
    PureElementType LCURLY = new PureElementType("{");
    PureElementType RCURLY = new PureElementType("}");

    PureElementType DBACKSLASH = new PureElementType("\\\\");
    PureElementType OPERATOR = new PureElementType("operator");
    PureElementType PROPER_NAME = new PureElementType("proper name");
    PureElementType MODULE_NAME = new PureElementType("proper name");
    PureElementType IDENT = new PureElementType("identifier");
    PureElementType STRING_ESCAPED = new PureElementType("string escaping");
    PureElementType STRING_GAP = new PureElementType("string escaping");
    PureElementType STRING_ERROR = new PureElementType("string escaping error");
    PureElementType STRING = new PureElementType("string");
    PureElementType NATURAL = new PureElementType("natural");


    TokenSet kWhiteSpaces = TokenSet.create(WS, NL, TAB, MLCOMMENT, SLCOMMENT);
    TokenSet kKeywords = TokenSet.create(DATA, TYPE, FOREIGN, IMPORT, INFIXL, INFIXR, INFIX, CLASS, INSTANCE, MODULE, CASE, OF, IF, THEN, ELSE, DO, LET, TRUE, FALSE, IN, WHERE, FORALL);
    TokenSet kComments = TokenSet.create(MLCOMMENT, SLCOMMENT);
    TokenSet kStrings = TokenSet.create(STRING);
    TokenSet kOperators = TokenSet.create(DARROW, ARROW, EQ, DOT, LPAREN, RPAREN, LBRACK, RBRACK, LCURLY, RCURLY);
}
