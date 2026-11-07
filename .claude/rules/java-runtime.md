---
paths:
  - "src/main/java/**"
---
# Regras para o código Java do runtime

- Estas regras complementam o AGENTS.md; em conflito, vale o AGENTS.md.
- Lógica nova vai em classe nova de pacote próprio. Em GameContext, RenderEngine e GameLoop, apenas a integração mínima (regras 10 e 11).
- Nada no caminho por quadro aloca objetos nem faz E/S. Reutilize buffers e instâncias.
- Eventos de gameplay passam pelo EventBus com tipo declarado, produtor e consumidor reais.
- Sem MPI no loop de frame (regra 12).
- Texto de interface somente em tema claro; sem dark mode.
- Um teste JUnit por comportamento novo, sem depender de display gráfico.
