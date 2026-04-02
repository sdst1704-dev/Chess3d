import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class Rook3DViewer extends JPanel {

    private double rotX = Math.toRadians(30);
    private double rotY = Math.toRadians(45);
    private double scale = 150;
    private int prevMouseX, prevMouseY;
    private boolean isRotating = false;

    private double rookCenterX = 0.5;
    private double rookCenterY = 0.5;
    private double rookCenterZ = 0.5;

    private List<Vertex> modelVertices;
    private List<Edge> modelEdges;
    private List<Triangle> modelTriangles;

    // Данные для куба
    private List<Vertex> cubeVertices;
    private List<Triangle> cubeTriangles;

    public Rook3DViewer() {
        setPreferredSize(new Dimension(900, 700));
        setBackground(Color.WHITE);

        buildRookModel();
        buildCubeModel();

        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(new Color(240, 240, 240));

        JTextField fieldX = new JTextField("0.0", 5);
        JTextField fieldY = new JTextField("0.0", 5);
        JTextField fieldZ = new JTextField("0.0", 5);

        JButton setCenterBtn = new JButton("Установить центр");
        setCenterBtn.addActionListener(e -> {
            try {
                double x = Double.parseDouble(fieldX.getText());
                double y = Double.parseDouble(fieldY.getText());
                double z = Double.parseDouble(fieldZ.getText());
                rookCenterX = x;
                rookCenterY = y;
                rookCenterZ = z;
                repaint();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка ввода числа");
            }
        });

        JButton resetViewBtn = new JButton("Сбросить вид");
        resetViewBtn.addActionListener(e -> {
            rotX = Math.toRadians(30);
            rotY = Math.toRadians(45);
            scale = 150;
            repaint();
        });

        controlPanel.add(new JLabel("X:"));
        controlPanel.add(fieldX);
        controlPanel.add(new JLabel("Y:"));
        controlPanel.add(fieldY);
        controlPanel.add(new JLabel("Z:"));
        controlPanel.add(fieldZ);
        controlPanel.add(setCenterBtn);
        controlPanel.add(resetViewBtn);

        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.SOUTH);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isRotating = true;
                prevMouseX = e.getX();
                prevMouseY = e.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isRotating = false;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isRotating) {
                    int dx = e.getX() - prevMouseX;
                    int dy = e.getY() - prevMouseY;
                    rotY += dx * 0.01;
                    rotX += dy * 0.01;
                    prevMouseX = e.getX();
                    prevMouseY = e.getY();
                    repaint();
                }
            }
        });

        addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            scale += notches * 10;
            if (scale < 50) scale = 50;
            if (scale > 500) scale = 500;
            repaint();
        });
    }

    private void buildCubeModel() {
        cubeVertices = new ArrayList<>();
        cubeTriangles = new ArrayList<>();

        double half = 0.25 / 2;

        // 8 вершин куба
        cubeVertices.add(new Vertex(-0.19, -0.2, -0.1)); // 0
        cubeVertices.add(new Vertex( 0.18, -0.2, -0.1)); // 1
        cubeVertices.add(new Vertex( 0.18, -0.2,  0.1)); // 2
        cubeVertices.add(new Vertex(-0.19, -0.2,  0.1)); // 3

        cubeVertices.add(new Vertex(-0.1, -0.2, -0.15)); // 4
        cubeVertices.add(new Vertex( 0.1, -0.2, -0.15)); // 5
        cubeVertices.add(new Vertex( 0.1, -0.2,  0.15)); // 6
        cubeVertices.add(new Vertex(-0.1, -0.2,  0.15)); // 7

        cubeVertices.add(new Vertex(-0.15,  0, -0.08)); // 8
        cubeVertices.add(new Vertex( 0.07,  0, -0.08)); // 9
        cubeVertices.add(new Vertex( 0.07,  0,  0.08)); // 10
        cubeVertices.add(new Vertex(-0.15,  0,  0.08)); // 11

        cubeVertices.add(new Vertex(-0.18, 0.1, -0.07)); // 12
        cubeVertices.add(new Vertex( 0.02, 0.1, -0.07)); // 13
        cubeVertices.add(new Vertex( 0.02, 0.1,  0.07)); // 14
        cubeVertices.add(new Vertex(-0.18, 0.1,  0.07)); // 15

        cubeVertices.add(new Vertex(-0.18, 0.13, -0.07)); // 16
        cubeVertices.add(new Vertex( 0.02, 0.13, -0.07)); // 17
        cubeVertices.add(new Vertex( 0.02, 0.13,  0.07)); // 18
        cubeVertices.add(new Vertex(-0.18, 0.13,  0.07)); // 19

        // 12 треугольников для 6 граней (по 2 на грань)
//первый уровень
        // Передняя грань (z = -half)
        cubeTriangles.add(new Triangle(0, 4, 8));
        cubeTriangles.add(new Triangle(1, 5, 9));
        // Задняя грань (z = half)
        cubeTriangles.add(new Triangle(2, 6, 10));
        cubeTriangles.add(new Triangle(3, 7, 11));
        // Левая грань (x = -half)
        cubeTriangles.add(new Triangle(0, 3, 8));
        cubeTriangles.add(new Triangle(3, 8, 11));
        // Правая грань (x = half)
        cubeTriangles.add(new Triangle(4, 5, 8));
        cubeTriangles.add(new Triangle(5, 8, 9));
        // Верхняя грань (y = half)
        cubeTriangles.add(new Triangle(1, 2, 9));
        cubeTriangles.add(new Triangle(2, 9, 10));
        // Нижняя грань (y = -half)
        cubeTriangles.add(new Triangle(6, 7, 10));
        cubeTriangles.add(new Triangle(7, 10, 11));
// второй уровень
        cubeTriangles.add(new Triangle(8, 9, 12));
        cubeTriangles.add(new Triangle(9, 12, 13));
        // Левая грань (x = -half)
        cubeTriangles.add(new Triangle(9, 10, 13));
        cubeTriangles.add(new Triangle(10, 13, 14));
        // Правая грань (x = half)
        cubeTriangles.add(new Triangle(10, 11, 14));
        cubeTriangles.add(new Triangle(11, 14, 15));
        // Верхняя грань (y = half)
        cubeTriangles.add(new Triangle(11, 8, 15));
        cubeTriangles.add(new Triangle(8, 15, 12));
//третий уровень
        cubeTriangles.add(new Triangle(12, 13, 16));
        cubeTriangles.add(new Triangle(13, 16, 17));
        // Левая грань (x = -half)
        cubeTriangles.add(new Triangle(13, 14, 17));
        cubeTriangles.add(new Triangle(14, 17, 18));
        // Правая грань (x = half)
        cubeTriangles.add(new Triangle(14, 15, 18));
        cubeTriangles.add(new Triangle(15, 18, 19));
        // Верхняя грань (y = half)
        cubeTriangles.add(new Triangle(15, 12, 19));
        cubeTriangles.add(new Triangle(12, 19, 16));
    }

    private void buildRookModel() {
        modelVertices = new ArrayList<>();
        modelEdges = new ArrayList<>();
        modelTriangles = new ArrayList<>();

        double[] levelsY = {-0.45, -0.45, -0.35, -0.3, -0.25, -0.2, -0.2};
        double[] levelsR = {0, 0.25, 0.25, 0.2, 0.23, 0.22, 0.0};
        int segments = 20;

        // Вершины ладьи
        for (int i = 0; i < levelsY.length; i++) {
            double y = levelsY[i];
            double r = levelsR[i];
            for (int j = 0; j < segments; j++) {
                double angle = 2 * Math.PI * j / segments;
                double x = r * Math.cos(angle);
                double z = r * Math.sin(angle);
                modelVertices.add(new Vertex(x, y, z));
            }
        }

        // Горизонтальные рёбра ладьи
        for (int i = 0; i < levelsY.length; i++) {
            int base = i * segments;
            for (int j = 0; j < segments; j++) {
                int next = (j + 1) % segments;
                modelEdges.add(new Edge(base + j, base + next));
            }
        }

        // Вертикальные рёбра ладьи
        for (int i = 0; i < levelsY.length - 1; i++) {
            int baseLow = i * segments;
            int baseHigh = (i + 1) * segments;
            for (int j = 0; j < segments; j++) {
                modelEdges.add(new Edge(baseLow + j, baseHigh + j));
            }
        }

        // Треугольники для заливки
        for (int i = 0; i < levelsY.length - 1; i++) {
            for (int j = 0; j < segments; j++) {
                int nextJ = (j + 1) % segments;
                int p0 = i * segments + j;
                int p1 = i * segments + nextJ;
                int p2 = (i + 1) * segments + nextJ;
                int p3 = (i + 1) * segments + j;

                modelTriangles.add(new Triangle(p0, p1, p2));
                modelTriangles.add(new Triangle(p0, p2, p3));
            }
        }
    }

    private double[] rotate(double x, double y, double z) {
        double cosY = Math.cos(rotY);
        double sinY = Math.sin(rotY);
        double x1 = x * cosY - z * sinY;
        double z1 = x * sinY + z * cosY;

        double cosX = Math.cos(rotX);
        double sinX = Math.sin(rotX);
        double y1 = y * cosX - z1 * sinX;
        double z2 = y * sinX + z1 * cosX;

        return new double[]{x1, y1, z2};
    }

    private Point project(double x, double y, double z) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int screenX = (int) (x * scale) + cx;
        int screenY = (int) (-y * scale) + cy;
        return new Point(screenX, screenY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g2);
        drawAxes(g2);
        drawRook(g2);
        drawCube(g2);  // Рисуем куб над ладьей
        drawInfo(g2);
    }

    private void drawCube(Graphics2D g2) {
        List<Point> projected = new ArrayList<>();

        // Смещаем куб вместе с ладьей
        for (Vertex v : cubeVertices) {
            double wx = v.x + rookCenterX;
            double wy = v.y + rookCenterY;
            double wz = v.z + rookCenterZ;
            double[] rot = rotate(wx, wy, wz);
            projected.add(project(rot[0], rot[1], rot[2]));
        }

        // Рисуем закрашенные грани куба (черный цвет)
        g2.setColor(new Color(100, 100, 100, 200));
        for (Triangle tri : cubeTriangles) {
            Point p0 = projected.get(tri.i0);
            Point p1 = projected.get(tri.i1);
            Point p2 = projected.get(tri.i2);
            int[] xPoints = {p0.x, p1.x, p2.x};
            int[] yPoints = {p0.y, p1.y, p2.y};
            g2.fillPolygon(xPoints, yPoints, 3);
        }

        // Рисуем границы куба (светло-серым для контраста)
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1));
        // Рисуем ребра куба
        int[][] edges = {
                //первый уровень
                {0,4}, {4,5}, {5,1}, {1,2}, {2,6}, {6,7}, {7,3}, {3,0}, // нижняя грань
                {8,9}, {9,10}, {10,11}, {11,8}, // верхняя грань
                {0,8}, {4,8}, {5,9}, {1,9}, {2,10}, {6,10}, {7,11}, {3,11},   // вертикальные ребра
                //второй уровень
                {12,13}, {13,14}, {14,15}, {15,12}, // верхняя грань
                {8,12}, {9,13}, {10,14}, {11,15},  // вертикальные ребра
                //третий уровень
                {16,17}, {17,18}, {18,19}, {19,16}, // верхняя грань
                {12,16}, {13,17}, {14,18}, {15,19}   // вертикальные ребра
        };
        for (int[] edge : edges) {
            Point p1 = projected.get(edge[0]);
            Point p2 = projected.get(edge[1]);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(230, 230, 230));
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int spacing = 40;

        for (int x = cx % spacing; x < getWidth(); x += spacing) {
            g2.drawLine(x, 0, x, getHeight());
        }
        for (int y = cy % spacing; y < getHeight(); y += spacing) {
            g2.drawLine(0, y, getWidth(), y);
        }

        g2.setColor(new Color(180, 180, 180));
        g2.drawLine(cx, 0, cx, getHeight());
        g2.drawLine(0, cy, getWidth(), cy);
    }

    private void drawAxes(Graphics2D g2) {
        double[] origin = rotate(0, 0, 0);
        Point pOrigin = project(origin[0], origin[1], origin[2]);

        double[] xEnd = rotate(1.5, 0, 0);
        Point pX = project(xEnd[0], xEnd[1], xEnd[2]);
        g2.setColor(Color.RED);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(pOrigin.x, pOrigin.y, pX.x, pX.y);
        drawArrow(g2, pOrigin, pX, Color.RED);
        g2.drawString("X", pX.x + 5, pX.y + 5);

        double[] yEnd = rotate(0, 1.5, 0);
        Point pY = project(yEnd[0], yEnd[1], yEnd[2]);
        g2.setColor(Color.GREEN);
        g2.drawLine(pOrigin.x, pOrigin.y, pY.x, pY.y);
        drawArrow(g2, pOrigin, pY, Color.GREEN);
        g2.drawString("Y", pY.x + 5, pY.y + 5);

        double[] zEnd = rotate(0, 0, 1.5);
        Point pZ = project(zEnd[0], zEnd[1], zEnd[2]);
        g2.setColor(Color.BLUE);
        g2.drawLine(pOrigin.x, pOrigin.y, pZ.x, pZ.y);
        drawArrow(g2, pOrigin, pZ, Color.BLUE);
        g2.drawString("Z", pZ.x + 5, pZ.y + 5);

        g2.setColor(Color.BLACK);
        g2.fillOval(pOrigin.x - 3, pOrigin.y - 3, 6, 6);
    }

    private void drawArrow(Graphics2D g2, Point from, Point to, Color color) {
        g2.setColor(color);
        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        int arrowSize = 10;
        int x1 = (int) (to.x - arrowSize * Math.cos(angle - Math.PI / 6));
        int y1 = (int) (to.y - arrowSize * Math.sin(angle - Math.PI / 6));
        int x2 = (int) (to.x - arrowSize * Math.cos(angle + Math.PI / 6));
        int y2 = (int) (to.y - arrowSize * Math.sin(angle + Math.PI / 6));
        g2.drawLine(to.x, to.y, x1, y1);
        g2.drawLine(to.x, to.y, x2, y2);
    }

    private void drawRook(Graphics2D g2) {
        List<Point> projected = new ArrayList<>();
        for (Vertex v : modelVertices) {
            double wx = v.x + rookCenterX;
            double wy = v.y + rookCenterY;
            double wz = v.z + rookCenterZ;
            double[] rot = rotate(wx, wy, wz);
            projected.add(project(rot[0], rot[1], rot[2]));
        }

        // Рисуем треугольники с заливкой
        g2.setColor(new Color(100, 100, 100, 200));
        for (Triangle tri : modelTriangles) {
            Point p0 = projected.get(tri.i0);
            Point p1 = projected.get(tri.i1);
            Point p2 = projected.get(tri.i2);
            int[] xPoints = {p0.x, p1.x, p2.x};
            int[] yPoints = {p0.y, p1.y, p2.y};
            g2.fillPolygon(xPoints, yPoints, 3);
        }

        // Рисуем рёбра
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1));
        for (Edge e : modelEdges) {
            Point p1 = projected.get(e.i1);
            Point p2 = projected.get(e.i2);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // Центр ладьи
        double[] centerRot = rotate(rookCenterX, rookCenterY, rookCenterZ);
        Point c = project(centerRot[0], centerRot[1], centerRot[2]);
        g2.setColor(Color.MAGENTA);
        g2.fillOval(c.x - 4, c.y - 4, 8, 8);
        g2.drawString("центр", c.x + 5, c.y - 5);
    }

    private void drawInfo(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.drawString("Положение ладьи: (" +
                String.format("%.2f", rookCenterX) + ", " +
                String.format("%.2f", rookCenterY) + ", " +
                String.format("%.2f", rookCenterZ) + ")", 10, 20);
        g2.drawString("Черный куб над ладьей (центр куба относительно ладьи: 0,0,1)", 10, 40);
        g2.drawString("Управление: перетаскивание мыши — вращение, колесико — масштаб", 10, 60);
        g2.drawString("Введите координаты центра внизу и нажмите 'Установить центр'", 10, 80);
    }

    private static class Vertex {
        double x, y, z;
        Vertex(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static class Edge {
        int i1, i2;
        Edge(int i1, int i2) {
            this.i1 = i1;
            this.i2 = i2;
        }
    }

    private static class Triangle {
        int i0, i1, i2;
        Triangle(int a, int b, int c) {
            i0 = a;
            i1 = b;
            i2 = c;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3D Ладья с черным кубом");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new Rook3DViewer());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}