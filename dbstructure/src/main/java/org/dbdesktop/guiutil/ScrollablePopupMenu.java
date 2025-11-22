package org.dbdesktop.guiutil;

import javax.swing.*;
import java.awt.*;

public class ScrollablePopupMenu extends JPopupMenu {

    private final JPanel container = new JPanel();
    private final JScrollPane scroll;

    public ScrollablePopupMenu(int maxHeight) {
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(200, maxHeight));

        add(scroll);
    }

    @Override
    public JMenuItem add(JMenuItem item) {
        container.add(item);
        return item;
    }
}