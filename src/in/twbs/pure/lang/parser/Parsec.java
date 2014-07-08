package in.twbs.pure.lang.parser;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public abstract class Parsec {
    @NotNull
    public abstract ParserInfo parse(@NotNull ParserContext context);

    @NotNull
    public abstract String getName();

    @NotNull
    public Parsec then(@NotNull Parsec next) {
        return Combinators.seq(this, next);
    }

    @NotNull
    public Parsec then(@NotNull IElementType type) {
        return then(Combinators.token(type));
    }

    @NotNull
    public Parsec lexeme(@NotNull IElementType type) {
        return then(Combinators.lexeme(Combinators.token(type)));
    }

    @NotNull
    public Parsec or(@NotNull Parsec next) {
        return Combinators.choice(this, next);
    }

    @NotNull
    public SymbolicParsec as(@NotNull final IElementType node) {
        return new SymbolicParsec(this, node);
    }
}