package org.dbdesktop.guiutil;

public interface IView {
    public void setController(Controller controller);
    public Controller getController();
    public void update(Document document); //initialize visible content by document
    public void synchronize(); //update document through controller:
    // {
    //     controller.getDocument().setBody(<this.body>);
    //     controller.updateExcept(this);
    // }
}