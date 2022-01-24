package net.openid.conformance.openbanking_brasil.opendata.acquiringServices;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

@UseResurce("jsonResponses/opendata/acquiringServices/BusinessAcquiringServicesResponse.json")
public class BusinessAcquiringServicesValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void evaluate() {
		run(new BusinessAcquiringServicesValidator());
	}
}
