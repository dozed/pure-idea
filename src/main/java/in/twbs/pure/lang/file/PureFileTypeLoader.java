package in.twbs.pure.lang.file;

import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

public class PureFileTypeLoader extends FileTypeFactory {
    @Override
    public void createFileTypes(@NotNull FileTypeConsumer consumer) {
        consumer.consume(PureFileType.INSTANCE, new ExtensionFileNameMatcher(PureFileType.INSTANCE.getDefaultExtension()));
    }
}
