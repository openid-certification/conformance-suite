package net.openid.conformance.apis.raidiam.superUsers;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.raidiam.validators.superUsers.GetSuperUsersResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class GetSuperUsersResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/superUsers/GetSuperUsersResponse.json")
	public void validateStructure() {
		GetSuperUsersResponseValidator condition = new GetSuperUsersResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/superUsers/GetSuperUsersResponse_FieldNullError.json")
	public void validateStructureFieldCantBeNull() {
		GetSuperUsersResponseValidator condition = new GetSuperUsersResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementCantBeNullMessage("Status",
			condition.getApiName())));
	}

}
