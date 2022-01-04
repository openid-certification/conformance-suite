package net.openid.conformance.openbanking_brasil.opendata.exchange;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class ExchangeVetValueValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/opendata/exchange/ExchangeVetValueResponse.json")
	public void evaluate() {
		run(new ExchangeVetValueValidator());
	}
}
