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
    private int width;
    private int height;
    private final int maxScrollback;

    // Ring buffer for O(1) queue access and O(1) scrolling
    private Line[] screen;
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

    private Cell createCurrentCell(String text, int displayWidth) {
        return Cell.of(text, displayWidth, currentFg, currentBg, currentStyles);
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

    /**
     * Determines the display width of a Unicode code point.
     * For simplicity:
     * - CJK Ideographs and emojis take 2 columns.
     * - All other characters take 1 column.
     * In real terminals, a POSIX wcwidth() lookup table would be used.
     */
    private int getDisplayWidth(int codePoint) {
        if (codePoint >= 0x4E00 && codePoint <= 0x9FFF) return 2; // CJK Ideographs
        if (codePoint > 0xFFFF) return 2; // Emojis / Surrogate Pairs
        return 1; // Regular letters/numbers
    }

    // EDIT METHODS
    public void write(String text) {
        // Iterate over actual Unicode code points, not Java chars
        text.codePoints().forEach(codePoint -> {
            String symbol = new String(Character.toChars(codePoint));
            int charWidth = getDisplayWidth(codePoint);

            // Edge case: wide character at the last column
            if (charWidth == 2 && cursorCol == width - 1) {
                getScreenLine(cursorRow).setCell(cursorCol, Cell.EMPTY); // Clear leftover
                cursorCol = 0;
                cursorRow++;
                if (cursorRow >= height) {
                    scrollUp();
                    cursorRow = height - 1;
                }
            }

            Line currentLine = getScreenLine(cursorRow);
            currentLine.setCell(cursorCol, createCurrentCell(symbol, charWidth));

            if (charWidth == 2) {
                // Place invisible filler to reserve space for wide char
                currentLine.setCell(cursorCol + 1, Cell.WIDE_FILLER);
            }

            cursorCol += charWidth;

            // Standard word wrap
            if (cursorCol >= width) {
                cursorCol = 0;
                cursorRow++;
                if (cursorRow >= height) {
                    scrollUp();
                    cursorRow = height - 1;
                }
            }
        });
    }

    public void insert(String text) {
        text.codePoints().forEach(codePoint -> {
            String symbol = new String(Character.toChars(codePoint));
            int charWidth = getDisplayWidth(codePoint);

            // Edge case for wide char at the last column
            if (charWidth == 2 && cursorCol == width - 1) {
                getScreenLine(cursorRow).setCell(cursorCol, Cell.EMPTY);
                cursorCol = 0;
                cursorRow++;
                if (cursorRow >= height) {
                    scrollUp();
                    cursorRow = height - 1;
                }
            }

            Line currentLine = getScreenLine(cursorRow);

            // Shift array for wide characters when inserting
            if (charWidth == 2) {
                currentLine.insertCellAt(cursorCol, Cell.WIDE_FILLER);
            }
            currentLine.insertCellAt(cursorCol, createCurrentCell(symbol, charWidth));

            cursorCol += charWidth;

            if (cursorCol >= width) {
                cursorCol = 0;
                cursorRow++;
                if (cursorRow >= height) {
                    scrollUp();
                    cursorRow = height - 1;
                }
            }
        });
    }

    public void fillLine(String s) {
        Line currentLine = getScreenLine(cursorRow);
        if (s == null || s.isEmpty()) {
            currentLine.fill(Cell.EMPTY);
            return;
        }

        int codePoint = s.codePointAt(0);
        String symbol = new String(Character.toChars(codePoint));
        int charWidth = getDisplayWidth(codePoint);

        // Fill entire line with repeated symbol
        for (int i = 0; i < width; i += charWidth) {
            currentLine.setCell(i, createCurrentCell(symbol, charWidth));
            if (charWidth == 2 && i + 1 < width) {
                currentLine.setCell(i + 1, Cell.WIDE_FILLER);
            }
        }
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

    // RESIZE METHODS
    private void copyLineData(Line src, Line dest, int copyWidth) {
        for (int i = 0; i < copyWidth; i++) {
            dest.setCell(i, src.getCell(i));
        }
    }

    public void resize(int newWidth, int newHeight) {
        if (newWidth < 1 || newHeight < 1) return;
        if (this.width == newWidth && this.height == newHeight) return;

        Line[] newScreen = new Line[newHeight];
        for (int i = 0; i < newHeight; i++) newScreen[i] = new Line(newWidth);

        if (newHeight < this.height) {
            // Preserve top lines removed due to shrinking
            int linesToPush = this.height - newHeight;
            for (int i = 0; i < linesToPush; i++) {
                if (maxScrollback > 0) {
                    if (scrollback.size() >= maxScrollback) scrollback.removeFirst();
                    scrollback.addLast(getScreenLine(i));
                }
            }
            for (int i = 0; i < newHeight; i++) {
                copyLineData(getScreenLine(i + linesToPush), newScreen[i], Math.min(this.width, newWidth));
            }
            this.cursorRow = Math.max(0, this.cursorRow - linesToPush);
        } else {
            for (int i = 0; i < this.height; i++) {
                copyLineData(getScreenLine(i), newScreen[i], Math.min(this.width, newWidth));
            }
        }

        this.width = newWidth;
        this.height = newHeight;
        this.screen = newScreen;
        this.screenTopIndex = 0;

        // Clamp cursor to screen
        if (this.cursorCol >= this.width) this.cursorCol = this.width - 1;
        if (this.cursorRow >= this.height) this.cursorRow = this.height - 1;
    }
}