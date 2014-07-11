package in.twbs.pure.lang.psi;

public interface PureElements {
    PureElementType Program = new PureElementType("Program");
    PureElementType Module = new PureElementType("Module");

    PureElementType Star = new PureElementType("Star");
    PureElementType Bang = new PureElementType("Bang");
    PureElementType RowKind = new PureElementType("RowKind");
    PureElementType FunKind = new PureElementType("FunKind");

    PureElementType Qualified = new PureElementType("Qualified");
    PureElementType Type = new PureElementType("Type");
    PureElementType TypeArgs = new PureElementType("TypeArgs");
    PureElementType ForAll = new PureElementType("ForAll");
    PureElementType ConstrainedType = new PureElementType("ConstrainedType");
    PureElementType Row = new PureElementType("Row");
    PureElementType TypeVar = new PureElementType("TypeVar");
    PureElementType TypeConstructor = new PureElementType("TypeConstructor");
    PureElementType TypeApp = new PureElementType("TypeApp");
    PureElementType TypeAtom = new PureElementType("TypeAtom");
    PureElementType PolyType = new PureElementType("PolyType");

    PureElementType DataDeclaration = new PureElementType("DataDeclaration");
    PureElementType PositionedDeclaration = new PureElementType("PositionedDeclaration");
    PureElementType TypeDeclaration = new PureElementType("TypeDeclaration");
    PureElementType TypeSynonymDeclaration = new PureElementType("TypeSynonymDeclaration");
    PureElementType ValueDeclaration = new PureElementType("ValueDeclaration");
    PureElementType ExternDataDeclaration = new PureElementType("ExternDataDeclaration");
    PureElementType ExternInstanceDeclaration = new PureElementType("ExternInstanceDeclaration");
    PureElementType ExternDeclaration = new PureElementType("ExternDeclaration");
    PureElementType FixityDeclaration = new PureElementType("FixityDeclaration");
    PureElementType ImportDeclaration = new PureElementType("ImportDeclaration");
    PureElementType LocalDeclaration = new PureElementType("LocalDeclaration");
    PureElementType PositionedDeclarationRef = new PureElementType("PositionedDeclarationRef");
    PureElementType TypeClassDeclaration = new PureElementType("TypeClassDeclaration");
    PureElementType TypeInstanceDeclaration = new PureElementType("TypeInstanceDeclaration");

    PureElementType Guard = new PureElementType("Guard");
    PureElementType NullBinder = new PureElementType("NullBinder");
    PureElementType StringBinder = new PureElementType("StringBinder");
    PureElementType BooleanBinder = new PureElementType("BooleanBinder");
    PureElementType NumberBinder = new PureElementType("NumberBinder");
    PureElementType NamedBinder = new PureElementType("NamedBinder");
    PureElementType VarBinder = new PureElementType("VarBinder");
    PureElementType ConstructorBinder = new PureElementType("ConstructorBinder");
    PureElementType ObjectBinder = new PureElementType("ObjectBinder");
    PureElementType ObjectBinderField = new PureElementType("ObjectBinderField");
    PureElementType BinderAtom = new PureElementType("BinderAtom");
    PureElementType Binder = new PureElementType("Binder");

    PureElementType ValueRef = new PureElementType("ValueRef");

    PureElementType BooleanLiteral = new PureElementType("BooleanLiteral");
    PureElementType NumericLiteral = new PureElementType("NumericLiteral");
    PureElementType StringLiteral = new PureElementType("StringLiteral");
    PureElementType ArrayLiteral = new PureElementType("ArrayLiteral");
    PureElementType ObjectLiteral = new PureElementType("ObjectLiteral");

    PureElementType Abs = new PureElementType("Abs");
    PureElementType IdentInfix = new PureElementType("IdentInfix");
    PureElementType Var = new PureElementType("Var");
    PureElementType Constructor = new PureElementType("Constructor");
    PureElementType Case = new PureElementType("Case");
    PureElementType CaseAlternative = new PureElementType("CaseAlternative");
    PureElementType IfThenElse = new PureElementType("IfThenElse");
    PureElementType Let = new PureElementType("Let");
    PureElementType Parens = new PureElementType("Parens");
    PureElementType UnaryMinus = new PureElementType("UnaryMinus");
    PureElementType PrefixValue = new PureElementType("PrefixValue");
    PureElementType Accessor = new PureElementType("Accessor");
    PureElementType DoNotationLet = new PureElementType("DoNotationLet");
    PureElementType DoNotationBind = new PureElementType("DoNotationBind");
    PureElementType DoNotationValue = new PureElementType("DoNotationValue");
    PureElementType Value = new PureElementType("Value");

    PureElementType Fixity = new PureElementType("Fixity");
    PureElementType JSRaw = new PureElementType("JavaScript");
    PureElementType pModuleName = new PureElementType("ModuleName");
    PureElementType pClassName = new PureElementType("ClassName");
    PureElementType pImplies = new PureElementType("Implies");
}
