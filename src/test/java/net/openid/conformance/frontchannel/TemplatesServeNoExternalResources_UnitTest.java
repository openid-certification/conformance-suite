package net.openid.conformance.frontchannel;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Thymeleaf templates under {@code src/main/resources/templates} are the pages the suite
 * serves to the browser during a test: {@code implicitCallback.html}, {@code resultCaptured.html},
 * the form_post and logout pages. In CI and on many testers' machines that browser is the
 * suite's own headless HtmlUnit instance ({@link BrowserControl}), which fetches every
 * stylesheet and script a page references and blocks for {@code BROWSER_HTTP_TIMEOUT_MILLIS}
 * per resource it cannot reach. A page that pulls Bootstrap from a CDN therefore turns a
 * network blip into a stalled test module. Everything these pages need must be served from
 * the suite itself, and that includes anything the stylesheets they link go on to import.
 */
public class TemplatesServeNoExternalResources_UnitTest {

	private static final Path TEMPLATES = Paths.get("src/main/resources/templates");

	private static final Path STATIC = Paths.get("src/main/resources/static");

	private static final Pattern EXTERNAL_TAG = Pattern.compile(
		"<(link|script|img|iframe)\\b[^>]*\\b(href|src)\\s*=\\s*[\"'](https?:)?//[^\"']*[\"']",
		Pattern.CASE_INSENSITIVE);

	/** {@code @import} or {@code url()} naming another host, in an inline style block or a stylesheet. */
	private static final Pattern EXTERNAL_CSS_URL = Pattern.compile(
		"(@import\\s+|url\\(\\s*)[\"']?(https?:)?//",
		Pattern.CASE_INSENSITIVE);

	private static final Pattern LOCAL_STYLESHEET = Pattern.compile(
		"<link\\b[^>]*\\bhref\\s*=\\s*[\"'](/[^\"']+\\.css)[\"']",
		Pattern.CASE_INSENSITIVE);

	@Test
	public void templates_reference_no_resources_on_other_hosts() throws IOException {
		assertThat(TEMPLATES).as("expected to run from the project root").isDirectory();

		List<Path> templates;
		try (Stream<Path> files = Files.walk(TEMPLATES)) {
			templates = files.filter(p -> p.toString().endsWith(".html")).sorted().toList();
		}

		List<String> offenders = new ArrayList<>();
		Set<Path> stylesheets = new TreeSet<>();
		for (Path template : templates) {
			String html = Files.readString(template);
			offenders.addAll(externalReferences(template, html, EXTERNAL_TAG, EXTERNAL_CSS_URL));
			Matcher m = LOCAL_STYLESHEET.matcher(html);
			while (m.find()) {
				stylesheets.add(STATIC.resolve(m.group(1).substring(1)));
			}
		}
		for (Path stylesheet : stylesheets) {
			assertThat(stylesheet).as("stylesheet linked from a template is not served").isRegularFile();
			offenders.addAll(externalReferences(stylesheet, Files.readString(stylesheet), EXTERNAL_CSS_URL));
		}

		assertThat(offenders)
			.as("Templates served to the test browser must not load resources from other hosts (vendor them under static/ instead)")
			.isEmpty();
	}

	private static List<String> externalReferences(Path file, String content, Pattern... patterns) {
		List<String> found = new ArrayList<>();
		for (Pattern pattern : patterns) {
			Matcher m = pattern.matcher(content);
			while (m.find()) {
				int line = 1 + (int) content.substring(0, m.start()).chars().filter(c -> c == '\n').count();
				found.add(file + ":" + line + ": " + m.group());
			}
		}
		return found;
	}
}
