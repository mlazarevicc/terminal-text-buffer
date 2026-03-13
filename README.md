# IntelliJ Terminal Emulator Buffer - Core Engine

This repository contains the core text buffer implementation for a terminal emulator, developed as part of the JetBrains Internship task ("Integration of an external terminal emulator into the IntelliJ Terminal").

The goal of this project is to implement a highly efficient, memory-safe data structure capable of handling terminal PTY output streams, maintaining cursor states, preserving history (scrollback), and dynamically handling complex edge cases like wide characters (CJK/Emojis) and screen resizing.

## Features Implemented
* **Core Buffer Operations:** Configurable dimensions, robust cursor boundary management, and strict separation of the editable Screen and unmodifiable Scrollback history.
* **Editing & State:** Full support for writing, inserting, line filling, and clearing, with respect to current foreground, background, and text styles.
* **Content Access API:** Clean getters designed to feed a decoupled UI rendering engine (e.g., Kotlin/Swing).
* **Bonus - Wide Character Support:** Proper handling of multi-byte Unicode code points (Emojis, CJK characters) using surrogate pairs and grid-aligned `WIDE_FILLER` cells.
* **Bonus - Dynamic Resizing:** Resilient handling of dimension changes with a "Scrollback Preservation" strategy to prevent data loss.

---

## Architectural Decisions

### 1. Ring Buffer for Screen Operations
Instead of using a standard 2D matrix (`Cell[][]`) which requires expensive $O(N)$ array shifting when the screen scrolls, I implemented a **Ring Buffer (Circular Array)** of `Line` objects.
* When the cursor moves past the bottom of the screen, the top line is popped, moved to the scrollback `ArrayDeque`, and a new empty line is placed at the current index.
* **Result:** Screen scrolling is an **$O(1)$** operation, maximizing performance during rapid standard output streams.

### 2. Object Pooling for Empty and Filler Cells
Terminal screens contain a vast amount of empty space. Instead of instantiating thousands of blank `Cell` objects, I implemented an **Object Pool pattern** by creating a single, static `Cell.EMPTY` instance. I used the same approach for `Cell.WIDE_FILLER`. This drastically reduces the memory footprint and relieves pressure on the Java Garbage Collector.

### 3. Wide Characters and Grid Alignment
Handling wide characters (like `🔥` or `字`) requires resolving the mismatch between byte length and visual grid width.
* The buffer iterates over **Code Points**, safely handling Java's UTF-16 surrogate pairs.
* I implemented a heuristic display-width calculator (mimicking the POSIX `wcwidth()` C function).
* Wide characters consume 2 grid columns: the character itself, and a `WIDE_FILLER` ghost cell to reserve spatial layout and prevent overlapping during cursor movements.

### 4. Resize Strategy: Truncation & Scrollback Preservation
Handling terminal resizing involves complex edge cases. Instead of implementing a destructive resize or a highly complex document-reflow engine, I chose a resilient, data-safe approach:
* **Horizontal Resize:** Text is truncated when shrinking, matching basic VT100 grid behavior. 
* **Vertical Resize:** When the screen height is reduced, the top rows that fall out of bounds are safely pushed into the `scrollback` history buffer. This prevents data loss and mimics the behavior of robust native terminal emulators.

---

## Trade-offs

During development, I actively balanced readability, performance, and maintenance. Here are the key trade-offs made:

1. **Objects vs. Primitives (Clean Code vs. Memory):** I modeled the terminal cell as a Java `record` (`Cell`). This provides excellent immutability, type safety, and clean code for this experimental phase. However, the trade-off is higher heap memory usage due to object overhead. In a hyper-optimized production environment (like Alacritty or a low-level native VTE), a cell is often bit-packed into a single 64-bit primitive `long` (e.g., 32 bits for the character code point, 16 bits for colors, 16 bits for attributes) to ensure contiguous memory and CPU cache locality.
2. **Eager vs. Delayed Line Wrapping:** The buffer currently implements "Eager Wrapping" (the cursor wraps immediately upon hitting the right boundary). Modern terminals (like `xterm`) use a "Delayed/Pending Wrap" state machine, where the cursor hangs on the edge until the next printable character is received. I chose eager wrapping to maintain scope and stability, though delayed wrapping would be necessary for perfect edge-case rendering.
3. **Insert Operation (`ICH`) Behavior:** When inserting text into the middle of a line, characters to the right are shifted. Any characters pushed beyond the screen width are truncated. I chose this approach because it accurately mimics the hardware VT100 `ICH` (Insert Character) escape sequence behavior, rather than cascading shifted text to new lines.

---

## Future Improvements & Production Integration

If given the opportunity to integrate this or an external native emulator into the IntelliJ Platform, I would focus on:

1. **Native Bridge Architecting:** If utilizing an external native C/Rust engine (like Alacritty's `vte` crate) as the headless state machine, I would explore using the **Foreign Function & Memory API (Project Panama)** for highly efficient, zero-copy interop, avoiding the heavy overhead of legacy JNI.
2. **Decoupled Rendering Loop:** Ensuring the Kotlin/Swing UI queries the buffer's state independently of the PTY I/O thread, locking the grid only during the exact moment of drawing to prevent IDE UI freezes.
3. **Optimized Scrollback Eviction:** For massive histories (e.g., 100,000+ lines), transitioning from an `ArrayDeque` to a memory-mapped file structure (`MappedByteBuffer`) or a primitive-backed ring buffer to prevent `OutOfMemoryError` crashes during long debugging sessions.

---

## Build and Test

The project uses Maven and JUnit 5. There are zero external dependencies for the core logic.
Tests are grouped logically using `@Nested` classes and named descriptively to serve as living documentation of the expected behaviors (including wide-character edge cases and eager-wrapping math).

```bash
# To run the comprehensive test suite:
mvn clean test
```
