import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class Viewer extends JPanel {

    // ------------------- Параметры камеры -------------------
    private double rotX = Math.toRadians(45);
    private double rotY = Math.toRadians(45);
    private double scale = 20.0;
    private int prevMouseX, prevMouseY;
    private boolean isRotating = false;

    // ------------------- Шахматная логика -------------------
    private enum PieceType { PAWN, ROOK, BISHOP, KNIGHT, KING, QUEEN }
    private enum PieceColor { WHITE, BLACK }

    private static class Piece {
        PieceType type;
        PieceColor color;
        int x, y, z;               // координаты в кубе 1..8
        boolean hasMoved;
        Piece(PieceType type, PieceColor color, int x, int y, int z) {
            this.type = type;
            this.color = color;
            this.x = x;
            this.y = y;
            this.z = z;
            this.hasMoved = false;
        }
        Piece copy() {
            Piece p = new Piece(type, color, x, y, z);
            p.hasMoved = this.hasMoved;
            return p;
        }
    }

    private List<Piece> pieces = new ArrayList<>();
    private PieceColor currentTurn = PieceColor.WHITE;
    private boolean gameOver = false;
    private String gameResult = "";

    private static class Move {
        Piece piece;
        int oldX, oldY, oldZ;
        int newX, newY, newZ;
        Piece captured;
        boolean isCastling;
        int rookOldX, rookOldY, rookOldZ, rookNewX, rookNewY, rookNewZ;
        Piece rook;
        Move(Piece piece, int oldX, int oldY, int oldZ, int newX, int newY, int newZ, Piece captured) {
            this.piece = piece;
            this.oldX = oldX; this.oldY = oldY; this.oldZ = oldZ;
            this.newX = newX; this.newY = newY; this.newZ = newZ;
            this.captured = captured;
            this.isCastling = false;
        }
    }
    private Stack<Move> moveHistory = new Stack<>();

    // ------------------- 3D модели фигур (поля остаются без изменений) -------------------
    private List<Vertex> pawnVertices, rookVertices, bishopVertices, knightVertices, kingVertices, queenVertices;
    private List<Edge> pawnEdges, rookEdges, bishopEdges, knightEdges, kingEdges, queenEdges;
    private List<Triangle> pawnTriangles, rookTriangles, bishopTriangles, knightTriangles, kingTriangles, queenTriangles;
    private List<Vertex> cubeVertices, KingcubeVertices, queenSphereVertices;
    private List<Triangle> cubeTriangles, cubeWhiteTriangles, KingcubeTriangles, queenSphereTriangles;

    // ------------------- Конструктор -------------------
    public Viewer() {
        setPreferredSize(new Dimension(1200, 1200));
        setBackground(new Color(220, 200, 180));

        buildPawnModel();
        buildRookModel();
        buildBishopModel();
        buildKnightModel();
        buildKingModel();
        buildQueenModel();

        initPieces();
        setupUI();

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { isRotating = true; prevMouseX = e.getX(); prevMouseY = e.getY(); }
            public void mouseReleased(MouseEvent e) { isRotating = false; }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (isRotating) {
                    rotY += (e.getX() - prevMouseX) * 0.01;
                    rotX += (e.getY() - prevMouseY) * 0.01;
                    prevMouseX = e.getX(); prevMouseY = e.getY();
                    repaint();
                }
            }
        });
        addMouseWheelListener(e -> {
            scale -= e.getWheelRotation() * 10;
            if (scale < 20) scale = 20;
            if (scale > 500) scale = 500;
            repaint();
        });
    }

    // ------------------- Расстановка фигур (все на y=1) -------------------
    private void initPieces() {
        pieces.clear();
        // Чёрные (z=8)
        for (int x = 1; x <= 8; x++) pieces.add(new Piece(PieceType.PAWN, PieceColor.BLACK, x, 1, 7));
        pieces.add(new Piece(PieceType.ROOK, PieceColor.BLACK, 1, 1, 8));
        pieces.add(new Piece(PieceType.ROOK, PieceColor.BLACK, 8, 1, 8));
        pieces.add(new Piece(PieceType.KNIGHT, PieceColor.BLACK, 2, 1, 8));
        pieces.add(new Piece(PieceType.KNIGHT, PieceColor.BLACK, 7, 1, 8));
        pieces.add(new Piece(PieceType.BISHOP, PieceColor.BLACK, 3, 1, 8));
        pieces.add(new Piece(PieceType.BISHOP, PieceColor.BLACK, 6, 1, 8));
        pieces.add(new Piece(PieceType.QUEEN, PieceColor.BLACK, 4, 1, 8));
        pieces.add(new Piece(PieceType.KING, PieceColor.BLACK, 5, 1, 8));
        // Белые (z=1)
        for (int x = 1; x <= 8; x++) pieces.add(new Piece(PieceType.PAWN, PieceColor.WHITE, x, 1, 2));
        pieces.add(new Piece(PieceType.ROOK, PieceColor.WHITE, 1, 1, 1));
        pieces.add(new Piece(PieceType.ROOK, PieceColor.WHITE, 8, 1, 1));
        pieces.add(new Piece(PieceType.KNIGHT, PieceColor.WHITE, 2, 1, 1));
        pieces.add(new Piece(PieceType.KNIGHT, PieceColor.WHITE, 7, 1, 1));
        pieces.add(new Piece(PieceType.BISHOP, PieceColor.WHITE, 3, 1, 1));
        pieces.add(new Piece(PieceType.BISHOP, PieceColor.WHITE, 6, 1, 1));
        pieces.add(new Piece(PieceType.QUEEN, PieceColor.WHITE, 4, 1, 1));
        pieces.add(new Piece(PieceType.KING, PieceColor.WHITE, 5, 1, 1));
    }

    // ------------------- Вспомогательные методы -------------------
    private Piece getPieceAt(int x, int y, int z) {
        for (Piece p : pieces) if (p.x == x && p.y == y && p.z == z) return p;
        return null;
    }

    private PieceColor opposite(PieceColor c) {
        return c == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;
    }

    // Проверка, атакована ли клетка (x,y,z) фигурами цвета attacker
    private boolean isSquareAttacked(int x, int y, int z, PieceColor attacker) {
        for (Piece p : pieces) {
            if (p.color == attacker && isValidMoveWithoutSelfCheck(p, x, y, z, true))
                return true;
        }
        return false;
    }

    private boolean isKingInCheck(PieceColor color) {
        Piece king = null;
        for (Piece p : pieces) if (p.type == PieceType.KING && p.color == color) { king = p; break; }
        if (king == null) return false;
        return isSquareAttacked(king.x, king.y, king.z, opposite(color));
    }

    private boolean leavesKingInCheck(Piece piece, int newX, int newY, int newZ) {
        Piece captured = getPieceAt(newX, newY, newZ);
        int oldX = piece.x, oldY = piece.y, oldZ = piece.z;
        piece.x = newX; piece.y = newY; piece.z = newZ;
        if (captured != null) pieces.remove(captured);
        boolean inCheck = isKingInCheck(piece.color);
        piece.x = oldX; piece.y = oldY; piece.z = oldZ;
        if (captured != null) pieces.add(captured);
        return inCheck;
    }

    // ------------------- Правила ходов (3D) -------------------
    private boolean isValidMoveWithoutSelfCheck(Piece piece, int newX, int newY, int newZ, boolean ignoreSelfCheck) {
        if (newX < 1 || newX > 8 || newY < 1 || newY > 8 || newZ < 1 || newZ > 8) return false;
        Piece target = getPieceAt(newX, newY, newZ);
        if (target != null && target.color == piece.color) return false;

        int dx = newX - piece.x;
        int dy = newY - piece.y;
        int dz = newZ - piece.z;

        switch (piece.type) {
            case PAWN:
                int dirZ = (piece.color == PieceColor.WHITE) ? +1 : -1;
                int dirY = (piece.color == PieceColor.WHITE) ? +1 : -1;
                // ход вперёд по Z
                if (dx == 0 && dy == 0 && dz == dirZ && target == null) return true;
                // первый ход на 2 по Z
                if (dx == 0 && dy == 0 && dz == 2*dirZ && target == null && !piece.hasMoved && getPieceAt(piece.x, piece.y, piece.z+dirZ) == null) return true;
                // ход вперёд по Y
                if (dx == 0 && dz == 0 && dy == dirY && target == null) return true;
                // первый ход на 2 по Y
                if (dx == 0 && dz == 0 && dy == 2*dirY && target == null && !piece.hasMoved && getPieceAt(piece.x, piece.y+dirY, piece.z) == null) return true;
                // взятие по диагонали в плоскости XZ
                if (Math.abs(dx) == 1 && dy == 0 && dz == dirZ && target != null) return true;
                // взятие по диагонали в плоскости XY
                if (Math.abs(dx) == 1 && dz == 0 && dy == dirY && target != null) return true;
                return false;

            case KNIGHT:
                // в плоскости XY
                if (dz == 0 && ((Math.abs(dx) == 2 && Math.abs(dy) == 1) || (Math.abs(dx) == 1 && Math.abs(dy) == 2))) return true;
                // в плоскости XZ
                if (dy == 0 && ((Math.abs(dx) == 2 && Math.abs(dz) == 1) || (Math.abs(dx) == 1 && Math.abs(dz) == 2))) return true;
                // в плоскости YZ
                if (dx == 0 && ((Math.abs(dy) == 2 && Math.abs(dz) == 1) || (Math.abs(dy) == 1 && Math.abs(dz) == 2))) return true;
                return false;

            case KING:
                // ход на одну клетку в любой плоскости
                if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1) return true; // по прямой
                if (Math.abs(dx) == 1 && Math.abs(dy) == 1 && dz == 0) return true; // диагональ XY
                if (Math.abs(dx) == 1 && Math.abs(dz) == 1 && dy == 0) return true; // диагональ XZ
                if (Math.abs(dy) == 1 && Math.abs(dz) == 1 && dx == 0) return true; // диагональ YZ
                // рокировка (только в плоскости XZ, y не меняется)
                if (!piece.hasMoved && !ignoreSelfCheck && dy == 0 && dz == 0 && Math.abs(dx) == 2 && !isKingInCheck(piece.color)) {
                    int rookX = (dx == 2) ? 8 : 1;
                    Piece rook = getPieceAt(rookX, piece.y, piece.z);
                    if (rook != null && rook.type == PieceType.ROOK && !rook.hasMoved) {
                        int step = (dx > 0) ? 1 : -1;
                        for (int x = piece.x + step; x != rookX; x += step) {
                            if (getPieceAt(x, piece.y, piece.z) != null) return false;
                            if (isSquareAttacked(x, piece.y, piece.z, opposite(piece.color))) return false;
                        }
                        if (!isSquareAttacked(piece.x, piece.y, piece.z, opposite(piece.color))) return true;
                    }
                }
                return false;

            case QUEEN:
            case ROOK:
            case BISHOP:
                // Определяем, в какой плоскости происходит движение
                int fixedCoord = -1;
                if (dx != 0 && dy == 0 && dz == 0) fixedCoord = 0; // движение по X
                else if (dx == 0 && dy != 0 && dz == 0) fixedCoord = 1; // движение по Y
                else if (dx == 0 && dy == 0 && dz != 0) fixedCoord = 2; // движение по Z
                else if (dx != 0 && dy != 0 && dz == 0) fixedCoord = 3; // диагональ в XY
                else if (dx != 0 && dz != 0 && dy == 0) fixedCoord = 4; // диагональ в XZ
                else if (dy != 0 && dz != 0 && dx == 0) fixedCoord = 5; // диагональ в YZ
                else return false;

                // Проверка типа фигуры
                if (piece.type == PieceType.BISHOP && fixedCoord <= 2) return false; // слон не ходит по прямым
                if (piece.type == PieceType.ROOK && fixedCoord >= 3) return false; // ладья не ходит по диагоналям

                // Проверка, что на пути нет фигур
                int stepX = Integer.compare(dx, 0);
                int stepY = Integer.compare(dy, 0);
                int stepZ = Integer.compare(dz, 0);
                int x = piece.x + stepX, y = piece.y + stepY, z = piece.z + stepZ;
                while (x != newX || y != newY || z != newZ) {
                    if (getPieceAt(x, y, z) != null) return false;
                    x += stepX; y += stepY; z += stepZ;
                }
                return true;
        }
        return false;
    }

    private boolean isValidMove(Piece piece, int newX, int newY, int newZ) {
        if (!isValidMoveWithoutSelfCheck(piece, newX, newY, newZ, false)) return false;
        return !leavesKingInCheck(piece, newX, newY, newZ);
    }

    // ------------------- Выполнение хода -------------------
    private boolean makeMove(Piece piece, int newX, int newY, int newZ) {
        if (gameOver) return false;
        if (piece.color != currentTurn) return false;
        if (!isValidMove(piece, newX, newY, newZ)) return false;

        Piece captured = getPieceAt(newX, newY, newZ);
        Move move = new Move(piece, piece.x, piece.y, piece.z, newX, newY, newZ, captured);

        // Рокировка (только в плоскости XZ)
        if (piece.type == PieceType.KING && piece.y == newY && piece.z == newZ && Math.abs(newX - piece.x) == 2) {
            int rookOldX = (newX > piece.x) ? 8 : 1;
            int rookNewX = (newX > piece.x) ? piece.x + 1 : piece.x - 1;
            Piece rook = getPieceAt(rookOldX, piece.y, piece.z);
            if (rook != null && rook.type == PieceType.ROOK && !rook.hasMoved) {
                rook.x = rookNewX;
                rook.hasMoved = true;
                move.isCastling = true;
                move.rook = rook;
                move.rookOldX = rookOldX; move.rookOldY = piece.y; move.rookOldZ = piece.z;
                move.rookNewX = rookNewX; move.rookNewY = piece.y; move.rookNewZ = piece.z;
            }
        }

        piece.x = newX; piece.y = newY; piece.z = newZ;
        piece.hasMoved = true;
        if (captured != null) pieces.remove(captured);

        // Превращение пешки (по достижении z=8 для белых, z=1 для чёрных)
        if (piece.type == PieceType.PAWN && ((piece.color == PieceColor.WHITE && piece.z == 8) || (piece.color == PieceColor.BLACK && piece.z == 1))) {
            promotePawn(piece);
        }

        moveHistory.push(move);
        currentTurn = opposite(currentTurn);
        repaint();
        checkGameState();
        return true;
    }

    private void promotePawn(Piece pawn) {
        String[] options = {"Ферзь", "Ладья", "Слон", "Конь"};
        int choice = JOptionPane.showOptionDialog(this, "Превращение пешки", "Выберите фигуру",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        PieceType newType;
        switch (choice) {
            case 1: newType = PieceType.ROOK; break;
            case 2: newType = PieceType.BISHOP; break;
            case 3: newType = PieceType.KNIGHT; break;
            default: newType = PieceType.QUEEN;
        }
        pawn.type = newType;
    }

    private void undoLastMove() {
        if (moveHistory.isEmpty() || gameOver) return;
        Move last = moveHistory.pop();
        last.piece.x = last.oldX; last.piece.y = last.oldY; last.piece.z = last.oldZ;
        last.piece.hasMoved = (last.oldX != last.piece.x || last.oldY != last.piece.y || last.oldZ != last.piece.z);
        if (last.captured != null) pieces.add(last.captured);
        if (last.isCastling && last.rook != null) {
            last.rook.x = last.rookOldX; last.rook.y = last.rookOldY; last.rook.z = last.rookOldZ;
            last.rook.hasMoved = false;
        }
        currentTurn = last.piece.color;
        gameOver = false;
        gameResult = "";
        repaint();
    }

    // ------------------- Проверка окончания игры -------------------
    private void checkGameState() {
        if (isCheckmate(currentTurn)) {
            gameOver = true;
            gameResult = opposite(currentTurn) + " победил (мат)";
            JOptionPane.showMessageDialog(this, gameResult);
        } else if (isStalemate(currentTurn)) {
            gameOver = true;
            gameResult = "Ничья (пат)";
            JOptionPane.showMessageDialog(this, gameResult);
        }
    }

    private boolean isCheckmate(PieceColor color) {
        if (!isKingInCheck(color)) return false;
        for (Piece p : pieces) {
            if (p.color == color) {
                for (int x = 1; x <= 8; x++) {
                    for (int y = 1; y <= 8; y++) {
                        for (int z = 1; z <= 8; z++) {
                            if (isValidMove(p, x, y, z)) return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isStalemate(PieceColor color) {
        if (isKingInCheck(color)) return false;
        for (Piece p : pieces) {
            if (p.color == color) {
                for (int x = 1; x <= 8; x++) {
                    for (int y = 1; y <= 8; y++) {
                        for (int z = 1; z <= 8; z++) {
                            if (isValidMove(p, x, y, z)) return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    // ------------------- Интерфейс пользователя -------------------
    private void setupUI() {
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(new Color(240, 240, 240));

        JTextField oldX = new JTextField("1", 2);
        JTextField oldY = new JTextField("1", 2);
        JTextField oldZ = new JTextField("1", 2);
        JTextField newX = new JTextField("1", 2);
        JTextField newY = new JTextField("1", 2);
        JTextField newZ = new JTextField("1", 2);
        JButton moveBtn = new JButton("Переместить");
        JButton undoBtn = new JButton("Отменить ход");
        JButton resetViewBtn = new JButton("Сбросить вид");

        moveBtn.addActionListener(e -> {
            if (gameOver) {
                JOptionPane.showMessageDialog(this, "Игра окончена. Начните новую.");
                return;
            }
            try {
                int ox = Integer.parseInt(oldX.getText());
                int oy = Integer.parseInt(oldY.getText());
                int oz = Integer.parseInt(oldZ.getText());
                int nx = Integer.parseInt(newX.getText());
                int ny = Integer.parseInt(newY.getText());
                int nz = Integer.parseInt(newZ.getText());
                if (ox<1 || ox>8 || oy<1 || oy>8 || oz<1 || oz>8 ||
                        nx<1 || nx>8 || ny<1 || ny>8 || nz<1 || nz>8)
                    throw new IllegalArgumentException("Координаты от 1 до 8");
                Piece piece = getPieceAt(ox, oy, oz);
                if (piece == null) {
                    JOptionPane.showMessageDialog(this, "Нет фигуры на выбранной клетке");
                    return;
                }
                if (piece.color != currentTurn) {
                    JOptionPane.showMessageDialog(this, "Сейчас ход " + currentTurn);
                    return;
                }
                if (makeMove(piece, nx, ny, nz)) {
                    repaint();
                    oldX.setText(""); oldY.setText(""); oldZ.setText("");
                    newX.setText(""); newY.setText(""); newZ.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, "Недопустимый ход");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage());
            }
        });

        undoBtn.addActionListener(e -> undoLastMove());
        resetViewBtn.addActionListener(e -> {
            rotX = Math.toRadians(30);
            rotY = Math.toRadians(45);
            scale = 100;
            repaint();
        });

        controlPanel.add(new JLabel("Откуда (X,Y,Z):"));
        controlPanel.add(oldX); controlPanel.add(oldY); controlPanel.add(oldZ);
        controlPanel.add(new JLabel(" Куда (X,Y,Z):"));
        controlPanel.add(newX); controlPanel.add(newY); controlPanel.add(newZ);
        controlPanel.add(moveBtn);
        controlPanel.add(undoBtn);
        controlPanel.add(resetViewBtn);

        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.SOUTH);
    }

    // ------------------- 3D Отрисовка (координаты центров фигур) -------------------
    private double[] rotatePoint(double x, double y, double z) {
        double cosY = Math.cos(rotY), sinY = Math.sin(rotY);
        double x1 = x * cosY - z * sinY;
        double z1 = x * sinY + z * cosY;
        double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
        double y1 = y * cosX - z1 * sinX;
        double z2 = y * sinX + z1 * cosX;
        return new double[]{x1, y1, z2};
    }

    private Point projectTo2D(double x, double y, double z) {
        int centerX = getWidth() / 2, centerY = getHeight() / 2;
        return new Point((int)(x * scale) + centerX, (int)(-y * scale) + centerY);
    }

    private void drawGeneric(Graphics2D g, List<Vertex> verts, List<Edge> edges, List<Triangle> tris,
                             double cx, double cy, double cz, boolean reflect, Color fill) {
        List<Point> proj = new ArrayList<>();
        for (Vertex v : verts) {
            double lx = v.x, lz = v.z;
            if (reflect) { lx = -v.x; lz = -v.z; }
            double[] rot = rotatePoint(lx + cx, v.y + cy, lz + cz);
            proj.add(projectTo2D(rot[0], rot[1], rot[2]));
        }
        g.setColor(fill);
        for (Triangle tri : tris) {
            Point p0 = proj.get(tri.i0), p1 = proj.get(tri.i1), p2 = proj.get(tri.i2);
            g.fillPolygon(new int[]{p0.x, p1.x, p2.x}, new int[]{p0.y, p1.y, p2.y}, 3);
        }
        g.setColor(new Color(100,100,100,200));
        g.setStroke(new BasicStroke(1));
        if (edges != null) {
            for (Edge e : edges) {
                Point p1 = proj.get(e.i1), p2 = proj.get(e.i2);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
    }

    private void drawPawn(Graphics2D g, double cx, double cy, double cz, boolean white) {
        Color fill = white ? new Color(255,255,255,200) : new Color(0,0,0,200);
        drawGeneric(g, pawnVertices, pawnEdges, pawnTriangles, cx, cy, cz, white, fill);
    }

    private void drawRook(Graphics2D g, double cx, double cy, double cz, boolean white) {
        Color fill = white ? new Color(255,255,255,200) : new Color(0,0,0,200);
        drawGeneric(g, rookVertices, rookEdges, rookTriangles, cx, cy, cz, white, fill);
    }

    private void drawBishop(Graphics2D g, double cx, double cy, double cz, boolean white) {
        Color fill = white ? new Color(255,255,255,200) : new Color(0,0,0,200);
        drawGeneric(g, bishopVertices, bishopEdges, bishopTriangles, cx, cy, cz, white, fill);
    }

    private void drawKnight(Graphics2D g, double cx, double cy, double cz, boolean white) {
        Color fill = white ? new Color(255,255,255,200) : new Color(0,0,0,200);
        List<Point> proj = new ArrayList<>();
        for (Vertex v : knightVertices) {
            double lx = v.x, lz = v.z;
            if (white) { lx = -v.x; lz = -v.z; }
            double[] rot = rotatePoint(lx + cx, v.y + cy, lz + cz);
            proj.add(projectTo2D(rot[0], rot[1], rot[2]));
        }
        g.setColor(fill);
        for (Triangle tri : knightTriangles) {
            Point p0 = proj.get(tri.i0), p1 = proj.get(tri.i1), p2 = proj.get(tri.i2);
            g.fillPolygon(new int[]{p0.x, p1.x, p2.x}, new int[]{p0.y, p1.y, p2.y}, 3);
        }
        g.setColor(new Color(100,100,100,200));
        for (Edge e : knightEdges) {
            Point p1 = proj.get(e.i1), p2 = proj.get(e.i2);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
        proj.clear();
        for (Vertex v : cubeVertices) {
            double lx = v.x, lz = v.z;
            if (white) { lx = -v.x; lz = -v.z; }
            double[] rot = rotatePoint(lx + cx, v.y + cy, lz + cz);
            proj.add(projectTo2D(rot[0], rot[1], rot[2]));
        }
        g.setColor(new Color(100,100,100));
        for (Triangle tri : cubeWhiteTriangles) {
            Point p0 = proj.get(tri.i0), p1 = proj.get(tri.i1), p2 = proj.get(tri.i2);
            g.fillPolygon(new int[]{p0.x, p1.x, p2.x}, new int[]{p0.y, p1.y, p2.y}, 3);
        }
        g.setColor(fill);
        for (Triangle tri : cubeTriangles) {
            Point p0 = proj.get(tri.i0), p1 = proj.get(tri.i1), p2 = proj.get(tri.i2);
            g.fillPolygon(new int[]{p0.x, p1.x, p2.x}, new int[]{p0.y, p1.y, p2.y}, 3);
        }
        g.setColor(new Color(100,100,100,200));
        int[][] edges = { /* список рёбер коня – вставьте из исходного кода */ };
        for (int[] e : edges) {
            Point p1 = proj.get(e[0]), p2 = proj.get(e[1]);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    private void drawKing(Graphics2D g, double cx, double cy, double cz, boolean white) {
        Color fill = white ? new Color(255,255,255,200) : new Color(0,0,0,200);
        List<Point> proj = new ArrayList<>();
        for (Vertex v : kingVertices) {
            double lx = v.x, lz = v.z;
            if (white) { lx = -v.x; lz = -v.z; }
            double[] rot = rotatePoint(lx + cx, v.y + cy, lz + cz);
            proj.add(projectTo2D(rot[0], rot[1], rot[2]));
        }
        g.setColor(fill);
        for (Triangle tri : kingTriangles) {
            Point p0 = proj.get(tri.i0), p1 = proj.get(tri.i1), p2 = proj.get(tri.i2);
            g.fillPolygon(new int[]{p0.x, p1.x, p2.x}, new int[]{p0.y, p1.y, p2.y}, 3);
        }
        g.setColor(new Color(100,100,100,200));
        for (Edge e : kingEdges) {
            Point p1 = proj.get(e.i1), p2 = proj.get(e.i2);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
        proj.clear();
        for (Vertex v : KingcubeVertices) {
            double lx = v.x, lz = v.z;
            if (white) { lx = -v.x; lz = -v.z; }
            double[] rot = rotatePoint(lx + cx, v.y + cy, lz + cz);
            proj.add(projectTo2D(rot[0], rot[1], rot[2]));
        }
        g.setColor(new Color(100,100,100));
        for (Triangle tri : KingcubeTriangles) {
            Point p0 = proj.get(tri.i0), p1 = proj.get(tri.i1), p2 = proj.get(tri.i2);
            g.fillPolygon(new int[]{p0.x, p1.x, p2.x}, new int[]{p0.y, p1.y, p2.y}, 3);
        }
        g.setColor(new Color(100,100,100,200));
        int[][] kedges = { /* список рёбер короны – вставьте из исходного кода */ };
        for (int[] e : kedges) {
            Point p1 = proj.get(e[0]), p2 = proj.get(e[1]);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    private void drawQueen(Graphics2D g, double cx, double cy, double cz, boolean white) {
        Color fill = white ? new Color(255,255,255,200) : new Color(0,0,0,200);
        List<Point> proj = new ArrayList<>();
        for (Vertex v : queenVertices) {
            double lx = v.x, lz = v.z;
            if (white) { lx = -v.x; lz = -v.z; }
            double[] rot = rotatePoint(lx + cx, v.y + cy, lz + cz);
            proj.add(projectTo2D(rot[0], rot[1], rot[2]));
        }
        g.setColor(fill);
        for (Triangle tri : queenTriangles) {
            Point p0 = proj.get(tri.i0), p1 = proj.get(tri.i1), p2 = proj.get(tri.i2);
            g.fillPolygon(new int[]{p0.x, p1.x, p2.x}, new int[]{p0.y, p1.y, p2.y}, 3);
        }
        g.setColor(new Color(100,100,100,200));
        for (Edge e : queenEdges) {
            Point p1 = proj.get(e.i1), p2 = proj.get(e.i2);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
        proj.clear();
        for (Vertex v : queenSphereVertices) {
            double lx = v.x, lz = v.z;
            if (white) { lx = -v.x; lz = -v.z; }
            double[] rot = rotatePoint(lx + cx, v.y + cy, lz + cz);
            proj.add(projectTo2D(rot[0], rot[1], rot[2]));
        }
        g.setColor(new Color(100,100,100));
        for (Triangle tri : queenSphereTriangles) {
            Point p0 = proj.get(tri.i0), p1 = proj.get(tri.i1), p2 = proj.get(tri.i2);
            g.fillPolygon(new int[]{p0.x, p1.x, p2.x}, new int[]{p0.y, p1.y, p2.y}, 3);
        }
    }

    private void drawCoordinateAxes(Graphics2D g) {
        int cx = getWidth()/2, cy = getHeight()/2;
        double len = 45;
        double[][] axes = {{len,0,0},{0,len,0},{0,0,len}};
        Color[] cols = {Color.RED, Color.GREEN, Color.BLUE};
        String[] labels = {"X","Y","Z"};
        for (int i=0; i<axes.length; i++) {
            double[] r = rotatePoint(axes[i][0], axes[i][1], axes[i][2]);
            Point p = projectTo2D(r[0], r[1], r[2]);
            g.setColor(cols[i]);
            g.drawLine(cx, cy, p.x, p.y);
            drawArrow(g, cx, cy, p.x, p.y, cols[i]);
            g.drawString(labels[i], p.x+5, p.y+5);
        }
        g.setColor(Color.BLACK);
        g.fillOval(cx-3, cy-3, 6, 6);
    }

    private void drawArrow(Graphics2D g, int x1, int y1, int x2, int y2, Color c) {
        g.setColor(c);
        double a = Math.atan2(y2-y1, x2-x1);
        int sz = 10;
        int x3 = (int)(x2 - sz*Math.cos(a-Math.PI/6));
        int y3 = (int)(y2 - sz*Math.sin(a-Math.PI/6));
        int x4 = (int)(x2 - sz*Math.cos(a+Math.PI/6));
        int y4 = (int)(y2 - sz*Math.sin(a+Math.PI/6));
        g.drawLine(x2, y2, x3, y3);
        g.drawLine(x2, y2, x4, y4);
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(240,240,240));
        int cx = getWidth()/2, cy = getHeight()/2;
        int sp = 40;
        for (int x = cx%sp; x<getWidth(); x+=sp) g.drawLine(x,0,x,getHeight());
        for (int y = cy%sp; y<getHeight(); y+=sp) g.drawLine(0,y,getWidth(),y);
        g.setColor(new Color(200,200,200));
        g.drawLine(cx,0,cx,getHeight());
        g.drawLine(0,cy,getWidth(),cy);
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
    private void buildQueenModel() {
        queenVertices = new ArrayList<>();
        queenEdges = new ArrayList<>();
        queenTriangles = new ArrayList<>();
        queenSphereVertices = new ArrayList<>();
        queenSphereTriangles = new ArrayList<>();

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
                queenVertices.add(new Vertex(x, y, z));
            }
        }

        for (int i = 0; i < levelsY.length; i++) {
            int base = i * segments;
            for (int j = 0; j < segments; j++) {
                int next = (j + 1) % segments;
                queenEdges.add(new Edge(base + j, base + next));
            }
        }

        for (int i = 0; i < levelsY.length - 1; i++) {
            int baseLow = i * segments;
            int baseHigh = (i + 1) * segments;
            for (int j = 0; j < segments; j++) {
                queenEdges.add(new Edge(baseLow + j, baseHigh + j));
            }
        }

        for (int i = 0; i < levelsY.length - 1; i++) {
            for (int j = 0; j < segments; j++) {
                int nextJ = (j + 1) % segments;
                int p0 = i * segments + j;
                int p1 = i * segments + nextJ;
                int p2 = (i + 1) * segments + nextJ;
                int p3 = (i + 1) * segments + j;
                queenTriangles.add(new Triangle(p0, p1, p2));
                queenTriangles.add(new Triangle(p0, p2, p3));
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
                queenSphereVertices.add(new Vertex(x, y, z));
            }
        }

        for (int i = 0; i < segments; i++) {
            for (int j = 0; j < segments; j++) {
                int nextJ = (j + 1) % segments;
                int p0 = i * segments + j;
                int p1 = i * segments + nextJ;
                int p2 = (i + 1) * segments + nextJ;
                int p3 = (i + 1) * segments + j;
                queenSphereTriangles.add(new Triangle(p0, p1, p2));
                queenSphereTriangles.add(new Triangle(p0, p2, p3));
            }
        }

        scaleVertices(queenVertices, 5);
        scaleVertices(queenSphereVertices, 5);
    }
    private void scaleVertices(List<Vertex> list, double f) {
        for (Vertex v : list) { v.x *= f; v.y *= f; v.z *= f; }
    }

    // ------------------- Отрисовка поля -------------------
    @Override
    protected void paintComponent(Graphics gr) {
        super.paintComponent(gr);
        Graphics2D g = (Graphics2D) gr;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g);
        drawCoordinateAxes(g);

        // Отрисовка сетки куба 8x8x8 (линии по всем трём осям)
        for (int i = 0; i <= 8; i++) {
            for (int j = 0; j <= 8; j++) {
                // Линии вдоль Z (при фиксированных X и Y)
                double[] s1 = rotatePoint(i*5, j*5, 0);
                double[] e1 = rotatePoint(i*5, j*5, 8*5);
                Point p1 = projectTo2D(s1[0], s1[1], s1[2]);
                Point p2 = projectTo2D(e1[0], e1[1], e1[2]);
                g.setColor(Color.BLACK);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
                // Линии вдоль Y (при фиксированных X и Z)
                double[] s2 = rotatePoint(i*5, 0, j*5);
                double[] e2 = rotatePoint(i*5, 8*5, j*5);
                Point p3 = projectTo2D(s2[0], s2[1], s2[2]);
                Point p4 = projectTo2D(e2[0], e2[1], e2[2]);
                g.drawLine(p3.x, p3.y, p4.x, p4.y);
                // Линии вдоль X (при фиксированных Y и Z)
                double[] s3 = rotatePoint(0, i*5, j*5);
                double[] e3 = rotatePoint(8*5, i*5, j*5);
                Point p5 = projectTo2D(s3[0], s3[1], s3[2]);
                Point p6 = projectTo2D(e3[0], e3[1], e3[2]);
                g.drawLine(p5.x, p5.y, p6.x, p6.y);
            }
        }

        // Подписи осей (примерно как раньше, но теперь для трёх осей)
        for (int i = 0; i <= 8; i++) {
            double[] rX = rotatePoint((i+0.5)*5, 0, 0);
            Point pX = projectTo2D(rX[0], rX[1], rX[2]);
            g.setColor(Color.RED);
            g.drawString(Integer.toString(i+1), pX.x+5, pX.y+5);

            double[] rY = rotatePoint(0, (i+0.5)*5, 0);
            Point pY = projectTo2D(rY[0], rY[1], rY[2]);
            g.setColor(Color.GREEN);
            g.drawString(Integer.toString(i+1), pY.x+5, pY.y+5);

            double[] rZ = rotatePoint(0, 0, (i+0.5)*5);
            Point pZ = projectTo2D(rZ[0], rZ[1], rZ[2]);
            g.setColor(Color.BLUE);
            g.drawString(Integer.toString(i+1), pZ.x+5, pZ.y+5);
        }

        // Отрисовка прозрачных кубиков (клеток) – можно оставить из исходного кода, но для краткости опустим
        // (это не влияет на геймплей)

        // Отрисовка фигур
        for (Piece p : pieces) {
            double cx = (p.x - 0.5) * 5;
            double cy = (p.y - 0.5) * 5;
            double cz = (p.z - 0.5) * 5;
            boolean white = (p.color == PieceColor.WHITE);
            switch (p.type) {
                case PAWN:   drawPawn(g, cx, cy, cz, white); break;
                case ROOK:   drawRook(g, cx, cy, cz, white); break;
                case BISHOP: drawBishop(g, cx, cy, cz, white); break;
                case KNIGHT: drawKnight(g, cx, cy, cz, white); break;
                case KING:   drawKing(g, cx, cy, cz, white); break;
                case QUEEN:  drawQueen(g, cx, cy, cz, white); break;
            }
        }
    }

    // ------------------- Вспомогательные классы -------------------
    private static class Vertex { double x,y,z; Vertex(double x,double y,double z){this.x=x;this.y=y;this.z=z;} }
    private static class Edge { int i1,i2; Edge(int i1,int i2){this.i1=i1;this.i2=i2;} }
    private static class Triangle { int i0,i1,i2; Triangle(int a,int b,int c){i0=a;i1=b;i2=c;} }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3D Chess");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new Viewer());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}