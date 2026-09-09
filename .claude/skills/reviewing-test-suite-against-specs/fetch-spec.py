#!/usr/bin/env python3
"""Download specifications as greppable plain text and inspect their section numbering.

Python 3 stdlib only. Writes into the current working directory (or --out).

Usage:
  fetch-spec.py fetch NAME URL [NAME URL ...]        # download + convert, write NAME.txt
  fetch-spec.py headings FILE.txt                    # print numbered section headings (tab-separated)
  fetch-spec.py diff-headings A.txt B.txt            # show where numbering diverges
  fetch-spec.py map-tags PREFIX SRC_DIR SPEC.txt...  # every "PREFIX-x.y" tag in SRC_DIR -> heading per spec
  fetch-spec.py links URL [SUBSTRING]                # print the hrefs on a page (html_to_text drops them)

Examples:
  fetch-spec.py fetch ssf-final https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html \
                      rfc8935 https://www.rfc-editor.org/rfc/rfc8935.txt
  fetch-spec.py diff-headings caep-interop-id1.txt caep-interop-wg.txt

A 404 page converts to text like anything else, so check the HTTP status line
printed by `fetch` before trusting a file. On an HTTP error any stale NAME.txt from an
earlier run is deleted so a later diff-headings cannot silently compare against it.

The MUST/SHOULD counts are a sanity signal only (a 6-line "spec moved" page shows 0);
FAPI documents use lower-case "shall", which is counted too.
"""
import html
import os
import re
import sys
import urllib.error
import urllib.request

# Title must start with an uppercase letter: filters "2.0 roles map to..." and
# "60 minutes." fragments that begin a wrapped line.
HEADING_RE = re.compile(r"^(\d+(?:\.\d+)*)\.?\s+([A-Z\"'(].*)$")


def html_to_text(raw: str) -> str:
    s = re.sub(r"<script.*?</script>", "", raw, flags=re.S)
    s = re.sub(r"<style.*?</style>", "", s, flags=re.S)
    s = re.sub(r"<(br|/p|/div|/li|/h\d|/tr|/pre|/dd|/dt|/section)[^>]*>", "\n", s)
    s = re.sub(r"<[^>]+>", "", s)
    s = html.unescape(s)
    s = s.replace("¶", "")  # pilcrow anchors in rfc-style html
    s = re.sub(r"[ \t]+\n", "\n", s)
    s = re.sub(r"\n\s*\n+", "\n\n", s)
    return s.strip() + "\n"


def fetch(name: str, url: str, out_dir: str) -> None:
    req = urllib.request.Request(url, headers={"User-Agent": "fetch-spec/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            status = resp.status
            body = resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        stale = os.path.join(out_dir, f"{name}.txt")
        if os.path.exists(stale):
            os.remove(stale)
            print(f"{name}\tHTTP {e.code}\t{url}\t(not written; stale {stale} removed)")
        else:
            print(f"{name}\tHTTP {e.code}\t{url}\t(not written)")
        return
    text = body if url.endswith(".txt") else html_to_text(body)
    path = os.path.join(out_dir, f"{name}.txt")
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)
    musts = len(re.findall(r"\bMUST\b|\bSHALL\b|\bREQUIRED\b|\bshall\b", text))
    shoulds = len(re.findall(r"\bSHOULD\b|\bRECOMMENDED\b|\bshould\b", text))
    print(f"{name}\tHTTP {status}\t{len(text.splitlines())} lines\tMUST/SHALL/REQUIRED={musts}\tSHOULD/RECOMMENDED={shoulds}\t{path}")


def headings(path: str):
    out = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            # Indented numbered lines are list items ("   1. The JWT MUST ..."), not headings:
            # RFC text puts section headings at column 0 and the HTML converter strips indents.
            if line[:1].isspace():
                continue
            m = HEADING_RE.match(line.rstrip())
            if not m:
                continue
            num, title = m.group(1), m.group(2).strip()
            # Skip table-of-contents-looking lines with page numbers or dotted leaders
            if re.search(r"\.{3,}|\s\d+$", title):
                continue
            if len(title) > 90:
                continue
            out.append((num, title))
    # A heading may appear twice (ToC + body); keep the first occurrence per number+title
    seen, uniq = set(), []
    for num, title in out:
        key = (num, title.lower())
        if key in seen:
            continue
        seen.add(key)
        uniq.append((num, title))
    return uniq


def cmd_headings(path: str) -> None:
    for num, title in headings(path):
        print(f"{num}\t{title}")


def cmd_diff(a: str, b: str) -> None:
    ha = {num: title for num, title in headings(a)}
    hb = {num: title for num, title in headings(b)}
    nums = sorted(set(ha) | set(hb), key=lambda n: [int(x) for x in n.split(".")])
    print(f"{'section':<10}{os.path.basename(a):<45}{os.path.basename(b)}")
    diverged = 0
    for n in nums:
        ta, tb = ha.get(n, "—"), hb.get(n, "—")
        if ta.lower() != tb.lower():
            diverged += 1
            print(f"{n:<10}{ta[:43]:<45}{tb[:43]}")
    print(f"\n{diverged} section numbers differ in title or presence. "
          "Any requirement tag whose number is in this list may link to the wrong text.")


def cmd_map_tags(prefix: str, src_dir: str, specs) -> None:
    """Grep requirement tags like "OIDSSF-8.1.1" out of Java sources and show the heading each
    section number resolves to in every spec version given. A '—' means the section does not
    exist in that version: the log link is broken or points at the wrong text."""
    tag_re = re.compile(r'"' + re.escape(prefix) + r'-(\d+(?:\.\d+)*)"')
    counts: dict = {}
    for root, _dirs, files in os.walk(src_dir):
        for fn in files:
            if not fn.endswith(".java"):
                continue
            with open(os.path.join(root, fn), encoding="utf-8", errors="replace") as f:
                for m in tag_re.finditer(f.read()):
                    counts[m.group(1)] = counts.get(m.group(1), 0) + 1
    if not counts:
        print(f"no \"{prefix}-x.y\" tags under {src_dir}")
        return
    tables = [(os.path.basename(p), {n: t for n, t in headings(p)}) for p in specs]
    width = max(len(prefix) + 1 + len(n) for n in counts) + 2
    header = f"{'tag':<{width}}{'uses':>5}  " + "  ".join(f"{name[:34]:<36}" for name, _ in tables)
    print(header)
    missing = 0
    for num in sorted(counts, key=lambda n: [int(x) for x in n.split(".")]):
        cells = []
        for _name, table in tables:
            title = table.get(num)
            if title is None:
                missing += 1
            cells.append(f"{(title or '—')[:34]:<36}")
        print(f"{prefix + '-' + num:<{width}}{counts[num]:>5}  " + "  ".join(cells))
    print(f"\n{len(counts)} distinct tags; {missing} tag/version cells have no such section.")


def cmd_links(url: str, needle: str = "") -> None:
    """Print href targets and link text from a page; html_to_text discards them, which
    makes index pages such as openid.net/how-to-certify-your-implementation/ useless
    for finding the family's certification page."""
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 fetch-spec/1.0"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        body = resp.read().decode("utf-8", errors="replace")
    seen = set()
    for m in re.finditer(r'<a\s[^>]*href="([^"]+)"[^>]*>(.*?)</a>', body, flags=re.S | re.I):
        href, text = m.group(1), re.sub(r"<[^>]+>|\s+", " ", m.group(2)).strip()
        if needle and needle.lower() not in (href + " " + text).lower():
            continue
        if href in seen:
            continue
        seen.add(href)
        print(f"{href}\t{text[:80]}")


def main(argv) -> int:
    if len(argv) < 2 or argv[1] in ("-h", "--help"):
        print(__doc__)
        return 0
    cmd = argv[1]
    if cmd == "fetch":
        args = argv[2:]
        out_dir = "."
        if "--out" in args:
            i = args.index("--out")
            out_dir = args[i + 1]
            del args[i:i + 2]
        if len(args) % 2 or not args:
            print("fetch needs NAME URL pairs", file=sys.stderr)
            return 2
        os.makedirs(out_dir, exist_ok=True)
        for name, url in zip(args[::2], args[1::2]):
            fetch(name, url, out_dir)
        return 0
    if cmd == "headings" and len(argv) == 3:
        cmd_headings(argv[2])
        return 0
    if cmd == "diff-headings" and len(argv) == 4:
        cmd_diff(argv[2], argv[3])
        return 0
    if cmd == "map-tags" and len(argv) >= 5:
        cmd_map_tags(argv[2], argv[3], argv[4:])
        return 0
    if cmd == "links" and len(argv) in (3, 4):
        cmd_links(argv[2], argv[3] if len(argv) == 4 else "")
        return 0
    print(__doc__, file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
