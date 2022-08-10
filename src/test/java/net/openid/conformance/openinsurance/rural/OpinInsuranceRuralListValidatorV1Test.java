package net.openid.conformance.openinsurance.rural;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.rural.v1.OpinInsuranceRuralListValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class OpinInsuranceRuralListValidatorV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/rural/ruralResponseStructure.json")
	public void validateStructure() {
		run(new OpinInsuranceRuralListValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/rural/ruralResponseStructure(FieldNotFound).json")
	public void validateFieldNotFound() {
		OpinInsuranceRuralListValidatorV1 condition = new OpinInsuranceRuralListValidatorV1();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createElementNotFoundMessage("brand", condition.getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/rural/ruralResponseStructure(LinksNOTFound).json")
	public void linksNOTFound() {
		OpinInsuranceRuralListValidatorV1 condition = new OpinInsuranceRuralListValidatorV1();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createElementNotFoundMessage("links", condition.getApiName())));
	}

}
