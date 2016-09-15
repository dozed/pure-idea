package in.twbs.pure.lang.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;

import static in.twbs.pure.lang.parser.Combinators.Predicate;
import static in.twbs.pure.lang.parser.Combinators.attempt;
import static in.twbs.pure.lang.parser.Combinators.braces;
import static in.twbs.pure.lang.parser.Combinators.choice;
import static in.twbs.pure.lang.parser.Combinators.commaSep;
import static in.twbs.pure.lang.parser.Combinators.commaSep1;
import static in.twbs.pure.lang.parser.Combinators.guard;
import static in.twbs.pure.lang.parser.Combinators.indented;
import static in.twbs.pure.lang.parser.Combinators.keyword;
import static in.twbs.pure.lang.parser.Combinators.lexeme;
import static in.twbs.pure.lang.parser.Combinators.many;
import static in.twbs.pure.lang.parser.Combinators.many1;
import static in.twbs.pure.lang.parser.Combinators.mark;
import static in.twbs.pure.lang.parser.Combinators.optional;
import static in.twbs.pure.lang.parser.Combinators.parens;
import static in.twbs.pure.lang.parser.Combinators.ref;
import static in.twbs.pure.lang.parser.Combinators.reserved;
import static in.twbs.pure.lang.parser.Combinators.same;
import static in.twbs.pure.lang.parser.Combinators.sepBy1;
import static in.twbs.pure.lang.parser.Combinators.squares;
import static in.twbs.pure.lang.parser.Combinators.token;
import static in.twbs.pure.lang.parser.Combinators.untilSame;

import in.twbs.pure.lang.psi.PureElements;
import in.twbs.pure.lang.psi.PureTokens;
import org.jetbrains.annotations.NotNull;

public class PureParser implements PsiParser, PureTokens, PureElements {
    @NotNull
    @Override
    public ASTNode parse(IElementType root, PsiBuilder builder) {
        // builder.setDebugMode(true);
        ParserContext context = new ParserContext(builder);
        PsiBuilder.Marker mark = context.start();
        context.whiteSpace();
        // Creating a new instance here allows hot swapping while debugging.
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
            return attempt(many(attempt(token(PROPER_NAME).then(token(DOT))).as(pModuleName)).then(p).as(Qualified));
        }

        private final Parsec idents = choice(token(IDENT), choice(token(FORALL), token(QUALIFIED), token(HIDING), token(AS)).as(Identifier));
        private final Parsec identifier = lexeme(idents);
        private final Parsec lname
                = lexeme(choice(
                token(IDENT),
                token(DATA),
                token(NEWTYPE),
                token(TYPE),
                token(FOREIGN),
                token(IMPORT),
                token(INFIXL),
                token(INFIXR),
                token(INFIX),
                token(CLASS),
                token(INSTANCE),
                token(MODULE),
                token(CASE),
                token(OF),
                token(IF),
                token(THEN),
                token(ELSE),
                token(DO),
                token(LET),
                token(TRUE),
                token(FALSE),
                token(IN),
                token(WHERE),
                token(FORALL),
                token(QUALIFIED),
                token(HIDING),
                token(AS)).as(Identifier));
        private final Parsec operator = choice(token(OPERATOR), token(DDOT), token(LARROW));
        private final Parsec properName = lexeme(PROPER_NAME);
        private final Parsec moduleName = lexeme(parseQualified(token(PROPER_NAME).as(pModuleName)));

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
        private final ParsecRef parseKindPrefixRef = ref();
        private final SymbolicParsec parseStar = keyword(START, "*").as(Star);
        private final SymbolicParsec parseBang = keyword(BANG, "!").as(Bang);
        private final Parsec parseKindAtom = indented(choice(parseStar, parseBang, parens(parseKindRef)));
        private final Parsec parseKindPrefix
                = choice(
                lexeme("#").then(parseKindPrefixRef).as(RowKind),
                parseKindAtom);
        private final SymbolicParsec parseKind
                = parseKindPrefix.then(optional(reserved(ARROW).then(parseKindRef))).as(FunKind);

        {
            parseKindPrefixRef.setRef(parseKindPrefix);
            parseKindRef.setRef(parseKind);
        }

        // Types.hs
        private final ParsecRef parsePolyTypeRef = ref();
        private final ParsecRef parseTypeRef = ref();
        private final ParsecRef parseForAllRef = ref();

        private final Parsec parseTypeWildcard = reserved("_");

        private final Parsec parseFunction = parens(reserved(ARROW));
        private final Parsec parseTypeVariable = lexeme(guard(idents, new Predicate<String>() {
            @Override
            public boolean test(String content) {
                return !content.equals("forall");
            }
        }, "not `forall`")).as(TypeVar);

        private final Parsec parseTypeConstructor = parseQualified(properName).as(TypeConstructor);

        @NotNull
        private Parsec parseNameAndType(Parsec p) {
            return indented(lexeme(choice(lname, stringLiteral))).then(indented(lexeme(DCOLON))).then(p);
        }

        //= indented(seq(attempt(lexeme(identifier).or(lexeme(STRING))), reserved(DCOLON)));
        private final Parsec parseRowEnding
                = optional(
                        indented(lexeme(PIPE)).then(indented(
                                choice(
                                        attempt(parseTypeWildcard),
                                        attempt(lexeme(identifier).as(TypeVar))))));

        private final Parsec parseRow
                = commaSep(parseNameAndType(parsePolyTypeRef))
                .then(parseRowEnding)
                .as(Row);
        private final Parsec parseObject = braces(parseRow).as(ObjectType);
        private final Parsec parseTypeAtom = indented(
                choice(
                        attempt(squares(optional(parseTypeRef))),
                        attempt(parseFunction),
                        attempt(parseObject),
                        attempt(parseTypeWildcard),
                        attempt(parseTypeVariable),
                        attempt(parseTypeConstructor),
                        attempt(parseForAllRef),
                        attempt(parens(parseRow)),
                        attempt(parens(parsePolyTypeRef)))
        ).as(TypeAtom);

        private final Parsec parseConstrainedType =
                optional(attempt(
                        parens(commaSep1(parseQualified(properName).then(indented(many(parseTypeAtom)))))
                                .then(lexeme(DARROW))
                )).then(indented(parseTypeRef)).as(ConstrainedType);
        private final SymbolicParsec parseForAll
                = reserved("forall")
                .then(many1(indented(lexeme(identifier))))
                .then(indented(lexeme(DOT)))
                .then(parseConstrainedType).as(ForAll);

        private final Parsec parseIdent = choice(
                lexeme(identifier),
                attempt(parens(lexeme(operator.as(Identifier))))
        );

        private final Parsec parseTypePostfix
                = parseTypeAtom
                .then(optional(attempt(indented(lexeme(DCOLON).then(parseKind)))));

        private final SymbolicParsec parseType
                = many1(parseTypePostfix)
                .then(optional(
                        reserved(ARROW).then(parseTypeRef)
                )).as(Type);

        {
            parsePolyTypeRef.setRef(parseType);
            parseTypeRef.setRef(parseType);
            parseForAllRef.setRef(parseForAll);
        }

        // Declarations.hs
        private final Parsec kindedIdent
                = lexeme(identifier)
                .or(parens(lexeme(identifier).then(indented(lexeme(DCOLON))).then(indented(parseKindRef))));
        private final ParsecRef parseBinderNoParensRef = ref();
        private final ParsecRef parseBinderRef = ref();
        private final ParsecRef parseValueRef = ref();
        private final ParsecRef parseLocalDeclarationRef = ref();

        private final SymbolicParsec parseGuard = lexeme(PIPE).then(indented(parseValueRef)).as(Guard);
        private final SymbolicParsec parseDataDeclaration
                = reserved(DATA)
                .then(indented(properName))
                .then(many(indented(kindedIdent)).as(TypeArgs))
                .then(optional(attempt(lexeme(EQ))
                        .then(sepBy1(properName.then(many(indented(parseTypeAtom))), PIPE))))
                .as(DataDeclaration);
        private final SymbolicParsec parseTypeDeclaration
                = attempt(parseIdent.then(indented(lexeme(DCOLON))))
                .then(parsePolyTypeRef)
                .as(TypeDeclaration);
        private final SymbolicParsec parseNewtypeDeclaration
                = reserved(NEWTYPE)
                .then(indented(properName))
                .then(optional(indented(identifier)))
                .then(lexeme(EQ))
                .then(properName.then(indented(parseTypeAtom)))
                .as(NewtypeDeclaration);
        private final SymbolicParsec parseTypeSynonymDeclaration
                = reserved(TYPE)
                .then(reserved(PROPER_NAME))
                .then(many(indented(lexeme(kindedIdent))))
                .then(indented(lexeme(EQ)).then(parsePolyTypeRef))
                .as(TypeSynonymDeclaration);

        private final Parsec parseValueWithWhereClause
                = parseValueRef
                .then(optional(
                        indented(lexeme(WHERE))
                                .then(indented(mark(many1(same(parseLocalDeclarationRef)))))));

        private final SymbolicParsec parseValueDeclaration
                = parseIdent
                .then(many(parseBinderNoParensRef))
                .then(choice(
                        indented(many1(parseGuard.then(indented(lexeme(EQ).then(parseValueWithWhereClause))))),
                        indented(lexeme(EQ).then(parseValueWithWhereClause)))).as(ValueDeclaration);

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
                .then(indented(lexeme(operator)))
                .as(FixityDeclaration);

        private final SymbolicParsec parseDeclarationRef = choice(
                parseIdent.as(ValueRef),
                properName.then(
                        optional(parens(optional(choice(
                                        reserved(DDOT),
                                commaSep1(properName))))))).as(PositionedDeclarationRef);

        private final SymbolicParsec parseTypeClassDeclaration
                = lexeme(CLASS)
                .then(optional(indented(parens(commaSep1(parseQualified(properName).then(many(parseTypeAtom))))).then(reserved("<=")).as(pImplies)))
                .then(indented(properName.as(pClassName)))
                .then(many(indented(kindedIdent)))
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

        private final SymbolicParsec parseDeriveTypeInstanceDeclaration
                = reserved(DERIVE)
                .then(reserved(INSTANCE))
                .then(parseIdent.then(indented(lexeme(DCOLON))))
                .then(indented(parseQualified(properName)).as(pClassName))
                .then(many(indented(parseTypeAtom)))
                .as(DeriveTypeInstanceDeclaration);

        private final Parsec importDeclarationType
            = optional(indented(parens(commaSep(parseDeclarationRef))));

        private final Parsec qualImport
                = optional(reserved(QUALIFIED))
                .then(indented(moduleName))
                .then(importDeclarationType)
                .then(reserved(AS))
                .then(moduleName);

        private final Parsec stdImport
                = moduleName
                .then(optional(reserved(HIDING)))
                .then(importDeclarationType);

        private final SymbolicParsec parseImportDeclaration
                = reserved(IMPORT)
                .then(indented(choice(attempt(qualImport), stdImport)))
                .as(ImportDeclaration);

        private final Parsec parseDeclaration = positioned(choice(
                parseDataDeclaration,
                parseNewtypeDeclaration,
                parseTypeDeclaration,
                parseTypeSynonymDeclaration,
                parseValueDeclaration,
                parseExternDeclaration,
                parseFixityDeclaration,
                parseImportDeclaration,
                parseTypeClassDeclaration,
                parseTypeInstanceDeclaration,
                parseDeriveTypeInstanceDeclaration
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
        private final SymbolicParsec parseBooleanLiteral = reserved(PureTokens.TRUE).or(reserved(PureTokens.FALSE)).as(BooleanLiteral);
        private final SymbolicParsec parseNumericLiteral = reserved(NATURAL).or(reserved(FLOAT)).as(NumericLiteral);
        private final SymbolicParsec parseStringLiteral = reserved(STRING).as(StringLiteral);
        private final SymbolicParsec parseArrayLiteral = squares(commaSep(parseValueRef)).as(ArrayLiteral);
        private final SymbolicParsec parseIdentifierAndValue
                = indented(lexeme(lname).or(stringLiteral))
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
                .then(indented(choice(
                        many1(parseGuard.then(indented(lexeme(ARROW).then(parseValueRef)))),
                        reserved(ARROW).then(parseValueRef))))
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
                .then(indented(reserved(LARROW)).then(parseValueRef))
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
                = reserved(lname.or(stringLiteral))
                .then(indented(lexeme(EQ)))
                .then(indented(parseValueRef));
        private final Parsec parseAccessor
                = attempt(indented(token(DOT)).then(indented(lname.or(stringLiteral)))).as(Accessor);

        private final Parsec parseIdentInfix =
                choice(
                        reserved(TICK).then(parseQualified(identifier)).lexeme(TICK),
                        parseQualified(lexeme(operator))
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
        private final SymbolicParsec parseNullBinder = reserved("_").as(NullBinder);
        private final SymbolicParsec parseStringBinder = lexeme(STRING).as(StringBinder);
        private final SymbolicParsec parseBooleanBinder = lexeme("true").or(lexeme("false")).as(BooleanBinder);
        private final SymbolicParsec parseNumberBinder
                = optional(choice(lexeme("+"), lexeme("-")))
                .then(lexeme(NATURAL).or(lexeme(FLOAT))).as(NumberBinder);
        private final SymbolicParsec parseNamedBinder = parseIdent.then(indented(lexeme("@")).then(indented(parseBinderRef))).as(NamedBinder);
        private final SymbolicParsec parseVarBinder = parseIdent.as(VarBinder);
        private final SymbolicParsec parseConstructorBinder = lexeme(parseQualified(properName).then(many(indented(parseBinderNoParensRef)))).as(ConstructorBinder);
        private final SymbolicParsec parseNullaryConstructorBinder = lexeme(parseQualified(properName)).as(ConstructorBinder);
        private final Parsec parseIdentifierAndBinder
                = lexeme(lname.or(stringLiteral))
                .then(indented(lexeme(EQ).or(lexeme(":"))))
                .then(indented(parseBinderRef));
        private final SymbolicParsec parseObjectBinder
                = braces(commaSep(parseIdentifierAndBinder)).as(ObjectBinder);
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
