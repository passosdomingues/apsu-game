# Blender → jogo 2D com aparência 3D

Esta e' a rota para o visual de Donkey Kong Country: o jogo **nao carrega GLB
em tempo real**. Ele usa modelos 3D para renderizar PNGs, muito mais leves e
previsiveis em JavaFX/FXGL. O `.blend` e' a fonte editavel; os PNGs são o asset
de runtime.

## Personagem

1. Importe o GLB gerado pelo Hunyuan3D no Blender e salve como
   `assets/characters/adapa.blend` (este é o layout atual do projeto).
2. Corrija a malha, crie um Armature e Actions como `Idle`, `Swim` e `Attack`.
   Hunyuan gera malha estatica: rigging e' a etapa que adiciona poses reais.
3. Posicione uma camera ortografica chamada `SpriteCamera`, com luzes e fundo
   transparente.
4. Exporte: `make blender-sprites ASSET=adapa`. Para limitar o uso de CPU,
   use `BLENDER_THREADS=4 make blender-sprites ASSET=adapa`; o padrão usa todos
   os CPUs lógicos disponíveis. O perfil padrão é rápido (96 px, 1 amostra);
   para render final use `BLENDER_RESOLUTION=512 BLENDER_SAMPLES=64`.

O padrão exporta uma direção: `ApsuGame` espelha o PNG quando Adapa olha para a
esquerda. Para gerar oito vistas de um modelo já riggado, use
`BLENDER_DIRECTIONS=8 make blender-sprites ASSET=adapa`.

Para oito direções em paralelo, use
`make blender-sprites-mpi ASSET=adapa`. O padrão são quatro processos MPI com
dois threads cada — usa os oito CPUs lógicos sem executar oito Blenders de uma
vez. Isso é pré-processamento offline; o jogo continua leve em tempo real.

Isso gera `src/main/resources/sprites/adapa/<Action>/dN_fNNN.png`. A camada de
assets procura primeiro `sprites/adapa/Swim/d0_f001.png`, depois
`Idle`, `Static` e por fim `hero.png`. A pose estática já exportada passa a
aparecer no jogo imediatamente; Actions animadas a substituem quando existirem.
Essa convenção permite migrar um elemento por vez sem quebrar o jogo. O mesmo
vale para `kullullu` e `enki`.

## Cenários

Use o mesmo processo com um `.blend` por camada: fundo distante, ruínas médias,
corais de colisão e primeiro plano. Renderize cada camada em PNG com a mesma
câmera e mova-as em velocidades de parallax diferentes. Colisão nunca vem da
imagem: ela fica em dados simples de fase (retângulos/corais), por isso trocar
o modelo 3D não altera a jogabilidade.

Para o labirinto aquático, modele apenas a estética dos corais/ruínas no Blender;
os corredores jogáveis continuam definidos no código para serem claros,
testáveis e acessíveis.

As camadas prontas da fase podem ser publicadas diretamente em
`src/main/resources/levels/phaseN/background.png` e `foreground.png`.
`background.png` substitui somente o fundo legado; `foreground.png` é desenhado
na frente dos atores. Não é preciso converter todos os assets de uma vez.
