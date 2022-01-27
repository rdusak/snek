package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.Random;

public class Snek {
    public static void main(String[] args) {
        GameState gs = new GameState();
        new GameWindow(gs);
    }
}

class GameWindow {
    public GameWindow(GameState state) {
        EventQueue.invokeLater(() -> {
            try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());} 
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) 
                    {ex.printStackTrace();}
            JFrame frame = new JFrame("snek");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new Grid(state));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

class Grid extends JPanel {
    final static int PADDING = 4;
    static int cellWidth, cellHeight, xOffset, yOffset;
    static Graphics2D g2d;
    GameState gs;
    public Grid(GameState state) {
        gs = state; setFocusable(true);
        addKeyListener(new InputHandler(gs));
        new Thread(() -> {
            while (!gs.quit) {
                gs.moveSnake();
                repaint();
                try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}
            }
        }).start();
        new Thread(() -> {
            while (!gs.quit) {
                if (gs.powerup == null)
                    gs.powerup = new Pos(gs);
                if (!gs.snake.contains(gs.powerup))
                    repaint();
                try {Thread.sleep(3000);} catch (InterruptedException e) {e.printStackTrace();}
            }
        }).start();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(700, 700);
    }

    void paintCellFromState(int x, int y, Graphics2D g, Color c) {
        int cellPadding = 1;
        g.setColor(c);
        g.fillRect(xOffset + x * cellWidth + cellPadding,
                yOffset + y * cellHeight + cellPadding,
                cellWidth - cellPadding , cellHeight - cellPadding);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        cellWidth = (getWidth() - PADDING) / gs.width;
        cellHeight = (getHeight() - PADDING) / gs.height;
        xOffset = ((getWidth() - (cellWidth * gs.width)) / 2);
        yOffset = ((getHeight() - (cellHeight * gs.height)) / 2);
        g2d = (Graphics2D) g.create();

        int y = yOffset; // padding
        for (int horz = 0; horz < gs.width; horz++) {
            int x = xOffset; // padding
            for (int vert = 0; vert < gs.height; vert++) {
                g2d.drawRect(x, y, cellWidth, cellHeight);
                x += cellWidth;
            }
            y += cellHeight;
        }
        if (gs.powerup != null) {
            paintCellFromState(gs.powerup.x, gs.powerup.y, g2d, Color.MAGENTA);
        }
        for(var pos : gs.snake) {
            if (pos.equals(gs.powerup) && !pos.equals(gs.snake.get(0)))
                continue;
            paintCellFromState(pos.x, pos.y, g2d, Color.GREEN);
        }
        paintCellFromState(gs.pos_x(), gs.pos_y(), g2d, Color.orange);
        g2d.dispose();
    }
}

class InputHandler extends KeyAdapter {
    GameState gs;
    public InputHandler(GameState g) {gs = g;}

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        switch (key) {
            case KeyEvent.VK_Q -> gs.quit = true;
            case KeyEvent.VK_W -> {
                if (gs.curDir != Dir.DOWN) gs.curDir = Dir.UP;
            }
            case KeyEvent.VK_A -> {
                if (gs.curDir != Dir.RIGHT) gs.curDir = Dir.LEFT;
            }
            case KeyEvent.VK_S -> {
                if (gs.curDir != Dir.UP) gs.curDir = Dir.DOWN;
            }
            case KeyEvent.VK_D -> {
                if (gs.curDir != Dir.LEFT) gs.curDir = Dir.RIGHT;
            }
        }
    }
}

class GameState {
    final static int  DEFAULT_WIDTH     =  10;
    final static int  DEFAULT_HEIGHT    =  10;
    final static int  DEFAULT_POSX      =   0;
    final static int  DEFAULT_POSY      =   0;

    boolean quit;
    int width, height;
    LinkedList<Pos> snake;
    Dir curDir;
    Pos powerup;

    public GameState(int w, int h) {
        width = w; height = h;
        quit = false; curDir = Dir.RIGHT; powerup = null;
        snake = new LinkedList<>();
        snake.add(new Pos(DEFAULT_POSX, DEFAULT_POSY));
        snake.add(new Pos(0 ,1));
        snake.add(new Pos(0 ,2));
    }
    public GameState() {this(DEFAULT_WIDTH, DEFAULT_HEIGHT);}
    public int pos_x() {return this.snake.get(0).x;}
    public int pos_y() {return this.snake.get(0).y;}
    public void pos_x(int i) {this.snake.get(0).x = i;}
    public void pos_y(int i) {this.snake.get(0).y = i;}

    public void moveSnake(Dir dir, int step) {
        Pos old = new Pos(pos_x(), pos_y());
        switch (dir) { // head
            case UP -> pos_y(pos_y() - step);
            case RIGHT -> pos_x(pos_x() + step);
            case DOWN -> pos_y(pos_y() + step);
            case LEFT -> pos_x(pos_x() - step);
        }
        if (pos_x() > width - 1) pos_x(0);
        if (pos_x() < 0)  pos_x(width - 1);
        if (pos_y() > height - 1) pos_y(0);
        if (pos_y() < 0)  pos_y(height - 1);

        if (powerup != null && powerup.equals(snake.get(0))) {
            powerup = null;
            snake.add(1, old);
        } else {
            Pos last = snake.removeLast();
            last.x = old.x;
            last.y = old.y;
            snake.add(1, last);
        }
        for (int i = snake.size() - 1; i > 1; i--) {
            if (snake.get(0).equals(snake.get(i))) {
                quit = true;
                break;
            }
        }
    }
    public void moveSnake() {this.moveSnake(curDir, 1);}
}

enum Dir {UP, DOWN, LEFT, RIGHT}

class Pos {
    int x,y; public Pos(int ix, int iy) {x = ix; y = iy;}
    public Pos(GameState gs) {
        Random random = new Random();
        x = random.nextInt(gs.width);
        y = random.nextInt(gs.height);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pos pos = (Pos) o;
        return x == pos.x && y == pos.y;
    }
}
