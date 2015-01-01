package in.twbs.pure.lang.module;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.SourcePathsBuilder;
import com.intellij.openapi.module.ModuleType;

public class PureModuleBuilder extends JavaModuleBuilder implements SourcePathsBuilder {
    public static final PureModuleBuilder INSTANCE = new PureModuleBuilder();

    private PureModuleBuilder() {
    }

    /**
     * Returns the Haskell module type.
     */
    @Override
    public ModuleType getModuleType() {
        return PureModuleType.getInstance();
    }
}
