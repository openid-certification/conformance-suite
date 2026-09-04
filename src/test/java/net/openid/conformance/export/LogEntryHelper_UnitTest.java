package net.openid.conformance.export;

import com.google.gson.Gson;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
}
