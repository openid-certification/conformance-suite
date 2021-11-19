package net.openid.conformance.apis.raidiam.users;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.users.GetUsersHistoryResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class GetUsersHistoryResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/users/getUsersHistoryResponse.json")
	public void validateStructure() {
		GetUsersHistoryResponseValidator condition = new GetUsersHistoryResponseValidator();
		run(condition);
	}

}
