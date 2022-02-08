package net.openid.conformance.apis.raidiam.enrol;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.raidiam.validators.enrol.PostEnrolOrganisationValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class EnrolOrganisationValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/enrol/GetEnrolResponse.json")
	public void validatePostEnrolOrganisationValidator() {
		PostEnrolOrganisationValidator condition = new PostEnrolOrganisationValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/enrol/GetEnrolResponse_maxLengthError.json")
	public void validateStructureWithWrongMaxLength() {
		PostEnrolOrganisationValidator condition = new PostEnrolOrganisationValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("GrantTypes",
			condition.getApiName())));
	}

}
