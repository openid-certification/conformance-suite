package net.openid.conformance.openbanking_brasil.opendata.capitalizationBounds;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.opendata.capitalizationBonds.CapitalizationBondsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class CapitalizationBondsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/opendata/capitalizationBonds/CapitalizationBondsResponse.json")
	public void evaluate() {
		run(new CapitalizationBondsValidator());
	}
}
