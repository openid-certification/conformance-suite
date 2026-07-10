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

		assertThat(helper.getRequirementLink("BrazilCIBA-5.2.2")).isEqualTo(spec + "5.2.2");
		assertThat(helper.getRequirementLink("BrazilCIBA-6.2.5")).isEqualTo(spec + "6.2.5");
		assertThat(helper.getRequirementLink("BrazilCIBA-6.3.7")).isEqualTo(spec + "6.3.7");
	}
}
