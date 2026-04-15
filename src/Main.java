//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.*;
//import java.util.*;
//import java.util.List;
//
//public class Main extends JPanel, Viewer{
//
//    // ------------------- Параметры камеры -------------------
//    private double rotX = Math.toRadians(45);
//    private double rotY = Math.toRadians(45);
//    private double scale = 100.0;        // увеличено в 5 раз (было 20)
//    private int prevMouseX, prevMouseY;
//    private boolean isRotating = false;
//
//    // ------------------- Шахматная логика -------------------
//    private enum PieceType { PAWN, ROOK, BISHOP, KNIGHT, KING, QUEEN }
//    private enum Color { WHITE, BLACK }
//
//    private static class Piece {
//        PieceType type;
//        Color color;
//        int x, z;          // координаты на доске (1..8)
//        boolean hasMoved;  // для рокировки и первого хода пешки
//        Piece(PieceType type, Color color, int x, int z) {
//            this.type = type;
//            this.color = color;
//            this.x = x;
//            this.z = z;
//            this.hasMoved = false;
//        }
//        // копия для истории
//        Piece copy() {
//            Piece p = new Piece(type, color, x, z);
//            p.hasMoved = this.hasMoved;
//            return p;
//        }
//    }
//
//    private List<Piece> pieces = new ArrayList<>();      // все фигуры на доске
//    private Color currentTurn = Color.WHITE;             // белые начинают
//    private boolean gameOver = false;
//    private String gameResult = "";
//
//    // История ходов (для undo)
//    private Stack<Move> moveHistory = new Stack<>();
//
//    // Структура хода
//    private static class Move {
//        Piece piece;
//        int oldX, oldZ;
//        int newX, newZ;
//        Piece captured;        // взятая фигура (может быть null)
//        boolean wasCastling;    // флаг рокировки
//        int rookOldX, rookOldZ, rookNewX, rookNewZ; // для рокировки
//        Piece rook;
//        Move(Piece piece, int oldX, int oldZ, int newX, int newZ, Piece captured) {
//            this.piece = piece;
//            this.oldX = oldX; this.oldZ = oldZ;
//            this.newX = newX; this.newZ = newZ;
//            this.captured = captured;
//            this.wasCastling = false;
//        }
//    }
//
//    // ------------------- 3D модели фигур (увеличены в 5 раз) -------------------
//    private List<Vertex> pawnVertices, rookVertices, bishopVertices, knightVertices, kingVertices, queenVertices;
//    private List<Edge> pawnEdges, rookEdges, bishopEdges, knightEdges, kingEdges, queenEdges;
//    private List<Triangle> pawnTriangles, rookTriangles, bishopTriangles, knightTriangles, kingTriangles, queenTriangles;
//    // дополнительные элементы для коня, короля, ферзя
//    private List<Vertex> cubeVertices, kingCubeVertices, queenSphereVertices;
//    private List<Triangle> cubeTriangles, cubeWhiteTriangles, kingCubeTriangles, queenSphereTriangles;
//
//    // ------------------- Конструктор -------------------
//    public Viewer() {
//        setPreferredSize(new Dimension(1200, 800));
//        setBackground(new Color(220, 200, 180));
//
//        buildAllModels();          // построить модели с масштабом 5
//        initPieces();             // расставить фигуры
//        setupUI();                // кнопки и мышь
//    }
//
//    private void setupUI() {
//        JPanel controlPanel = new JPanel(new FlowLayout());
//        controlPanel.setBackground(new Color(240, 240, 240));
//
//        // Поля для перемещения фигуры (координаты от 1 до 8)
//        JTextField oldX = new JTextField("1", 2);
//        JTextField oldZ = new JTextField("1", 2);
//        JTextField newX = new JTextField("1", 2);
//        JTextField newZ = new JTextField("1", 2);
//        JButton moveBtn = new JButton("Переместить");
//        JButton undoBtn = new JButton("Отменить ход");
//        JButton resetBtn = new JButton("Сбросить вид");
//
//        moveBtn.addActionListener(e -> {
//            if (gameOver) {
//                JOptionPane.showMessageDialog(this, "Игра окончена. Начните новую.");
//                return;
//            }
//            try {
//                int ox = Integer.parseInt(oldX.getText());
//                int oz = Integer.parseInt(oldZ.getText());
//                int nx = Integer.parseInt(newX.getText());
//                int nz = Integer.parseInt(newZ.getText());
//                if (ox < 1 || ox > 8 || oz < 1 || oz > 8 || nx < 1 || nx > 8 || nz < 1 || nz > 8)
//                    throw new IllegalArgumentException("Координаты от 1 до 8");
//                Piece piece = getPieceAt(ox, oz);
//                if (piece == null) {
//                    JOptionPane.showMessageDialog(this, "Нет фигуры на выбранной клетке");
//                    return;
//                }
//                if (piece.color != currentTurn) {
//                    JOptionPane.showMessageDialog(this, "Сейчас ход " + currentTurn);
//                    return;
//                }
//                if (makeMove(piece, nx, nz)) {
//                    repaint();
//                    oldX.setText(""); oldZ.setText(""); newX.setText(""); newZ.setText("");
//                    if (!gameOver) checkGameState();
//                } else {
//                    JOptionPane.showMessageDialog(this, "Недопустимый ход");
//                }
//            } catch (Exception ex) {
//                JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage());
//            }
//        });
//
//        undoBtn.addActionListener(e -> undoLastMove());
//        resetBtn.addActionListener(e -> {
//            rotX = Math.toRadians(30);
//            rotY = Math.toRadians(45);
//            scale = 100;
//            repaint();
//        });
//
//        controlPanel.add(new JLabel("Откуда (X,Z):"));
//        controlPanel.add(oldX); controlPanel.add(oldZ);
//        controlPanel.add(new JLabel(" Куда (X,Z):"));
//        controlPanel.add(newX); controlPanel.add(newZ);
//        controlPanel.add(moveBtn);
//        controlPanel.add(undoBtn);
//        controlPanel.add(resetBtn);
//
//        setLayout(new BorderLayout());
//        add(controlPanel, BorderLayout.SOUTH);
//
//        // Управление камерой
//        addMouseListener(new MouseAdapter() {
//            public void mousePressed(MouseEvent e) { isRotating = true; prevMouseX = e.getX(); prevMouseY = e.getY(); }
//            public void mouseReleased(MouseEvent e) { isRotating = false; }
//        });
//        addMouseMotionListener(new MouseMotionAdapter() {
//            public void mouseDragged(MouseEvent e) {
//                if (isRotating) {
//                    rotY += (e.getX() - prevMouseX) * 0.01;
//                    rotX += (e.getY() - prevMouseY) * 0.01;
//                    prevMouseX = e.getX(); prevMouseY = e.getY();
//                    repaint();
//                }
//            }
//        });
//        addMouseWheelListener(e -> {
//            scale -= e.getWheelRotation() * 10;
//            if (scale < 30) scale = 30;
//            if (scale > 300) scale = 300;
//            repaint();
//        });
//    }
//
//    // ------------------- Инициализация фигур -------------------
//    private void initPieces() {
//        pieces.clear();
//        // Чёрные (Y=0.5, Z – номер ряда, X – номер вертикали)
//        // Пешки на 2-м ряду (z=2)
//        for (int x = 1; x <= 8; x++) pieces.add(new Piece(PieceType.PAWN, Color.BLACK, x, 2));
//        // Ладьи
//        pieces.add(new Piece(PieceType.ROOK, Color.BLACK, 1, 1));
//        pieces.add(new Piece(PieceType.ROOK, Color.BLACK, 8, 1));
//        // Кони
//        pieces.add(new Piece(PieceType.KNIGHT, Color.BLACK, 2, 1));
//        pieces.add(new Piece(PieceType.KNIGHT, Color.BLACK, 7, 1));
//        // Слоны
//        pieces.add(new Piece(PieceType.BISHOP, Color.BLACK, 3, 1));
//        pieces.add(new Piece(PieceType.BISHOP, Color.BLACK, 6, 1));
//        // Ферзь
//        pieces.add(new Piece(PieceType.QUEEN, Color.BLACK, 4, 1));
//        // Король
//        pieces.add(new Piece(PieceType.KING, Color.BLACK, 5, 1));
//
//        // Белые (Y=7.5, зеркально)
//        for (int x = 1; x <= 8; x++) pieces.add(new Piece(PieceType.PAWN, Color.WHITE, x, 7));
//        pieces.add(new Piece(PieceType.ROOK, Color.WHITE, 1, 8));
//        pieces.add(new Piece(PieceType.ROOK, Color.WHITE, 8, 8));
//        pieces.add(new Piece(PieceType.KNIGHT, Color.WHITE, 2, 8));
//        pieces.add(new Piece(PieceType.KNIGHT, Color.WHITE, 7, 8));
//        pieces.add(new Piece(PieceType.BISHOP, Color.WHITE, 3, 8));
//        pieces.add(new Piece(PieceType.BISHOP, Color.WHITE, 6, 8));
//        pieces.add(new Piece(PieceType.QUEEN, Color.WHITE, 4, 8));
//        pieces.add(new Piece(PieceType.KING, Color.WHITE, 5, 8));
//    }
//
//    // ------------------- Вспомогательные методы доски -------------------
//    private Piece getPieceAt(int x, int z) {
//        for (Piece p : pieces) if (p.x == x && p.z == z) return p;
//        return null;
//    }
//
//    private boolean isSquareAttacked(int x, int z, Color attackerColor) {
//        for (Piece p : pieces) {
//            if (p.color == attackerColor && isValidMoveWithoutSelfCheck(p, x, z, false)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    // Проверка, не оставляет ли ход своего короля под шахом
//    private boolean leavesKingInCheck(Piece piece, int newX, int newZ) {
//        // Сохраняем состояние
//        Piece captured = getPieceAt(newX, newZ);
//        int oldX = piece.x, oldZ = piece.z;
//        piece.x = newX; piece.z = newZ;
//        if (captured != null) pieces.remove(captured);
//        boolean inCheck = isKingInCheck(piece.color);
//        // Откат
//        piece.x = oldX; piece.z = oldZ;
//        if (captured != null) pieces.add(captured);
//        return inCheck;
//    }
//
//    private boolean isKingInCheck(Color color) {
//        Piece king = null;
//        for (Piece p : pieces) if (p.type == PieceType.KING && p.color == color) { king = p; break; }
//        if (king == null) return false;
//        return isSquareAttacked(king.x, king.z, oppositeColor(color));
//    }
//
//    private Color oppositeColor(Color c) { return c == Color.WHITE ? Color.BLACK : Color.WHITE; }
//
//    // ------------------- Правила ходов (без проверки шаха) -------------------
//    private boolean isValidMoveWithoutSelfCheck(Piece piece, int newX, int newZ, boolean checkOnlyDirection) {
//        if (newX < 1 || newX > 8 || newZ < 1 || newZ > 8) return false;
//        Piece target = getPieceAt(newX, newZ);
//        if (target != null && target.color == piece.color) return false;
//
//        int dx = newX - piece.x;
//        int dz = newZ - piece.z;
//
//        switch (piece.type) {
//            case PAWN:
//                int dir = (piece.color == Color.WHITE) ? -1 : 1;
//                // ход вперёд
//                if (dx == 0 && dz == dir && target == null) return true;
//                // первый ход на 2
//                if (dx == 0 && dz == 2*dir && target == null && !piece.hasMoved && getPieceAt(piece.x, piece.z+dir) == null) return true;
//                // взятие по диагонали
//                if (Math.abs(dx) == 1 && dz == dir && target != null) return true;
//                return false;
//
//            case KNIGHT:
//                return (Math.abs(dx) == 2 && Math.abs(dz) == 1) || (Math.abs(dx) == 1 && Math.abs(dz) == 2);
//
//            case KING:
//                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) return true;
//                // рокировка
//                if (!piece.hasMoved && !checkOnlyDirection && dz == 0 && Math.abs(dx) == 2 && !isKingInCheck(piece.color)) {
//                    int rookX = (dx == 2) ? 8 : 1;
//                    int rookZ = piece.z;
//                    Piece rook = getPieceAt(rookX, rookZ);
//                    if (rook != null && rook.type == PieceType.ROOK && !rook.hasMoved) {
//                        // клетки между королём и ладьей свободны и не под ударом
//                        int step = (dx > 0) ? 1 : -1;
//                        for (int x = piece.x + step; x != rookX; x += step) {
//                            if (getPieceAt(x, rookZ) != null) return false;
//                            if (isSquareAttacked(x, rookZ, oppositeColor(piece.color))) return false;
//                        }
//                        if (!isSquareAttacked(piece.x, piece.z, oppositeColor(piece.color))) return true;
//                    }
//                }
//                return false;
//
//            case QUEEN:
//            case ROOK:
//            case BISHOP:
//                int stepX = Integer.compare(dx, 0);
//                int stepZ = Integer.compare(dz, 0);
//                if (piece.type == PieceType.BISHOP && (stepX == 0 || stepZ == 0)) return false;
//                if (piece.type == PieceType.ROOK && (stepX != 0 && stepZ != 0)) return false;
//                if (stepX == 0 && stepZ == 0) return false;
//                int x = piece.x + stepX, z = piece.z + stepZ;
//                while (x != newX || z != newZ) {
//                    if (getPieceAt(x, z) != null) return false;
//                    x += stepX; z += stepZ;
//                }
//                return true;
//        }
//        return false;
//    }
//
//    // Полная проверка хода (включая шах)
//    private boolean isValidMove(Piece piece, int newX, int newZ) {
//        if (!isValidMoveWithoutSelfCheck(piece, newX, newZ, false)) return false;
//        return !leavesKingInCheck(piece, newX, newZ);
//    }
//
//    // ------------------- Выполнение хода -------------------
//    private boolean makeMove(Piece piece, int newX, int newZ) {
//        if (!isValidMove(piece, newX, newZ)) return false;
//
//        // Запоминаем ход для undo
//        Piece captured = getPieceAt(newX, newZ);
//        Move move = new Move(piece, piece.x, piece.z, newX, newZ, captured);
//        boolean isCastling = false;
//
//        // Обработка рокировки
//        if (piece.type == PieceType.KING && Math.abs(newX - piece.x) == 2 && newZ == piece.z) {
//            int rookOldX = (newX > piece.x) ? 8 : 1;
//            int rookNewX = (newX > piece.x) ? piece.x + 1 : piece.x - 1;
//            Piece rook = getPieceAt(rookOldX, newZ);
//            if (rook != null && rook.type == PieceType.ROOK && !rook.hasMoved) {
//                rook.x = rookNewX;
//                rook.hasMoved = true;
//                move.wasCastling = true;
//                move.rook = rook;
//                move.rookOldX = rookOldX; move.rookOldZ = newZ;
//                move.rookNewX = rookNewX; move.rookNewZ = newZ;
//                isCastling = true;
//            }
//        }
//
//        // Перемещение фигуры
//        piece.x = newX;
//        piece.z = newZ;
//        piece.hasMoved = true;
//        if (captured != null) pieces.remove(captured);
//
//        // Превращение пешки
//        if (piece.type == PieceType.PAWN && ((piece.color == Color.WHITE && newZ == 1) || (piece.color == Color.BLACK && newZ == 8))) {
//            promotePawn(piece);
//        }
//
//        moveHistory.push(move);
//        currentTurn = oppositeColor(currentTurn);
//        repaint();
//        return true;
//    }
//
//    private void promotePawn(Piece pawn) {
//        String[] options = {"Ферзь", "Ладья", "Слон", "Конь"};
//        int choice = JOptionPane.showOptionDialog(this, "Превращение пешки", "Выберите фигуру",
//                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
//        PieceType newType;
//        switch (choice) {
//            case 1: newType = PieceType.ROOK; break;
//            case 2: newType = PieceType.BISHOP; break;
//            case 3: newType = PieceType.KNIGHT; break;
//            default: newType = PieceType.QUEEN;
//        }
//        pawn.type = newType;
//    }
//
//    private void undoLastMove() {
//        if (moveHistory.isEmpty()) return;
//        Move last = moveHistory.pop();
//        // Откат фигуры
//        last.piece.x = last.oldX;
//        last.piece.z = last.oldZ;
//        last.piece.hasMoved = (last.oldX != last.piece.x || last.oldZ != last.piece.z); // упрощённо
//        if (last.captured != null) pieces.add(last.captured);
//        if (last.wasCastling && last.rook != null) {
//            last.rook.x = last.rookOldX;
//            last.rook.z = last.rookOldZ;
//            last.rook.hasMoved = false;
//        }
//        currentTurn = last.piece.color;
//        gameOver = false;
//        gameResult = "";
//        repaint();
//    }
//
//    // ------------------- Проверка окончания игры -------------------
//    private void checkGameState() {
//        if (isCheckmate(currentTurn)) {
//            gameOver = true;
//            gameResult = oppositeColor(currentTurn) + " победил (мат)";
//            JOptionPane.showMessageDialog(this, gameResult);
//        } else if (isStalemate(currentTurn)) {
//            gameOver = true;
//            gameResult = "Ничья (пат)";
//            JOptionPane.showMessageDialog(this, gameResult);
//        }
//    }
//
//    private boolean isCheckmate(Color color) {
//        if (!isKingInCheck(color)) return false;
//        for (Piece p : pieces) {
//            if (p.color == color) {
//                for (int x = 1; x <= 8; x++) {
//                    for (int z = 1; z <= 8; z++) {
//                        if (isValidMove(p, x, z)) return false;
//                    }
//                }
//            }
//        }
//        return true;
//    }
//
//    private boolean isStalemate(Color color) {
//        if (isKingInCheck(color)) return false;
//        for (Piece p : pieces) {
//            if (p.color == color) {
//                for (int x = 1; x <= 8; x++) {
//                    for (int z = 1; z <= 8; z++) {
//                        if (isValidMove(p, x, z)) return false;
//                    }
//                }
//            }
//        }
//        return true;
//    }
//
//    // ------------------- Построение 3D моделей (все размеры умножены на 5) -------------------
//    private void buildAllModels() {
//        // Функция масштабирования вершин
//        scaleVertices = (list, factor) -> {
//            for (Vertex v : list) { v.x *= factor; v.y *= factor; v.z *= factor; }
//        };
//        // Строим каждую модель, затем умножаем координаты на 5
//        buildPawnModel();   scaleVertices(pawnVertices, 5);
//        buildRookModel();   scaleVertices(rookVertices, 5);
//        buildBishopModel(); scaleVertices(bishopVertices, 5);
//        buildKnightModel(); scaleVertices(knightVertices, 5); scaleVertices(cubeVertices, 5);
//        buildKingModel();   scaleVertices(kingVertices, 5); scaleVertices(kingCubeVertices, 5);
//        buildQueenModel();  scaleVertices(queenVertices, 5); scaleVertices(queenSphereVertices, 5);
//    }
//
//    private interface ScaleFunc { void apply(List<Vertex> list, double factor); }
//    private ScaleFunc scaleVertices;
//
//    // Далее идут оригинальные методы построения моделей (buildPawnModel и т.д.), они не изменились, кроме того,
//    // что теперь они заполняют соответствующие списки. Чтобы не загромождать ответ, они опущены,
//    // но в полной версии они присутствуют. Ниже приведены только заглушки.
//    private void buildPawnModel() { /* ... полный код как в исходнике ... */ }
//    private void buildRookModel() { /* ... */ }
//    private void buildBishopModel() { /* ... */ }
//    private void buildKnightModel() { /* ... */ }
//    private void buildKingModel() { /* ... */ }
//    private void buildQueenModel() { /* ... */ }
//
//    // ------------------- Отрисовка -------------------
//    private double[] rotatePoint(double x, double y, double z) { /* как раньше */ }
//    private Point projectTo2D(double x, double y, double z) { /* как раньше */ }
//
//    private void drawPiece(Graphics2D g, Piece piece, List<Vertex> verts, List<Edge> edges, List<Triangle> tris,
//                           List<Vertex> extraVerts, List<Triangle> extraTris, boolean reflect) {
//        double cx = (piece.x - 0.5) * 5;
//        double cz = (piece.z - 0.5) * 5;
//        double cy = (piece.color == Color.WHITE) ? 7.5 * 5 : 0.5 * 5;
//        Color fill = (piece.color == Color.WHITE) ? new Color(255,255,255,200) : new Color(0,0,0,200);
//        drawGeneric(g, verts, edges, tris, cx, cy, cz, reflect, fill);
//        if (extraVerts != null && extraTris != null)
//            drawGeneric(g, extraVerts, null, extraTris, cx, cy, cz, reflect, new Color(100,100,100));
//    }
//
//    private void drawGeneric(Graphics2D g, List<Vertex> verts, List<Edge> edges, List<Triangle> tris,
//                             double cx, double cy, double cz, boolean reflect, Color fill) {
//        // реализация как в исходном коде, но с учётом масштаба (уже масштабировано)
//    }
//
//    @Override
//    protected void paintComponent(Graphics g) {
//        super.paintComponent(g);
//        Graphics2D g2 = (Graphics2D) g;
//        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        drawGrid(g2);
//        drawBoard(g2);
//        for (Piece p : pieces) {
//            switch (p.type) {
//                case PAWN: drawPiece(g2, p, pawnVertices, pawnEdges, pawnTriangles, null, null, p.color == Color.WHITE); break;
//                case ROOK: drawPiece(g2, p, rookVertices, rookEdges, rookTriangles, null, null, p.color == Color.WHITE); break;
//                case BISHOP: drawPiece(g2, p, bishopVertices, bishopEdges, bishopTriangles, null, null, p.color == Color.WHITE); break;
//                case KNIGHT: drawPiece(g2, p, knightVertices, knightEdges, knightTriangles, cubeVertices, cubeTriangles, p.color == Color.WHITE); break;
//                case KING: drawPiece(g2, p, kingVertices, kingEdges, kingTriangles, kingCubeVertices, kingCubeTriangles, p.color == Color.WHITE); break;
//                case QUEEN: drawPiece(g2, p, queenVertices, queenEdges, queenTriangles, queenSphereVertices, queenSphereTriangles, p.color == Color.WHITE); break;
//            }
//        }
//        drawCoordinateAxes(g2);
//    }
//
//    private void drawBoard(Graphics2D g) { /* отрисовка клеток доски, координаты умножены на 5 */ }
//    private void drawGrid(Graphics2D g) { /* как раньше */ }
//    private void drawCoordinateAxes(Graphics2D g) { /* оси увеличены в 5 раз */ }
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            JFrame frame = new JFrame("3D Chess");
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            frame.add(new Viewer());
//            frame.pack();
//            frame.setLocationRelativeTo(null);
//            frame.setVisible(true);
//        });
//    }
//}