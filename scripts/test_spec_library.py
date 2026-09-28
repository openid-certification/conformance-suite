import contextlib
import hashlib
import io
import json
import os
import pathlib
import subprocess
import tarfile
import tempfile
import unittest
import zipfile

import spec_library as sl

JAVA = '''
        specLinks.put("RFC6749-", "https://tools.ietf.org/html/rfc6749#section-");
        specLinks.put("RFC6749A-", "https://tools.ietf.org/html/rfc6749#appendix-");
        specLinks.put("PAR-", "https://www.rfc-editor.org/rfc/rfc9126.html#section-");
        specLinks.put("BCP195-", "https://tools.ietf.org/html/bcp195#section-");
        specLinks.put("SDJWTVC-", "https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-13.html#section-");
        specLinks.put("OTSL-", "https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-15#section-");
        specLinks.put("OIDCC-", "https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.");
        specLinks.put("ISO18013-5-", "https://www.iso.org/standard/69084.html#");
        specLinks.put("CDR-", "https://consumerdatastandardsaustralia.github.io/standards/#");
        specLinks.put("OpenID4VCI-", "https://github.com/openid/OpenID4VCI/issues/");
'''


class ParseTest(unittest.TestCase):
    def test_parses_every_put(self):
        links = sl.parse_spec_links(JAVA)
        self.assertEqual(10, len(links))
        self.assertEqual("https://tools.ietf.org/html/rfc6749#appendix-", links["RFC6749A-"])


class IetfSourceTest(unittest.TestCase):
    def test_rfc_from_tools_and_rfc_editor(self):
        self.assertEqual(("rfc6749", "https://www.rfc-editor.org/rfc/rfc6749.txt"),
                         sl.ietf_source("https://tools.ietf.org/html/rfc6749#section-"))
        self.assertEqual(("rfc9126", "https://www.rfc-editor.org/rfc/rfc9126.txt"),
                         sl.ietf_source("https://www.rfc-editor.org/rfc/rfc9126.html#section-"))

    def test_bcp(self):
        self.assertEqual(("bcp195", "https://www.rfc-editor.org/bcp/bcp195.txt"),
                         sl.ietf_source("https://tools.ietf.org/html/bcp195#section-"))

    def test_drafts_keep_their_revision(self):
        self.assertEqual(("draft-ietf-oauth-sd-jwt-vc-13",
                          "https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-13.txt"),
                         sl.ietf_source("https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-13.html#section-"))
        self.assertEqual(("draft-ietf-oauth-status-list-15",
                          "https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-15.txt"),
                         sl.ietf_source("https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-15#section-"))


class ClassifyTest(unittest.TestCase):
    def test_kinds(self):
        self.assertEqual("ietf", sl.classify("https://tools.ietf.org/html/rfc6749#section-")["kind"])
        self.assertEqual("openid", sl.classify("https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.")["kind"])
        self.assertEqual("iso", sl.classify("https://www.iso.org/standard/69084.html#")["kind"])
        self.assertEqual("excluded", sl.classify("https://consumerdatastandardsaustralia.github.io/standards/#")["kind"])
        self.assertEqual("excluded", sl.classify("https://github.com/openid/OpenID4VCI/issues/")["kind"])

    def test_ob_bitbucket_reason_names_the_real_source_not_a_mutable_url(self):
        c = sl.classify("https://bitbucket.org/openid/obuk/src/b36035c22e96ce160524066c7fde9a45cbaeb949/"
                                "uk-openbanking-security-profile.md?at=master&fileviewer=file-view-default#")
        self.assertEqual("excluded", c["kind"])
        self.assertEqual("UK Open Banking security profile source on bitbucket.org, not an IETF/OIDF published "
                          "specification page; out of scope for the first import", c["reason"])
        # a different bitbucket.org repo still gets the generic "mutable URL" reason
        other = sl.classify("https://bitbucket.org/openid/fapi/src/master/x.md#")
        self.assertEqual("OIDF working copy at a mutable URL; no published source sibling", other["reason"])


class HeadingsTest(unittest.TestCase):
    TEXT = ("Table of Contents\n\n   1.  Introduction\n     5.8.  aud of a Request Object\n\n"
            "1.  Introduction\n\n   body\n\n5.8.  aud of a Request Object\n\n   1.  a list item\n\n"
            "5.3.2.1.  General requirements\n")
    HTML = ('<h2 id="a"><a href="#section-1" class="section-number selfRef">1. </a>'
            '<a href="#name-introduction" class="section-name selfRef">Introduction</a></h2>'
            '<h3 id="b"><a href="#section-5.8" class="section-number selfRef">5.8. </a>'
            '<a class="section-name selfRef"><code>aud</code> of a Request Object</a></h3>'
            '<h5><a class="section-number selfRef">5.3.2.1. </a><a>General requirements</a></h5>')

    def test_text_headings_are_column_zero_only(self):
        self.assertEqual({"1": "Introduction", "5.8": "aud of a Request Object",
                          "5.3.2.1": "General requirements"}, sl.text_headings(self.TEXT))

    def test_html_headings_strip_markup(self):
        self.assertEqual(sl.text_headings(self.TEXT), sl.html_headings(self.HTML))

    def test_wrapped_text_heading_is_joined(self):
        text = ("6.1.1.  Issuer identification and key resolution to validate an issued\n"
                "        Credential\n\n   Body text that is indented\n   and wrapped.\n")
        self.assertEqual({"6.1.1": "Issuer identification and key resolution to validate an issued Credential"},
                         sl.text_headings(text))

    def test_appendices_are_extracted_from_text_and_html(self):
        text = "Appendix A.  OpenID4VP over the Digital Credentials API\n\nA.1.  Protocol\n\n   body\n"
        page = ('<h2><a class="section-number selfRef">Appendix A. </a><a>OpenID4VP over the Digital Credentials API</a></h2>'
                '<h3><a class="section-number selfRef">A.1. </a><a>Protocol</a></h3>')
        want = {"A": "OpenID4VP over the Digital Credentials API", "A.1": "Protocol"}
        self.assertEqual(want, sl.text_headings(text))
        self.assertEqual(want, sl.html_headings(page))

    def test_appendix_title_difference_is_reported_and_sorted_after_numbers(self):
        a = {"10": "Security", "2": "Terms", "A": "Examples", "A.1": "One"}
        b = {"10": "Security", "2": "Terms", "A": "IANA", "A.1": "One", "B": "History"}
        self.assertEqual(["A: 'Examples' != 'IANA'", "B: missing != 'History'"], sl.diff_headings(a, b))

    def test_diff_reports_renumbering(self):
        a = {"1": "Introduction", "2": "Terminology"}
        b = {"1": "Introduction", "2": "Scope", "3": "Terminology"}
        self.assertEqual(["2: 'Terminology' != 'Scope'", "3: missing != 'Terminology'"], sl.diff_headings(a, b))
        self.assertEqual([], sl.diff_headings(a, a))


class SeedTest(unittest.TestCase):
    def test_seed_generates_manifest_from_links(self):
        links = {"RFC6749-": "https://tools.ietf.org/html/rfc6749#section-",
                 "RFC6749A-": "https://tools.ietf.org/html/rfc6749#appendix-",
                 "OIDCC-": "https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.",
                 "HAIP-": "https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0.html#section-",
                 "HAIPA-": "https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0.html#appendix-",
                 "NOSRC-": "https://openid.net/specs/no-source-1_0.html#section-",
                 "ISO18013-5-": "https://www.iso.org/standard/69084.html#",
                 "CDR-": "https://consumerdatastandardsaustralia.github.io/standards/#",
                 "OpenID4VCI-": "https://github.com/openid/OpenID4VCI/issues/"}
        def exists(url):
            return url.endswith(("openid-connect-core-1_0.txt", "openid-connect-core-1_0.xml",
                                 "profile-1_0.zip", "profile-1_0.xml"))
        out = io.StringIO()
        sl.cmd_seed(links=links, exists=exists, out=out, print_only=True)
        manifest = json.loads(out.getvalue())

        # (a) RFC6749 has both prefixes, correct link_url, and linked version
        self.assertEqual(["RFC6749-", "RFC6749A-"], manifest["documents"]["rfc6749"]["prefixes"])
        self.assertEqual("https://tools.ietf.org/html/rfc6749", manifest["documents"]["rfc6749"]["link_url"])
        self.assertEqual(1, len(manifest["documents"]["rfc6749"]["versions"]))
        v = manifest["documents"]["rfc6749"]["versions"][0]
        self.assertEqual("linked", v["role"])
        self.assertEqual("ietf/rfc6749.txt", v["file"])
        self.assertEqual("https://www.rfc-editor.org/rfc/rfc6749.txt", v["source_url"])
        self.assertEqual("txt", v["source_format"])

        # (b) openid-connect-core-1_0 picks txt (first in preference order)
        self.assertEqual("txt", manifest["documents"]["openid-connect-core-1_0"]["versions"][0]["source_format"])
        self.assertTrue(manifest["documents"]["openid-connect-core-1_0"]["versions"][0]["source_url"].endswith(".txt"))
        self.assertEqual("openid/openid-connect-core-1_0.txt", manifest["documents"]["openid-connect-core-1_0"]["versions"][0]["file"])

        # (c) HAIP has both prefixes, zip-xml format
        self.assertEqual(["HAIP-", "HAIPA-"], sorted(manifest["documents"]["openid4vc-high-assurance-interoperability-profile-1_0"]["prefixes"]))
        self.assertEqual("zip-xml", manifest["documents"]["openid4vc-high-assurance-interoperability-profile-1_0"]["versions"][0]["source_format"])
        self.assertTrue(manifest["documents"]["openid4vc-high-assurance-interoperability-profile-1_0"]["versions"][0]["source_url"].endswith(".zip"))

        # (d) Excluded items have correct reasons
        self.assertEqual("openid.net publishes no .txt/.zip/.xml/.md sibling for this document", manifest["excluded"]["NOSRC-"])
        self.assertEqual({"private": "library/iso/18013-5/"}, manifest["excluded"]["ISO18013-5-"])
        self.assertEqual(sl.THIRD_PARTY, manifest["excluded"]["CDR-"])
        self.assertEqual("issue tracker, not a specification", manifest["excluded"]["OpenID4VCI-"])


    def seeded(self, links, manifest):
        """Run seed in place on a temporary copy of `manifest`; return (output, manifest after)."""
        with tempfile.TemporaryDirectory() as d:
            path = os.path.join(d, "manifest.json")
            pathlib.Path(path).write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")
            before = pathlib.Path(path).read_bytes()
            out = io.StringIO()
            sl.cmd_seed(links=links, exists=lambda url: False, out=out, manifest_path=path)
            after = pathlib.Path(path).read_bytes()
            return out.getvalue(), json.loads(after), before == after

    EXISTING = {"documents": {"rfc6749": {"prefixes": ["RFC6749-"],
                                          "link_url": "https://tools.ietf.org/html/rfc6749",
                                          "versions": [{"role": "linked", "file": "ietf/rfc6749.txt",
                                                        "source_url": "https://www.rfc-editor.org/rfc/rfc6749.txt",
                                                        "source_format": "txt", "sha256": "abc", "fetched": "2026-09-21",
                                                        "latest_note": "hand-edited field survives"}]}},
                "excluded": {"CDR-": "hand-written reason"}}

    def test_seed_adds_only_the_missing_entries(self):
        links = {"RFC6749-": "https://tools.ietf.org/html/rfc6749#section-",
                 "RFC6749A-": "https://tools.ietf.org/html/rfc6749#appendix-",
                 "PAR-": "https://www.rfc-editor.org/rfc/rfc9126.html#section-",
                 "NOSRC-": "https://openid.net/specs/no-source-1_0.html#section-",
                 "ISO18013-5-": "https://www.iso.org/standard/69084.html#",
                 "CDR-": "https://consumerdatastandardsaustralia.github.io/standards/#"}
        output, manifest, unchanged = self.seeded(links, self.EXISTING)
        self.assertFalse(unchanged)
        # new document
        self.assertEqual(["PAR-"], manifest["documents"]["rfc9126"]["prefixes"])
        self.assertEqual("ietf/rfc9126.txt", manifest["documents"]["rfc9126"]["versions"][0]["file"])
        self.assertNotIn("sha256", manifest["documents"]["rfc9126"]["versions"][0])
        # new prefix on an existing document; its hand-edited version is left alone
        self.assertEqual(["RFC6749-", "RFC6749A-"], manifest["documents"]["rfc6749"]["prefixes"])
        self.assertEqual(self.EXISTING["documents"]["rfc6749"]["versions"], manifest["documents"]["rfc6749"]["versions"])
        # new exclusions; the existing hand-written reason is kept
        self.assertEqual({"private": "library/iso/18013-5/"}, manifest["excluded"]["ISO18013-5-"])
        self.assertIn("no .txt/.zip/.xml/.md sibling", manifest["excluded"]["NOSRC-"])
        self.assertEqual("hand-written reason", manifest["excluded"]["CDR-"])
        for token in ("rfc9126", "PAR-", "RFC6749A-", "ISO18013-5-", "NOSRC-", "sync --only"):
            self.assertIn(token, output)
        self.assertNotIn("CDR-", output)

    def test_seed_with_nothing_missing_leaves_the_file_untouched(self):
        links = {"RFC6749-": "https://tools.ietf.org/html/rfc6749#section-",
                 "CDR-": "https://consumerdatastandardsaustralia.github.io/standards/#"}
        output, _manifest, unchanged = self.seeded(links, self.EXISTING)
        self.assertTrue(unchanged)
        self.assertIn("nothing to add", output)

    def test_seed_ignores_manifest_prefixes_that_left_log_entry_helper(self):
        # A stale manifest prefix is check's job to report; seed must not delete it.
        output, manifest, unchanged = self.seeded({"RFC6749-": "https://tools.ietf.org/html/rfc6749#section-"}, self.EXISTING)
        self.assertTrue(unchanged)
        self.assertIn("CDR-", manifest["excluded"])


class ValidateManifestTest(unittest.TestCase):
    LINKS = {"RFC6749-": "https://tools.ietf.org/html/rfc6749#section-",
             "CDR-": "https://consumerdatastandardsaustralia.github.io/standards/#"}

    def manifest(self):
        return {"documents": {"rfc6749": {"prefixes": ["RFC6749-"],
                                          "link_url": "https://tools.ietf.org/html/rfc6749",
                                          "versions": [{"role": "linked", "file": "ietf/rfc6749.txt"}]}},
                "excluded": {"CDR-": "third-party"}}

    def test_clean(self):
        self.assertEqual([], sl.validate_manifest(self.manifest(), self.LINKS))

    def test_unlisted_prefix(self):
        links = dict(self.LINKS, **{"NEW-": "https://example.com/#"})
        self.assertEqual(["NEW-: not in manifest"], sl.validate_manifest(self.manifest(), links))

    def test_link_url_drift(self):
        links = dict(self.LINKS, **{"RFC6749-": "https://tools.ietf.org/html/rfc9999#section-"})
        self.assertEqual(["RFC6749-: LogEntryHelper links https://tools.ietf.org/html/rfc9999 but manifest document "
                          "rfc6749 has link_url https://tools.ietf.org/html/rfc6749"],
                         sl.validate_manifest(self.manifest(), links))

    def test_stale_and_duplicate_and_missing_linked(self):
        m = self.manifest()
        m["excluded"]["RFC6749-"] = "dup"
        m["excluded"]["GONE-"] = "x"
        m["documents"]["rfc6749"]["versions"] = [{"role": "latest", "file": "ietf/x.txt"}]
        self.assertEqual(["GONE-: in manifest but not in LogEntryHelper",
                          "RFC6749-: listed more than once in manifest",
                          "rfc6749: 0 versions with role 'linked', need exactly one"],
                         sorted(sl.validate_manifest(m, self.LINKS)))

    def test_two_linked_versions_are_rejected(self):
        m = self.manifest()
        m["documents"]["rfc6749"]["versions"].append({"role": "linked", "file": "ietf/rfc6749bis.txt"})
        self.assertEqual(["rfc6749: 2 versions with role 'linked', need exactly one"],
                         sl.validate_manifest(m, self.LINKS))

    def test_unknown_role_is_rejected(self):
        m = self.manifest()
        m["documents"]["rfc6749"]["versions"].append({"role": "lastest", "file": "ietf/rfc6749bis.txt"})
        self.assertEqual(["rfc6749: unknown role 'lastest', must be 'linked' or 'latest'"],
                         sl.validate_manifest(m, self.LINKS))

    def test_file_shared_by_two_versions_is_rejected(self):
        m = self.manifest()
        m["documents"]["rfc6749"]["versions"].append({"role": "latest", "file": "ietf/rfc6749.txt"})
        self.assertEqual(["ietf/rfc6749.txt: used by more than one version (rfc6749, rfc6749)"],
                         sl.validate_manifest(m, self.LINKS))

    def test_absolute_file_path_is_rejected(self):
        m = self.manifest()
        m["documents"]["rfc6749"]["versions"][0]["file"] = "/etc/passwd"
        errors = sl.validate_manifest(m, self.LINKS)
        self.assertTrue(any("must be a relative ietf/... or openid/... path" in e for e in errors), errors)

    def test_dotdot_file_path_is_rejected(self):
        m = self.manifest()
        m["documents"]["rfc6749"]["versions"][0]["file"] = "ietf/../../etc/passwd"
        errors = sl.validate_manifest(m, self.LINKS)
        self.assertTrue(any("must be a relative ietf/... or openid/... path" in e for e in errors), errors)

    def test_file_path_outside_ietf_or_openid_is_rejected(self):
        m = self.manifest()
        m["documents"]["rfc6749"]["versions"][0]["file"] = "profiles/rfc6749.txt"
        errors = sl.validate_manifest(m, self.LINKS)
        self.assertTrue(any("must be a relative ietf/... or openid/... path" in e for e in errors), errors)


class RenderTest(unittest.TestCase):
    LINK = "https://x/spec.html"
    PUBLISHED = '<h2><a class="section-number selfRef">1. </a><a>Introduction</a></h2>'
    RENDERED = "1.  Introduction\n\n   body\n"
    SOURCE_URL = {"txt": "https://x/rfc1.txt", "zip-xml": "https://x/spec.zip", "xml": "https://x/spec.xml",
                  "md": "https://x/spec.md", "github-md": "https://x/repo.tar.gz"}

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)

    @classmethod
    def _version(cls, fmt, **extra):
        file = "ietf/rfc1.txt" if fmt == "txt" else "openid/spec.txt"
        return {"role": "linked", "file": file, "source_url": cls.SOURCE_URL[fmt], "source_format": fmt, **extra}

    def _fetch(self, source, published=PUBLISHED):
        return lambda url: published.encode() if url == self.LINK else source

    @classmethod
    def _xml2rfc_run(cls, doc_name="spec", text=RENDERED):
        def run(cmd, **kw):
            if cmd[0] == "mmark":
                return subprocess.CompletedProcess(cmd, 0, stdout=f'<rfc docName="{doc_name}"></rfc>')
            if "--version" in cmd:
                return subprocess.CompletedProcess(cmd, 0, stdout="xml2rfc 3.34.0\n")
            out = cmd[cmd.index("-o") + 1]
            pathlib.Path(out).write_text(text, encoding="utf-8")
            return subprocess.CompletedProcess(cmd, 0, stdout="")
        return run

    def render(self, v, fetch, run=None):
        """Render v under the test's temp dir; returns the destination path."""
        dest = os.path.join(self.dir.name, v["file"])
        sl.render_version(v, self.LINK, dest, fetch=fetch, run=run or self._xml2rfc_run())
        return dest

    def test_txt_is_stored_byte_for_byte(self):
        body = b"1.  Introduction\n\n   RFC text \x0c with form feed and trailing space \n"
        v = self._version("txt")
        dest = self.render(v, lambda url: body)
        self.assertEqual(body, pathlib.Path(dest).read_bytes())
        self.assertEqual(hashlib.sha256(body).hexdigest(), v["sha256"])
        self.assertNotIn("generator", v)

    def test_txt_without_numbered_headings_is_rejected_and_not_written(self):
        body = b"<!DOCTYPE html><html><body>Please enable JavaScript</body></html>\n"
        v = self._version("txt")
        with self.assertRaises(sl.HeadingMismatch):
            self.render(v, lambda url: body)
        self.assertFalse(os.path.exists(os.path.join(self.dir.name, v["file"])))
        self.assertNotIn("sha256", v)

    def test_zip_xml_renders_the_single_xml_and_cross_checks_headings(self):
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w") as z:
            z.writestr("spec-1_0-30.xml", '<rfc docName="spec-1_0-30"></rfc>')
            z.writestr("spec-1_0.md", "# x")
            z.writestr("examples/a.json", "{}")
        v = self._version("zip-xml")
        dest = self.render(v, self._fetch(buf.getvalue()))
        self.assertEqual("spec-1_0-30", v["doc_name"])
        self.assertEqual("xml2rfc 3.34.0", v["generator"])
        self.assertTrue(os.path.exists(dest))

        with self.assertRaisesRegex(sl.HeadingMismatch, "1: 'Introduction' != 'Scope'"):
            self.render(self._version("zip-xml"),
                        self._fetch(buf.getvalue(), self.PUBLISHED.replace("Introduction", "Scope")))

    def test_xml_source_is_rendered_directly(self):
        v = self._version("xml")
        dest = self.render(v, self._fetch(b'<rfc docName="spec-x"></rfc>'), self._xml2rfc_run("spec-x"))
        self.assertEqual("spec-x", v["doc_name"])
        self.assertTrue(os.path.exists(dest))

    def _mmark_capturing_run(self, doc_name, captured):
        inner = self._xml2rfc_run(doc_name)

        def run(cmd, **kw):
            if cmd[0] == "mmark":
                captured["cwd"] = kw.get("cwd")
                captured["md_dir"] = os.path.dirname(cmd[1])
            return inner(cmd, **kw)
        return run

    def test_md_source_runs_mmark_in_the_md_files_directory(self):
        captured = {}
        v = self._version("md")
        self.render(v, self._fetch(b"# x"), self._mmark_capturing_run("spec-md", captured))
        self.assertEqual("spec-md", v["doc_name"])
        self.assertEqual(captured["md_dir"], captured["cwd"])

    @staticmethod
    def _make_targz(files):
        buf = io.BytesIO()
        with tarfile.open(fileobj=buf, mode="w:gz") as t:
            for name, content in files.items():
                data = content.encode()
                info = tarfile.TarInfo(name=name)
                info.size = len(data)
                t.addfile(info, io.BytesIO(data))
        return buf.getvalue()

    def test_github_md_source_honours_path_and_runs_mmark_in_its_directory(self):
        captured = {}
        v = self._version("github-md", path="dir/spec.md")
        self.render(v, self._fetch(self._make_targz({"repo-sha/dir/spec.md": "# x"})),
                    self._mmark_capturing_run("spec-gh", captured))
        self.assertEqual("spec-gh", v["doc_name"])
        self.assertEqual(captured["md_dir"], captured["cwd"])
        self.assertEqual(os.path.join("repo-sha", "dir"), captured["cwd"][-len(os.path.join("repo-sha", "dir")):])

    def test_github_md_missing_path_key_raises_value_error(self):
        with self.assertRaisesRegex(ValueError, "path"):
            self.render(self._version("github-md"), self._fetch(self._make_targz({"repo-sha/dir/spec.md": "# x"})))

    def test_github_md_empty_tarball_raises_value_error_not_index_error(self):
        with self.assertRaisesRegex(ValueError, "empty tarball"):
            self.render(self._version("github-md", path="dir/spec.md"), self._fetch(self._make_targz({})))

    def test_tar_member_path_traversal_does_not_escape_the_work_dir(self):
        with self.assertRaises(Exception):
            self.render(self._version("github-md", path="dir/spec.md"), self._fetch(self._make_targz({"../evil": "evil"})))

    def test_no_rendered_headings_is_a_heading_mismatch(self):
        with self.assertRaisesRegex(sl.HeadingMismatch, "no numbered headings"):
            self.render(self._version("xml"), self._fetch(b'<rfc docName="x"></rfc>'),
                        self._xml2rfc_run("x", text="no headings in this rendered text\n"))


class SyncTest(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)
        self.specs = self.dir.name
        self.manifest_path = os.path.join(self.specs, "manifest.json")
        os.makedirs(os.path.join(self.specs, "ietf"))
        for name in ("linked.txt", "latest.txt"):
            pathlib.Path(self.specs, "ietf", name).write_text("original " + name)
        sha = lambda n: hashlib.sha256(("original " + n).encode()).hexdigest()
        self.write_manifest({"documents": {"doc": {"prefixes": ["D-"], "link_url": "https://x/doc", "versions": [
            {"role": "linked", "file": "ietf/linked.txt", "source_format": "txt", "source_url": "u1", "sha256": sha("linked.txt")},
            {"role": "latest", "file": "ietf/latest.txt", "source_format": "txt", "source_url": "u2", "sha256": sha("latest.txt")},
        ]}}, "excluded": {}})
        self.rendered = []
        self.caches = []
        # file -> exception the fake render raises for that version
        self.fail_on = {}

    def read_manifest(self):
        return json.loads(pathlib.Path(self.manifest_path).read_text())

    def write_manifest(self, manifest):
        pathlib.Path(self.manifest_path).write_text(json.dumps(manifest))

    def add_version(self, doc_id, file, **extra):
        """Add a version to doc_id (creating the document) with no recorded hash, so sync renders it."""
        manifest = self.read_manifest()
        doc = manifest["documents"].setdefault(doc_id, {"prefixes": [doc_id + "-"], "link_url": "https://x/" + doc_id,
                                                        "versions": []})
        doc["versions"].append({"role": "linked", "file": file, "source_format": "txt", "source_url": "u-" + doc_id, **extra})
        self.write_manifest(manifest)

    def render(self, version, link, dest, cache_dir=None):
        if version["file"] in self.fail_on:
            raise self.fail_on[version["file"]]
        self.rendered.append(version["role"])
        self.caches.append(cache_dir)
        pathlib.Path(dest).write_text("refreshed", encoding="utf-8")
        version["sha256"] = "new"

    def sync(self, **kw):
        with contextlib.redirect_stdout(io.StringIO()) as out:
            rc = sl.cmd_sync(manifest_path=self.manifest_path, specs_dir=self.specs, render=self.render, **kw)
        return rc, out.getvalue()

    def test_versions_rendered_in_one_run_share_one_xml2rfc_cache(self):
        rc, _ = self.sync(refresh="doc", refresh_linked="doc")
        self.assertEqual(0, rc)
        self.assertEqual(2, len(self.caches))
        self.assertEqual(1, len(set(self.caches)))
        self.assertFalse(os.path.exists(self.caches[0]), "the cache does not outlive the run")

    def test_up_to_date_tree_renders_nothing(self):
        self.assertEqual((0, ""), self.sync())
        self.assertEqual([], self.rendered)

    def test_refresh_replaces_latest_only(self):
        rc, _ = self.sync(refresh="doc")
        self.assertEqual(0, rc)
        self.assertEqual(["latest"], self.rendered)

    def test_refresh_linked_replaces_linked_only(self):
        rc, _ = self.sync(refresh_linked="doc")
        self.assertEqual(0, rc)
        self.assertEqual(["linked"], self.rendered)

    def test_refresh_of_a_document_with_no_latest_version_is_an_error(self):
        manifest = self.read_manifest()
        del manifest["documents"]["doc"]["versions"][1]
        self.write_manifest(manifest)
        rc, out = self.sync(refresh="doc")
        self.assertEqual(1, rc)
        self.assertIn("--refresh doc: nothing was refreshed; it has no latest version", out)
        self.assertEqual([], self.rendered)

    def test_refresh_of_a_document_excluded_by_only_is_an_error(self):
        self.add_version("other", "ietf/linked.txt", sha256=self.read_manifest()["documents"]["doc"]["versions"][0]["sha256"])
        rc, out = self.sync(only="other", refresh="doc")
        self.assertEqual(1, rc)
        self.assertIn("--refresh doc: nothing was refreshed; skipped by --only other", out)
        self.assertEqual([], self.rendered)

    def test_unknown_doc_for_only_returns_2_without_writing_the_manifest(self):
        before = pathlib.Path(self.manifest_path).read_text()
        rc, _ = self.sync(only="nope")
        self.assertEqual(2, rc)
        self.assertEqual(before, pathlib.Path(self.manifest_path).read_text())

    def test_unknown_doc_for_refresh_returns_2(self):
        self.assertEqual(2, self.sync(refresh="nope")[0])

    def test_modified_existing_file_is_an_error(self):
        with open(os.path.join(self.specs, "ietf", "linked.txt"), "a") as f:
            f.write(" ")
        rc, out = self.sync()
        self.assertEqual(1, rc)
        self.assertIn("ietf/linked.txt does not match the sha256 in the manifest", out)
        self.assertEqual([], self.rendered)

    def test_missing_file_with_recorded_hash_is_an_error_naming_the_right_flag(self):
        os.remove(os.path.join(self.specs, "ietf", "linked.txt"))
        rc, out = self.sync()
        self.assertEqual(1, rc)
        self.assertIn("pass --refresh-linked doc", out)

    def test_new_version_without_sha256_is_rendered(self):
        self.add_version("doc", "ietf/new.txt", role="latest")
        rc, _ = self.sync()
        self.assertEqual(0, rc)
        self.assertEqual(["latest"], self.rendered)
        self.assertTrue(os.path.exists(os.path.join(self.specs, "ietf", "new.txt")))
        self.assertEqual("new", self.read_manifest()["documents"]["doc"]["versions"][2]["sha256"])

    def test_existing_file_without_sha256_is_rendered_not_skipped(self):
        pathlib.Path(self.specs, "ietf", "incomplete.txt").write_text("stale partial render")
        self.add_version("doc", "ietf/incomplete.txt", role="latest")
        rc, _ = self.sync()
        self.assertEqual(0, rc)
        self.assertEqual(["latest"], self.rendered)
        self.assertEqual("refreshed", pathlib.Path(self.specs, "ietf", "incomplete.txt").read_text())

    def test_exception_rendering_one_version_does_not_abort_the_run(self):
        self.add_version("docA", "ietf/a.txt")
        self.add_version("docB", "ietf/b.txt")
        self.fail_on["ietf/a.txt"] = zipfile.BadZipFile("corrupt")
        rc, out = self.sync()
        self.assertEqual(1, rc)
        self.assertIn("docA", out)
        self.assertIn("BadZipFile", out)
        updated = self.read_manifest()
        self.assertEqual("new", updated["documents"]["docB"]["versions"][0]["sha256"])
        self.assertNotIn("sha256", updated["documents"]["docA"]["versions"][0])

    def test_manifest_is_written_in_a_finally_even_on_keyboard_interrupt(self):
        self.add_version("docA", "ietf/a.txt")
        self.add_version("docB", "ietf/b.txt")
        self.fail_on["ietf/b.txt"] = KeyboardInterrupt()
        with self.assertRaises(KeyboardInterrupt):
            self.sync()
        self.assertEqual("new", self.read_manifest()["documents"]["docA"]["versions"][0]["sha256"])

    def test_file_not_found_error_names_the_missing_tool(self):
        self.add_version("docX", "openid/x.txt", source_format="xml")
        self.fail_on["openid/x.txt"] = FileNotFoundError(2, "No such file or directory", "xml2rfc")
        rc, out = self.sync()
        self.assertEqual(1, rc)
        self.assertIn("xml2rfc", out)
        self.assertIn("nix", out)

    def test_file_not_found_error_for_a_path_is_not_blamed_on_the_tools(self):
        self.add_version("docX", "openid/x.txt", source_format="github-md", path="1.2/x.md")
        self.fail_on["openid/x.txt"] = FileNotFoundError(2, "No such file or directory", "/tmp/work/OpenID4VP-abc/1.2")
        rc, out = self.sync()
        self.assertEqual(1, rc)
        self.assertIn("/tmp/work/OpenID4VP-abc/1.2", out)
        self.assertNotIn("PATH", out)

    def test_failed_tool_run_reports_its_stderr(self):
        self.add_version("docX", "openid/x.txt", source_format="xml")
        self.fail_on["openid/x.txt"] = subprocess.CalledProcessError(
            1, ["xml2rfc", "in.xml"], output="", stderr="Warning: ignored\nError: Unable to fetch reference.RFC.9999.xml\n")
        rc, out = self.sync()
        self.assertEqual(1, rc)
        self.assertIn("Unable to fetch reference.RFC.9999.xml", out)


class ArgParsingTest(unittest.TestCase):
    def parse(self, *argv):
        return sl.build_parser().parse_args(argv)

    def test_sync_flags_are_parsed(self):
        args = self.parse("sync", "--only", "doc1", "--refresh-linked", "doc2")
        self.assertEqual(("sync", "doc1", None, "doc2"), (args.command, args.only, args.refresh, args.refresh_linked))

    def test_bad_sync_args_exit_2_without_touching_anything(self):
        for argv in (["sync", "--only"], ["sync", "--only", "--refresh"], ["sync", "--bogus", "x"],
                     ["check", "extra"], ["nonsense"], []):
            with self.subTest(argv=argv), contextlib.redirect_stderr(io.StringIO()), \
                    self.assertRaises(SystemExit) as cm:
                sl.main(["spec_library.py"] + argv)
            self.assertEqual(2, cm.exception.code)


class CheckTest(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)
        self.specs = self.dir.name
        os.makedirs(os.path.join(self.specs, "ietf"))
        os.makedirs(os.path.join(self.specs, "openid"))
        pathlib.Path(self.specs, "ietf", "rfc1.txt").write_bytes(b"x")
        pathlib.Path(self.specs, "ietf", "orphan.txt").write_bytes(b"y")
        sha = hashlib.sha256(b"x").hexdigest()
        self.manifest_path = os.path.join(self.specs, "manifest.json")
        pathlib.Path(self.manifest_path).write_text(json.dumps({
            "documents": {"doc": {"prefixes": ["D-"], "link_url": "u", "versions": [
                {"role": "linked", "file": "ietf/rfc1.txt", "sha256": sha}]}},
            "excluded": {}}))
        self.links = {"D-": "u#"}

    def check(self):
        with contextlib.redirect_stdout(io.StringIO()) as out:
            rc = sl.cmd_check(manifest_path=self.manifest_path, specs_dir=self.specs, links=self.links)
        return rc, out.getvalue()

    def test_clean_tree_reports_ok(self):
        os.remove(os.path.join(self.specs, "ietf", "orphan.txt"))
        rc, out = self.check()
        self.assertEqual(0, rc)
        self.assertIn("ok:", out)

    def test_orphan_file_is_reported(self):
        rc, out = self.check()
        self.assertEqual(1, rc)
        self.assertIn("ietf/orphan.txt: orphan", out)

    def test_referenced_file_is_not_reported_as_orphan(self):
        rc, out = self.check()
        self.assertNotIn("ietf/rfc1.txt: orphan", out)


if __name__ == "__main__":
    unittest.main()
