# Sprint 19: Fechamentos, Lapidações e Experiência do Usuário (26/11 a 02/12)

Capacidade: 34 pontos. Planejado: 34.

Meta: eliminar o bug de dano do boss, substituir retângulos da ecolocalização por ondas esféricas orgânicas, adicionar menu de pausa e continuação de fase, introduzir a 5ª tabuleta no templo de Kullullû, barra de recarga do sonar, animações e documentação simplificada com destaque ao instalador.

### APSU-B19A | Correção e Calibração de Dano do Boss Kullullû e Inimigos | 5 pts | Rafa
Como jogador, quero que o boss e os inimigos causem dano real e tenham resistência condizente, para que o combate final seja desafiador e emocionante.

Complexidade 5: envolve desacoplamento de proteção de tutorial, ajuste de hitboxes de projéteis 3D/2D e rebalanceamento de HP e cadência.

Critérios de aceite:
- AC1: O Boss Kullullû causa dano real por contato e por projéteis de lava em todas as dificuldades, incluindo o modo Fácil.
- AC2: A proteção de vida mínima (50% HP) de `TutorialController` fica estritamente restrita à Fase 1, sendo desativada nas Fases 2 a 5.
- AC3: Hitbox das bolas de lava do boss ampliada para 28x28 px (compatível com a escala visual 54) com dano entre 1.0 e 1.5.
- AC4: Vida máxima de Kullullû aumentada (24 a 30 HP) e cadência de tiro do herói calibrada para evitar morte instantânea do chefe.
- AC5: Inimigos comuns recebem aumento balanceado de HP para valorizar o poder das bolhas purificadoras.

Tarefas:
- [x] B19A.1 Restringir filtro de dano do tutorial exclusivamente à Fase 1 (1 h)
- [x] B19A.2 Ampliar caixa de colisão dos projéteis `BOSS_BULLET` e calibrar dano (1 h)
- [x] B19A.3 Rebalancear HP de Kullullû e inimigos no `BossEntity` e `EnemyType` (1,5 h)
- [x] B19A.4 Calibrar cadência de disparo de bolha única e rajada tripla no `GameContext` (1,5 h)

### APSU-G19B | Ondas Esféricas Orgânicas na Ecolocalização dos Cenários | 5 pts | Marcos
Como jogador, quero que a ecolocalização revele obstáculos do cenário como ondas acústicas esféricas e orgânicas, para evitar bordas retangulares artificiais na água.

Complexidade 5: substituição de renderização geométrica por anéis concêntricos orgânicos com atenuação radial em zero-alloc.

Critérios de aceite:
- AC1: Remoção completa de `strokeRoundRect` no destaque de cenário em `EchoWaveRenderer`.
- AC2: Os obstáculos atingidos pelo sonar emitem ondas esféricas concêntricas (`strokeOval`) que emergem do centro do elemento.
- AC3: O pulso esférico atenua suavemente em opacidade e escala durante a duração do destaque (1,8 s).
- AC4: Culling de viewport mantido para não desenhar ondas fora da tela.
- AC5: Zero alocações de objetos por frame no loop de renderização do eco.

Tarefas:
- [x] G19B.1 Substituir desenho de caixas retangulares por círculos concêntricos em `EchoWaveRenderer` (2 h)
- [x] G19B.2 Parametrizar raio e pulso acústico pelo centro e dimensões do obstáculo (1,5 h)
- [x] G19B.3 Validar renderização em todas as 4 fases com cenário (1,5 h)

### APSU-U19C | Barra Visual de Recarga da Ecolocalização e Cooldown no HUD | 3 pts | Rafa
Como jogador, quero uma barra visual indicando a recarga da ecolocalização, para saber com exatidão quando o sonar estará pronto para uso.

Complexidade 3: adição de elemento gráfico responsivo na HUD superior com gradiente e transição de estado.

Critérios de aceite:
- AC1: O HUD exibe uma barra indicativa de recarga para a Ecolocalização com fundo escuro e preenchimento gradual.
- AC2: Enquanto em recarga, a barra exibe o preenchimento proporcional (0% a 100%) em azul-ciano e o tempo restante.
- AC3: Ao atingir 100%, a barra pulsa em tom dourado/ciano indicando estado "PRONTO".
- AC4: O ícone [Q] no teclado virtual e na HUD sincroniza com a disponibilidade da habilidade.
- AC5: Renderização 100% zero-alloc reutilizando cores cacheadas da `Palette`.

Tarefas:
- [x] U19C.1 Criar componente visual da barra de cooldown em `UIRenderer` (2 h)
- [x] U19C.2 Integrar leitura contínua de `EchoController.getCooldownProgress()` (1,5 h)
- [x] U19C.3 Adicionar feedback luminoso de habilidade pronta (1,5 h)

### APSU-M19D | Menu de Pausa, Reinício Instantâneo sem Travamento e Teclas Rápidas | 5 pts | Marcos
Como jogador, quero pausar o jogo e reiniciar fases rapidamente sem travamentos, para ter controle total sobre a sessão de gameplay.

Complexidade 5: introdução do estado `State.PAUSE` com controle de loop, overlay de pausa e reinicialização limpa.

Critérios de aceite:
- AC1: Pressionar `ESC` ou `P` durante as fases P1 a P5 alterna o jogo para o estado `State.PAUSE`.
- AC2: O menu de pausa oferece opções claras: "Continuar", "Reiniciar Fase" e "Menu Principal".
- AC3: A tecla `R` durante o jogo ou na tela de Game Over/Vitória reinicia instantaneamente a fase sem congelamento.
- AC4: A purga e reinicialização de nós da cena 3D e contadores de entidades ocorre sem falhas de estado.
- AC5: Ao despausar, o jogo retoma suavemente sem saltos temporais de delta-time.

Tarefas:
- [x] M19D.1 Adicionar `State.PAUSE` no `GameContext` e travar avanço de simulação quando pausado (1,5 h)
- [x] M19D.2 Implementar overlay gráfico glassmorphic de pausa no `RenderEngine` (2 h)
- [x] M19D.3 Implementar reinício imediato via tecla `R` em `GameContext` e telas finais (1,5 h)

### APSU-M19E | Sistema de Checkpoint e Opção "Continuar Fase" no Menu Principal | 5 pts | Rafa
Como jogador, quero continuar minha campanha a partir da fase alcançada, para não perder o progresso ao retornar ao menu.

Complexidade 5: integração do `SaveManager` ao menu principal com carregamento dinâmico de inventário e estado.

Critérios de aceite:
- AC1: Se o jogador já desbloqueou fases além da Fase 1 (`phaseDone > 0`), o menu principal exibe a opção "CONTINUAR [FASE X]".
- AC2: Ao selecionar "Continuar", o jogo carrega diretamente na fase correspondente com as tabuletas e poderes conquistados.
- AC3: A opção "NOVO JOGO" permanece disponível para iniciar a campanha do começo quando desejado.
- AC4: O arquivo de save é atualizado automaticamente a cada conclusão de fase.
- AC5: Falhas de leitura de save iniciam novo jogo sem lançar exceções.

Tarefas:
- [x] M19E.1 Adicionar suporte a opções dinâmicas de menu no `GameContext` e `RenderEngine` (2 h)
- [x] M19E.2 Carregar inventário e poderes condizentes com a fase continuada (1,5 h)
- [x] M19E.3 Testar fluxo de save e continue entre todas as fases (1,5 h)

### APSU-G19F | A Quinta Tabuleta Sagrada Após a Derrota de Kullullû | 8 pts | Wagner
Como jogador, quero que a 5ª Tabuleta Sagrada seja revelada após derrotar Kullullû, para completar a profecia de Apsu com todas as 5 tabuletas.

Complexidade 8: sincronização de evento de morte do chefe, instanciação da relíquia no altar e transição triunfal para vitória.

Critérios de aceite:
- AC1: Ao zerar a vida de Kullullû na Fase 5, a 5ª Tabuleta Sagrada emerge flutuando no centro do Templo de Apsu.
- AC2: A relíquia utiliza o modelo especial `tabuleta_sagrada_2_3d` com irradiação dourada celestial.
- AC3: Mensagem de alerta épica orienta o herói a aproximar-se e coletar a relíquia sagrada.
- AC4: Ao tocar a 5ª tabuleta, o contador atinge 5/5 tabuletas, toca a fanfarra de vitória e aciona `State.WIN`.
- AC5: A tela de vitória celebra a purificação com todas as 5 tabuletas coletadas.

Tarefas:
- [x] G19F.1 Configurar criação da 5ª Tabuleta Sagrada no evento de morte do chefe em `GameContext` (2 h)
- [x] G19F.2 Renderizar a Tabuleta Especial 3D no centro da arena em `Runtime3DLayer` (1,5 h)
- [x] G19F.3 Vincular coleta da 5ª tabuleta ao acionamento da tela de vitória (1,5 h)

### APSU-D19G | Documentação, Requisitos de Hardware, Instruções no README e Higienização Git | 3 pts | Erik
Como avaliador e usuário, quero instruções simples de clonagem e links diretos, para rodar o jogo com rapidez e facilidade.

Complexidade 3: documentação técnica e de usuário, badges, guia rápido e configuração de isolamento de ferramentas.

Critérios de aceite:
- AC1: README.md atualizado com seção destacada "Como Rodar Rapidamente" com comandos prontos de `git clone` e `make`.
- AC2: Requisitos de hardware mínimos e recomendados documentados de forma clara e acessível.
- AC3: Destaque preferencial para o download do instalador Windows `.exe` e `.deb` Linux.
- AC4: Diretório `.claude/` e artefatos de assistentes incluídos no `.gitignore`.
- AC5: Guia de compilação multiplataforma validado.

Tarefas:
- [x] D19G.1 Atualizar `README.md` com comandos rápidos, requisitos e links destacados (1,5 h)
- [x] D19G.2 Configurar `.gitignore` para omitir `.claude` e metadados locais (0,5 h)
- [x] D19G.3 Revisar e alinhar notas de lançamento (1 h)
- [x] D19G.4 Testar instruções em ambiente limpo (1 h)
