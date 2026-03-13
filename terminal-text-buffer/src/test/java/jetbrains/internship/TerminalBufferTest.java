package jetbrains.internship;

import jetbrains.internship.enums.Style;
import jetbrains.internship.enums.TerminalColor;
import jetbrains.internship.model.Cell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Terminal Buffer Specifications")
class TerminalBufferTest {

    private TerminalBuffer buffer;
    private static final int WIDTH = 10;
    private static final int HEIGHT = 5;
    private static final int MAX_SCROLLBACK = 3;

    @BeforeEach
    void setUp() {
        // Initialize a new buffer before EACH test to keep tests isolated
        buffer = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK);
    }

    @Nested
    @DisplayName("Cursor Movement & Boundaries")
    class CursorTests {

        @Test
        @DisplayName("Cursor should not move beyond the left and top edges")
        void cursorShouldNotMoveBeyondTopLeft() {
            buffer.moveCursor(-5, -5);
            assertEquals(0, buffer.getCursorCol());
            assertEquals(0, buffer.getCursorRow());
        }

        @Test
        @DisplayName("Cursor should not move beyond the right and bottom edges")
        void cursorShouldNotMoveBeyondBottomRight() {
            buffer.moveCursor(WIDTH + 5, HEIGHT + 5);
            assertEquals(WIDTH - 1, buffer.getCursorCol());
            assertEquals(HEIGHT - 1, buffer.getCursorRow());
        }
    }

    @Nested
    @DisplayName("Writing & Attributes")
    class WritingTests {

        @Test
        @DisplayName("Writing text should advance cursor correctly")
        void writingTextShouldAdvanceCursor() {
            buffer.write("Hello");
            assertEquals(5, buffer.getCursorCol());
            assertEquals(0, buffer.getCursorRow());
        }

        @Test
        @DisplayName("Written text should apply current attributes")
        void writtenTextShouldHaveCorrectAttributes() {
            buffer.setAttributes(TerminalColor.RED, TerminalColor.BLACK, EnumSet.of(Style.BOLD));
            buffer.write("A");

            Cell writtenCell = buffer.getCellAt(0, 0, false);
            assertEquals('A', writtenCell.character());
            assertEquals(TerminalColor.RED, writtenCell.fgColor());
            assertEquals(TerminalColor.BLACK, writtenCell.bgColor());
            assertTrue(writtenCell.styles().contains(Style.BOLD));
        }

        @Test
        @DisplayName("Text exceeding screen width should wrap to the next line")
        void textShouldWrapToNextLine() {
            buffer.write("1234567890A"); // 11 characters on a width of 10

            assertEquals(1, buffer.getCursorCol()); // Cursor moves to the second position in the new line
            assertEquals(1, buffer.getCursorRow()); // Cursor is on the second row

            Cell firstCellNextLine = buffer.getCellAt(0, 1, false);
            assertEquals('A', firstCellNextLine.character());
        }
    }

    @Nested
    @DisplayName("Scrolling & Scrollback History")
    class ScrollingTests {

        @Test
        @DisplayName("Writing past the last line should scroll up and preserve history")
        void writingPastBottomShouldScroll() {

            // Fill the screen completely (HEIGHT rows * WIDTH characters)
            for (int i = 0; i < HEIGHT; i++) {
                buffer.write("Line" + i + "-----");
            }

            // After filling the screen, the next write should trigger scrollUp()
            String scrollbackLine = buffer.getLineAsString(0, true);
            assertTrue(scrollbackLine.startsWith("Line0"), "The first line should be moved to scrollback");

            buffer.write("Overflow");

            // Cursor should remain on the last row
            assertEquals(HEIGHT - 1, buffer.getCursorRow());
            assertEquals(8, buffer.getCursorCol());
        }

        @Test
        @DisplayName("Scrollback should drop the oldest lines when max limit is reached")
        void scrollbackShouldRespectMaxSize() {

            // Write enough lines to exceed the scrollback capacity
            for (int i = 0; i < HEIGHT + 4; i++) {
                buffer.write("L" + i + "--------");
            }

            // The oldest lines (L0, L1) should be dropped when the limit is exceeded
            String oldestInScrollback = buffer.getLineAsString(0, true);
            assertTrue(oldestInScrollback.startsWith("L2"),
                    "L0 and L1 should be dropped, L2 should be the oldest in scrollback");
        }
    }

    @Nested
    @DisplayName("Screen Clearing")
    class ClearingTests {

        @Test
        @DisplayName("Clearing screen should reset all cells to empty and move cursor to 0,0")
        void clearScreenShouldEmptyCells() {
            buffer.write("Test");
            buffer.clearScreen();

            assertEquals(0, buffer.getCursorCol());
            assertEquals(0, buffer.getCursorRow());
            assertEquals(Cell.EMPTY, buffer.getCellAt(0, 0, false));
        }
    }
}