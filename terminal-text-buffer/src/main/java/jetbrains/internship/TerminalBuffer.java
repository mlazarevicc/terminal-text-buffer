package jetbrains.internship;

import jetbrains.internship.enums.Style;
import jetbrains.internship.enums.TerminalColor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;

public class TerminalBuffer {
    private final int width;
    private final int height;
    private final int maxScrollback;

    // Ring buffer for O(1) queue access and O(1) scrolling
    private final Line[] screen;
    private int screenTopIndex = 0;

    // Deque for scrollback history
    private final Deque<Line> scrollback;

    private int cursorCol = 0;
    private int cursorRow = 0;

    private TerminalColor currentFg = TerminalColor.DEFAULT;
    private TerminalColor currentBg = TerminalColor.DEFAULT;
    private EnumSet<Style> currentStyles = EnumSet.noneOf(Style.class);

    public TerminalBuffer(int width, int height, int maxScrollback) {
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;
        this.screen = new Line[height];
        this.scrollback = new ArrayDeque<>(maxScrollback);

        for (int i = 0; i < height; i++) {
            screen[i] = new Line(width);
        }
    }

    public void setAttributes(TerminalColor fg, TerminalColor bg, EnumSet<Style> styles) {
        this.currentFg = fg != null ? fg : TerminalColor.DEFAULT;
        this.currentBg = bg != null ? bg : TerminalColor.DEFAULT;
        this.currentStyles = styles != null ? EnumSet.copyOf(styles) : EnumSet.noneOf(Style.class);
    }

    public int getCursorCol() { return cursorCol; }
    public int getCursorRow() { return cursorRow; }

    public void setCursorPosition(int col, int row) {
        this.cursorCol = Math.max(0, Math.min(col, width - 1));
        this.cursorRow = Math.max(0, Math.min(row, height - 1));
    }

    public void moveCursor(int dCol, int dRow) {
        setCursorPosition(cursorCol + dCol, cursorRow + dRow);
    }
}