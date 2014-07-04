package in.twbs.pure.lang.file;

import com.intellij.openapi.fileTypes.LanguageFileType;
import in.twbs.pure.lang.PureLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class PureFileType extends LanguageFileType {
    public static final PureFileType INSTANCE = new PureFileType();

    private PureFileType() {
        super(PureLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Purescript";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Purescript file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "purs";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return null;
    }
}
