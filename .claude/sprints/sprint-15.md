# Sprint 15 — Polimento Visual, Diálogo e Fundo do Boss

**Período:** Pós-Sprint 14 (Release v20261125)  
**Meta:** Corrigir diálogos truncados, melhorar fundo da Fase 5 (boss), remover vulcão flutuante da Fase 4, aumentar vida dos inimigos/boss e corrigir diálogos por fase.

---

## Diagnóstico Visual das Capturas (Sprint 15)

1. **Diálogo truncado** — Texto cortado em ~40 chars visíveis ("Adapa, meu filho. As profun"), apesar do conteúdo ser mais longo. O `charsShown` anima caractere por caractere mas o snapshot é tirado logo cedo.
2. **Fase 5 cinza/branco** — Fundo `phase-5-fundo.png` tem cor RGB≈(108,126,135) azul-acinzentado genérico. O boss Kullullû não tem identidade de templo abissal profundo.
3. **Vulcão flutuante na Fase 4** — Visible no canto superior esquerdo da captura 10. Elemento decorativo anacrônico.
4. **Diálogo Fase 2 genérico** — O segundo diálogo da Fase 2 (Guardião Celestial) faz pista para "segunda tabuleta" que é a mesma frase do Fase 3/4.
5. **Boss vive pouco** — Vida atual não exige muito esforço do jogador.

---

## Histórias Sprint 15

### APSU-V15A | Fundo do Templo Apsu (Fase 5) | 3 pts | ✅ commit `9dbcc9c`
- **Critérios:**
  - [x] AC1: `phase-5-fundo.png` e `phase-5.png` retintados para azul-abissal profundo (`RGB≈24,12,68`).
  - [x] AC2: Sensor visual confirma center pixel `(24,12,68)`.
  - [x] AC3: `mvn test` 208/208 verde.

### APSU-V15B | Remover Vulcão Flutuante da Fase 4 | 2 pts | ✅ commit `95155ea`
- **Critérios:**
  - [x] AC1: `SceneryElement(VOLCANIC_ROCK, 900, 0, 85, 260)` removido do `startP4()` em `GameContext.java`.
  - [x] AC2: Rocha inferior `(2200, 768-280)` mantida.
  - [x] AC3: `mvn test` 208/208 verde.

### APSU-V15C | Diálogos por Fase Verificados | 2 pts | ✅ commit `95155ea`
- **Critérios:**
  - [x] AC1: Fase 1 — diálogos específicos (recife, primeira tabuleta).
  - [x] AC2: Fase 2 — menciona naufrágio e baú ancestral.
  - [x] AC3: Todas as linhas ≤ 120 chars (já estava correto).
  - [x] AC4: `GuardianEntityTest` 100% verde.

### APSU-B15D | Aumentar Vida do Boss e Subchefes | 2 pts | ✅ commit `95155ea`
- **Critérios:**
  - [x] AC1: Boss Kullullû (var 0): HP 7 → **12** (modo Médio).
  - [x] AC2: Variantes: `{7,9,8}` → `{12,15,13}` (+70%/+67%/+63%).
  - [x] AC3: `BossEntityTest`, `BossAttackTelegraphTest` e `DifficultyTest` atualizados e verdes.
  - [x] AC4: `mvn test` 208/208 verde.

### APSU-P15E | Sprint Gate 15 | 1 pt | ✅
- **Critérios:**
  - [x] AC1: `mvn test` **208/208** verde após todas as histórias.
  - [x] AC2: Capturas geradas (11 screenshots em `.claude/capturas/inspecao/`).
  - [x] AC3: Dual push: `origin/main` e `origin/perf/apsu-r01-zero-alloc` em `95155ea`.
