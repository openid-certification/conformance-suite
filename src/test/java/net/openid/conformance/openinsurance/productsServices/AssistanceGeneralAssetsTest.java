package net.openid.conformance.openinsurance.productsServices;

	import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
	import net.openid.conformance.openinsurance.validator.productsServices.AssistanceGeneralAssets;
	import net.openid.conformance.util.UseResurce;
	import org.junit.Test;

public class AssistanceGeneralAssetsTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/productServices/assistanceGeneralAssets.json")
	public void validateStructure() {
		AssistanceGeneralAssets condition = new AssistanceGeneralAssets();
		run(condition);
	}
}
