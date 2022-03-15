package net.openid.conformance.openbanking_brasil.opendata.capitalizationBounds;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.opendata.capitalizationBonds.CapitalizationBondsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class CapitalizationBondsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/opendata/capitalizationBonds/CapitalizationBondsResponse.json")
	public void evaluate() {
		run(new CapitalizationBondsValidator());
	}

	@Test
	@UseResurce("jsonResponses/opendata/capitalizationBonds/CapitalizationBondsResponse(totalRecordsNOTFound).json")
	public void totalRecordsNOTFound() {
		CapitalizationBondsValidator condition = new CapitalizationBondsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
				.createElementNotFoundMessage("totalRecords", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/opendata/capitalizationBonds/CapitalizationBondsResponse(linkSelfNOTFound).json")
	public void linksSelfNOTFound() {
		CapitalizationBondsValidator condition = new CapitalizationBondsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
				.createElementNotFoundMessage("self", condition.getApiName())));
	}
}
