package net.openid.conformance.apis.raidiam;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.OrgApiStructureValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OrgApiValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/orgapi_response.json")
	public void validateStructure() {

		OrgApiStructureValidator condition = new OrgApiStructureValidator();

		run(condition);

	}

}
