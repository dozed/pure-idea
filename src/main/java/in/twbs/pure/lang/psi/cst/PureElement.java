package in.twbs.pure.lang.psi.cst;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ReflectionCache;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class PureElement extends CompositePsiElement {
    protected PureElement(@NotNull IElementType type) {
        super(type);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T extends PureElement> T[] findChildren(Class<T> type) {
        ArrayList<T> result = new ArrayList<T>();
        for (PsiElement psiElement : this.getChildren()) {
            if (ReflectionCache.isInstance(psiElement, type)) {
                result.add((T) psiElement);
            }
        }

        return result.toArray((T[]) Array.newInstance(type, result.size()));
    }
}
