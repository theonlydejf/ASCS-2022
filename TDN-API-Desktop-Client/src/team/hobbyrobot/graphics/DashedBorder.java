package team.hobbyrobot.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.border.AbstractBorder;

public class DashedBorder extends AbstractBorder {
    @Override
    public void paintBorder(Component comp, Graphics g, int x, int y, int w, int h) {
        Graphics2D gg = (Graphics2D) g;
        gg.setColor(Color.GRAY);
        gg.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{1}, 0));
        gg.drawRect(x, y, w, h);
    }
}