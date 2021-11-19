package net.openid.conformance.apis.raidiam.superUsers;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.superUsers.GetSuperUsersResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class GetSuperUsersResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/superUsers/GetSuperUsersResponse.json")
	public void validateStructure() {
		GetSuperUsersResponseValidator condition = new GetSuperUsersResponseValidator();
		run(condition);
	}

}
