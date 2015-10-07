package com.josephcatrambone.sharpcloud;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.Random;
import java.util.logging.Logger;

public class MainWindow extends Application {
	private static final Logger LOGGER = Logger.getLogger(MainWindow.class.getName());
	public final int WIDTH = 800;
	public final int HEIGHT = 600;

	@Override
	public void start(Stage stage) {
		buildUI(stage);
	}

	public void buildUI(Stage stage) {

		// Learning data and consts
		Random random = new Random();

		// Load or build RBM for learning
		Pane pane = new Pane();

		Scene scene = new Scene(pane, WIDTH, HEIGHT);
		stage.setScene(scene);
		stage.show();
	}
}