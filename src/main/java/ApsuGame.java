import br.apsu.ApsuGameMain;

/**
 * Classe principal de compatibilidade (Facade) apontando para a nova arquitetura modular `br.apsu.ApsuGameMain`.
 */
public class ApsuGame extends ApsuGameMain {
    public static void main(String[] args) {
        ApsuGameMain.main(args);
    }
}
