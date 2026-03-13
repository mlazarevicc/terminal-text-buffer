package jetbrains.internship;

import jetbrains.internship.enums.Style;
import jetbrains.internship.enums.TerminalColor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record Cell(char character, TerminalColor fgColor, TerminalColor bgColor, Set<Style> styles) {

    // Memory optimization: We use one object for all empty cells
    public static final Cell EMPTY = new Cell(' ', TerminalColor.DEFAULT, TerminalColor.DEFAULT, Collections.emptySet());

    public static Cell of(char character, TerminalColor fgColor, TerminalColor bgColor, Set<Style> styles) {
        if (character == ' ' && fgColor == TerminalColor.DEFAULT && bgColor == TerminalColor.DEFAULT && (styles == null || styles.isEmpty())) {
            return EMPTY;
        }
        Set<Style> safeStyles = (styles == null || styles.isEmpty()) ? Collections.emptySet() : EnumSet.copyOf(styles);
        return new Cell(character, fgColor, bgColor, safeStyles);
    }
}
