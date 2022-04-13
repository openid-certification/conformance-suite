package net.openid.conformance.openinsurance.validator.productsServices;

	import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
	import net.openid.conformance.util.UseResurce;
	import org.junit.Test;

public class GetAssistanceGeneralAssetsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/productsServices/assistanceGeneralAssetsStructure.json")
	public void validateStructure() {
		GetAssistanceGeneralAssetsValidator condition = new GetAssistanceGeneralAssetsValidator();
		run(condition);
	}
}
