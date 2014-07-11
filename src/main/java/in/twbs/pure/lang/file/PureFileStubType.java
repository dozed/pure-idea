package in.twbs.pure.lang.file;

import com.intellij.psi.tree.IStubFileElementType;
import in.twbs.pure.lang.PureLanguage;

public class PureFileStubType extends IStubFileElementType {
    public static final PureFileStubType INSTANCE = new PureFileStubType();

    private PureFileStubType() {
        super(PureLanguage.INSTANCE);
    }
}
