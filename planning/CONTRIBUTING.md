# 🤝 CONTRIBUTING — Guia de Contribuição

> **Para quem é este documento?**
> Para todo desenvolvedor que vai tocar neste projeto — especialmente **juniores** que estão aprendendo.
> Escrito por sêniores para que você obtenha os mesmos resultados sem precisar aprender na base do erro.

---

## 🎯 Filosofia do Projeto

> *"Código bom não é o que o computador entende. É o que o próximo dev vai entender sem precisar te ligar."*

1. **Clareza antes de esperteza** — escreva para ser lido, não para impressionar
2. **Falhe cedo, falhe alto** — erros devem aparecer em startup, não no meio do gameplay
3. **Documento primeiro, código depois** — atualize `STATE.md` antes de começar qualquer task
4. **Não tem gambiarra pequena** — toda dívida técnica deve ser documentada

---

## 🔄 Fluxo de Trabalho (Para Cada Task)

```
1. Leia STATE.md para entender o contexto atual
2. Escolha uma task TODO em SPRINTS.md
3. Mude o status da task: TODO → IN_PROGRESS
4. Crie/edite os arquivos necessários
5. Teste manualmente os critérios de aceite
6. Atualize STATE.md com o que foi feito
7. Registre no CHANGELOG.md na seção [Unreleased]
8. Mude o status: IN_PROGRESS → DONE
```

---

## 📐 Padrões de Código Java

### Nomes e Convenções

```java
// ✅ CORRETO — campos simples do jogo usam camelCase curto
double heroX, heroY, camX;
int heroHP, tabs, bossHP;
boolean inv, fRight, tab1alive;

// ✅ CORRETO — constantes em UPPER_SNAKE_CASE
static final int W = 1366, H = 768;
static final double HERO_W = 56, HERO_H = 80;

// ✅ CORRETO — métodos curtos com verbo + objeto
void upP1()      // update Phase 1
void rMenu()     // render Menu
void dHero()     // draw Hero
void hitHero()   // hero hit processing

// ❌ ERRADO — abreviações sem contexto
void hH()        // o que é isso?
double x1, xx;  // qual x é qual?
```

### Tratamento de Erros

```java
// ✅ CORRETO — erros de arquivo não devem crashar o jogo
Image loadImg(String name) {
    try {
        InputStream is = getClass().getResourceAsStream("/" + name);
        if (is != null) return new Image(is);
        // Tenta filesystem como fallback
        File f = new File("resources/" + name);
        if (f.exists()) return new Image(f.toURI().toString());
        System.out.println("[INFO] " + name + " não encontrado → fallback geométrico");
    } catch (Exception e) {
        System.out.println("[WARN] " + name + ": " + e.getMessage());
    }
    return null;  // null é intencional — o código usa fallback geométrico
}

// ✅ CORRETO — verificar null antes de usar imagem
void drawHero(GraphicsContext gc) {
    if (imgHero != null) {
        gc.drawImage(imgHero, heroX, heroY, HERO_W, HERO_H);
    } else {
        drawHeroFallback(gc);  // formas geométricas
    }
}

// ❌ ERRADO — deixar NullPointerException acontecer
gc.drawImage(imgHero, x, y, w, h);  // BOOM se imgHero == null
```

### Animações e Timing

```java
// ✅ CORRETO — animação baseada em tempo (independente de FPS)
double t = nano / 1_000_000_000.0;  // converter para segundos
y = baseY + Math.sin(t * 2.5 + phase) * amplitude;

// ✅ CORRETO — cooldown de ação
if (nano - lastShot > 400_000_000L) {   // 400ms em nanosegundos
    fireBeam();
    lastShot = nano;
}

// ❌ ERRADO — animação baseada em frames (quebra se FPS variar)
frameCount++;
y = baseY + Math.sin(frameCount * 0.05) * amplitude;

// ❌ ERRADO — Thread.sleep() no game loop (NUNCA FAÇA)
Thread.sleep(16);  // isso trava o JavaFX Application Thread
```

### Iteração Segura sobre Listas

```java
// ✅ CORRETO — remover durante iteração
Iterator<double[]> it = beams.iterator();
while (it.hasNext()) {
    double[] b = it.next();
    b[0] += b[2];  // mover
    if (b[0] < 0 || b[0] > W) it.remove();  // remover com segurança
}

// ✅ CORRETO — removeIf para casos simples
particles.removeIf(p -> { p[4] -= 0.03; return p[4] <= 0; });

// ❌ ERRADO — ConcurrentModificationException
for (double[] b : beams) {
    if (outOfBounds(b)) beams.remove(b);  // NÃO FAÇA
}
```

### Renderização com GraphicsContext

```java
// ✅ CORRETO — save/restore ao fazer transformações
void drawHeroFlipped(GraphicsContext gc, double x, double y) {
    gc.save();                           // salva estado
    gc.translate(x + HERO_W, y);        // move origem
    gc.scale(-1, 1);                    // espelha
    gc.drawImage(imgHero, 0, 0, HERO_W, HERO_H);
    gc.restore();                        // SEMPRE restaura
}

// ❌ ERRADO — transformação sem restore vaza para próximos draws
gc.translate(100, 0);
gc.drawImage(img, 0, 0, w, h);
// próximo drawImage também estará deslocado!
```

---

## 🎮 Regras de Gameplay (Não Quebre Sem Aprovação)

| Regra | Valor Sábio | Valor Ira de Enki | Por quê é crítico |
|---|---|---|---|
| HP inicial do herói | 5 | 3 | Balanceamento definido no GamePlan |
| Velocidade do herói | 4.5 px/frame | 6.0 px/frame | Idem |
| Cooldown de disparo | 400ms | 400ms | Feedback satisfatório ao jogador |
| Duração de invencibilidade | 1.5s | 1.5s | Previne one-shot frustrating |
| HP do boss | 5 | 5 | 5 hits = satisfatório, não muito fácil |
| Intervalo de bolhas do boss | 2.2s | 1.2s | Diferença de dificuldade perceptível |

Para alterar qualquer valor, documente em `CHANGELOG.md` na seção `### Balance`.

---

## 📁 Onde Colocar Cada Arquivo

```
/Game/
├── src/main/java/           → Código Java (ApsuGame.java e futuras classes)
├── src/main/resources/      → Sprites PNG, sons, configs de nível
│   ├── hero.png             → deve chamar EXATAMENTE assim (ver loadImg)
│   ├── boss.png
│   ├── npc.png
│   ├── bg1.png, bg2.png, bg3.png
│   └── *.wav / *.mp3        → sons (Sprint 4)
├── mpi/                     → Código C do demo MPI
│   └── MapGenerator.c
├── planning/                → Documentação de equipe (NÃO tem código aqui)
├── pom.xml                  → Configuração Maven (NÃO edite sem saber o que faz)
├── Makefile                 → Automação de build
├── Dockerfile               → Container Docker
└── .gitignore               → NÃO commite: target/, .class, .idea/
```

---

## 🚨 O que NUNCA Fazer

| ❌ Proibido | ✅ Alternativa |
|---|---|
| `Thread.sleep()` no AnimationTimer | Use `nano - lastAction > delay` |
| `System.exit()` sem motivo | Use `state = State.MENU` ou `Platform.exit()` |
| `new Image()` a cada frame | Carregue no `start()`, guarde em campo |
| Modificar lista durante for-each | Use `Iterator.remove()` ou `removeIf()` |
| Hardcode de path (`/home/user/...`) | Use `getResourceAsStream("/arquivo.png")` |
| Push direto na main sem testar | Sempre execute `mvn javafx:run` antes |
| Deixar TODO no código sem ticket | Crie APSU-XXX no SPRINTS.md |
| Commits com mensagem "fix" ou "wip" | Use formato `APSU-XXX: descrição clara` |

---

## ✍️ Formato de Commit

```
APSU-NNN: verbo no imperativo em português ou inglês

# Exemplos:
APSU-011: Adiciona esqueleto do ApsuGame com janela fullscreen
APSU-022: Implementa colisão AABB entre herói e inimigos
APSU-016: Copia sprites para src/main/resources
bugfix: Corrige NullPointerException ao carregar bg3.png em Docker
docs: Atualiza STATE.md com progresso da Sprint 1
```

---

## 🧪 Checklist de Qualidade Antes de Marcar DONE

Antes de mudar uma task para `[x]` em `SPRINTS.md`, verifique:

- [ ] `mvn javafx:run` compila e executa sem erros no console
- [ ] Nenhum `NullPointerException` ou `ArrayIndexOutOfBoundsException`
- [ ] A feature funciona com **e sem** os sprites PNG (fallback geométrico)
- [ ] A feature funciona em **ambas as dificuldades** (Sábio e Ira de Enki)
- [ ] Sem memory leak óbvio (listas não crescem infinitamente)
- [ ] `STATE.md` atualizado com o que foi feito
- [ ] `CHANGELOG.md` atualizado na seção `[Unreleased]`
- [ ] Commit feito com mensagem no formato `APSU-NNN: descrição`

---

## 💡 Dicas para Juniores — Perguntas Frequentes

**Q: Por que usamos `double[]` e não uma classe `Enemy`?**
> A: Para este escopo (~3 tipos de entidade, ~5 inimigos por fase), a complexidade de classes separadas não vale o benefício. O código fica mais longo mas mais simples de seguir linearmente. Em um projeto maior (50+ tipos de entidade), usaríamos classes ou ECS.

**Q: O game loop não tem `Thread.sleep(16)`. Como controla o FPS?**
> A: `AnimationTimer` é gerenciado pela JVM e sincronizado com a taxa de atualização do monitor via VSync. Ele já chama `handle()` em ~60fps sem precisarmos de sleep. Adicionar sleep causaria stuttering (engasgos visuais).

**Q: Posso usar `Platform.runLater()` para atualizar a UI de outra thread?**
> A: Neste projeto, **não precisamos**. Todo o código roda na JavaFX Application Thread via AnimationTimer. Se no futuro precisarmos de I/O assíncrono (ex: salvar jogo), aí sim usamos `Platform.runLater()`.

**Q: Por que o `State.md` precisa ser atualizado sempre?**
> A: Porque em projeto colaborativo, qualquer membro da equipe precisa saber o estado real sem precisar ler o código. Em empresa, isso é o que separa uma equipe profissional de amadores. Trate como regra não negociável.

**Q: Errei e precisei fazer `git commit --amend`. É permitido?**
> A: Somente se o commit ainda não foi feito push. Após push, use `git revert` e documente no CHANGELOG o que foi revertido e por quê.
