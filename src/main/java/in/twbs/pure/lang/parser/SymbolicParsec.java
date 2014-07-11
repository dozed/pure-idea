package in.twbs.pure.lang.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class SymbolicParsec extends Parsec {
    @NotNull
    private final Parsec ref;
    @NotNull
    private final IElementType node;

    public SymbolicParsec(@NotNull Parsec ref, @NotNull IElementType node) {
        this.ref = ref;
        this.node = node;
    }

    @NotNull
    @Override
    public ParserInfo parse(@NotNull ParserContext context) {
        int startPosition = context.getPosition();
        PsiBuilder.Marker pack = context.start();
        ParserInfo info = ref.parse(context);
        if (info.success) {
            pack.done(node);
        } else {
            pack.drop();
        }
        if (startPosition == context.getPosition()) {
            info = new ParserInfo(info.position, this, info.success);
        }
        return info;
    }

    @NotNull
    @Override
    public String getName() {
        return node.toString();
    }
}
