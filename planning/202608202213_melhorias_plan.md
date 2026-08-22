# 🎨 Plano de Implementação — Arte de Cenário 3D/2.5D, Hitboxes Precisas & Zero Lag

Este plano aborda diretamente o feedback sobre os elementos de cenário (substituindo todas as formas genéricas/improvisadas por assets 3D renderizados do Blender), o ajuste fino de hitboxes e a eliminação completa de travamentos.

---

## 🎯 Objetivos

1. **Substituição dos Elementos de Cenário por Assets 3D Ricos**
   - **Corais (Fase 2)**: Substituir os retângulos procedurais escuros por sprites animados 2.5D da suíte Blender (`sprites/scenery/10_perigos_e_obstaculos_cenario` e `08_recifes_e_cardume_elemento-cenario`), com rotação orgânica de iluminação e profundidade real.
   - **Obstáculos Móveis & Cristais (Fase 3)**: Integrar a sequência animada 2.5D do `sprites/scenery/13_obstaculo_abissal` com rotação de luz bioluminescente e rastro de água, eliminando retângulos e imagens estáticas.
   - **Rochas Vulcânicas (Fase 4)**: Integrar a sequência 2.5D do `sprites/scenery/12_obstaculo_vulcanico` com fissuras incandescentes pulsantes.
   - **Ruínas e Colunas de Atlantis (Fase 5 e Fundos)**: Renderizar as colunas 3D (`sprites/scenery/09_ruinas_e_colunas_atlantis`) integradas com algas procedurais e iluminação de profundidade.

2. **Ajuste Fino de Hitboxes (Precisão Absoluta)**
   - **Corais e Colunas**: Hitbox restrita ao tronco sólido central da arte (descontando pontas finas e transparências), permitindo esquivas tangentes perfeitas.
   - **Rochas Vulcânicas e Obstáculos Móveis**: Hitbox reduzida para 70% da área visível do asset 3D, garantindo que colisão ocorra apenas no núcleo rígido.
   - **Piscinas de Lava**: Dano registrado apenas se a base/pés do herói tocarem a lava (área de contato reduzida na Y inferior).

3. **Otimização de Lag e Câmera (60 FPS Cravados)**
   - Garantir *culling* estrito em todos os elementos de cenário, partículas e luzes (não processar nem renderizar nada a ±100px fora da tela).
   - Cachear sequências de animação dos cenários via `SpriteManager` para evitar qualquer alocação de memória no loop principal.

---

## 🛠️ Alterações Propostas

### Componente: Gráficos e Renderização (`RenderEngine.java` & `LightingEngine.java`)

#### [MODIFY] [RenderEngine.java](file:///home/rafael/github/cache/grafica/Game/src/main/java/br/apsu/graphics/RenderEngine.java)
- Substituir o desenho de caixa em `drawOrganicCoral` pelo sprite 2.5D animado de corais/perigos do Blender.
- Substituir a renderização estática de `MOVING_OBSTACLE` e `VOLCANIC_ROCK` por animações 2.5D com brilho bioluminescente embutido.
- Melhorar o posicionamento e a profundidade de ruínas e galeões.

#### [MODIFY] [GameContext.java](file:///home/rafael/github/cache/grafica/Game/src/main/java/br/apsu/core/GameContext.java)
- Recalibrar a checagem de colisão em `checkSceneryCollisions()` para cada tipo de elemento (`CORAL`, `VOLCANIC_ROCK`, `MOVING_OBSTACLE`, `LAVA_POOL`).
- Ajustar os limites de colisão para casar com as proporções exatas dos novos sprites 3D.

---

## 🧪 Plano de Verificação

### Automated Tests
- Executar `mvn test` para garantir que a suíte de 37 testes continue passando sem regressões.

### Manual Verification & Performance
- Compilar e executar o jogo via Docker com `make all`.
- Testar a navegação do herói entre os corais na Fase 2, obstáculos na Fase 3 e rochas na Fase 4, confirmando a precisão do hitbox e a ausência de lag.
