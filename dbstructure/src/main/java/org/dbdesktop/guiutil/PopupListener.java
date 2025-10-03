package org.dbdesktop.guiutil;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PopupListener extends MouseAdapter
{
    private JPopupMenu pop;

    public PopupListener(JPopupMenu pop)
    {
        super();
        this.pop = pop;
    }

    public void mousePressed(MouseEvent e)
    {
        showPopup(e);
    }

    public void mouseReleased(MouseEvent e)
    {
        showPopup(e);
    }

    private void showPopup(MouseEvent e)
    {
        if (e.isPopupTrigger()) {
            pop.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}