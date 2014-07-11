package in.twbs.pure.lang.psi.cst;

import in.twbs.pure.lang.psi.PureElements;
import org.jetbrains.annotations.NotNull;

public class Program extends PureElement {
    protected Program() {
        super(PureElements.Program);
    }

    @NotNull
    public Module[] getModules() {
        return this.findChildren(Module.class);
    }
}
