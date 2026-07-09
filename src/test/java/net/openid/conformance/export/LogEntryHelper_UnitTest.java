package net.openid.conformance.export;

import com.google.gson.Gson;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LogEntryHelper_UnitTest {

	@Test
	public void brazilCibaRequirementLabelsDoNotResolveToStaleDraftLink() {
		LogEntryHelper helper = new LogEntryHelper(new Document(), new Gson());

		assertThat(helper.getRequirementLink("BrazilCIBA-6.2.5")).isEmpty();
	}
}
