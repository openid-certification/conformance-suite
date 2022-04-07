package net.openid.conformance.openinsurance.validator.productsServices;

	import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
	import net.openid.conformance.util.UseResurce;
	import org.junit.Test;

@UseResurce("openinsuranceResponses/productsServices/lostProfitStructure.json")
public class GetLostProfitValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new GetLostProfitValidator());
	}
}
