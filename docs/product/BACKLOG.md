# Backlog de produto: As Águas de Apsu

Fonte de verdade das user stories. Validado por `python3 tools/harness/backlog.py lint` (ou `make backlog-lint`).

## Como ler

- Escala de pontos: Fibonacci (1, 2, 3, 5, 8, 13). Acima de 13 a story deve ser dividida.
- Prioridade: P0 bloqueia a jornada do jogador, P1 alto valor, P2 desejável, P3 opcional.
- Status: `todo`, `doing`, `done`. Uma story só vai para `done` com todos os critérios marcados `[x]` e a Definição de Pronto cumprida (`DEFINITION_OF_DONE.md`).
- Critérios de aceitação: sempre no formato "Dado ..., quando ..., então ...", mensuráveis e verificáveis por teste, medição ou playtest descrito.
- Valores numéricos (tempos, raios, tecla do eco) são decisões iniciais registradas aqui. Mudam por playtest, alterando esta tabela e a story afetada.
- Antes de implementar qualquer story, execute `/apsu-audit` (regra 02 do AGENTS.md). Notas técnicas são hipóteses a confirmar no código.

## Épicos

| Id | Épico |
|---|---|
| E1 | Onboarding e tutorial |
| E2 | Orientação, câmera e cutscenes |
| E3 | Ecolocalização |
| E4 | Combate, chefe e progressão |
| E5 | Bugs e colisão visual |
| E6 | Feedback e áudio |
| E7 | Legibilidade das fases |
| E8 | Qualidade e release |
| E9 | Harness e fluxo de trabalho |

## Rastreabilidade do feedback de intuitividade

| Feedback recebido | Stories |
|---|---|
| Bug na fase 2, estalactite e estalagmite invisíveis | US-001, US-006 |
| Seta no guardião, principalmente no primeiro | US-010, US-024 |
| Cutscenes mostrando focos, como o baú no naufrágio | US-011, US-021, US-022, US-023, US-024 |
| Apontar para o guardião pela câmera | US-012 |
| Chegou ao portal e não conversou | US-003 |
| Primeira fase como tutorial, intuitividade | US-013, US-014, US-020, US-025 |
| Radar temporário, como ecolocalização, por tecla | US-030, US-031, US-032, US-033, US-034 |
| Chefe deve dar dano só de encostar | US-002, US-040 |
| Windows não renderiza personagens; notebooks sem GPU dedicada lentos | US-007, US-008, US-009 |
| Docker deveria dar o mesmo resultado em qualquer máquina | US-075, US-076 |
| Investigar a fundo e permitir que a IA jogue | US-077 |

## Sprint S0: Estabilização de renderização

### US-007 | Diagnóstico de renderização 3D no início | 3 pts | P0 | S0 | E8 | done
**Como** time, **quero** que o jogo registre o pipeline gráfico e o suporte a 3D ao iniciar, **para** descobrir em segundos por que os personagens não aparecem.
- Fonte: playtest no Windows, cenário e HUD renderizam mas herói, NPCs e inimigos não.
- Depende de: nenhuma
- Notas técnicas: personagens são malhas OBJ/MTL numa SubScene 3D (Runtime3DLayer); cenário, HUD e partículas são Canvas. Hipótese principal: SCENE3D indisponível ou pipeline Prism em software no Windows. Confirmado em experimento: no pipeline Prism sw, Platform.isSupported(SCENE3D) é falso e uma caixa 3D gera 0 pixels (o 2D aparece normal), enquanto no ES2 (OpenGL, inclusive Mesa em software) a mesma caixa gera 52.900 pixels. No jogo, o pipeline sw reproduz o print do Windows: HUD, aviso de interação e sombras aparecem, sem herói, guardião nem inimigos, e a vida cai por inimigos invisíveis. O JavaFX escreveu 1.240 avisos de SCENE3D em cerca de 60 s. O Makefile não renderiza nada em tempo de execução, só escolhe JDK e opções da JVM.

**Critérios de aceitação**
- [x] Dado o início do jogo em qualquer sistema, quando a janela abre, então o console registra uma linha [GFX] com sistema operacional, versão do JavaFX, resultado de SCENE3D e pipeline Prism em uso.
- [x] Dado SCENE3D não suportado, quando a fase 1 inicia, então o HUD exibe por 5 s o aviso "Modo 2D: 3D indisponível neste equipamento".
- [x] Dado a falha ao carregar qualquer OBJ ou MTL, quando ocorre, então o console registra [GFX] com o caminho do recurso e a causa, sem encerrar o jogo.
- [x] Dado make test, quando executado, então um teste cobre a formatação da linha de diagnóstico sem exigir JavaFX.

### US-008 | Fallback 2D para personagens quando o 3D não está disponível | 8 pts | P0 | S0 | E8 | done
**Como** jogador, **quero** ver herói, NPCs e inimigos mesmo sem suporte a 3D, **para** jogar em qualquer computador.
- Fonte: playtest no Windows.
- Depende de: US-007
- Notas técnicas: o README informa que há sprites PNG para os personagens e o runtime tem SpriteManager. O fallback desenha esses sprites no Canvas quando SCENE3D falta ou o carregamento 3D falha. Implementar em classe nova, sem engordar RenderEngine.

**Critérios de aceitação**
- [x] Dado SCENE3D indisponível, quando a fase 1 inicia, então herói, guardião e inimigos aparecem como sprites no Canvas, na mesma posição e com escala até 10% diferente da versão 3D.
- [x] Dado o fallback ativo, quando há colisão, dano ou diálogo, então o comportamento é idêntico ao do modo 3D, coberto pelos mesmos testes.
- [x] Dado o 3D disponível, quando o jogo inicia, então o fallback não é usado e o modo 3D permanece idêntico ao atual.
- [x] Dado a opção -Dapsu.render=2d, quando o jogo inicia, então o fallback é forçado, permitindo testá-lo em qualquer máquina.
- [x] Dado um personagem sem sprite correspondente, quando make test é executado, então um teste falha apontando o asset faltante.
- [x] Dado o fallback ativo, quando medido com APSU_PERF=1 na mesma máquina, então o frame-time p95 não é pior que o do modo 3D.

### US-009 | Modo híbrido adaptativo entre 3D e Canvas 2D | 5 pts | P1 | S0 | E8 | done
**Como** jogador com notebook sem GPU dedicada, **quero** que o jogo misture 3D e 2D conforme o desempenho, **para** jogar com fluidez sem perder os personagens.
- Fonte: diagnóstico do time. Com GPU dedicada (PC do laboratório) o 3D roda bem; em notebooks sem GPU dedicada o JavaFX renderiza em software e fica lento. O cache do jogo guarda só o estado da partida e não é a causa.
- Depende de: US-008
- Notas técnicas: o Makefile já detecta aceleração e escolhe o perfil (APSU_PROFILE low, balanced ou performance, repassado como -Dapsu.quality). O modo híbrido usa esse perfil e a medição de frame-time. Medição feita no ambiente de teste (Mesa llvmpipe, perfil low): fase 1 a cerca de 9 a 10 fps com pulse p50 de 96 ms, enquanto update custa 0,02 ms e render do Canvas 0,2 a 1 ms, sem GC relevante. Ou seja, o custo está na rasterização da SubScene 3D em software, não na lógica, no carregamento de modelos (OBJ assíncrono, 300 a 700 ms uma vez) nem no cache de estado. Com o pipeline sw (sem 3D) o mesmo trecho roda a cerca de 33 fps. Hipóteses a medir: o custo vem da SubScene 3D em software e cai ao renderizá-la em resolução menor no perfil low; manter 3D só no herói e no guardião e desenhar inimigos e props em sprites no Canvas pode bastar. Os elementos em 3D ficam numa constante configurável.

**Critérios de aceitação**
- [x] Dado o perfil low (sem aceleração detectada), quando o jogo inicia, então o modo híbrido é escolhido sem intervenção, com herói e guardião em 3D e inimigos e props em sprites no Canvas.
- [x] Dado o modo 3D completo com p95 de frame-time acima de 33 ms por 10 s seguidos, quando isso é medido em execução, então o jogo migra um grupo de elementos secundários para 2D por vez e registra a mudança em [GFX].
- [x] Dado o modo híbrido ativo, quando o p95 volta abaixo de 16,7 ms, então o jogo não volta ao 3D completo até a fase reiniciar, evitando oscilação.
- [x] Dado a opção -Dapsu.render=3d, hybrid ou 2d, quando informada, então ela prevalece sobre a escolha automática.
- [x] Dado um notebook sem GPU dedicada do time, quando a fase 2 é jogada por 2 minutos com APSU_PERF=1, então o p95 no modo híbrido é menor que no 3D completo e nenhum quadro passa de 50 ms, com os dois resultados em docs/qa/performance.md.
- [x] Dado um computador com GPU dedicada, quando o jogo inicia, então o 3D completo é mantido.

### US-077 | Playtest automático por IA em display virtual | 5 pts | P0 | S0 | E9 | done
**Como** time, **quero** que a IA jogue o jogo e me mostre capturas e medições, **para** verificar bugs visuais e desempenho sem depender de jogar a cada mudança.
- Fonte: pedido do time, "se tivesse uma forma da própria IA jogar".
- Depende de: nenhuma
- Notas técnicas: tools/harness/play/ai_play.py usa Xvfb, xdotool e ImageMagick, sem alterar o projeto nem o Makefile. Limite: o Mesa em software mede o pior caso e não substitui GPU real, áudio nem sensação de jogo.

**Critérios de aceitação**
- [x] Dado um build do jogo, quando ai_play.py é executado com um cenário, então abre o jogo em display virtual, navega do menu até a fase 1 e grava capturas de tela em target/ai-play.
- [x] Dado o pipeline es2 em Mesa, quando o cenário termina, então o relatório mostra pipeline, renderizador, avisos de SCENE3D e FPS e pulse por estado.
- [x] Dado o pipeline sw, quando o cenário termina com --assert-3d, então o script sai com código 1 e registra os avisos de SCENE3D.
- [x] Dado o pipeline es2, quando o cenário termina com --assert-3d, então o script sai com código 0.
- [x] Dado o script, quando executado, então não altera arquivos do projeto fora de target/ai-play.

## Sprint S1: Bloqueadores e base de testes

### US-001 | Corrigir obstáculos de coral invisíveis na fase 2 | 5 pts | P0 | S1 | E5 | done
**Como** jogador, **quero** ver cada estalactite e estalagmite da fase 2, **para** desviar deles sem sofrer colisões inexplicáveis.
- Fonte: playtest, "bug na fase 2, estalactite e estalagmite invisível".
- Depende de: nenhuma
- Notas técnicas: causa raiz confirmada por leitura do código e por jogo automático. GameContext cria os seis pares de CORAL (colisão e dano em updateP2), mas Runtime3DLayer.sceneryModel devolve null para CORAL de propósito (o único OBJ de recife era um conjunto completo) e é o único código que desenha cenário. Resultado: obstáculos que machucam e nunca são desenhados, em qualquer pipeline. Caminho recomendado: desenhar cada coral em Canvas, de forma procedural a partir da própria caixa de colisão (estalactite pendurada no topo, estalagmite subindo do fundo), em classe nova, o que funciona também sem 3D. O modelo 3D dedicado fica para a US-006. Preferir classe nova a editar GameContext (regra 10).

**Critérios de aceitação**
- [x] Dado a fase 2 iniciada, quando o jogo carrega os seis pares de coral, então cada hitbox registrada na QuadTree possui um elemento visual associado na mesma posição.
- [x] Dado um par de coral na tela, quando a caixa de colisão é comparada com a malha desenhada, então a diferença de limites é de no máximo 8 px em x e em y.
- [x] Dado o herói colidindo com um coral, quando o contato ocorre, então o coral atingido está visível no quadro do contato, confirmado em playtest dos seis pares.
- [x] Dado um teste automatizado da fase 2, quando executado com make test, então ele falha se existir hitbox de coral sem visual associado.
- [x] Dado APSU_PERF=1 make run na fase 2, quando comparado com a medição anterior à mudança, então o frame-time médio varia no máximo 5%.

### US-002 | Dano por contato do Kullullû | 3 pts | P0 | S1 | E4 | done
**Como** jogador, **quero** sofrer dano ao encostar no chefe, **para** entender que ele é perigoso sem esperar por um ataque.
- Fonte: feedback, "o boss tem que dar dano só de encostar".
- Depende de: nenhuma
- Notas técnicas: o Kullullû existe apenas na fase 5. Usar a QuadTree e a mecânica de dano atuais; a invulnerabilidade segue a regra já existente de dano ao herói.

**Critérios de aceitação**
- [x] Dado a fase 5 com o chefe vivo, quando a caixa do herói intersecta a do Kullullû, então o herói perde vida e o evento HERO_DAMAGED é publicado uma única vez.
- [x] Dado o herói em invulnerabilidade após o dano, quando o contato continua, então nenhum dano adicional é aplicado até o fim do período de 1,0 s.
- [x] Dado o valor do dano de contato do chefe, quando comparado ao de um inimigo comum, então ele é maior ou igual a este e menor que 25% da vida máxima do herói, em constante nomeada.
- [x] Dado o chefe em qualquer das suas fases de combate, quando há contato, então o dano é aplicado mesmo que o chefe não esteja atacando.
- [x] Dado um teste unitário, quando executado com make test, então ele cobre contato único, contato contínuo e contato durante a invulnerabilidade.

### US-003 | Feedback de saída bloqueada | 3 pts | P0 | S1 | E2 | done
**Como** jogador, **quero** saber por que não consigo sair da fase ao chegar ao portal, **para** voltar e conversar com o guardião.
- Fonte: feedback, "chegou no portal, e não conversou".
- Depende de: nenhuma
- Notas técnicas: o runtime já exige a tabuleta nas fases 1 a 4 para a transição. Falta comunicar o motivo ao jogador.

**Critérios de aceitação**
- [x] Dado uma fase de 1 a 4 sem a tabuleta, quando o herói alcança a saída, então o HUD mostra por pelo menos 3 s uma mensagem curta com o objetivo pendente.
- [x] Dado a mensagem de bloqueio exibida, quando o herói permanece na saída, então a mensagem não se repete mais de uma vez a cada 5 s.
- [x] Dado a saída bloqueada, quando o herói a alcança, então nenhuma transição ocorre e o estado da fase permanece inalterado.
- [x] Dado a tabuleta coletada, quando o herói alcança a saída, então a transição ocorre uma única vez e nenhuma mensagem de bloqueio aparece.

### US-004 | Testes de regressão da progressão por fase | 5 pts | P0 | S1 | E8 | done
**Como** time, **quero** testes automatizados das regras de progressão, **para** não quebrar a jornada das cinco fases ao mexer em intuitividade.
- Fonte: README (progressão, baú, chefe apenas na fase 5, partida nova limpa o estado).
- Depende de: nenhuma
- Notas técnicas: estender a suíte JUnit existente, sem depender de display gráfico.

**Critérios de aceitação**
- [x] Dado as fases 1 a 4, quando a saída é alcançada sem tabuleta, então a transição não ocorre; com tabuleta, ocorre uma única vez.
- [x] Dado a fase 2, quando o baú é aberto duas vezes, então o poder das bolhas é liberado uma única vez.
- [x] Dado qualquer fase de 1 a 4, quando o contexto é montado, então não existe Kullullû; na fase 5, existe exatamente um.
- [x] Dado uma partida reiniciada após a fase 5, quando a nova partida começa, então vida, tabuletas, poder das bolhas e entidades estão no estado inicial.
- [x] Dado make test, quando executado em máquina sem display, então os novos testes passam.

### US-005 | Adoção do harness e linha de base | 2 pts | P1 | S1 | E9 | todo
**Como** time, **quero** instalar o harness sem alterar o fluxo atual, **para** ganhar guardrails mantendo o Makefile e o AGENTS.md.
- Fonte: pedido do time.
- Depende de: nenhuma
- Notas técnicas: instalação por `python3 tools/harness/install.py`, idempotente.

**Critérios de aceitação**
- [ ] Dado o repositório após a instalação, quando install.py é executado uma segunda vez, então nenhum arquivo é alterado.
- [ ] Dado python3 tools/harness/verify.py, quando executado, então termina com código 0 e lista todas as verificações aprovadas.
- [ ] Dado make test antes e depois da instalação, quando os resultados são comparados, então o número de testes e o resultado são idênticos.
- [ ] Dado git status após a instalação, quando revisado, então aparecem apenas arquivos do harness, uma linha no Makefile, um bloco em DEVELOPMENT.md e entradas em .gitignore.

### US-006 | Modelo de coral de colisão dedicado e leve | 3 pts | P1 | S1 | E5 | todo
**Como** jogador, **quero** corais de colisão com forma legível, **para** prever por onde posso passar.
- Fonte: README, pendência "modelo de coral de colisão dedicado e leve".
- Depende de: US-001
- Notas técnicas: o desenho em Canvas da US-001 já resolve a visibilidade; este modelo 3D é o acabamento. O modelo atual de recife mistura cardume e vários materiais. Exportar com `make export-model MODEL=...`.

**Critérios de aceitação**
- [ ] Dado o novo modelo de coral, quando exportado, então ele usa no máximo 1 material e no máximo 2.000 triângulos.
- [ ] Dado os seis pares de coral da fase 2, quando renderizados, então todos usam o novo modelo com escala entre 0,9 e 1,1.
- [ ] Dado a silhueta do coral sobre o panorama da fase 2, quando revisada em playtest, então os seis pares são distinguíveis do fundo.
- [ ] Dado o modelo antigo fora de uso, quando a mudança é revisada, então nenhum arquivo de asset foi excluído (regra 08).

## Sprint S2: Orientação e cutscenes

### US-010 | Seta de orientação para o objetivo | 5 pts | P0 | S2 | E2 | done
**Como** jogador, **quero** uma seta apontando para o objetivo atual, **para** nunca ficar sem saber para onde ir.
- Fonte: feedback, "seta no guardião" e "seta no primeiro guardião".
- Depende de: nenhuma
- Notas técnicas: componente novo de HUD (proposta: pacote br.apsu.guidance), lendo a posição do objetivo sem alterar a lógica de GameContext.

**Critérios de aceitação**
- [x] Dado a fase 1 antes da primeira conversa, quando o guardião está fora da tela, então uma seta na borda da tela aponta para ele com erro angular menor que 5 graus.
- [x] Dado o guardião visível, quando o herói está a mais de 300 unidades dele, então a seta aponta para ele sem cobrir o herói nem o HUD.
- [x] Dado a tabuleta recebida, quando a conversa termina, então a seta passa a apontar para a saída da fase.
- [x] Dado a seta animada, quando o jogo roda com APSU_PERF=1, então não há aumento de coletas de lixo em relação à base anterior.
- [x] Dado a seta desenhada, quando renderizada, então usa apenas o tema claro, sem dark mode.

### US-011 | Sistema de cutscene de foco de câmera | 8 pts | P0 | S2 | E2 | done
**Como** jogador, **quero** cenas curtas que mostrem o que é importante, **para** entender os objetivos sem ler instruções.
- Fonte: feedback, "cutscenes mostrando focos de elementos".
- Depende de: nenhuma
- Notas técnicas: classes novas em pacote próprio (proposta: br.apsu.cutscene), acionadas pelo EventBus. A Camera ganha apenas um ponto de entrada para foco temporário. Integração mínima em GameContext, com confirmação do harness.

**Critérios de aceitação**
- [x] Dado uma cutscene com ponto-alvo e duração, quando ela inicia, então a câmera chega ao alvo em até 0,8 s, mantém o foco e retorna ao herói em até 0,8 s.
- [x] Dado uma cutscene em execução, quando o jogador pressiona Espaço, Enter ou Esc, então ela termina e o controle volta ao herói em até 0,3 s.
- [x] Dado uma cutscene em execução, quando há inimigos ativos, então o herói não sofre dano durante a cena e retoma sem salto de posição ao final.
- [x] Dado duas cutscenes disparadas no mesmo quadro, quando processadas, então são enfileiradas e executadas em ordem, sem sobreposição.
- [x] Dado um teste unitário, quando executado com make test, então ele cobre início, fim, pulo e fila sem exigir JavaFX.
- [x] Dado uma cutscene rodando com APSU_PERF=1, quando medido no perfil balanced, então o frame-time p95 não passa de 16,7 ms.

### US-012 | Câmera aponta para o primeiro guardião | 3 pts | P0 | S2 | E2 | done
**Como** jogador, **quero** que a câmera mostre o guardião logo no início, **para** saber que preciso ir até ele.
- Fonte: feedback, "apontar para o guardião pela câmera".
- Depende de: US-011
- Notas técnicas: usa o sistema de cutscene da US-011 e a seta da US-010 como reforço.

**Critérios de aceitação**
- [x] Dado uma nova partida na fase 1, quando o diálogo inicial do NPC termina, então a câmera foca o guardião por 2,0 s e volta ao herói.
- [x] Dado a cutscene ativa, quando o jogador a pula, então o foco termina e a seta da US-010 permanece visível.
- [x] Dado a partida reiniciada, quando a fase 1 recomeça, então a cutscene toca novamente uma única vez.
- [x] Dado a cutscene tocando, quando inimigos estão próximos, então nenhum deles causa dano durante a cena.

### US-013 | Aviso contextual de interação | 3 pts | P1 | S2 | E1 | done
**Como** jogador, **quero** ver qual tecla usar perto de algo interativo, **para** não precisar adivinhar.
- Fonte: feedback, "intuitividade".
- Depende de: nenhuma
- Notas técnicas: o diálogo usa Espaço ou Enter. Depois do baú da fase 2, Espaço também dispara bolhas, o que pode confundir a interação.

**Critérios de aceitação**
- [x] Dado o herói a menos de 150 unidades de um guardião ou do baú, quando a interação está disponível, então aparece o aviso "Espaço ou Enter para conversar" (ou "interagir" no baú) acima do alvo.
- [x] Dado o aviso visível, quando o herói se afasta mais de 180 unidades, então o aviso desaparece, sem piscar na fronteira do raio.
- [x] Dado o poder das bolhas desbloqueado, quando o jogador inicia um diálogo com Espaço, então nenhuma bolha é disparada naquele pressionamento.
- [x] Dado a conversa já concluída com um guardião, quando o herói volta a se aproximar dele, então o aviso não reaparece.

### US-014 | Objetivo atual dinâmico no HUD | 2 pts | P1 | S2 | E1 | done
**Como** jogador, **quero** ver no HUD o que fazer agora, **para** seguir a história sem consultar nada.
- Fonte: feedback, "intuitividade".
- Depende de: nenhuma
- Notas técnicas: o HUD já mostra objetivo de coleta no canto superior esquerdo.

**Critérios de aceitação**
- [x] Dado a fase 1 antes da conversa, quando o HUD é desenhado, então o objetivo mostra "Fale com o guardião".
- [x] Dado a tabuleta recebida, quando a conversa termina, então o objetivo muda para "Siga para o portal" em até 1 s.
- [x] Dado a fase 2 com o poder das bolhas bloqueado, quando o HUD é desenhado, então o objetivo mostra "Encontre o naufrágio e abra o baú".
- [x] Dado qualquer texto de objetivo, quando renderizado em 1366x768, então ele cabe no HUD sem sobrepor a barra de vida.

## Sprint S3: Tutorial e cutscenes de fase

### US-020 | Primeira fase como tutorial guiado | 8 pts | P0 | S3 | E1 | done
**Como** jogador novo, **quero** aprender os controles jogando a primeira fase, **para** entender empuxo, dano, GPS e diálogo sem manual.
- Fonte: feedback, "fazer a primeira fase ser um tutorial".
- Depende de: US-010, US-013, US-014
- Notas técnicas: passos do tutorial como máquina de estados em classe nova (proposta: br.apsu.tutorial), sem condicionais novas em GameContext.

**Critérios de aceitação**
- [x] Dado uma nova partida, quando a fase 1 começa, então o jogo guia em ordem: descer contra o empuxo, desviar de um inimigo, ler o GPS e conversar com o guardião, e cada passo só avança quando o jogador executa a ação.
- [x] Dado um passo do tutorial, quando o jogador passa mais de 15 s sem executá-lo, então a dica do passo é reforçada com destaque visual ou seta.
- [x] Dado o passo de desviar, quando o jogador sofre dano, então a vida nunca cai abaixo de 50% durante o tutorial e o passo é repetido.
- [x] Dado um jogador experiente, quando pressiona Esc durante o tutorial, então os passos restantes são pulados e a fase segue normalmente.
- [x] Dado o tutorial concluído, quando a conversa com o guardião termina, então as dicas somem e não reaparecem na mesma partida.
- [x] Dado a partida reiniciada, quando a fase 1 recomeça, então o tutorial recomeça do primeiro passo.

### US-021 | Cutscene do baú no naufrágio | 3 pts | P0 | S3 | E2 | done
**Como** jogador, **quero** que a câmera destaque o baú no naufrágio, **para** saber onde despertar o poder das bolhas.
- Fonte: feedback, "cutscenes mostrando focos de elementos, como o baú no naufrágio".
- Depende de: US-011
- Notas técnicas: a fase 2 posiciona o baú por volta de x=1800.

**Critérios de aceitação**
- [x] Dado a fase 2 recém-carregada sem o poder das bolhas, quando o herói entra na fase, então a câmera foca o naufrágio e o baú por 2,0 s e retorna ao herói.
- [x] Dado a cutscene concluída, quando o controle volta ao jogador, então a seta da US-010 aponta para o baú.
- [x] Dado o baú já aberto, quando a fase 2 é revisitada na mesma partida, então a cutscene não toca.
- [x] Dado a cutscene tocando, quando o jogador a pula, então o foco termina em até 0,3 s.

### US-022 | Desbloqueio do poder das bolhas com destaque no HUD | 3 pts | P1 | S3 | E2 | done
**Como** jogador, **quero** perceber claramente quando ganho o poder das bolhas, **para** saber que posso atacar.
- Fonte: feedback, "intuitividade".
- Depende de: US-021
- Notas técnicas: novo evento CHEST_OPENED conforme o contrato de eventos do README (produtor, payload, frequência, consumidor, teste).

**Critérios de aceitação**
- [x] Dado o baú aberto, quando a interação conclui, então o HUD troca "poder bloqueado" por "bolhas" com destaque animado de 1 a 2 s.
- [x] Dado o poder desbloqueado, quando o jogador chega ao primeiro quadro com controle, então a dica "Espaço para atacar" aparece uma única vez.
- [x] Dado o baú aberto, quando a abertura ocorre, então CHEST_OPENED é publicado uma única vez no EventBus e documentado com produtor, payload e consumidor.
- [x] Dado o baú já aberto, quando o jogador interage de novo, então nenhum efeito é reaplicado.

### US-023 | Introdução de guardião com foco de câmera | 3 pts | P1 | S3 | E2 | done
**Como** jogador, **quero** que a câmera apresente cada guardião das fases 2 a 4, **para** reconhecer rapidamente quem devo procurar.
- Fonte: feedback, "cutscenes mostrando focos de elementos".
- Depende de: US-011
- Notas técnicas: reutiliza a cutscene da US-011 e o aviso da US-013.

**Critérios de aceitação**
- [x] Dado as fases 2 a 4, quando o guardião chega a 800 unidades do herói pela primeira vez, então a câmera o foca por 1,5 s.
- [x] Dado a cutscene concluída, quando o herói se aproxima, então o aviso da US-013 funciona normalmente.
- [x] Dado o guardião já apresentado, quando o herói volta a passar perto, então a cutscene não repete.
- [x] Dado a cutscene tocando, quando o jogador a pula, então o foco termina em até 0,3 s.

### US-024 | Cutscene de abertura da saída após a tabuleta | 2 pts | P2 | S3 | E2 | done
**Como** jogador, **quero** ver onde fica a saída depois de pegar a tabuleta, **para** seguir sem procurar.
- Fonte: feedback, "seta no guardião" e cutscenes.
- Depende de: US-011
- Notas técnicas: o objetivo passa de conversar para sair, tratado também pelo HUD na US-014.

**Critérios de aceitação**
- [x] Dado a tabuleta recebida nas fases 1 a 4, quando o diálogo termina, então a câmera mostra a saída por 1,5 s e a seta passa a apontá-la.
- [x] Dado a cutscene tocando, quando o jogador a pula, então o foco termina em até 0,3 s.
- [x] Dado a cutscene já exibida na fase, quando o herói permanece na fase, então ela não repete.

### US-025 | Reescrita do diálogo inicial do NPC | 2 pts | P1 | S3 | E1 | done
**Como** jogador, **quero** um diálogo inicial curto e claro, **para** entender a missão em menos de 20 segundos.
- Fonte: feedback, "intuitividade".
- Depende de: nenhuma
- Notas técnicas: o NPC inicial explica hoje a missão de encontrar os guardiões e recuperar as cinco tabuletas sagradas.

**Critérios de aceitação**
- [x] Dado o diálogo inicial, quando lido do início ao fim, então tem no máximo 4 caixas de texto de até 120 caracteres cada.
- [x] Dado o conteúdo, quando revisado, então menciona explicitamente encontrar os guardiões, recuperar as cinco tabuletas sagradas e a tecla de interação.
- [x] Dado o jogador avançando com Espaço ou Enter, quando pressiona a tecla, então cada caixa avança uma única vez por pressionamento.
- [x] Dado o texto revisado, quando verificado, então não contém emojis nem travessões longos.

## Sprint S4: Ecolocalização

### US-030 | Habilidade de ecolocalização temporária | 8 pts | P0 | S4 | E3 | todo
**Como** jogador, **quero** acionar um radar de ecolocalização por tecla, **para** ver perigos e guardiões só quando preciso.
- Fonte: feedback, "radar ser temporário como uma habilidade de ecolocalização quando teclar e no teclado, ser intuitivo".
- Depende de: nenhuma
- Notas técnicas: tecla padrão Q (não conflita com WASD, setas, Espaço, Enter, E e F). Duração 4,0 s e recarga 8,0 s em constantes nomeadas. Classe nova (proposta: br.apsu.echo) acionada via InputManager.

**Critérios de aceitação**
- [ ] Dado o herói em qualquer fase, quando o jogador pressiona Q fora de recarga, então o eco ativa por 4,0 s.
- [ ] Dado o eco ativo, quando o tempo acaba, então ele desativa e a recarga de 8,0 s começa.
- [ ] Dado o eco em recarga, quando Q é pressionado, então nada é ativado e o indicador de recarga fica destacado.
- [ ] Dado uma nova partida, quando a fase 1 começa, então o eco já está disponível.
- [ ] Dado o jogo pausado, quando os contadores do eco estão ativos, então eles também pausam e retomam ao sair da pausa.
- [ ] Dado um teste unitário, quando executado com make test, então ele cobre ativação, expiração, recarga e pausa.

### US-031 | Onda visual do eco | 5 pts | P1 | S4 | E3 | todo
**Como** jogador, **quero** ver uma onda se expandindo a partir do herói, **para** perceber que o eco está revelando o ambiente.
- Fonte: feedback, radar como ecolocalização.
- Depende de: US-030
- Notas técnicas: desenho imperativo no Canvas, sem alocação por quadro.

**Critérios de aceitação**
- [ ] Dado o eco ativado, quando ele inicia, então um anel se expande do herói até o raio de 600 unidades em 1,0 s.
- [ ] Dado o anel alcançando guardiões, inimigos e obstáculos fixos, quando eles são atingidos, então cada um é destacado por 3,0 s em cor distinta (amarelo, vermelho e branco), coerente com o GPS.
- [ ] Dado o destaque desenhado, quando renderizado, então não cobre o herói nem o HUD.
- [ ] Dado o eco ativo na fase mais pesada, quando medido com APSU_PERF=1 no perfil balanced, então o frame-time p95 não passa de 16,7 ms.

### US-032 | GPS passivo e revelação de inimigos pelo eco | 3 pts | P1 | S4 | E3 | todo
**Como** jogador, **quero** que o GPS só mostre inimigos quando uso o eco, **para** que o radar seja uma habilidade e não uma muleta.
- Fonte: feedback, "radar ser temporário".
- Depende de: US-030
- Notas técnicas: o GPS atual mostra herói (triângulo), guardiões (amarelo) e inimigos (vermelho) o tempo todo.

**Critérios de aceitação**
- [ ] Dado o eco inativo, quando o GPS é desenhado, então mostra apenas o herói e os guardiões.
- [ ] Dado o eco ativo, quando o GPS é desenhado, então os inimigos dentro do raio do eco aparecem em vermelho.
- [ ] Dado o eco encerrado, quando passam 3,0 s, então os inimigos somem do GPS.
- [ ] Dado a fase 5, quando o eco está inativo, então o chefe continua visível no GPS por ser o objetivo.

### US-033 | Indicador de recarga e dica de tecla | 3 pts | P1 | S4 | E3 | todo
**Como** jogador, **quero** ver quando o eco está pronto e qual tecla usar, **para** usá-lo sem tentativa e erro.
- Fonte: feedback, "no teclado, ser intuitivo".
- Depende de: US-030
- Notas técnicas: o HUD do canto superior esquerdo já abriga vida, poder, objetivo e fase.

**Critérios de aceitação**
- [ ] Dado o HUD, quando o eco está disponível, então um ícone com a letra Q aparece abaixo da barra de vida.
- [ ] Dado a recarga em andamento, quando o HUD é desenhado, então o ícone mostra progresso de 0 a 100% e aparece esmaecido.
- [ ] Dado a fase 1, quando passam 20 s sem o jogador usar o eco, então a dica "Q: eco" aparece uma única vez.
- [ ] Dado o ícone e a dica, quando renderizados, então usam apenas o tema claro.

### US-034 | Evento semântico e som do eco | 2 pts | P2 | S4 | E3 | todo
**Como** jogador, **quero** ouvir o eco, **para** sentir o efeito da habilidade.
- Fonte: contrato de eventos e áudio do README.
- Depende de: US-030
- Notas técnicas: o core publica evento e AudioCue semântico; o nome do WAV fica em JavaFxAudioOutput. Fontes provisórias geradas por make audio-assets.

**Critérios de aceitação**
- [ ] Dado o eco ativado, quando ele inicia, então ECHO_PINGED é publicado uma única vez, com produtor, payload, cooldown e consumidor documentados.
- [ ] Dado o AudioCue ECHO_PING, quando resolvido, então o arquivo de áudio é referenciado apenas no adaptador JavaFxAudioOutput.
- [ ] Dado a recarga em andamento, quando Q é pressionado, então nenhum ECHO_PINGED é publicado.

## Sprint S5: Combate e chefe

### US-040 | Dano por contato do Leviatã Menor | 3 pts | P1 | S5 | E4 | todo
**Como** jogador, **quero** sofrer dano ao encostar no Leviatã Menor, **para** tratá-lo como ameaça desde o primeiro contato.
- Fonte: feedback, "o boss tem que dar dano só de encostar", estendido ao sub-chefe da fase 4.
- Depende de: nenhuma
- Notas técnicas: reutilizar o mecanismo da US-002.

**Critérios de aceitação**
- [ ] Dado a fase 4, quando a caixa do herói intersecta a do Leviatã Menor, então o herói perde vida e HERO_DAMAGED é publicado uma única vez.
- [ ] Dado o herói invulnerável, quando o contato continua, então nenhum dano adicional é aplicado até o fim de 1,0 s.
- [ ] Dado o valor do dano de contato, quando revisado, então está em constante nomeada e é maior ou igual ao de um inimigo comum da fase 4.

### US-041 | Telegraph dos ataques do Kullullû | 8 pts | P1 | S5 | E4 | todo
**Como** jogador, **quero** um aviso visual antes de cada ataque do chefe, **para** reagir com justiça e aprender os padrões.
- Fonte: README, pendência "telegraphs" do chefe.
- Depende de: US-002
- Notas técnicas: um sinal por padrão de ataque, documentado na matriz de assets do README.

**Critérios de aceitação**
- [ ] Dado cada padrão de ataque do chefe, quando o ataque está prestes a ocorrer, então um sinal visual dedicado aparece de 0,6 a 1,0 s antes do dano.
- [ ] Dado o sinal visível, quando o tempo de aviso não terminou, então o ataque ainda não causa dano.
- [ ] Dado os padrões das fases de combate, quando comparados, então cada um tem sinal distinto em forma ou cor, listado em documento.
- [ ] Dado o sinal desenhado, quando renderizado, então não cobre o herói nem o HUD.
- [ ] Dado um teste unitário, quando executado com make test, então ele valida a janela de 0,6 a 1,0 s entre sinal e dano.
- [ ] Dado a arena com sinais ativos, quando medida com APSU_PERF=1 no perfil balanced, então o frame-time p95 não passa de 16,7 ms.

### US-042 | Invulnerabilidade com feedback sem retângulo | 3 pts | P1 | S5 | E6 | todo
**Como** jogador, **quero** perceber o dano e a invulnerabilidade sem poluição visual, **para** ler a silhueta do herói durante o combate.
- Fonte: ARCHITECTURE.md, "não desenhar contornos retangulares intermitentes sobre a silhueta durante dano".
- Depende de: US-002
- Notas técnicas: a animação de sangue já existe e continua.

**Critérios de aceitação**
- [ ] Dado dano ao herói, quando a invulnerabilidade inicia, então o herói pisca por 1,0 s por variação de opacidade da malha, sem retângulo desenhado.
- [ ] Dado a animação de sangue, quando ocorre, então dura no máximo 0,5 s e não cobre o HUD.
- [ ] Dado o fim da invulnerabilidade, quando o período termina, então a opacidade do herói volta a 100%.

### US-043 | Eventos de áudio do chefe | 5 pts | P2 | S5 | E6 | todo
**Como** jogador, **quero** ouvir a introdução, os ataques, o atordoamento e a derrota do chefe, **para** sentir o peso do combate.
- Fonte: README, eventos BOSS_INTRO, BOSS_ATTACK, BOSS_STAGGER e BOSS_DEFEATED pendentes.
- Depende de: US-041
- Notas técnicas: cada evento só entra com produtor, payload, frequência, consumidor e teste.

**Critérios de aceitação**
- [ ] Dado cada um dos quatro eventos, quando registrado, então produtor, payload, cooldown, prioridade e consumidor estão documentados no README.
- [ ] Dado os AudioCues correspondentes, quando resolvidos, então nenhum arquivo de áudio é referenciado fora do adaptador.
- [ ] Dado make audio-assets, quando executado, então gera fontes provisórias substituíveis e registra créditos em docs/audio/SOURCES.md.
- [ ] Dado um teste unitário, quando executado com make test, então cada evento é publicado uma única vez por ocorrência.

### US-044 | Reinício limpo após derrota | 2 pts | P1 | S5 | E4 | todo
**Como** jogador, **quero** tentar de novo depois de perder toda a vida, **para** não precisar fechar e abrir o jogo.
- Fonte: README, partida nova deve limpar o estado.
- Depende de: nenhuma
- Notas técnicas: a derrota reinicia a fase atual, preservando as tabuletas já coletadas na partida. Um novo jogo reinicia tudo.

**Critérios de aceitação**
- [ ] Dado a vida do herói chegando a zero, quando a derrota é detectada, então uma tela oferece "Tentar de novo" e "Menu".
- [ ] Dado "Tentar de novo", quando escolhido, então a fase atual recomeça com vida cheia, chefe e inimigos restaurados e sem entidades residuais.
- [ ] Dado "Tentar de novo" após derrota na fase 5, quando a fase recomeça, então existe exatamente um Kullullû.

## Sprint S6: Legibilidade das fases 3 e 4

### US-050 | Direção e força visual das correntes e vórtices | 5 pts | P1 | S6 | E7 | todo
**Como** jogador, **quero** ver para onde uma corrente me empurra e com que força, **para** planejar a travessia.
- Fonte: README, fase 3, "orientação/força legíveis" e correntes abissais em vórtice.
- Depende de: nenhuma
- Notas técnicas: a física já existe; falta a leitura visual.

**Critérios de aceitação**
- [ ] Dado uma corrente, quando desenhada, então exibe setas ou partículas animadas na direção real da força aplicada ao herói.
- [ ] Dado correntes de intensidade diferente, quando comparadas, então a intensidade tem pelo menos 3 níveis visuais distintos.
- [ ] Dado o herói dentro da corrente, quando ela atua, então não há tremor da malha nem da câmera.
- [ ] Dado um teste unitário, quando executado com make test, então o sentido desenhado coincide com o sinal do vetor de força aplicado.

### US-051 | Sinal visual da zona de pressão | 3 pts | P2 | S6 | E7 | todo
**Como** jogador, **quero** saber quando entro numa zona de pressão, **para** entender por que o herói afunda.
- Fonte: README, fase 3, zona de pressão sem sinal próprio.
- Depende de: nenhuma
- Notas técnicas: a pressão reduz a flutuabilidade.

**Critérios de aceitação**
- [ ] Dado o herói entrando na zona de pressão, quando cruza a borda, então um efeito visual de transição de 0,3 s indica a entrada.
- [ ] Dado o herói dentro da zona, quando o HUD é desenhado, então um indicador textual "pressão" está visível.
- [ ] Dado o herói saindo da zona, quando cruza a borda, então o efeito e o indicador desaparecem em até 0,3 s.

### US-052 | Lava com contraste e área de dano coincidente | 5 pts | P1 | S6 | E7 | todo
**Como** jogador, **quero** distinguir a lava do fundo e saber onde ela machuca, **para** não ser ferido por algo que não vi.
- Fonte: README, fase 4, "contraste do hazard" e "área de dano coincide com visual".
- Depende de: nenhuma
- Notas técnicas: medir em captura da fase 4 no perfil balanced.

**Critérios de aceitação**
- [ ] Dado a lava e o fundo adjacente, quando medidos por amostragem em captura da fase 4, então a razão de luminância relativa é de pelo menos 3:1.
- [ ] Dado a caixa de dano da lava, quando comparada com a malha desenhada, então a diferença de limites é de no máximo 8 px.
- [ ] Dado o herói tocando a lava, quando o dano ocorre, então o contato está visualmente sobre a lava e o dano não se acumula além de uma vez por período de invulnerabilidade.
- [ ] Dado um teste unitário, quando executado com make test, então a caixa de dano da lava e a do visual são derivadas da mesma geometria.

### US-053 | Aviso antes da erupção do gêiser | 3 pts | P1 | S6 | E7 | todo
**Como** jogador, **quero** um aviso antes de um gêiser entrar em erupção, **para** desviar a tempo.
- Fonte: README, pendência de telegraph dos gêiseres.
- Depende de: nenhuma
- Notas técnicas: aplica-se a todos os gêiseres das fases 1 a 4.

**Critérios de aceitação**
- [ ] Dado um gêiser prestes a entrar em erupção, quando faltam 1,0 s, então um sinal visual (bolhas e brilho na base) aparece.
- [ ] Dado o aviso ativo, quando o herói está sobre o gêiser, então nenhum dano ou impulso é aplicado antes da erupção.
- [ ] Dado o início do aviso, quando ele ocorre, então GEYSER_WARNING é publicado uma única vez por erupção.

### US-054 | Cues de áudio ambientais das fases 3 e 4 | 5 pts | P2 | S6 | E6 | todo
**Como** jogador, **quero** ouvir correntes, pressão, gêiseres e lava, **para** perceber perigos mesmo olhando para outro ponto da tela.
- Fonte: README, eventos CURRENT_ENTERED, CURRENT_EXITED, PRESSURE_ENTERED, GEYSER_WARNING, GEYSER_ERUPTED e LAVA_CONTACT pendentes.
- Depende de: US-053
- Notas técnicas: contrato obrigatório de produtor, payload, cooldown, consumidor e teste por evento.

**Critérios de aceitação**
- [ ] Dado cada um dos seis eventos, quando registrado, então produtor, payload, cooldown, prioridade e consumidor estão documentados.
- [ ] Dado um mesmo cue disparado em sequência, quando o intervalo é menor que 1,0 s, então não há sobreposição de players concorrentes.
- [ ] Dado os cues reproduzidos juntos, quando mixados na fase 4, então não há clipping audível, confirmado em playtest com o volume padrão.
- [ ] Dado o core, quando revisado, então nenhum nome de arquivo de áudio aparece fora do adaptador.

## Sprint S7: Fase 5, escopo final e música

### US-060 | Arena da fase 5 com herói e chefe sempre visíveis | 3 pts | P1 | S7 | E7 | todo
**Como** jogador, **quero** ver o herói e o chefe o tempo todo na arena, **para** lutar sem ser surpreendido por elementos de cenário.
- Fonte: README, fase 5, sobreposição e contraste do foreground.
- Depende de: nenhuma
- Notas técnicas: o runtime já evita uma ruína que escondia o chefe; revisar as demais camadas.

**Critérios de aceitação**
- [ ] Dado a arena, quando capturada em três momentos de combate (início, meio e fase final do chefe), então nenhum elemento de foreground cobre a caixa do herói ou a do chefe.
- [ ] Dado o HUD da arena, quando desenhado, então não sobrepõe a caixa do chefe na posição inicial dele.
- [ ] Dado a ordem de desenho, quando revisada, então herói e chefe ficam acima de todo o cenário.

### US-061 | Epílogo e tela de vitória | 5 pts | P2 | S7 | E4 | todo
**Como** jogador, **quero** uma conclusão clara depois de derrotar o Kullullû, **para** sentir que a jornada terminou.
- Fonte: README, epílogo e encerramento pós-chefe marcados como a definir.
- Depende de: nenhuma
- Notas técnicas: decisão desta story: tela de vitória simples, sem cutscene, com texto de encerramento, as cinco tabuletas e retorno ao menu.

**Critérios de aceitação**
- [ ] Dado o Kullullû derrotado, quando a derrota é registrada, então a tela de vitória aparece em até 2,0 s.
- [ ] Dado a tela de vitória, quando exibida, então mostra o texto de encerramento e o contador de tabuletas "5/5".
- [ ] Dado a tela de vitória, quando o jogador pressiona Enter, então o jogo volta ao menu com o estado da partida limpo.
- [ ] Dado a tela de vitória, quando renderizada, então usa apenas o tema claro.

### US-062 | Revisão das dificuldades normal e difícil | 3 pts | P2 | S7 | E4 | todo
**Como** jogador, **quero** que a dificuldade difícil seja claramente diferente da normal, **para** ter um desafio justo.
- Fonte: README, variantes aleatórias do chefe na dificuldade difícil.
- Depende de: nenhuma
- Notas técnicas: a diferença hoje inclui variantes aleatórias do Kullullû.

**Critérios de aceitação**
- [ ] Dado as dificuldades normal e difícil, quando comparadas, então uma tabela em documento lista todos os parâmetros que mudam entre elas.
- [ ] Dado a dificuldade normal, quando a fase 5 é jogada, então o chefe usa sempre a mesma variante.
- [ ] Dado a dificuldade difícil, quando a semente aleatória é fixada em teste, então a sequência de variantes é reproduzível.

### US-063 | Stingers de transição e de tabuleta | 5 pts | P2 | S7 | E6 | todo
**Como** jogador, **quero** ouvir uma marca sonora ao pegar a tabuleta e ao mudar de fase, **para** sentir progresso.
- Fonte: README, stingers de entrada e transição pendentes.
- Depende de: nenhuma
- Notas técnicas: evento TABLET_COLLECTED já está declarado em GameEvent.Type; garantir produtor e consumidor antes de contar como implementado.

**Critérios de aceitação**
- [ ] Dado a coleta de uma tabuleta, quando ocorre, então TABLET_COLLECTED é publicado uma única vez e consumido pelo áudio.
- [ ] Dado a transição entre fases, quando ocorre, então um stinger toca sem cortar bruscamente a trilha da fase anterior.
- [ ] Dado os stingers, quando tocam sobre a música, então a mixagem não apresenta clipping audível, confirmado em playtest.
- [ ] Dado os novos arquivos de áudio, quando adicionados, então fonte, autor e licença estão registrados em docs/audio/SOURCES.md.

### US-064 | Escala e enquadramento dos modelos das cinco fases | 5 pts | P2 | S7 | E7 | todo
**Como** jogador, **quero** proporções coerentes entre herói, inimigos e cenário, **para** julgar distâncias e perigos corretamente.
- Fonte: README, escala e composição pendentes em todas as fases.
- Depende de: nenhuma
- Notas técnicas: registrar escala por modelo na matriz de assets do README.

**Critérios de aceitação**
- [ ] Dado cada modelo usado nas fases 1 a 5, quando revisado, então sua escala e unidade estão registradas em uma tabela.
- [ ] Dado herói e inimigos comuns, quando comparados, então a razão de altura entre eles está definida em documento e respeitada em todos os modelos.
- [ ] Dado cada fase, quando capturada no início, então herói, guardião e inimigos principais cabem no quadro sem corte.
- [ ] Dado a inclinação do nado, quando medida em jogo, então permanece dentro do limite de 20 graus do runtime.

## Sprint S8: Qualidade e release

### US-070 | Orçamento de desempenho definido e medido | 5 pts | P0 | S8 | E8 | todo
**Como** time, **quero** um orçamento de frame-time medido nas cinco fases, **para** saber se o jogo roda bem antes de entregar.
- Fonte: README, orçamento numérico e hardware de referência a definir.
- Depende de: nenhuma
- Notas técnicas: orçamento adotado: p95 de 16,7 ms e p99 de 25 ms no perfil balanced. O hardware de referência é o da máquina de medição, registrado com make system-info.

**Critérios de aceitação**
- [ ] Dado a máquina de medição, quando make system-info é executado, então sua saída é salva em docs/qa/performance.md.
- [ ] Dado APSU_PERF=1 make run em cada uma das cinco fases, quando a medição termina, então p95 e p99 do frame-time de cada fase estão registrados no mesmo documento.
- [ ] Dado uma fase acima do orçamento, quando a medição é revisada, então existe um item no backlog com a causa e a correção planejada.
- [ ] Dado a gravação JFR de uma fase pesada, quando resumida com make perf-summary, então o resumo está anexado ao documento.

### US-071 | Playtest de intuitividade com jogadores novos | 5 pts | P0 | S8 | E8 | todo
**Como** time, **quero** validar a intuitividade com pessoas que nunca viram o jogo, **para** confirmar que o feedback inicial foi resolvido.
- Fonte: feedback original de intuitividade.
- Depende de: US-020
- Notas técnicas: cinco jogadores, sem ajuda, observação com cronômetro e roteiro único.

**Critérios de aceitação**
- [ ] Dado 5 jogadores novos, quando jogam a fase 1 sem ajuda, então pelo menos 4 chegam à conversa com o primeiro guardião em até 90 s.
- [ ] Dado os mesmos jogadores, quando perguntados ao fim da fase 1, então pelo menos 4 explicam corretamente o objetivo geral do jogo.
- [ ] Dado a fase 2, quando jogada por eles, então pelo menos 4 encontram e abrem o baú sem ajuda.
- [ ] Dado o relatório do playtest, quando concluído, então cada problema encontrado vira story ou bug com prioridade, sem itens pendentes sem dono.

### US-072 | Cobertura mínima nos módulos novos | 3 pts | P1 | S8 | E8 | todo
**Como** time, **quero** cobertura de testes nos módulos criados neste ciclo, **para** manter a evolução segura.
- Fonte: README, cobertura via JaCoCo.
- Depende de: US-030
- Notas técnicas: aplica-se aos pacotes novos de orientação, cutscene, tutorial e eco.

**Critérios de aceitação**
- [ ] Dado make coverage, quando executado, então o relatório de target/site/jacoco/index.html é gerado.
- [ ] Dado os pacotes novos de guidance, cutscene, tutorial e echo, quando medidos, então cada um tem pelo menos 70% de cobertura de linhas.
- [ ] Dado a suíte completa, quando executada com make test, então todos os testes passam e nenhum foi desativado para atingir a meta.

### US-073 | Regressão completa das cinco fases | 5 pts | P0 | S8 | E8 | todo
**Como** time, **quero** jogar as cinco fases do início ao fim, **para** entregar sem bloqueadores.
- Fonte: README, checklist de QA por fase.
- Depende de: nenhuma
- Notas técnicas: duas rodadas completas, uma em normal e outra em difícil, com a checklist de QA do README.

**Critérios de aceitação**
- [ ] Dado uma partida nova na dificuldade normal, quando as cinco fases são jogadas em sequência, então o jogo é concluído sem bloqueador.
- [ ] Dado uma partida nova na dificuldade difícil, quando as cinco fases são jogadas em sequência, então o jogo é concluído sem bloqueador.
- [ ] Dado a checklist de QA por fase do README, quando preenchida, então cada item está marcado como aprovado, reprovado ou não aplicável, com evidência.
- [ ] Dado o resultado final, quando revisado, então não há bug de prioridade P0 ou P1 em aberto.

### US-074 | Empacotamento e checklist de release | 3 pts | P1 | S8 | E8 | todo
**Como** time, **quero** gerar o pacote final e validar o documento central, **para** entregar uma versão reproduzível.
- Fonte: Makefile, alvo package, e README como documento central de produção.
- Depende de: nenhuma
- Notas técnicas: validar o JAR gerado em máquina limpa com Java 21.

**Critérios de aceitação**
- [ ] Dado make package, quando executado, então o JAR com dependências e o pacote de recursos são gerados em target/.
- [ ] Dado o JAR gerado, quando executado com Java 21 em máquina limpa, então o jogo inicia e chega ao menu.
- [ ] Dado o README, quando revisado, então as matrizes de fases, assets e eventos refletem o estado real implementado, sem marcar como implementado o que não foi confirmado no runtime.
- [ ] Dado os créditos, quando revisados, então toda fonte nova de áudio e de asset tem origem e licença registradas.

## Sprint S9: Distribuição

### US-075 | Alvo docker-test para build e testes sem display | 3 pts | P1 | S9 | E8 | todo
**Como** time, **quero** rodar build e testes dentro do Docker, **para** obter o mesmo resultado em Windows e Linux sem depender de GPU.
- Fonte: pedido do time, Docker como ambiente igual para todos.
- Depende de: nenhuma
- Notas técnicas: o Docker padroniza JDK, Maven e bibliotecas, mas não a GPU, então serve para build e testes, não para jogar. Auditar o Dockerfile e o docker-compose existentes antes de editar. Não alterar docker-run.

**Critérios de aceitação**
- [ ] Dado make docker-test, quando executado em Windows com Docker Desktop ou em Linux, então a imagem é construída e mvn test roda dentro do container sem exigir display.
- [ ] Dado os testes falhando dentro do container, quando o alvo termina, então o código de saída do make é diferente de zero.
- [ ] Dado a mesma revisão do código, quando make test e make docker-test são executados, então o número de testes e o resultado são idênticos.
- [ ] Dado o alvo docker-run existente, quando o novo alvo é adicionado, então o comportamento de docker-run permanece inalterado.
- [ ] Dado o DEVELOPMENT.md, quando revisado, então documenta o alvo e deixa claro que o Docker não é usado para jogar no Windows.

### US-076 | Instalador com Java e JavaFX embutidos | 5 pts | P1 | S9 | E8 | todo
**Como** jogador, **quero** instalar e abrir o jogo sem Maven nem JDK, **para** jogar em qualquer computador Windows ou Linux.
- Fonte: pedido do time, padronizar a execução fora do Docker.
- Depende de: US-074
- Notas técnicas: usar jpackage sobre o JAR do make package, com runtime reduzido por jlink contendo os módulos JavaFX. O jpackage gera o instalador do sistema em que roda (Windows exige o WiX Toolset). O driver de vídeo continua sendo do computador; o fallback 2D da US-008 cobre a falta de 3D.

**Critérios de aceitação**
- [ ] Dado make package-app em Windows com JDK 21 e WiX, quando executado, então gera um instalador em target/ que não exige Java nem Maven na máquina de destino.
- [ ] Dado make package-app em Linux, quando executado, então gera um pacote ou app-image em target/ que roda sem Java instalado.
- [ ] Dado o aplicativo instalado em máquina limpa, quando aberto, então chega ao menu e registra a linha [GFX] no console ou no arquivo de log.
- [ ] Dado uma máquina sem Direct3D, quando o aplicativo instalado abre a fase 1, então os personagens aparecem pelo fallback 2D.
- [ ] Dado os alvos existentes do Makefile, quando package-app é adicionado, então make package e make test continuam com o mesmo resultado.
