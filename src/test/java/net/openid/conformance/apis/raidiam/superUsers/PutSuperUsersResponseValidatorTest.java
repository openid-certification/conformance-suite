package net.openid.conformance.apis.raidiam.superUsers;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.superUsers.PutSuperUsersResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class PutSuperUsersResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/superUsers/PutSuperUsersResponse.json")
	public void validateStructure() {
		PutSuperUsersResponseValidator condition = new PutSuperUsersResponseValidator();
		run(condition);
	}

}
