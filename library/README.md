# Specification library

Reference copies of the specifications the conformance suite tests against, for people and
LLM tooling to grep. **The published URL is authoritative**; these files exist so a requirement
tag such as `OID4VP-1FINAL-8.3` can be resolved to text without fetching anything.

## specs/

`specs/manifest.json` maps every prefix in `LogEntryHelper.specLinks` to a document here or to
the reason it is not included. Each document has a `linked` version (what `LogEntryHelper`
links to; `LogEntryHelper_UnitTest` fails if the two drift) and, where a successor is being
drafted, a `latest` version (a dated snapshot). `LogEntryHelper_UnitTest` hashes every stored
file, `linked` and `latest` alike; it is only the *freshness* of `latest` that is unenforced —
nothing checks whether a newer draft has appeared upstream.

All texts are numbered plain text: find a section with `grep -n '^8\.3\.  ' <file>`.

- `specs/ietf/`: RFCs and Internet-Drafts, byte-for-byte from rfc-editor.org / ietf.org.
- `specs/openid/`: OpenID Foundation specifications. Where openid.net publishes a `.txt` it is
  stored as-is; otherwise the published XML source was rendered with
  `xml2rfc --text --no-pagination` (network access needed: the sources include their bibliography
  from xml2rfc.ietf.org), so the text is the suite's rendering of OIDF's source, not
  an OIDF artefact. Because the bibliography is resolved at render time, the References section
  of such a text names the IETF draft revision current on the render date, not the revision the
  published document cites; read pinned draft revisions from the document's own "pre-final"
  clause or from the published HTML, not from these References. The manifest records the
  source URL, its sha256, the XML `docName` and the xml2rfc version.

First import covers IETF and OIDF only. Third-party ecosystem profiles are listed under
`excluded` until redistribution rights are confirmed.

## Manifest fields

- `documents.<id>.prefixes`: the `LogEntryHelper.specLinks` prefixes this document answers for.
- `documents.<id>.link_url`: the URL (fragment stripped) `LogEntryHelper` links to; must match
  every one of those prefixes' URLs.
- `documents.<id>.versions[]`:
  - `role`: `linked` (exactly one per document, required) or `latest`.
  - `file`: path under `specs/`, e.g. `ietf/rfc6749.txt`.
  - `source_url`: where `sync` fetches the source from.
  - `source_format`: `txt` (stored as fetched, no rendering), `zip-xml`, `xml`, `md`, or
    `github-md` (a `.tar.gz` of a repository at a pinned commit; needs `path`, below).
  - `sha256`: hash of the stored file; filled in by `sync`.
  - `fetched`: the date `sync` last wrote the file.
  - For rendered versions (every format except `txt`), `sync` also records `source_sha256`
    (hash of the fetched source, before rendering), `doc_name` (the XML `docName`, for humans;
    not compared against anything) and `generator` (the `xml2rfc --version` string).
  - `published_url` (optional): the page to cross-check rendered headings against, when it
    differs from `link_url`. For a `github-md` snapshot this is the working-group draft page,
    which moves with every merge while `source_url` pins one commit: the cross-check holds only
    while the page is built from that commit, so re-rendering an old snapshot fails with a
    heading mismatch. Refresh to the commit the page currently shows instead.
  - `path` (required for `github-md` only): the markdown file's path inside the tarball's
    top-level directory.
- `excluded.<prefix>`: either a reason string, or `{"private": "<dir>"}` for ISO documents kept
  outside the public repository.

## ISO documents

ISO/IEC texts cannot be redistributed publicly. People with access to the
`conformance-suite-private` repository will find them in `../conformance-suite-private/library/iso/`
(the manifest's `{"private": ...}` entries give the directory).

## profiles/

Ecosystem profile documents that have no stable public URL and are linked from
`LogEntryHelper` directly.

## Updating

`sync` needs Python 3.12 or later (it extracts tarballs with `filter="data"`) and, for
rendered formats, xml2rfc and mmark on `PATH`:

    nix --extra-experimental-features 'nix-command flakes' shell nixpkgs#xml2rfc nixpkgs#mmark
    python3 scripts/spec_library.py check                # offline consistency check (superset of the unit test)
    python3 scripts/spec_library.py sync                 # fetch whatever the manifest lists and the tree lacks
    python3 scripts/spec_library.py sync --refresh DOC          # re-fetch DOC's `latest` snapshot
    python3 scripts/spec_library.py sync --refresh-linked DOC   # replace the `linked` text (LogEntryHelper moved to a new version)

When a `specLinks` entry is added or its URL changes, add or update the manifest entry
(`python3 scripts/spec_library.py seed` prints what a fresh manifest would contain), run
`sync`, and commit the manifest and text together.

After refreshing a `latest` whose file name changes (sha-named wg-draft snapshots), `git rm`
the superseded file yourself; `check` lists files under `specs/ietf` or `specs/openid` that no
manifest version references as orphans.

`LogEntryHelper_UnitTest` is the CI gate: prefix coverage, `link_url` drift, a `linked` version
per document, and every file present with a matching sha256. `check` runs the same rules in
milliseconds and additionally rejects orphan files, unknown `role` values, a `file` shared by two
versions, and `file` paths outside `ietf/` or `openid/`.
