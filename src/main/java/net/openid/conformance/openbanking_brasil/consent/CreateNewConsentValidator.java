package net.openid.conformance.openbanking_brasil.consent;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_consents_apis.yaml
 * Api endpoint: /consents
 * Api git hash: 152a9f02d94d612b26dbfffb594640f719e96f70
 */
@ApiName("Create New Consent")
public class CreateNewConsentValidator extends AbstractJsonAssertingCondition {
	private static final Set<String> STATUS_LIST = Sets.newHashSet("TRANSACAO_EFETIVADA", "AUTHORISED", "AWAITING_AUTHORISATION", "REJECTED");
	private static final Set<String> PERMISSIONS_LIST = Sets.newHashSet("RESOURCES_READ", "ACCOUNTS_READ", "ACCOUNTS_BALANCES_READ", "ACCOUNTS_TRANSACTIONS_READ", "ACCOUNTS_OVERDRAFT_LIMITS_READ",
		"CREDIT_CARDS_ACCOUNTS_READ", "CREDIT_CARDS_ACCOUNTS_BILLS_READ", "CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ", "CREDIT_CARDS_ACCOUNTS_LIMITS_READ", "CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ",
		"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ", "CUSTOMERS_PERSONAL_ADITTIONALINFO_READ", "CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ", "CUSTOMERS_BUSINESS_ADITTIONALINFO_READ",
		"FINANCINGS_READ", "FINANCINGS_SCHEDULED_INSTALMENTS_READ", "FINANCINGS_PAYMENTS_READ", "FINANCINGS_WARRANTIES_READ", "INVOICE_FINANCINGS_READ",
		"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ", "INVOICE_FINANCINGS_PAYMENTS_READ", "INVOICE_FINANCINGS_WARRANTIES_READ", "LOANS_READ", "LOANS_SCHEDULED_INSTALMENTS_READ", "LOANS_PAYMENTS_READ", "LOANS_WARRANTIES_READ",
		"UNARRANGED_ACCOUNTS_OVERDRAFT_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body, new ObjectField.Builder(ROOT_PATH).setValidator(this::assertInnerFields).build());
		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("consentId")
				.setPattern("^urn:[a-zA-Z0-9][a-zA-Z0-9-]{0,31}:[a-zA-Z0-9()+,\\-.:=@;$_!*'%\\/?#]+$")
				.setMaxLength(256)
				.build());

		assertField(body,
			new DatetimeField
				.Builder("creationDateTime")
				.setPattern(DatetimeField.ALTERNATIVE_PATTERN)
				.build());

		assertField(body,
			new StringField
				.Builder("status")
				.setEnums(STATUS_LIST)
				.build());

		assertField(body,
			new DatetimeField
				.Builder("statusUpdateDateTime")
				.setPattern(DatetimeField.ALTERNATIVE_PATTERN)
				.build());

		assertField(body,
			new StringArrayField
				.Builder("permissions")
				.setEnums(PERMISSIONS_LIST)
				.setMinItems(1)
				.setMaxItems(30)
				.build());

		assertField(body,
			new DatetimeField
				.Builder("expirationDateTime")
				.setDaysOlderAccepted(730)
				.setPattern(DatetimeField.ALTERNATIVE_PATTERN)
				.build());

		assertField(body,
			new DatetimeField
				.Builder("transactionFromDateTime")
				.setOptional()
				.setPattern(DatetimeField.ALTERNATIVE_PATTERN)
				.build());

		assertField(body,
			new DatetimeField
				.Builder("transactionToDateTime")
				.setOptional()
				.setPattern(DatetimeField.ALTERNATIVE_PATTERN)
				.build());
	}
}
