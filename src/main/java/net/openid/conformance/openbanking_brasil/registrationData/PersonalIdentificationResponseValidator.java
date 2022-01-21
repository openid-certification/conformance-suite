package net.openid.conformance.openbanking_brasil.registrationData;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 *  * API: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_accounts_apis.yaml
 *  * URL: /personal/identifications
 *  * Api git hash: 152a9f02d94d612b26dbfffb594640f719e96f70
 **/

@ApiName("Natural Person Identity")
public class PersonalIdentificationResponseValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> ENUM_MARITAL_STATUS_CODE = Set.of("SOLTEIRO", "CASADO",
		"VIUVO", "SEPARADO_JUDICIALMENTE", "DIVORCIADO", "UNIAO_ESTAVEL", "OUTRO");
	public static final Set<String> ENUM_SEX = Set.of("FEMININO", "MASCULINO", "OUTRO", "NAO_DISPONIVEL");
	public static final Set<String> ENUM_PERSONAL_OTHER_DOCUMENT_TYPES = Set.of("CNH", "RG", "NIF", "RNE",
		"OUTROS", "SEM_OUTROS_DOCUMENTOS");
	public static final Set<String> ENUM_COUNTRY_SUB_DIVISION = Set.of("AC", "AL", "AP", "AM",
		"BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ",
		"RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO", "NA");
	public static final Set<String> ENUM_CUSTOMER_PHONE_TYPE = Set.of("FIXO", "MOVEL", "OUTRO");
	public static final Set<String> ENUM_AREA_CODES = Set.of("11", "12", "13", "14", "15", "16", "17",
		"18", "19", "21", "22", "24", "27", "28", "31", "32", "33", "34", "35", "37", "38",
		"41", "42", "43", "44", "45", "46", "47", "48", "49", "51", "53", "54", "55",
		"61", "62", "63", "64", "65", "66", "67", "68", "69", "71", "73", "74", "75",
		"77", "79", "81", "82", "83", "84", "85", "86", "87", "88", "89", "91", "92",
		"93", "94", "95", "96", "97", "98", "99", "NA");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body, new ObjectArrayField.Builder(ROOT_PATH).setValidator(this::assertInnerFieldsForData).build());
		return environment;
	}

	private void assertInnerFieldsForData(JsonObject body) {

		assertField(body,
			new StringField
				.Builder("updateDateTime")
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("personalId")
				.setMaxLength(100)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.build());

		assertField(body,
			new StringField
				.Builder("brandName")
				.setMaxLength(80)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("civilName")
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("socialName")
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
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
				.setEnums(ENUM_MARITAL_STATUS_CODE)
				.build());

		assertField(body,
			new StringField
				.Builder("maritalStatusAdditionalInfo")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("sex")
				.setEnums(ENUM_SEX)
				.build());

		assertField(body,
			new StringArrayField
				.Builder("companyCnpj")
				.setPattern("\\d{14}|^NA$")
				.setMaxLength(14)
				.build());

		assertField(body,
			new BooleanField
				.Builder("hasBrazilianNationality")
				.setNullable()
				.build());

		assertDocuments(body);

		assertField(body,
			new ObjectArrayField
				.Builder("otherDocuments")
				.setValidator(this::assertInnerOtherDocuments)
				.setMinItems(1)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("nationality")
				.setValidator(this::assertInnerNationalityFields)
				.build());

		assertFiliation(body);

		assertField(body,
			new ObjectField
				.Builder("contacts")
				.setValidator(this::assertContracts)
				.build());
	}

	private void assertContracts(JsonObject body) {
		assertField(body,
			new ObjectArrayField
				.Builder("postalAddresses")
				.setValidator(this::assertInnerPostalAddressesFields)
				.setMinItems(1)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("phones")
				.setValidator(this::assertInnerPhonesFields)
				.setMinItems(1)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("emails")
				.setValidator(this::assertInnerEmailsFields)
				.setMinItems(1)
				.build());
	}

	private void assertFiliation(JsonObject body) {
		assertHasField(body, "filiation");
		assertField(body, new ObjectArrayField.Builder("filiation").setValidator(this::assertInnerFiliationFields).build());
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
				.setPattern("[\\w\\W\\s]*")
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

		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(ENUM_PERSONAL_OTHER_DOCUMENT_TYPES)
				.build());

		assertField(body,
			new StringField
				.Builder("typeAdditionalInfo")
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("number")
				.setMaxLength(11)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("checkDigit")
				.setMaxLength(2)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("additionalInfo")
				.setOptional()
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
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
		assertField(body, new ObjectArrayField.Builder("documents").setValidator(this::assertInnerNationalityDocumentsFields).build());
	}

	private void assertInnerNationalityDocumentsFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("type")
				.setMaxLength(10)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("number")
				.setMaxLength(40)
				.setPattern("[\\w\\W\\s]*")
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
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("typeAdditionalInfo")
				.setOptional()
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
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
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("socialName")
				.setOptional()
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertInnerPostalAddressesFields(JsonObject body) {

		assertField(body, new BooleanField.Builder("isMain").build());

		assertField(body,
			new StringField
				.Builder("address")
				.setMaxLength(150)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("additionalInfo")
				.setOptional()
				.setMaxLength(30)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("districtName")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("townName")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
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
				.setEnums(ENUM_COUNTRY_SUB_DIVISION)
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
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("countryCode")
				.setOptional()
				.setMaxLength(3)
				.build());

		 assertGeographicCoordinates(body);
	}

	private void assertInnerPhonesFields(JsonObject body) {

		assertField(body, new BooleanField.Builder("isMain").build());

		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(ENUM_CUSTOMER_PHONE_TYPE)
				.setMaxLength(5)
				.build());

		assertField(body,
			new StringField
				.Builder("additionalInfo")
				.setOptional()
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
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
				.setEnums(ENUM_AREA_CODES)
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
				.setPattern("[\\w\\W\\s]*")
				.build());
	}
}
