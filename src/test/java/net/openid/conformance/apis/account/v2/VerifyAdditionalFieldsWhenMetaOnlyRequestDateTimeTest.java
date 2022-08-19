package net.openid.conformance.apis.account.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.account.v2.AccountTransactionsValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.support.VerifyAdditionalFieldsWhenMetaOnlyRequestDateTime;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


public class VerifyAdditionalFieldsWhenMetaOnlyRequestDateTimeTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/account/accountV2/transactionsV2/accountTransactionsResponseOK.json")
	public void validateStructure() {
		environment.putString("metaOnlyRequestDateTime", "true");
		run(new VerifyAdditionalFieldsWhenMetaOnlyRequestDateTime());
	}

	@Test
	@UseResurce("jsonResponses/account/accountV2/transactionsV2/accountTransactionsResponseWrongMetaOnlyRequestDateTimeTest.json")
	public void validateStructureWrongMeta() {
		environment.putString("metaOnlyRequestDateTime", "true");
		run(new VerifyAdditionalFieldsWhenMetaOnlyRequestDateTime());
	}
}
