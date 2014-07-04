package in.twbs.pure.lang;

import com.intellij.lang.Language;

public class PureLanguage extends Language {
    public static final PureLanguage INSTANCE = new PureLanguage();

    private PureLanguage() {
        super("Purescript", "text/purescript", "text/x-purescript", "application/x-purescript");
    }
}
