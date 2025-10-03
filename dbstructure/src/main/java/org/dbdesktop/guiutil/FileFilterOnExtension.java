package org.dbdesktop.guiutil;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class FileFilterOnExtension extends FileFilter {

    String extension;

    public FileFilterOnExtension(String extension) {
        super();
        this.extension = extension;
    }

    @Override
    public boolean accept(File f) {
        boolean ok = f.isDirectory()
                || f.getName().toLowerCase().endsWith(extension);
        return ok;
    }

    @Override
    public String getDescription() {
        return "*." + extension;
    }
}