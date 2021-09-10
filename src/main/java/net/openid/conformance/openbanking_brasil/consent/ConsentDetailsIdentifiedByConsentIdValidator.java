package net.openid.conformance.openbanking_brasil.consent;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * This is validator for API- Obtain consent details identified by consentId
 * https://openbanking-brasil.github.io/areadesenvolvedor/?java#obter-detalhes-do-consentimento-identificado-por-consentid
 */

@ApiName("Consent Details Identified By ConsentId")
public class ConsentDetailsIdentifiedByConsentIdValidator extends AbstractJsonAssertingCondition {
	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertJsonObject(body, ROOT_PATH, this::assertInnerFields);

		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		Set<String> statusList = Sets.newHashSet("TRANSACAO_EFETIVADA", "AUTHORISED", "AWAITING_AUTHORISATION", "REJECTED");
		Set<String> permissionsList = Sets.newHashSet("RESOURCES_READ", "ACCOUNTS_READ", "ACCOUNTS_BALANCES_READ", "ACCOUNTS_TRANSACTIONS_READ", "ACCOUNTS_OVERDRAFT_LIMITS_READ",
			"CREDIT_CARDS_ACCOUNTS_READ", "CREDIT_CARDS_ACCOUNTS_BILLS_READ", "CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ", "CREDIT_CARDS_ACCOUNTS_LIMITS_READ", "CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ",
			"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ", "CUSTOMERS_PERSONAL_ADITTIONALINFO_READ", "CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ", "CUSTOMERS_BUSINESS_ADITTIONALINFO_READ",
			"FINANCINGS_READ", "FINANCINGS_SCHEDULED_INSTALMENTS_READ", "FINANCINGS_PAYMENTS_READ", "FINANCINGS_WARRANTIES_READ", "INVOICE_FINANCINGS_READ",
			"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ", "INVOICE_FINANCINGS_PAYMENTS_READ", "INVOICE_FINANCINGS_WARRANTIES_READ", "LOANS_READ", "LOANS_SCHEDULED_INSTALMENTS_READ", "LOANS_PAYMENTS_READ", "LOANS_WARRANTIES_READ",
			"UNARRANGED_ACCOUNTS_OVERDRAFT_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ");

		assertField(body,
			new StringField
				.Builder("consentId")
				.setPattern("^urn:[a-zA-Z0-9][a-zA-Z0-9-]{0,31}:[a-zA-Z0-9()+,\\-.:=@;$_!*'%\\/?#]+$")
				.setMaxLength(256)
				.build());

		assertField(body,
			new DatetimeField
				.Builder("creationDateTime")
				.build());

		assertField(body,
			new StringField
				.Builder("status")
				.setEnums(statusList)
				.build());

		assertField(body,
			new DatetimeField
				.Builder("statusUpdateDateTime")
				.setPattern(DatetimeField.ALTERNATIVE_PATTERN)
				.build());

		assertField(body,
			new StringArrayField
				.Builder("permissions")
				.setEnums(permissionsList)
				.build());

		assertField(body,
			new DatetimeField
				.Builder("expirationDateTime")
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

