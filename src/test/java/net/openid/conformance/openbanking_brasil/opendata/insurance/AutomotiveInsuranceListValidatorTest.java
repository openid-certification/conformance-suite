package net.openid.conformance.openbanking_brasil.opendata.insurance;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

@UseResurce("jsonResponses/opendata/insurance/AutomativeInsuranceListResponse.json")
public class AutomotiveInsuranceListValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void evaluate() {
		run(new AutomotiveInsuranceListValidator());
	}
}
