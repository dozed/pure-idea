package in.twbs.pure.lang.parser;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashSet;

public class ParserInfo {
    public final int position;
    public final String[] expected;
    public final boolean success;

    public ParserInfo(int position, String[] expected, boolean success) {
        this.position = position;
        this.success = success;
        this.expected = expected;
    }

    public ParserInfo(int position, Parsec expected, boolean success) {
        this(position, new String[]{expected.getName()}, success);
    }

    public static ParserInfo merge(@NotNull ParserInfo info1, @NotNull ParserInfo info2, boolean success) {
        if (info1.position < info2.position) {
            if (success == info2.success) {
                return info2;
            } else {
                return new ParserInfo(info2.position, info2.expected, success);
            }
        } else if (info1.position < info2.position) {
            return info1;
        } else {
            int position = info1.position;
            LinkedHashSet<String> expected = new LinkedHashSet<String>();
            Collections.addAll(expected, info1.expected);
            Collections.addAll(expected, info2.expected);
            return new ParserInfo(position, expected.toArray(new String[expected.size()]), success);
        }
    }

    @Override
    public String toString() {
        if (expected.length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expecting ");
            for (int i = 0; i < expected.length - 2; i++) {
                sb.append(expected[i]).append(", ");
            }
            if (expected.length >= 2) {
                sb.append(expected[expected.length - 2]).append(" or ");
            }

            sb.append(expected[expected.length - 1]);
            return sb.toString();
        }
        return "Error";
    }
}
