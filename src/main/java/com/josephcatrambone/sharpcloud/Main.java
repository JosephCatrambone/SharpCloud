package com.josephcatrambone.sharpcloud;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.awt.*;
import java.io.*;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jblas.DoubleMatrix;

public class Main extends Application {
	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
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

		DoubleMatrix m = ImageTools.ImageFileToMatrix("test.png", -1, -1);
		DoubleMatrix edges = ImageTools.edgeDetector(m);
		ImageTools.FXImageToDisk(ImageTools.MatrixToFXImage(edges), "output.png");

		stage.close();
	}


	public static void main(String[] args) {
		launch(args);
	}


}
