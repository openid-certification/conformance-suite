#!/usr/bin/env python3
"""Maintain library/specs: the text of the specifications LogEntryHelper.specLinks references.

Rendering needs xml2rfc (and mmark for md sources) on PATH:
  nix --extra-experimental-features 'nix-command flakes' shell nixpkgs#xml2rfc nixpkgs#mmark
"""
import argparse
import datetime
import functools
import glob
import hashlib
import html
import io
import json
import os
import pathlib
import re
import subprocess
import sys
import tarfile
import tempfile
import urllib.error
import urllib.request
import zipfile

NIX_SHELL_CMD = "nix --extra-experimental-features 'nix-command flakes' shell nixpkgs#xml2rfc nixpkgs#mmark"
ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
LOG_ENTRY_HELPER = os.path.join(ROOT, "src/main/java/net/openid/conformance/export/LogEntryHelper.java")
SPECS_DIR = os.path.join(ROOT, "library/specs")
MANIFEST = os.path.join(SPECS_DIR, "manifest.json")
XML2RFC = ["xml2rfc", "--text", "--no-pagination", "-q"]
# openid.net source siblings, in order of preference.
OPENID_SOURCE_FORMATS = (("txt", ".txt"), ("zip-xml", ".zip"), ("xml", ".xml"), ("md", ".md"))

PUT_RE = re.compile(r'specLinks\.put\("([^"]+)",\s*"([^"]+)"\)')
RFC_RE = re.compile(r"/(rfc\d+)(?:\.html)?$")
BCP_RE = re.compile(r"/(bcp\d+)$")
DRAFT_RE = re.compile(r"/(draft-[a-z0-9-]+-\d\d)(?:\.html)?$")
# "5.8.  Title", "Appendix A.  Title" and "A.1.  Title"; the section id is group 1.
TEXT_HEADING_RE = re.compile(r"^(?:Appendix )?((?:\d+|[A-Z])(?:\.\d+)*)\.\s+(\S.*)$")
HTML_HEADING_RE = re.compile(r"<h[1-6][^>]*>(.*?)</h[1-6]>", re.S)
THIRD_PARTY = "third-party specification; redistribution rights not confirmed (#1957 first import is IETF+OIDF only)"


def parse_spec_links(java_src):
    return dict(PUT_RE.findall(java_src))


def link_url(url):
    return url.split("#", 1)[0]


def ietf_source(url):
    base = link_url(url)
    m = RFC_RE.search(base)
    if m:
        return m.group(1), f"https://www.rfc-editor.org/rfc/{m.group(1)}.txt"
    m = BCP_RE.search(base)
    if m:
        return m.group(1), f"https://www.rfc-editor.org/bcp/{m.group(1)}.txt"
    m = DRAFT_RE.search(base)
    if m:
        return m.group(1), f"https://www.ietf.org/archive/id/{m.group(1)}.txt"
    raise ValueError(f"not an IETF document URL: {url}")


def classify(url):
    base = link_url(url)
    host = re.sub(r"^https?://([^/]+).*$", r"\1", base)
    if host in ("tools.ietf.org", "www.rfc-editor.org", "www.ietf.org", "datatracker.ietf.org"):
        return {"kind": "ietf"}
    if host == "openid.net" and base.endswith(".html"):
        return {"kind": "openid"}
    if host == "www.iso.org":
        return {"kind": "iso"}
    if "/issues/" in base:
        return {"kind": "excluded", "reason": "issue tracker, not a specification"}
    if host == "bitbucket.org" and "/openid/obuk/" in base:
        return {"kind": "excluded", "reason": "UK Open Banking security profile source on bitbucket.org, not an "
                                               "IETF/OIDF published specification page; out of scope for the "
                                               "first import"}
    if host in ("openid.bitbucket.io", "openid.github.io", "bitbucket.org"):
        return {"kind": "excluded", "reason": "OIDF working copy at a mutable URL; no published source sibling"}
    return {"kind": "excluded", "reason": THIRD_PARTY}


def _norm_title(s):
    return re.sub(r"\s+", " ", html.unescape(s)).strip()


def text_headings(text):
    """Numbered headings that start in column 0; table-of-contents and list lines are indented.

    xml2rfc wraps a long title onto indented continuation lines, which end at the first blank line.
    """
    out = {}
    lines = text.split("\n")
    for i, line in enumerate(lines):
        m = TEXT_HEADING_RE.match(line)
        if not m:
            continue
        title = m.group(2)
        for cont in lines[i + 1:]:
            if not cont.strip() or not cont[:1].isspace():
                break
            title += " " + cont.strip()
        out[m.group(1)] = _norm_title(title)
    return out


def html_headings(page):
    out = {}
    for inner in HTML_HEADING_RE.findall(page):
        m = TEXT_HEADING_RE.match(_norm_title(re.sub(r"<[^>]+>", "", inner)))
        if m:
            out[m.group(1)] = m.group(2)
    return out


def _section_key(n):
    """Numeric sections first, then appendices: 2 < 10 < 10.1 < A < A.1 < B."""
    return [(0, int(x)) if x.isdigit() else (1, ord(x)) for x in n.split(".")]


def _show(title):
    return "missing" if title is None else repr(title)


def diff_headings(rendered, published):
    return [f"{n}: {_show(rendered.get(n))} != {_show(published.get(n))}"
            for n in sorted(set(rendered) | set(published), key=_section_key)
            if rendered.get(n) != published.get(n)]


# One file name directly under ietf/ or openid/: rules out absolute paths and ".." parts as well.
FILE_PATH_RE = re.compile(r"^(?:ietf|openid)/[^/]+\.txt$")


def validate_manifest(manifest, spec_links):
    errors = []
    seen = {}
    files = {}
    for doc_id, doc in manifest.get("documents", {}).items():
        for p in doc.get("prefixes", []):
            seen.setdefault(p, []).append(doc_id)
        linked = sum(1 for v in doc.get("versions", []) if v.get("role") == "linked")
        if linked != 1:
            errors.append(f"{doc_id}: {linked} versions with role 'linked', need exactly one")
        for v in doc.get("versions", []):
            if v.get("role") not in ("linked", "latest"):
                errors.append(f"{doc_id}: unknown role {v.get('role')!r}, must be 'linked' or 'latest'")
            f = v.get("file", "")
            if not FILE_PATH_RE.match(f):
                errors.append(f"{doc_id}: version file {f!r} must be a relative ietf/... or openid/... path")
            files.setdefault(f, []).append(doc_id)
    for f, owners in files.items():
        if len(owners) > 1:
            errors.append(f"{f}: used by more than one version ({', '.join(owners)})")
    for p in manifest.get("excluded", {}):
        seen.setdefault(p, []).append(None)
    for p, owners in seen.items():
        if len(owners) > 1:
            errors.append(f"{p}: listed more than once in manifest")
        if p not in spec_links:
            errors.append(f"{p}: in manifest but not in LogEntryHelper")
    for p, url in spec_links.items():
        if p not in seen:
            errors.append(f"{p}: not in manifest")
            continue
        doc_id = seen[p][0]
        if doc_id is not None:
            want = manifest["documents"][doc_id].get("link_url")
            if link_url(url) != want:
                errors.append(f"{p}: LogEntryHelper links {link_url(url)} but manifest document {doc_id} "
                              f"has link_url {want}")
    return errors


class HeadingMismatch(Exception):
    pass


def http_get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "conformance-suite-spec-library/1.0"})
    with urllib.request.urlopen(req, timeout=120) as resp:
        return resp.read()


def http_exists(url):
    req = urllib.request.Request(url, method="HEAD", headers={"User-Agent": "conformance-suite-spec-library/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return resp.status == 200 and "text/html" not in resp.headers.get("Content-Type", "")
    except urllib.error.HTTPError:
        return False


def _sha256(data):
    return hashlib.sha256(data).hexdigest()


def _xml_from_source(fmt, data, version, work, run):
    """Return the path of an xml2rfc input file for a non-txt source."""
    if fmt == "xml":
        path = os.path.join(work, "in.xml")
        pathlib.Path(path).write_bytes(data)
        return path
    if fmt == "zip-xml":
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            z.extractall(work)
        xmls = glob.glob(os.path.join(work, "*.xml"))
        if len(xmls) != 1:
            raise ValueError(f"expected exactly one top-level .xml in {version['source_url']}, found {xmls}")
        return xmls[0]
    if fmt == "md":
        md = os.path.join(work, "in.md")
        pathlib.Path(md).write_bytes(data)
    elif fmt == "github-md":
        with tarfile.open(fileobj=io.BytesIO(data), mode="r:gz") as t:
            t.extractall(work, filter="data")
        dirs = glob.glob(os.path.join(work, "*/"))
        if not dirs:
            raise ValueError(f"{version.get('source_url', '?')}: empty tarball, expected a top-level directory")
        if "path" not in version:
            raise ValueError(f"{version.get('file', '?')}: github-md source needs a 'path' key naming the "
                              f"markdown file inside the tarball")
        md = os.path.join(dirs[0], version["path"])
    else:
        raise ValueError(f"unknown source_format {fmt}")
    path = os.path.join(work, "in.xml")
    res = run(["mmark", md], capture_output=True, text=True, check=True, cwd=os.path.dirname(md))
    pathlib.Path(path).write_text(res.stdout, encoding="utf-8")
    return path


@functools.lru_cache
def _xml2rfc_version(run):
    return run(["xml2rfc", "--version"], capture_output=True, text=True).stdout.strip()


def render_version(version, link, dest, cache_dir=None, fetch=http_get, run=subprocess.run):
    """cache_dir is xml2rfc's XDG_CACHE_HOME for this run; the bibliography xi:includes are fetched once per
    run and never survive it, so a refresh always behaves like a first run."""
    fmt = version["source_format"]
    data = fetch(version["source_url"])
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    if fmt == "txt":
        out = data
        # A soft 404 or interstitial HTML page served at a .txt URL has no numbered headings.
        if not text_headings(out.decode("utf-8", "replace")):
            raise HeadingMismatch(f"{version['file']} fetched from {version['source_url']}: "
                                  f"text has no numbered headings")
    else:
        with tempfile.TemporaryDirectory() as work:
            xml = _xml_from_source(fmt, data, version, work, run)
            m = re.search(r'docName="([^"]+)"', pathlib.Path(xml).read_text(encoding="utf-8"))
            rendered = os.path.join(work, "out.txt")
            env = dict(os.environ, XDG_CACHE_HOME=cache_dir or os.path.join(work, "cache"))
            run(XML2RFC + ["-o", rendered, xml], capture_output=True, text=True, check=True, env=env)
            out = pathlib.Path(rendered).read_bytes()
        rendered_headings = text_headings(out.decode("utf-8"))
        if not rendered_headings:
            raise HeadingMismatch(f"{version['file']} rendered from {version['source_url']}: "
                                   f"rendered text has no numbered headings")
        diffs = diff_headings(rendered_headings, html_headings(fetch(link).decode("utf-8", "replace")))
        if diffs:
            raise HeadingMismatch(f"{version['file']} rendered from {version['source_url']} does not match "
                                  f"{link}:\n  " + "\n  ".join(diffs[:10]))
        version["source_sha256"] = _sha256(data)
        version["doc_name"] = m.group(1) if m else ""
        version["generator"] = _xml2rfc_version(run)
    pathlib.Path(dest).write_bytes(out)
    version["sha256"] = _sha256(out)
    version["fetched"] = datetime.date.today().isoformat()


def load_links():
    return parse_spec_links(pathlib.Path(LOG_ENTRY_HELPER).read_text(encoding="utf-8"))


def cmd_seed(links=None, exists=http_exists, out=None):
    if links is None:
        links = load_links()
    if out is None:
        out = sys.stdout
    docs, excluded = {}, {}
    for prefix, url in sorted(links.items()):
        c = classify(url)
        if c["kind"] == "excluded":
            excluded[prefix] = c["reason"]
        elif c["kind"] == "iso":
            n = re.sub(r"^ISO(\d+-\d+)-$", r"\1", prefix)
            excluded[prefix] = {"private": f"library/iso/{n}/"}
        elif c["kind"] == "ietf":
            doc_id, src = ietf_source(url)
            d = docs.setdefault(doc_id, {"prefixes": [], "link_url": link_url(url), "versions": [
                {"role": "linked", "file": f"ietf/{doc_id}.txt", "source_url": src, "source_format": "txt"}]})
            d["prefixes"].append(prefix)
        else:
            base = link_url(url)[:-len(".html")]
            doc_id = base.rsplit("/", 1)[1]
            if doc_id not in docs:
                fmt, ext = next(((f, e) for f, e in OPENID_SOURCE_FORMATS if exists(base + e)), (None, None))
                if fmt is None:
                    excluded[prefix] = "openid.net publishes no .txt/.zip/.xml/.md sibling for this document"
                    continue
                docs[doc_id] = {"prefixes": [], "link_url": link_url(url), "versions": [
                    {"role": "linked", "file": f"openid/{doc_id}.txt", "source_url": base + ext, "source_format": fmt}]}
            docs[doc_id]["prefixes"].append(prefix)
    json.dump({"documents": docs, "excluded": excluded}, out, indent=2, sort_keys=True)
    out.write("\n")


def cmd_sync(only=None, refresh=None, refresh_linked=None, manifest_path=MANIFEST, specs_dir=SPECS_DIR,
             render=render_version):
    """Fetch what the tree lacks. --refresh DOC replaces DOC's `latest` versions only;
    a `linked` version is replaced only by --refresh-linked DOC."""
    manifest = json.loads(pathlib.Path(manifest_path).read_text(encoding="utf-8"))
    for flag, doc_id in (("--only", only), ("--refresh", refresh), ("--refresh-linked", refresh_linked)):
        if doc_id is not None and doc_id not in manifest["documents"]:
            print(f"ERROR {flag} {doc_id}: not a document in {manifest_path}")
            return 2
    failures = 0
    refresh_flags = {"--refresh": refresh, "--refresh-linked": refresh_linked}
    refreshed = set()
    with tempfile.TemporaryDirectory(prefix="xml2rfc-cache-") as cache:
        try:
            for doc_id, doc in sorted(manifest["documents"].items()):
                if only and doc_id != only:
                    continue
                for v in doc["versions"]:
                    dest = os.path.join(specs_dir, v["file"])
                    flag = "--refresh-linked" if v["role"] == "linked" else "--refresh"
                    replace = doc_id == refresh_flags[flag]
                    if replace:
                        refreshed.add(flag)
                    elif "sha256" in v:
                        # A version with a recorded hash is verified, never re-rendered; one without (a missing
                        # file, or one an interrupted run left behind) is rendered.
                        if not os.path.exists(dest):
                            print(f"ERROR {doc_id}\t{v['role']}\t{v['file']} is missing but has a recorded hash; "
                                  f"restore it or pass {flag} {doc_id}")
                            failures += 1
                        elif _sha256(pathlib.Path(dest).read_bytes()) != v["sha256"]:
                            print(f"ERROR {doc_id}\t{v['role']}\t{v['file']} does not match the sha256 "
                                  f"in the manifest")
                            failures += 1
                        continue
                    try:
                        render(v, v.get("published_url", doc["link_url"]), dest, cache_dir=cache)
                        print(f"ok    {doc_id}\t{v['role']}\t{v['file']}\t{v.get('doc_name', '')}")
                    except FileNotFoundError as e:
                        # A missing tool arrives with its bare name; a wrong `path` or `cwd` arrives as a path.
                        if e.filename in (XML2RFC[0], "mmark"):
                            print(f"ERROR {doc_id}\t{v['role']}\t{e.filename} not found on PATH; install it with: "
                                  f"{NIX_SHELL_CMD}")
                        else:
                            print(f"ERROR {doc_id}\t{v['role']}\tFileNotFoundError: {e}")
                        failures += 1
                    except subprocess.CalledProcessError as e:
                        # The temp dir is gone by now; the tool's stderr is all there is to diagnose from.
                        tail = "\n  ".join((e.stderr or "").strip().splitlines()[-8:])
                        print(f"ERROR {doc_id}\t{v['role']}\t{e}\n  {tail}")
                        failures += 1
                    except Exception as e:
                        print(f"ERROR {doc_id}\t{v['role']}\t{type(e).__name__}: {e}")
                        failures += 1
        finally:
            with open(manifest_path, "w", encoding="utf-8") as f:
                json.dump(manifest, f, indent=2, sort_keys=True)
                f.write("\n")
    for flag, doc_id in refresh_flags.items():
        if doc_id is not None and flag not in refreshed:
            role = "linked" if flag == "--refresh-linked" else "latest"
            why = f"skipped by --only {only}" if only and only != doc_id else f"it has no {role} version"
            print(f"ERROR {flag} {doc_id}: nothing was refreshed; {why}")
            failures += 1
    return 1 if failures else 0


def cmd_check(manifest_path=MANIFEST, specs_dir=SPECS_DIR, links=None):
    if links is None:
        links = load_links()
    manifest = json.loads(pathlib.Path(manifest_path).read_text(encoding="utf-8"))
    errors = validate_manifest(manifest, links)
    referenced = set()
    for doc_id, doc in manifest["documents"].items():
        for v in doc["versions"]:
            referenced.add(v["file"])
            path = os.path.join(specs_dir, v["file"])
            if not os.path.exists(path):
                errors.append(f"{doc_id}: {v['file']} missing")
            elif _sha256(pathlib.Path(path).read_bytes()) != v.get("sha256"):
                errors.append(f"{doc_id}: {v['file']} sha256 differs from manifest")
    for sub in ("ietf", "openid"):
        for path in sorted(glob.glob(os.path.join(specs_dir, sub, "*"))):
            rel = f"{sub}/{os.path.basename(path)}"
            if rel not in referenced:
                errors.append(f"{rel}: orphan, not referenced by any manifest version")
    print("\n".join(errors) if errors else
          f"ok: {len(manifest['documents'])} documents, {len(manifest['excluded'])} excluded prefixes")
    return 1 if errors else 0


def build_parser():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("seed", help="print a manifest skeleton derived from LogEntryHelper.java (probes openid.net)")
    sync = sub.add_parser(
        "sync", help="fetch/render every manifest version whose file is missing and fill in its hashes",
        description="Fetch/render every manifest version whose file is missing and fill in its hashes. An existing "
                    "file whose sha256 differs is an error. A `latest` file name can change (sha-named wg-draft "
                    "snapshots): after --refresh, `git rm` the superseded file yourself; `check` lists it as an "
                    "orphan.")
    sync.add_argument("--only", metavar="DOC", help="process this manifest document only")
    sync.add_argument("--refresh", metavar="DOC", help="re-fetch DOC's `latest` versions")
    sync.add_argument("--refresh-linked", metavar="DOC", help="re-fetch DOC's `linked` version")
    sub.add_parser("check", help="offline: manifest vs LogEntryHelper.java, files exist, sha256 match, and orphan "
                                 "files under library/specs/{ietf,openid} no version references")
    return parser


def main(argv):
    args = build_parser().parse_args(argv[1:])
    if args.command == "seed":
        cmd_seed()
        return 0
    if args.command == "sync":
        return cmd_sync(only=args.only, refresh=args.refresh, refresh_linked=args.refresh_linked)
    return cmd_check()


if __name__ == "__main__":
    sys.exit(main(sys.argv))
