package net.openid.conformance.apis.raidiam.superUsers;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.superUsers.PostSuperUsersResponseValidator;
import net.openid.conformance.raidiam.validators.users.GetUsersResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class PostSuperUsersResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/superUsers/PostSuperUsersResponse.json")
	public void validateStructure() {
		PostSuperUsersResponseValidator condition = new PostSuperUsersResponseValidator();
		run(condition);
	}

}
