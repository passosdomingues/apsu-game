# As Águas de Apsu — v20261125-213629

> Jogo de ação subaquático com mitologia suméria, mecânicas de Ecolocalização e batalha contra Kullullû.

---

## 📥 Downloads — Escolha seu sistema

| Sistema | Arquivo | Tamanho | Java necessário? |
|---|---|---|---|
| 🪟 **Windows** (64-bit) | `apsu-game-...-windows-x64.exe` | ~396 MB | ❌ Não — JRE embutido |
| 🐧 **Linux** (Ubuntu/Debian 64-bit) | `apsu-game-...-linux-x64.deb` | ~400 MB | ❌ Não — JRE embutido |
| 🍎 **macOS Apple Silicon** (M1/M2/M3) | `apsu-game-...-macos-arm64.dmg` | ~426 MB | ❌ Não — JRE embutido |
| 🍎 **macOS Intel** (x64) | `apsu-game-...-macos-x64.dmg` | ~429 MB | ❌ Não — JRE embutido |
| ☕ **Qualquer sistema** (JAR leve) | `apsu-game-...-linux-x64.jar` | ~15 MB | ✅ Sim — Java 21 |

> Cada instalador inclui o Java 21 embutido — **baixe e jogue, sem configuração.**

---

## 🪟 Windows — `.exe`

1. Baixe `apsu-game-v20261125-213629-windows-x64.exe`
2. Clique duas vezes para instalar
3. Abra pelo menu Iniciar: **As Águas de Apsu**

> Se o Windows Defender alertar: clique em **"Mais informações" → "Executar mesmo assim"**

---

## 🐧 Linux — `.deb` (Ubuntu, Debian, Mint, Pop!_OS)

```bash
sudo dpkg -i apsu-game-v20261125-213629-linux-x64.deb
apsu-game
```

> Para desinstalar: `sudo apt remove apsu-game`

---

## 🍎 macOS — `.dmg`

- **Apple Silicon (M1/M2/M3):** baixe o arquivo `macos-arm64.dmg`
- **Intel:** baixe o arquivo `macos-x64.dmg`

1. Clique duas vezes no `.dmg`
2. Arraste **As Águas de Apsu** para a pasta Aplicativos
3. Abra pelo Launchpad

> Se aparecer aviso: **Preferências do Sistema → Privacidade e Segurança → Abrir mesmo assim**

---

## 🕹️ Controles

| Tecla | Ação |
|---|---|
| `↑ ↓ ← →` ou `W A S D` | Mover Adapa |
| `Espaço` | Disparar bolha |
| `Q` | **Ecolocalização** — sonar que revela tabuletas ocultas |
| `E` | Falar com Enki (guardião) |
| `ESC` | Pular tutorial |
| `Enter` | Avançar diálogo |

**Dica:** Fale com Enki em cada fase. Ele revela onde usar `[Q]` para encontrar a Tabuleta Sagrada!

---

## 🗺️ Fases

| # | Ambiente | Desafio |
|---|---|---|
| 1 | Águas Claras (recife costeiro) | Tutorial — primeira tabuleta oculta nos recifes |
| 2 | Cavernas de Coral (naufrágio) | Baú ancestral desbloqueia as bolhas |
| 3 | Correntes Abissais | Correntes fortes · tabuleta em fenda rochosa |
| 4 | Abismo Vulcânico | Lava · gêiseres · sub-boss Leviatã |
| 5 | Templo de Apsu | Boss final: Kullullû |

---

## 📦 Novidades (Sprint 14–16)

- ✅ Tabuleta revelada **exclusivamente** pela Ecolocalização `[Q]` — sem entrega de bandeja
- ✅ Enki dá pista específica por fase sobre onde usar o sonar
- ✅ Diálogos de Enki sempre à frente dos modelos 3D (sem oclusão)
- ✅ Minimap: RADAR → **ECOLOCALIZAÇÃO**
- ✅ Fase 5 (boss): fundo azul-abissal profundo
- ✅ Fase 4: vulcão flutuante removido · 6 corais enfileirados no chão de lava
- ✅ Boss Kullullû com +70% de vida (combate mais intenso)
- ✅ Tutorial visual com pílulas de tecla `[Q]` `[E]` `[↓]`
- ✅ 208 testes automatizados verdes · 60 FPS estável

---

## 🔐 Verificação de integridade

Cada arquivo tem um `.sha256` correspondente para verificar autenticidade:

```bash
# Linux / macOS
sha256sum -c apsu-game-v20261125-213629-linux-x64.deb.sha256
```

---

*Engine: Java 21 + JavaFX 21 · Render híbrido 2D/3D*  
*Repositório: https://github.com/passosdomingues/apsu-game*
