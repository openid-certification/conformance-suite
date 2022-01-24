package net.openid.conformance.openbanking_brasil.opendata.investmentsAPI;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.opendata.investmentsAPI.validator.GetVariableIncomeValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class GetVariableIncomeValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/opendata/investments/GetVariableIncomeResponse.json")
	public void validateValidator() {
		run(new GetVariableIncomeValidator());
	}
}
