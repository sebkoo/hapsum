# Hapsum

Point your camera at a receipt. Hapsum reads it, sorts it, and explains your month — all
on-device. Android app: Kotlin, Jetpack Compose, MVI on MVVM, offline-first, on-device AI.

## Module map

Current: `:app` · `:core:model` · `:core:testing` · `:core:data` · `:core:designsystem` ·
`:core:mvi` · `:core:ai` · `:feature:ledger` · `:feature:capture` · `:feature:confirm`.
End state: adds `:feature:insights`.
Modules are carved out when their code arrives, never as empty shells. `:core:mvi` is generic
infrastructure — lifecycle + coroutines only, never a domain type (ADR-0004).

## MVI contract (project law)

Every screen defines `data class XUiState`, `sealed interface XUiIntent`, `sealed interface
XUiEffect`; the ViewModel exposes `StateFlow<XUiState>` + `Flow<XUiEffect>` and a single
`onIntent(XUiIntent)` entry; reduction is a pure function; no state mutation outside the
reducer; effects are one-shot.

## Commands

- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Format + lint: `./gradlew spotlessCheck lint` (fix formatting: `./gradlew spotlessApply`)
- The gate: `scripts/verify.sh` — local runs, the pre-commit hook, and CI execute this exact
  script

## Conventions

- Commits: Conventional Commits, imperative subject ≤72 chars. `feat`/`refactor`/ADR-implementing
  commits carry a Problem / Decision / Trade-off body plus the ADR reference; `chore`/`docs`/`ci`
  bodies are a one-line why. No per-commit AI trailers — the README's AI section and this
  committed harness are the disclosure.
- Tests: `` `method — condition — outcome` `` backtick naming. MockK only (no Mockito), Turbine
  for Flow assertions, injected `DispatcherProvider` + `runTest` for coroutine time.
- ADRs: `docs/adr/`, numbered, indexed in `docs/adr/README.md`. Write one whenever a decision is
  hard to reverse: namespace, storage, navigation, module boundaries, AI abstraction, the
  monetization seam. Scaffold with `/adr`.

## Definition of done

Done is: `scripts/verify.sh` green AND the commit's checkbox ticked in `docs/PROGRESS.md`
(mirrored in the README progress board, same commit). Judgment — including this assistant's own
"looks done" — is never the criterion.

## Triage law

Honestly-unsupported input may ship: a receipt the parser genuinely cannot read (garbled OCR,
an unsupported layout) may surface as `null`/low-confidence fields for the confirm screen to
show empty or highlighted — that is truthful degradation, not a defect. Known-wrong output and
crash paths may not ship: a field that parses to a value that is actually incorrect, or any path
that throws instead of degrading, blocks the commit until fixed.

## Reporting rule

A transient failure that passes on retry (a flaky test, a one-off CI hiccup, a build that failed
once and then succeeded unchanged) still gets one line in the completion report — silently
retrying and reporting green hides signal a future flake investigation would need.

## Never do

- No secrets, tokens, or real receipt images — fixtures are synthetic by construction.
- No client, employer, or personal-context names anywhere in the repo.
- No unverified dependencies: every version is pinned in `gradle/libs.versions.toml`.
- Never proceed while verify.sh is red: fix that one failure, or roll back to the last green
  commit and re-approach.
- Never cross a phase boundary without human approval.

## Model policy

Fable-class models for architecture, review, and audits; Sonnet-class for mechanical
implementation; `scripts/verify.sh` is the judge either way. Git history is the evidence humans
read.

## The loop

plan → failing test → make it pass → refactor → ADR if the decision is hard to reverse →
self-review the diff → one atomic commit → tick `docs/PROGRESS.md` → stop. One commit per
`/next-commit` invocation. The loop never pushes: commits stay local until the human reviews the
diff and pushes — a push is the per-commit approval, and gives every commit its own CI verdict.
`docs/PROGRESS.md` is the single authority on the plan: the loop may tick checkboxes and fix
typos there, but semantic ladder changes are a human act at a phase gate.

Philosophy: git history is the architectural narrative — a reviewer should be able to
reconstruct every major decision from commit subjects and bodies alone.
