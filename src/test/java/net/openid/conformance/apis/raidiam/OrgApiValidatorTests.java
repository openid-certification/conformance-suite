package net.openid.conformance.apis.raidiam;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.OrgApiValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OrgApiValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/orgapi_response.json")
	public void validateStructure() {

		OrgApiValidator condition = new OrgApiValidator();

		run(condition);

	}

}
