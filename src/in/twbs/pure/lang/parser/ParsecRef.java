package in.twbs.pure.lang.parser;

import org.jetbrains.annotations.NotNull;

public final class ParsecRef extends Parsec {
    private SymbolicParsec ref;

    public void setRef(@NotNull SymbolicParsec ref) {
        this.ref = ref;
    }

    @NotNull
    @Override
    public ParserInfo parse(@NotNull ParserContext context) {
        return ref.parse(context);
    }

    @NotNull
    @Override
    public String getName() {
        return ref.getName();
    }
}
