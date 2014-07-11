package in.twbs.pure.lang.psi;

import com.intellij.psi.tree.IElementType;
import in.twbs.pure.lang.PureLanguage;
import org.jetbrains.annotations.NotNull;

public class PureElementType extends IElementType {
    public PureElementType(@NotNull String name) {
        super(name, PureLanguage.INSTANCE);
    }
}
