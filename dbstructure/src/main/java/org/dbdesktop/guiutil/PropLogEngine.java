package org.dbdesktop.guiutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class PropLogEngine {

    public static class NoOwnerException extends Exception {

        public NoOwnerException(String msg) {
            super(msg);
        }
    }

    public static PropLogEngine instance = null;
    private static Class owner = null;
    private Logger logger = null;
    private Properties props = null;
    private static final String homeDir = System.getProperty("user.home") + File.separator;

    private static String getPropertyFileName() {
        String propertyFileName = getOwner().getName() + ".config";
        return (new File(propertyFileName).exists() ? propertyFileName : homeDir + propertyFileName);
    }

    private PropLogEngine(Level logLevel) {
        instance = this;
        if (logger == null) {
            try {
                logger = Logger.getLogger(getOwner().getName());
                FileHandler fh = new FileHandler("%h/" + getOwner().getName() + ".log", 1048576, 10, true);
                logger.addHandler(fh);
                logger.setLevel(logLevel);
                SimpleFormatter formatter = new SimpleFormatter();
                fh.setFormatter(formatter);
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static PropLogEngine getInstance() {
        return instance;
    }

    public static PropLogEngine getInstance(Level logLevel) throws NoOwnerException {
        if (null == owner) {
            throw (new NoOwnerException("Unknown class-owner"));
        }
        if (null == instance) {
            instance = new PropLogEngine(logLevel);
        }
        return instance;
    }

    public void log(Level lvl, String msg, Throwable th) {
        logger.log(lvl, msg, th);
    }

    public void log(String msg, Throwable th) {
        logger.log(logger.getLevel(), msg, th);
    }

    public void log(String msg) {
        logger.log(logger.getLevel(), msg, (Throwable) null);
    }

    public void log(Throwable th) {
        logger.log(logger.getLevel(), null, th);
    }

    public void logAndShowMessage(Throwable ne) {
        GeneralUtils.errMessageBox(GeneralUtils.ERROR, ne.getMessage());
        log(ne);
    }

    public String readProperty(String key, String deflt) {
        if (null == getProps()) {
            props = new Properties();
        }
        try {
            File propFile = new File(getPropertyFileName());
            if (!propFile.exists() || propFile.length() == 0) {
//                props.setProperty("user", "admin");
//                props.setProperty("userPassword", "admin");
                propFile.createNewFile();
            } else {
                getProps().load(new FileInputStream(propFile));
            }
        } catch (IOException e) {
            log(e);
            return deflt;
        }

        return getProps().getProperty(key, deflt);
    }

    /**
     * @return the owner
     */
    public static Class getOwner() {
        return owner;
    }

    /**
     * @param aOwner the owner to set
     */
    public static void setOwner(Class aOwner) {
        owner = aOwner;
    }

    /**
     * @return the props
     */
    public Properties getProps() {
        return props;
    }

    private void saveProperties() {
        try {
            if (props != null) {
                props.store(new FileOutputStream(getPropertyFileName()),
                        "-----------------------");
            }
        } catch (IOException e) {
            logAndShowMessage(e);
        }
    }

    public void saveProps() {

        saveProperties();
    }
}