# 📋 Planning — As Águas de Apsu

> **Pasta de planejamento e gerenciamento do projeto.**
> Mantida pela equipe como fonte única de verdade sobre o estado, progresso e decisões técnicas.

---

## 📂 Estrutura desta pasta

| Arquivo | Propósito |
|---|---|
| [`PLANO_DE_MELHORIAS_HOLISTICO.md`](./PLANO_DE_MELHORIAS_HOLISTICO.md) | **Plano holístico de melhorias** em 5 dimensões estratégicas (Arquitetura, CG/Shaders, Game Design, Testes, DevOps) |
| [`ROADMAP.md`](./ROADMAP.md) | Visão macro do projeto — fases, milestones e objetivos de longo prazo |
| [`SPRINTS.md`](./SPRINTS.md) | Sprints detalhadas com tasks, critérios de aceite e responsáveis |
| [`STATE.md`](./STATE.md) | **Estado atual do projeto** — o que está feito, em progresso e bloqueado |
| [`CHANGELOG.md`](./CHANGELOG.md) | Histórico de versões e mudanças significativas |
| [`ARCHITECTURE.md`](./ARCHITECTURE.md) | Decisões de arquitetura técnica (ADRs) e diagramas |
| [`CONTRIBUTING.md`](./CONTRIBUTING.md) | Guia de contribuição — orientações sênior → júnior |

---

## 🧭 Como usar esta pasta

### Para Desenvolvedores Juniores
1. Comece sempre pelo [`STATE.md`](./STATE.md) — é o "dashboard" atual do projeto
2. Leia seu sprint em [`SPRINTS.md`](./SPRINTS.md) e pegue uma task com status `TODO`
3. Quando terminar, atualize o status da task no `SPRINTS.md` e anote no `CHANGELOG.md`
4. Tem dúvida? Consulte [`ARCHITECTURE.md`](./ARCHITECTURE.md) e [`CONTRIBUTING.md`](./CONTRIBUTING.md)

### Para Desenvolvedores Sênior
1. Atualizar `STATE.md` ao início/fim de cada sprint
2. Revisar PRs com base nos critérios de aceite definidos em `SPRINTS.md`
3. Manter `ARCHITECTURE.md` atualizado com decisões tomadas
4. Usar `CHANGELOG.md` para documentar qualquer breaking change

---

## 🏷️ Convenção de Status

| Emoji | Status | Descrição |
|---|---|---|
| 🔴 | `BLOCKED` | Bloqueado por dependência externa ou decisão pendente |
| 🟡 | `IN_PROGRESS` | Em desenvolvimento ativo |
| 🟢 | `DONE` | Concluído e validado |
| ⚪ | `TODO` | Definido mas ainda não iniciado |
| 🔵 | `REVIEW` | Aguardando revisão/aprovação |
| ⛔ | `CANCELLED` | Cancelado com justificativa documentada |

---

## 📌 Quick Links

- **Repositório raiz:** `../` (Game/)
- **Código-fonte:** `../src/main/java/`
- **Recursos/Sprites:** `../src/main/resources/`
- **Build:** `../Makefile` e `../pom.xml`
- **Container:** `../Dockerfile`
