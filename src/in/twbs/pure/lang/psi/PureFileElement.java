package in.twbs.pure.lang.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import in.twbs.pure.lang.PureLanguage;
import in.twbs.pure.lang.file.PureFileType;
import org.jetbrains.annotations.NotNull;

public class PureFileElement extends PsiFileBase {
    public PureFileElement(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, PureLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return PureFileType.INSTANCE;
    }
}
