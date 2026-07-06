# CLAUDE.md

@AGENTS.md

Canonical guidance lives in AGENTS.md (harness-neutral, imported above).
Nested AGENTS.md files carry directory-specific conventions; each has a
sibling one-line CLAUDE.md shim like this file so Claude Code loads it on
demand. Do not add substantive guidance here — put it in the owning
AGENTS.md per the conventions map.

Claude Code specifics:

- Project skills resolve through `.claude/skills/` (committed symlinks into
  `.agents/skills/`, the canonical home).
- Approve the project-settings hooks prompt once so
  `frontend/scripts/agent-edit-check.sh` runs on `static/` edits.
