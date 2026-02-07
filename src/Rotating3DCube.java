import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

// Класс для представления точки в 3D пространстве
class Point3D {
    double x, y, z;

    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Поворот точки вокруг оси X
    public void rotateX(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double newY = y * cos - z * sin;
        double newZ = y * sin + z * cos;
        y = newY;
        z = newZ;
    }

    // Поворот точки вокруг оси Y
    public void rotateY(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double newX = x * cos + z * sin;
        double newZ = -x * sin + z * cos;
        x = newX;
        z = newZ;
    }

    // Поворот точки вокруг оси Z
    public void rotateZ(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double newX = x * cos - y * sin;
        double newY = x * sin + y * cos;
        x = newX;
        y = newY;
    }

    // Проекция 3D точки на 2D экран
    public Point2D project(int width, int height, double fov) {
        double factor = fov / (fov + z);
        int screenX = (int)(x * factor) + width / 2;
        int screenY = (int)(-y * factor) + height / 2;
        return new Point2D(screenX, screenY);
    }
}


class Point2D {
    double x, y, z;

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }


}


    // Класс для представления кубической ячейки
class Cell {
    private int x, y, z; // Координаты ячейки в сетке куба (0-7)
    private Point3D[] vertices; // 8 вершин ячейки
    private int[] coordinates; // Массив координат [x, y, z]

    public Cell(int x, int y, int z, double cellSize) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.coordinates = new int[]{x, y, z};

        // Создаем вершины куба (8 точек)
        vertices = new Point3D[8];

        // Вычисляем мировые координаты вершин
        double startX = -4 * cellSize + x * cellSize;
        double startY = -4 * cellSize + y * cellSize;
        double startZ = -4 * cellSize + z * cellSize;

        // Вершины куба
        vertices[0] = new Point3D(startX, startY, startZ);
        vertices[1] = new Point3D(startX + cellSize, startY, startZ);
        vertices[2] = new Point3D(startX + cellSize, startY + cellSize, startZ);
        vertices[3] = new Point3D(startX, startY + cellSize, startZ);
        vertices[4] = new Point3D(startX, startY, startZ + cellSize);
        vertices[5] = new Point3D(startX + cellSize, startY, startZ + cellSize);
        vertices[6] = new Point3D(startX + cellSize, startY + cellSize, startZ + cellSize);
        vertices[7] = new Point3D(startX, startY + cellSize, startZ + cellSize);
    }

    // Поворот ячейки
    public void rotateX(double angle) {
        for (Point3D vertex : vertices) {
            vertex.rotateX(angle);
        }
    }

    public void rotateY(double angle) {
        for (Point3D vertex : vertices) {
            vertex.rotateY(angle);
        }
    }

    public void rotateZ(double angle) {
        for (Point3D vertex : vertices) {
            vertex.rotateZ(angle);
        }
    }

    // Получение массива координат
    public int[] getCoordinates() {
        return coordinates.clone();
    }

    // Отрисовка ячейки
    public void draw(Graphics2D g, int width, int height, double fov) {
        // Проецируем вершины на 2D
        Point2D[] projected = new Point2D[8];
        for (int i = 0; i < 8; i++) {
            projected[i] = vertices[i].project(width, height, fov);
        }

        // Устанавливаем цвет в зависимости от положения ячейки
        int colorValue = (x * 32) + (y * 32) + (z * 32);
        g.setColor(new Color(
                Math.min(255, colorValue),
                Math.min(255, 128 + x * 16),
                Math.min(255, 128 + z * 16)
        ));

        // Рисуем грани куба
        // Передняя грань
        drawQuad(g, projected, new int[]{0, 1, 2, 3});

        // Задняя грань
        drawQuad(g, projected, new int[]{4, 5, 6, 7});

        // Верхняя грань
        drawQuad(g, projected, new int[]{3, 2, 6, 7});

        // Нижняя грань
        drawQuad(g, projected, new int[]{0, 1, 5, 4});

        // Левая грань
        drawQuad(g, projected, new int[]{0, 3, 7, 4});

        // Правая грань
        drawQuad(g, projected, new int[]{1, 2, 6, 5});
    }

    // Рисование четырехугольника по точкам
    private void drawQuad(Graphics2D g, Point2D[] points, int[] indices) {
        int[] xPoints = new int[4];
        int[] yPoints = new int[4];

        for (int i = 0; i < 4; i++) {
            xPoints[i] = x;
            yPoints[i] = y;
        }

        // Рисуем заполненный полигон с более прозрачным цветом
        Color fillColor = new Color(g.getColor().getRed(), g.getColor().getGreen(), g.getColor().getBlue(), 50);
        g.setColor(fillColor);
        g.fillPolygon(xPoints, yPoints, 4);

        // Рисуем контур более темным цветом
        g.setColor(g.getColor().darker().darker());
        g.drawPolygon(xPoints, yPoints, 4);
    }
}

// Класс для хранения массива всех ячеек
class CubeGrid {
    private ArrayList<Cell> cells;
    private ArrayList<int[]> coordinatesArray;
    private int cellsPerSide;
    private double cellSize;

    public CubeGrid(int cellsPerSide, double cellSize) {
        this.cellsPerSide = cellsPerSide;
        this.cellSize = cellSize;
        this.cells = new ArrayList<>();
        this.coordinatesArray = new ArrayList<>();

        createGrid();
    }

    private void createGrid() {
        // Создаем 512 ячеек (8×8×8)
        for (int x = 0; x < cellsPerSide; x++) {
            for (int y = 0; y < cellsPerSide; y++) {
                for (int z = 0; z < cellsPerSide; z++) {
                    Cell cell = new Cell(x, y, z, cellSize);
                    cells.add(cell);
                    coordinatesArray.add(cell.getCoordinates());
                }
            }
        }
    }

    // Поворот всех ячеек
    public void rotateX(double angle) {
        for (Cell cell : cells) {
            cell.rotateX(angle);
        }
    }

    public void rotateY(double angle) {
        for (Cell cell : cells) {
            cell.rotateY(angle);
        }
    }

    public void rotateZ(double angle) {
        for (Cell cell : cells) {
            cell.rotateZ(angle);
        }
    }

    // Получение массива координат всех ячеек
    public ArrayList<int[]> getCoordinatesArray() {
        return coordinatesArray;
    }

    // Получение списка ячеек
    public ArrayList<Cell> getCells() {
        return cells;
    }

    // Отрисовка всех ячеек
    public void draw(Graphics2D g, int width, int height, double fov) {
        // Сортируем ячейки по Z-координате для правильного отображения
        ArrayList<Cell> sortedCells = new ArrayList<>(cells);
        sortedCells.sort((c1, c2) -> {
            // Простая сортировка по центру Z
            return Double.compare(getCellCenterZ(c2), getCellCenterZ(c1));
        });

        for (Cell cell : sortedCells) {
            cell.draw(g, width, height, fov);
        }
    }

    private double getCellCenterZ(Cell cell) {
        // Простая реализация для получения Z центра ячейки
        int[] coords = cell.getCoordinates();
        return coords[2];
    }
}

// Основной класс для отрисовки куба
class CubePanel extends JPanel {
    private CubeGrid cubeGrid;
    private double rotationX = 0;
    private double rotationY = 0;
    private double rotationZ = 0;
    private double fov = 500;
    private double cellSize = 30;

    // Элементы управления
    private JSlider xSlider, ySlider, zSlider;

    public CubePanel() {
        setBackground(Color.BLACK);
        cubeGrid = new CubeGrid(8, cellSize);
        createControls();
    }

    private void createControls() {
        // Создаем слайдеры для управления вращением
        xSlider = new JSlider(JSlider.HORIZONTAL, 0, 360, 0);
        ySlider = new JSlider(JSlider.HORIZONTAL, 0, 360, 0);
        zSlider = new JSlider(JSlider.HORIZONTAL, 0, 360, 0);

        xSlider.addChangeListener(e -> {
            rotationX = Math.toRadians(xSlider.getValue());
            repaint();
        });

        ySlider.addChangeListener(e -> {
            rotationY = Math.toRadians(ySlider.getValue());
            repaint();
        });

        zSlider.addChangeListener(e -> {
            rotationZ = Math.toRadians(zSlider.getValue());
            repaint();
        });

        // Панель управления
        JPanel controlPanel = new JPanel(new GridLayout(3, 2));
        controlPanel.setOpaque(false);
        controlPanel.add(new JLabel("Вращение X:"));
        controlPanel.add(xSlider);
        controlPanel.add(new JLabel("Вращение Y:"));
        controlPanel.add(ySlider);
        controlPanel.add(new JLabel("Вращение Z:"));
        controlPanel.add(zSlider);

        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.SOUTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Создаем временную копию куба для текущего кадра
        CubeGrid tempGrid = new CubeGrid(8, cellSize);

        // Применяем вращение
        tempGrid.rotateX(rotationX);
        tempGrid.rotateY(rotationY);
        tempGrid.rotateZ(rotationZ);

        // Рисуем куб
        tempGrid.draw(g2d, width, height, fov);

        // Отображаем информацию
        g2d.setColor(Color.WHITE);
        g2d.drawString("3D Куб 8×8×8 (512 ячеек)", 10, 20);
        g2d.drawString("Используйте слайдеры для вращения", 10, 40);

        // Отображаем текущие углы
        g2d.drawString(String.format("Углы: X=%.1f° Y=%.1f° Z=%.1f°",
                Math.toDegrees(rotationX),
                Math.toDegrees(rotationY),
                Math.toDegrees(rotationZ)), 10, 60);

        // Отображаем информацию о массиве координат
        ArrayList<int[]> coords = cubeGrid.getCoordinatesArray();
        g2d.drawString("Координаты хранятся в массиве из " + coords.size() + " элементов", 10, 80);
    }
}

// Главное окно приложения
public class Rotating3DCube extends JFrame {

    public Rotating3DCube() {
        setTitle("3D Куб 8×8×8 с вращением");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);

        // Добавляем панель с кубом
        CubePanel cubePanel = new CubePanel();
        add(cubePanel);

        // Выводим информацию о массиве координат в консоль
        printCoordinatesInfo();
    }

    private void printCoordinatesInfo() {
        System.out.println("=== 3D Куб 8×8×8 ===");
        System.out.println("Общее количество ячеек: 512");
        System.out.println("\nКоординаты ячеек хранятся в массиве:");

        // Создаем пример координат для демонстрации
        System.out.println("\nПримеры координат первых 10 ячеек:");
        int count = 0;
        for (int x = 0; x < 2 && count < 10; x++) {
            for (int y = 0; y < 2 && count < 10; y++) {
                for (int z = 0; z < 2 && count < 10; z++) {
                    System.out.printf("Ячейка %d: [%d, %d, %d]\n",
                            count + 1, x, y, z);
                    count++;
                }
            }
        }

        System.out.println("\nКоординаты последних 10 ячеек:");
        count = 0;
        for (int x = 6; x < 8 && count < 10; x++) {
            for (int y = 6; y < 8 && count < 10; y++) {
                for (int z = 6; z < 8 && count < 10; z++) {
                    System.out.printf("Ячейка %d: [%d, %d, %d]\n",
                            512 - 10 + count + 1, x, y, z);
                    count++;
                }
            }
        }

        System.out.println("\nВсе координаты хранятся в ArrayList<int[]>");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Rotating3DCube frame = new Rotating3DCube();
            frame.setVisible(true);
        });
    }
}