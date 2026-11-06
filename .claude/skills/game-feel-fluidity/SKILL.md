---
name: game-feel-fluidity
description: Padrões para movimento fluido em jogos de passo fixo: debounce de entrada, interpolação de render, física com dt, câmera amortecida, pixel snapping e colisão por eixo. Use ao tocar em InputManager, GameLoop, HeroEntity, Camera ou colisão.
---
# Fluidez de movimento

## Entrada (X11)
- Release só vale depois de 25 ms sem press da mesma tecla. `registerKeyPress` cancela o release pendente.
- Direções opostas se anulam: `axis = (right ? 1 : 0) - (left ? 1 : 0)`.
- Buffer de ação de 120 ms para tiro: guardar o instante do press e consumir quando a recarga acabar.

## Loop com interpolação
```java
accumulator += clamp(elapsed, 0, 250ms);
while (accumulator >= STEP && steps < MAX) { world.update(STEP_SECONDS); accumulator -= STEP; }
double alpha = accumulator / (double) STEP;
renderer.render(world, alpha);
```
- Toda entidade móvel guarda `prevX, prevY` copiados no começo de cada update.
- Desenho: `x = prevX + (x - prevX) * alpha`.
- Em teletransporte, chame `snapPrevious()`.

## Física com dt
- Unidades em px/s e px/s2. Atrito por tick vira `pow(fator, dt * 60)`.
- Semi-implícito: atualize a velocidade, depois a posição com a velocidade nova.
- Mantenha um teste dourado com `dt = 1/60` contra a versão anterior antes de migrar.

## Câmera
- Amortecimento crítico: `cam += (alvo - cam) * (1 - exp(-k * dt))`, `k` entre 8 e 12.
- Interpole a câmera como as entidades. Arredonde só na hora de desenhar.
- Parallax: `Math.round(cam * fator)` por camada. Nunca desenhe imagem em posição fracionária.

## Colisão
- Resolva pelo menor eixo de penetração e zere só a componente de velocidade que entra no obstáculo.
- Sem folga fixa em pixels: use a margem do próprio hitbox.
- Deslizar na parede é esperado.

## Medição
- `perf_gate` falha se `sim/frame` variar mais de 0,35 em 60 s.
- Teste unitário com relógio simulado e pulsos de 16,2 ms e 17,1 ms alternados.
