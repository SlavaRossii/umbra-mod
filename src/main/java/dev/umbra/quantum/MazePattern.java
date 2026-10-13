package dev.umbra.quantum;

/**
 * One way a quantum maze can change. Patterns only ever touch walls nobody is looking at
 * ({@link MazeContext#set} enforces that). Add new ones to {@link MazePatterns#ALL}.
 */
public interface MazePattern {
    /** Stable id, used by /umbra mazepattern and in translations (maze.umbra.pattern.&lt;id&gt;). */
    String id();

    /** Called once when the maze switches to this pattern. */
    default void start(MazeContext ctx) {}

    /** Called every maze tick (twice a second) while players are inside. */
    void step(MazeContext ctx);
}
