package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class EnsureConsentHasNoTransactionFromToDateTimeTest extends AbstractJsonResponseConditionUnitTest {

	@UseResurce("jsonResponses/consent/createConsentResponse/v2/createConsentResponse.json")
	@Test
	public void happyPath() {
		EnsureConsentHasNoTransactionFromToDateTime cond = new EnsureConsentHasNoTransactionFromToDateTime();
		run(cond);
	}

	@UseResurce("jsonResponses/consent/createConsentResponse/v2/createConsentResponseTransactionFromToDateTime.json")
	@Test
	public void unhappyPath() {
		EnsureConsentHasNoTransactionFromToDateTime cond = new EnsureConsentHasNoTransactionFromToDateTime();
		ConditionError error = runAndFail(cond);
		assertThat(error.getMessage(), containsString("transactionFromDateTime and transactionToDateTime can not be in the consent response."));
	}

}
