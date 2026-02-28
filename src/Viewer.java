import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class Viewer extends JPanel {

    // Параметры вращения и масштабирования
    private double rotX = Math.toRadians(45);
    private double rotY = Math.toRadians(45);
    private double scale = 20.0;
    private int prevMouseX, prevMouseY;
    private boolean isRotating = false;

    // Типы фигур
    private enum PieceType { PAWN, ROOK, BISHOP }

    // Класс фигуры: тип и координаты центра
    private static class Piece {
        PieceType type;
        double x, y, z;
        Piece(PieceType type, double x, double y, double z) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private List<Piece> pieces;          // все фигуры на доске

    // Модели фигур (общие для всех экземпляров)
    // Для пешки
    private List<Vertex> pawnVertices;
    private List<Edge> pawnEdges;
    private List<Triangle> pawnTriangles;

    // Для ладьи
    private List<Vertex> rookVertices;
    private List<Edge> rookEdges;
    private List<Triangle> rookTriangles;
    // Для слона
    private List<Vertex> bishopVertices;
    private List<Edge> bishopEdges;
    private List<Triangle> bishopTriangles;

    public Viewer() {
        setPreferredSize(new Dimension(800, 800));
        setBackground(Color.WHITE);

        // Построение моделей
        buildPawnModel();
        buildRookModel();
        buildBishopModel();

        // Инициализация фигур
        initPieces();

        // Панель управления
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(new Color(240, 240, 240));

        // Поля для старых координат
        JTextField oldXField = new JTextField("1", 3);
        JTextField oldYField = new JTextField("1", 3);
        JTextField oldZField = new JTextField("1", 3);

        // Поля для новых координат
        JTextField newXField = new JTextField("1", 3);
        JTextField newYField = new JTextField("1", 3);
        JTextField newZField = new JTextField("1", 3);

        JButton moveBtn = new JButton("Переместить фигуру");
        moveBtn.addActionListener(e -> {
            try {
                double oldX = Double.parseDouble(oldXField.getText());
                double oldY = Double.parseDouble(oldYField.getText());
                double oldZ = Double.parseDouble(oldZField.getText());
                double newX = Double.parseDouble(newXField.getText());
                double newY = Double.parseDouble(newYField.getText());
                double newZ = Double.parseDouble(newZField.getText());

                // Проверка диапазона (0..8)
                if (newX < 0 || newX > 8 || newY < 0 || newY > 8 || newZ < 0 || newZ > 8 ||
                        oldX < 0 || oldX > 8 || oldY < 0 || oldY > 8 || oldZ < 0 || oldZ > 8) {
                    throw new IllegalArgumentException("Координаты должны быть от 0 до 8");
                }

                // Вычисляем центр для старой позиции
                double targetX = oldX - 0.5;
                double targetY = oldY - 0.5;
                double targetZ = oldZ - 0.5;

                // Ищем фигуру с таким центром (с учётом погрешности)
                Piece found = null;
                for (Piece p : pieces) {
                    if (Math.abs(p.x - targetX) < 1e-6 &&
                            Math.abs(p.y - targetY) < 1e-6 &&
                            Math.abs(p.z - targetZ) < 1e-6) {
                        found = p;
                        break;
                    }
                }

                if (found == null) {
                    JOptionPane.showMessageDialog(this, "Фигура с указанными координатами не найдена");
                    return;
                }

                // Перемещаем
                found.x = newX - 0.5;
                found.y = newY - 0.5;
                found.z = newZ - 0.5;

                repaint();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка ввода числа");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            }
        });

        JButton resetViewBtn = new JButton("Сбросить вид");
        resetViewBtn.addActionListener(e -> {
            rotX = Math.toRadians(30);
            rotY = Math.toRadians(45);
            scale = 150;
            repaint();
        });

        // Добавляем компоненты на панель
        controlPanel.add(new JLabel("Старые:"));
        controlPanel.add(oldXField);
        controlPanel.add(oldYField);
        controlPanel.add(oldZField);
        controlPanel.add(new JLabel("    Новые:"));
        controlPanel.add(newXField);
        controlPanel.add(newYField);
        controlPanel.add(newZField);
        controlPanel.add(moveBtn);
        controlPanel.add(resetViewBtn);

        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.SOUTH);

        // Обработчики мыши для вращения
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
                    int deltaX = - e.getX() + prevMouseX;
                    int deltaY = e.getY() - prevMouseY;
                    rotY += deltaX * 0.01;
                    rotX += deltaY * 0.01;
                    prevMouseX = e.getX();
                    prevMouseY = e.getY();
                    repaint();
                }
            }
        });

        addMouseWheelListener(e -> {
            int rotation = e.getWheelRotation();
            scale -= rotation * 10;
            if (scale < 10) scale = 10;
            if (scale > 500) scale = 500;
            repaint();
        });
    }

    // Инициализация фигур
    private void initPieces() {
        pieces = new ArrayList<>();

        // Пешки на позициях (i,1,1) и вариации по Y,Z (как в исходном коде)
        for (int i = 1; i <= 8; i++) {
//            pieces.add(new Piece(PieceType.PAWN, i - 0.5, 0.5 + 1, 0.5 + 1)); // (i,2,2)?
//            pieces.add(new Piece(PieceType.PAWN, i - 0.5, 0.5 + 1, 0.5));     // (i,2,1)
            pieces.add(new Piece(PieceType.PAWN, i - 0.5, 0.5, 0.5 + 1));     // (i,1,2)
        }

        // Две ладьи
        pieces.add(new Piece(PieceType.ROOK, 1 - 0.5, 0.5, 0.5)); // (1,1,1)
        pieces.add(new Piece(PieceType.ROOK, 8 - 0.5, 0.5, 0.5)); // (8,1,1)
        // Два слона
        pieces.add(new Piece(PieceType.BISHOP, 3 - 0.5, 0.5, 0.5)); // (1,1,1)
        pieces.add(new Piece(PieceType.BISHOP, 6 - 0.5, 0.5, 0.5)); // (8,1,1)
    }

    // ----- Построение модели пешки (без изменений, но сохраняем в отдельные списки) -----
    private void buildPawnModel() {
        pawnVertices = new ArrayList<>();
        pawnEdges = new ArrayList<>();
        pawnTriangles = new ArrayList<>();

        double[] levelsY = {-0.45, -0.45, -0.35, -0.2, 0.10, 0.15, 0.18};
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
                pawnVertices.add(new Vertex(x, y, z));
            }
        }

        // Горизонтальные рёбра
        for (int i = 0; i < levelsY.length; i++) {
            int base = i * segments;
            for (int j = 0; j < segments; j++) {
                int next = (j + 1) % segments;
                pawnEdges.add(new Edge(base + j, base + next));
            }
        }

        // Вертикальные рёбра
        for (int i = 0; i < levelsY.length - 1; i++) {
            int baseLow = i * segments;
            int baseHigh = (i + 1) * segments;
            for (int j = 0; j < segments; j++) {
                pawnEdges.add(new Edge(baseLow + j, baseHigh + j));
            }
        }

        double sphereRadius = 0.15;
        double sphereCenterY = 0.30;

        int sphereStartIndex = pawnVertices.size();

        // Вершины сферы (головы)
        for (int i = segments; i >= 0; i--) {
            double theta = Math.PI * i / segments;
            double y = sphereCenterY + sphereRadius * Math.cos(theta);
            double r = sphereRadius * Math.sin(theta);
            for (int j = 0; j < segments; j++) {
                double phi = 2 * Math.PI * j / segments;
                double x = r * Math.cos(phi);
                double z = r * Math.sin(phi);
                pawnVertices.add(new Vertex(x, y, z));
            }
        }

        // Треугольники для всей модели (включая сферу)
        for (int i = 0; i < segments + levelsY.length; i++) {
            for (int j = 0; j < segments; j++) {
                int nextJ = (j + 1) % segments;
                int p0 = i * segments + j;
                int p1 = i * segments + nextJ;
                int p2 = (i + 1) * segments + nextJ;
                int p3 = (i + 1) * segments + j;
                pawnTriangles.add(new Triangle(p0, p1, p2));
                pawnTriangles.add(new Triangle(p0, p2, p3));
            }
        }

        // Рёбра для сферы
        for (int i = 0; i <= segments; i++) {
            for (int j = 0; j < segments; j++) {
                int nextJ = (j + 1) % segments;
                int current = sphereStartIndex + i * segments + j;
                int next = sphereStartIndex + i * segments + nextJ;
                pawnEdges.add(new Edge(current, next));
                if (i < segments) {
                    int below = sphereStartIndex + (i + 1) * segments + j;
                    pawnEdges.add(new Edge(current, below));
                }
            }
        }
    }
// Строим модель слона
    private void buildBishopModel() {
        bishopVertices = new ArrayList<>();
        bishopEdges = new ArrayList<>();
        bishopTriangles = new ArrayList<>(); // инициализация

        // Параметры ладьи (без изменений)
        double[] levelsY = {-0.45, -0.45, -0.35, -0.3, -0.25, -0.10,   0,   0.02, 0.05, 0.08,   0.1,  0.12,   0.15,  0.18, 0.21, 0.24, 0.26, 0.28, 0.30,  0.315, 0.33, 0.35, 0.37, 0.38};
        double[] levelsR = {0, 0.3, 0.3,         0.2, 0.15,    0.1,   0.08, 0.1, 0.15, 0.1,    0.12,  0.09,  0.11, 0.11, 0.10, 0.08, 0.065, 0.05, 0.038, 0.033, 0.04, 0.04, 0.03, 0};
        int segments = 20;

        // Вершины ладьи
        for (int i = 0; i < levelsY.length; i++) {
            double y = levelsY[i];
            double r = levelsR[i];
            for (int j = 0; j < segments; j++) {
                double angle = 2 * Math.PI * j / segments;
                double x = r * Math.cos(angle);
                double z = r * Math.sin(angle);
                bishopVertices.add(new Vertex(x, y, z));
            }
        }

        // Горизонтальные рёбра ладьи
        for (int i = 0; i < levelsY.length; i++) {
            int base = i * segments;
            for (int j = 0; j < segments; j++) {
                int next = (j + 1) % segments;
                bishopEdges.add(new Edge(base + j, base + next));
            }
        }

        // Вертикальные рёбра ладьи
        for (int i = 0; i < levelsY.length - 1; i++) {
            int baseLow = i * segments;
            int baseHigh = (i + 1) * segments;
            for (int j = 0; j < segments; j++) {
                bishopEdges.add(new Edge(baseLow + j, baseHigh + j));
            }
        }
        for (int i = 0; i < levelsY.length-1; i++) {
            for (int j = 0; j < segments; j++) {
                int nextJ = (j + 1) % segments;
                int p0 = i * segments + j;
                int p1 =  i * segments + nextJ;
                int p2 = (i + 1) * segments + nextJ;
                int p3 =  (i + 1) * segments + j;

                // Два треугольника: (p0, p1, p2) и (p0, p2, p3)
                bishopTriangles.add(new Triangle(p0, p1, p2));
                bishopTriangles.add(new Triangle(p0, p2, p3));
            }
        }
    }

    // ----- Построение модели ладьи -----
    private void buildRookModel() {
        rookVertices = new ArrayList<>();
        rookEdges = new ArrayList<>();
        rookTriangles = new ArrayList<>();

        // Параметры ладьи
        double[] levelsY = {-0.45, -0.45, -0.35, -0.3, -0.25, -0.10, 0, 0.25, 0.3, 0.4, 0.4, 0.35, 0.35};
        double[] levelsR = {0, 0.3, 0.3,         0.2, 0.15,    0.1,   0.08,  0.08, 0.15, 0.15, 0.12, 0.12, 0};
        int segments = 20;

        // Вершины ладьи
        for (int i = 0; i < levelsY.length; i++) {
            double y = levelsY[i];
            double r = levelsR[i];
            for (int j = 0; j < segments; j++) {
                double angle = 2 * Math.PI * j / segments;
                double x = r * Math.cos(angle);
                double z = r * Math.sin(angle);
                rookVertices.add(new Vertex(x, y, z));
            }
        }

        // Горизонтальные рёбра
        for (int i = 0; i < levelsY.length; i++) {
            int base = i * segments;
            for (int j = 0; j < segments; j++) {
                int next = (j + 1) % segments;
                rookEdges.add(new Edge(base + j, base + next));
            }
        }

        // Вертикальные рёбра
        for (int i = 0; i < levelsY.length - 1; i++) {
            int baseLow = i * segments;
            int baseHigh = (i + 1) * segments;
            for (int j = 0; j < segments; j++) {
                rookEdges.add(new Edge(baseLow + j, baseHigh + j));
            }
        }

        // Треугольники (для заливки)
        for (int i = 0; i < levelsY.length - 1; i++) {
            for (int j = 0; j < segments; j++) {
                int nextJ = (j + 1) % segments;
                int p0 = i * segments + j;
                int p1 = i * segments + nextJ;
                int p2 = (i + 1) * segments + nextJ;
                int p3 = (i + 1) * segments + j;
                rookTriangles.add(new Triangle(p0, p1, p2));
                rookTriangles.add(new Triangle(p0, p2, p3));
            }
        }
    }

    // Вращение точки
    private double[] rotatePoint(double x, double y, double z) {
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

    // Проекция на экран
    private Point projectTo2D(double x, double y, double z) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int screenX = (int)(x * scale) + centerX;
        int screenY = (int)(-y * scale) + centerY;
        return new Point(screenX, screenY);
    }

    // Рисование осей
    private void drawCoordinateAxes(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        double axisLength = 9;

        double[][] axes = {{axisLength, 0, 0}, {0, axisLength, 0}, {0, 0, axisLength}};
        Color[] axisColors = {Color.RED, Color.GREEN, Color.BLUE};
        String[] axisLabels = {"X", "Y", "Z"};

        for (int i = 0; i < axes.length; i++) {
            double[] rotated = rotatePoint(axes[i][0], axes[i][1], axes[i][2]);
            Point p = projectTo2D(rotated[0], rotated[1], rotated[2]);
            g2d.setColor(axisColors[i]);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(centerX, centerY, p.x, p.y);
            drawArrow(g2d, centerX, centerY, p.x, p.y, axisColors[i]);
            g2d.drawString(axisLabels[i], p.x + 5, p.y + 5);
        }

        g2d.setColor(Color.BLACK);
        g2d.fillOval(centerX - 3, centerY - 3, 6, 6);
    }

    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2, Color color) {
        g2d.setColor(color);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowSize = 10;
        int x3 = (int)(x2 - arrowSize * Math.cos(angle - Math.PI / 6));
        int y3 = (int)(y2 - arrowSize * Math.sin(angle - Math.PI / 6));
        int x4 = (int)(x2 - arrowSize * Math.cos(angle + Math.PI / 6));
        int y4 = (int)(y2 - arrowSize * Math.sin(angle + Math.PI / 6));
        g2d.drawLine(x2, y2, x3, y3);
        g2d.drawLine(x2, y2, x4, y4);
    }

    // Рисование пешки
    private void drawPawn(Graphics2D g2d, double cx, double cy, double cz) {
        drawGeneric(g2d, pawnVertices, pawnEdges, pawnTriangles, cx, cy, cz);
    }

    // Рисование ладьи
    private void drawRook(Graphics2D g2d, double cx, double cy, double cz) {
        drawGeneric(g2d, rookVertices, rookEdges, rookTriangles, cx, cy, cz);
    }

    private void drawBishop(Graphics2D g2d, double cx, double cy, double cz) {
        drawGeneric(g2d, bishopVertices, bishopEdges, bishopTriangles, cx, cy, cz);
    }

    // Обобщённый метод отрисовки фигуры по заданной модели
    private void drawGeneric(Graphics2D g2d, List<Vertex> vertices, List<Edge> edges, List<Triangle> triangles,
                             double cx, double cy, double cz) {
        List<Point> projected = new ArrayList<>();
        for (Vertex v : vertices) {
            double wx = v.x + cx;
            double wy = v.y + cy;
            double wz = v.z + cz;
            double[] rot = rotatePoint(wx, wy, wz);
            projected.add(projectTo2D(rot[0], rot[1], rot[2]));
        }

        // Треугольники (заливка)
        g2d.setColor(new Color(0, 0, 0, 200));
        for (Triangle tri : triangles) {
            Point p0 = projected.get(tri.i0);
            Point p1 = projected.get(tri.i1);
            Point p2 = projected.get(tri.i2);
            int[] xPoints = {p0.x, p1.x, p2.x};
            int[] yPoints = {p0.y, p1.y, p2.y};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }

        // Рёбра
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(0.5f)); // исправлено: 1/2 -> 0.5f
        for (Edge e : edges) {
            Point p1 = projected.get(e.i1);
            Point p2 = projected.get(e.i2);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // Вершины (точки)
        g2d.setColor(Color.BLACK);
        for (Point p : projected) {
            g2d.fillOval(p.x - 1, p.y - 1, 2, 2);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawGrid(g2d);

        // Рисуем все фигуры
        for (Piece p : pieces) {
            switch (p.type) {
                case PAWN: drawPawn(g2d, p.x, p.y, p.z); break;
                case ROOK: drawRook(g2d, p.x, p.y, p.z); break;
                case BISHOP: drawBishop(g2d, p.x, p.y, p.z); break;
            }
        }

        drawCoordinateAxes(g2d);

        // Рисуем сетку доски (как в исходном коде) – без изменений
        for (int i = 0; i <= 8; i++) {
            for (int j = 0; j <= 8; j++) {
                double[] startRotated = rotatePoint(j, i, 0);
                double[] endRotated = rotatePoint(j, i, 8);
                Point start = projectTo2D(startRotated[0], startRotated[1], startRotated[2]);
                Point end = projectTo2D(endRotated[0], endRotated[1], endRotated[2]);
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawLine(start.x, start.y, end.x, end.y);
            }
            if (i != 8) {
                String label = Integer.toString(i + 1);
                double[] rotated = rotatePoint(i + 0.5, 0, 0);
                Point p = projectTo2D(rotated[0], rotated[1], rotated[2]);
                g2d.setColor(Color.RED);
                g2d.drawString(label, p.x + 5, p.y + 5);
            }
        }
        for (int i = 0; i <= 8; i++) {
            for (int j = 0; j <= 8; j++) {
                double[] startRotated = rotatePoint(j, 0, i);
                double[] endRotated = rotatePoint(j, 8, i);
                Point start = projectTo2D(startRotated[0], startRotated[1], startRotated[2]);
                Point end = projectTo2D(endRotated[0], endRotated[1], endRotated[2]);
                g2d.setColor(Color.BLACK);
                g2d.drawLine(start.x, start.y, end.x, end.y);
            }
            if (i != 8) {
                String label = Integer.toString(i + 1);
                double[] rotated = rotatePoint(0, i + 0.5, 0);
                Point p = projectTo2D(rotated[0], rotated[1], rotated[2]);
                g2d.setColor(Color.GREEN);
                g2d.drawString(label, p.x + 5, p.y + 5);
            }
        }
        for (int i = 0; i <= 8; i++) {
            for (int j = 0; j <= 8; j++) {
                double[] startRotated = rotatePoint(0, j, i);
                double[] endRotated = rotatePoint(8, j, i);
                Point start = projectTo2D(startRotated[0], startRotated[1], startRotated[2]);
                Point end = projectTo2D(endRotated[0], endRotated[1], endRotated[2]);
                g2d.setColor(Color.BLACK);
                g2d.drawLine(start.x, start.y, end.x, end.y);
            }
            if (i != 8) {
                String label = Integer.toString(i + 1);
                double[] rotated = rotatePoint(0, 0, i + 0.5);
                Point p = projectTo2D(rotated[0], rotated[1], rotated[2]);
                g2d.setColor(Color.BLUE);
                g2d.drawString(label, p.x + 5, p.y + 5);
            }
        }

        // Клетки доски (зелёные и бежевые кубы) – без изменений
        for (int b = 0; b < 8; b++) {
            for (int a = 0; a < 8; a++) {
                for (int c = 0; c < 8; c += 2) {
                    double[][][] CUBE = {
                            {{c + (a + b) % 2, b, a}, {c + 1 + (a + b) % 2, b, a}, {c + 1 + (a + b) % 2, b + 1, a}, {c + (a + b) % 2, b + 1, a}},
                            {{c + (a + b) % 2, b, a + 1}, {c + 1 + (a + b) % 2, b, a + 1}, {c + 1 + (a + b) % 2, b + 1, a + 1}, {c + (a + b) % 2, b + 1, a + 1}},
                            {{c + (a + b) % 2, b, a}, {c + 1 + (a + b) % 2, b, a}, {c + 1 + (a + b) % 2, b, a + 1}, {c + (a + b) % 2, b, a + 1}},
                            {{c + (a + b) % 2, b + 1, a}, {c + 1 + (a + b) % 2, b + 1, a}, {c + 1 + (a + b) % 2, b + 1, a + 1}, {c + (a + b) % 2, b + 1, a + 1}},
                            {{c + (a + b) % 2, b, a}, {c + (a + b) % 2, b + 1, a}, {c + (a + b) % 2, b + 1, a + 1}, {c + (a + b) % 2, b, a + 1}},
                            {{c + 1 + (a + b) % 2, b, a}, {c + 1 + (a + b) % 2, b + 1, a}, {c + 1 + (a + b) % 2, b + 1, a + 1}, {c + 1 + (a + b) % 2, b, a + 1}}
                    };
                    for (double[][] face : CUBE) {
                        int[] xs = new int[4], ys = new int[4];
                        for (int i = 0; i < 4; i++) {
                            double[] p = rotatePoint(face[i][0], face[i][1], face[i][2]);
                            Point s = projectTo2D(p[0], p[1], p[2]);
                            xs[i] = s.x;
                            ys[i] = s.y;
                        }
                        g2d.setColor(new Color(0, 200, 0, 15));
                        g2d.fillPolygon(xs, ys, 4);
                    }
                }
            }
        }
        for (int b = 0; b < 8; b++) {
            for (int a = 0; a < 8; a++) {
                for (int c = 0; c < 8; c += 2) {
                    double[][][] CUBE = {
                            {{8 - (c + (a + b) % 2), b, a}, {8 - (c + 1 + (a + b) % 2), b, a}, {8 - (c + 1 + (a + b) % 2), b + 1, a}, {8 - (c + (a + b) % 2), b + 1, a}},
                            {{8 - (c + (a + b) % 2), b, a + 1}, {8 - (c + 1 + (a + b) % 2), b, a + 1}, {8 - (c + 1 + (a + b) % 2), b + 1, a + 1}, {8 - (c + (a + b) % 2), b + 1, a + 1}},
                            {{8 - (c + (a + b) % 2), b, a}, {8 - (c + 1 + (a + b) % 2), b, a}, {8 - (c + 1 + (a + b) % 2), b, a + 1}, {8 - (c + (a + b) % 2), b, a + 1}},
                            {{8 - (c + (a + b) % 2), b + 1, a}, {8 - (c + 1 + (a + b) % 2), b + 1, a}, {8 - (c + 1 + (a + b) % 2), b + 1, a + 1}, {8 - (c + (a + b) % 2), b + 1, a + 1}},
                            {{8 - (c + (a + b) % 2), b, a}, {8 - (c + (a + b) % 2), b + 1, a}, {8 - (c + (a + b) % 2), b + 1, a + 1}, {8 - (c + (a + b) % 2), b, a + 1}},
                            {{8 - (c + 1 + (a + b) % 2), b, a}, {8 - (c + 1 + (a + b) % 2), b + 1, a}, {8 - (c + 1 + (a + b) % 2), b + 1, a + 1}, {8 - (c + 1 + (a + b) % 2), b, a + 1}}
                    };
                    for (double[][] face : CUBE) {
                        int[] xs = new int[4], ys = new int[4];
                        for (int i = 0; i < 4; i++) {
                            double[] p = rotatePoint(face[i][0], face[i][1], face[i][2]);
                            Point s = projectTo2D(p[0], p[1], p[2]);
                            xs[i] = s.x;
                            ys[i] = s.y;
                        }
                        g2d.setColor(new Color(200, 100, 0, 15));
                        g2d.fillPolygon(xs, ys, 4);
                    }
                }
            }
        }
    }

    // Рисование сетки (фон)
    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(240, 240, 240));
        g2d.setStroke(new BasicStroke(1));
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int gridSpacing = 40;

        for (int x = centerX % gridSpacing; x < getWidth(); x += gridSpacing)
            g2d.drawLine(x, 0, x, getHeight());
        for (int y = centerY % gridSpacing; y < getHeight(); y += gridSpacing)
            g2d.drawLine(0, y, getWidth(), y);

        g2d.setColor(new Color(200, 200, 200));
        g2d.drawLine(centerX, 0, centerX, getHeight());
        g2d.drawLine(0, centerY, getWidth(), centerY);
    }

    // Вспомогательные классы для модели
    private static class Vertex { double x, y, z; Vertex(double x, double y, double z) { this.x = x; this.y = y; this.z = z; } }
    private static class Edge { int i1, i2; Edge(int i1, int i2) { this.i1 = i1; this.i2 = i2; } }
    private static class Triangle { int i0, i1, i2; Triangle(int a, int b, int c) { i0 = a; i1 = b; i2 = c; } }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Chess 3D");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new Viewer());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}