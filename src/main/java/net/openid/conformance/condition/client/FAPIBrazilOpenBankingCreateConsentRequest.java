package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.models.external.OpenBankingBrasilConsentRequest;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilOpenBankingCreateConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config" )
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {

		String cpf = env.getString("config", "resource.brazilCpf");
		String cnpj = env.getString("config", "resource.brazilCnpj");
		String productType = env.getString("config", "consent.productType");
		if (Strings.isNullOrEmpty(cpf) && Strings.isNullOrEmpty(cnpj)) {
			throw error("A least one of CPF and CNPJ must be specified in the test configuration");
		}

		String[] permissions;

		String consentPermissions = env.getString("consent_permissions");
		if (Strings.isNullOrEmpty(consentPermissions)) {
			// We previously used just:
			//   "ACCOUNTS_READ", "ACCOUNTS_BALANCES_READ", "RESOURCES_READ"
			// however some banks don't support these, resulting in the tests failing.
			// This set is as per https://gitlab.com/openid/conformance-suite/-/issues/927 albeit with one typo
			// corrected to match https://openbanking-brasil.github.io/areadesenvolvedor/swagger/swagger_consents_apis.yaml

			if (productType != null && productType.equals("business")) {
				permissions = new String[]{
					"RESOURCES_READ",
					"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ",
					"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ",
					"ACCOUNTS_READ",
					"ACCOUNTS_BALANCES_READ",
					"ACCOUNTS_OVERDRAFT_LIMITS_READ",
					"ACCOUNTS_TRANSACTIONS_READ",
					"CREDIT_CARDS_ACCOUNTS_LIMITS_READ",
					"CREDIT_CARDS_ACCOUNTS_READ",
					"CREDIT_CARDS_ACCOUNTS_BILLS_READ",
					"CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ",
					"CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ",
					"LOANS_READ",
					"LOANS_WARRANTIES_READ",
					"LOANS_SCHEDULED_INSTALMENTS_READ",
					"LOANS_PAYMENTS_READ",
					"FINANCINGS_READ",
					"FINANCINGS_WARRANTIES_READ",
					"FINANCINGS_SCHEDULED_INSTALMENTS_READ",
					"FINANCINGS_PAYMENTS_READ",
					"UNARRANGED_ACCOUNTS_OVERDRAFT_READ",
					"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ",
					"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ",
					"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ",
					"INVOICE_FINANCINGS_READ",
					"INVOICE_FINANCINGS_WARRANTIES_READ",
					"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ",
					"INVOICE_FINANCINGS_PAYMENTS_READ"
				};

			} else {
				// Personal products
				permissions = new String[]{
					"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ",
					"RESOURCES_READ",
					"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ",
					"ACCOUNTS_READ",
					"ACCOUNTS_BALANCES_READ",
					"ACCOUNTS_OVERDRAFT_LIMITS_READ",
					"ACCOUNTS_TRANSACTIONS_READ",
					"CREDIT_CARDS_ACCOUNTS_LIMITS_READ",
					"CREDIT_CARDS_ACCOUNTS_READ",
					"CREDIT_CARDS_ACCOUNTS_BILLS_READ",
					"CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ",
					"CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ",
					"LOANS_READ",
					"LOANS_WARRANTIES_READ",
					"LOANS_SCHEDULED_INSTALMENTS_READ",
					"LOANS_PAYMENTS_READ",
					"FINANCINGS_READ",
					"FINANCINGS_WARRANTIES_READ",
					"FINANCINGS_SCHEDULED_INSTALMENTS_READ",
					"FINANCINGS_PAYMENTS_READ",
					"UNARRANGED_ACCOUNTS_OVERDRAFT_READ",
					"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ",
					"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ",
					"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ",
					"INVOICE_FINANCINGS_READ",
					"INVOICE_FINANCINGS_WARRANTIES_READ",
					"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ",
					"INVOICE_FINANCINGS_PAYMENTS_READ"
				};
			}
		} else {
			permissions = consentPermissions.split("\\W");
		}

		JsonObject e = new JsonObject();
		e.add("requested_permissions", new Gson().toJsonTree(permissions));

		env.putObject("brazil_consent", e);

		OpenBankingBrasilConsentRequest consentRequest =
			new OpenBankingBrasilConsentRequest(cpf, cnpj, permissions);
		JsonObject requestObject = consentRequest.toJson();

		env.putObject("consent_endpoint_request", requestObject);

		logSuccess(args("consent_endpoint_request", requestObject));

		return env;
	}

}
