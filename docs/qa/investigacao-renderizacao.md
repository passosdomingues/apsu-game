# Investigação: personagens que não aparecem e lentidão

Método: leitura do código do repositório (commit 5832603), experimentos mínimos com JavaFX e jogo real rodando em display virtual (Xvfb, Mesa llvmpipe), com o jogador automático `tools/harness/play/ai_play.py`. Ambiente de teste: JavaFX 11 do Ubuntu com JDK 21, não o JavaFX 21.0.3 do projeto. As conclusões sobre pipelines valem para ambos, mas confirme no seu Windows (seção final).

## 1. Personagens ausentes no Windows

Fatos medidos:

| Pipeline Prism | SCENE3D | Caixa 3D de teste | Retângulo 2D de controle |
|---|---|---|---|
| es2 (OpenGL, Mesa em software) | verdadeiro | 52.900 pixels | desenhado |
| sw (software do JavaFX) | falso | 0 pixels | 900 pixels |

No jogo, o pipeline sw reproduz o print do Windows: HUD, aviso "E / ESPAÇO: CONVERSAR" e sombras aparecem, mas herói, guardião e inimigos não. Durante o teste a vida caiu de 4,0 para 3,4, ou seja, inimigos invisíveis causam dano. O JavaFX registrou 1.240 avisos de SCENE3D em cerca de 60 s.

Explicação: `Runtime3DLayer` desenha todos os personagens como malhas OBJ numa SubScene 3D, e cenário, HUD e partículas em Canvas. No Windows o JavaFX tenta d3d e depois sw. Quando o Direct3D não inicia (driver genérico, máquina virtual, área de trabalho remota), cai no sw, que não suporta 3D e descarta a SubScene sem erro. O código não verifica `Platform.isSupported(SCENE3D)`.

Capturas: `investigacao-renderizacao/sw_sem_personagens.jpg` (sw) e `es2_mesa_jogando.jpg` (es2).

## 2. Lentidão em notebooks sem GPU dedicada

Medido no ambiente de teste (Mesa llvmpipe, perfil low), fase 1:

- es2 em software: cerca de 9 a 10 fps, pulse p50 de 96 ms.
- update do jogo: 0,02 ms. Render do Canvas: 0,2 a 1 ms. GC sem relevância.
- Carga dos modelos OBJ: assíncrona, 300 a 700 ms uma única vez, fora da thread de desenho.
- sw (sem 3D): cerca de 33 fps no mesmo trecho.

Conclusão: o custo está na rasterização da SubScene 3D em software, não na lógica do jogo, não no carregamento de modelos e não no cache. Isso confirma o diagnóstico do time. O cache do jogo (`SaveManager`) guarda só o estado da partida.

Nota: a checagem de GPU do JavaFX recusa o es2 em Mesa llvmpipe sem `-Dprism.forceGPU=true`. Em notebooks reais com Intel integrada o es2 costuma ser aceito, e a lentidão que vocês veem é a do 3D em GPU fraca ou em Mesa de software.

## 3. Estalactites e estalagmites invisíveis na fase 2

`GameContext` cria seis pares de `SceneryElement.Type.CORAL` (topo e fundo) e aplica colisão e dano em `updateP2`. `Runtime3DLayer.sceneryModel(CORAL)` devolve `null` de propósito, com um comentário explicando que o único OBJ era um recife completo, e é o único código que lê os elementos de cenário. Logo, os corais colidem e machucam, mas nunca são desenhados, em qualquer pipeline. O README marca "corais com colisão" como IMPLEMENTADO, o que é só a colisão.

## 4. O que fazer, em ordem

1. US-001: desenhar os corais em Canvas a partir da caixa de colisão (resolve em 3D e em 2D).
2. US-007: registrar pipeline e SCENE3D na partida e avisar o jogador.
3. US-008: desenhar personagens com sprites quando o 3D falhar.
4. US-009: modo híbrido e teste de reduzir a resolução da SubScene 3D no perfil low.

## 5. Como reproduzir

```
python3 tools/harness/play/ai_play.py --label es2 --prism es2
python3 tools/harness/play/ai_play.py --label sw --prism sw --assert-3d
```

No seu Windows, confirme com `-Dprism.verbose=true`: procure `D3DPipeline` ou `SWPipeline` e o aviso de SCENE3D.

## 6. Limites desta investigação

- JavaFX 11 no lugar do 21.0.3, e Mesa em software no lugar de uma GPU real.
- Não testei a fase 2 jogando (a conclusão vem do código); `ai_play.py` ainda cobre menu e fase 1.
- Não há áudio no ambiente de teste.
