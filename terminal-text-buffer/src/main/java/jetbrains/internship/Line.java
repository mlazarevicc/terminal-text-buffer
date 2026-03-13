package jetbrains.internship;

import java.util.Arrays;

public class Line {
    private final Cell[] cells;
    private final int width;

    public Line(int width) {
        this.width = width;
        this.cells = new Cell[width];
        fill(Cell.EMPTY);
    }

    public void fill(Cell cell) {
        Arrays.fill(cells, cell);
    }

    public void setCell(int index, Cell cell) {
        if (index >= 0 && index < width) {
            cells[index] = cell;
        }
    }

    public Cell getCell(int index) {
        if (index >= 0 && index < width) {
            return cells[index];
        }
        return Cell.EMPTY;
    }

    public void insertCellAt(int index, Cell cell) {
        if (index >= 0 && index < width) {
            // Shifts all characters to the right to make space for insertion
            System.arraycopy(cells, index, cells, index + 1, width - index - 1);
            cells[index] = cell;
        }
    }

    public String toStringRepresentation() {
        StringBuilder sb = new StringBuilder(width);
        for (Cell cell : cells) {
            sb.append(cell.character());
        }
        return sb.toString();
    }
}