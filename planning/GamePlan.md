Atue como um Desenvolvedor Senior especialista em JavaFX 21 e Game Design. 

Preciso que você crie o código-fonte completo e funcional de um jogo 2D Side-Scroller em JavaFX (utilizando Canvas e AnimationTimer) para Java 21 LTS.

---

### 🎨 TEMA E HISTÓRIA (Mitologia Mesopotâmica)
- **Protagonista:** Adapa (um herói Apkallu, homem-peixe).
- **NPC:** Ancião Enki (Deus da sabedoria e das águas).
- **Vilão/Boss:** Kullullû (o Apkallu corrompido).
- **Enredo:** O oceano primordial (Apsu) foi envenenado por Kullullû. O sábio Enki envia Adapa para nadar pelas profundezas, evitar perigos, recuperar as 3 Tabuletas da Sabedoria e derrotar Kullullû no templo submerso para purificar o oceano.

---

### 🎮 MECÂNICAS DE JOGO (Sem Gravidade - Nado Livre)
1. **Movimentação:** Setas do teclado (UP, DOWN, LEFT, RIGHT) ou WASD para nadar livremente nas 4 direções.
2. **Ataque / Interação:** Tecla ESPAÇO (dispara um projétil de luz no Boss ou interage com o Easter Egg).
3. **Gerenciador de Sprites (Renderização 2.5D/Donkey Kong Style):**
   - O jogo deve tentar carregar imagens `.png` da pasta `resources/` (ex: `hero.png`, `boss.png`, `npc.png`, `bg1.png`).
   - CRÍTICO: Se a imagem PNG não for encontrada, o código DEVE desenhar formas geométricas coloridas (Canvas shapes) automaticamente como fallback, para que o jogo rode de primeira sem erros de arquivo ausente.

---

### 🏛️ ESTRUTURA DE ESTADOS E TELAS
O jogo deve usar uma máquina de estados simples (`GameState` enum):
1. `MENU`:
   - Título estilizado: "AS ÁGUAS DE APSU: A LENDA DOS APKALLU".
   - Seleção de Dificuldade: 
     - **Sábio (Fácil):** 5 de Vida, Inimigos lentos.
     - **Ira de Enki (Difícil):** 3 de Vida, Inimigos rápidos, projéteis extras.
   - Opção de Iniciar e Créditos Mitológicos rápidos.
2. `DIÁLOGO ENKI` (Antes da Fase 1):
   - Tela com avatar do Ancião Enki explicando a missão ao Herói Adapa. Pressione ESPAÇO para começar.
3. `FASE 1 - As Águas Claras`:
   - Nado lateral. Desviar de 3 peixes/esporos que se movem na vertical.
   - Objetivo: Chegar até o final do percurso e coletar a 1ª Tabuleta da Sabedoria.
4. `FASE 2 - As Cavernas de Coral`:
   - Cenário com limites no teto e chão (stalactites/corais).
   - **EASTER EGG:** Há um Baú antigo no fundo da caverna. Se o jogador se aproximar e apertar ESPAÇO, exibe uma caixa de texto na tela: *"Easter Egg Encontrado: Você achou o fóssil da Tartaruga Ancestral de 4.000 a.C.!"*.
   - Objetivo: Chegar ao fim da caverna e pegar a 2ª Tabuleta.
5. `FASE 3 - O Templo Submerso (Boss Fight)`:
   - Arena fixa. Kullullû (Boss) fica no canto direito disparando bolhas de veneno periodicamente.
   - Herói precisa desviar e apertar ESPAÇO para atirar feixes de luz contra o Boss.
   - O Boss precisa tomar 5 acertos para ser derrotado.
   - Objetivo: Derrotar o Boss e pegar a 3ª Tabuleta.
6. `VITÓRIA` / `GAME OVER`:
   - Telas de encerramento com opção de reiniciar o jogo (Restart).

---

### 📊 HUD (Interface na Tela)
- Barra de Vida (Corações ou Barra Vermelha/Verde).
- Tabuletas Coletadas (ex: 0/3, 1/3, 2/3, 3/3).
- Indicador da Fase Atual.
- Mensagem de Alerta/Diálogo no centro/rodape quando necessário.

---

### 💻 REQUISITOS DE CÓDIGO
- Forneça o código-fonte completo em uma classe Java principal (ex: `ApsuGame.java`) para que seja fácil de compilar e testar no IntelliJ / Eclipse / VS Code.
- Utilize padrão `AnimationTimer` para o Game Loop suave (60 FPS).
- Tratamento limpo de input de teclado e checagem de colisão simples por Bounding Box (`Rectangle.intersects`).

Gere todo o código com comentários explicativos de como incluir as imagens PNG futuramente e como rodar o projeto.

preciso de Makefile com Docker e aplique paralelismo com MPI, considerando meu hardware

rafael@mint-zena 
 :MMM:MMM`  :MM:`  ``    ``  `:MMM:MMM:    ---------------- 
.MMM.MMMM`  :MM.  -MM.  .MM-  `MMMM.MMM.   OS: Linux Mint 22.3 x86_64 
:MMM:MMMM`  :MM.  -MM-  .MM:  `MMMM-MMM:   Host: Inspiron 3583 
:MMM:MMMM`  :MM.  -MM-  .MM:  `MMMM:MMM:   Kernel: 6.8.0-137-generic 
:MMM:MMMM`  :MM.  -MM-  .MM:  `MMMM-MMM:   Uptime: 41 mins 
.MMM.MMMM`  :MM:--:MM:--:MM:  `MMMM.MMM.   Packages: 4468 (dpkg), 43 (flatpak) 
 :MMM:MMM-  `-MMMMMMMMMMMM-`  -MMM-MMM:    Shell: bash 5.2.21 
  :MMM:MMM:`                `:MMM:MMM:     Resolution: 1366x768 
   .MMM.MMMM:--------------:MMMM.MMM.      DE: Cinnamon 6.6.9 
     '-MMMM.-MMMMMMMMMMMMMMM-.MMMM-'       WM: Mutter (Muffin) 
       '.-MMMM``--:::::--``MMMM-.'         WM Theme: cinnamon (Mint-Y) 
            '-MMMMMMMMMMMMM-'              Theme: Mint-Y-Dark-Teal [GTK2/3] 
               ``-:::::-``                 Icons: Mint-Y-Teal [GTK2/3] 
                                           Terminal: vscode 
                                           CPU: Intel i7-8565U (8) @ 2.001GHz 
                                           GPU: Intel WhiskeyLake-U GT2 [UHD Graphics 620] 
                                           GPU: AMD ATI Radeon R5 M230 / R7 M260DX / Radeon 520/610 Mobile 
                                           Memory: 5202MiB / 15863MiB 

e ve se consegue rodar o Hunyuan3D localmente