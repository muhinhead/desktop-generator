package org.dbdesktop.guiutil;

import java.util.ArrayList;

public final class Controller {
    private ArrayList<IView> views = new ArrayList<IView>();
    private Document document = null;

    public Controller() {
    }

    public Controller(Document document) {
        setDocument(document);
    }

    public Controller(Document document, IView view) {
        setDocument(document);
        addView(view);
    }

    public Controller(Document document, IView[] views) {
        setDocument(document);
        for (IView view : views) {
            addView(view);
        }
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
        updateExcept(null);
    }

    public void addView(IView view) {
        view.setController(this);
        getViews().add(view);
        view.update(document);
    }

    public void removeView(IView view) {
        view.setController(null);
        getViews().remove(view);
    }

    public ArrayList<IView> getViews() {
        return views;
    }

    public void updateExcept(IView except) {
        for (IView view : views) {
            if (view != except) {
                view.update(document);
            }
        }
    }
}
