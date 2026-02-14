
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Rotating3DCube extends JPanel {

    private static final double[][] LINE = {{1,1,1}, {0,0,0}};
    private static final double[] TEXT_POINT = {2,2,2};

    // Куб: центр (1.5,1.5,1.5), ребро=1
    private static final double[][][] CUBE = {
            {{1,1,1}, {2,1,1}, {2,2,1}, {1,2,1}}, // Нижняя грань
            {{1,1,2}, {2,1,2}, {2,2,2}, {1,2,2}}, // Верхняя грань
            {{1,1,1}, {2,1,1}, {2,1,2}, {1,1,2}}, // Передняя грань
            {{1,2,1}, {2,2,1}, {2,2,2}, {1,2,2}}, // Задняя грань
            {{1,1,1}, {1,2,1}, {1,2,2}, {1,1,2}}, // Левая грань
            {{2,1,1}, {2,2,1}, {2,2,2}, {2,1,2}}  // Правая грань
    };

    private double rotX = 0.5, rotY = 0.5;
    private double zoom = 100;

    public  Rotating3DCube() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                rotY += (e.getX() - lastX) * 0.01;
                rotX += (e.getY() - lastY) * 0.01;
                lastX = e.getX();
                lastY = e.getY();
                repaint();
            }
        });
    }

    private int lastX, lastY;

    private double[] rotate(double x, double y, double z) {
        double cosY = Math.cos(rotY), sinY = Math.sin(rotY);
        double x1 = x * cosY - z * sinY;
        double z1 = x * sinY + z * cosY;

        double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
        double y1 = y * cosX - z1 * sinX;
        double z2 = y * sinX + z1 * cosX;

        return new double[]{x1, y1, z2};
    }

    private Point project(double x, double y, double z) {
        int cx = getWidth()/2, cy = getHeight()/2;
        return new Point((int)(x * zoom) + cx, (int)(-y * zoom) + cy);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = getWidth()/2, cy = getHeight()/2;

        // Оси
        g2.setColor(Color.RED);
        g2.drawLine(cx, cy, cx+100, cy);
        g2.drawString("X", cx+105, cy);

        g2.setColor(Color.GREEN);
        g2.drawLine(cx, cy, cx, cy-100);
        g2.drawString("Y", cx, cy-105);

        g2.setColor(Color.BLUE);
        g2.drawLine(cx, cy, cx+70, cy+70);
        g2.drawString("Z", cx+75, cy+75);

        // Куб (прозрачный зеленый)
        for (double[][] face : CUBE) {
            int[] xs = new int[4];
            int[] ys = new int[4];

            for (int i = 0; i < 4; i++) {
                double[] p = rotate(face[i][0], face[i][1], face[i][2]);
                Point screen = project(p[0], p[1], p[2]);
                xs[i] = screen.x;
                ys[i] = screen.y;
            }

            // Прозрачная зеленая заливка
            g2.setColor(new Color(0, 255, 0, 50));
            g2.fillPolygon(xs, ys, 4);

            // Контур грани
            g2.setColor(new Color(0, 150, 0));
            g2.drawPolygon(xs, ys, 4);
        }

        // Отрезок
        double[] p1 = rotate(1,1,1);
        double[] p2 = rotate(0,0,0);
        Point s1 = project(p1[0], p1[1], p1[2]);
        Point s2 = project(p2[0], p2[1], p2[2]);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(s1.x, s1.y, s2.x, s2.y);

        // Текст
        double[] p3 = rotate(2,2,2);
        Point s3 = project(p3[0], p3[1], p3[2]);

        g2.setColor(Color.MAGENTA);
        g2.fillOval(s3.x-4, s3.y-4, 8, 8);

        g2.setColor(Color.RED);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString("привет", s3.x+10, s3.y);

        // Инфо
        g2.setColor(Color.BLACK);
        g2.drawString("Зеленый прозрачный куб с центром (1.5,1.5,1.5)", 10, 20);
        g2.drawString("Вращайте мышью", 10, 40);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("3D с кубом");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new  Rotating3DCube());
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}