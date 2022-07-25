package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

@UseResurce("jsonResponses/resourcesAPI/resourcesAPIResponseMultipleResources.json")
public class ValidateMetaOnlyRequestDateTimeTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new ValidateMetaOnlyRequestDateTime());
	}

}
