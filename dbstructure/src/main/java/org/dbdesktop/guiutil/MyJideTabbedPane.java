package org.dbdesktop.guiutil;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class MyJideTabbedPane extends JTabbedPane {

    public MyJideTabbedPane() {
        super(JTabbedPane.LEFT);
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int count = MyJideTabbedPane.this.getTabCount();
                int selected = MyJideTabbedPane.this.getSelectedIndex();

                if (e.getWheelRotation() > 0) { // scroll down
                    if (selected < count - 1) {
                        MyJideTabbedPane.this.setSelectedIndex(selected + 1);
                    }
                } else if (e.getWheelRotation() < 0) { // scroll up
                    if (selected > 0) {
                        MyJideTabbedPane.this.setSelectedIndex(selected - 1);
                    }
                }
            }
        });
//        super(JideTabbedPane.TOP);
//        setShowTabButtons(true);
//        setBoldActiveTab(true);
//        setColorTheme(JideTabbedPane.COLOR_THEME_OFFICE2003);
//        setTabShape(JideTabbedPane.SHAPE_BOX);
    }

    public void addTab(JComponent comp, String title) {//, Icon icon) {
//        if (icon == null) {
//            super.add(comp, title);
//        } else {
//            super.addTab(title, icon, comp);
//        }
        super.add(title, comp);
    }

//    public void addTab(JComponent comp, String title) {
//        addTab(comp, title, new ImageIcon(XlendWorks.loadImage("xlendfolder.jpg", DashBoard.ourInstance)));
//    }
}