import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class Viewer extends JPanel {

    // Параметры вращения и масштабирования (масштаб увеличен для компенсации увеличенных моделей)
    private double rotX = Math.toRadians(45);
    private double rotY = Math.toRadians(45);
    private double scale = 100.0;          // было 20, увеличено в 5 раз
    private int prevMouseX, prevMouseY;
    private boolean isRotating = false;

    // Типы фигур
    private enum PieceType { PAWN, ROOK, BISHOP, KNIGHT, KING, QEEN }

    // Класс фигуры: тип, цвет и координаты центра (в увеличенном масштабе)
    private static class Piece {
        PieceType type;
        double x, y, z;
        boolean white; // true - белая, false - чёрная
        Piece(PieceType type, double x, double y, double z, boolean white) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.white = white;
        }
    }

    private List<Piece> pieces;          // все фигуры на доске

    // Модели фигур (общие для всех экземпляров) – координаты вершин уже умножены на 5
    private List<Vertex> pawnVertices;
    private List<Edge> pawnEdges;
    private List<Triangle> pawnTriangles;

    private List<Vertex> rookVertices;
    private List<Edge> rookEdges;
    private List<Triangle> rookTriangles;

    private List<Vertex> bishopVertices;
    private List<Edge> bishopEdges;
    private List<Triangle> bishopTriangles;

    private List<Vertex> knightVertices;
    private List<Edge> knightEdges;
    private List<Triangle> knightTriangles;
    private List<Vertex> cubeVertices;
    private List<Triangle> cubeTriangles;
    private List<Triangle> cubeWhiteTriangles;

    private List<Vertex> kingVertices;
    private List<Edge> kingEdges;
    private List<Triangle> kingTriangles;
    private List<Vertex> KingcubeVertices;
    private List<Triangle> KingcubeTriangles;

    private List<Vertex> QeenVertices;
    private List<Edge> QeenEdges;
    private List<Triangle> QeenTriangles;
    private List<Vertex> QeenSphereVertices;
    private List<Triangle> QeenSphereTriangles;

    public Viewer() {
        setPreferredSize(new Dimension(800, 800));
        setBackground(new Color(220, 200, 180));

        // Построение моделей (все координаты внутри методов будут умножены на 5)
        buildPawnModel();
        buildRookModel();
        buildBishopModel();
        buildKnightModel();
        buildKingModel();
        buildQeenModel();

        // Инициализация фигур (координаты умножены на 5)
        initPieces();

        // Панель управления
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(new Color(240, 240, 240));

        JTextField oldXField = new JTextField("1", 3);
        JTextField oldYField = new JTextField("1", 3);
        JTextField oldZField = new JTextField("1", 3);
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

                if (newX < 0 || newX > 8 || newY < 0 || newY > 8 || newZ < 0 || newZ > 8 ||
                        oldX < 0 || oldX > 8 || oldY < 0 || oldY > 8 || oldZ < 0 || oldZ > 8) {
                    throw new IllegalArgumentException("Координаты должны быть от 0 до 8");
                }

                // Поиск фигуры по её центру в увеличенных координатах
                double targetX = (oldX - 0.5) * 5;
                double targetY = (oldY - 0.5) * 5;
                double targetZ = (oldZ - 0.5) * 5;

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

                // Перемещаем в новые увеличенные координаты
                found.x = (newX - 0.5) * 5;
                found.y = (newY - 0.5) * 5;
                found.z = (newZ - 0.5) * 5;
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
            scale = 100;          // новый масштаб
            repaint();
        });

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
            if (scale < 30) scale = 30;
            if (scale > 500) scale = 500;
            repaint();
        });
    }

    // Инициализация фигур (чёрные + белые) с увеличенными в 5 раз координатами
    private void initPieces() {
        pieces = new ArrayList<>();

        // ----- Чёрные фигуры (white = false) -----
        // Пешки (8 штук) – исходные координаты (1.5, 0.5, i-0.5) умножаем на 5
        for (int i = 1; i <= 8; i++) {
            pieces.add(new Piece(PieceType.PAWN, 1.5*5, 0.5*5, (i - 0.5)*5, false));
            pieces.add(new Piece(PieceType.PAWN, 1.5*5, 1.5*5, (i - 0.5)*5, false));
            pieces.add(new Piece(PieceType.PAWN, 0.5*5, 1.5*5, (i - 0.5)*5, false));
        }
        // Ладьи
        pieces.add(new Piece(PieceType.ROOK, 0.5*5, 0.5*5, 0.5*5, false));
        pieces.add(new Piece(PieceType.ROOK, 0.5*5, 0.5*5, 7.5*5, false));
        // Слоны
        pieces.add(new Piece(PieceType.BISHOP, 0.5*5, 0.5*5, 2.5*5, false));
        pieces.add(new Piece(PieceType.BISHOP, 0.5*5, 0.5*5, 5.5*5, false));
        // Кони
        pieces.add(new Piece(PieceType.KNIGHT, 0.5*5, 0.5*5, 1.5*5, false));
        pieces.add(new Piece(PieceType.KNIGHT, 0.5*5, 0.5*5, 6.5*5, false));
        // Король
        pieces.add(new Piece(PieceType.KING, 0.5*5, 0.5*5, 4.5*5, false));
        // Ферзь
        pieces.add(new Piece(PieceType.QEEN, 0.5*5, 0.5*5, 3.5*5, false));

        // ----- Белые фигуры (white = true) -----
        for (int i = 1; i <= 8; i++) {
            pieces.add(new Piece(PieceType.PAWN, 6.5*5, 0.5*5, (i - 0.5)*5, true));
            pieces.add(new Piece(PieceType.PAWN, 6.5*5, 1.5*5, (i - 0.5)*5, true));
            pieces.add(new Piece(PieceType.PAWN, 7.5*5, 1.5*5, (i - 0.5)*5, true));
        }
        // Ладьи
        pieces.add(new Piece(PieceType.ROOK, (8-0.5)*5, 0.5*5, (8-0.5)*5, true));
        pieces.add(new Piece(PieceType.ROOK, (8-0.5)*5, 0.5*5, (8-7.5)*5, true));
        // Слоны
        pieces.add(new Piece(PieceType.BISHOP, (8-0.5)*5, 0.5*5, (8-2.5)*5, true));
        pieces.add(new Piece(PieceType.BISHOP, (8-0.5)*5, 0.5*5, (8-5.5)*5, true));
        // Кони
        pieces.add(new Piece(PieceType.KNIGHT, (8-0.5)*5, 0.5*5, (8-1.5)*5, true));
        pieces.add(new Piece(PieceType.KNIGHT, (8-0.5)*5, 0.5*5, (8-6.5)*5, true));
        // Король
        pieces.add(new Piece(PieceType.KING, (8-0.5)*5, 0.5*5, (8-4.5)*5, true));
        // Ферзь
        pieces.add(new Piece(PieceType.QEEN, (8-0.5)*5, 0.5*5, (8-3.5)*5, true));
    }

    // ----- Построение моделей с последующим масштабированием в 5 раз -----
    private void scaleVertices(List<Vertex> vertices, double factor) {
        for (Vertex v : vertices) {
            v.x *= factor;
            v.y *= factor;
            v.z *= factor;
        }
    }

    private void buildPawnModel() {
        pawnVertices = new ArrayList<>();
        pawnEdges = new ArrayList<>();
        pawnTriangles = new ArrayList<>();

        double[] levelsY = {-0.35, -0.35, -0.25, -0.15, 0.06, 0.11, 0.13};
        double[] levelsR = {0, 0.25, 0.25, 0.12, 0.05, 0.13, 0.09};
        int segments = 20;

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

        for (int i = 0; i < levelsY.length; i++) {
            int base = i * segments;
            for (int j = 0; j < segments; j++) {
                int next = (j + 1) % segments;
                pawnEdges.add(new Edge(base + j, base + next));
            }
        }

        for (int i = 0; i < levelsY.length - 1; i++) {
            int baseLow = i * segments;
            int baseHigh = (i + 1) * segments;
            for (int j = 0; j < segments; j++) {
                pawnEdges.add(new Edge(baseLow + j, baseHigh + j));
            }
        }

        double sphereRadius = 0.1;
        double sphereCenterY = 0.2;
        int sphereStartIndex = pawnVertices.size();

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

        // Масштабирование модели пешки в 5 раз
        scaleVertices(pawnVertices, 5);
    }

    private void buildBishopModel() {
        bishopVertices = new ArrayList<>();
        bishopEdges = new ArrayList<>();
        bishopTriangles = new ArrayList<>();

        double[] levelsY = {-0.45, -0.45, -0.35, -0.3, -0.25, -0.10,   0,   0.02, 0.05, 0.08,   0.1,  0.12,   0.15,  0.18, 0.21, 0.24, 0.26, 0.28, 0.30,  0.315, 0.33, 0.35, 0.37, 0.38};
        double[] levelsR = {0, 0.3, 0.3,         0.2, 0.15,    0.1,   0.08, 0.1, 0.15, 0.1,    0.12,  0.09,  0.11, 0.11, 0.10, 0.08, 0.065, 0.05, 0.038, 0.033, 0.04, 0.04, 0.03, 0};
        int segments = 20;

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

        for (int i = 0; i < levelsY.length; i++) {
            int base = i * segments;
            for (int j = 0; j < segments; j++) {
                int next = (j + 1) % segments;
                bishopEdges.add(new Edge(base + j, base + next));
            }
        }

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
                bishopTriangles.add(new Triangle(p0, p1, p2));
                bishopTriangles.add(new Triangle(p0, p2, p3));
            }
        }

        scaleVertices(bishopVertices, 5);
    }

    private void buildKnightModel() {
        cubeVertices = new ArrayList<>();
        cubeTriangles = new ArrayList<>();
        cubeWhiteTriangles = new ArrayList<>();

        cubeVertices.add(new Vertex(-0.19, -0.2, -0.1)); // 0
        cubeVertices.add(new Vertex(0.18, -0.2, -0.1)); // 1
        cubeVertices.add(new Vertex(0.18, -0.2, 0.1)); // 2
        cubeVertices.add(new Vertex(-0.19, -0.2, 0.1)); // 3
        cubeVertices.add(new Vertex(-0.1, -0.2, -0.15)); // 4
        cubeVertices.add(new Vertex(0.1, -0.2, -0.15)); // 5
        cubeVertices.add(new Vertex(0.1, -0.2, 0.15)); // 6
        cubeVertices.add(new Vertex(-0.1, -0.2, 0.15)); // 7
        cubeVertices.add(new Vertex(-0.15, 0, -0.08)); // 8
        cubeVertices.add(new Vertex(0.07, 0, -0.08)); // 9
        cubeVertices.add(new Vertex(0.07, 0, 0.08)); // 10
        cubeVertices.add(new Vertex(-0.15, 0, 0.08)); // 11
        cubeVertices.add(new Vertex(-0.18, 0.1, -0.07)); // 12
        cubeVertices.add(new Vertex(0.02, 0.1, -0.07)); // 13
        cubeVertices.add(new Vertex(0.02, 0.1, 0.07)); // 14
        cubeVertices.add(new Vertex(-0.18, 0.1, 0.07)); // 15
        cubeVertices.add(new Vertex(-0.19, 0.13, -0.08)); // 16
        cubeVertices.add(new Vertex(0.02, 0.13, -0.08)); // 17
        cubeVertices.add(new Vertex(0.02, 0.13, 0.08)); // 18
        cubeVertices.add(new Vertex(-0.19, 0.13, 0.08)); // 19
        cubeVertices.add(new Vertex(0.2, 0.05, -0.04)); // 20
        cubeVertices.add(new Vertex(0.2, 0.05, 0.04)); // 21
        cubeVertices.add(new Vertex(0.215, 0.07, -0.04)); // 22
        cubeVertices.add(new Vertex(0.215, 0.07, 0.04)); // 23
        cubeVertices.add(new Vertex(0.16, 0.1, -0.05)); // 24
        cubeVertices.add(new Vertex(0.16, 0.1, 0.05)); // 25
        cubeVertices.add(new Vertex(0.17, 0.115, -0.05)); // 26
        cubeVertices.add(new Vertex(0.17, 0.115, 0.05)); // 27
        cubeVertices.add(new Vertex(0.224, 0.087, -0.04)); // 28
        cubeVertices.add(new Vertex(0.224, 0.087, 0.04)); // 29
        cubeVertices.add(new Vertex(0.24, 0.11, -0.04)); // 30
        cubeVertices.add(new Vertex(0.24, 0.11, 0.04)); // 31
        cubeVertices.add(new Vertex(-0.05, 0.3, -0.03)); // 32
        cubeVertices.add(new Vertex(-0.05, 0.3, 0.03)); // 33
        cubeVertices.add(new Vertex(-0.15, 0.3, -0.03)); // 34
        cubeVertices.add(new Vertex(-0.15, 0.3, 0.03)); // 35
        cubeVertices.add(new Vertex(-0.2, 0.2, -0.06)); // 36
        cubeVertices.add(new Vertex(-0.2, 0.2, 0.06)); // 37
        cubeVertices.add(new Vertex(0.236, 0.105, -0.04)); // 38
        cubeVertices.add(new Vertex(0.236, 0.105, 0.04)); // 39
        cubeVertices.add(new Vertex(-0.05, 0.3, -0.01)); // 40
        cubeVertices.add(new Vertex(-0.05, 0.3, 0.01)); // 41
        cubeVertices.add(new Vertex(-0.15, 0.3, -0.01)); // 42
        cubeVertices.add(new Vertex(-0.15, 0.3, 0.01)); // 43
        cubeVertices.add(new Vertex(-0.2, 0.2, -0.01)); // 44
        cubeVertices.add(new Vertex(-0.2, 0.2, 0.01)); // 45
        cubeVertices.add(new Vertex(-0.19, 0.13, -0.01)); // 46
        cubeVertices.add(new Vertex(-0.19, 0.13, 0.01)); // 47
        cubeVertices.add(new Vertex(-0.23, 0.23, -0.01)); // 48
        cubeVertices.add(new Vertex(-0.23, 0.23, 0.01)); // 49
        cubeVertices.add(new Vertex(-0.18, 0.33, -0.01)); // 50
        cubeVertices.add(new Vertex(-0.18, 0.33, 0.01)); // 51
        cubeVertices.add(new Vertex(-0.1, 0.33, -0.01)); // 52
        cubeVertices.add(new Vertex(-0.1, 0.33, 0.01)); // 53
        cubeVertices.add(new Vertex(-0.15, 0.3, -0.03)); // 54
        cubeVertices.add(new Vertex(-0.15, 0.3, 0.03)); // 55
        cubeVertices.add(new Vertex(-0.1, 0.3, -0.03)); // 56
        cubeVertices.add(new Vertex(-0.1, 0.3, 0.03)); // 57
        cubeVertices.add(new Vertex(-0.13, 0.35, -0.04)); // 58
        cubeVertices.add(new Vertex(-0.13, 0.35, 0.04)); // 59
        cubeVertices.add(new Vertex(-0.12, 0.3, -0.01)); // 60
        cubeVertices.add(new Vertex(-0.12, 0.3, 0.01)); // 61

        // Треугольники куба (основная часть)
        cubeTriangles.add(new Triangle(0, 4, 8));
        cubeTriangles.add(new Triangle(1, 5, 9));
        cubeTriangles.add(new Triangle(2, 6, 10));
        cubeTriangles.add(new Triangle(3, 7, 11));
        cubeTriangles.add(new Triangle(0, 3, 8));
        cubeTriangles.add(new Triangle(3, 8, 11));
        cubeTriangles.add(new Triangle(4, 5, 8));
        cubeTriangles.add(new Triangle(5, 8, 9));
        cubeTriangles.add(new Triangle(1, 2, 9));
        cubeTriangles.add(new Triangle(2, 9, 10));
        cubeTriangles.add(new Triangle(6, 7, 10));
        cubeTriangles.add(new Triangle(7, 10, 11));
        cubeTriangles.add(new Triangle(8, 9, 12));
        cubeTriangles.add(new Triangle(9, 12, 13));
        cubeTriangles.add(new Triangle(9, 10, 13));
        cubeTriangles.add(new Triangle(10, 13, 14));
        cubeTriangles.add(new Triangle(10, 11, 14));
        cubeTriangles.add(new Triangle(11, 14, 15));
        cubeTriangles.add(new Triangle(11, 8, 15));
        cubeTriangles.add(new Triangle(8, 15, 12));
        cubeTriangles.add(new Triangle(12, 13, 16));
        cubeTriangles.add(new Triangle(13, 16, 17));
        cubeTriangles.add(new Triangle(13, 14, 17));
        cubeTriangles.add(new Triangle(14, 17, 18));
        cubeTriangles.add(new Triangle(14, 15, 18));
        cubeTriangles.add(new Triangle(15, 18, 19));
        cubeTriangles.add(new Triangle(15, 12, 19));
        cubeTriangles.add(new Triangle(12, 19, 16));
        cubeTriangles.add(new Triangle(54, 56, 58));
        cubeTriangles.add(new Triangle(55, 57, 59));
        cubeTriangles.add(new Triangle(56, 58, 60));
        cubeTriangles.add(new Triangle(57, 59, 61));
        cubeTriangles.add(new Triangle(54, 58, 60));
        cubeTriangles.add(new Triangle(55, 59, 61));
        cubeTriangles.add(new Triangle(17, 18, 20));
        cubeTriangles.add(new Triangle(20, 21, 18));
        cubeTriangles.add(new Triangle(20, 21, 22));
        cubeTriangles.add(new Triangle(22, 23, 21));
        cubeTriangles.add(new Triangle(22, 23, 24));
        cubeTriangles.add(new Triangle(24, 25, 23));
        cubeTriangles.add(new Triangle(24, 25, 26));
        cubeTriangles.add(new Triangle(26, 27, 25));
        cubeTriangles.add(new Triangle(26, 27, 28));
        cubeTriangles.add(new Triangle(28, 29, 27));
        cubeTriangles.add(new Triangle(28, 29, 30));
        cubeTriangles.add(new Triangle(30, 31, 29));
        cubeTriangles.add(new Triangle(30, 31, 32));
        cubeTriangles.add(new Triangle(32, 33, 31));
        cubeTriangles.add(new Triangle(32, 33, 34));
        cubeTriangles.add(new Triangle(34, 35, 33));
        cubeTriangles.add(new Triangle(34, 35, 36));
        cubeTriangles.add(new Triangle(36, 37, 35));
        cubeTriangles.add(new Triangle(36, 37, 16));
        cubeTriangles.add(new Triangle(16, 19, 37));
        cubeTriangles.add(new Triangle(17, 20, 22));
        cubeTriangles.add(new Triangle(18, 21, 23));
        cubeTriangles.add(new Triangle(17, 22, 24));
        cubeTriangles.add(new Triangle(18, 23, 25));
        cubeTriangles.add(new Triangle(17, 24, 26));
        cubeTriangles.add(new Triangle(18, 25, 27));
        cubeTriangles.add(new Triangle(26, 28, 30));
        cubeTriangles.add(new Triangle(27, 29, 31));
        cubeTriangles.add(new Triangle(26, 30, 32));
        cubeTriangles.add(new Triangle(27, 31, 33));
        cubeTriangles.add(new Triangle(17, 26, 32));
        cubeTriangles.add(new Triangle(18, 27, 33));
        cubeTriangles.add(new Triangle(17, 16, 36));
        cubeTriangles.add(new Triangle(18, 19, 37));
        cubeTriangles.add(new Triangle(17, 36, 34));
        cubeTriangles.add(new Triangle(18, 37, 35));
        cubeTriangles.add(new Triangle(17, 34, 32));
        cubeTriangles.add(new Triangle(18, 35, 33));

        // Грива (белые треугольники)
        cubeWhiteTriangles.add(new Triangle(46, 47, 48));
        cubeWhiteTriangles.add(new Triangle(48, 49, 47));
        cubeWhiteTriangles.add(new Triangle(48, 49, 50));
        cubeWhiteTriangles.add(new Triangle(50, 51, 49));
        cubeWhiteTriangles.add(new Triangle(50, 51, 52));
        cubeWhiteTriangles.add(new Triangle(52, 53, 51));
        cubeWhiteTriangles.add(new Triangle(52, 53, 40));
        cubeWhiteTriangles.add(new Triangle(40, 41, 53));
        cubeWhiteTriangles.add(new Triangle(40, 42, 52));
        cubeWhiteTriangles.add(new Triangle(41, 43, 53));
        cubeWhiteTriangles.add(new Triangle(52, 50, 42));
        cubeWhiteTriangles.add(new Triangle(53, 51, 43));
        cubeWhiteTriangles.add(new Triangle(50, 42, 44));
        cubeWhiteTriangles.add(new Triangle(51, 43, 45));
        cubeWhiteTriangles.add(new Triangle(50, 48, 44));
        cubeWhiteTriangles.add(new Triangle(51, 49, 45));
        cubeWhiteTriangles.add(new Triangle(48, 46, 44));
        cubeWhiteTriangles.add(new Triangle(49, 47, 45));

        knightVertices = new ArrayList<>();
        knightEdges = new ArrayList<>();
        knightTriangles = new ArrayList<>();

        double[] levelsY = {-0.45, -0.45, -0.35, -0.3, -0.25, -0.2, -0.2};
        double[] levelsR = {0, 0.25, 0.25, 0.2, 0.23, 0.22, 0.0};
        int segments = 20;

        for (int i = 0; i < levelsY.length; i++) {
            double y = levelsY[i];
            double r = levelsR[i];
            for (int j = 0; j < segments; j++) {
                double angle = 2 * Math.PI * j / segments;
                double x = r * Math.cos(angle);
                double z = r * Math.sin(angle);
                knightVertices.add(new Vertex(x, y, z));
            }
        }

        for (int i = 0; i < levelsY.length; i++) {
            int base = i * segments;
            for (int j = 0; j < segments; j++) {
                int next = (j + 1) % segments;
                knightEdges.add(new Edge(base + j, base + next));
            }
        }

        for (int i = 0; i < levelsY.length - 1; i++) {
            int baseLow = i * segments;
            int baseHigh = (i + 1) * segments;
            for (int j = 0; j < segments; j++) {
                knightEdges.add(new Edge(baseLow + j, baseHigh + j));
            }
        }

        for (int i = 0; i < levelsY.length - 1; i++) {
            for (int j = 0; j < segments; j++) {
                int nextJ = (j + 1) % segments;
                int p0 = i * segments + j;
                int p1 = i * segments + nextJ;
                int p2 = (i + 1) * segments + nextJ;
                int p3 = (i + 1) * segments + j;
                knightTriangles.add(new Triangle(p0, p1, p2));
                knightTriangles.add(new Triangle(p0, p2, p3));
            }
        }

        scaleVertices(knightVertices, 5);
        scaleVertices(cubeVertices, 5);
    }

    private void buildKingModel() {
        KingcubeVertices = new ArrayList<>();
        KingcubeTriangles = new ArrayList<>();

        KingcubeVertices.add(new Vertex(-0.02, 0.39, -0.015)); // 0
        KingcubeVertices.add(new Vertex(0.02, 0.39, -0.015)); // 1
        KingcubeVertices.add(new Vertex(0.02, 0.39, 0.015)); // 2
        KingcubeVertices.add(new Vertex(-0.02, 0.39, 0.015)); // 3
        KingcubeVertices.add(new Vertex(-0.015, 0.42, -0.015)); // 4
        KingcubeVertices.add(new Vertex(0.015, 0.42, -0.015)); // 5
        KingcubeVertices.add(new Vertex(0.015, 0.42, 0.015)); // 6
        KingcubeVertices.add(new Vertex(-0.015, 0.42, 0.015)); // 7
        KingcubeVertices.add(new Vertex(-0.02, 0.42, -0.045)); // 8
        KingcubeVertices.add(new Vertex(0.02, 0.42, -0.045)); // 9
        KingcubeVertices.add(new Vertex(0.02, 0.42, 0.045)); // 10
        KingcubeVertices.add(new Vertex(-0.02, 0.42, 0.045)); // 11
        KingcubeVertices.add(new Vertex(-0.02, 0.45, -0.045)); // 12
        KingcubeVertices.add(new Vertex(0.02, 0.45, -0.045)); // 13
        KingcubeVertices.add(new Vertex(0.02, 0.45, 0.045)); // 14
        KingcubeVertices.add(new Vertex(-0.02, 0.45, 0.045)); // 15
        KingcubeVertices.add(new Vertex(-0.015, 0.45, -0.015)); // 16
        KingcubeVertices.add(new Vertex(0.015, 0.45, -0.015)); // 17
        KingcubeVertices.add(new Vertex(0.015, 0.45, 0.015)); // 18
        KingcubeVertices.add(new Vertex(-0.015, 0.45, 0.015)); // 19
        KingcubeVertices.add(new Vertex(-0.02, 0.48, -0.015)); // 20
        KingcubeVertices.add(new Vertex(0.02, 0.48, -0.015)); // 21
        KingcubeVertices.add(new Vertex(0.02, 0.48, 0.015)); // 22
        KingcubeVertices.add(new Vertex(-0.02, 0.48, 0.015)); // 23

        KingcubeTriangles.add(new Triangle(0, 1, 2));
        KingcubeTriangles.add(new Triangle(0, 2, 3));
        KingcubeTriangles.add(new Triangle(0, 1, 4));
        KingcubeTriangles.add(new Triangle(5, 1, 4));
        KingcubeTriangles.add(new Triangle(1, 2, 5));
        KingcubeTriangles.add(new Triangle(2, 5, 6));
        KingcubeTriangles.add(new Triangle(2, 3, 6));
        KingcubeTriangles.add(new Triangle(6, 7, 3));
        KingcubeTriangles.add(new Triangle(0, 3, 7));
        KingcubeTriangles.add(new Triangle(0, 4, 7));
        KingcubeTriangles.add(new Triangle(4, 8, 9));
        KingcubeTriangles.add(new Triangle(5, 4, 9));
        KingcubeTriangles.add(new Triangle(6, 10, 11));
        KingcubeTriangles.add(new Triangle(7, 6, 11));
        KingcubeTriangles.add(new Triangle(12, 8, 9));
        KingcubeTriangles.add(new Triangle(12, 13, 9));
        KingcubeTriangles.add(new Triangle(14, 10, 11));
        KingcubeTriangles.add(new Triangle(14, 15, 11));
        KingcubeTriangles.add(new Triangle(12, 13, 16));
        KingcubeTriangles.add(new Triangle(17, 13, 16));
        KingcubeTriangles.add(new Triangle(14, 15, 18));
        KingcubeTriangles.add(new Triangle(18, 15, 19));
        KingcubeTriangles.add(new Triangle(16, 17, 20));
        KingcubeTriangles.add(new Triangle(17, 20, 21));
        KingcubeTriangles.add(new Triangle(18, 19, 22));
        KingcubeTriangles.add(new Triangle(19, 22, 23));
        KingcubeTriangles.add(new Triangle(20, 21, 22));
        KingcubeTriangles.add(new Triangle(20, 22, 23));
        KingcubeTriangles.add(new Triangle(4, 8, 12));
        KingcubeTriangles.add(new Triangle(4, 16, 12));
        KingcubeTriangles.add(new Triangle(5, 9, 13));
        KingcubeTriangles.add(new Triangle(5, 17, 13));
        KingcubeTriangles.add(new Triangle(6, 10, 14));
        KingcubeTriangles.add(new Triangle(6, 18, 14));
        KingcubeTriangles.add(new Triangle(7, 11, 15));
        KingcubeTriangles.add(new Triangle(7, 19, 15));
        KingcubeTriangles.add(new Triangle(16, 20, 23));
        KingcubeTriangles.add(new Triangle(16, 19, 23));
        KingcubeTriangles.add(new Triangle(17, 21, 22));
        KingcubeTriangles.add(new Triangle(17, 18, 22));
        KingcubeTriangles.add(new Triangle(4, 16, 19));
        KingcubeTriangles.add(new Triangle(4, 7, 19));
        KingcubeTriangles.add(new Triangle(5, 6, 17));
        KingcubeTriangles.add(new Triangle(6, 18, 17));

        kingVertices = new ArrayList<>();
        kingEdges = new ArrayList<>();
        kingTriangles = new ArrayList<>();

        double[] levelsY = {-0.45, -0.45, -0.35, -0.25, -0.22, -0.20, -0.15, 0, 0.17, 0.19, 0.21, 0.23, 0.35, 0.37, 0.37};
        double[] levelsR = {0, 0.25, 0.25, 0.15, 0.17, 0.17, 0.11, 0.08, 0.06, 0.09, 0.09, 0.07, 0.09, 0.07, 0};
        int segments = 20;

        for (int i = 0; i < levelsY.length; i++) {
            double y = levelsY[i];
            double r = levelsR[i];
            for (int j = 0; j < segments; j++) {
                double angle = 2 * Math.PI * j / segments;
                double x = r * Math.cos(angle);
                double z = r * Math.sin(angle);
                kingVertices.add(new Vertex(x, y, z));
            }
        }

        for (int i = 0; i < levelsY.length; i++) {
            int base = i * segments;
            for (int j = 0; j < segments; j++) {
                int next = (j + 1) % segments;
                kingEdges.add(new Edge(base + j, base + next));
            }
        }

        for (int i = 0; i < levelsY.length - 1; i++) {
            int baseLow = i * segments;
            int baseHigh = (i + 1) * segments;
            for (int j = 0; j < segments; j++) {
                kingEdges.add(new Edge(baseLow + j, baseHigh + j));
            }
        }

        for (int i = 0; i < levelsY.length - 1; i++) {
            for (int j = 0; j < segments; j++) {
                int nextJ = (j + 1) % segments;
                int p0 = i * segments + j;
                int p1 = i * segments + nextJ;
                int p2 = (i + 1) * segments + nextJ;
                int p3 = (i + 1) * segments + j;
                kingTriangles.add(new Triangle(p0, p1, p2));
                kingTriangles.add(new Triangle(p0, p2, p3));
            }
        }

        scaleVertices(kingVertices, 5);
        scaleVertices(KingcubeVertices, 5);
    }

    private void buildQeenModel() {
        QeenVertices = new ArrayList<>();
        QeenEdges = new ArrayList<>();
        QeenTriangles = new ArrayList<>();
        QeenSphereVertices = new ArrayList<>();
        QeenSphereTriangles = new ArrayList<>();

        double[] levelsY = {-0.45, -0.45, -0.35, -0.25, -0.22, -0.20, -0.15, 0, 0.15, 0.17, 0.19, 0.21, 0.33, 0.34, 0.3, 0.3};
        double[] levelsR = {0, 0.25, 0.25, 0.15, 0.17, 0.17, 0.11, 0.08, 0.06, 0.09, 0.09, 0.07, 0.13, 0.12, 0.04, 0};
        int segments = 20;

        for (int i = 0; i < levelsY.length; i++) {
            double y = levelsY[i];
            double r = levelsR[i];
            for (int j = 0; j < segments; j++) {
                double angle = 2 * Math.PI * j / segments;
                double x = r * Math.cos(angle);
                double z = r * Math.sin(angle);
                QeenVertices.add(new Vertex(x, y, z));
            }
        }

        for (int i = 0; i < levelsY.length; i++) {
            int base = i * segments;
            for (int j = 0; j < segments; j++) {
                int next = (j + 1) % segments;
                QeenEdges.add(new Edge(base + j, base + next));
            }
        }

        for (int i = 0; i < levelsY.length - 1; i++) {
            int baseLow = i * segments;
            int baseHigh = (i + 1) * segments;
            for (int j = 0; j < segments; j++) {
                QeenEdges.add(new Edge(baseLow + j, baseHigh + j));
            }
        }

        for (int i = 0; i < levelsY.length - 1; i++) {
            for (int j = 0; j < segments; j++) {
                int nextJ = (j + 1) % segments;
                int p0 = i * segments + j;
                int p1 = i * segments + nextJ;
                int p2 = (i + 1) * segments + nextJ;
                int p3 = (i + 1) * segments + j;
                QeenTriangles.add(new Triangle(p0, p1, p2));
                QeenTriangles.add(new Triangle(p0, p2, p3));
            }
        }

        double sphereRadius = 0.05;
        double sphereCenterY = 0.39;

        for (int i = segments; i >= 0; i--) {
            double theta = Math.PI * i / segments;
            double y = sphereCenterY + sphereRadius * Math.cos(theta);
            double r = sphereRadius * Math.sin(theta);
            for (int j = 0; j < segments; j++) {
                double phi = 2 * Math.PI * j / segments;
                double x = r * Math.cos(phi);
                double z = r * Math.sin(phi);
                QeenSphereVertices.add(new Vertex(x, y, z));
            }
        }

        for (int i = 0; i < segments; i++) {
            for (int j = 0; j < segments; j++) {
                int nextJ = (j + 1) % segments;
                int p0 = i * segments + j;
                int p1 = i * segments + nextJ;
                int p2 = (i + 1) * segments + nextJ;
                int p3 = (i + 1) * segments + j;
                QeenSphereTriangles.add(new Triangle(p0, p1, p2));
                QeenSphereTriangles.add(new Triangle(p0, p2, p3));
            }
        }

        scaleVertices(QeenVertices, 5);
        scaleVertices(QeenSphereVertices, 5);
    }

    private void buildRookModel() {
        rookVertices = new ArrayList<>();
        rookEdges = new ArrayList<>();
        rookTriangles = new ArrayList<>();

        double[] levelsY = {-0.45, -0.45, -0.35, -0.3, -0.25, -0.10, 0, 0.25, 0.3, 0.4, 0.4, 0.35, 0.35};
        double[] levelsR = {0, 0.3, 0.3, 0.2, 0.15, 0.1, 0.08, 0.08, 0.15, 0.15, 0.12, 0.12, 0};
        int segments = 20;

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

        for (int i = 0; i < levelsY.length; i++) {
            int base = i * segments;
            for (int j = 0; j < segments; j++) {
                int next = (j + 1) % segments;
                rookEdges.add(new Edge(base + j, base + next));
            }
        }

        for (int i = 0; i < levelsY.length - 1; i++) {
            int baseLow = i * segments;
            int baseHigh = (i + 1) * segments;
            for (int j = 0; j < segments; j++) {
                rookEdges.add(new Edge(baseLow + j, baseHigh + j));
            }
        }

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

        scaleVertices(rookVertices, 5);
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

    private Point projectTo2D(double x, double y, double z) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int screenX = (int)(x * scale) + centerX;
        int screenY = (int)(-y * scale) + centerY;
        return new Point(screenX, screenY);
    }

    private void drawCoordinateAxes(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        double axisLength = 45;   // было 9, увеличено в 5 раз

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

    // Обобщённый метод отрисовки фигуры с поддержкой отражения и цвета заливки
    private void drawGeneric(Graphics2D g2d, List<Vertex> vertices, List<Edge> edges, List<Triangle> triangles,
                             double cx, double cy, double cz, boolean reflect, Color fillColor) {
        List<Point> projected = new ArrayList<>();
        for (Vertex v : vertices) {
            double lx = v.x;
            double lz = v.z;
            if (reflect) {
                lx = -v.x;
                lz = -v.z;
            }
            double wx = lx + cx;
            double wy = v.y + cy;
            double wz = lz + cz;
            double[] rot = rotatePoint(wx, wy, wz);
            projected.add(projectTo2D(rot[0], rot[1], rot[2]));
        }

        g2d.setColor(fillColor);
        for (Triangle tri : triangles) {
            Point p0 = projected.get(tri.i0);
            Point p1 = projected.get(tri.i1);
            Point p2 = projected.get(tri.i2);
            int[] xPoints = {p0.x, p1.x, p2.x};
            int[] yPoints = {p0.y, p1.y, p2.y};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }

        g2d.setColor(new Color(100, 100, 100, 200));
        g2d.setStroke(new BasicStroke(1));
        for (Edge e : edges) {
            Point p1 = projected.get(e.i1);
            Point p2 = projected.get(e.i2);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    // Методы отрисовки отдельных фигур с учётом цвета
    private void drawPawn(Graphics2D g2d, double cx, double cy, double cz, boolean white) {
        Color fill = white ? new Color(255, 255, 255, 200) : new Color(0, 0, 0, 200);
        drawGeneric(g2d, pawnVertices, pawnEdges, pawnTriangles, cx, cy, cz, white, fill);
    }

    private void drawRook(Graphics2D g2d, double cx, double cy, double cz, boolean white) {
        Color fill = white ? new Color(255, 255, 255, 200) : new Color(0, 0, 0, 200);
        drawGeneric(g2d, rookVertices, rookEdges, rookTriangles, cx, cy, cz, white, fill);
    }

    private void drawBishop(Graphics2D g2d, double cx, double cy, double cz, boolean white) {
        Color fill = white ? new Color(255, 255, 255, 200) : new Color(0, 0, 0, 200);
        drawGeneric(g2d, bishopVertices, bishopEdges, bishopTriangles, cx, cy, cz, white, fill);
    }

    private void drawKnight(Graphics2D g2d, double cx, double cy, double cz, boolean white) {
        Color fill = white ? new Color(255, 255, 255, 200) : new Color(0, 0, 0, 200);
        // Основная часть коня (тело)
        List<Point> projected = new ArrayList<>();
        for (Vertex v : knightVertices) {
            double lx = v.x, lz = v.z;
            if (white) { lx = -v.x; lz = -v.z; }
            double wx = lx + cx;
            double wy = v.y + cy;
            double wz = lz + cz;
            double[] rot = rotatePoint(wx, wy, wz);
            projected.add(projectTo2D(rot[0], rot[1], rot[2]));
        }

        g2d.setColor(fill);
        for (Triangle tri : knightTriangles) {
            Point p0 = projected.get(tri.i0);
            Point p1 = projected.get(tri.i1);
            Point p2 = projected.get(tri.i2);
            int[] xPoints = {p0.x, p1.x, p2.x};
            int[] yPoints = {p0.y, p1.y, p2.y};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }

        g2d.setColor(new Color(100, 100, 100, 200));
        g2d.setStroke(new BasicStroke(1));
        for (Edge e : knightEdges) {
            Point p1 = projected.get(e.i1);
            Point p2 = projected.get(e.i2);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // Куб (голова, грива и т.д.)
        projected.clear();
        for (Vertex v : cubeVertices) {
            double lx = v.x, lz = v.z;
            if (white) { lx = -v.x; lz = -v.z; }
            double wx = lx + cx;
            double wy = v.y + cy;
            double wz = lz + cz;
            double[] rot = rotatePoint(wx, wy, wz);
            projected.add(projectTo2D(rot[0], rot[1], rot[2]));
        }

        // Грива (белые треугольники) – для белых фигур заливается белым, для чёрных – чёрным
        g2d.setColor(new Color(100,100, 100));
        for (Triangle tri : cubeWhiteTriangles) {
            Point p0 = projected.get(tri.i0);
            Point p1 = projected.get(tri.i1);
            Point p2 = projected.get(tri.i2);
            int[] xPoints = {p0.x, p1.x, p2.x};
            int[] yPoints = {p0.y, p1.y, p2.y};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }

        g2d.setColor(fill);
        for (Triangle tri : cubeTriangles) {
            Point p0 = projected.get(tri.i0);
            Point p1 = projected.get(tri.i1);
            Point p2 = projected.get(tri.i2);
            int[] xPoints = {p0.x, p1.x, p2.x};
            int[] yPoints = {p0.y, p1.y, p2.y};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }

        g2d.setColor(new Color(100, 100, 100, 200));
        int[][] edges = {
                {0,4},{4,5},{5,1},{1,2},{2,6},{6,7},{7,3},{3,0},
                {8,9},{9,10},{10,11},{11,8},
                {0,8},{4,8},{5,9},{1,9},{2,10},{6,10},{7,11},{3,11},
                {12,13},{13,14},{14,15},{15,12},
                {8,12},{9,13},{10,14},{11,15},
                {16,17},{17,18},{18,19},{19,16},
                {12,16},{13,17},{14,18},{15,19},
                {17,20},{20,21},{21,18},
                {20,22},{22,23},{23,21},
                {22,24},{24,25},{25,23},
                {26,24},{26,27},{27,25},
                {28,26},{28,29},{29,27},
                {30,28},{30,31},{31,29},
                {32,30},{32,33},{33,31},{32,17},{33,18},
                {34,32},{34,35},{35,33},
                {36,34},{36,37},{37,35},
                {36,16},{37,19},
                {40,41},{41,43},{40,42},{42,43},
                {42,44},{44,45},{45,43},
                {44,46},{46,47},{47,45},
                {46,48},{48,49},{49,47},
                {48,50},{50,51},{51,49},
                {50,52},{52,53},{53,51},
                {52,40},{53,41},
                {54,58},{56,58},{58,60},{56,60},{54,60},
                {55,59},{57,59},{59,61},{57,61},{55,61},
        };
        for (int[] edge : edges) {
            Point p1 = projected.get(edge[0]);
            Point p2 = projected.get(edge[1]);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    private void drawKing(Graphics2D g2d, double cx, double cy, double cz, boolean white) {
        Color fill = white ? new Color(255, 255, 255, 200) : new Color(0, 0, 0, 200);
        // Основное тело
        List<Point> projected = new ArrayList<>();
        for (Vertex v : kingVertices) {
            double lx = v.x, lz = v.z;
            if (white) { lx = -v.x; lz = -v.z; }
            double wx = lx + cx;
            double wy = v.y + cy;
            double wz = lz + cz;
            double[] rot = rotatePoint(wx, wy, wz);
            projected.add(projectTo2D(rot[0], rot[1], rot[2]));
        }

        g2d.setColor(fill);
        for (Triangle tri : kingTriangles) {
            Point p0 = projected.get(tri.i0);
            Point p1 = projected.get(tri.i1);
            Point p2 = projected.get(tri.i2);
            int[] xPoints = {p0.x, p1.x, p2.x};
            int[] yPoints = {p0.y, p1.y, p2.y};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }

        g2d.setColor(new Color(100, 100, 100, 200));
        g2d.setStroke(new BasicStroke(1));
        for (Edge e : kingEdges) {
            Point p1 = projected.get(e.i1);
            Point p2 = projected.get(e.i2);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // Корона
        projected.clear();
        for (Vertex v : KingcubeVertices) {
            double lx = v.x, lz = v.z;
            if (white) { lx = -v.x; lz = -v.z; }
            double wx = lx + cx;
            double wy = v.y + cy;
            double wz = lz + cz;
            double[] rot = rotatePoint(wx, wy, wz);
            projected.add(projectTo2D(rot[0], rot[1], rot[2]));
        }

        g2d.setColor(new Color(100,100, 100));
        for (Triangle tri : KingcubeTriangles) {
            Point p0 = projected.get(tri.i0);
            Point p1 = projected.get(tri.i1);
            Point p2 = projected.get(tri.i2);
            int[] xPoints = {p0.x, p1.x, p2.x};
            int[] yPoints = {p0.y, p1.y, p2.y};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }

        g2d.setColor(new Color(100, 100, 100, 200));
        int[][] edges = {
                {0,1},{1,2},{2,3},{3,1},
                {0,4},{1,5},{2,6},{3,7},{4,5},{6,7},
                {4,8},{5,9},{6,10},{7,11},{10,11},{8,9},
                {8,12},{9,13},{10,14},{11,15},{12,13},{14,15},
                {12,16},{13,17},{14,18},{15,19},{16,17},{18,19},
                {16,20},{17,21},{18,22},{19,23},
                {20,21},{21,22},{22,23},{23,20},
        };
        for (int[] edge : edges) {
            Point p1 = projected.get(edge[0]);
            Point p2 = projected.get(edge[1]);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    private void drawQeen(Graphics2D g2d, double cx, double cy, double cz, boolean white) {
        Color fill = white ? new Color(255, 255, 255, 200) : new Color(0, 0, 0, 200);
        // Основное тело
        List<Point> projected = new ArrayList<>();
        for (Vertex v : QeenVertices) {
            double lx = v.x, lz = v.z;
            if (white) { lx = -v.x; lz = -v.z; }
            double wx = lx + cx;
            double wy = v.y + cy;
            double wz = lz + cz;
            double[] rot = rotatePoint(wx, wy, wz);
            projected.add(projectTo2D(rot[0], rot[1], rot[2]));
        }

        g2d.setColor(fill);
        for (Triangle tri : QeenTriangles) {
            Point p0 = projected.get(tri.i0);
            Point p1 = projected.get(tri.i1);
            Point p2 = projected.get(tri.i2);
            int[] xPoints = {p0.x, p1.x, p2.x};
            int[] yPoints = {p0.y, p1.y, p2.y};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }

        g2d.setColor(new Color(100, 100, 100, 200));
        g2d.setStroke(new BasicStroke(1));
        for (Edge e : QeenEdges) {
            Point p1 = projected.get(e.i1);
            Point p2 = projected.get(e.i2);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // Сфера (голова)
        projected.clear();
        for (Vertex v : QeenSphereVertices) {
            double lx = v.x, lz = v.z;
            if (white) { lx = -v.x; lz = -v.z; }
            double wx = lx + cx;
            double wy = v.y + cy;
            double wz = lz + cz;
            double[] rot = rotatePoint(wx, wy, wz);
            projected.add(projectTo2D(rot[0], rot[1], rot[2]));
        }

        g2d.setColor(new Color(100,100, 100));
        for (Triangle tri : QeenSphereTriangles) {
            Point p0 = projected.get(tri.i0);
            Point p1 = projected.get(tri.i1);
            Point p2 = projected.get(tri.i2);
            int[] xPoints = {p0.x, p1.x, p2.x};
            int[] yPoints = {p0.y, p1.y, p2.y};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawGrid(g2d);
        drawCoordinateAxes(g2d);

        // Отрисовка сетки доски – все координаты умножены на 5
        for (int i = 0; i <= 8; i++) {
            for (int j = 0; j <= 8; j++) {
                double[] startRotated = rotatePoint(j*5, i*5, 0);
                double[] endRotated = rotatePoint(j*5, i*5, 8*5);
                Point start = projectTo2D(startRotated[0], startRotated[1], startRotated[2]);
                Point end = projectTo2D(endRotated[0], endRotated[1], endRotated[2]);
                g2d.setColor(Color.BLACK);
                g2d.drawLine(start.x, start.y, end.x, end.y);
            }
            if (i != 8) {
                String label = Integer.toString(i + 1);
                double[] rotated = rotatePoint((i + 0.5)*5, 0, 0);
                Point p = projectTo2D(rotated[0], rotated[1], rotated[2]);
                g2d.setColor(Color.RED);
                g2d.drawString(label, p.x + 5, p.y + 5);
            }
        }
        for (int i = 0; i <= 8; i++) {
            for (int j = 0; j <= 8; j++) {
                double[] startRotated = rotatePoint(j*5, 0, i*5);
                double[] endRotated = rotatePoint(j*5, 8*5, i*5);
                Point start = projectTo2D(startRotated[0], startRotated[1], startRotated[2]);
                Point end = projectTo2D(endRotated[0], endRotated[1], endRotated[2]);
                g2d.setColor(Color.BLACK);
                g2d.drawLine(start.x, start.y, end.x, end.y);
            }
            if (i != 8) {
                String label = Integer.toString(i + 1);
                double[] rotated = rotatePoint(0, (i + 0.5)*5, 0);
                Point p = projectTo2D(rotated[0], rotated[1], rotated[2]);
                g2d.setColor(Color.GREEN);
                g2d.drawString(label, p.x + 5, p.y + 5);
            }
        }
        for (int i = 0; i <= 8; i++) {
            for (int j = 0; j <= 8; j++) {
                double[] startRotated = rotatePoint(0, j*5, i*5);
                double[] endRotated = rotatePoint(8*5, j*5, i*5);
                Point start = projectTo2D(startRotated[0], startRotated[1], startRotated[2]);
                Point end = projectTo2D(endRotated[0], endRotated[1], endRotated[2]);
                g2d.setColor(Color.BLACK);
                g2d.drawLine(start.x, start.y, end.x, end.y);
            }
            if (i != 8) {
                String label = Integer.toString(i + 1);
                double[] rotated = rotatePoint(0, 0, (i + 0.5)*5);
                Point p = projectTo2D(rotated[0], rotated[1], rotated[2]);
                g2d.setColor(Color.BLUE);
                g2d.drawString(label, p.x + 5, p.y + 5);
            }
        }

        // Клетки доски (зелёные и бежевые) – координаты умножены на 5
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
                            double[] p = rotatePoint(face[i][0]*5, face[i][1]*5, face[i][2]*5);
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
                            double[] p = rotatePoint(face[i][0]*5, face[i][1]*5, face[i][2]*5);
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

        // Рисуем все фигуры с учётом цвета
        for (Piece p : pieces) {
            switch (p.type) {
                case PAWN:   drawPawn(g2d, p.x, p.y, p.z, p.white); break;
                case ROOK:   drawRook(g2d, p.x, p.y, p.z, p.white); break;
                case BISHOP: drawBishop(g2d, p.x, p.y, p.z, p.white); break;
                case KNIGHT: drawKnight(g2d, p.x, p.y, p.z, p.white); break;
                case KING:   drawKing(g2d, p.x, p.y, p.z, p.white); break;
                case QEEN:   drawQeen(g2d, p.x, p.y, p.z, p.white); break;
            }
        }
    }

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