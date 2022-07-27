package net.openid.conformance.openbanking_brasil.consent.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: swagger/openBanking/swagger-consents-api-v2.yaml
 * Api endpoint: /consents
 * Api version: 2.0.1.final
 **/
@ApiName("Create New Consent V2")
public class CreateNewConsentValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaConsentValidatorV2 linksAndMetaValidator = new LinksAndMetaConsentValidatorV2(this);
	private static final Set<String> STATUS_LIST = SetUtils.createSet("AUTHORISED, AWAITING_AUTHORISATION, REJECTED");
	private static final Set<String> PERMISSIONS_LIST = SetUtils.createSet("ACCOUNTS_READ, ACCOUNTS_BALANCES_READ, ACCOUNTS_TRANSACTIONS_READ, ACCOUNTS_OVERDRAFT_LIMITS_READ, CREDIT_CARDS_ACCOUNTS_READ, CREDIT_CARDS_ACCOUNTS_BILLS_READ, CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ, CREDIT_CARDS_ACCOUNTS_LIMITS_READ, CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ, CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ, CUSTOMERS_PERSONAL_ADITTIONALINFO_READ, CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ, CUSTOMERS_BUSINESS_ADITTIONALINFO_READ, FINANCINGS_READ, FINANCINGS_SCHEDULED_INSTALMENTS_READ, FINANCINGS_PAYMENTS_READ, FINANCINGS_WARRANTIES_READ, INVOICE_FINANCINGS_READ, INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ, INVOICE_FINANCINGS_PAYMENTS_READ, INVOICE_FINANCINGS_WARRANTIES_READ, LOANS_READ, LOANS_SCHEDULED_INSTALMENTS_READ, LOANS_PAYMENTS_READ, LOANS_WARRANTIES_READ, UNARRANGED_ACCOUNTS_OVERDRAFT_READ, UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ, UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ, UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ, RESOURCES_READ");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectField
				.Builder("data")
				.setValidator(this::assertInnerFields)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
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
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("status")
				.setEnums(STATUS_LIST)
				.build());

		assertField(body,
			new DatetimeField
				.Builder("statusUpdateDateTime")
				.setMaxLength(20)
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
				.setMaxLength(20)
				.build());
	}
}
