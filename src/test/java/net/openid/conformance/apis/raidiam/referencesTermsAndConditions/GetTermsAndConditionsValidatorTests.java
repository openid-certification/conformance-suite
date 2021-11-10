package net.openid.conformance.apis.raidiam.referencesTermsAndConditions;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.referencesTermsAndConditions.GetTermsAndConditionsByTnCIdValidator;
import net.openid.conformance.raidiam.validators.referencesTermsAndConditions.GetTermsAndConditionsValidator;
import net.openid.conformance.raidiam.validators.referencesTermsAndConditions.PostTermsAndConditionsValidator;
import net.openid.conformance.raidiam.validators.referencesTermsAndConditions.PutTermsAndConditionsByTnCIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class GetTermsAndConditionsValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/referencesTermsAndConditions/PostTermsAndConditionsResponse.json")
	public void validatePostTermsAndConditionsValidator() {
		PostTermsAndConditionsValidator condition = new PostTermsAndConditionsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesTermsAndConditions/PutTermsAndConditionsByTcCIdResponse.json")
	public void validatePutTermsAndConditionsByTnCIdValidator() {
		PutTermsAndConditionsByTnCIdValidator condition = new PutTermsAndConditionsByTnCIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesTermsAndConditions/GetTermsAndConditionsByTnCIdResponse.json")
	public void validateGetTermsAndConditionsByTnCIdValidator() {
		GetTermsAndConditionsByTnCIdValidator condition = new GetTermsAndConditionsByTnCIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesTermsAndConditions/GetTermsAndConditionsResponse.json")
	public void validateGetTermsAndConditionsValidator() {
		GetTermsAndConditionsValidator condition = new GetTermsAndConditionsValidator();
		run(condition);
	}
}
