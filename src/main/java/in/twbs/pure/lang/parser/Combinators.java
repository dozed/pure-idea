package in.twbs.pure.lang.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import in.twbs.pure.lang.psi.PureTokens;
import org.jetbrains.annotations.NotNull;

public class Combinators {
    @NotNull
    static Parsec token(@NotNull final IElementType tokenType) {
        return new Parsec() {
            @NotNull
            @Override
            public ParserInfo parse(@NotNull ParserContext context) {
                if (context.eat(tokenType)) {
                    return new ParserInfo(context.getPosition(), this, true);
                }
                return new ParserInfo(context.getPosition(), this, false);
            }

            @NotNull
            @Override
            public String getName() {
                return tokenType.toString();
            }
        };
    }

    @NotNull
    static Parsec token(@NotNull final String token) {
        return new Parsec() {
            @NotNull
            @Override
            public ParserInfo parse(@NotNull ParserContext context) {
                if (context.text().equals(token)) {
                    context.advance();
                    return new ParserInfo(context.getPosition(), this, true);
                }
                return new ParserInfo(context.getPosition(), this, false);
            }

            @NotNull
            @Override
            public String getName() {
                return "\"" + token + "\"";
            }
        };
    }

    @NotNull
    static Parsec lexeme(@NotNull final String text) {
        return lexeme(token(text));
    }

    @NotNull
    static Parsec lexeme(@NotNull final Parsec p) {
        return new Parsec() {
            @NotNull
            @Override
            public ParserInfo parse(@NotNull ParserContext context) {
                ParserInfo info = p.parse(context);
                if (info.success) {
                    context.whiteSpace();
                }
                return info;
            }

            @NotNull
            @Override
            public String getName() {
                return p.getName() + " ws*";
            }
        };
    }

    @NotNull
    static Parsec lexeme(@NotNull final IElementType type) {
        return lexeme(token(type));
    }

    @NotNull
    static Parsec reserved(@NotNull final Parsec p) {
        return attempt(lexeme(p));
    }

    static Parsec reserved(@NotNull final String content) {
        return attempt(lexeme(content));
    }

    @NotNull
    static Parsec reserved(@NotNull final IElementType tokenType) {
        return attempt(lexeme(token(tokenType)));
    }

    @NotNull
    static Parsec seq(@NotNull final Parsec p1, @NotNull final Parsec p2) {
        return new Parsec() {
            @NotNull
            @Override
            public ParserInfo parse(@NotNull ParserContext context) {
                ParserInfo info = p1.parse(context);
                if (info.success) {
                    ParserInfo info2 = p2.parse(context);
                    return ParserInfo.merge(info, info2, info2.success);
                }
                return info;
            }

            @NotNull
            @Override
            public String getName() {
                String name1 = p1.getName();
                String name2 = p2.getName();
                return name1 + " " + name2;
            }
        };
    }

    @NotNull
    static Parsec choice(@NotNull final Parsec head, @NotNull final Parsec... tail) {
        return new Parsec() {
            @NotNull
            @Override
            public ParserInfo parse(@NotNull ParserContext context) {
                int position = context.getPosition();
                ParserInfo info = head.parse(context);
                if (context.getPosition() > position || info.success) {
                    return info;
                }
                for (Parsec p2 : tail) {
                    ParserInfo info2 = p2.parse(context);
                    info = ParserInfo.merge(info, info2, info2.success);
                    if (context.getPosition() > position || info.success) {
                        return info;
                    }
                }
                return info;
            }

            @NotNull
            @Override
            public String getName() {
                // TODO: avoid unnecessary parentheses.
                StringBuilder sb = new StringBuilder();
                sb.append("(").append(head.getName()).append(")");
                for (Parsec parsec : tail) {
                    sb.append(" | (").append(parsec.getName()).append(")");
                }
                return sb.toString();
            }
        };
    }

    @NotNull
    static Parsec many(@NotNull final Parsec p) {
        return new Parsec() {
            @NotNull
            @Override
            public ParserInfo parse(@NotNull ParserContext context) {
                ParserInfo info = new ParserInfo(context.getPosition(), p, true);
                while (!context.eof()) {
                    int position = context.getPosition();
                    info = p.parse(context);
                    if (info.success) {
                        if (position == context.getPosition()) {
                            // TODO: this should not be allowed.
                            return new ParserInfo(info.position, info.expected, false);
                        }
                    } else if (position == context.getPosition()) {
                        return new ParserInfo(info.position, info.expected, true);
                    } else {
                        return info;
                    }
                }
                return info;
            }

            @NotNull
            @Override
            public String getName() {
                return "(" + p.getName() + ")*";
            }
        };
    }

    @NotNull
    static Parsec many1(@NotNull final Parsec p) {
        return p.then(many(p));
    }

    @NotNull
    static Parsec optional(@NotNull final Parsec p) {
        return new Parsec() {
            @NotNull
            @Override
            public ParserInfo parse(@NotNull ParserContext context) {
                int position = context.getPosition();
                ParserInfo info1 = p.parse(context);
                if (info1.success) {
                    return info1;
                }
                return new ParserInfo(info1.position, info1.expected, context.getPosition() == position);
            }

            @NotNull
            @Override
            public String getName() {
                // TODO: avoid unnecessary parentheses.
                return "(" + p.getName() + ")?";
            }
        };
    }

    @NotNull
    static Parsec attempt(@NotNull final Parsec p) {
        return new Parsec() {
            @NotNull
            @Override
            public ParserInfo parse(@NotNull ParserContext context) {
                PsiBuilder.Marker pack = context.start();
                ParserInfo info1 = p.parse(context);
                if (info1.success) {
                    pack.drop();
                    return info1;
                }
                pack.rollbackTo();
                return info1;
            }

            @NotNull
            @Override
            public String getName() {
                // TODO: avoid unnecessary parentheses.
                return "try(" + p.getName() + ")";
            }
        };
    }

    @NotNull
    public static Parsec parens(@NotNull Parsec p) {
        return lexeme(PureTokens.LPAREN).then(indented(p)).then(indented(lexeme(PureTokens.RPAREN)));
    }

    @NotNull
    public static Parsec squares(@NotNull Parsec p) {
        return lexeme(PureTokens.LBRACK).then(indented(p)).then(indented(lexeme(PureTokens.RBRACK)));
    }

    @NotNull
    public static Parsec braces(@NotNull Parsec p) {
        return lexeme(PureTokens.LCURLY).then(indented(p)).then(indented(lexeme(PureTokens.RCURLY)));
    }

    @NotNull
    public static Parsec indented(@NotNull final Parsec p) {
        return new Parsec() {
            @NotNull
            @Override
            public ParserInfo parse(@NotNull ParserContext context) {
                if (context.getColumn() > context.getIndentationLevel()) {
                    return p.parse(context);
                }
                return new ParserInfo(context.getPosition(), this, false);
            }

            @NotNull
            @Override
            public String getName() {
                return "indented (" + p.getName() + ")";
            }
        };
    }

    @NotNull
    public static Parsec same(@NotNull final Parsec p) {
        return new Parsec() {
            @NotNull
            @Override
            public ParserInfo parse(@NotNull ParserContext context) {
                if (context.getColumn() == context.getIndentationLevel()) {
                    return p.parse(context);
                }
                return new ParserInfo(context.getPosition(), this, false);
            }

            @NotNull
            @Override
            public String getName() {
                return "not indented (" + p.getName() + ")";
            }
        };
    }

    @NotNull
    public static Parsec mark(@NotNull final Parsec p) {
        return new Parsec() {
            @NotNull
            @Override
            public ParserInfo parse(@NotNull ParserContext context) {
                context.pushIndentationLevel();
                try {
                    return p.parse(context);
                } finally {
                    context.popIndentationLevel();
                }
            }

            @NotNull
            @Override
            public String getName() {
                return "not indented (" + p.getName() + ")";
            }
        };
    }

    @NotNull
    static Parsec sepBy1(@NotNull final Parsec p, @NotNull final Parsec sep) {
        return p.then(attempt(many(sep.then(p))));
    }

    @NotNull
    static Parsec commaSep1(@NotNull final Parsec p) {
        return sepBy1(p, lexeme(PureTokens.COMMA));
    }

    @NotNull
    static Parsec sepBy(@NotNull final Parsec p, @NotNull final Parsec sep) {
        return optional(p.then(many(sep.then(p))));
    }

    @NotNull
    static Parsec commaSep(@NotNull final Parsec p) {
        return sepBy(p, lexeme(PureTokens.COMMA));
    }

    @NotNull
    static ParsecRef ref() {
        return new ParsecRef();
    }

    @NotNull
    static Parsec until(@NotNull final Parsec p, @NotNull final IElementType type, final boolean consume) {
        return new Parsec() {
            @NotNull
            @Override
            public ParserInfo parse(@NotNull ParserContext context) {
                ParserInfo info = p.parse(context);
                if (info.success) {
                    return info;
                }
                PsiBuilder.Marker start = context.start();
                while (!context.eof()) {
                    if (context.match(type)) {
                        break;
                    }
                    context.advance();
                }
                if (consume && !context.eof()) {
                    context.advance();
                }
                start.error(info.toString());
                return new ParserInfo(context.getPosition(), this, true);
            }

            @NotNull
            @Override
            public String getName() {
                return p.getName();
            }
        };
    }

    @NotNull
    static Parsec untilSame(@NotNull final Parsec p) {
        return p;
//        return new Parsec() {
//            @NotNull
//            @Override
//            public ParserInfo parse(@NotNull ParserContext context) {
//                int position = context.getPosition();
//                ParserInfo info = p.parse(context);
//                if (info.success || position == context.getPosition()) {
//                    return info;
//                }
//                context.whiteSpace();
//                if (context.getColumn() <= context.getLastIndentationLevel()) {
//                    return info;
//                }
//                PsiBuilder.Marker start = context.start();
//                while (!context.eof()) {
//                    if (context.getColumn() == context.getIndentationLevel()) {
//                        break;
//                    }
//                    context.advance();
//                }
//                start.error(info.toString());
//                return new ParserInfo(context.getPosition(), this, true);
//            }
//
//            @NotNull
//            @Override
//            public String getName() {
//                return p.getName();
//            }
//        };
    }
}
