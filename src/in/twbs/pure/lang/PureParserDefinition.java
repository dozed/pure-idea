package in.twbs.pure.lang;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import in.twbs.pure.lang.file.PureFile;
import in.twbs.pure.lang.file.PureFileStubType;
import in.twbs.pure.lang.lexer.PureLexer;
import in.twbs.pure.lang.parser.PureParser;
import in.twbs.pure.lang.psi.PureTokens;
import org.jetbrains.annotations.NotNull;

public class PureParserDefinition implements ParserDefinition, PureTokens {
    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new PureLexer();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new PureParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return PureFileStubType.INSTANCE;
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return kWhiteSpaces;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return kComments;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return kStrings;
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        return new ASTWrapperPsiElement(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new PureFile(viewProvider);
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }
}
