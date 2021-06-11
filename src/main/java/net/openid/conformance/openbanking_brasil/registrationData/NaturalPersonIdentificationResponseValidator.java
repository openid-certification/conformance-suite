package net.openid.conformance.openbanking_brasil.registrationData;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API - Dados Cadastrais "Identificação Pessoa Natural"
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#identificacao-pessoa-natural">Identificação Pessoa Natural</a>
 **/

@ApiName("Natural Person Identity")
public class NaturalPersonIdentificationResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data[0].updateDateTime");
		assertHasStringField(body, "$.data[0].personalId");
		assertHasStringField(body, "$.data[0].brandName");
		assertHasStringField(body, "$.data[0].civilName");
		assertHasStringField(body, "$.data[0].socialName");
		assertHasStringField(body, "$.data[0].birthDate");
		assertHasStringField(body, "$.data[0].maritalStatusCode");
		assertHasStringField(body, "$.data[0].maritalStatusAdditionalInfo");
		assertHasStringField(body, "$.data[0].sex");

		assertHasStringArrayField(body, "$.data[0].companyCnpj");

		assertHasField(body, "$.data[0].documents");
		assertHasStringField(body, "$.data[0].documents.cpfNumber");
		assertHasStringField(body, "$.data[0].documents.passportNumber");
		assertHasStringField(body, "$.data[0].documents.passportCountry");
		assertHasStringField(body, "$.data[0].documents.passportExpirationDate");
		assertHasStringField(body, "$.data[0].documents.passportIssueDate");

		assertHasField(body, "$.data[0].otherDocuments[0]");
		assertHasStringField(body, "$.data[0].otherDocuments[0].type");
		assertHasStringField(body, "$.data[0].otherDocuments[0].typeAdditionalInfo");
		assertHasStringField(body, "$.data[0].otherDocuments[0].number");
		assertHasStringField(body, "$.data[0].otherDocuments[0].checkDigit");
		assertHasStringField(body, "$.data[0].otherDocuments[0].additionalInfo");
		assertHasStringField(body, "$.data[0].otherDocuments[0].expirationDate");

		assertHasBooleanField(body, "$.data[0].hasBrazilianNationality");

		assertHasField(body, "$.data[0].nationality[0]");
		assertHasStringField(body, "$.data[0].nationality[0].otherNationalitiesInfo");

		assertHasField(body, "$.data[0].nationality[0].documents[0]");
		assertHasStringField(body, "$.data[0].nationality[0].documents[0].type");
		assertHasStringField(body, "$.data[0].nationality[0].documents[0].number");
		assertHasStringField(body, "$.data[0].nationality[0].documents[0].expirationDate");
		assertHasStringField(body, "$.data[0].nationality[0].documents[0].issueDate");
		assertHasStringField(body, "$.data[0].nationality[0].documents[0].country");
		assertHasStringField(body, "$.data[0].nationality[0].documents[0].typeAdditionalInfo");

		assertHasField(body, "$.data[0].filiation[0]");
		assertHasStringField(body, "$.data[0].filiation[0].type");
		assertHasStringField(body, "$.data[0].filiation[0].civilName");
		assertHasStringField(body, "$.data[0].filiation[0].socialName");

		assertHasField(body, "$.data[0].contacts.postalAddresses[0]");
		assertHasBooleanField(body, "$.data[0].contacts.postalAddresses[0].isMain");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].address");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].additionalInfo");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].districtName");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].townName");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].ibgeTownCode");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].countrySubDivision");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].postCode");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].country");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].countryCode");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].geographicCoordinates.latitude");
		assertHasStringField(body, "$.data[0].contacts.postalAddresses[0].geographicCoordinates.longitude");

		assertHasField(body, "$.data[0].contacts.phones[0]");
		assertHasBooleanField(body, "$.data[0].contacts.phones[0].isMain");
		assertHasStringField(body, "$.data[0].contacts.phones[0].type");
		assertHasStringField(body, "$.data[0].contacts.phones[0].additionalInfo");
		assertHasStringField(body, "$.data[0].contacts.phones[0].countryCallingCode");
		assertHasStringField(body, "$.data[0].contacts.phones[0].areaCode");
		assertHasStringField(body, "$.data[0].contacts.phones[0].number");
		assertHasStringField(body, "$.data[0].contacts.phones[0].phoneExtension");

		assertHasField(body, "$.data[0].contacts.emails[0]");
		assertHasBooleanField(body, "$.data[0].contacts.emails[0].isMain");
		assertHasStringField(body, "$.data[0].contacts.emails[0].email");

		return environment;
	}
}
