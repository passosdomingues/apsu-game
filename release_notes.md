# As Águas de Apsu — v20261125-213629

> **Jogo de ação subaquático** com mitologia suméria, mecânicas de Ecolocalização e batalha contra Kullullû, o guardião do abismo.

---

## 🎮 Como Jogar (Instruções por Sistema Operacional)

### ✅ Requisito único: Java 21 instalado

Verifique se já tem:
```
java -version
```
Deve aparecer `openjdk 21` ou `java version "21"`. Se não tiver, instale primeiro:

| Sistema | Link para instalar Java 21 |
|---|---|
| Windows | https://adoptium.net/temurin/releases/?version=21 |
| macOS | https://adoptium.net/temurin/releases/?version=21 |
| Linux (Ubuntu/Debian) | `sudo apt install openjdk-21-jdk` |
| Linux (Fedora/RHEL) | `sudo dnf install java-21-openjdk` |

---

### 🪟 Windows

1. Baixe o arquivo `apsu-game-20261125-213629-jar-with-dependencies.jar`
2. Clique duas vezes no arquivo — se abrir, pronto!
3. Se não abrir com clique duplo, use o Prompt de Comando:
```bat
java -jar apsu-game-20261125-213629-jar-with-dependencies.jar
```

> Dica: Crie um atalho na área de trabalho clicando com botão direito → Novo atalho → `java -jar "C:\caminho\apsu-game-20261125-213629-jar-with-dependencies.jar"`

---

### 🍎 macOS

1. Baixe o arquivo `apsu-game-20261125-213629-jar-with-dependencies.jar`
2. Abra o Terminal e execute:
```bash
java -jar ~/Downloads/apsu-game-20261125-213629-jar-with-dependencies.jar
```

> Se aparecer aviso de segurança: Preferências do Sistema → Privacidade e Segurança → clique em "Abrir mesmo assim"

---

### 🐧 Linux

1. Baixe o arquivo `apsu-game-20261125-213629-jar-with-dependencies.jar`
2. No terminal:
```bash
java -jar apsu-game-20261125-213629-jar-with-dependencies.jar
```

> Se não iniciar graficamente, verifique se tem um display: `echo $DISPLAY` deve mostrar `:0` ou similar.

---

## 🕹️ Controles

| Tecla | Ação |
|---|---|
| `↑ ↓ ← →` ou `W A S D` | Mover Adapa |
| `Espaço` | Disparar bolha |
| `Q` | **Ecolocalização** — emite onda sonora para revelar tabuletas ocultas |
| `E` | Interagir com Enki (guardião) |
| `ESC` | Pular tutorial / Pausar |
| `Enter` | Confirmar / Avançar diálogo |

---

## 🗺️ O Jogo

**4 fases + 1 arena de boss:**

| Fase | Ambiente | Desafio |
|---|---|---|
| 1 — Águas Claras | Recife costeiro | Tutorial de movimento e Ecolocalização |
| 2 — Cavernas de Coral | Naufrágio profundo | Desbloqueio das bolhas no baú ancestral |
| 3 — Correntes Abissais | Planície abissal | Correntes que arrastam + tabuleta em fenda |
| 4 — Abismo Vulcânico | Chaminés hidrotermais | Lava, gêiseres e sub-boss Leviatã |
| 5 — Templo de Apsu | Arena do boss | Kullullû — guardião das profundezas |

**Dica:** Fale com Enki em cada fase — ele dá a pista de onde a Tabuleta Sagrada está escondida. Use `Q` no lugar certo para revelá-la!

---

## 📦 O que mudou nesta versão (Sprint 14-16)

- ✅ Tabuleta Sagrada oculta revelada **exclusivamente** pela Ecolocalização `[Q]`
- ✅ Enki dá pistas específicas por fase sobre onde usar o sonar
- ✅ Diálogos de Enki na frente de tudo — sem oclusão por modelos 3D
- ✅ Minimap renomeado de RADAR → ECOLOCALIZAÇÃO
- ✅ Novo modelo 3D da segunda Tabuleta Sagrada integrado
- ✅ Fundo da Fase 5 (boss) → azul-abissal profundo
- ✅ Vulcão flutuante removido da Fase 4
- ✅ 6 corais enfileirados no chão de lava da Fase 4
- ✅ Boss Kullullû com mais vida (+70%) para combate mais intenso
- ✅ Tutorial visual com pílulas de tecla `[Q]`, `[E]`, `[↓]`
- ✅ 208 testes automatizados verdes | 60 FPS estável

---

*Engine: Java 21 + JavaFX 21 | Render híbrido 2D/3D*  
*Repositório: https://github.com/passosdomingues/apsu-game*
