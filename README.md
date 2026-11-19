# As Águas de Apsu

> Jogo de ação e aventura subaquática 2.5D retro arcade — Java 21 + JavaFX

Mergulhe nas profundezas do oceano primordial da mitologia suméria. Controle **Adapa**, o sábio pescador amaldiçoado, pelas águas de Apsu enquanto enfrenta monstros abissais, recupera tabuletas sagradas e desperta poderes ancestrais — até o duelo final contra o temível **Kullullu** no Templo das Profundezas.

---

## Capturas de Tela do Jogo

<table>
  <tr>
    <td align="center"><img src="docs/evolucao/gameplay/v20261118/01_menu_principal.png" width="340" alt="Menu Principal"/><br/><sub>Menu Principal — Adapa &amp; Enki</sub></td>
    <td align="center"><img src="docs/evolucao/gameplay/v20261118/02_fase_1_aguas_claras.png" width="340" alt="Fase 1"/><br/><sub>Fase 1 — Águas Claras: Enki Guardião, Tabuleta 3D &amp; Corais</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/evolucao/gameplay/v20261118/03_fase_2_cavernas_coral_navio.png" width="340" alt="Fase 2 — Navio e Baú"/><br/><sub>Fase 2 — Cavernas de Coral: Navio Naufragado, Baú &amp; Bolhas 3D</sub></td>
    <td align="center"><img src="docs/evolucao/gameplay/v20261118/03_fase_2_dialogo_enki.png" width="340" alt="Fase 2 — Diálogo Enki"/><br/><sub>Fase 2 — Diálogo in-game com Enki Celestial &amp; Tabuleta Sagrada</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/evolucao/gameplay/v20261118/04_fase_3_correntes_abissais.png" width="340" alt="Fase 3"/><br/><sub>Fase 3 — Correntes Abissais: Enki Eremita, Tabuleta 3D &amp; Enguia</sub></td>
    <td align="center"><img src="docs/evolucao/gameplay/v20261118/05_fase_4_abismo_vulcanico.png" width="340" alt="Fase 4"/><br/><sub>Fase 4 — Abismo Vulcânico: Portal Ativado 3D &amp; Magma</sub></td>
  </tr>
  <tr>
    <td align="center" colspan="2"><img src="docs/evolucao/gameplay/v20261118/06_fase_5_templo_apsu_boss.png" width="700" alt="Fase 5 — Boss"/><br/><sub>Fase 5 — Templo de Apsu: Duelo Final contra Boss Kullullû com Disparo de Bolhas 3D</sub></td>
  </tr>
</table>

### Adapa — Herói Atualizado (render 3D)

<div align="center">
  <img src="docs/evolucao/gameplay/v20261118/09_adapa_heroi_render.png" height="300" alt="Adapa — novo render 3D"/>
  <br/><sub>Adapa, o Apkallu — modelagem 3D por Wagner Donizete Gonçalves</sub>
</div>

---

## Como Jogar

### Instaladores Prontos

Baixe e instale diretamente — Java já vem incluído, sem dependências extras:

| Plataforma | Formato |
|---|---|
| Linux (Ubuntu/Debian x64) | `.deb` |
| macOS Apple Silicon (M1–M4) | `.dmg` |
| macOS Intel | `.dmg` |
| Windows 10/11 x64 | `.exe` |

👉 **[Releases no GitHub](https://github.com/passosdomingues/apsu-game/releases)**

### Compilar do Código-Fonte

Requer **Java 21+** e **Maven 3.9+**:

```bash
# Via Make (recomendado — verifica ambiente automaticamente)
make run

# Via Maven
mvn javafx:run
```

---

## Controles

| Ação | Teclas |
|---|---|
| Nadar | `W A S D` ou setas |
| Disparar bolhas / Interagir | `Espaço` ou `E` |
| Ecolocalização | `Q` ou `Shift` esquerdo |
| Pausa / Menu | `ESC` |

> O disparo de bolhas é desbloqueado ao interagir com o baú ancestral na **Fase 2**.

---

## As Cinco Fases

| # | Região | Desafio |
|---|---|---|
| 1 | **Águas Claras** | Introdução ao nado e esquiva; primeiro encontro com **Enki, o Sábio das Águas** |
| 2 | **Cavernas de Coral** | Galeão naufragado e o baú ancestral — desperta o poder das bolhas |
| 3 | **Correntes Abissais** | Correntes submarinas e zonas de pressão alteram a flutuabilidade |
| 4 | **Abismo Vulcânico** | Chaminés hidrotermais e confronto com o Leviatã Menor |
| 5 | **Templo de Apsu** | Duelo final contra Kullullu, lorde das profundezas |

---

## Personagens (Renders 3D Blender)

**Herói e Aliados**

<table>
  <tr>
    <td align="center"><img src="tools/personagens/renders/01_adapa_heroi.png" height="150" alt="Adapa"/><br/><b>Adapa</b><br/><sub>Herói</sub></td>
    <td align="center"><img src="tools/personagens/renders/03_enki_npc.png" height="150" alt="Enki"/><br/><b>Enki</b><br/><sub>Deus Guia</sub></td>
    <td align="center"><img src="tools/personagens/renders/03_enki_var_celestial.png" height="150" alt="Enki Celestial"/><br/><b>Enki Celestial</b><br/><sub>Guardião</sub></td>
    <td align="center"><img src="tools/personagens/renders/03_enki_var_eremita.png" height="150" alt="Enki Eremita"/><br/><b>Enki Eremita</b><br/><sub>Guardião</sub></td>
  </tr>
</table>

**Inimigos e Chefes**

<table>
  <tr>
    <td align="center"><img src="tools/personagens/renders/02_kullullu_boss.png" height="150" alt="Kullullu"/><br/><b>Kullullu</b><br/><sub>Boss Final</sub></td>
    <td align="center"><img src="tools/personagens/renders/12_leviata_menor.png" height="150" alt="Leviatã Menor"/><br/><b>Leviatã Menor</b><br/><sub>Sub-chefe</sub></td>
    <td align="center"><img src="tools/personagens/renders/04_peixe_sombrio_inimigo.png" height="150" alt="Peixe Sombrio"/><br/><b>Peixe Sombrio</b><br/><sub>Inimigo</sub></td>
    <td align="center"><img src="tools/personagens/renders/04_inimigo_medusa_eletrica.png" height="150" alt="Medusa Elétrica"/><br/><b>Medusa Elétrica</b><br/><sub>Inimigo</sub></td>
    <td align="center"><img src="tools/personagens/renders/11_arraiao_abissal.png" height="150" alt="Arraião Abissal"/><br/><b>Arraião Abissal</b><br/><sub>Inimigo</sub></td>
  </tr>
</table>

**Elementos de Cenário 3D**

<table>
  <tr>
    <td align="center"><img src="tools/personagens/renders/tabuleta.png" height="120" alt="Tabuleta Sagrada"/><br/><b>Tabuleta Sagrada</b><br/><sub>Coleta — flutua com rotação 3D</sub></td>
    <td align="center"><img src="tools/personagens/renders/corais.png" height="120" alt="Corais"/><br/><b>Corais</b><br/><sub>Obstáculo — Fase 2</sub></td>
    <td align="center"><img src="tools/personagens/renders/bolhas.png" height="120" alt="Bolhas"/><br/><b>Bolhas</b><br/><sub>Projétil 3D do Adapa</sub></td>
    <td align="center"><img src="tools/personagens/renders/portal.png" height="120" alt="Portal"/><br/><b>Portal de Fase</b><br/><sub>Saída para próxima fase</sub></td>
  </tr>
</table>

---

## Stack Técnica

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (LTS) |
| UI / Gráficos | JavaFX 21 — Canvas 2D + SubScene 3D |
| Modelos 3D | Blender 4.5.5 LTS → OBJ/MTL via pipeline de scripts `bpy` |
| Build | Apache Maven 3.9 + GNU Make |
| Testes | JUnit 5 — 198 testes, 100% aprovados |
| CI/CD | GitHub Actions — build, testes com xvfb e release automáticos |
| Containerização | Docker (ambiente de desenvolvimento) |
| Áudio | JavaFX MediaPlayer + trilha original FLAC/MP3 |

**Destaques da engine:**
- Renderização híbrida 2D/3D com degradê automático de qualidade (zero-alloc a 60 FPS)
- Sistema de ecolocalização: ondas de pulso acústico revelam segredos no escuro
- Bioluminescência dinâmica sincronizada com a mecânica de flutuabilidade

---

## Equipe e Contribuições

| Integrante | Papel | Contribuições Principais |
|---|---|---|
| ⚙️ **Rafael Passos Domingues** (`@passosdomingues`) | **Product Owner** · Engine Lead | Mecânica de jogo, simulação física de fluidos, arquitetura do motor JavaFX, pipeline gráfico híbrido 2.5D/3D, otimizações zero-allocation e automação CI/CD multiplataforma |
| 🌊 **Marcos Felipe dos Reis Bento** | **Scrum Master** · Cenógrafo 3D | Modelagem procedural dos cenários 3D das 5 fases no Blender, progressão visual profundidade-escuridão por nível do mar, arte dos fundos multicamada e easter eggs (Boto, Golfinho) |
| 🛠️ **Carlos Eduardo Pereira dos Santos** | **Quality Assurance** | Verificação de requisitos de jogabilidade, cobertura de testes, garantia de qualidade e integração de sistemas |
| 🧊 **Wagner Donizete Gonçalves** | Modelador 3D | Modelagem geométrica de todos os personagens: Adapa, chefe Kullullû, Enki, criaturas marinhas, inimigos e guardiões (Blender → OBJ/MTL) |
| 🎵 **Érik Alexandre Vieira Peres** | Compositor | Trilha sonora original, paisagens sonoras subaquáticas adaptativas por fase e efeitos de áudio 3D |
| 📜 **Cawe dos Reis Barbosa Domiciano** | Narrativo · Designer de Fases | Pesquisa mitológica mesopotâmica (Apkallu/Apsu), inspiração e paleta de profundidade de cada fase (cada nível do mar mais escuro), conceito dos easter eggs Boto e Golfinho e modelagem de inimigos que cometam atrocidades |

Revisão de código por área definida em [CODEOWNERS](CODEOWNERS).

---

## Documentação Técnica & Sprints

Disponível em [`docs/`](docs/) e documentação interna de sprint:

- [Arquitetura e Desenvolvimento](docs/ARQUITETURA_E_DESENVOLVIMENTO.md)
- [Pipeline 3D](docs/PIPELINE_3D.md)
- [Guia de Contribuição](docs/CONTRIBUTING.md)
- [Guia de Desenvolvimento](docs/DEVELOPMENT.md)

---

## Licença

Distribuído sob a licença **Apache 2.0** — consulte [LICENSE](LICENSE).
