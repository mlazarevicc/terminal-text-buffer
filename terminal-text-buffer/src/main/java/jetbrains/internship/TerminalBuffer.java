package jetbrains.internship;

import jetbrains.internship.enums.Style;
import jetbrains.internship.enums.TerminalColor;
import jetbrains.internship.model.Cell;
import jetbrains.internship.model.Line;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;

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

    // HELPER METHODS
    private Line getScreenLine(int row) {
        return screen[(screenTopIndex + row) % height];
    }

    private Cell createCurrentCell(char c) {
        return Cell.of(c, currentFg, currentBg, currentStyles);
    }

    private void scrollUp() {
        Line top = screen[screenTopIndex];
        if (maxScrollback > 0) {
            if (scrollback.size() >= maxScrollback) {
                scrollback.removeFirst(); // Remove the oldest history entry (memory management)
            }
            scrollback.addLast(top);
        }
        // Replace the top line with a new empty line and move the start of the ring buffer
        screen[screenTopIndex] = new Line(width);
        screenTopIndex = (screenTopIndex + 1) % height;
    }

    // EDIT METHODS
    public void write(String text) {
        Line currentLine = getScreenLine(cursorRow);
        for (char c : text.toCharArray()) {
            currentLine.setCell(cursorCol, createCurrentCell(c));
            cursorCol++;
            if (cursorCol >= width) {
                cursorCol = 0;
                cursorRow++;
                if (cursorRow >= height) {
                    scrollUp();
                    cursorRow = height - 1;
                }
                currentLine = getScreenLine(cursorRow);
            }
        }
    }

    public void insert(String text) {
        Line currentLine = getScreenLine(cursorRow);
        for (char c : text.toCharArray()) {
            currentLine.insertCellAt(cursorCol, createCurrentCell(c));
            cursorCol++;
            if (cursorCol >= width) {
                cursorCol = 0;
                cursorRow++;
                if (cursorRow >= height) {
                    scrollUp();
                    cursorRow = height - 1;
                }
                currentLine = getScreenLine(cursorRow);
            }
        }
    }

    public void fillLine(char c) {
        getScreenLine(cursorRow).fill(createCurrentCell(c));
    }

    public void insertEmptyLineAtBottom() {
        scrollUp();
    }

    public void clearScreen() {
        for (int i = 0; i < height; i++) {
            screen[i].fill(Cell.EMPTY);
        }
        setCursorPosition(0, 0);
    }

    public void clearScreenAndScrollback() {
        clearScreen();
        scrollback.clear();
    }

    // CONTENT READING
    public Cell getCellAt(int col, int row, boolean fromScrollback) {
        if (fromScrollback) {
            if (row < 0 || row >= scrollback.size()) return Cell.EMPTY;
            Iterator<Line> it = scrollback.iterator();
            for(int i = 0; i < row; i++) it.next();
            return it.next().getCell(col);
        } else {
            if (row < 0 || row >= height) return Cell.EMPTY;
            return getScreenLine(row).getCell(col);
        }
    }

    public String getLineAsString(int row, boolean fromScrollback) {
        if (fromScrollback) {
            if (row < 0 || row >= scrollback.size()) return "";
            Iterator<Line> it = scrollback.iterator();
            for(int i = 0; i < row; i++) it.next();
            return it.next().toStringRepresentation();
        } else {
            if (row < 0 || row >= height) return "";
            return getScreenLine(row).toStringRepresentation();
        }
    }

    public String getScreenContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < height; i++) {
            sb.append(getScreenLine(i).toStringRepresentation()).append(System.lineSeparator());
        }
        return sb.toString();
    }

    public String getFullContent() {
        StringBuilder sb = new StringBuilder();
        for (Line line : scrollback) {
            sb.append(line.toStringRepresentation()).append(System.lineSeparator());
        }
        sb.append(getScreenContent());
        return sb.toString();
    }
}