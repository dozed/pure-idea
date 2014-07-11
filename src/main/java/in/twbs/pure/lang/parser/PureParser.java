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
    public final static PureParsecParser PARSER = new PureParsecParser();

    @NotNull
    @Override
    public ASTNode parse(IElementType root, PsiBuilder builder) {
        builder.setDebugMode(true);
        ParserContext context = new ParserContext(builder);
        PsiBuilder.Marker mark = context.start();
        context.whiteSpace();
        ParserInfo info = new PureParsecParser().program.parse(context);
        IElementType nextType = null;
        if (!context.eof()) {
            PsiBuilder.Marker errorMarker = null;
            while (!context.eof()) {
                if (context.getPosition() >= info.position && errorMarker == null) {
                    errorMarker = context.start();
                    nextType = builder.getTokenType();
                }
                context.advance();
            }
            if (errorMarker != null) {
                if (nextType != null)
                    errorMarker.error("Unexpected " + nextType.toString() + ". " + info.toString());
                else
                    errorMarker.error(info.toString());
            }
        }
        mark.done(root);
        return builder.getTreeBuilt();
    }

    public final static class PureParsecParser {
        private PureParsecParser() {
        }

        @NotNull
        private Parsec parseQualified(@NotNull Parsec p) {
            return attempt(many(attempt(token(PROPER_NAME).then(token(DOT)))).then(p).as(Qualified));
        }

        private final Parsec identifier = reserved(IDENT);
        private final Parsec properName = reserved(PROPER_NAME);
        private final Parsec moduleName = parseQualified(properName).as(pModuleName);

        private final Parsec stringLiteral = attempt(lexeme(STRING));

        @NotNull
        private Parsec positioned(@NotNull final Parsec p) {
            return p;
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
        private final SymbolicParsec parseStar = lexeme("*").as(Star);
        private final SymbolicParsec parseBang = lexeme("!").as(Bang);
        private final Parsec parseKindAtom = indented(choice(parseStar, parseBang, parens(parseKindRef)));
        private final SymbolicParsec parseKindPrefix = many(lexeme("#")).then(parseKindAtom).as(RowKind);
        private final SymbolicParsec parseKind = parseKindPrefix.then(optional(reserved(ARROW).then(parseKindRef))).as(FunKind);

        {
            parseKindRef.setRef(parseKind);
        }

        // Types.hs
        private final ParsecRef parsePolyTypeRef = ref();
        private final ParsecRef parseTypeRef = ref();
        private final ParsecRef parseForAllRef = ref();

        private final Parsec parseFunction = parens(reserved(ARROW));
        private final Parsec parseTypeVariable = lexeme(IDENT).as(TypeVar);

        private final Parsec parseTypeConstructor = parseQualified(properName).as(TypeConstructor);
        private final Parsec parseNameAndType = indented(seq(
                        attempt(lexeme(IDENT).or(lexeme(STRING))),
                        reserved(DCOLON))
        );
        private final Parsec parseRowEnding = optional(indented(lexeme("|")).then(indented(lexeme(IDENT))).as(TypeVar));
        private final Parsec parseRowNonEmpty
                = commaSep1(parseNameAndType.then(parsePolyTypeRef))
                .then(parseRowEnding)
                .as(Row);
        private final Parsec parseRowAllowEmpty
                = commaSep(parseNameAndType.then(parsePolyTypeRef))
                .then(parseRowEnding)
                .as(Row);
        private final Parsec parseObject = braces(parseRowAllowEmpty).as(ObjectType);
        private final Parsec parseTypeAtom = indented(
                choice(
                        reserved(lexeme("Number")),
                        reserved(lexeme("String")),
                        reserved(lexeme("Boolean")),
                        attempt(squares(optional(parseTypeRef))),
                        attempt(parseFunction),
                        attempt(parseObject),
                        attempt(parseTypeVariable),
                        attempt(parseTypeConstructor),
                        parseForAllRef,
                        parens(attempt(parseRowNonEmpty).or(parsePolyTypeRef)))
        ).as(TypeAtom);

        private final Parsec parseConstrainedType =
                optional(attempt(
                        parens(commaSep1(parseQualified(properName).then(indented(many(parseTypeAtom)))))
                                .then(lexeme(DARROW))
                )).then(indented(parseTypeRef)).as(ConstrainedType);
        private final SymbolicParsec parseForAll
                = reserved(FORALL)
                .then(many1(indented(identifier)))
                .then(indented(lexeme(DOT)))
                .then(parseConstrainedType).as(ForAll);

        private final Parsec parseIdent = choice(
                reserved(IDENT),
                attempt(parens(lexeme(OPERATOR)).as(IDENT))
        );

        private final SymbolicParsec parseType
                = many1(parseTypeAtom)
                .then(optional(
                        reserved(ARROW).then(parseTypeRef)
                )).as(Type);

        {
            parsePolyTypeRef.setRef(parseType);
            parseTypeRef.setRef(parseType);
            parseForAllRef.setRef(parseForAll);
        }

        // Declarations.hs
        private final ParsecRef parseBinderNoParensRef = ref();
        private final ParsecRef parseBinderRef = ref();
        private final ParsecRef parseValueRef = ref();
        private final ParsecRef parseLocalDeclarationRef = ref();

        private final SymbolicParsec parseGuard = indented(lexeme("|")).then(indented(parseValueRef)).as(Guard);
        private final SymbolicParsec parseDataDeclaration
                = reserved(DATA)
                .then(indented(properName))
                .then(many(indented(identifier)).as(TypeArgs))
                .then(optional(attempt(lexeme(EQ))
                        .then(sepBy1(properName.then(many(indented(parseTypeAtom))), lexeme("|")))))
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
                .then(lexeme(indented(lexeme(EQ))).then(parseValueRef))
                .then(optional(indented(lexeme(WHERE)).then(indentedList1(parseLocalDeclarationRef))))
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
                                        .then(indented(lexeme(DCOLON)))
                                        .then(optional(parseDeps))
                                        .then(parseQualified(properName).as(pClassName))
                                        .then(many(indented(parseTypeAtom)))
                                        .as(ExternInstanceDeclaration),
                                attempt(parseIdent)
                                        .then(optional(stringLiteral.as(JSRaw)))
                                        .then(indented(lexeme(DCOLON)))
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
                                        lexeme(DDOT),
                                        commaSep1(properName)
                                )
                        ))
                )
        ).as(PositionedDeclarationRef);

        private final SymbolicParsec parseTypeClassDeclaration
                = lexeme(CLASS)
                .then(optional(indented(parens(commaSep1(parseQualified(properName).then(many(parseTypeAtom))))).then(reserved("<=")).as(pImplies)))
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
                .then(parseIdent.then(indented(lexeme(DCOLON))))
                .then(optional(
                        parens(commaSep1(parseQualified(properName).then(many(parseTypeAtom))))
                                .then(indented(reserved(DARROW)))
                ))
                .then(indented(parseQualified(properName)).as(pClassName))
                .then(many(indented(parseTypeAtom)))
                .then(optional(attempt(
                        indented(reserved(WHERE))
                                .then(indented(indentedList(positioned(parseValueDeclaration))))
                )))
                .as(TypeInstanceDeclaration);

        private final Parsec qualImport
                = reserved("qualified")
                .then(indented(moduleName))
                .then(optional(indented(parens(commaSep(parseDeclarationRef)))))
                .then(reserved("as"))
                .then(moduleName);

        private final Parsec stdImport
                = moduleName
                .then(optional(indented(parens(commaSep(parseDeclarationRef)))));
        private final SymbolicParsec parseImportDeclaration
                = reserved(IMPORT)
                .then(indented(choice(qualImport, stdImport)))
                .as(ImportDeclaration);

        private final Parsec parseDeclaration = positioned(choice(
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

        private final Parsec parseLocalDeclaration = positioned(choice(
                parseTypeDeclaration,
                parseValueDeclaration
        ));

        {
            parseLocalDeclarationRef.setRef(parseLocalDeclaration);
        }

        private final SymbolicParsec parseModule
                = reserved(MODULE)
                .then(indented(moduleName))
                .then(optional(parens(commaSep1(parseDeclarationRef))))
                .then(reserved(WHERE))
                .then(indentedList(parseDeclaration))
                .as(Module);

        private final Parsec program = indentedList(parseModule).as(Program);
        // Literals
        private final SymbolicParsec parseBooleanLiteral = reserved("true").or(reserved("false")).as(BooleanLiteral);
        private final SymbolicParsec parseNumericLiteral = reserved(NATURAL).or(reserved(FLOAT)).as(NumericLiteral);
        private final SymbolicParsec parseStringLiteral = reserved(STRING).as(StringLiteral);
        private final SymbolicParsec parseArrayLiteral = squares(commaSep(parseValueRef)).as(ArrayLiteral);
        private final SymbolicParsec parseIdentifierAndValue
                = indented(identifier.or(stringLiteral))
                .then(indented(lexeme(":")))
                .then(indented(parseValueRef))
                .as(ObjectBinderField);
        private final SymbolicParsec parseObjectLiteral =
                braces(commaSep(parseIdentifierAndValue)).as(ObjectLiteral);

        private final Parsec parseAbs
                = reserved(BACKSLASH)
                .then(many1(indented(parseIdent.or(parseBinderNoParensRef).as(Abs))))
                .then(indented(reserved(ARROW)))
                .then(parseValueRef);
        private final SymbolicParsec parseVar = parseQualified(parseIdent).as(Var);
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
                .then(indented(indentedList(mark(parseCaseAlternative))))
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
                .then(indented(reserved("<-")).then(parseValueRef))
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
                .then(indented(lexeme(EQ)))
                .then(indented(parseValueRef));
        private final Parsec parseAccessor // P.try $ Accessor <$> (C.indented *> C.dot *> P.notFollowedBy C.opLetter *> C.indented *> (C.identifier <|> C.stringLiteral)) <*> pure obj
                = attempt(indented(token(DOT)).then(indented(identifier.or(stringLiteral)))).as(Accessor);

        private final Parsec parseIdentInfix =
                choice(
                        reserved(TICK).then(parseQualified(identifier)).lexeme(TICK),
                        parseQualified(lexeme(OPERATOR))
                ).as(IdentInfix);

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
        private final SymbolicParsec parsePrefix =
                choice(
                        parseValuePostFix,
                        indented(lexeme("-")).then(parsePrefixRef).as(UnaryMinus)
                ).as(PrefixValue);

        {
            parsePrefixRef.setRef(parsePrefix);
        }

        private final SymbolicParsec parseValue
                = parsePrefix
                .then(optional(
                        attempt(indented(parseIdentInfix)).then(parseValueRef)
                ))
                .as(Value);

        // Binders
        private final SymbolicParsec parseNullBinder = lexeme("_").as(NullBinder);
        private final SymbolicParsec parseStringBinder = lexeme(STRING).as(StringBinder);
        private final SymbolicParsec parseBooleanBinder = lexeme("true").or(lexeme("false")).as(BooleanBinder);
        private final SymbolicParsec parseNumberBinder = lexeme(NATURAL).or(lexeme(FLOAT)).as(NumberBinder);
        private final SymbolicParsec parseNamedBinder = parseIdent.then(indented(lexeme("@")).then(indented(parseBinderRef))).as(NamedBinder);
        private final SymbolicParsec parseVarBinder = parseIdent.as(VarBinder);
        private final SymbolicParsec parseConstructorBinder = lexeme(parseQualified(properName).then(many(indented(parseBinderNoParensRef)))).as(ConstructorBinder);
        private final SymbolicParsec parseNullaryConstructorBinder = lexeme(parseQualified(properName)).as(ConstructorBinder);
        private final Parsec parseIdentifierAndBinder
                = lexeme(identifier.or(stringLiteral))
                .then(indented(lexeme(EQ)))
                .then(indented(parseBinderRef));
        private final SymbolicParsec parseObjectBinder = braces(commaSep(parseIdentifierAndBinder)).as(ObjectBinder);
        private final SymbolicParsec parseArrayBinder = squares(commaSep(parseBinderRef)).as(ObjectBinder);
        private final SymbolicParsec parseBinderAtom = choice(
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
        ).as(BinderAtom);
        private final SymbolicParsec parseBinder
                = parseBinderAtom
                .then(optional(lexeme(":").then(parseBinderRef)))
                .as(Binder);
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
