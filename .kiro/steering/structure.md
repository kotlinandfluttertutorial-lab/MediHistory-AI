# MediHistory AI — Repository Structure Steering

## Repository Layout
```
MediHistory-AI/
  android/           — Android application module
  backend/           — FastAPI backend service
  docs/              — All project documentation
    architecture/    — ADRs, data model, API contracts, threat model
    design/          — Screen inventory, navigation map, design system, Figma refs
    jira/            — Jira ticket CSV, epics, kiro-prompts/
    product/         — PRD, user stories, feature specs
    release/         — Release checklist
  .kiro/
    steering/        — These steering files
    specs/           — Feature specs organised by domain
    skills/          — Skill guides organised by domain
    agents/          — Custom Kiro agent definitions
    workflows/       — Kiro workflow definitions
  .github/
    workflows/       — GitHub Actions CI/CD pipelines
```

## Android Naming Conventions
- ViewModel classes: `[Feature]ViewModel` (e.g. `PrescriptionViewModel`)
- Repository interfaces: `[Entity]Repository` (e.g. `PrescriptionRepository`)
- Repository implementations: `Room[Entity]Repository` or `[Entity]RepositoryImpl`
- UseCase classes: `[Action][Entity]UseCase` (e.g. `GetMedicineListUseCase`)
- DAO interfaces: `[Entity]Dao` (e.g. `PrescriptionDao`)
- Screen composables: `[Feature]Screen` (e.g. `PrescriptionListScreen`)
- Composable component files: placed in `ui/components/`
- Screen files: placed in `ui/screens/`
- ViewModel files: placed in `ui/viewmodel/`
- Repository files: placed in `data/repository/`
- DAO files: placed in `data/dao/`
- Entity files: placed in `data/model/`

## Kiro File Locations
- Steering files: `.kiro/steering/[name].md`
- Spec files: `.kiro/specs/[domain]/requirements.md`
- Skill files: `.kiro/skills/[domain]/SKILL.md`
- Jira prompt files: `docs/jira/kiro-prompts/MHA-NNN.md`

## CI Workflow Location
- GitHub Actions: `.github/workflows/[name].yml`

## File Placement Rules
- No application source files outside `android/` or `backend/`
- No secrets or credentials in any committed file
- No test fixtures with real patient data — synthetic data only
- Schema exports for Room: `android/app/schemas/`
