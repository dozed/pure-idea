package in.twbs.pure.lang.module;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.SourcePathsBuilder;
import com.intellij.openapi.module.ModuleType;

import org.apache.commons.lang.builder.HashCodeBuilder;

public class PureModuleBuilder extends JavaModuleBuilder implements SourcePathsBuilder {
    PureModuleBuilder() {
    }

    /**
     * Returns the Haskell module type.
     */
    @Override
    public ModuleType getModuleType() {
        return PureModuleType.getInstance();
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
