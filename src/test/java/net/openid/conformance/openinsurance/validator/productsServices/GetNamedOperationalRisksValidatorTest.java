package net.openid.conformance.openinsurance.validator.productsServices;

	import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
	import net.openid.conformance.util.UseResurce;
	import org.junit.Test;

@UseResurce("openinsuranceResponses/productsServices/namedOperationalRisksStructure.json")
public class GetNamedOperationalRisksValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new GetNamedOperationalRisksValidator());
	}
}
