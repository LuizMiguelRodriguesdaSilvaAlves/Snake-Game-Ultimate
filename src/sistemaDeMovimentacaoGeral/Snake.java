package sistemaDeMovimentacaoGeral;

import java.awt.Point;
import java.util.Deque;
import java.util.LinkedList;

import exceptions.MovementException;
import personalizacaoCobra.SnakeSkin;
import personalizacaoCobra.SnakeSkinManager;

/** Models the player snake and owns its body, direction, growth and movement rules. */
public class Snake {
    private Deque<Point> body;
    private Direction direction;
    private boolean growNextMove = false;

    // The selected skin is synchronized with the global skin manager.
    private SnakeSkin skin;

    /** Creates a contiguous snake body extending behind its initial direction. */
    public Snake(Point initialHead, Direction initialDirection, int initialLength) {
        if (initialHead == null || initialDirection == null || initialLength < 1) {
            throw new MovementException("Initial snake position, direction and length must be valid.");
        }
        body = new LinkedList<>();
        direction = initialDirection;
        this.skin = SnakeSkinManager.getSkin(); // pega skin atual globalmente

        body.addFirst(new Point(initialHead));
        for (int i = 1; i < initialLength; i++) {
            Point prev = body.getLast();
            Point newSeg = new Point(prev.x, prev.y);
            // Add exactly one cell behind the previous segment to keep the body contiguous.
            switch (initialDirection) {
                case UP:    newSeg.y += 1; break;
                case DOWN:  newSeg.y -= 1; break;
                case LEFT:  newSeg.x += 1; break;
                case RIGHT: newSeg.x -= 1; break;
            }
            body.addLast(newSeg);
        }
    }

    /** Returns the currently applied snake skin. */
    public SnakeSkin getSkin() {
        return skin;
    }

    /** Applies a non-null skin and updates the shared skin manager. */
    public void setSkin(SnakeSkin skin) {
        if (skin == null) throw new IllegalArgumentException("Snake skin cannot be null.");
        this.skin = skin;
        SnakeSkinManager.setSkin(skin);
    }

    /** Returns the current head coordinate. */
    public Point getHead() {
        return body.peekFirst();
    }

    /** Returns the body deque, ordered from head to tail. */
    public Deque<Point> getBody() {
        return body;
    }

    /** Returns the current movement direction. */
    public Direction getDirection() {
        return direction;
    }

    /** Changes direction unless the requested direction is directly opposite. */
    public void setDirection(Direction newDir) {
        if (newDir == null) return;
        if (!newDir.isOpposite(this.direction)) this.direction = newDir;
    }

    /** Adds one body segment on the next movement update. */
    public void grow() { growNextMove = true; }

    /** Moves one cell in the current direction without changing board boundaries. */
    public Point move() {
        return move(false, 0, 0);
    }

    /** Moves one cell and wraps the head around the board when wrapAround is enabled. */
    public Point move(boolean wrapAround, int columns, int rows) {
        if (direction == null) throw new MovementException("Snake direction has not been defined.");
        Point head = getHead();
        Point newHead = new Point(head.x, head.y);
        switch (direction) {
            case UP:    newHead.y -= 1; break;
            case DOWN:  newHead.y += 1; break;
            case LEFT:  newHead.x -= 1; break;
            case RIGHT: newHead.x += 1; break;
        }
        if (wrapAround && columns > 0 && rows > 0) {
            newHead.x = (newHead.x + columns) % columns;
            newHead.y = (newHead.y + rows) % rows;
        }
        body.addFirst(newHead);
        if (!growNextMove) body.removeLast();
        else growNextMove = false;
        return newHead;
    }

    /** Removes up to the requested number of tail segments without deleting the head. */
    public int shrink(int segments) {
        if (segments < 0) throw new MovementException("Shrink amount cannot be negative.");
        int removed = 0;
        while (removed < segments && body.size() > 1) {
            body.removeLast();
            removed++;
        }
        growNextMove = false;
        return removed;
    }

    /** Checks whether a prospective head coordinate overlaps any body segment. */
    public boolean collidesWithSelf(Point p) {
        int i = 0;
        for (Point seg : body) {
            if (i > 0 && seg.equals(p)) return true;
            i++;
        }
        return false;
    }
    
    /** Translates the entire snake while preserving the relative body shape. */
    public void teleportTo(Point newHead) {
        if (newHead == null) {
            throw new MovementException("The new head position cannot be null.");
        }

        Point oldHead = getHead();

        int dx = newHead.x - oldHead.x;
        int dy = newHead.y - oldHead.y;

        Deque<Point> newBody = new LinkedList<>();

        for (Point segment : body) {
            newBody.addLast(new Point(segment.x + dx, segment.y + dy));
        }

        body = newBody;
    }
}
