# Sprints: As Águas de Apsu

Sprint ativa: S0

Plano de dez sprints com capacidade de 21 pontos (197 pontos planejados, 48 user stories). Duração sugerida: 2 semanas por sprint, ajustável pelo time. As stories de cada sprint ficam em `BACKLOG.md`; o relatório sai de `python3 tools/harness/backlog.py report` (ou `make sprint-status`).

Regras do plano:

- A capacidade (21 pontos) é um teto, não uma meta. Se o time medir velocidade menor, mova as stories de menor prioridade para a sprint seguinte e atualize o campo de sprint no cabeçalho da story.
- Dependências nunca apontam para sprints posteriores (o lint do backlog verifica).
- Cada sprint termina com uma revisão coletiva jogando a fase afetada do início ao fim, conforme o processo de sprint do README (preparação, produção, integração, revisão, consolidação, aceite).
- O que a sprint adiciona ao jogo precisa passar pela Definição de Pronto (`DEFINITION_OF_DONE.md`).

## S0 | Estabilização de renderização | capacidade 21

Meta: o jogo mostra os personagens no Windows e no Linux, diagnostica sozinho por que algo não renderiza e roda fluido em notebooks sem GPU dedicada, com modo híbrido de 3D e Canvas 2D.
Demo da revisão: abrir o jogo no Windows e no Linux, ver a linha [GFX] no console, ver os personagens (em 3D ou em fallback 2D) e jogar a fase 2 em um notebook sem GPU dedicada sem lentidão perceptível.
Riscos: a causa no Windows pode ser driver ou pipeline em software e não o código; por isso o diagnóstico vem antes do fallback.

## S1 | Bloqueadores e base de testes | capacidade 21

Meta: eliminar o bug visual da fase 2, fazer o chefe causar dano por contato, explicar o bloqueio do portal e travar a progressão com testes.
Demo da revisão: jogar a fase 2 vendo os seis pares de coral; encostar no Kullullû e perder vida uma vez por período de invulnerabilidade; tentar sair sem a tabuleta e ler a mensagem.
Riscos: a causa real do coral invisível pode estar no pipeline de assets e não na renderização; confirmar na auditoria.

## S2 | Orientação e cutscenes | capacidade 21

Meta: o jogador sempre sabe para onde ir (seta, objetivo no HUD, aviso de interação) e existe uma infraestrutura de cutscene reutilizável.
Demo da revisão: iniciar partida, ver a câmera apontar o guardião, seguir a seta e ler "Espaço ou Enter para conversar".
Riscos: integração com GameContext e Camera sem inflar os objetos centrais; manter a integração mínima.

## S3 | Tutorial e cutscenes de fase | capacidade 21

Meta: a fase 1 vira tutorial e as fases 2 a 4 ganham cutscenes de foco (baú, guardiões, saída).
Demo da revisão: jogar a fase 1 como jogador novo e a fase 2 até abrir o baú, vendo o destaque no HUD.
Riscos: o tutorial não pode punir (vida limitada a 50% como piso durante os passos).

## S4 | Ecolocalização | capacidade 21

Meta: radar temporário por tecla (Q), com onda visual, GPS passivo e indicador de recarga.
Demo da revisão: usar o eco numa fase com inimigos, ver o GPS revelar e esconder inimigos.
Riscos: custo de desenho do anel e dos destaques no loop de 60 FPS.

## S5 | Combate e chefe | capacidade 21

Meta: telegraphs do chefe, dano por contato do Leviatã Menor, feedback de dano limpo, eventos de áudio do chefe e reinício após derrota.
Demo da revisão: lutar contra o Kullullû, perder, tentar de novo e vencer a fase 5 com sinais claros de cada ataque.
Riscos: sincronizar o tempo do telegraph com o dano sem criar ataques injustos.

## S6 | Legibilidade das fases 3 e 4 | capacidade 21

Meta: correntes, pressão, lava e gêiseres legíveis, com áudio ambiental correspondente.
Demo da revisão: atravessar as fases 3 e 4 identificando cada perigo antes de tocá-lo.
Riscos: contraste da lava depende do panorama gerado por Blender; pode exigir make render-backgrounds com confirmação.

## S7 | Fase 5, escopo final e música | capacidade 21

Meta: arena da fase 5 limpa, tela de vitória, dificuldades documentadas, stingers e escala dos modelos revisada.
Demo da revisão: partida completa, da fase 1 à tela de vitória, com stingers nas transições.
Riscos: licenças de áudio novo; registrar sempre em docs/audio/SOURCES.md.

## S8 | Qualidade e release | capacidade 21

Meta: orçamento de desempenho medido, playtest de intuitividade com cinco jogadores, cobertura mínima e pacote final.
Demo da revisão: relatório de desempenho, relatório do playtest e JAR rodando em máquina limpa.
Riscos: o playtest pode revelar problemas que exijam sprint extra; reservar capacidade ao replanejar.

## S9 | Distribuição | capacidade 21

Meta: gerar um instalador com Java e JavaFX embutidos, para jogar sem Maven nem JDK.
Demo da revisão: instalar em um Windows limpo e em um Linux limpo, abrir o menu e jogar a fase 1.
Riscos: o jpackage só gera o instalador do sistema em que roda, então o de Windows precisa ser gerado em Windows com WiX.
