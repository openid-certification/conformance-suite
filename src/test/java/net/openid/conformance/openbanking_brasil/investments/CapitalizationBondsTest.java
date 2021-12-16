package net.openid.conformance.openbanking_brasil.investments;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class CapitalizationBondsTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/investments/CapitalizationBondsResponse.json")
	public void evaluate() {
		run(new CapitalizationBonds());
	}
}
