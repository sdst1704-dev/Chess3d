import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class Viewer extends JPanel {

    // Параметры вращения и масштабирования
    private double rotX = Math.toRadians(45); // 30 градусов для лучшего обзора
    private double rotY = Math.toRadians(45); // 45 градусов для лучшего обзора
    private double scale = 20.0; // Масштаб
    private int prevMouseX, prevMouseY;

    double pawnCenterX = 0.5;
    double pawnCenterY = 0.5;
    double pawnCenterZ = 0.5;

    private List<Vertex> modelVertices;
    private List<Edge> modelEdges;
    // Новый список для хранения треугольников (граней)
    private List<Triangle> modelTriangles;

//    private double rotX = 0.5, rotY = 0.5;
    private double[] rotate(double x, double y, double z) {
        double cosY = Math.cos(rotY), sinY = Math.sin(rotY);
        double x1 = x * cosY - z * sinY;
        double z1 = x * sinY + z * cosY;

        double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
        double y1 = y * cosX - z1 * sinX;
        double z2 = y * sinX + z1 * cosX;

         return new double[]{x1, y1, z2};
    }


    // Флаги для режима вращения
    private boolean isRotating = false;

    public Viewer() {
        // Настройка панели
        setPreferredSize(new Dimension(800, 800));
        setBackground(Color.WHITE);

        buildPawnModel();

        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(new Color(240, 240, 240));

        JTextField fieldX = new JTextField("1", 5);
        JTextField fieldY = new JTextField("1", 5);
        JTextField fieldZ = new JTextField("1", 5);

        JButton setCenterBtn = new JButton("Установить центр");
        setCenterBtn.addActionListener(e -> {
            try {
                double x = Double.parseDouble(fieldX.getText());
                double y = Double.parseDouble(fieldY.getText());
                double z = Double.parseDouble(fieldZ.getText());
                if (x>8||y>8||z>8||x<0||y<0||z<0){
                    throw new IllegalArgumentException();
                }
                pawnCenterX = x-0.5;
                pawnCenterY = y-0.5;
                pawnCenterZ = z-0.5;
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


        // Добавление обработчиков мыши для вращения
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

                    // Вращение вокруг осей
                    rotY += deltaX * 0.01;
                    rotX += deltaY * 0.01;

                    prevMouseX = e.getX();
                    prevMouseY = e.getY();
                    repaint();
                }
            }
        });

        // Масштабирование колесиком мыши
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int rotation = e.getWheelRotation();
                scale -= rotation * 10;

                // Ограничения масштаба
                if (scale < 10) scale = 10;
                if (scale > 500) scale = 500;

                repaint();
            }
        });
    }
    private void buildPawnModel() {
        modelVertices = new ArrayList<>();
        modelEdges = new ArrayList<>();
        modelTriangles = new ArrayList<>(); // инициализация

        // Параметры пешки (без изменений)
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
        double sphereCenterY = 0.30; // центр сферы по Y (верх пешки)

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


    // Вращение точки в 3D пространстве
    private double[] rotatePoint(double x, double y, double z) {
        // Вращение вокруг оси Y
        double cosY = Math.cos(rotY);
        double sinY = Math.sin(rotY);
        double x1 = x * cosY - z * sinY;
        double z1 = x * sinY + z * cosY;

        // Вращение вокруг оси X
        double cosX = Math.cos(rotX);
        double sinX = Math.sin(rotX);
        double y1 = y * cosX - z1 * sinX;
        double z2 = y * sinX + z1 * cosX;

        return new double[]{x1, y1, z2};
    }

    // Проекция 3D точки на 2D экран (ортографическая проекция)
    private Point projectTo2D(double x, double y, double z) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // Простая ортографическая проекция
        int screenX = (int)(x * scale) + centerX;
        int screenY = (int)(-y * scale) + centerY; // Отрицательный Y для правильной ориентации

        return new Point(screenX, screenY);
    }
    // Рисование координатных осей
    private void drawCoordinateAxes(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // Длина осей
        double axisLength = 9;

        // Векторы осей
        double[][] axes = {
                {axisLength, 0, 0},  // Ось X
                {0, axisLength, 0},  // Ось Y
                {0, 0, axisLength}   // Ось Z
        };

        // Цвета осей
        Color[] axisColors = {Color.RED, Color.GREEN, Color.BLUE};
        String[] axisLabels = {"X", "Y", "Z"};

        // Рисование каждой оси
        for (int i = 0; i < axes.length; i++) {
            double[] rotatedPoint = rotatePoint(axes[i][0], axes[i][1], axes[i][2]);
            Point screenPoint = projectTo2D(rotatedPoint[0], rotatedPoint[1], rotatedPoint[2]);
            // Линия оси
            g2d.setColor(axisColors[i]);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(centerX, centerY, screenPoint.x, screenPoint.y);

            // Стрелка на конце оси
            drawArrow(g2d, centerX, centerY, screenPoint.x, screenPoint.y, axisColors[i]);

            // Подпись оси
            g2d.drawString(axisLabels[i], screenPoint.x + 5, screenPoint.y + 5);
        }

        // Центр координат
        g2d.setColor(Color.BLACK);
        g2d.fillOval(centerX - 3, centerY - 3, 6, 6);
    }

    // Рисование стрелки
    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2, Color color) {
        g2d.setColor(color);

        // Вычисление угла линии
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowLength = 10;

        // Координаты стрелки
        int x3 = (int)(x2 - arrowLength * Math.cos(angle - Math.PI / 6));
        int y3 = (int)(y2 - arrowLength * Math.sin(angle - Math.PI / 6));
        int x4 = (int)(x2 - arrowLength * Math.cos(angle + Math.PI / 6));
        int y4 = (int)(y2 - arrowLength * Math.sin(angle + Math.PI / 6));

        // Рисование стрелки
        g2d.drawLine(x2, y2, x3, y3);
        g2d.drawLine(x2, y2, x4, y4);
    }
    private void drawModel(Graphics2D g2d) {
        // Проецируем все вершины
        List<Point> projected = new ArrayList<>();
        for (Vertex v : modelVertices) {
            double wx = v.x + pawnCenterX;
            double wy = v.y + pawnCenterY;
            double wz = v.z + pawnCenterZ;
            double[] rot = rotate(wx, wy, wz);
            projected.add(projectTo2D(rot[0], rot[1], rot[2]));
        }

        // Сначала рисуем треугольники (грани) с заливкой
        g2d.setColor(new Color(0, 0, 0, 200)); // синий полупрозрачный
        for (Triangle tri : modelTriangles) {
            Point p0 = projected.get(tri.i0);
            Point p1 = projected.get(tri.i1);
            Point p2 = projected.get(tri.i2);
            int[] xPoints = {p0.x, p1.x, p2.x};
            int[] yPoints = {p0.y, p1.y, p2.y};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }

        // Затем рисуем рёбра (контуры)
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1/5));
        for (Edge e : modelEdges) {
            Point p1 = projected.get(e.i1);
            Point p2 = projected.get(e.i2);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // Вершины (точки) - опционально
        g2d.setColor(Color.BLACK);
        for (Point p : projected) {
            g2d.fillOval(p.x - 1, p.y - 1, 2, 2);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Включение сглаживания
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Рисование координатной сетки
        drawGrid(g2d);
        //пешка
        drawModel(g2d);

        // Рисование координатных осей
        drawCoordinateAxes(g2d);
//        drawInfo(g2d);
        // Рисование сетки
        for (int i = 0; i <= 8; i++) {
            for (int j = 0; j <= 8; j++) {
                double[] startRotated = rotatePoint(j, i, 0);
                double[] endRotated = rotatePoint(j, i, 8);

                Point startPoint = projectTo2D(startRotated[0], startRotated[1], startRotated[2]);
                Point endPoint = projectTo2D(endRotated[0], endRotated[1], endRotated[2]);

                // Рисование черного отрезка
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
            }
            if (i != 8) {
                String Labels = Integer.toString(i + 1);
                double[][] axesx = {
                        {i + 0.5, 0, 0}
                };
                double[] rotatedPoint = rotatePoint(axesx[0][0], axesx[0][1], axesx[0][2]);
                Point screenPoint = projectTo2D(rotatedPoint[0], rotatedPoint[1], rotatedPoint[2]);

                // Подпись координаты
                g2d.setColor(Color.RED);
                g2d.drawString(Labels, screenPoint.x + 5, screenPoint.y + 5);
            }
        }
        for (int i = 0; i <= 8; i++) {
            for (int j = 0; j <= 8; j++) {
                double[] startRotated = rotatePoint(j, 0, i);
                double[] endRotated = rotatePoint(j, 8, i);

                Point startPoint = projectTo2D(startRotated[0], startRotated[1], startRotated[2]);
                Point endPoint = projectTo2D(endRotated[0], endRotated[1], endRotated[2]);

                // Рисование черного отрезка
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
            }

            if (i != 8) {
                String Labels = Integer.toString(i + 1);
                double[][] axesy = {
                        {0, i + 0.5, 0}
                };
                double[] rotatedPoint = rotatePoint(axesy[0][0], axesy[0][1], axesy[0][2]);
                Point screenPoint = projectTo2D(rotatedPoint[0], rotatedPoint[1], rotatedPoint[2]);

                // Подпись координаты
                g2d.setColor(Color.GREEN);
                g2d.drawString(Labels, screenPoint.x + 5, screenPoint.y + 5);
            }
        }
        for (int i = 0; i <= 8; i++) {
            for (int j = 0; j <= 8; j++) {
                double[] startRotated = rotatePoint(0, j, i);
                double[] endRotated = rotatePoint(8, j, i);

                Point startPoint = projectTo2D(startRotated[0], startRotated[1], startRotated[2]);
                Point endPoint = projectTo2D(endRotated[0], endRotated[1], endRotated[2]);

                // Рисование черного отрезка
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
            }

            if (i != 8) {
                String Label = Integer.toString(i + 1);
                double[][] axesz = {
                        {0, 0, i + 0.5}
                };
                double[] rotatedPoint = rotatePoint(axesz[0][0], axesz[0][1], axesz[0][2]);
                Point screenPoint = projectTo2D(rotatedPoint[0], rotatedPoint[1], rotatedPoint[2]);

                // Подпись координаты
                g2d.setColor(Color.BLUE);
                g2d.drawString(Label, screenPoint.x + 5, screenPoint.y + 5);
            }
        }
        for (int b = 0; b < 8; b++) {
            for (int a = 0; a < 8; a++) {
                for (int c = 0; c < 8; c+=2) {
                    // Куб
                    double[][][] CUBE = {
                            {{c+(a+b)%2, b, a},       {c+1+(a+b)%2, b, a},      {c+1+(a+b)%2, b + 1, a},      {c+(a+b)%2, b + 1, a}}, // Нижняя грань
                            {{c+(a+b)%2, b, a + 1},   {c+1+(a+b)%2, b, a + 1},  {c+1+(a+b)%2, b + 1, a + 1},  {c+(a+b)%2, b + 1, a + 1}}, // Верхняя грань
                            {{c+(a+b)%2, b, a},       {c+1+(a+b)%2, b, a},      {c+1+(a+b)%2, b, a + 1},      {c+(a+b)%2, b, a + 1}}, // Передняя грань
                            {{c+(a+b)%2, b + 1, a},   {c+1+(a+b)%2, b + 1, a},  {c+1+(a+b)%2, b + 1, a + 1},  {c+(a+b)%2, b + 1, a + 1}}, // Задняя грань
                            {{c+(a+b)%2, b, a},       {c+(a+b)%2, b + 1, a},    {c+(a+b)%2, b + 1, a + 1},    {c+(a+b)%2, b, a + 1}}, // Левая грань
                            {{c+1+(a+b)%2, b, a},     {c+1+(a+b)%2, b + 1, a},  {c+1+(a+b)%2, b + 1, a + 1},  {c+1+(a+b)%2, b, a + 1}}  // Правая грань
                    };
                    for (double[][] face : CUBE) {
                        int[] xs = new int[4];
                        int[] ys = new int[4];

                        for (int i = 0; i < 4; i++) {
                            double[] p = rotatePoint(face[i][0], face[i][1], face[i][2]);
                            Point screen = projectTo2D(p[0], p[1], p[2]);
                            xs[i] = screen.x;
                            ys[i] = screen.y;
                        }
                        // Прозрачная зеленая заливка
                        g2d.setColor(new Color(0, 200, 0, 15));
                        g2d.fillPolygon(xs, ys, 4);
                    }
                }
            }
        }
        for (int b = 0; b < 8; b++) {
            for (int a = 0; a < 8; a++) {
                for (int c = 0; c < 8; c+=2) {
                    // Куб
                    double[][][] CUBE = {
                            {{8-(c+(a+b)%2), b, a},       {8-(c+1+(a+b)%2), b, a},      {8-(c+1+(a+b)%2), b + 1, a},      {8-(c+(a+b)%2), b + 1, a}}, // Нижняя грань
                            {{8-(c+(a+b)%2), b, a + 1},   {8-(c+1+(a+b)%2), b, a + 1},  {8-(c+1+(a+b)%2), b + 1, a + 1},  {8-(c+(a+b)%2), b + 1, a + 1}}, // Верхняя грань
                            {{8-(c+(a+b)%2), b, a},       {8-(c+1+(a+b)%2), b, a},      {8-(c+1+(a+b)%2), b, a + 1},      {8-(c+(a+b)%2), b, a + 1}}, // Передняя грань
                            {{8-(c+(a+b)%2), b + 1, a},   {8-(c+1+(a+b)%2), b + 1, a},  {8-(c+1+(a+b)%2), b + 1, a + 1},  {8-(c+(a+b)%2), b + 1, a + 1}}, // Задняя грань
                            {{8-(c+(a+b)%2), b, a},       {8-(c+(a+b)%2), b + 1, a},    {8-(c+(a+b)%2), b + 1, a + 1},    {8-(c+(a+b)%2), b, a + 1}}, // Левая грань
                            {{8-(c+1+(a+b)%2), b, a},     {8-(c+1+(a+b)%2), b + 1, a},  {8-(c+1+(a+b)%2), b + 1, a + 1},  {8-(c+1+(a+b)%2), b, a + 1}}  // Правая грань
                    };
                    for (double[][] face : CUBE) {
                        int[] xs = new int[4];
                        int[] ys = new int[4];

                        for (int i = 0; i < 4; i++) {
                            double[] p = rotatePoint(face[i][0], face[i][1], face[i][2]);
                            Point screen = projectTo2D(p[0], p[1], p[2]);
                            xs[i] = screen.x;
                            ys[i] = screen.y;
                        }
                        // Прозрачная бежевая заливка
                        g2d.setColor(new Color(200, 100, 0, 15));
                        g2d.fillPolygon(xs, ys, 4);
                    }
                }
            }
        }
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





    // Рисование координатной сетки
    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(240, 240, 240)); // Светло-серый цвет сетки
        g2d.setStroke(new BasicStroke(1));

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int gridSize = 20;
        int gridSpacing = 40;

        // Вертикальные линии
        for (int x = centerX % gridSpacing; x < getWidth(); x += gridSpacing) {
            g2d.drawLine(x, 0, x, getHeight());
        }

        // Горизонтальные линии
        for (int y = centerY % gridSpacing; y < getHeight(); y += gridSpacing) {
            g2d.drawLine(0, y, getWidth(), y);
        }

        // Центральные линии (более темные)
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawLine(centerX, 0, centerX, getHeight());
        g2d.drawLine(0, centerY, getWidth(), centerY);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Создание главного окна
                JFrame frame = new JFrame("Chess 3D");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                // Создание и добавление панели отрисовки
                Viewer renderer = new Viewer();
                frame.add(renderer);

                // Настройка окна
                frame.pack();
                frame.setLocationRelativeTo(null); // Центрирование окна
                frame.setVisible(true);
            }
        });
    }
}