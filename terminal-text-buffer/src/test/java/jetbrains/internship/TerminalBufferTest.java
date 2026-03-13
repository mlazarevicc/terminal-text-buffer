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
            assertEquals("A", writtenCell.text());
            assertEquals(TerminalColor.RED, writtenCell.fgColor());
            assertEquals(TerminalColor.BLACK, writtenCell.bgColor());
            assertTrue(writtenCell.styles().contains(Style.BOLD));
        }

        @Test
        @DisplayName("Text exceeding screen width should wrap to the next line")
        void textShouldWrapToNextLine() {
            buffer.write("1234567890A"); // 11 characters, width is 10

            assertEquals(1, buffer.getCursorCol());
            assertEquals(1, buffer.getCursorRow());

            Cell firstCellNextLine = buffer.getCellAt(0, 1, false);
            assertEquals("A", firstCellNextLine.text());
        }
    }

    @Nested
    @DisplayName("Scrolling & Scrollback History")
    class ScrollingTests {

        @Test
        @DisplayName("Writing past the last line should scroll up and preserve history")
        void writingPastBottomShouldScroll() {
            for (int i = 0; i < HEIGHT; i++) {
                buffer.write("Line" + i + "-----");
            }

            String scrollbackLine = buffer.getLineAsString(0, true);
            assertTrue(scrollbackLine.startsWith("Line0"));

            buffer.write("Overflow");
            assertEquals(HEIGHT - 1, buffer.getCursorRow());
            assertEquals(8, buffer.getCursorCol());
        }

        @Test
        @DisplayName("Scrollback should drop the oldest lines when max limit is reached")
        void scrollbackShouldRespectMaxSize() {
            for (int i = 0; i < HEIGHT + 4; i++) {
                buffer.write("L" + i + "--------");
            }

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

    @Nested
    @DisplayName("Wide Characters (CJK & Emojis)")
    class WideCharacterTests {

        @Test
        @DisplayName("Writing a wide character should consume two columns and place a WIDE_FILLER")
        void writingWideCharConsumesTwoColumns() {
            buffer.write("字"); // CJK character
            assertEquals(2, buffer.getCursorCol());

            Cell firstCell = buffer.getCellAt(0, 0, false);
            Cell secondCell = buffer.getCellAt(1, 0, false);

            assertEquals("字", firstCell.text());
            assertEquals(2, firstCell.displayWidth());
            assertEquals(Cell.WIDE_FILLER, secondCell, "Second cell must be a filler to reserve grid space");
        }

        @Test
        @DisplayName("Writing a wide character at the edge should wrap entirely to the next line")
        void writingWideCharAtEdgeWrapsEntirely() {
            buffer.setCursorPosition(WIDTH - 1, 0); // Last column
            buffer.write("🔥"); // Emoji occupies 2 columns

            assertEquals(2, buffer.getCursorCol());
            assertEquals(1, buffer.getCursorRow());

            assertEquals(Cell.EMPTY, buffer.getCellAt(WIDTH - 1, 0, false));
            assertEquals("🔥", buffer.getCellAt(0, 1, false).text());
            assertEquals(Cell.WIDE_FILLER, buffer.getCellAt(1, 1, false));
        }
    }
}