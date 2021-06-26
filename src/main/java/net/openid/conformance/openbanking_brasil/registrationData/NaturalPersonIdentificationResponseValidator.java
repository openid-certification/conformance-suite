package net.openid.conformance.openbanking_brasil.registrationData;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

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
		assertHasField(body, ROOT_PATH);
		assertJsonArrays(body, ROOT_PATH, this::assertInnerFieldsForData);
		return environment;
	}

	private void assertInnerFieldsForData(JsonObject body) {
		final Set<String> enumMaritalStatusCode = Set.of("SOLTEIRO", "CASADO",
			"VIUVO", "SEPARADO_JUDICIALMENTE", "DIVORCIADO", "UNIAO_ESTAVEL", "OUTRO");
		final Set<String> enumSex = Set.of("FEMININO", "MASCULINO", "OUTRO");

		assertField(body,
			new StringField
				.Builder("updateDateTime")
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("personalId")
				.setMaxLength(100)
				//.setPattern("\\w*\\W*")TODO:wrong pattern
				.build());

		assertField(body,
			new StringField
				.Builder("brandName")
				.setMaxLength(80)
				//.setPattern("\\w*\\W*")TODO:wrong pattern
				.build());

		assertField(body,
			new StringField
				.Builder("civilName")
				.setMaxLength(70)
				//.setPattern("\\w*\\W*")TODO:wrong pattern
				.build());

		assertField(body,
			new StringField
				.Builder("socialName")
				.setMaxLength(70)
				//.setPattern("\\w*\\W*")TODO:wrong pattern
				.build());

		assertField(body,
			new StringField
				.Builder("birthDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.build());

		assertField(body,
			new StringField
				.Builder("maritalStatusCode")
				.setEnums(enumMaritalStatusCode)
				.build());

		assertField(body,
			new StringField
				.Builder("maritalStatusAdditionalInfo")
				.setMaxLength(50)
				.setPattern("\\w*\\W*")
				.build());

		assertField(body,
			new StringField
				.Builder("sex")
				.setEnums(enumSex)
				.build());

		assertField(body,
			new StringArrayField
				.Builder("companyCnpj")
				.setPattern("\\d{14}|^NA$")
				.setMaxLength(14)
				.build());

		assertField(body, new BooleanField
			.Builder("hasBrazilianNationality")
			.build());

		assertDocuments(body);
		assertOtherDocuments(body);
		assertNationality(body);
		assertFiliation(body);
		assertContracts(body);
	}

	private void assertContracts(JsonObject body) {
		assertHasField(body, "contacts");
		assertHasField(body, "contacts.postalAddresses");
		assertJsonArrays(body, "contacts.postalAddresses", this::assertInnerPostalAddressesFields);

		assertHasField(body, "contacts.phones");
		assertJsonArrays(body, "contacts.phones", this::assertInnerPhonesFields);

		assertHasField(body, "contacts.emails");
		assertJsonArrays(body, "contacts.emails", this::assertInnerEmailsFields);
	}

	private void assertFiliation(JsonObject body) {
		assertHasField(body, "filiation");
		assertJsonArrays(body, "filiation", this::assertInnerFiliationFields);
	}

	private void assertNationality(JsonObject body) {
		assertHasField(body, "nationality");
		assertJsonArrays(body, "nationality", this::assertInnerNationalityFields);
	}

	private void assertOtherDocuments(JsonObject body) {
		assertHasField(body, "otherDocuments");
		assertJsonArrays(body, "otherDocuments", this::assertInnerOtherDocuments);
	}

	private void assertDocuments(JsonObject body) {
		JsonObject documents = findByPath(body, "documents").getAsJsonObject();

		assertField(documents,
			new StringField
				.Builder("cpfNumber")
				.setPattern("^\\d{11}$|^NA$")
				.setMaxLength(11)
				.build());

		assertField(documents,
			new StringField
				.Builder("passportNumber")
				.setPattern("\\w*\\W*|^NA$")
				.setMaxLength(20)
				.build());

		assertField(documents,
			new StringField
				.Builder("passportCountry")
				.setPattern("^(\\w{3}){1}$|^NA$")
				.setMaxLength(3)
				.build());

		assertField(documents,
			new StringField
				.Builder("passportExpirationDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$|^NA$")
				.setMaxLength(10)
				.build());

		assertField(documents,
			new StringField
				.Builder("passportIssueDate")
				.setOptional()
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$|^NA$")
				.setMaxLength(10)
				.build());
	}

	private void assertInnerOtherDocuments(JsonObject body) {
		final Set<String> enumPersonalOtherDocumentTypes = Set.of("CNH", "RG", "NIF", "RNE",
			"OUTROS", "SEM_OUTROS_DOCUMENTOS");

		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(enumPersonalOtherDocumentTypes)
				.build());

		assertField(body,
			new StringField
				.Builder("typeAdditionalInfo")
				.setMaxLength(70)
				.setPattern("\\w*\\W*")
				.build());

		assertField(body,
			new StringField
				.Builder("number")
				.setMaxLength(11)
				.setPattern("\\w*\\W*")
				.build());

		assertField(body,
			new StringField
				.Builder("checkDigit")
				.setMaxLength(2)
				.setPattern("\\w*\\W*")
				.build());

		assertField(body,
			new StringField
				.Builder("checkDigit")
				.setOptional()
				.setMaxLength(50)
				.setPattern("\\w*\\W*")
				.build());

		assertField(body,
			new StringField
				.Builder("expirationDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$|^NA$")
				.build());
	}

	private void assertInnerNationalityFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("otherNationalitiesInfo")
				.setMaxLength(40)
				.build());

		assertHasField(body, "documents");
		assertJsonArrays(body, "documents", this::assertInnerNationalityDocumentsFields);
	}

	private void assertInnerNationalityDocumentsFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("type")
				.setMaxLength(10)
				//.setPattern("\\w*\\W*")TODO:wrong pattern
				.build());

		assertField(body,
			new StringField
				.Builder("number")
				.setMaxLength(40)
				.setPattern("\\w*\\W*")
				.build());

		assertField(body,
			new StringField
				.Builder("expirationDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$|^NA$")
				.build());

		assertField(body,
			new StringField
				.Builder("issueDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$|^NA$")
				.build());

		assertField(body,
			new StringField
				.Builder("country")
				.setOptional()
				.setMaxLength(80)
				.setPattern("\\w*\\W*")
				.build());

		assertField(body,
			new StringField
				.Builder("typeAdditionalInfo")
				.setOptional()
				.setMaxLength(70)
				//.setPattern("\\w*\\W*")TODO: wrong pattern
				.build());
	}

	private void assertInnerFiliationFields(JsonObject body) {
		final Set<String> enumFiliationType = Set.of("MAE", "PAI", "SEM_FILIACAO");

		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(enumFiliationType)
				.build());

		assertField(body,
			new StringField
				.Builder("civilName")
				.setMaxLength(70)
				//.setPattern("\\w*\\W*|^NA$")TODO:wrong pattern
				.build());

		assertField(body,
			new StringField
				.Builder("socialName")
				.setOptional()
				.setMaxLength(70)
				//.setPattern("\\w*\\W*|^NA$")TODO:wrong pattern
				.build());
	}

	private void assertInnerPostalAddressesFields(JsonObject body) {
		final Set<String> enumCountrySubDivision =  Set.of("AC", "AL", "AP", "AM",
			"BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ",
			"RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO", "NA");

		assertField(body, new BooleanField("isMain"));

		assertField(body,
			new StringField
				.Builder("address")
				.setMaxLength(150)
				//.setPattern("\\w*\\W*")TODO:wrong pattern
				.build());

		assertField(body,
			new StringField
				.Builder("additionalInfo")
				.setOptional()
				.setMaxLength(30)
				//.setPattern("\\w*\\W*")TODO:wrong pattern
				.build());

		assertField(body,
			new StringField
				.Builder("districtName")
				.setMaxLength(50)
				.setPattern("\\w*\\W*|^NA$")
				.build());

		assertField(body,
			new StringField
				.Builder("townName")
				.setMaxLength(50)
				//.setPattern("\\w*\\W*|^NA$")TODO:wrong pattern
				.build());

		assertField(body,
			new StringField
				.Builder("ibgeTownCode")
				.setOptional()
				.setMaxLength(7)
				.setPattern("\\d{7}$")
				.build());

		assertField(body,
			new StringField
				.Builder("countrySubDivision")
				.setEnums(enumCountrySubDivision)
				.build());

		assertField(body,
			new StringField
				.Builder("postCode")
				.setMaxLength(8)
				.setPattern("\\d{8}|^NA$")
				.build());

		assertField(body,
			new StringField
				.Builder("country")
				.setMaxLength(80)
				.setPattern("\\w*\\W*")
				.build());

		assertField(body,
			new StringField
				.Builder("countryCode")
				.setOptional()
				.setMaxLength(3)
				.build());

//TODO: need to add latitude & longitude checking
		assertField(body,
			new StringField
				.Builder("latitude")
				.setOptional()
				.setMaxLength(13)
				.setPattern("^-?\\d{1,2}\\.\\d{1,9}$")
				.build());

//TODO: need to add latitude & longitude checking
		assertField(body,
			new StringField
				.Builder("longitude")
				.setOptional()
				.setMaxLength(13)
				.setPattern("^-?\\d{1,3}\\.\\d{1,8}$")
				.build());
	}

	private void assertInnerPhonesFields(JsonObject body) {
		final Set<String> enumCustomerPhoneType = Set.of("FIXO", "MOVEL", "OUTRO");
		final Set<String> enumAreaCodes = Set.of("11", "12", "13", "14", "15", "16", "17",
			"18", "19", "21", "22", "24", "27", "28", "31", "32", "33", "34", "35", "37", "38",
			"41", "42", "43", "44", "45", "46", "47", "48", "49", "51", "53", "54", "55",
			"61", "62", "63", "64", "65", "66", "67", "68", "69", "71", "73", "74", "75",
			"77", "79", "81", "82", "83", "84", "85", "86", "87", "88", "89", "91", "92",
			"93", "94", "95", "96", "97", "98", "99", "NA");

		assertField(body, new BooleanField.Builder("isMain").build());

		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(enumCustomerPhoneType)
				.setMaxLength(5)
				.build());

		assertField(body,
			new StringField
				.Builder("additionalInfo")
				.setOptional()
				.setMaxLength(70)
				//.setPattern("\\w*\\W*")TODO:wrong pattern
				.build());

		assertField(body,
			new StringField
				.Builder("countryCallingCode")
				.setMaxLength(4)
				.setPattern("^\\d{2,4}$|^NA$")
				.build());

		assertField(body,
			new StringField
				.Builder("areaCode")
				.setMaxLength(2)
				.setEnums(enumAreaCodes)
				.build());

		assertField(body,
			new StringField
				.Builder("number")
				.setMaxLength(11)
				.setPattern("^([0-9]{8,11})|^NA$")
				.build());

		assertField(body,
			new StringField
				.Builder("phoneExtension")
				.setMaxLength(5)
				.setPattern("^\\d{1,5}$|^NA$")
				.build());
	}

	private void assertInnerEmailsFields(JsonObject body) {
		assertField(body, new BooleanField.Builder("isMain").build());

		assertField(body,
			new StringField
				.Builder("email")
				.setMaxLength(320)
				//.setPattern("\\w*\\W*")TODO:wrong pattern
				.build());
	}
}
