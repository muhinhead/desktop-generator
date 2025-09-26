package org.dbdesktop.guiutil;

import com.jtattoo.plaf.aero.AeroLookAndFeel;
import com.jtattoo.plaf.bernstein.BernsteinLookAndFeel;
import com.jtattoo.plaf.hifi.HiFiLookAndFeel;
import com.jtattoo.plaf.noire.NoireLookAndFeel;
import org.dbdesktop.orm.ExchangeFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

public class GeneralUtils {

    public static final String NMSOFTWARE = "Nick Mukhin (c)2017";
    public static final String ERROR = "Ошибка:";

    public static void setSizes(JFrame frame, double x, double y) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize((int) (x * d.width), (int) (y * d.height));
    }

    public static void centerOnScreen(JFrame frame) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(d.width / 2 - frame.getWidth() / 2, d.height / 2 - frame.getHeight() / 2);
    }

    public static float getXratio(JFrame frame) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        return (float) frame.getWidth() / d.width;
    }

    public static float getYratio(JFrame frame) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        return (float) frame.getHeight() / d.height;
    }

    public static byte[] readFile(String fileName) {
        byte[] b = null;
        try {
            File file = new File(fileName);
            FileInputStream fin = new FileInputStream(file);
            int n = 0;
            b = new byte[(int) file.length()];
            n = fin.read(b);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return b;
    }

    public static String getFileExtension(File file) {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".")+1);
        else return "";
    }

    public static void writeFile(File file, byte[] imageData) {
        try {
            FileOutputStream fout = new FileOutputStream(file);
            fout.write(imageData);
            fout.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static abstract class LookupAbstractAction extends AbstractAction {
        public LookupAbstractAction(Class cls) {
            super(null, new ImageIcon(loadImage("lookup.png", cls)));
        }
    }

    public static Image loadImage(String imageFile) {
        String fileName = "images/" + imageFile;
        Image image = null;
        if (new File(fileName).exists()) {
            try {
                ImageIcon ic = new javax.swing.ImageIcon(fileName, "");
                image = ic.getImage();
            } catch (Exception ex) {
                System.err.println("!!! loadImage 1: "+ex.getMessage());
            }
        } else {
            try {
                image = ImageIO.read(ToolBarButton.class.getResourceAsStream("/" + imageFile));
            } catch (Exception ie) {
                System.err.println("!!! loadImage 2: "+ie.getMessage());
            }
        }
        return image;
    }

    public static Image loadImage(String iconName, Class cl) {
        Image im = null;
        File f = new File("images/" + iconName);
        if (f.exists()) {
            try {
                ImageIcon ic = new javax.swing.ImageIcon("images/" + iconName, "");
                im = ic.getImage();
            } catch (Exception ex) {
                PropLogEngine.getInstance().log(ex);
            }
        } else {
            try {
                im = ImageIO.read(cl.getResourceAsStream("/" + iconName));
            } catch (Exception ie) {
                PropLogEngine.getInstance().log(ie);
            }
        }
        return im;
    }

    public static void setWindowIcon(Window w, String iconName) {
        w.setIconImage(loadImage(iconName, w.getClass()));
    }

    public static JMenu appearanceMenu(String item, final Component root) {
        JMenu m;
        JMenuItem it;
        m = new JMenu(item);
        it = m.add(new JMenuItem("Tiny"));
        it.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    setLookAndFeel("de.muntjak.tinylookandfeel.TinyLookAndFeel", root);
                } catch (Exception e1) {
                }
            }
        });
        it = m.add(new JMenuItem("Nimbus"));
        it.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel", root);
                } catch (Exception e1) {
                }
            }
        });
        it = m.add(new JMenuItem("Nimrod"));
        it.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    setLookAndFeel("com.nilo.plaf.nimrod.NimRODLookAndFeel", root);
                } catch (Exception e1) {
                }
            }
        });
        it = m.add(new JMenuItem("Plastic"));
        it.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    setLookAndFeel("com.jgoodies.plaf.plastic.PlasticXPLookAndFeel", root);
                } catch (Exception e1) {
                }
            }
        });
        it = m.add(new JMenuItem("HiFi"));
        it.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    HiFiLookAndFeel.setTheme("Default", "", NMSOFTWARE);
                    setLookAndFeel("com.jtattoo.plaf.hifi.HiFiLookAndFeel", root);
                } catch (Exception e1) {
                }
            }
        });
        it = m.add(new JMenuItem("Noire"));
        it.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    NoireLookAndFeel.setTheme("Default", "", NMSOFTWARE);
                    setLookAndFeel("com.jtattoo.plaf.noire.NoireLookAndFeel", root);
                } catch (Exception e1) {
                }
            }
        });
        it = m.add(new JMenuItem("Bernstein"));
        it.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    BernsteinLookAndFeel.setTheme("Default", "", NMSOFTWARE);
                    setLookAndFeel("com.jtattoo.plaf.bernstein.BernsteinLookAndFeel", root);
                } catch (Exception e1) {
                }
            }
        });
        it = m.add(new JMenuItem("Aero"));
        it.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    AeroLookAndFeel.setTheme("Green", "", NMSOFTWARE);
                    setLookAndFeel("com.jtattoo.plaf.aero.AeroLookAndFeel", root);
                } catch (Exception e1) {
                }
            }
        });

        it = m.add(new JMenuItem("System"));
        it.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    setLookAndFeel(UIManager.getSystemLookAndFeelClassName(), root);
                } catch (Exception e1) {
                }
            }
        });
        it = m.add(new JMenuItem("Java"));
        it.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel", root);
                } catch (Exception e1) {
                }
            }
        });
        it = m.add(new JMenuItem("Motif"));
        it.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel", root);
                } catch (Exception e1) {
                }
            }
        });
        return m;
    }

    public static void errMessageBox(String title, String msg) {
        JOptionPane.showMessageDialog(null, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void infoMessageBox(String title, String msg) {
        JOptionPane.showMessageDialog(null, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static int yesNo(String msg, String title) {
        int ok = JOptionPane.showConfirmDialog(null, title, msg,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return ok;
    }

    public static void notImplementedYet() {
        errMessageBox("Sorry!", "Not implemented yet");
    }

    private static void setLookAndFeel(String lf, Component root) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException,
            UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(lf);
//        FontUIResource font = new FontUIResource("Verdana", Font.PLAIN, 16);
//        UIManager.put("Table.font", font);
//        UIManager.put("Table.foreground", Color.AQUAMARINE);
        SwingUtilities.updateComponentTreeUI(root);
//        DashBoard.getProperties().setProperty("LookAndFeel", lf);
//        DashBoard.saveProps();
        ExchangeFactory.getPropLogEngine().getProps().setProperty("LookAndFeel", lf);
        ExchangeFactory.getPropLogEngine().saveProps();
    }

    public static void quit(int code) {
        ExchangeFactory.getPropLogEngine().saveProps();
        System.exit(code);
    }
}
