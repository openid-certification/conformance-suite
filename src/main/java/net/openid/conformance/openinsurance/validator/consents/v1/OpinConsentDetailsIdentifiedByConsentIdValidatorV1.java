package net.openid.conformance.openinsurance.validator.consents.v1;

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
 * Api url: swagger/openinsurance/consents/v1/swagger-consents-api-v1.yaml
 * Api endpoint: /consents/{consentId}
 * Api version: 1.0.4
 **/
@ApiName("Consent Details Identified By Consent Id V1")
public class OpinConsentDetailsIdentifiedByConsentIdValidatorV1 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaConsentValidatorV1 linksAndMetaValidator = new LinksAndMetaConsentValidatorV1(this);
	private static final Set<String> STATUS_LIST = SetUtils.createSet("AUTHORISED, AWAITING_AUTHORISATION, REJECTED");
	private static final Set<String> PERMISSIONS_LIST = SetUtils.createSet("CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ, CUSTOMERS_PERSONAL_ADITTIONALINFO_READ, CUSTOMERS_PERSONAL_QUALIFICATION_READ, CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ, CUSTOMERS_BUSINESS_QUALIFICATION_READ, CUSTOMERS_BUSINESS_ADITTIONALINFO_READ, CAPITALIZATION_TITLES_READ, CAPITALIZATION_TITLES_POLICYINFO_READ, CAPITALIZATION_TITLES_PREMIUM_READ, CAPITALIZATION_TITLES_CLAIM_READ, PENSION_RISK_READ, PENSION_RISK_CONTRACTINFO_READ, PENSION_RISK_CONTRIBUTIONS_READ, DAMAGES_AND_PEOPLE_PATRIMONIAL_READ, DAMAGES_AND_PEOPLE_PATRIMONIAL_POLICYINFO_READ, DAMAGES_AND_PEOPLE_PATRIMONIAL_PREMIUM_READ, DAMAGES_AND_PEOPLE_PATRIMONIAL_CLAIM_READ, DAMAGES_AND_PEOPLE_AERONAUTICAL_READ, DAMAGES_AND_PEOPLE_AERONAUTICAL_POLICYINFO_READ, DAMAGES_AND_PEOPLE_AERONAUTICAL_PREMIUM_READ, DAMAGES_AND_PEOPLE_AERONAUTICAL_CLAIM_READ, DAMAGES_AND_PEOPLE_NAUTICAL_READ, DAMAGES_AND_PEOPLE_NAUTICAL_POLICYINFO_READ, DAMAGES_AND_PEOPLE_NAUTICAL_PREMIUM_READ, DAMAGES_AND_PEOPLE_NAUTICAL_CLAIM_READ, DAMAGES_AND_PEOPLE_NUCLEAR_READ, DAMAGES_AND_PEOPLE_NUCLEAR_POLICYINFO_READ, DAMAGES_AND_PEOPLE_NUCLEAR_PREMIUM_READ, DAMAGES_AND_PEOPLE_NUCLEAR_CLAIM_READ, DAMAGES_AND_PEOPLE_OIL_READ, DAMAGES_AND_PEOPLE_OIL_POLICYINFO_READ, DAMAGES_AND_PEOPLE_OIL_PREMIUM_READ, DAMAGES_AND_PEOPLE_OIL_CLAIM_READ, DAMAGES_AND_PEOPLE_RESPONSIBILITY_READ, DAMAGES_AND_PEOPLE_RESPONSIBILITY_POLICYINFO_READ, DAMAGES_AND_PEOPLE_RESPONSIBILITY_PREMIUM_READ, DAMAGES_AND_PEOPLE_RESPONSIBILITY_CLAIM_READ, DAMAGES_AND_PEOPLE_TRANSPORT_READ, DAMAGES_AND_PEOPLE_TRANSPORT_POLICYINFO_READ, DAMAGES_AND_PEOPLE_TRANSPORT_PREMIUM_READ, DAMAGES_AND_PEOPLE_TRANSPORT_CLAIM_READ, DAMAGES_AND_PEOPLE_FINANCIAL_READ, DAMAGES_AND_PEOPLE_FINANCIAL_RISKS_POLICYINFO_READ, DAMAGES_AND_PEOPLE_FINANCIAL_RISKS_PREMIUM_READ, DAMAGES_AND_PEOPLE_FINANCIAL_RISKS_CLAIM_READ, DAMAGES_AND_PEOPLE_RURAL_READ, DAMAGES_AND_PEOPLE_RURAL_POLICYINFO_READ, DAMAGES_AND_PEOPLE_RURAL_PREMIUM_READ, DAMAGES_AND_PEOPLE_RURAL_CLAIM_READ, DAMAGES_AND_PEOPLE_AUTO_READ, DAMAGES_AND_PEOPLE_AUTO_POLICYINFO_READ, DAMAGES_AND_PEOPLE_AUTO_PREMIUM_READ, DAMAGES_AND_PEOPLE_AUTO_CLAIM_READ, DAMAGES_AND_PEOPLE_HOUSING_READ, DAMAGES_AND_PEOPLE_HOUSING_POLICYINFO_READ, DAMAGES_AND_PEOPLE_HOUSING_PREMIUM_READ, DAMAGES_AND_PEOPLE_HOUSING_CLAIM_READ, DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_READ, DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_POLICYINFO_READ, DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_PREMIUM_READ, DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_CLAIM_READ, DAMAGES_AND_PEOPLE_PERSON_READ, DAMAGES_AND_PEOPLE_PERSON_POLICYINFO_READ, DAMAGES_AND_PEOPLE_PERSON_PREMIUM_READ, DAMAGES_AND_PEOPLE_PERSON_CLAIM_READ, RESOURCES_READ");

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

		assertField(body,
			new DatetimeField
				.Builder("transactionFromDateTime")
				.setMaxLength(20)
				.setOptional()
				.build());

		assertField(body,
			new DatetimeField
				.Builder("transactionToDateTime")
				.setMaxLength(20)
				.setOptional()
				.build());
	}
}
