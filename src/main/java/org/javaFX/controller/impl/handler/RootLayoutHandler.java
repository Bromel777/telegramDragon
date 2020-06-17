package org.javaFX.controller.impl.handler;

import javafx.fxml.FXML;
import org.javaFX.EncryWindow;
import org.javaFX.controller.DataHandler;
import org.javaFX.util.observers.BasicObserver;

public class RootLayoutHandler extends DataHandler {

    private void terminateObserver(){
        BasicObserver observer = getObserver();
        if( observer != null){
            observer.cancel();
        }
    }

    @FXML
    private void createLocalCommunity(){
        terminateObserver();
        getEncryWindow().launchWindowByPathToFXML(EncryWindow.pathToCreateCommunityWindowFXML);
    }

    @FXML
    private void closeWindow(){
        System.exit(0);
    }

    @FXML
    private void launchMainWindow(){
        terminateObserver();
        getEncryWindow().launchWindowByPathToFXML(EncryWindow.pathToMainWindowFXML);
    }

    @FXML
    private void showLocalCommunity(){
        terminateObserver();
        getEncryWindow().launchWindowByPathToFXML(EncryWindow.pathToLocalShowCommunitiesWindowFXML);
    }

    @Override
    public String toString() {
        return "RootLayoutHandler{}"+getEncryWindow()+"\t"+getRootLayout()+"\t"+getStage()+"\t"+getUserStateRef();
    }
}
