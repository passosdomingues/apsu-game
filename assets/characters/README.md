# Personagens generativos

O jogo 2D continua usando `src/main/resources/hero.png`. Esta pasta e' a
pipeline nao destrutiva para produzir um modelo 3D que depois pode ser
renderizado em sprites pre-renderizados, no estilo 2.5D de Donkey Kong Country.

Troque a referencia a qualquer momento:

```bash
make character-reference CHARACTER=adapa FILE=/caminho/novo-personagem.png
make character-3d CHARACTER=adapa
```

`character-3d` envia a referencia para uma instancia local da API Hunyuan3D em
`http://127.0.0.1:8080`. O resultado e' salvo em
`assets/characters/adapa/generated/`. A assinatura SHA-256 da imagem e dos
parametros faz a operacao ser idempotente: executar de novo nao gasta inferencia
e apenas reusa o GLB existente. Para outra instancia use
`HUNYUAN3D_URL=http://host:porta make character-3d`.

Hunyuan3D gera uma malha estaticamente; ele nao cria um esqueleto/animacao de
nado a partir de uma unica imagem. A etapa seguinte correta e' rigging e poses
no Blender, seguida de renderizacao do sprite sheet. Isso e' separado para que
uma troca de imagem nunca sobrescreva ou corrompa o personagem anterior.
