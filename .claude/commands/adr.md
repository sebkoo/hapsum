---
description: Scaffold a new ADR from the template and index it.
argument-hint: <decision title>
---

Create the next ADR from `docs/adr/template.md`:

1. Next number = highest existing `docs/adr/NNNN-*.md` + 1, zero-padded to four digits.
2. File: `docs/adr/NNNN-<kebab-case-title>.md`. Status starts as Proposed.
3. Fill Context / Decision / Consequences / Alternatives considered from the conversation so
   far; leave honest TODOs where the decision is still open.
4. Add its row to the `docs/adr/README.md` index (number · title · status · implemented-by: —).
5. Stop — do not commit. The ADR lands with the commit that implements it.
