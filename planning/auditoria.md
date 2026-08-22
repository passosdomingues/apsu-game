# Objetivo

Continue a auditoria e implementação do projeto do jogo a partir do estado atual do repositório.

Você tem autonomia para tomar decisões de design e implementação quando houver um problema visual, técnico ou arquitetural evidente. Não espere minha aprovação para correções que sejam claramente melhorias objetivas. Só interrompa para perguntar quando houver uma decisão que altere significativamente o conceito, gameplay ou direção artística já estabelecida.

## 1. Auditoria completa antes de alterar

Leia e considere TODO o conteúdo relevante do projeto, não apenas os arquivos mais recentes:

- `planning/**`
- `gameplay/**`
- `STATE.md`
- `ROADMAP.md`
- `implementation plans`
- `walkthroughs`
- scripts Python/Blender
- código Java
- assets
- renders existentes
- documentação dos personagens
- documentação dos cenários
- configurações de build e execução

Não trate `STATE.md`, `ROADMAP.md` ou qualquer implementação anterior como verdade absoluta. Verifique no código e nos artefatos se aquilo que foi declarado como concluído realmente está implementado.

Para cada implementação anterior, determine:

1. O que foi especificado.
2. O que realmente foi implementado.
3. O que foi parcialmente implementado.
4. O que não foi implementado.
5. O que funciona tecnicamente, mas produz resultado visual ruim.
6. O que precisa ser redesenhado em vez de simplesmente corrigido.

Sempre diferencie:

- fato verificado;
- hipótese;
- decisão de design;
- melhoria proposta;
- item ainda não testado.

## 2. Execute e valide sempre que possível

Não faça apenas análise estática.

Quando as ferramentas e o ambiente permitirem:

- compile o projeto;
- execute os testes;
- execute o jogo;
- execute os scripts Blender;
- gere os `.blend`;
- gere os renders;
- compare renders anteriores e novos;
- faça pixel-diff quando fizer sentido;
- inspecione dimensões, escalas, bounding boxes e proporções;
- analise as imagens como um jogador, não apenas como programador;
- procure bugs que só aparecem durante execução;
- valide diferentes fases e estados do gameplay.

Se Blender, Maven ou alguma dependência não estiver disponível, registre explicitamente o que não pôde ser validado. Não considere "sintaxe válida" como equivalente a "funcionando".

## 3. Direção visual geral

Use tudo que estiver documentado em `planning/` como especificação artística e técnica.

Melhore sistematicamente:

- modelos 3D;
- proporções;
- silhuetas;
- materiais;
- texturas;
- iluminação;
- composição;
- profundidade;
- escala relativa;
- contraste;
- integração personagem/cenário;
- animações;
- efeitos;
- partículas;
- leitura visual durante gameplay.

A direção deve preservar a proposta 2.5D inspirada em jogos como Donkey Kong Country: personagens e elementos 3D renderizados sobre cenários com forte composição, profundidade e sensação de mundo físico.

Não transforme isso em um jogo 3D genérico.

## 4. Regra de cor 60/30/10

Aplique sistematicamente o princípio 60/30/10 em:

- cenários;
- personagens;
- inimigos;
- bosses;
- elementos interativos;
- efeitos;
- partículas;
- HUD.

Não aplique a regra mecanicamente.

Analise primeiro a paleta real dos assets e renders. Determine:

- cor dominante;
- cor secundária;
- cor de destaque/acento.

Garanta que personagens importantes tenham separação cromática suficiente do cenário.

O contraste deve funcionar também em escala reduzida, como durante gameplay.

## 5. Personagens

Analise os renders dos personagens como um conjunto.

Procure especialmente:

- silhuetas indistintas;
- proporções estranhas;
- elementos que parecem colados;
- materiais sem diferenciação;
- ausência de profundidade;
- falta de contraste com o cenário;
- poses estáticas;
- membros/caudas sem movimento convincente;
- escala inconsistente entre personagens;
- aparência de placeholder.

Use iluminação de recorte/rim light quando necessário para separar a silhueta do fundo.

Use materiais apropriados ao tipo de criatura. Quando fizer sentido, utilize PBR, subsurface scattering, Fresnel, roughness variation, normal information e outras técnicas de computação gráfica disponíveis no pipeline.

Não use efeitos apenas porque são tecnicamente sofisticados. Cada técnica precisa produzir uma melhoria visual perceptível.

## 6. Animação: não use apenas números fixos

Este ponto é obrigatório.

Sempre que houver caudas, tentáculos, corpos serpentiformes, barbatanas ou outras estruturas articuladas, NÃO resolva o problema simplesmente ajustando manualmente os ângulos de cada segmento.

Por exemplo, uma sequência como:

`0° → 20° → 35° → 20° → -10° → -30° → -45°`

deve ser tratada como uma curva geométrica a ser analisada, e não como sete números independentes.

Se existir uma descontinuidade visual, implemente uma função procedural que produza uma curva suave.

Para estruturas ondulantes, prefira modelos baseados em:

- senoides;
- curvas interpoladas;
- splines;
- amplitude progressiva;
- defasagem de fase;
- amortecimento;
- propagação da onda ao longo dos segmentos.

A amplitude normalmente deve aumentar em direção à extremidade da estrutura, enquanto a fase deve variar entre os segmentos.

A animação deve produzir uma onda que percorre o corpo, e não uma rotação simultânea de todos os segmentos.

### Exemplo conceitual

Para uma cauda com `N` segmentos:

```text
angle(i,t) =
    base_curve(i)
    + amplitude(i) * sin(phase(t) + frequency * i)

Onde:

i representa o segmento;
t representa o tempo/frame;
base_curve(i) representa a postura anatômica;
amplitude(i) aumenta progressivamente em direção à ponta;
phase(t) controla o avanço temporal;
frequency controla a propagação da onda.

A função final deve respeitar a anatomia e a direção de movimento do personagem.

Não copie literalmente essa fórmula se outra abordagem produzir resultado melhor. O requisito é o comportamento contínuo, não a fórmula específica.

7. Ciclos de nado

Crie ciclos de animação realmente diferentes entre frames.

Não aceite:

frames pixel-identicos;
pequenas mudanças irrelevantes;
apenas rotação global;
deslocamento rígido do personagem;
cauda simplesmente alternando entre duas poses.

O ciclo deve possuir:

propagação de movimento;
antecipação;
passagem da onda;
retorno;
continuidade entre último e primeiro frame;
movimento corporal coerente;
variação de cauda/barbatanas;
eventualmente banking/pitch/roll quando apropriado.

O último frame deve conectar suavemente ao primeiro.

Use quantidade de frames suficiente para evitar sensação de animação quebrada. Se 8 frames forem insuficientes, aumente. Não preserve 8 apenas porque a implementação anterior decidiu por 8.

8. Gere variações de movimento

A partir dos mesmos modelos, gere proceduralmente variações para:

idle;
nado lento;
nado normal;
nado rápido;
aceleração;
desaceleração;
subida;
descida;
mudança de direção;
ataque;
preparação do ataque;
execução;
recuperação;
dano;
morte;
interação.

Ataques devem ter leitura corporal clara.

Use princípios de animação:

anticipation;
squash/stretch quando apropriado;
follow-through;
overlap;
easing;
aceleração/desaceleração;
recuperação após impacto.
9. Física

Quando o comportamento for aquático, não use movimentação arbitrária se uma aproximação física simples puder produzir comportamento melhor.

Implemente, quando apropriado:

empuxo;
gravidade;
arrasto;
aceleração;
velocidade terminal;
resistência do meio;
flutuação;
impulso;
inércia.

A física não precisa ser uma simulação científica completa. Deve ser uma aproximação consistente e parametrizável que produza movimento visualmente convincente.

Separe claramente:

posição
velocidade
aceleração
forças
controle do jogador
animação visual

Não misture lógica física com manipulação puramente estética do sprite/modelo.

10. Integração personagem + cenário

Analise se os personagens parecem realmente existir dentro do cenário.

Verifique:

escala;
perspectiva;
profundidade;
iluminação;
sombra;
contraste;
oclusão;
posição relativa;
contato com elementos;
parallax;
movimento;
direção da luz.

Um personagem tecnicamente correto, mas que parece "colado" no fundo, deve ser considerado incorreto visualmente.

11. Gameplay

Jogue mentalmente e, se possível, execute o jogo como um usuário.

Pergunte em cada fase:

consigo identificar imediatamente o personagem?
consigo distinguir inimigos?
entendo o que é interativo?
os ataques têm feedback suficiente?
as colisões parecem corretas?
a escala parece consistente?
o movimento parece físico?
o cenário interfere na leitura?
existe algum elemento que parece placeholder?
existe algum comportamento que parece bug?
há inconsistências entre fases?
há mudanças de câmera ou escala inesperadas?

Não avalie somente código.

12. Scripts Blender/Python

Os scripts Python existentes são matéria-prima para evolução.

Leia-os antes de reescrever.

Extraia funções reutilizáveis para bibliotecas compartilhadas quando isso reduzir duplicação.

Priorize arquitetura procedural para:

geometria;
materiais;
iluminação;
paletas;
poses;
animações;
variações de personagens;
renderização;
sprites;
efeitos.

Evite copiar e colar lógica entre personagens.

Quando uma melhoria for aplicável a vários personagens, transforme-a em abstração reutilizável.

13. Renders como evidência

Sempre que houver renders disponíveis:

compare versões;
examine silhueta;
examine paleta;
examine contraste;
examine proporções;
examine animação frame a frame;
examine integração com o cenário.

Quando possível, use métricas objetivas:

pixel-diff;
bounding box;
área ocupada;
diferença de silhueta;
histograma/paleta;
contraste;
dimensões;
proporção entre elementos.

Não substitua avaliação visual por métricas. Use ambas.

14. Código Java

Audite os pontos identificados anteriormente e procure novos problemas.

Particularmente:

câmera;
HUD;
colisões;
partículas;
ataques;
física;
escalas;
animações;
transição entre fases;
estado persistente;
reset de estado;
coordenadas mundo/tela.

Procure bugs causados por estado residual entre fases.

Uma correção deve eliminar a causa-raiz sempre que possível, e não apenas esconder o sintoma.

15. Documentação viva

Depois de cada conjunto significativo de alterações:

atualize STATE.md;
atualize ROADMAP.md;
registre decisões relevantes;
registre problemas encontrados;
registre o que foi realmente testado;
registre o que não pôde ser testado;
registre resultados de comparação;
registre próximos passos.

Não marque algo como concluído se não foi validado.

Use estados como:

DONE
PARTIAL
VERIFIED
UNVERIFIED
BLOCKED

quando forem úteis.

16. Auditoria dos Implementation Plans e Walkthroughs

Não apenas leia os últimos plans e walkthroughs.

Faça uma auditoria retrospectiva:

Especificação
    ↓
Implementation Plan
    ↓
Implementação
    ↓
Render/Teste
    ↓
Resultado observado

Identifique divergências.

Se um walkthrough declarou sucesso, mas o resultado visual mostra que o problema continua, considere o problema NÃO resolvido.

17. Prioridade de execução

Trabalhe nesta ordem:

bugs que quebram gameplay;
bugs de estado/câmera/física;
problemas de escala e proporção;
problemas de silhueta;
problemas de animação;
integração personagem/cenário;
iluminação e materiais;
paleta e composição;
efeitos e refinamentos;
documentação.

Não gaste tempo polindo materiais enquanto existe um personagem com animação quebrada ou escala inconsistente.

18. Autonomia de design

Você tem autorização explícita para corrigir decisões visuais ruins.

Se encontrar algo como:

curva de cauda artificial;
pose estranha;
proporção ruim;
combinação de cores inadequada;
animação rígida;
iluminação insuficiente;
material incoerente;
silhueta ruim;

corrija diretamente.

Não me pergunte se deve trocar 35° por 25°, por exemplo.

Analise o problema em termos de princípios de animação, anatomia, geometria, composição e direção artística e implemente a solução que considerar tecnicamente e visualmente superior.

A única exceção é quando a alteração mudar substancialmente:

conceito do personagem;
mecânica do jogo;
identidade visual central;
estrutura arquitetural;
comportamento de gameplay previamente especificado.
19. Critério de qualidade

Não considere uma tarefa concluída porque:

o código compila;
o script passa no parser;
o .blend foi gerado;
existe um render;
o teste unitário passou.

Considere concluída quando:

implementação
+
execução
+
validação técnica
+
validação visual
+
integração com o restante do jogo

forem satisfatórias.

20. Comunicação durante a execução

Mantenha-me informado sobre o trabalho realizado.

Não faça um relato excessivamente detalhado de cada comando trivial.

Prefira checkpoints objetivos:

AUDITORIA
- encontrado X
- encontrado Y
- confirmado Z


IMPLEMENTAÇÃO
- corrigido X
- refatorado Y
- criado Z


VALIDAÇÃO
- teste A: passou
- render B: melhorou
- problema C: ainda não validado


DOCUMENTAÇÃO
- STATE.md atualizado
- ROADMAP.md atualizado

Se encontrar uma nova causa-raiz importante, explique-a antes de simplesmente mascarar o sintoma.

21. Regra final

Não faça apenas o que está explicitamente listado neste prompt.

Use todo o conhecimento encontrado em planning/, gameplay, código, assets, renders, scripts e documentação para expandir as possibilidades do projeto.

O objetivo é evoluir o jogo como um sistema integrado:

gameplay
    ↕
física
    ↕
animação
    ↕
modelagem
    ↕
materiais
    ↕
iluminação
    ↕
composição
    ↕
renderização
    ↕
sprites
    ↕
código Java

Uma melhoria em uma camada deve ser avaliada pelo impacto nas demais.

No caso específico das criaturas aquáticas, a prioridade é eliminar movimentos artificiais baseados em ângulos independentes e substituir esse comportamento por animações procedurais contínuas, suaves e reutilizáveis.

A cauda do Adapa deve ser tratada como uma cadeia articulada que propaga uma onda pelo corpo, e não como uma lista de segmentos com rotações arbitrárias.

Se a solução anterior tiver sido construída com números manuais, refatore-a.

Depois de implementar, gere novamente os frames, compare com os anteriores e confirme se a curva e o ciclo realmente melhoraram.

Por fim, atualize STATE.md e ROADMAP.md com o estado real do projeto.