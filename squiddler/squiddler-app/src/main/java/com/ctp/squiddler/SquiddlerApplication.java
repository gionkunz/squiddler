package com.ctp.squiddler;

import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SquiddlerApplication extends Application {
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		
		URL config = getClass().getResource("main-view.fxml");
		 primaryStage.setScene
	       ((Scene)FXMLLoader.load(config));
		primaryStage.setTitle("Squiddler");
		primaryStage.show();
	}
}
