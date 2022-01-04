package net.openid.conformance.openbanking_brasil.investments;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.opendata.capitalizationBonds.CapitalizationBondsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class CapitalizationBondsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/investments/CapitalizationBondsResponse.json")
	public void evaluate() {
		run(new CapitalizationBondsValidator());
	}
}
