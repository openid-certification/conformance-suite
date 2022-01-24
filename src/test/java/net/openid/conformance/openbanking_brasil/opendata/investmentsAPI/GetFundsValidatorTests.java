package net.openid.conformance.openbanking_brasil.opendata.investmentsAPI;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.opendata.investmentsAPI.validator.GetFundsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class GetFundsValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/opendata/investments/GetFundsResponse.json")
	public void validateValidator() {
		run(new GetFundsValidator());
	}
}
