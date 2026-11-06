# Decisões registradas

Decisões tomadas durante a montagem do harness. Cada uma está fechada. Para mudar, abra uma nova entrada com data e motivo.

| Data | Decisão | Motivo |
|---|---|---|
| 2026-11-05 | Manter personagens em malha 3D no runtime e otimizar as malhas (atlas, decimação, animação por troca de frames de malha) | Decisão já registrada no ARCHITECTURE.md e no README: modelos 3D em todos os perfis, sem trocar por sprites |
| 2026-11-05 | Projétil, flash de boca e impacto são modelados no Blender e entregues como sprite sheet desenhado em um Canvas acima da cena 3D | Muitos projéteis simultâneos tornam MeshView por bala caro, e a camada de efeitos precisa ficar na frente dos modelos |
| 2026-11-05 | Aura é uma textura radial pré-gerada na carga, desenhada no Canvas de fundo com pulso por escala e alfa | Custo constante, sem gradiente por frame, funciona em todos os perfis |
| 2026-11-05 | Simulação a 60 Hz fixos com interpolação no render | Preserva a calibração atual dos HeroType e remove o judder do acumulador |
| 2026-11-05 | Física migra para unidades por segundo com teste dourado contra a implementação atual (dt = 1/60) | Garante que o feel calibrado pela equipe não muda na migração |
| 2026-11-05 | Release de tecla é confirmado após 25 ms sem novo press | Neutraliza o par release/press do X11 com custo máximo de 1,5 tick na parada |
| 2026-11-05 | Velocidade planejada de 30 a 34 pontos por sprint | Não há histórico de pontos, o backlog existente estima em horas. Reavaliar na retro de 12/11 |
| 2026-11-05 | Referência de hardware: GPU integrada, 4 threads, 8 GB, 1366x768 | Corresponde ao perfil `balanced` do Makefile e ao pior caso esperado na demo de 04/12 |
| 2026-11-05 | Hooks e ferramentas do harness em Python com biblioteca padrão, executados por Makefile e Dockerfile próprios | Sem venv, conforme convenção do projeto |
