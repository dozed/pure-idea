package in.twbs.pure.lang.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import in.twbs.pure.lang.psi.PureElements;
import in.twbs.pure.lang.psi.PureTokens;
import org.jetbrains.annotations.NotNull;

import static in.twbs.pure.lang.parser.Combinators.*;

public class PureParser implements PsiParser, PureTokens, PureElements {
    @NotNull
    @Override
    public ASTNode parse(IElementType root, PsiBuilder builder) {
        builder.setDebugMode(true);
        ParserContext context = new ParserContext(builder);
        PsiBuilder.Marker mark = context.start();
        context.whiteSpace();
        ParserInfo info = parseModules.parse(context);
        while (!context.eof()) {
            if (!info.success && context.getPosition() == info.position) {
                builder.error(info.toString());
            }
            context.advance();
        }
        mark.done(root);
        return builder.getTreeBuilt();
    }

    private static final Parsec identifier = attempt(lexeme(IDENT));
    private static final Parsec moduleName = attempt(sepBy(lexeme(PROPER_NAME), token(DOT)));
    private static final Parsec stringLiteral = attempt(lexeme(STRING));
    private static final Parsec qualified = attempt(many(token(PROPER_NAME).then(DOT)).then(PROPER_NAME).as(Qualified));

    @NotNull
    private static Parsec positioned(@NotNull final Parsec p) {
        return p.as(PositionedDeclaration);
    }

    // Kinds.hs
    private static final ParsecRef parseKindRef = ref();
    private static final SymbolicParsec parseStar = lexeme(token("*")).as(Star);
    private static final SymbolicParsec parseBang = lexeme(token("!")).as(Bang);
    private static final Parsec parseKindAtom = indented(choice(parseStar, parseBang, parens(parseKindRef)));
    private static final SymbolicParsec parseKindPrefix = many(lexeme(token("#"))).then(parseKindAtom).as(RowKind);
    private static final SymbolicParsec parseKind = parseKindPrefix.then(optional(reserved(ARROW).then(parseKindRef))).as(FunKind);

    static {
        parseKindRef.setRef(parseKind);
    }

    // Types.hs
    private static final ParsecRef parsePolyTypeRef = ref();
    private static final ParsecRef parseTypeRef = ref();
    private static final ParsecRef parseForAllRef = ref();

    private static final Parsec parseFunction = parens(attempt(lexeme(ARROW)));
    private static final Parsec parseTypeVariable = token(IDENT).as(TypeVar);

    private static final Parsec parseTypeConstructor = qualified;
    private static final Parsec parseNameAndType = indented(seq(
                    attempt(lexeme(IDENT).or(lexeme(STRING))),
                    reserved(DCOLON))
    );
    private static final Parsec parseRowEnding = optional(lexeme(indented(token(PIPE))).then(indented(token(IDENT))).as(TypeVar));
    private static final Parsec parseRowNonEmpty
            = commaSep(parseNameAndType.then(parsePolyTypeRef).then(parseRowEnding).as(Row));
    private static final Parsec parseObject = braces(optional(attempt(parseRowNonEmpty)));
    private static final Parsec parseTypeAtom = indented(
            choice(
                    attempt(token("Number")),
                    attempt(token("String")),
                    attempt(token("Boolean")),
                    attempt(squares(EMPTY)),
                    attempt(squares(parseTypeRef)),
                    attempt(parseFunction),
                    attempt(parseObject),
                    attempt(parseTypeVariable),
                    attempt(parseTypeConstructor),
                    attempt(parseForAllRef),
                    attempt(parens(parseRowNonEmpty)),
                    attempt(parens(parsePolyTypeRef))
            )
    );

    private static final Parsec parseConstrainedType = attempt(
            parens(sepBy1(qualified.then(indented(parseTypeAtom)), token(COMMA)))
    ).then(indented(parseTypeRef));
    private static final SymbolicParsec parseForAll
            = attempt(reserved(FORALL))
            .then(indented(attempt(token(IDENT))))
            .then(indented(token(DOT)))
            .then(parseConstrainedType).as(ForAll);

    private static final Parsec parseIdent = choice(
            attempt(token(IDENT)),
            token(LPAREN).then(OPERATOR).then(RPAREN)
    );

    private static final SymbolicParsec parseType = parseTypeAtom.then(many(parseTypeAtom)).then(optional(attempt(token("=>")).then(parseTypeRef))).as(Type);

    static {
        parsePolyTypeRef.setRef(parseType);
        parseTypeRef.setRef(parseType);
        parseForAllRef.setRef(parseForAll);
    }

    // Declarations.hs
    private static final ParsecRef parseBinderNoParensRef = ref();
    private static final ParsecRef parseBinderRef = ref();
    private static final ParsecRef parseValueRef = ref();

    private static final SymbolicParsec parseGuard = indented(lexeme(PIPE)).then(indented(parseValueRef)).as(Guard);
    private static final SymbolicParsec parseDataDeclaration
            = reserved(DATA)
            .then(indented(lexeme(PROPER_NAME)))
            .then(many(indented(token(IDENT))).as(TypeArgs))
            .lexeme(EQ)
            .then(sepBy1(lexeme(PROPER_NAME).then(many(indented(parseTypeAtom))).as(Cons), lexeme(PIPE)))
            .as(DataDeclaration);
    private static final SymbolicParsec parseTypeDeclaration
            = attempt(parseIdent.lexeme(DCOLON))
            .then(parsePolyTypeRef)
            .as(TypeDeclaration);

    private static final SymbolicParsec parseTypeSynonymDeclaration
            = reserved(TYPE)
            .then(reserved(PROPER_NAME))
            .then(many(indented(lexeme(IDENT))))
            .then(indented(lexeme(EQ)).then(parsePolyTypeRef))
            .as(TypeSynonymDeclaration);

    private static final SymbolicParsec parseValueDeclaration
            = parseIdent
            .then(many(parseBinderNoParensRef))
            .then(optional(parseGuard))
            .then(lexeme(indented(token("=")).then(parseValueRef)))
            .as(ValueDeclaration);

    private static final Parsec parseDeps
            = parens(commaSep1(qualified.then(many(parseTypeAtom))))
            .then(indented(reserved(DARROW)));

    private static final Parsec parseExternDeclaration
            = reserved(FOREIGN)
            .then(indented(reserved(IMPORT)))
            .then(indented(
                    choice(
                            reserved(DATA)
                                    .then(indented(reserved(PROPER_NAME)))
                                    .then(lexeme(DCOLON))
                                    .then(parseKind)
                                    .as(ExternDataDeclaration),
                            reserved(INSTANCE)
                                    .then(parseIdent)
                                    .then(lexeme(indented(token(DCOLON))))
                                    .then(parseDeps)
                                    .then(qualified.as(pClassName))
                                    .then(many(indented(parseTypeAtom)))
                                    .as(ExternInstanceDeclaration),
                            attempt(parseIdent)
                                    .then(optional(stringLiteral.as(JSRaw)))
                                    .then(lexeme(indented(token(DCOLON))))
                                    .then(parsePolyTypeRef)
                                    .as(ExternDeclaration)
                    )
            ));
    private static final Parsec parseAssociativity = choice(
            reserved(INFIXL),
            reserved(INFIXR),
            reserved(INFIX)
    );
    private static final SymbolicParsec parseFixity = parseAssociativity.then(indented(token(NATURAL))).as(Fixity);
    private static final SymbolicParsec parseFixityDeclaration
            = parseFixity
            .then(indented(token(OPERATOR)))
            .as(FixityDeclaration);

    private static final SymbolicParsec parseDeclarationRef = choice(
            parseIdent.as(ValueRef),
            lexeme(PROPER_NAME).then(
                    optional(parens(choice(
                                    token(DDOT),
                                    commaSep1(lexeme(PROPER_NAME))
                            )
                    ))
            )
    ).as(PositionedDeclarationRef);

    private static final SymbolicParsec parseTypeClassDeclaration
            = reserved(CLASS)
            .then(optional(indented(parens(commaSep1(qualified.then(many(parseTypeAtom)))))).then(reserved(DERIVE)).as(pImplies))
            .then(indented(lexeme(PROPER_NAME).as(pClassName)))
            .then(many(indented(lexeme(IDENT))))
            .then(optional(attempt(
                    indented(reserved(WHERE)).then(
                            mark(many(same(positioned(parseTypeDeclaration))))
                    )
            )))
            .as(TypeClassDeclaration);

    private static final SymbolicParsec parseTypeInstanceDeclaration
            = reserved(INSTANCE)
            .then(parseIdent.then(lexeme(indented(token(DCOLON)))))
            .then(optional(
                    parens(commaSep1(qualified.then(many(parseTypeAtom))))
                            .then(indented(reserved(DARROW)))
            ))
            .then(indented(qualified).as(pClassName))
            .then(many(indented(parseTypeAtom)))
            .then(
                    attempt(reserved(WHERE)).then(
                            mark(many(same(positioned(parseValueDeclaration))))
                    )
            )
            .as(TypeInstanceDeclaration);

    private static final Parsec qualImport
            = reserved(token("qualifield"))
            .then(indented(moduleName))
            .then(optional(indented(parens(commaSep(parseDeclarationRef)))))
            .then(reserved(token("as")))
            .then(moduleName);

    private static final Parsec stdImport
            = moduleName
            .then(optional(indented(parens(commaSep1(parseDeclarationRef)))));
    private static final SymbolicParsec parseImportDeclaration
            = reserved(IMPORT)
            .then(indented(choice(qualImport, stdImport)))
            .as(ImportDeclaration);

    public static final Parsec parseDeclaration = positioned(choice(
            parseDataDeclaration,
            parseTypeDeclaration,
            parseTypeSynonymDeclaration,
            parseValueDeclaration,
            parseExternDeclaration,
            parseFixityDeclaration,
            parseImportDeclaration,
            parseTypeClassDeclaration,
            parseTypeInstanceDeclaration
    ));

    public static final Parsec parseLocalDeclaration = positioned(choice(
            parseTypeDeclaration,
            parseValueDeclaration
    ));

    public static final SymbolicParsec parseModule
            = reserved(MODULE)
            .then(indented(
                    moduleName.then(optional(parens(commaSep1(parseDeclarationRef))))
            ))
            .then(reserved(WHERE))
            .then(mark(many(same(parseDeclaration))))
            .as(Module);

    public static final Parsec parseModules = mark(many(same(parseModule)));

    // Literals
    private static final SymbolicParsec parseBooleanLiteral = reserved(token("true")).or(reserved(token("false"))).as(BooleanLiteral);
    private static final SymbolicParsec parseNumericLiteral = reserved(NATURAL).or(reserved(token("false"))).as(NumericLiteral);
    private static final SymbolicParsec parseStringLiteral = reserved(STRING).as(StringLiteral);
    private static final SymbolicParsec parseArrayLiteral = squares(commaSep(parseValueRef)).as(StringLiteral);
    private static final SymbolicParsec parseIdentifierAndValue = indented(identifier.or(stringLiteral)).then(indented(lexeme(token(":")))).then(indented(parseValueRef)).as(ObjectBinderField);
    private static final SymbolicParsec parseObjectLiteral = braces(commaSep(parseIdentifierAndValue)).as(ObjectLiteral);

    private static final Parsec parseAbs
            = reserved(BACKSLASH)
            .then(many1(indented(parseIdent.or(parseBinderNoParensRef).as(Abs))))
            .then(indented(reserved(ARROW)))
            .then(parseValueRef);
    private static final SymbolicParsec parseVar = attempt(many(token(PROPER_NAME).then(DOT)).then(IDENT)).as(Var);
    private static final SymbolicParsec parseConstructor = qualified.as(Constructor);
    private static final SymbolicParsec parseCaseAlternative
            = parseBinderRef
            .then(optional(parseGuard))
            .then(indented(reserved(ARROW).then(parseValueRef)))
            .as(CaseAlternative);
    private static final SymbolicParsec parseCase
            = reserved(CASE)
            .then(parseValueRef)
            .then(indented(reserved(OF)))
            .then(mark(many(same(mark(parseCaseAlternative)))))
            .as(Case);
    private static final SymbolicParsec parseIfThenElse
            = reserved(IF)
            .then(indented(parseValueRef))
            .then(indented(reserved(THEN)))
            .then(indented(parseValueRef))
            .then(indented(reserved(ELSE)))
            .then(indented(parseValueRef))
            .as(IfThenElse);
    private static final SymbolicParsec parseLet
            = reserved(LET)
            .then(indented(mark(many1(same(parseLocalDeclaration)))))
            .then(indented(reserved(IN)))
            .then(parseValueRef)
            .as(Let);

    private static final Parsec parseDoNotationLet
            = reserved(LET)
            .then(indented(mark(many1(same(parseLocalDeclaration)))))
            .as(DoNotationLet);
    private static final Parsec parseDoNotationBind
            = parseBinderRef
            .then(indented(reserved(token("<-"))).then(parseValueRef))
            .as(DoNotationBind);
    private static final Parsec parseDoNotationElement = choice(
            attempt(parseDoNotationBind),
            parseDoNotationLet,
            attempt(parseValueRef.as(DoNotationValue))
    );
    private static final Parsec parseDo
            = reserved(DO)
            .then(indented(mark(many(same(mark(parseDoNotationElement))))));


    private static final Parsec parseValueAtom = choice(
            attempt(parseNumericLiteral),
            attempt(parseStringLiteral),
            attempt(parseBooleanLiteral),
            parseArrayLiteral,
            attempt(parseObjectLiteral),
            parseAbs,
            attempt(parseConstructor),
            attempt(parseVar),
            parseCase,
            parseIfThenElse,
            parseDo,
            parseLet,
            parens(parseValueRef).as(Parens)
    );

    private static final Parsec parsePropertyUpdate
            = reserved(identifier.or(stringLiteral))
            .then(lexeme(indented(token(EQ))))
            .then(indented(parseValueRef));
    private static final Parsec parseAccessor // P.try $ Accessor <$> (C.indented *> C.dot *> P.notFollowedBy C.opLetter *> C.indented *> (C.identifier <|> C.stringLiteral)) <*> pure obj
            = attempt(indented(token(DOT)).then(indented(identifier.or(stringLiteral)))).as(Accessor);

    private static final Parsec parseIdentInfix = token(TICK).then(attempt(many(token(PROPER_NAME).then(DOT))).then(identifier.or(token(OPERATOR)))).then(TICK);

    private static final Parsec parsePostfix
            = parseValueAtom
            .then(many(choice(
                    parseAccessor,
                    attempt(indented(braces(commaSep1(indented(parsePropertyUpdate))))),
                    indented(reserved(DCOLON).then(parseType))
            )));
    private static final Parsec parsePrefix
            = many(reserved(token("-"))).then(parsePostfix);

    private static final SymbolicParsec parseValue = parseValueAtom.then(attempt(indented(parseIdentInfix)).then(parseValueRef)).as(Value);

    // Binders
    private static final SymbolicParsec parseNullBinder = lexeme(token("_")).as(NullBinder);
    private static final SymbolicParsec parseStringBinder = lexeme(STRING).as(StringBinder);
    private static final SymbolicParsec parseBooleanBinder = lexeme(token("true")).or(lexeme(token("false"))).as(BooleanBinder);
    private static final SymbolicParsec parseNumberBinder = lexeme(NATURAL).or(lexeme(FLOAT)).as(NumberBinder);
    private static final SymbolicParsec parseNamedBinder = parseIdent.then(lexeme(indented(token("@").then(indented(parseBinderRef))))).as(NamedBinder);
    private static final SymbolicParsec parseVarBinder = parseIdent.as(VarBinder);
    private static final SymbolicParsec parseConstructorBinder = lexeme(qualified.then(many(indented(parseBinderNoParensRef)))).as(ConstructorBinder);
    private static final SymbolicParsec parseNullaryConstructorBinder = lexeme(qualified).as(ConstructorBinder);
    private static final SymbolicParsec parseObjectBinder = braces(sepBy(parseIdentifierAndValue, token(COMMA))).as(ObjectBinder);
    private static final SymbolicParsec parseArrayBinder = squares(sepBy(parseBinderRef, token(COMMA))).as(ObjectBinder);
    private static final SymbolicParsec parseBinder = choice(
            attempt(parseNullBinder),
            attempt(parseStringBinder),
            attempt(parseBooleanBinder),
            attempt(parseNumberBinder),
            attempt(parseNamedBinder),
            attempt(parseVarBinder),
            attempt(parseConstructorBinder),
            attempt(parseObjectBinder),
            attempt(parseArrayBinder),
            attempt(parens(parseBinderRef))
    ).as(Binder);
    private static final SymbolicParsec parseBinderNoParens = choice(
            attempt(parseNullBinder),
            attempt(parseStringBinder),
            attempt(parseBooleanBinder),
            attempt(parseNumberBinder),
            attempt(parseNamedBinder),
            attempt(parseVarBinder),
            attempt(parseNullaryConstructorBinder),
            attempt(parseObjectBinder),
            attempt(parseArrayBinder),
            attempt(parens(parseBinderRef))
    ).as(Binder);

    static {
        parseValueRef.setRef(parseValue);
        parseBinderRef.setRef(parseBinder);
        parseBinderNoParensRef.setRef(parseBinderNoParens);
    }
}
