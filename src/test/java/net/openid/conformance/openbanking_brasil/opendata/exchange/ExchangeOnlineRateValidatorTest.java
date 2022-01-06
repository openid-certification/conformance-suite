package net.openid.conformance.openbanking_brasil.opendata.exchange;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

@UseResurce("jsonResponses/opendata/exchange/ExchangeOnlineRateResponse.json")
public class ExchangeOnlineRateValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void evaluate() {
		run(new ExchangeOnlineRateValidator());
	}
}
