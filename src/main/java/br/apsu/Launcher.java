package br.apsu;

/**
 * Launcher wrapper sem herdar de javafx.application.Application.
 * Permite que o fat JAR seja executado via 'java -jar' sem exigir --module-path.
 */
public class Launcher {
    public static void main(String[] args) {
        ApsuGameMain.main(args);
    }
}
