package net.openid.conformance.openbanking_brasil.opendata.insurance;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.junit.Assert.*;

@UseResurce("jsonResponses/opendata/insurance/HomeInsuranceListResponse.json")
public class HomeInsuranceListValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void evaluate() {
		run(new HomeInsuranceListValidator());
	}
}
