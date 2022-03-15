package net.openid.conformance.openbanking_brasil.opendata.investmentsAPI;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.opendata.investments.GetFixedIncomeCreditValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class GetFixedIncomeCreditValidatorTest extends AbstractJsonResponseConditionUnitTest {

    @Test
	@UseResurce("jsonResponses/opendata/investments/GetFixedIncomeCreditResponse.json")
	public void evaluate() {
		run(new GetFixedIncomeCreditValidator());
	}
}
