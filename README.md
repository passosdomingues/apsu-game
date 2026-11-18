# As Aguas de Apsu

Jogo de acao e aventura subaquatica 2.5D retro arcade desenvolvido em Java 21 e JavaFX.

Inspirado na mitologia sumeria e nos classicos dos fliperamas dos anos 90, o jogador controla o sabio Adapa pelas profundezas do oceano primordial. Enfrente perigos abissais, recupere tabuletas sagradas, desperte o poder das bolhas e confronte o temivel monstro marinho Kullullu no Templo de Apsu.

## Fases e Progressao

O jogo e dividido em cinco regioes progressivas de profundidade:

1. **Aguas Claras**: Introducao a mecanica de nado e esquiva, recifes costeiros e o primeiro contato com o Guardiao Atlante.
2. **Cavernas de Coral**: Navegacao ate o galeao naufragado, onde repousa o bau ancestral que desperta o poder de disparo de bolhas.
3. **Correntes Abissais**: Correntes submarinas e zonas de pressao que alteram a flutuabilidade e exigem leitura tatica do ambiente.
4. **Abismo Vulcanico**: Chamines hidrotermais, piscinas de lava ardente e o confronto com o sub-chefe Leviata Menor.
5. **Templo de Apsu**: Arena final nas fossas abissais para o duelo definitivo contra o lorde das profundezas, Kullullu.

## Controles

- **Mover / Nadar**: W, A, S, D ou Teclas de Direcao
- **Disparo de Bolhas / Interagir**: Barra de Espaco ou Tecla E (apos obter o poder na Fase 2)
- **Eco de Ecolocalizacao**: Q ou Shift Esquerdo (revela perigos e segredos no escuro)
- **Menu / Pausa**: ESC

## Requisitos do Sistema

- **Java Runtime**: OpenJDK 21 LTS ou superior
- **Build Tool**: Apache Maven 3.9+ (ou GNU Make para automacao)
- **Plataformas**: Linux (x86_64), macOS (Intel e Apple Silicon), Windows 10/11 (x64)

## Como Executar

### Via Maven (Desenvolvimento)

```bash
# Compilar e executar diretamente
mvn javafx:run
```

### Via Makefile

```bash
# Executa verificacao de ambiente e inicia o jogo
make run

# Executa suite completa de testes automatizados
make test
```

### Instaladores Prontos

Pacotes binarios compilados com instaladores nativos (.deb para Linux, .dmg para macOS e .exe para Windows) estao disponiveis para download direto na aba de [Releases no GitHub](https://github.com/passosdomingues/apsu-game/releases).

## Arquitetura e Engenharia

- **Engine Grafica**: Camada hibrida com renderizacao 2D no Canvas combinada a modelos 3D em tempo real via JavaFX SubScene e carregador customizado de malhas OBJ/MTL (`ObjModelLoader`).
- **Desempenho Zero-Allocation**: Loop de jogo desacoplado a 60 FPS, com mitigacao de pausas do Garbage Collector nas rotas criticas de combate e particulas.
- **Ecolocalizacao e Iluminacao**: Sistema de ondas de pulso acustico e bioluminescencia dinamica para iluminar o oceano profundo.
- **Documentacao Tecnica Detalhada**: Detalhes completos da arquitetura, pipeline 3D e notas tecnicas estao disponiveis no diretorio `docs/`.

## Contribuicoes da Equipe

- **Erik Alexandre**: Trilha Sonora e Composicao Musical
- **Wagner**: Modelos 3D dos Personagens
- **Marcos**: Modelos 3D e Arte dos Cenarios
- **Rafael**: Mecanica, Fisica e Engine em Geral

## Licenca

Este projeto e distribuido sob a licenca Apache 2.0. Consulte o arquivo [LICENSE](LICENSE) para mais detalhes.
