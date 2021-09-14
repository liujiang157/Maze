package midautumn.robot;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;


public class World {
    private int width, height;
    private char[][] terrain;
    private final char FIRST_TERRAIN = ' ', LAST_TERRAIN = '~'; //地图上能展示的ASCII 方便用于机器人说话
    private Position[] spawnMarker = new Position[10]; //from digits 0 to 9

    private List<Robot> robots = new ArrayList<>();
    private Controller controller = new Controller(this);
    private Renderer renderer;

    private int targetFrametime = 16;
    private boolean pause = true;
    private long currentFrame = 0;
    private long pauseAtFrame = -1; //will pause if current frame equals this, set to <0 to not use

    private Method work, memoryToString;


    //Controller class listens to and stores all keyboard and mouse input events and has getters to ask for the state
    public static class Controller extends MouseAdapter implements KeyListener {
        /// Internal Attributes
        private World world;
        private Map<Integer, Boolean> isKeyPressed = new HashMap<>();
        private boolean[] isMousePressed = new boolean[3]; // 0: M1, 1: M2, 2: M3
        private Position mousePos = new Position();
        private double zoomSpeed = 1.05;

        public Controller(World world) {
            this.world = world;
        }

        /// Status Getters: Use these to check mouse and keyboard status
        public boolean isKeyPressed(int vkCode) {
            return isKeyPressed.getOrDefault(vkCode, false);
        }

        public boolean isLeftMousePressed() {
            return isMousePressed[0];
        }

        public boolean isRightMousePressed() {
            return isMousePressed[1];
        }

        public boolean isMiddleMousePressed() {
            return isMousePressed[2];
        }

        public Position getMousePos() {
            return new Position(mousePos);
        }


        /// Event Handlers: The GUI uses these methods to handle user input and remember the states
        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            isKeyPressed.put(e.getKeyCode(), true);

            //default keybindings for the UI buttons, you can remove or change these if you want
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                world.pause = world.currentFrame != world.pauseAtFrame && !world.pause;
                world.pauseAtFrame = -1;
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                world.pauseAtFrame = world.currentFrame + 1;
                world.pause = false;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            isKeyPressed.put(e.getKeyCode(), false);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            isMousePressed[e.getButton() - 1] = true;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            isMousePressed[e.getButton() - 1] = false;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mousePos.x = (e.getX() - world.renderer.cameraX) / world.renderer.scale;
            mousePos.y = (e.getY() - world.renderer.cameraY) / world.renderer.scale;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            world.renderer.scale = Math.max(world.renderer.minScale, Math.min(world.renderer.maxScale,
                    world.renderer.scale * Math.pow(zoomSpeed, -e.getPreciseWheelRotation())));
            //render
            world.renderer.frame.revalidate();
            world.renderer.frame.repaint();
        }
    }


    //constructs the world from the "2D" String
    public World(String mapData) {
        this(mapData, true);
    }

    //you should probably leave shouldRender to true, otherwise you will not see anything
    public World(String mapData, boolean shouldRender) {
        if (shouldRender)
            renderer = new Renderer();

        width = mapData.lines().mapToInt(String::length).max().orElseThrow();
        height = (int) mapData.lines().count();

        terrain = new char[width][height];
        String[] rows = mapData.lines().map(s -> s + " ".repeat(width - s.length())).toArray(String[]::new);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);
                if ('0' <= c && c <= '9')
                    spawnMarker[c - '0'] = new Position(x + 0.5, y + 0.5);
                terrain[x][y] = c;
            }
        }

        try {
            work = Robot.class.getMethod("work");
            memoryToString = Robot.class.getMethod("memoryToString");
        } catch (NoSuchMethodException e) {
            /* student has not implemented these methods yet */
        }
    }

    public Controller getController() {
        return controller;
    }

    public List<Robot> getRobots() {
        return robots;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public char getTerrain(Position pos) {
        return getTerrain(pos.x, pos.y);
    }

    public char getTerrain(double x, double y) {
        int xR = (int) Math.floor(x), yR = (int) Math.floor(y); //we NEED to floor, for negative numbers!

        //it's only SPACE out there when you leave the world...
        return 0 <= xR && xR < width && 0 <= yR && yR < height ? terrain[xR][yR] : ' ';
    }

    public boolean setTerrain(Position pos, char newType) {
        int xR = (int) Math.floor(pos.x), yR = (int) Math.floor(pos.y); //we NEED to floor, for negative numbers!

        //can't paint out of bounds
        if (!(0 <= xR && xR < width && 0 <= yR && yR < height))
            return false;

        //you can't put a wall where a robot is
        if (newType == '#')
            for (Robot robot : robots) {
                double dist = robot.getPosition().distanceTo(new Position(xR + 0.5, yR + 0.5));
                if (dist < robot.getSize() / 2 + 0.5)
                    return false;
            }

        terrain[xR][yR] = newType;
        return true;
    }

    public void say(Robot robot, String str) {
        if (renderer != null) {
            renderer.speechBubble.put(robot, str);
            renderer.speechTimer.put(robot, renderer.speechBubbleTimeout);
        } else
            System.out.println(robot.getName() + ": \"" + str + "\"");
    }

    public Position spawnRobotAt(Robot robot, char spawnMarker) {
        robots.add(robot);
        if (renderer != null) {
            renderer.movingSpeed.put(robot, 0);
            renderer.movingTime.put(robot, 0);
            renderer.speechTimer.put(robot, 0);
            renderer.speechBubble.put(robot, "");
        }
        if ('0' <= spawnMarker && spawnMarker <= '9') {
            if (this.spawnMarker[spawnMarker - '0'] != null)
                return this.spawnMarker[spawnMarker - '0'];
            else
                throw new IllegalArgumentException("Spawn marker " + spawnMarker + " not found on the map!");
        } else
            throw new IllegalArgumentException("Invalid spawn marker '" + spawnMarker + "', it should be a single digit!");
    }

    public boolean resolveCollision(Robot robot, Position pos) {
        if (renderer != null)
            renderer.movingSpeed.put(robot, renderer.movingSpeed.get(robot) + 1);

        double radius = robot.getSize() / 2;
        boolean xEdge = Math.ceil(pos.x - radius) == Math.floor(pos.x + radius), //this edge (vertical axis) intersects
                yEdge = Math.ceil(pos.y - radius) == Math.floor(pos.y + radius); //this edge (horizontal axis) intersects

        Position intersection = new Position(
                xEdge ? Math.ceil(pos.x - radius) : pos.x,
                yEdge ? Math.ceil(pos.y - radius) : pos.y);

        boolean horizontallyBlocked = false, verticallyBlocked = false;

        //"edge" case
        if (xEdge ^ yEdge) {
            boolean blocked = (getTerrain(intersection) == '#')
                    ^ (getTerrain(intersection.x + (xEdge ? -1 : 0), intersection.y + (yEdge ? -1 : 0)) == '#');

            horizontallyBlocked = blocked && yEdge;
            verticallyBlocked = blocked && xEdge;
        }
        //"corner" case
        else if (xEdge && yEdge) {
            //NW, NE, SE, SW
            double[] dx = {-0.5, +0.5, +0.5, -0.5};
            double[] dy = {-0.5, -0.5, +0.5, +0.5};
            boolean[] blocked = new boolean[4];
            int numBlocked = 0;
            for (int dir = 0; dir < 4; dir++)
                if (blocked[dir] = (getTerrain(intersection.x + dx[dir], intersection.y + dy[dir]) == '#'))
                    numBlocked++;

            //inner corner or smooth edge between two cells
            if (numBlocked >= 2) {
                horizontallyBlocked = blocked[0] == blocked[1];
                verticallyBlocked = blocked[0] == blocked[3];
                for (int i = 0; i < 4; i++)
                    if (blocked[i] && blocked[(i + 2) % 4]) {
                        horizontallyBlocked = verticallyBlocked = true;
                        break;
                    }
            }
            //outer corner
            else if (numBlocked == 1) {
                intersection.x = blocked[0] || blocked[3] ? Math.min(pos.x, intersection.x) : Math.max(pos.x, intersection.x);
                intersection.y = blocked[0] || blocked[1] ? Math.min(pos.y, intersection.y) : Math.max(pos.y, intersection.y);
                boolean isColliding = pos.distanceTo(intersection) <= radius;
                if (isColliding)
                    pos.moveBy(radius - intersection.distanceTo(pos), intersection.directionTo(pos));
                return isColliding;
            }
        }

        if (horizontallyBlocked)
            pos.y = intersection.y - (pos.y < intersection.y ? radius : -radius);
        if (verticallyBlocked)
            pos.x = intersection.x - (pos.x < intersection.x ? radius : -radius);
        return horizontallyBlocked || verticallyBlocked;
    }


    public void run() {
        if(renderer != null)
            renderer.setup();

        try {
            while (true) {
                if(renderer != null) {
                    // wait until unpaused
                    while (pause || currentFrame == pauseAtFrame)
                        Thread.sleep(50);

                    long before = System.currentTimeMillis();

                    simulateFrame();
                    renderer.render();

                    long frametime = System.currentTimeMillis() - before;
                    long sleepTime = targetFrametime - frametime;
                    if (sleepTime > 0)
                        Thread.sleep(sleepTime);
                } else
                    simulateFrame();
            }
        } catch (InterruptedException e) {
            /* Intentionally left blank */
        }
    }

    public void simulateFrame() {
        currentFrame++;

        for (Robot robot : robots) {
            try {
                if (work != null)
                    work.invoke(robot);
            } catch (IllegalAccessException e) {
                /* intentionally left blank: student has not implemented Robot.work() method yet */
            } catch (InvocationTargetException e) {
                System.out.println("Exception thrown by Robot " + robot + " while working: " + e.getCause());
                throw (RuntimeException) e.getCause();
            }
        }
    }


    private class Renderer {
        private JFrame frame;
        private Map<Robot, JLabel> memoryLabels;
        private JPanel memoryPanel;
        private BufferedImage[] pengu, ice; //"3D" sprite stacking
        private BufferedImage[] ground = new BufferedImage[LAST_TERRAIN - FIRST_TERRAIN + 1]; //ground with potential text
        Map<Robot, Integer> movingTime = new HashMap<>();
        Map<Robot, Integer> movingSpeed = new HashMap<>();
        Map<Robot, String> speechBubble = new HashMap<>();
        Map<Robot, Integer> speechTimer = new HashMap<>();
        int speechBubbleTimeout = 60;
        double cameraX = 0, cameraY = 0;
        double scale = 50, minScale = 1, maxScale = 1000;
        final int zResolution = 8;
        double zHeight = 0.4;

        void setup() {
            // load images
            BufferedImage snow = loadImage("snow");
            for (char c = FIRST_TERRAIN; c <= LAST_TERRAIN; c++)
                ground[c - FIRST_TERRAIN] = overlayChar(snow, c);
            pengu = load3DImage("peng");
            ice = load3DImage("ice");

            // init frame and layout
            frame = new JFrame("Penguin Robot");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setLayout(new BorderLayout());

            //center canvas
            JPanel canvas = new Canvas();
            canvas.setFont(canvas.getFont().deriveFont(16.0f));
            frame.getContentPane().add(canvas, BorderLayout.CENTER);

            //top control panel
            JPanel controlPanel = new JPanel();
            controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));

            //toggle pause
            JButton pauseButt = new JButton("Start/Pause");
            pauseButt.addActionListener(e -> {
                pause = currentFrame != pauseAtFrame && !pause;
                pauseAtFrame = -1;
            });
            pauseButt.setFocusable(false);
            pauseButt.setToolTipText("or press Spacebar");
            controlPanel.add(pauseButt);

            //one frame
            JButton nextFrame = new JButton("Next Frame");
            nextFrame.addActionListener(e -> {
                pauseAtFrame = currentFrame + 1;
                pause = false;
            });
            nextFrame.setFocusable(false);
            nextFrame.setToolTipText("or press Enter");
            controlPanel.add(nextFrame);

            //speed controls
            controlPanel.add(new JLabel("  Speed:"));
            ButtonGroup group = new ButtonGroup();
            JRadioButton[] r = {new JRadioButton("x0.25"), new JRadioButton("x0.5"),
                    new JRadioButton("x1"), new JRadioButton("x2"), new JRadioButton("x4")};
            for (int i = 0, t = 64; i < r.length; i++, t /= 2) {
                final int tF = t; //this is stupid
                r[i].addActionListener(e -> targetFrametime = tF);
                r[i].setFocusable(false);
                group.add(r[i]);
                controlPanel.add(r[i]);
            }
            r[2].setSelected(true);

            //3D toggle
            JToggleButton toggle3D = new JToggleButton("3D Mode");
            toggle3D.addActionListener(e -> {
                if (zHeight == 0)
                    zHeight = 0.4;
                else
                    zHeight = 0;
                //render
                frame.revalidate();
                frame.repaint();
            });
            toggle3D.setSelected(true);
            toggle3D.setFocusable(false);
            controlPanel.add(toggle3D);

            frame.getContentPane().add(controlPanel, BorderLayout.NORTH);

            //bottom memory panel
            memoryPanel = new JPanel();
            memoryPanel.setBackground(Color.LIGHT_GRAY);
            memoryPanel.setLayout(new BoxLayout(memoryPanel, BoxLayout.Y_AXIS));
            memoryLabels = new HashMap<>();
            updateMemoryPanel();
            frame.getContentPane().add(memoryPanel, BorderLayout.SOUTH);

            // event handlers
            frame.addKeyListener(controller);
            frame.addMouseListener(controller);
            canvas.addMouseMotionListener(controller);
            frame.addMouseWheelListener(controller);

            // frame initial state
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setPreferredSize(new Dimension(1600, 900));
            frame.setMinimumSize(new Dimension(800, 450));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }

        void render() {
            //increase moving time
            for (Map.Entry<Robot, Integer> entry : movingTime.entrySet())
                entry.setValue(entry.getValue() + 1);

            //reset non-moving penguins
            for (Map.Entry<Robot, Integer> entry : movingSpeed.entrySet())
                if (entry.getValue() == 0)
                    movingTime.put(entry.getKey(), 0);

            updateMemoryPanel();

            //render
            frame.revalidate();
            frame.repaint();

            //decrease timer for speech bubble
            for (Map.Entry<Robot, Integer> entry : speechTimer.entrySet())
                entry.setValue(Math.max(0, entry.getValue() - 1));

            //reset moved
            for (Map.Entry<Robot, Integer> entry : movingSpeed.entrySet())
                entry.setValue(0);
        }

        private void updateMemoryPanel() {
            for (Robot robot : robots) {
                if (!memoryLabels.containsKey(robot)) {
                    JLabel memoryLabel = new JLabel();
                    memoryPanel.add(memoryLabel);
                    memoryLabels.put(robot, memoryLabel);
                }

                String memStr = "";
                try {
                    if (memoryToString != null)
                        memStr = (String) memoryToString.invoke(robot);
                } catch (IllegalAccessException e) {
                    /* */
                } catch (InvocationTargetException e) {
                    throw (RuntimeException) e.getCause();
                }
                memoryLabels.get(robot).setText("  " + robot.toString() + ": " + memStr);
            }
        }

        private class Canvas extends JPanel {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                //update camera pos
                cameraX = getWidth() / 2 - width * scale / 2;
                cameraY = getHeight() / 2 - height * scale / 2;

                //draw cursor
                //g.setColor(Color.BLUE);
                //g.drawOval(round(controller.mousePos.x * scale), round(controller.mousePos.y * scale), 10, 10);

                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++) {
                        char c = terrain[x][y];
                        if (FIRST_TERRAIN <= c && c <= LAST_TERRAIN && c != '#')
                            drawImage(g, ground[c - FIRST_TERRAIN],
                                    cameraX + (x + 0.5) * scale, cameraY + (y + 0.5) * scale,
                                    scale, 0);
                    }

                double dz = -zHeight / zResolution;
                for (int z = 0; z < zResolution; z++) {
                    if (pengu[z] != null)
                        for (Robot robot : robots) {
                            Position pos = robot.getPosition();
                            double size = robot.getSize();
                            double wiggle = Math.sin(movingTime.get(robot) / 3.0) * Math.toRadians(15);
                            drawImage(g, pengu[z],
                                    cameraX + pos.x * scale, cameraY + (pos.y + dz * z * size * 1.5) * scale,
                                    scale * size, robot.getDirection() + wiggle);
                        }

                    for (int y = 0; y < height; y++)
                        for (int x = 0; x < width; x++)
                            if (terrain[x][y] == '#' && ice[z] != null)
                                drawImage(g, ice[z],
                                        cameraX + (x + 0.5) * scale, cameraY + (y + dz * z * 1.2 + 0.5) * scale,
                                        scale, 0);
                }

                for (Robot robot : robots) {
                    Position pos = robot.getPosition();
                    double size = robot.getSize();

                    if (speechTimer.get(robot) > 0) {
                        int x = (int) Math.round(cameraX + (pos.x + 0.25) * scale);
                        int y = (int) Math.round(cameraY + (pos.y - zHeight * size * 1.5) * scale);
                        FontMetrics fm = g.getFontMetrics();
                        int arrowW = 6, arrowH = 10;
                        int w = Math.max(20, fm.stringWidth(speechBubble.get(robot) + 2)), h = fm.getHeight() + 2;
                        int[] xP = {x, x + arrowW, x + w - arrowW, x + w - arrowW, x - arrowW, x - arrowW, x};
                        int[] yP = {y, y - arrowH, y - arrowH, y - arrowH - h, y - arrowH - h, y - arrowH, y - arrowH};
                        g.setColor(Color.WHITE);
                        g.fillPolygon(xP, yP, 7);
                        g.setColor(Color.BLACK);
                        g.drawPolygon(xP, yP, 7);
                        g.drawString(speechBubble.get(robot), x, y - arrowH - 5);
                    }
                }
            }

            private void drawImage(Graphics g, BufferedImage img, double x, double y, double scale, double rot) {
                AffineTransform transform = new AffineTransform();

                transform.translate(x, y);
                transform.rotate(rot);
                transform.scale(scale / img.getWidth(), scale / img.getHeight());
                transform.translate(-img.getWidth() / 2.0, -img.getHeight() / 2.0);

                ((Graphics2D) g).drawImage(img, transform, null);
            }
        }

        BufferedImage loadImage(String file) {
            try {
                return ImageIO.read(World.this.getClass().getResource("img/" + file + ".png"));
            } catch (IOException | IllegalArgumentException e) {
                return null;
            }
        }

        BufferedImage[] load3DImage(String filePrefix) {
            BufferedImage[] images = new BufferedImage[zResolution];
            for (int i = 0; i < zResolution; i++)
                images[i] = loadImage(filePrefix + i);
            return images;
        }

        BufferedImage overlayChar(BufferedImage background, char c) {
            int w = background.getWidth(), h = background.getHeight();
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.drawImage(background, 0, 0, w, h, null);
            g.setPaint(Color.BLACK);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(String.valueOf(c), w / 2 - fm.stringWidth(String.valueOf(c)) / 2, fm.getHeight() - 4);
            g.dispose();
            return img;
        }
    }
}

