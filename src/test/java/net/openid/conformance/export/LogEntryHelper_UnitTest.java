package net.openid.conformance.export;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import net.openid.conformance.testmodule.OIDFJSON;

import static org.assertj.core.api.Assertions.assertThat;

public class LogEntryHelper_UnitTest {

	@Test
	public void brazilDcrRequirementLabelsUseVersion21ConfluenceHeadingAnchors() {
		LogEntryHelper helper = new LogEntryHelper(new Document(), new Gson());
		String spec = "https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/1334116474/" +
			"EN+Open+Finance+Brasil+Dynamic+Client+Registration+-+v2.1.0#";

		assertThat(helper.getRequirementLink("BrazilOBDCR-6.1"))
			.isEqualTo(spec + "6.1.-Authorization-server");
		assertThat(helper.getRequirementLink("BrazilOBDCR-7.1-5"))
			.isEqualTo(spec + "7.1.-Authorization-server");
		assertThat(helper.getRequirementLink("BrazilOBDCR-7.1.1"))
			.isEqualTo(spec + "7.1.1.-Applying-Server-Defaults");
		assertThat(helper.getRequirementLink("BrazilOBDCR-9.3.2-4"))
			.isEqualTo(spec + "9.3.2.-Client-Maintenance---GET-%2Fregister---PUT-%2Fregister---" +
				"DELETE-%2Fregister");
	}

	@Test
	public void brazilCibaRequirementLabelsUseBeta2ConfluenceHeadingAnchors() {
		LogEntryHelper helper = new LogEntryHelper(new Document(), new Gson());
		String spec = "https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/2092204111/" +
			"EN+Open+Finance+Brasil+Client+Initiated+Backchannel+Authentication+-+v2.1.0-beta2#";

		assertThat(helper.getRequirementLink("BrazilCIBA-6.2.2"))
			.isEqualTo(spec + "6.2.2.-CIBA-delivery-modes");
		assertThat(helper.getRequirementLink("BrazilCIBA-6.2.6"))
			.isEqualTo(spec + "6.2.6.-requested_expiry-parameter-and-the-validity-period-of-the-" +
				"authentication-request-(expires_in)");
		assertThat(helper.getRequirementLink("BrazilCIBA-6.3.7"))
			.isEqualTo(spec + "6.3.7.-requested_expiry-parameter");

		Map<String, String> remainingAnchors = Map.ofEntries(
			Map.entry("BrazilCIBA-6.2.3", "6.2.3.-login_hint-parameter-and-user-identification"),
			Map.entry("BrazilCIBA-6.2.4", "6.2.4.-Client-registration-and-user_code-parameter"),
			Map.entry("BrazilCIBA-6.2.5", "6.2.5.-binding_message-parameter"),
			Map.entry("BrazilCIBA-6.2.8", "6.2.8.-PING-notifications-and-idempotency"),
			Map.entry("BrazilCIBA-6.3.2", "6.3.2.-Use-of-login_hint"),
			Map.entry("BrazilCIBA-6.3.4", "6.3.4.-Support-for-ping-mode"),
			Map.entry("BrazilCIBA-6.3.4.1", "6.3.4.1.-Use-of-poll-mode-as-fallback"),
			Map.entry("BrazilCIBA-6.3.5", "6.3.5.-Client-registration-and-user_code-parameter"),
			Map.entry("BrazilCIBA-6.3.6", "6.3.6.-binding_message-parameter"),
			Map.entry("BrazilCIBA-6.3.8", "6.3.8.-Dynamic-client-registration-(DCR/DCM)"));
		remainingAnchors.forEach((requirement, anchor) ->
			assertThat(helper.getRequirementLink(requirement)).isEqualTo(spec + anchor));
	}

	@Test
	public void brazilFapi22RequirementLabelsUseFinalConfluenceHeadingAnchors() {
		LogEntryHelper helper = new LogEntryHelper(new Document(), new Gson());
		String spec = "https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/1675395195/" +
			"EN+Open+Finance+Brasil+Financial-grade+API+Security+Profile+-+v2.2.0#";

		assertThat(helper.getRequirementLink("BrazilOB22-5.1-4"))
			.isEqualTo(spec + "5.1.-Authorization-Server");
		assertThat(helper.getRequirementLink("BrazilOB22-5.1.1-1"))
			.isEqualTo(spec + "5.1.1.-ID-Token");
		assertThat(helper.getRequirementLink("BrazilOB22-5.2-9"))
			.isEqualTo(spec + "5.2.-Cliente-confidencial");
		assertThat(helper.getRequirementLink("BrazilOB22-6.2"))
			.isEqualTo(spec + "6.2.-Signing-algorithm-considerations");
		assertThat(helper.getRequirementLink("BrazilOB22-6.3"))
			.isEqualTo(spec + "6.3.-Encryption-algorithm-considerations");
	}

	@Test
	public void brazilFapi22SectionPrefixDoesNotMatchASeparateLongerSection() {
		LogEntryHelper helper = new LogEntryHelper(new Document(), new Gson());
		String spec = "https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/1675395195/" +
			"EN+Open+Finance+Brasil+Financial-grade+API+Security+Profile+-+v2.2.0#";

		assertThat(helper.getRequirementLink("BrazilOB22-5.12"))
			.isEqualTo(spec + "5.12");
	}

	private static final Path SPEC_LIBRARY = Path.of("library", "specs");

	private static JsonObject specLibraryManifest() throws Exception {
		JsonObject manifest = JsonParser.parseString(Files.readString(SPEC_LIBRARY.resolve("manifest.json"), StandardCharsets.UTF_8))
			.getAsJsonObject();
		assertThat(manifest.getAsJsonObject("documents").entrySet())
			.as("library/specs/manifest.json must list at least one document")
			.isNotEmpty();
		return manifest;
	}

	@Test
	public void everySpecLinkIsAccountedForInTheSpecLibraryManifest() throws Exception {
		JsonObject manifest = specLibraryManifest();
		Map<String, String> linkUrlByPrefix = new HashMap<>();
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, JsonElement> doc : manifest.getAsJsonObject("documents").entrySet()) {
			String linkUrl = OIDFJSON.getStringOrNull(doc.getValue().getAsJsonObject().get("link_url"));
			if (linkUrl == null) {
				problems.add(doc.getKey() + ": document has no link_url in the manifest");
				linkUrl = "";
			}
			for (JsonElement prefix : doc.getValue().getAsJsonObject().getAsJsonArray("prefixes")) {
				if (linkUrlByPrefix.put(OIDFJSON.getString(prefix), linkUrl) != null) {
					problems.add(OIDFJSON.getString(prefix) + ": listed more than once in manifest");
				}
			}
		}
		for (String prefix : manifest.getAsJsonObject("excluded").keySet()) {
			if (linkUrlByPrefix.put(prefix, "") != null) {
				problems.add(prefix + ": listed more than once in manifest");
			}
		}
		for (Map.Entry<String, String> link : LogEntryHelper.specLinks.entrySet()) {
			String expected = linkUrlByPrefix.remove(link.getKey());
			String actual = link.getValue().split("#", 2)[0];
			if (expected == null) {
				problems.add(link.getKey() + ": not in library/specs/manifest.json");
			} else if (!expected.isEmpty() && !expected.equals(actual)) {
				problems.add(link.getKey() + ": LogEntryHelper links " + actual + " but the manifest has " + expected);
			}
		}
		linkUrlByPrefix.keySet().forEach(prefix -> problems.add(prefix + ": in manifest but not in LogEntryHelper"));

		assertThat(problems)
			.as("library/specs/manifest.json must list every LogEntryHelper.specLinks prefix; see library/README.md")
			.isEmpty();
	}

	@Test
	public void specLibraryFilesMatchTheirManifestHashes() throws Exception {
		JsonObject documents = specLibraryManifest().getAsJsonObject("documents");
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, JsonElement> doc : documents.entrySet()) {
			boolean hasLinked = false;
			for (JsonElement v : doc.getValue().getAsJsonObject().getAsJsonArray("versions")) {
				JsonObject version = v.getAsJsonObject();
				String role = OIDFJSON.getStringOrNull(version.get("role"));
				hasLinked |= "linked".equals(role);
				String fileName = OIDFJSON.getStringOrNull(version.get("file"));
				if (fileName == null) {
					problems.add(doc.getKey() + ": version has no file in the manifest");
					continue;
				}
				Path file = SPEC_LIBRARY.resolve(fileName);
				if (!Files.isRegularFile(file)) {
					problems.add(doc.getKey() + ": " + file + " is missing");
					continue;
				}
				String expectedSha256 = OIDFJSON.getStringOrNull(version.get("sha256"));
				if (expectedSha256 == null) {
					problems.add(doc.getKey() + ": " + file + " has no sha256 in the manifest");
					continue;
				}
				String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
				if (!sha256.equals(expectedSha256)) {
					problems.add(doc.getKey() + ": " + file + " does not match the sha256 in the manifest");
				}
			}
			if (!hasLinked) {
				problems.add(doc.getKey() + ": no version with role 'linked'");
			}
		}
		assertThat(problems).isEmpty();
	}
}
