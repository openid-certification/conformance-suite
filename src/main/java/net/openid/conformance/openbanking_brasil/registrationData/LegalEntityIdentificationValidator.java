package net.openid.conformance.openbanking_brasil.registrationData;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
/**
 * This is validator for API-Dados Cadastrais | Identificacao pessoa jurídica
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#identificacao-pessoa-juridica">Identificação Pessoa Jurídica </a>
 **/

public class LegalEntityIdentificationValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data[0].businessId");
		assertHasStringField(body, "$.data[0].updateDateTime");
		assertHasStringField(body, "$.data[0].brandName");
		assertHasStringField(body, "$.data[0].cnpjNumber");
		assertHasStringField(body, "$.data[0].tradeName");
		assertHasStringArrayField(body, "$.data[0].companyCnpjNumber");
		assertHasStringField(body, "$.data[0].incorporationDate");

		assertHasField(body, "$.data[0].parties[0]");
		assertHasStringField(body, "$.data[0].parties[0].type");
		assertHasStringField(body, "$.data[0].parties[0].personType");
		assertHasStringField(body, "$.data[0].parties[0].civilName");
		assertHasStringField(body, "$.data[0].parties[0].socialName");
		assertHasStringField(body, "$.data[0].parties[0].companyName");
		assertHasStringField(body, "$.data[0].parties[0].startDate");
		assertHasStringField(body, "$.data[0].parties[0].shareholding");
		assertHasStringField(body, "$.data[0].parties[0].documentType");
		assertHasStringField(body, "$.data[0].parties[0].documentNumber");
		assertHasStringField(body, "$.data[0].parties[0].documentCountry");
		assertHasStringField(body, "$.data[0].parties[0].documentExpirationDate");
		assertHasStringField(body, "$.data[0].parties[0].tradeName");
		assertHasStringField(body, "$.data[0].parties[0].documentAdditionalInfo");
		assertHasStringField(body, "$.data[0].parties[0].documentIssueDate");

		assertHasField(body,"$.data[0].contacts.postalAddresses[0]");
		assertHasBooleanField(body, "$.data[0].contacts.postalAddresses[0].isMain");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].address");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].districtName");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].townName");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].countrySubDivision.value");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].postCode");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].country");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].additionalInfo");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].ibgeTownCode");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].countryCode");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].geographicCoordinates.value");

		assertHasField(body,"$.data[0].contacts.phones[0]");
		assertHasBooleanField(body, "$.data[0].contacts.phones[0].isMain");
		assertHasStringField(body, "$.data[0].contacts.phones[0].type.value");
		assertHasStringField(body, "$.data[0].contacts.phones[0].countryCallingCode");
		assertHasStringField(body, "$.data[0].contacts.phones[0].areaCode.value");
		assertHasStringField(body, "$.data[0].contacts.phones[0].number");
		assertHasStringField(body, "$.data[0].contacts.phones[0].phoneExtension");
		assertHasStringField(body, "$.data[0].contacts.phones[0].additionalInfo");

		assertHasField(body,"$.data[0].contacts.emails[0]");
		assertHasBooleanField(body, "$.data[0].contacts.emails[0].isMain");
		assertHasStringField(body, "$.data[0].contacts.emails[0].email");
		assertHasStringField(body, "$.data[0].otherDocuments[0].number");
		assertHasStringField(body, "$.data[0].otherDocuments[0].country");
		assertHasStringField(body, "$.data[0].otherDocuments[0].expirationDate");
		assertHasStringField(body, "$.data[0].otherDocuments[0].type");

		assertHasStringField(body, "$.data[1].businessId");
		assertHasStringField(body, "$.data[1].updateDateTime");
		assertHasStringField(body, "$.data[1].brandName");
		assertHasStringField(body, "$.data[1].cnpjNumber");
		assertHasStringField(body, "$.data[1].tradeName");
		assertHasStringArrayField(body, "$.data[1].companyCnpjNumber");
		assertHasStringField(body, "$.data[1].incorporationDate");

		assertHasField(body, "$.data[1].parties[0]");
		assertHasStringField(body, "$.data[1].parties[0].type");
		assertHasStringField(body, "$.data[1].parties[0].personType");
		assertHasStringField(body, "$.data[1].parties[0].civilName");
		assertHasStringField(body, "$.data[1].parties[0].socialName");
		assertHasStringField(body, "$.data[1].parties[0].companyName");
		assertHasStringField(body, "$.data[1].parties[0].startDate");
		assertHasStringField(body, "$.data[1].parties[0].shareholding");
		assertHasStringField(body, "$.data[1].parties[0].documentType");
		assertHasStringField(body, "$.data[1].parties[0].documentNumber");
		assertHasStringField(body, "$.data[1].parties[0].documentCountry");
		assertHasStringField(body, "$.data[1].parties[0].documentExpirationDate");
		assertHasStringField(body, "$.data[1].parties[0].tradeName");
		assertHasStringField(body, "$.data[1].parties[0].documentAdditionalInfo");
		assertHasStringField(body, "$.data[1].parties[0].documentIssueDate");

		assertHasField(body, "$.data[1].contacts");
		assertHasField(body, "$.data[1].contacts.postalAddresses[0]");
		assertHasBooleanField(body, "$.data[1].contacts.postalAddresses[0].isMain");
		assertHasStringField(body, "$.data[1].contacts.postalAddresses[0].address");
		assertHasStringField(body, "$.data[1].contacts.postalAddresses[0].districtName");
		assertHasStringField(body, "$.data[1].contacts.postalAddresses[0].townName");
		assertHasStringField(body, "$.data[1].contacts.postalAddresses[0].countrySubDivision.value");
		assertHasStringField(body, "$.data[1].contacts.postalAddresses[0].postCode");
		assertHasStringField(body, "$.data[1].contacts.postalAddresses[0].country");
		assertHasStringField(body, "$.data[1].contacts.postalAddresses[0].additionalInfo");
		assertHasStringField(body, "$.data[1].contacts.postalAddresses[0].ibgeTownCode");
		assertHasStringField(body, "$.data[1].contacts.postalAddresses[0].countryCode");
		assertHasStringField(body, "$.data[1].contacts.postalAddresses[0].geographicCoordinates.value");

		assertHasField(body, "$.data[1].contacts.phones[0]");
		assertHasBooleanField(body, "$.data[1].contacts.phones[0].isMain");
		assertHasStringField(body, "$.data[1].contacts.phones[0].type.value");
		assertHasStringField(body, "$.data[1].contacts.phones[0].countryCallingCode");
		assertHasStringField(body, "$.data[1].contacts.phones[0].areaCode.value");
		assertHasStringField(body, "$.data[1].contacts.phones[0].number");
		assertHasStringField(body, "$.data[1].contacts.phones[0].phoneExtension");
		assertHasStringField(body, "$.data[1].contacts.phones[0].additionalInfo");

		assertHasField(body,"$.data[1].contacts.emails[0]");
		assertHasBooleanField(body, "$.data[1].contacts.emails[0].isMain");
		assertHasStringField(body, "$.data[1].contacts.emails[0].email");

		assertHasField(body,"$.data[1].otherDocuments[0]" );
		assertHasStringField(body, "$.data[1].otherDocuments[0].number");
		assertHasStringField(body, "$.data[1].otherDocuments[0].country");
		assertHasStringField(body, "$.data[1].otherDocuments[0].expirationDate");
		assertHasStringField(body, "$.data[1].otherDocuments[0].type");
		return environment;
	}
}


