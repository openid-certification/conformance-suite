package net.openid.conformance.openinsurance.testmodule.consents.v1;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.models.external.OpenBankingBrasilConsentRequest;
import net.openid.conformance.testmodule.Environment;



public class OPINBrazilCreateConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config" )
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {

		String cpf;
		String cnpj = null;

		String productType = env.getString("config", "consent.productType");
		if (Strings.isNullOrEmpty(productType)) {
			throw error("Product type (Business or Personal) must be specified in the test configuration");
		}

		Boolean operationalLimitTest = env.getBoolean("operational_limit_consent");
		if (operationalLimitTest != null && operationalLimitTest){
			if (productType.equals("business")) {
				log("Product type business was chosen, proceeding with Business CPF and Business CNPJ.");

				cpf  = env.getString("config", "resource.brazilCpfOperationalBusiness");
				cnpj = env.getString("config", "resource.brazilCnpjOperationalBusiness");

				if (Strings.isNullOrEmpty(cnpj)) {
					throw error("The operational limit CNPJ must be provided in the test configuration");
				}
			} else {
				log("Product type Personal was chosen, proceeding with Personal CPF.");
				cpf  = env.getString("config", "resource.brazilCpfOperationalPersonal");
			}

			if (Strings.isNullOrEmpty(cpf)) {
				throw error("The operational limit CPF must be provided in the test configuration");
			}
		} else {
			cpf = env.getString("config", "resource.brazilCpf");
			cnpj = env.getString("config", "resource.brazilCnpj");

			if (Strings.isNullOrEmpty(cpf) && Strings.isNullOrEmpty(cnpj)) {
				throw error("A least one of CPF and CNPJ must be specified in the test configuration");
			}
		}

		String[] permissions;

		String consentPermissions = env.getString("consent_permissions");
		if (Strings.isNullOrEmpty(consentPermissions)) {
			if (productType.equals("business")) {
				permissions = new String[]{
					"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ",
					"CUSTOMERS_BUSINESS_QUALIFICATION_READ",
					"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ",
					"CAPITALIZATION_TITLES_READ",
					"CAPITALIZATION_TITLES_POLICYINFO_READ",
					"CAPITALIZATION_TITLES_PREMIUM_READ",
					"CAPITALIZATION_TITLES_CLAIM_READ",
					"PENSION_RISK_READ",
					"PENSION_RISK_CONTRACTINFO_READ",
					"PENSION_RISK_CONTRIBUTIONS_READ",
					"DAMAGES_AND_PEOPLE_PATRIMONIAL_READ",
					"DAMAGES_AND_PEOPLE_PATRIMONIAL_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_PATRIMONIAL_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_PATRIMONIAL_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_AERONAUTICAL_READ",
					"DAMAGES_AND_PEOPLE_AERONAUTICAL_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_AERONAUTICAL_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_AERONAUTICAL_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_NAUTICAL_READ",
					"DAMAGES_AND_PEOPLE_NAUTICAL_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_NAUTICAL_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_NAUTICAL_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_NUCLEAR_READ",
					"DAMAGES_AND_PEOPLE_NUCLEAR_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_NUCLEAR_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_NUCLEAR_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_OIL_READ",
					"DAMAGES_AND_PEOPLE_OIL_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_OIL_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_OIL_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_RESPONSIBILITY_READ",
					"DAMAGES_AND_PEOPLE_RESPONSIBILITY_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_RESPONSIBILITY_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_RESPONSIBILITY_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_TRANSPORT_READ",
					"DAMAGES_AND_PEOPLE_TRANSPORT_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_TRANSPORT_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_TRANSPORT_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_FINANCIAL_READ",
					"DAMAGES_AND_PEOPLE_FINANCIAL_RISKS_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_FINANCIAL_RISKS_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_FINANCIAL_RISKS_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_RURAL_READ",
					"DAMAGES_AND_PEOPLE_RURAL_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_RURAL_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_RURAL_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_AUTO_READ",
					"DAMAGES_AND_PEOPLE_AUTO_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_AUTO_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_AUTO_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_HOUSING_READ",
					"DAMAGES_AND_PEOPLE_HOUSING_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_HOUSING_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_HOUSING_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_READ",
					"DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_PERSON_READ",
					"DAMAGES_AND_PEOPLE_PERSON_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_PERSON_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_PERSON_CLAIM_READ",
					"RESOURCES_READ"
				};

			} else {
				// Personal products
				permissions = new String[]{
					"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ",
					"CUSTOMERS_PERSONAL_QUALIFICATION_READ",
					"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ",
					"CAPITALIZATION_TITLES_READ",
					"CAPITALIZATION_TITLES_POLICYINFO_READ",
					"CAPITALIZATION_TITLES_PREMIUM_READ",
					"CAPITALIZATION_TITLES_CLAIM_READ",
					"PENSION_RISK_READ",
					"PENSION_RISK_CONTRACTINFO_READ",
					"PENSION_RISK_CONTRIBUTIONS_READ",
					"DAMAGES_AND_PEOPLE_PATRIMONIAL_READ",
					"DAMAGES_AND_PEOPLE_PATRIMONIAL_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_PATRIMONIAL_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_PATRIMONIAL_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_AERONAUTICAL_READ",
					"DAMAGES_AND_PEOPLE_AERONAUTICAL_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_AERONAUTICAL_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_AERONAUTICAL_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_NAUTICAL_READ",
					"DAMAGES_AND_PEOPLE_NAUTICAL_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_NAUTICAL_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_NAUTICAL_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_NUCLEAR_READ",
					"DAMAGES_AND_PEOPLE_NUCLEAR_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_NUCLEAR_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_NUCLEAR_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_OIL_READ",
					"DAMAGES_AND_PEOPLE_OIL_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_OIL_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_OIL_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_RESPONSIBILITY_READ",
					"DAMAGES_AND_PEOPLE_RESPONSIBILITY_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_RESPONSIBILITY_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_RESPONSIBILITY_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_TRANSPORT_READ",
					"DAMAGES_AND_PEOPLE_TRANSPORT_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_TRANSPORT_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_TRANSPORT_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_FINANCIAL_READ",
					"DAMAGES_AND_PEOPLE_FINANCIAL_RISKS_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_FINANCIAL_RISKS_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_FINANCIAL_RISKS_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_RURAL_READ",
					"DAMAGES_AND_PEOPLE_RURAL_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_RURAL_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_RURAL_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_AUTO_READ",
					"DAMAGES_AND_PEOPLE_AUTO_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_AUTO_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_AUTO_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_HOUSING_READ",
					"DAMAGES_AND_PEOPLE_HOUSING_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_HOUSING_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_HOUSING_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_READ",
					"DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_CLAIM_READ",
					"DAMAGES_AND_PEOPLE_PERSON_READ",
					"DAMAGES_AND_PEOPLE_PERSON_POLICYINFO_READ",
					"DAMAGES_AND_PEOPLE_PERSON_PREMIUM_READ",
					"DAMAGES_AND_PEOPLE_PERSON_CLAIM_READ",
					"RESOURCES_READ"
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
