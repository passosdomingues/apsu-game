import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

/**
 * Porta de entrada FXGL para a v2 do Apsu.
 *
 * O Canvas atual continua sendo o jogo de producao. Esta cena prova a
 * integracao da engine e define o ponto de migracao seguro: cada elemento novo
 * (decoracao, inimigo, gatilho, camera) pode virar uma Entity FXGL sem forcar
 * a reescrita de todas as fases de uma vez.
 */
public final class ApsuFXGLGame extends GameApplication {

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1366);
        settings.setHeight(768);
        settings.setTitle("As Águas de Apsu — Laboratório FXGL");
        settings.setVersion("0.3.0");
        settings.setMainMenuEnabled(true);
        settings.setGameMenuEnabled(true);
    }

    @Override
    protected void initGame() {
        // Primeiro Entity real: marcador de spawn. Troque a view por um PNG
        // pré-renderizado do Blender quando ele estiver em resources/sprites/.
        entityBuilder()
                .at(92, 344)
                .view(new Rectangle(56, 80, Color.web("#34a068")))
                .buildAndAttach();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
