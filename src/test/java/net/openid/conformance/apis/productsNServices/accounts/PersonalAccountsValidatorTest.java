package net.openid.conformance.apis.productsNServices.accounts;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.account.AccountBalancesResponseValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.accounts.PersonalAccountsValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;
@UseResurce("jsonResponses/productsNServices/accounts/personalAccountsResponseOK.json")
public class PersonalAccountsValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		PersonalAccountsValidator condition = new PersonalAccountsValidator();
		run(condition);
	}
}
