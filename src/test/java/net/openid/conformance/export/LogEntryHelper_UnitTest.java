package net.openid.conformance.export;

import com.google.gson.Gson;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LogEntryHelper_UnitTest {

	@Test
	public void brazilCibaRequirementLabelsResolveToBeta1SpecSections() {
		LogEntryHelper helper = new LogEntryHelper(new Document(), new Gson());
		String spec = "https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/1799979087/" +
			"EN+Open+Finance+Brasil+Client+Initiated+Backchannel+Authentication+-+v2.1.0-beta1#";

		assertThat(helper.getRequirementLink("BrazilCIBA-6.2.2")).isEqualTo(spec + "6.2.2");
		assertThat(helper.getRequirementLink("BrazilCIBA-6.2.5")).isEqualTo(spec + "6.2.5");
		assertThat(helper.getRequirementLink("BrazilCIBA-6.3.7")).isEqualTo(spec + "6.3.7");
	}

	@Test
	public void brazilFapi22RequirementLabelsResolveToFinalSpecSections() {
		LogEntryHelper helper = new LogEntryHelper(new Document(), new Gson());
		String spec = "https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/1675395195/" +
			"EN+Open+Finance+Brasil+Financial-grade+API+Security+Profile+-+v2.2.0#";

		assertThat(helper.getRequirementLink("BrazilOB22-5.1.1-1")).isEqualTo(spec + "5.1.1-1");
		assertThat(helper.getRequirementLink("BrazilOB22-6.2")).isEqualTo(spec + "6.2");
		assertThat(helper.getRequirementLink("BrazilOB22-6.3")).isEqualTo(spec + "6.3");
	}
}
