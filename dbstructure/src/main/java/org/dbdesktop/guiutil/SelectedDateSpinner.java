package org.dbdesktop.guiutil;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class SelectedDateSpinner extends JSpinner {
    public SelectedDateSpinner() {
        super(new SpinnerDateModel());
        ((JSpinner.DefaultEditor) getEditor()).getTextField().addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(final FocusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        ((JTextComponent) e.getSource()).selectAll();
                    }
                });
            }
        });
    }

    public static void addFocusSelectAllAction(JSpinner spinner) {
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(final FocusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        ((JTextComponent) e.getSource()).selectAll();
                    }
                });
            }
        });
    }
}
