package br.apsu;

import br.apsu.audio.AudioManager;
import br.apsu.audio.AudioEventSubscriber;
import br.apsu.core.GameContext;
import br.apsu.core.GameLoop;
import br.apsu.core.Viewport;
import br.apsu.core.events.EventBus;
import br.apsu.graphics.RenderEngine;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Ponto de entrada modular e desacoplado do jogo As Águas de Apsu.
 */
public class ApsuGameMain extends Application {

    public static final int W = 1366, H = 768;

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(W, H);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(canvas);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: black;");
        Scene scene = new Scene(root, W, H, Color.BLACK);
        Runnable fitCanvas = () -> {
            Viewport viewport = Viewport.fit(root.getWidth(), root.getHeight(), W, H);
            canvas.setScaleX(viewport.scale());
            canvas.setScaleY(viewport.scale());
        };
        root.widthProperty().addListener((obs, oldValue, newValue) -> fitCanvas.run());
        root.heightProperty().addListener((obs, oldValue, newValue) -> fitCanvas.run());

        EventBus eventBus = new EventBus();
        AudioEventSubscriber.bind(eventBus, AudioManager.getInstance());
        GameContext context = new GameContext(new br.apsu.core.SaveManager(), eventBus);
        RenderEngine renderer = new RenderEngine();
        GameLoop loop = new GameLoop(context, renderer, gc, W, H);

        scene.setOnKeyPressed(e -> {
            context.getInputManager().registerKeyPress(e.getCode());
            context.onKey(e.getCode());
        });

        scene.setOnKeyReleased(e -> context.getInputManager().registerKeyRelease(e.getCode()));

        stage.setTitle("As Águas de Apsu");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.show();
        fitCanvas.run();

        AudioManager.getInstance().playMusic();
        loop.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
