package org.javaFX.controller.impl.handler;

import javafx.fxml.FXML;
import org.javaFX.EncryWindow;
import org.javaFX.controller.DataHandler;

public class OnboardingThreeHandler extends DataHandler {

    @FXML
    private void handleNextOnboardingAction(){
        getEncryWindow().launchWindowByPathToFXML(EncryWindow.pathToOnboardingFourFXML);
    }
}