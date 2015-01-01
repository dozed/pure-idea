package in.twbs.pure.lang.module;

import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import in.twbs.pure.lang.icons.PureIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class PureModuleType extends ModuleType<PureModuleBuilder> {
    public static final String MODULE_TYPE_ID = "PURESCRIPT_MODULE";

    public PureModuleType() {
        super(MODULE_TYPE_ID);
    }

    public static PureModuleType getInstance() {
        return (PureModuleType) ModuleTypeManager.getInstance().findByID(MODULE_TYPE_ID);
    }

    @NotNull
    @Override
    public PureModuleBuilder createModuleBuilder() {
        return PureModuleBuilder.INSTANCE;
    }

    @NotNull
    @Override
    public String getName() {
        return "PureScript Module";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "PureScript modules are used for developing <b>PureScript</b> applications.";
    }

    @Override
    public Icon getBigIcon() {
        return PureIcons.FILE;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean b) {
        return PureIcons.FILE;
    }
}
