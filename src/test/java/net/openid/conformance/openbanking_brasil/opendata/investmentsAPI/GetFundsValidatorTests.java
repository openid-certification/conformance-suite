package net.openid.conformance.openbanking_brasil.opendata.investmentsAPI;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.opendata.investments.GetFundsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class GetFundsValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/opendata/investments/GetFundsResponse.json")
	public void validateValidator() {
		run(new GetFundsValidator());
	}

	@Test
	@UseResurce("jsonResponses/opendata/investments/GetFundsResponseError(MustNOTBeEmpty).json")
	public void validateValidatorDataMustNotBeEmpty() {
		GetFundsValidator condition = new GetFundsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
				.createArrayMustNotBeEmptyMessage("data", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/opendata/investments/GetFundsResponseError(DataMustBeProvided).json")
	public void validateValidatorDataMustBeProvided() {
		GetFundsValidator condition = new GetFundsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
				.createElementNotFoundMessage("data", condition.getApiName())));
	}
}
