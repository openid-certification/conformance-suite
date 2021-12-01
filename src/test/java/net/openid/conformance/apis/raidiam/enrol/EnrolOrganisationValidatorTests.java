package net.openid.conformance.apis.raidiam.enrol;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.enrol.PostEnrolOrganisationValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class EnrolOrganisationValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/enrol/GetEnrolResponse.json")
	public void validatePostEnrolOrganisationValidator() {
		PostEnrolOrganisationValidator condition = new PostEnrolOrganisationValidator();
		run(condition);
	}

}
