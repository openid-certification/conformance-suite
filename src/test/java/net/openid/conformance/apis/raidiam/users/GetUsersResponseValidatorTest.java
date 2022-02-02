package net.openid.conformance.apis.raidiam.users;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.users.GetUsersResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class GetUsersResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/users/getUsersResponse.json")
	public void validateStructure() {
		GetUsersResponseValidator condition = new GetUsersResponseValidator();
		run(condition);
	}

}
