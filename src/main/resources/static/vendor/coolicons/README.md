# coolicons (vendored)

The conformance suite's icon library: per-icon SVG files in `icons/`, each ~3-5 KB, served from `/vendor/coolicons/icons/{name}.svg`. All consumers reach these via the `<cts-icon>` Lit primitive — see `src/main/resources/static/components/cts-icon.js`.

Curated set, not full library. Only the icons actually used in the app live here. Adding a new icon means extracting one more file from upstream (see "Adding a new icon" below).

## Source

- **Project:** [coolicons](https://github.com/krystonschwarze/coolicons) by Kryston Schwarze
- **Version:** v4.1
- **License:** MIT
- **Distribution shape:** per-icon SVGs, derived from the upstream `Sprite/coolicons-sprite.svg`. Each upstream `<symbol id="External_Link">` becomes a standalone `external-link.svg` with `<svg id="i" viewBox="0 0 24 24"><path stroke="currentColor".../></svg>`.

A canonical `LICENSE` file from the upstream project should be committed alongside the icons. Tracked separately; not yet present.

## Why per-icon files

- **HTTP/2 makes per-resource overhead negligible.** A page that uses 8 icons fetches 8 small files in parallel, total ~25 KB. Cheaper than shipping a 110 KB gzipped sprite of which 95% is unused symbols.
- **Cache locality matches usage.** Browsers cache per-URL; only the icons actually referenced ever land in cache.
- **Zero runtime indirection.** No sprite, no manifest, no fetch+inline JS, no symbol-id translation. The public `<cts-icon name>` is the filename, full stop.

## Icon shape

Every per-icon file uses:
- `id="i"` on the root `<svg>` (so `<use href="...svg#i">` works reliably across browsers)
- `viewBox="0 0 24 24"`
- `fill="none"` on the root
- `stroke="currentColor"` on every path (so `currentColor` theming flows from the consumer's text colour)
- `stroke-width="2"`, `stroke-linecap="round"`, `stroke-linejoin="round"`

## Discovering icon names

Open Storybook and visit **`Primitives/cts-icon → AllIcons`** for a browsable grid of every icon currently vendored, with its kebab-case name. Copy the name verbatim to use as the `name` attribute.

If you need the list outside Storybook:

```bash
ls src/main/resources/static/vendor/coolicons/icons/ | sed 's/\.svg$//'
```

## Adding a new icon

When a call site needs an icon that isn't already vendored:

1. Download `coolicons.v4.1.zip` from upstream (or restore it locally — it's git-ignored via `*.zip`).
2. Find the upstream PascalCase_Underscore symbol id you want by browsing:
   ```bash
   unzip -p coolicons.v4.1.zip "Sprite/coolicons-sprite_demo.html" > /tmp/coolicons-demo.html && open /tmp/coolicons-demo.html
   ```
3. Extract that one symbol into a per-icon file. From the repo root:
   ```bash
   python3 - <<'PY'
   import re, zipfile
   wanted = "your-icon-name"  # the kebab-case public name (e.g. "calendar-add")
   with zipfile.ZipFile("coolicons.v4.1.zip") as z:
       sprite = z.read("Sprite/coolicons-sprite.svg").decode()
   pat = re.compile(r'<symbol\s+([^>]*?)id="([^"]+)"([^>]*?)>(.*?)</symbol>', re.DOTALL)
   for m in pat.finditer(sprite):
       if m.group(2).replace("_", "-").lower() != wanted:
           continue
       inner = re.sub(r'<g\s+id="[^"]*">\s*', '', m.group(4).strip())
       inner = re.sub(r"\s*</g>\s*$", "", inner).strip()
       inner = re.sub(r'\s*id="Vector"', '', inner)
       svg = f'<svg xmlns="http://www.w3.org/2000/svg" id="i" fill="none" viewBox="0 0 24 24">{inner}</svg>\n'
       out = f"src/main/resources/static/vendor/coolicons/icons/{wanted}.svg"
       open(out, "w").write(svg)
       print(f"wrote {out}")
       break
   else:
       print(f"NOT FOUND in upstream sprite: {wanted}")
   PY
   ```
4. Verify the new file renders — open it in Storybook's AllIcons story or in your call-site page.
5. Commit just the new SVG file alongside your call-site change. Don't commit the zip.

The extraction is a per-icon one-liner, not a build step — no script in the repo, no Maven plugin, no Node tooling.

## Refresh procedure (upstream version bump)

When upstream releases a new version:

1. Download the new `coolicons.vX.Y.zip` to the repo root.
2. Capture the current set of icons: `ls src/main/resources/static/vendor/coolicons/icons/ > /tmp/icon-names.txt`.
3. Delete the existing per-icon files: `rm src/main/resources/static/vendor/coolicons/icons/*.svg`.
4. Re-run the extraction in a loop over `/tmp/icon-names.txt` (use the snippet above, swapping `wanted` for each filename).
5. Diff the regenerated icons against the old ones — coolicons sometimes renames or restyles symbols between versions. Reconcile any call sites that broke.
6. Update this README's version field.
7. Delete the zip — it is git-ignored (`*.zip` in `.gitignore`); do not commit it.
