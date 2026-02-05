import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

import java.awt.geom.*;
import java.awt.image.BufferedImage;

public  class  Viewer {

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        // ползунок для управления горизонтальным вращением
        JSlider headingSlider = new JSlider(0, 360, 180);
        pane.add(headingSlider, BorderLayout.SOUTH);

        // ползунок для управления вертикальным вращением
        JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        pane.add(pitchSlider, BorderLayout.EAST);

        // Панель для отображения результатов рендеринга
        JPanel renderPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Здесь произойдет магия рендеринга
            }
        };
        pane.add(renderPanel, BorderLayout.CENTER);

        frame.setSize(400, 400);
        frame.setVisible(true);
    }


    // Класс Matrix3 моделирует матрицу 3x3
    class Matrix3 {
        double[] values;

        Matrix3(double[] values) {
            this.values = values;
        }

        Matrix3 multiply(Matrix3 other) {
            double[] result = new double[9];
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    for (int i = 0; i < 3; i++) {
                        result[row * 3 + col] += this.values[row * 3 + i] * other.values[i * 3 + col];
                    }
                }
            }
            return new Matrix3(result);
        }

        Vertex transform(Vertex in) {
            return new Vertex(
                    in.x * values[0] + in.y * values[3] + in.z * values[6],
                    in.x * values[1] + in.y * values[4] + in.z * values[7],
                    in.x * values[2] + in.y * values[5] + in.z * values[8]
            );
        }
    }

    public static List<Triangle> inflate(List<Triangle> tris) {
        List<Triangle> result = new ArrayList<>();
        for (Triangle t : tris) {
            Vertex m1 = new Vertex((t.v1.x + t.v2.x) / 2, (t.v1.y + t.v2.y) / 2, (t.v1.z + t.v2.z) / 2);
            Vertex m2 = new Vertex((t.v2.x + t.v3.x) / 2, (t.v2.y + t.v3.y) / 2, (t.v2.z + t.v3.z) / 2);
            Vertex m3 = new Vertex((t.v1.x + t.v3.x) / 2, (t.v1.y + t.v3.y) / 2, (t.v1.z + t.v3.z) / 2);

            result.add(new Triangle(t.v1, m1, m3, t.color));
            result.add(new Triangle(t.v2, m1, m2, t.color));
            result.add(new Triangle(t.v3, m2, m3, t.color));

            result.add(new Triangle(m1, m3, m2, t.color));
        }
        for (Triangle t : result) {
            for (Vertex v : new Vertex[]{t.v1, t.v2, t.v3}) {
                double l = Math.sqrt(vx * vx + vy * vy + vz * vz) / Math.sqrt(30000);
                vx /= l;
                vy /= l;
                vz /= l;
            }
        }
        return result;
    }

    public static Color getShade(Color color, double shade) {
        double redLiner = Math.pow(color.getRed(), 2.4) * shade;
        double greenLiner = Math.pow(color.getGreen(), 2.4) * shade;
        double blueLiner = Math.pow(color.getBlue(), 2.4) * shade;

        int red = (int) Math.pow(redLiner, 1.0 / 2.4);
        int green = (int) Math.pow(greenLiner, 1.0 / 2.4);
        int blue = (int) Math.pow(blueLiner, 1.0 / 2.4);

        return new Color(
                Math.min(255, Math.max(0, red)),
                Math.min(255, Math.max(0, green)),
                Math.min(255, Math.max(0, blue))
        );
    }
}
    // Треугольники — это основные строительные блоки 3D-модели.
    class  Triangle {
        Vertex v1;
        Vertex v2;
        Vertex v3;
        Color color;

        Triangle(Vertex v1, Vertex v2, Vertex v3, Color color) {
            this .v1 = v1;
            this .v2 = v2;
            this .v3 = v3;
            this .color = color;
        }
    }
    // Класс Vertex, представляющий точку в трехмерном пространстве.
    class  Vertex {
        double x;
        double y;
        double z;
        Vertex ( double x, double y, double z){
            this .x =x;
            this .y = y;
            this .z = z;
        }
    }