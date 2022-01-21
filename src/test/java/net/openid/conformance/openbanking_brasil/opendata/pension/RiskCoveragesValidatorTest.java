package net.openid.conformance.openbanking_brasil.opendata.pension;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

@UseResurce("jsonResponses/opendata/pension/RiskCoveragesResponse.json")
public class RiskCoveragesValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void evaluate() {
		run(new RiskCoveragesValidator());
	}
}
