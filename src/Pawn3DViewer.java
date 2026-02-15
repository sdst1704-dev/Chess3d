import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class Pawn3DViewer extends JPanel {

    private double rotX = Math.toRadians(30);
    private double rotY = Math.toRadians(45);
    private double scale = 150;
    private int prevMouseX, prevMouseY;
    private boolean isRotating = false;

    private double pawnCenterX = 0.5;
    private double pawnCenterY = 0.5;
    private double pawnCenterZ = 0.5;

    private List<Vertex> modelVertices;
    private List<Edge> modelEdges;
    // Новый список для хранения треугольников (граней)
    private List<Triangle> modelTriangles;

    public Pawn3DViewer() {
        setPreferredSize(new Dimension(900, 700));
        setBackground(Color.WHITE);

        buildPawnModel();

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
                pawnCenterX = x;
                pawnCenterY = y;
                pawnCenterZ = z;
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

    private void buildPawnModel() {
        modelVertices = new ArrayList<>();
        modelEdges = new ArrayList<>();
        modelTriangles = new ArrayList<>(); // инициализация

        // Параметры пешки (без изменений)
        double[] levelsY = {-0.45, -0.45, -0.35, -0.2, 0.15, 0.20, 0.23};
        double[] levelsR = {0, 0.3, 0.3, 0.15, 0.05, 0.15, 0.1};
        int segments = 20;

        // Вершины пешки
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

        // Горизонтальные рёбра пешки
        for (int i = 0; i < levelsY.length; i++) {
            int base = i * segments;
            for (int j = 0; j < segments; j++) {
                int next = (j + 1) % segments;
                modelEdges.add(new Edge(base + j, base + next));
            }
        }

        // Вертикальные рёбра пешки
        for (int i = 0; i < levelsY.length - 1; i++) {
            int baseLow = i * segments;
            int baseHigh = (i + 1) * segments;
            for (int j = 0; j < segments; j++) {
                modelEdges.add(new Edge(baseLow + j, baseHigh + j));
            }
        }
        double sphereRadius = 0.15;
        double sphereCenterY = 0.35; // центр сферы по Y (верх пешки)

        // Запоминаем индекс, с которого начинаются вершины сферы
        int sphereStartIndex = modelVertices.size();

        // Генерируем вершины сферы (сферические координаты)
        for (int i = segments; i >= 0; i--) {
            double theta = Math.PI * i / segments; // от 0 до PI (широта)
            double y = sphereCenterY + sphereRadius * Math.cos(theta);
            double r = sphereRadius * Math.sin(theta);
            for (int j = 0; j < segments; j++) {
                double phi = 2 * Math.PI * j / segments; // долгота
                double x = r * Math.cos(phi);
                double z = r * Math.sin(phi);
                modelVertices.add(new Vertex(x, y, z));
            }
        }
        for (int i = 0; i < segments+levelsY.length; i++) {
            for (int j = 0; j < segments; j++) {
                int nextJ = (j + 1) % segments;
                int p0 = i * segments + j;
                int p1 =  i * segments + nextJ;
                int p2 = (i + 1) * segments + nextJ;
                int p3 =  (i + 1) * segments + j;

                // Два треугольника: (p0, p1, p2) и (p0, p2, p3)
                modelTriangles.add(new Triangle(p0, p1, p2));
                modelTriangles.add(new Triangle(p0, p2, p3));
            }
        }

        // Добавляем рёбра для сферы (опционально, чтобы был виден контур)
        for (int i = 0; i <= segments; i++) {
            for (int j = 0; j < segments; j++) {
                int nextJ = (j + 1) % segments;
                int current = sphereStartIndex + i * segments + j;
                int next = sphereStartIndex + i * segments + nextJ;
                modelEdges.add(new Edge(current, next));
                if (i < segments) {
                    int below = sphereStartIndex + (i + 1) * segments + j;
                    modelEdges.add(new Edge(current, below));
                }
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
        int screenX = (int)(x * scale) + cx;
        int screenY = (int)(-y * scale) + cy;
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
        drawModel(g2); // переименовал для ясности
        drawInfo(g2);
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
        int x1 = (int)(to.x - arrowSize * Math.cos(angle - Math.PI / 6));
        int y1 = (int)(to.y - arrowSize * Math.sin(angle - Math.PI / 6));
        int x2 = (int)(to.x - arrowSize * Math.cos(angle + Math.PI / 6));
        int y2 = (int)(to.y - arrowSize * Math.sin(angle + Math.PI / 6));
        g2.drawLine(to.x, to.y, x1, y1);
        g2.drawLine(to.x, to.y, x2, y2);
    }

    private void drawModel(Graphics2D g2) {
        // Проецируем все вершины
        List<Point> projected = new ArrayList<>();
        for (Vertex v : modelVertices) {
            double wx = v.x + pawnCenterX;
            double wy = v.y + pawnCenterY;
            double wz = v.z + pawnCenterZ;
            double[] rot = rotate(wx, wy, wz);
            projected.add(project(rot[0], rot[1], rot[2]));
        }

        // Сначала рисуем треугольники (грани) с заливкой
        g2.setColor(new Color(0, 0, 0, 200)); // синий полупрозрачный
        for (Triangle tri : modelTriangles) {
            Point p0 = projected.get(tri.i0);
            Point p1 = projected.get(tri.i1);
            Point p2 = projected.get(tri.i2);
            int[] xPoints = {p0.x, p1.x, p2.x};
            int[] yPoints = {p0.y, p1.y, p2.y};
            g2.fillPolygon(xPoints, yPoints, 3);
        }

        // Затем рисуем рёбра (контуры)
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1/5));
        for (Edge e : modelEdges) {
            Point p1 = projected.get(e.i1);
            Point p2 = projected.get(e.i2);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // Вершины (точки) - опционально
        g2.setColor(Color.BLACK);
        for (Point p : projected) {
            g2.fillOval(p.x - 1, p.y - 1, 2, 2);
        }

        // Подпись центра
        double[] centerRot = rotate(pawnCenterX, pawnCenterY, pawnCenterZ);
        Point c = project(centerRot[0], centerRot[1], centerRot[2]);
        g2.setColor(Color.MAGENTA);
        g2.fillOval(c.x - 4, c.y - 4, 8, 8);
        g2.drawString("центр пешки", c.x + 5, c.y - 5);
    }

    private void drawInfo(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.drawString("Положение пешки: (" +
                String.format("%.2f", pawnCenterX) + ", " +
                String.format("%.2f", pawnCenterY) + ", " +
                String.format("%.2f", pawnCenterZ) + ")", 10, 20);
        g2.drawString("Управление: перетаскивание мыши — вращение, колесико — масштаб", 10, 40);
        g2.drawString("Введите координаты центра внизу и нажмите 'Установить центр'", 10, 60);
    }

    private static class Vertex {
        double x, y, z;
        Vertex(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    }

    private static class Edge {
        int i1, i2;
        Edge(int i1, int i2) { this.i1 = i1; this.i2 = i2; }
    }

    // Новый класс для треугольника
    private static class Triangle {
        int i0, i1, i2;
        Triangle(int a, int b, int c) { i0 = a; i1 = b; i2 = c; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3D Шахматная пешка с синей сферой");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new Pawn3DViewer());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}