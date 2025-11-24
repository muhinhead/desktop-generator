package org.dbdesktop.guiutil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class NoFrameButton extends JButton {

    private final Cursor normalCursor;
    private Object tag;
    private boolean containsCursor;

    public NoFrameButton(String iconName) {
        this(new ImageIcon(GeneralUtils.loadImage(iconName)));
        containsCursor = false;
    }

    public NoFrameButton(Icon icon) {
        super(icon);
        normalCursor = Cursor.getDefaultCursor();
        Dimension d = getPreferredSize();
        setPreferredSize(new Dimension(d.width, d.height + 20));

        addMouseListener(new MouseAdapter() {

            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    containsCursor = true;
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
            }

            public void mouseExited(MouseEvent e) {
                if (isEnabled()) {
                    containsCursor = false;
                    setCursor(normalCursor);
                }
            }
        });
    }

    //    public void setText(String lbl) {
//        super.setText(lbl);
//        repaint();
//    }
//
//    protected void paintComponent(Graphics g) {
//        g.setColor(getBackground());
//        g.fillRect(0, 0, getWidth(), getHeight());
//        g.setColor(getForeground());
//        FontMetrics fm = g.getFontMetrics();
//        int labelWidth = fm.stringWidth(getText());
//        if (getIcon() != null) {
//            int w = getIcon().getIconWidth();
//            int h = getIcon().getIconHeight();
//            getIcon().paintIcon(this, g, getWidth() / 2 - w / 2, 1);
//        }
//        g.drawString(getText(), getWidth() / 2 - labelWidth / 2, getHeight() - 5);
//    }
    @Override
    protected void paintBorder(Graphics g) {
        if (containsCursor) {
            super.paintBorder(g);
        }
    }

    /**
     * @return the tag
     */
    public Object getTag() {
        return tag;
    }

    /**
     * @param tag the tag to set
     */
    public void setTag(Object tag) {
        this.tag = tag;
    }
}
