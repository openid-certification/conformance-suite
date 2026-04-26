# coolicons (vendored)

The conformance suite's icon library: a single SVG sprite containing 442 line icons, served from `/vendor/coolicons/coolicons.svg`. All consumers reach this via the `<cts-icon>` Lit primitive — see `src/main/resources/static/components/cts-icon.js`.

## Source

- **Project:** [coolicons](https://github.com/krystonschwarze/coolicons) by Kryston Schwarze
- **Version:** v4.1
- **License:** MIT
- **Vendored asset:** `Sprite/coolicons-sprite.svg` from the upstream `coolicons.v4.1.zip` archive, renamed to `coolicons.svg`. Committed unmodified — no minification, no symbol-name rewriting.
- **Upstream digest:** `sha256:181a365d707a4df4b85e67ddb2cce0ea0d1b0badfd7ee34c67f9cf1482abdb7b`

A canonical `LICENSE` file from the upstream project should be committed alongside the sprite. Tracked separately; not yet present.

## Sprite shape

Every symbol in the sprite uses:
- `viewBox="0 0 24 24"`
- `fill="none"` on the symbol root
- `stroke="currentColor"` on every path
- `stroke-width="2"`, `stroke-linecap="round"`, `stroke-linejoin="round"`

This is what makes `currentColor` theming and stroke-based sizing work uniformly across the set.

## Naming convention

The sprite ships symbol IDs in `PascalCase_Underscore` (e.g., `External_Link`, `Info_Circle`, `Arrow_Down_Left_LG`). The `<cts-icon>` public API uses lowercase kebab-case (e.g., `external-link`, `info-circle`, `arrow-down-left-lg`) and resolves to the sprite ID via a collapsed-key lookup:

```
"external-link"        →  key: "externallink"        →  symbol: "External_Link"
"arrow-down-left-lg"   →  key: "arrowdownleftlg"     →  symbol: "Arrow_Down_Left_LG"
"info-circle"          →  key: "infocircle"          →  symbol: "Info_Circle"
```

The collapsed-key approach handles the all-caps suffixes (`_LG`, `_MD`, `_SM`, `_XL`) and numeric suffixes (`_01`, `_02`) that defeat naive `kebab → Pascal_Case` transforms.

## Discovering icon names

Open Storybook and visit **`Primitives/cts-icon → AllIcons`** for a browsable grid of all 442 icons with their kebab-case names. Copy a caption verbatim to use as the `name` attribute.

## Refresh procedure

When upstream releases a new version:

1. Download the new `coolicons.vX.Y.zip` to the repo root.
2. Extract just the sprite:
   ```bash
   unzip -j -o coolicons.vX.Y.zip "Sprite/coolicons-sprite.svg" \
     -d src/main/resources/static/vendor/coolicons/ && \
     mv src/main/resources/static/vendor/coolicons/coolicons-sprite.svg \
        src/main/resources/static/vendor/coolicons/coolicons.svg
   ```
3. Regenerate the `SYMBOL_BY_KEY` manifest in `src/main/resources/static/components/cts-icon.js`:
   ```bash
   grep -oE 'id="[^"]+"' src/main/resources/static/vendor/coolicons/coolicons.svg \
     | sed 's/id="//;s/"$//' \
     | grep -v "^Vector$" \
     | grep -v " " \
     | sort -u \
     | awk '{ key=tolower($0); gsub("_","",key); printf "  %s: \"%s\",\n", key, $0 }'
   ```
   Replace the body of the `SYMBOL_BY_KEY` const with the output. Manual review the diff for any new collisions (two upstream IDs collapsing to the same key) — the map literal will silently drop the second key, which is the failure mode to catch by eye.
4. Update this README's version, license, and digest fields.
5. Open the AllIcons Storybook story to spot-check that previously-used icons still render. If a symbol was renamed or removed upstream, reconcile call sites.
6. Delete the zip — it is git-ignored (`*.zip` in `.gitignore`); do not commit it.

This is intentionally a manual procedure, not a build script. Sprite bumps are rare, and a script would add tooling without saving meaningful effort.
