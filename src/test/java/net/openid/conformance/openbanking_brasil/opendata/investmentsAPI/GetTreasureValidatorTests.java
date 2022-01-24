package net.openid.conformance.openbanking_brasil.opendata.investmentsAPI;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.opendata.investmentsAPI.validator.GetTreasureValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class GetTreasureValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/opendata/investments/GetTreasureResponse.json")
	public void validateValidator() {
		run(new GetTreasureValidator());
	}
}
