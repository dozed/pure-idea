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
        ParserInfo info = new PureParsecParser().parseModules.parse(context);
        if (!context.eof()) {
            PsiBuilder.Marker errorMarker = null;
            while (!context.eof()) {
                if (context.getPosition() >= info.position && errorMarker == null) {
                    errorMarker = context.start();
                }
                context.advance();
            }
            if (errorMarker != null) {
                errorMarker.error(info.toString());
            }
        }
        mark.done(root);
        return builder.getTreeBuilt();
    }

    private final static class PureParsecParser {
        @NotNull
        private Parsec parseQualified(@NotNull Parsec p) {
            return attempt(many(attempt(token(PROPER_NAME).then(DOT))).then(p).as(Qualified));
        }

        private final Parsec identifier = attempt(lexeme(IDENT));
        private final Parsec properName = attempt(lexeme(PROPER_NAME));
        private final Parsec moduleName = parseQualified(properName).as(ModuleName);

        private final Parsec stringLiteral = attempt(lexeme(STRING));

        @NotNull
        private Parsec positioned(@NotNull final Parsec p) {
            return p.as(PositionedDeclaration);
        }

        @NotNull
        private Parsec indentedList(@NotNull final Parsec p) {
            return mark(many(untilSame(same(p))));
        }

        @NotNull
        private Parsec indentedList1(@NotNull final Parsec p) {
            return mark(many1(untilSame(same(p))));
        }

        // Kinds.hs
        private final ParsecRef parseKindRef = ref();
        private final SymbolicParsec parseStar = lexeme(token("*")).as(Star);
        private final SymbolicParsec parseBang = lexeme(token("!")).as(Bang);
        private final Parsec parseKindAtom = indented(choice(parseStar, parseBang, parens(parseKindRef)));
        private final SymbolicParsec parseKindPrefix = many(lexeme(token("#"))).then(parseKindAtom).as(RowKind);
        private final SymbolicParsec parseKind = parseKindPrefix.then(optional(reserved(ARROW).then(parseKindRef))).as(FunKind);

        {
            parseKindRef.setRef(parseKind);
        }

        // Types.hs
        private final ParsecRef parsePolyTypeRef = ref();
        private final ParsecRef parseTypeRef = ref();
        private final ParsecRef parseForAllRef = ref();

        private final Parsec parseFunction = parens(attempt(lexeme(ARROW)));
        private final Parsec parseTypeVariable = token(IDENT).as(TypeVar);

        private final Parsec parseTypeConstructor = parseQualified(properName).as(TypeConstructor);
        private final Parsec parseNameAndType = indented(seq(
                        attempt(lexeme(IDENT).or(lexeme(STRING))),
                        reserved(DCOLON))
        );
        private final Parsec parseRowEnding = optional(lexeme(indented(token(PIPE))).then(indented(token(IDENT))).as(TypeVar));
        private final Parsec parseRowNonEmpty
                = commaSep(parseNameAndType.then(parsePolyTypeRef).then(parseRowEnding).as(Row));
        private final Parsec parseObject = braces(optional(attempt(parseRowNonEmpty)));
        private final Parsec parseTypeAtom = indented(
                choice(
                        attempt(lexeme(token("Number"))),
                        attempt(lexeme(token("String"))),
                        attempt(lexeme(token("Boolean"))),
                        attempt(squares(optional(parseTypeRef))),
                        attempt(parseFunction),
                        attempt(parseObject),
                        attempt(parseTypeVariable),
                        attempt(parseTypeConstructor),
                        attempt(parseForAllRef),
                        attempt(parens(parseRowNonEmpty.or(parsePolyTypeRef))))
        ).as(TypeAtom);

        private final Parsec parseConstrainedType = attempt(
                parens(commaSep1(parseQualified(properName).then(indented(parseTypeAtom))))
        ).then(indented(parseTypeRef));
        private final SymbolicParsec parseForAll
                = attempt(reserved(FORALL))
                .then(indented(attempt(lexeme(IDENT))))
                .then(indented(lexeme(DOT)))
                .then(parseConstrainedType).as(ForAll);

        private final Parsec parseIdent = choice(
                attempt(lexeme(IDENT)),
                attempt(parens(lexeme(OPERATOR)))
        );

        private final SymbolicParsec parseType = many1(parseTypeAtom).as(TypeApp).then(optional(attempt(lexeme(ARROW)).then(parseTypeRef))).as(Type);

        {
            parsePolyTypeRef.setRef(parseType);
            parseTypeRef.setRef(parseType);
            parseForAllRef.setRef(parseForAll);
        }

        // Declarations.hs
        private final ParsecRef parseBinderNoParensRef = ref();
        private final ParsecRef parseBinderRef = ref();
        private final ParsecRef parseValueRef = ref();

        private final SymbolicParsec parseGuard = indented(lexeme(PIPE)).then(indented(parseValueRef)).as(Guard);
        private final SymbolicParsec parseDataDeclaration
                = reserved(DATA)
                .then(indented(properName))
                .then(many(indented(identifier)).as(TypeArgs))
                .then(lexeme(EQ))
                .then(sepBy1(properName.then(many(indented(parseTypeAtom))).as(Cons), lexeme(PIPE)))
                .as(DataDeclaration);
        private final SymbolicParsec parseTypeDeclaration
                = attempt(parseIdent.then(indented(lexeme(DCOLON))))
                .then(parsePolyTypeRef)
                .as(TypeDeclaration);

        private final SymbolicParsec parseTypeSynonymDeclaration
                = reserved(TYPE)
                .then(reserved(PROPER_NAME))
                .then(many(indented(lexeme(IDENT))))
                .then(indented(lexeme(EQ)).then(parsePolyTypeRef))
                .as(TypeSynonymDeclaration);

        private final SymbolicParsec parseValueDeclaration
                = parseIdent
                .then(many(parseBinderNoParensRef))
                .then(optional(parseGuard))
                .then(lexeme(indented(lexeme(EQ))))
                .then(parseValueRef)
                .as(ValueDeclaration);

        private final Parsec parseDeps
                = parens(commaSep1(parseQualified(properName).then(many(parseTypeAtom))))
                .then(indented(reserved(DARROW)));

        private final Parsec parseExternDeclaration
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
                                        .then(parseQualified(properName).as(pClassName))
                                        .then(many(indented(parseTypeAtom)))
                                        .as(ExternInstanceDeclaration),
                                attempt(parseIdent)
                                        .then(optional(stringLiteral.as(JSRaw)))
                                        .then(lexeme(indented(token(DCOLON))))
                                        .then(parsePolyTypeRef)
                                        .as(ExternDeclaration)
                        )
                ));
        private final Parsec parseAssociativity = choice(
                reserved(INFIXL),
                reserved(INFIXR),
                reserved(INFIX)
        );
        private final SymbolicParsec parseFixity = parseAssociativity.then(indented(lexeme(NATURAL))).as(Fixity);
        private final SymbolicParsec parseFixityDeclaration
                = parseFixity
                .then(indented(lexeme(OPERATOR)))
                .as(FixityDeclaration);

        private final SymbolicParsec parseDeclarationRef = choice(
                parseIdent.as(ValueRef),
                properName.then(
                        optional(parens(choice(
                                        token(DDOT),
                                        commaSep1(properName)
                                )
                        ))
                )
        ).as(PositionedDeclarationRef);

        private final SymbolicParsec parseTypeClassDeclaration
                = reserved(CLASS)
                .then(optional(indented(parens(commaSep1(parseQualified(properName).then(many(parseTypeAtom)))))).then(reserved(DERIVE)).as(pImplies))
                .then(indented(properName.as(pClassName)))
                .then(many(indented(lexeme(IDENT))))
                .then(optional(attempt(
                                indented(reserved(WHERE)).then(
                                        indentedList(positioned(parseTypeDeclaration)))
                        )
                ))
                .as(TypeClassDeclaration);

        private final SymbolicParsec parseTypeInstanceDeclaration
                = reserved(INSTANCE)
                .then(parseIdent.then(lexeme(indented(token(DCOLON)))))
                .then(optional(
                        parens(commaSep1(parseQualified(properName).then(many(parseTypeAtom))))
                                .then(indented(reserved(DARROW)))
                ))
                .then(indented(parseQualified(properName)).as(pClassName))
                .then(many(indented(parseTypeAtom)))
                .then(indented(reserved(WHERE)))
                .then(indented(indentedList(positioned(parseValueDeclaration))))
                .as(TypeInstanceDeclaration);

        private final Parsec qualImport
                = reserved(token("qualifield"))
                .then(indented(moduleName))
                .then(optional(indented(parens(commaSep(parseDeclarationRef)))))
                .then(reserved(token("as")))
                .then(moduleName);

        private final Parsec stdImport
                = moduleName
                .then(optional(indented(parens(commaSep1(parseDeclarationRef)))));
        private final SymbolicParsec parseImportDeclaration
                = reserved(IMPORT)
                .then(indented(choice(qualImport, stdImport)))
                .as(ImportDeclaration);

        public final Parsec parseDeclaration = positioned(choice(
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

        public final Parsec parseLocalDeclaration = positioned(choice(
                parseTypeDeclaration,
                parseValueDeclaration
        ));

        public final SymbolicParsec parseModule
                = reserved(MODULE)
                .then(indented(
                        moduleName.then(optional(parens(commaSep1(parseDeclarationRef))))
                ))
                .then(reserved(WHERE))
                .then(indentedList(parseDeclaration))
                .as(Module);

        public final Parsec parseModules = indentedList(parseModule);

        // Literals
        private final SymbolicParsec parseBooleanLiteral = reserved(token("true")).or(reserved(token("false"))).as(BooleanLiteral);
        private final SymbolicParsec parseNumericLiteral = reserved(NATURAL).or(reserved(token("false"))).as(NumericLiteral);
        private final SymbolicParsec parseStringLiteral = reserved(STRING).as(StringLiteral);
        private final SymbolicParsec parseArrayLiteral = squares(commaSep(parseValueRef)).as(StringLiteral);
        private final SymbolicParsec parseIdentifierAndValue = indented(identifier.or(stringLiteral)).then(indented(lexeme(token(":")))).then(indented(parseValueRef)).as(ObjectBinderField);
        private final SymbolicParsec parseObjectLiteral = braces(commaSep(parseIdentifierAndValue)).as(ObjectLiteral);

        private final Parsec parseAbs
                = reserved(BACKSLASH)
                .then(many1(indented(parseIdent.or(parseBinderNoParensRef).as(Abs))))
                .then(indented(reserved(ARROW)))
                .then(parseValueRef);
        private final SymbolicParsec parseVar = parseQualified(identifier).as(Var);
        private final SymbolicParsec parseConstructor = parseQualified(properName).as(Constructor);
        private final SymbolicParsec parseCaseAlternative
                = parseBinderRef
                .then(optional(parseGuard))
                .then(indented(reserved(ARROW).then(parseValueRef)))
                .as(CaseAlternative);
        private final SymbolicParsec parseCase
                = reserved(CASE)
                .then(parseValueRef)
                .then(indented(reserved(OF)))
                .then(indentedList(mark(parseCaseAlternative)))
                .as(Case);
        private final SymbolicParsec parseIfThenElse
                = reserved(IF)
                .then(indented(parseValueRef))
                .then(indented(reserved(THEN)))
                .then(indented(parseValueRef))
                .then(indented(reserved(ELSE)))
                .then(indented(parseValueRef))
                .as(IfThenElse);
        private final SymbolicParsec parseLet
                = reserved(LET)
                .then(indented(indentedList1(parseLocalDeclaration)))
                .then(indented(reserved(IN)))
                .then(parseValueRef)
                .as(Let);

        private final Parsec parseDoNotationLet
                = reserved(LET)
                .then(indented(indentedList1(parseLocalDeclaration)))
                .as(DoNotationLet);
        private final Parsec parseDoNotationBind
                = parseBinderRef
                .then(indented(reserved(token("<-"))).then(parseValueRef))
                .as(DoNotationBind);
        private final Parsec parseDoNotationElement = choice(
                attempt(parseDoNotationBind),
                parseDoNotationLet,
                attempt(parseValueRef.as(DoNotationValue))
        );
        private final Parsec parseDo
                = reserved(DO)
                .then(indented(indentedList(mark(parseDoNotationElement))));


        private final Parsec parseValueAtom = choice(
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

        private final Parsec parsePropertyUpdate
                = reserved(identifier.or(stringLiteral))
                .then(lexeme(indented(token(EQ))))
                .then(indented(parseValueRef));
        private final Parsec parseAccessor // P.try $ Accessor <$> (C.indented *> C.dot *> P.notFollowedBy C.opLetter *> C.indented *> (C.identifier <|> C.stringLiteral)) <*> pure obj
                = attempt(indented(token(DOT)).then(indented(identifier.or(stringLiteral)))).as(Accessor);

        private final Parsec parseIdentInfix =
                choice(
                        attempt(lexeme(TICK)).then(parseQualified(identifier)).lexeme(TICK),
                        parseQualified(lexeme(OPERATOR))
                );

        private final Parsec indexersAndAccessors
                = parseValueAtom
                .then(many(choice(
                        parseAccessor,
                        attempt(indented(braces(commaSep1(indented(parsePropertyUpdate))))),
                        indented(reserved(DCOLON).then(parseType))
                )));

        private final Parsec parseValuePostFix
                = indexersAndAccessors
                .then(many(choice(indented(indexersAndAccessors), attempt(indented(lexeme(DCOLON)).then(parsePolyTypeRef)))));

        private final ParsecRef parsePrefixRef = ref();
        private final SymbolicParsec parsePrefix = choice(parseValuePostFix, indented(token("-")).then(parsePrefixRef).as(UnaryMinus)).as(PrefixValue);

        {
            parsePrefixRef.setRef(parsePrefix);
        }

        private final SymbolicParsec parseValue
                = parsePrefix
                .then(optional(attempt(indented(parseIdentInfix)).then(parseValueRef)))
                .as(Value);

        // Binders
        private final SymbolicParsec parseNullBinder = lexeme(token("_")).as(NullBinder);
        private final SymbolicParsec parseStringBinder = lexeme(STRING).as(StringBinder);
        private final SymbolicParsec parseBooleanBinder = lexeme(token("true")).or(lexeme(token("false"))).as(BooleanBinder);
        private final SymbolicParsec parseNumberBinder = lexeme(NATURAL).or(lexeme(FLOAT)).as(NumberBinder);
        private final SymbolicParsec parseNamedBinder = parseIdent.then(lexeme(indented(token("@").then(indented(parseBinderRef))))).as(NamedBinder);
        private final SymbolicParsec parseVarBinder = parseIdent.as(VarBinder);
        private final SymbolicParsec parseConstructorBinder = lexeme(parseQualified(properName).then(many(indented(parseBinderNoParensRef)))).as(ConstructorBinder);
        private final SymbolicParsec parseNullaryConstructorBinder = lexeme(parseQualified(properName)).as(ConstructorBinder);
        private final SymbolicParsec parseObjectBinder = braces(commaSep(parseIdentifierAndValue)).as(ObjectBinder);
        private final SymbolicParsec parseArrayBinder = squares(commaSep(parseBinderRef)).as(ObjectBinder);
        private final SymbolicParsec parseBinder = choice(
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
        private final SymbolicParsec parseBinderNoParens = choice(
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

        {
            parseValueRef.setRef(parseValue);
            parseBinderRef.setRef(parseBinder);
            parseBinderNoParensRef.setRef(parseBinderNoParens);
        }
    }
}
