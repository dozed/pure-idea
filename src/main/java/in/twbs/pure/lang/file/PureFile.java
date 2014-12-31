package in.twbs.pure.lang.file;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import in.twbs.pure.lang.PureLanguage;
import org.jetbrains.annotations.NotNull;

public class PureFile extends PsiFileBase {

    public PureFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, PureLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return PureFileType.INSTANCE;
    }
}
