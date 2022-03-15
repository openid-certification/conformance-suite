package net.openid.conformance.openbanking_brasil.opendata.investmentsAPI;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.opendata.investments.GetFixedIncomeBankValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class GetFixedIncomeBankValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/opendata/investments/GetFixedIncomeBankResponse.json")
	public void evaluate() {
		run(new GetFixedIncomeBankValidator());
	}
}
