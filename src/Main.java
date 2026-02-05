import javax.swing.*;
import java.awt.*;

public  class  Main {

    public  static  void  main (String[] args) {
        JFrame  frame  =  new  JFrame ();
        Container  pane  = frame.getContentPane();
        pane.setLayout( new  BorderLayout ());

        // ползунок для управления горизонтальным вращением
        JSlider  headingSlider  =  new  JSlider ( 0 , 360 , 180 );
        pane.add(headingSlider, BorderLayout.SOUTH);

        // ползунок для управления вертикальным вращением
        JSlider  pitchSlider  =  new  JSlider (SwingConstants.VERTICAL, - 90 , 90 , 0 );
        pane.add(pitchSlider, BorderLayout.EAST);

        // Панель для отображения результатов рендеринга
        JPanel  renderPanel  =  new  JPanel () {
            public  void  paintComponent (Graphics g) {
                Graphics2D  g2  = (Graphics2D) g;
                g2.setColor(Color.WHITE);
                g2.fillRect( 0 , 0 , getWidth(), getHeight());

                // Здесь произойдет магия рендеринга
            }
        };
        pane.add(renderPanel, BorderLayout.CENTER);

        frame.setSize( 400 , 400 );
        frame.setVisible( true );
    }
}