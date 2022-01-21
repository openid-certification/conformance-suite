package net.openid.conformance.openbanking_brasil.opendata.pension;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

@UseResurce("jsonResponses/opendata/pension/SurvivalCoveragesResponse.json")
public class SurvivalCoveragesValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void evaluate() {
		run(new SurvivalCoveragesValidator());
	}
}
