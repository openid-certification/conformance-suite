import importlib.util
import os
import tempfile
import unittest

_spec = importlib.util.spec_from_file_location(
    "fetch_spec", os.path.join(os.path.dirname(os.path.abspath(__file__)), "fetch-spec.py"))
fs = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(fs)


def headings_of(text):
    with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False, encoding="utf-8") as f:
        f.write(text)
    try:
        return fs.headings(f.name)
    finally:
        os.unlink(f.name)


class HeadingsTest(unittest.TestCase):
    def test_title_may_start_with_a_code_literal(self):
        self.assertEqual([("5.8", "aud of a Request Object")],
                         headings_of("5.8. aud of a Request Object\n\nbody\n"))

    def test_wrapped_body_lines_are_not_headings(self):
        self.assertEqual([], headings_of("2.0 roles map to the following\n60 minutes.\n"))

    def test_indented_list_items_are_not_headings(self):
        self.assertEqual([], headings_of("   1. The JWT MUST contain\n"))

    def test_long_joined_title_is_kept(self):
        # OID4VCI 1.0 section 14.4 is 120 characters once its continuation line is joined.
        title = ("Relationship between the Credential Issuer Identifier in the Metadata and the Issuer "
                 "Identifier in the Issued Credential")
        self.assertEqual([("14.4", title)],
                         headings_of("14.4.  Relationship between the Credential Issuer Identifier in the\n"
                                     "       Metadata and the Issuer Identifier in the Issued Credential\n\n   body\n"))

    def test_xml2rfc_wrapped_title_is_joined(self):
        self.assertEqual([("3.3.4", "Identifying Credentials Being Issued Throughout the Issuance Flow")],
                         headings_of("3.3.4.  Identifying Credentials Being Issued Throughout the Issuance\n"
                                     "        Flow\n\n   body\n"))


class HtmlToTextTest(unittest.TestCase):
    def test_rfc2629_heading_split_over_two_lines_becomes_one_line(self):
        raw = ('<a name="rfc.section.3.1.2.1"></a><h3>3.1.2.1.&nbsp;\nAuthentication Request</h3>\n'
               '<p>An Authentication Request is\nan OAuth 2.0 Authorization Request</p>')
        text = fs.html_to_text(raw)
        self.assertIn("3.1.2.1. Authentication Request\n", text)
        self.assertEqual([("3.1.2.1", "Authentication Request")], headings_of(text))

    def test_numbered_lines_in_a_code_block_are_indented_not_headings(self):
        raw = ('<p>Some example URIs are:</p>\n<pre class="highlight"><code>1. https://mtls.dh.example.com/api\n'
               '2. https://tls.dh.example.com/complex\n</code></pre>\n<h2>2. Terminology</h2>')
        text = fs.html_to_text(raw)
        self.assertIn("\n   1. https://mtls.dh.example.com/api\n   2. https://tls.dh.example.com/complex\n", text)
        self.assertEqual([("2", "Terminology")], headings_of(text))

    def test_list_items_are_indented_not_headings(self):
        text = fs.html_to_text("<p>Steps:</p><ol><li>1. first</li><li><p>2. second</p></li></ol>")
        self.assertIn("\n   1. first\n", text)
        self.assertIn("\n   2. second\n", text)
        self.assertEqual([], headings_of(text))

    def test_paragraph_line_breaks_are_left_alone(self):
        self.assertIn("is\nan OAuth", fs.html_to_text("<p>An Authentication Request is\nan OAuth 2.0 request</p>"))

    # html_to_text strips only the start/end of the whole document, so a heading must not be
    # first for its leading indentation to be exercised by the test.
    LEADING_CONTENT = "<p>Front matter.</p>\n"
    # xml2rfc-v3 pages indent <hN> itself and put its content on the next line, wrapped in
    # <a> elements for the number and title (unlike rfc2629's <h3>NUM.&nbsp;\nTITLE</h3>).
    V3_HEADING = ('<section>\n         <h3 id="x">\n'
                  '<a href="#section-5.8" class="section-number selfRef">5.8. </a>'
                  '<a href="#name-aud" class="section-name selfRef"><code>aud</code> of a Request Object</a>\n'
                  '         </h3>\n<p>body</p></section>')

    def test_indented_v3_style_heading_with_nested_tags(self):
        self.assertEqual([("5.8", "aud of a Request Object")],
                         headings_of(fs.html_to_text(self.LEADING_CONTENT + self.V3_HEADING)))

    def test_indented_v3_style_single_line_heading(self):
        raw = self.LEADING_CONTENT + (
               '      <h2 id="y"><a class="section-number selfRef">2. </a>'
               '<a class="section-name selfRef">Terminology</a></h2>')
        self.assertEqual([("2", "Terminology")], headings_of(fs.html_to_text(raw)))

    def test_indented_toc_entry_is_not_double_counted(self):
        toc = ('<li>\n            <p><a href="#section-5.8">5.8</a>.  '
               '<a href="#name-aud">aud of a Request Object</a></p></li>\n')
        self.assertEqual([("5.8", "aud of a Request Object")],
                         headings_of(fs.html_to_text(self.LEADING_CONTENT + toc + self.V3_HEADING)))


if __name__ == "__main__":
    unittest.main()
