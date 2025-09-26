package org.dbdesktop.guiutil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class ToolBarButton extends JButton implements MouseListener {

    private static final double KOEFF = 1.2;
    private static Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
    private boolean containsCursor = false;
    private static ImageIcon staticIcon = null;
    private static Image staticImage = null;
    protected Image originalImage = null;
    protected ImageIcon originalIcon = null;
    protected int originalWidth;
    protected Rectangle originalRect;
    protected boolean animate = false;

    public ToolBarButton(Icon icon) {
        super(icon);
//        originalRect = getBounds();
        setVerticalTextPosition(BOTTOM);
        setHorizontalTextPosition(CENTER);
        addMouseListener(this);
    }

    public ToolBarButton(String imageFile, boolean animate) {
        super(staticIcon = new ImageIcon(staticImage = GeneralUtils.loadImage(imageFile)));
//        originalRect = getBounds();
        this.animate = animate;
        originalImage = staticImage;
        originalIcon = staticIcon;
        originalWidth = originalIcon.getIconWidth();
        addMouseListener(this);
    }

//    public ToolBarButton(String imageFile, boolean animate, Dimension d) {
//        this(imageFile, animate);
//        scaleTo(d);
//    }

    public void scaleTo(Dimension d) {
        if (originalIcon != null) {
            originalImage = originalIcon.getImage().getScaledInstance(d.width, d.height, Image.SCALE_SMOOTH);
            originalIcon.setImage(originalImage);
            originalWidth = originalIcon.getIconWidth();
        }
    }

    public ToolBarButton(String imageFile) {
        this(imageFile, false);
    }

    public ToolBarButton(String imageFile, String text) {
        super(text);
        setIcon(staticIcon = new ImageIcon(staticImage = GeneralUtils.loadImage(imageFile)));
//        originalRect = getBounds();
        originalImage = staticImage;
        originalIcon = staticIcon;
        originalWidth = staticIcon.getIconWidth();
        addMouseListener(this);
    }

    @Override
    protected void paintBorder(Graphics g) {
        if (containsCursor) {
            super.paintBorder(g);
        }
    }

    public void mouseClicked(MouseEvent e) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mousePressed(MouseEvent e) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseReleased(MouseEvent e) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseEntered(MouseEvent e) {
        if (animate) {
            originalRect = getBounds();
            if (isEnabled()) {
                containsCursor = true;
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                if (originalImage != null) {
                    int deltaX, deltaY;
                    deltaX = (int) (originalWidth * 1.3 - originalWidth) / 2;
                    deltaY = (int) (originalRect.height * 1.3 - originalRect.height) / 2;
                    Rectangle newBounds
                            = new Rectangle(originalRect.x - deltaX, originalRect.y - deltaY,
                            (int) (originalRect.width * KOEFF), (int) (originalRect.height * KOEFF));
                    setBounds(newBounds);
                    Image scaledImage = originalImage.getScaledInstance(
                            (int) (originalWidth * KOEFF), -1, Image.SCALE_FAST);
                    setIcon(new ImageIcon(scaledImage));
                    repaint();
                }
            }
        }
    }

    public void mouseExited(MouseEvent e) {
        if (isEnabled() && animate) {
            if (originalImage != null) {
                setBounds(originalRect);
                setIcon(new ImageIcon(originalImage));
                repaint();
            }
            containsCursor = false;
            setCursor(Cursor.getDefaultCursor().getDefaultCursor());
        }
    }
}
