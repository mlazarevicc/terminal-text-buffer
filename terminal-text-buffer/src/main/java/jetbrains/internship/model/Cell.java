package jetbrains.internship.model;

import jetbrains.internship.enums.Style;
import jetbrains.internship.enums.TerminalColor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record Cell(String text, int displayWidth, TerminalColor fgColor, TerminalColor bgColor, Set<Style> styles) {

    // Standard empty cell (width = 1)
    public static final Cell EMPTY =
            new Cell(" ", 1, TerminalColor.DEFAULT, TerminalColor.DEFAULT, Collections.emptySet());

    // Placeholder cell used as the second half of a wide character (width = 0)
    public static final Cell WIDE_FILLER =
            new Cell("", 0, TerminalColor.DEFAULT, TerminalColor.DEFAULT, Collections.emptySet());

    public static Cell of(String text, int displayWidth, TerminalColor fgColor, TerminalColor bgColor, Set<Style> styles) {
        // Reuse EMPTY instance for default cells to reduce allocations
        if (" ".equals(text) && displayWidth == 1 &&
                fgColor == TerminalColor.DEFAULT && bgColor == TerminalColor.DEFAULT &&
                (styles == null || styles.isEmpty())) {
            return EMPTY;
        }

        Set<Style> safeStyles =
                (styles == null || styles.isEmpty()) ? Collections.emptySet() : EnumSet.copyOf(styles);

        return new Cell(text, displayWidth, fgColor, bgColor, safeStyles);
    }
}
