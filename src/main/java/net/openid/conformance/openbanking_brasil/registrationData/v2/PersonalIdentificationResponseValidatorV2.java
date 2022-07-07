package net.openid.conformance.openbanking_brasil.registrationData.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: swagger/openinsurance/registrationData/swagger-customers-v2.yaml
 * Api endpoint: /personal/identifications
 * Api version: 2.0.0.final
 **/

@ApiName("Natural Person Identity V2")
public class PersonalIdentificationResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);
	public static final Set<String> ENUM_MARITAL_STATUS_CODE = SetUtils.createSet("SOLTEIRO, CASADO, VIUVO, SEPARADO_JUDICIALMENTE, DIVORCIADO, UNIAO_ESTAVEL, OUTRO");
	public static final Set<String> ENUM_SEX = SetUtils.createSet("FEMININO, MASCULINO, OUTRO");
	public static final Set<String> ENUM_PERSONAL_OTHER_DOCUMENT_TYPES = SetUtils.createSet("CNH, RG, NIF, RNE, OUTROS, SEM_OUTROS_DOCUMENTOS");
	public static final Set<String> ENUM_COUNTRY_SUB_DIVISION = SetUtils.createSet("AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO");
	public static final Set<String> ENUM_CUSTOMER_PHONE_TYPE = SetUtils.createSet("FIXO, MOVEL, OUTRO");
	public static final Set<String> ENUM_AREA_CODES = SetUtils.createSet("11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 24, 27, 28, 31, 32, 33, 34, 35, 37, 38, 41, 42, 43, 44, 45, 46, 47, 48, 49, 51, 53, 54, 55, 61, 62, 63, 64, 65, 66, 67, 68, 69, 71, 73, 74, 75, 77, 79, 81, 82, 83, 84, 85, 86, 87, 88, 89, 91, 92, 93, 94, 95, 96, 97, 98, 99, NA");
	final Set<String> FILIATION_TYPE = SetUtils.createSet("MAE, PAI");


	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertData)
				.setMinItems(1)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertData(JsonObject body) {
		assertField(body,
			new DatetimeField
				.Builder("updateDateTime")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("personalId")
				.setMaxLength(100)
				.setMinLength(1)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{0,99}$")
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
				.setOptional()
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
				.setOptional()
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
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("companiesCnpj")
				.setPattern("^\\d{14}$")
				.setMaxLength(14)
				.setMinItems(1)
				.build());

		assertField(body,
			new ObjectField
				.Builder("documents")
				.setValidator(this::assertDocuments)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("otherDocuments")
				.setValidator(this::assertInnerOtherDocuments)
				.setMinItems(1)
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("hasBrazilianNationality")
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("nationality")
				.setValidator(this::assertInnerNationalityFields)
				.setOptional()
				.setMinItems(1)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("filiation")
				.setValidator(this::assertInnerFiliationFields)
				.setOptional()
				.setMinItems(1)
				.build());

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
				.setMinItems(0)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("phones")
				.setValidator(this::assertInnerPhonesFields)
				.setMinItems(0)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("emails")
				.setValidator(this::assertInnerEmailsFields)
				.setMinItems(0)
				.build());
	}

	private void assertDocuments(JsonObject documents) {

		assertField(documents,
			new StringField
				.Builder("cpfNumber")
				.setPattern("^\\d{11}$")
				.setMaxLength(11)
				.setOptional()
				.build());

		assertField(documents,
			new ObjectField
				.Builder("passport")
				.setValidator(this::assertPassport)
				.setOptional()
				.build());
	}

	private void assertPassport(JsonObject passport) {
		assertField(passport,
			new StringField
				.Builder("number")
				.setPattern("^[\\w\\W]*$")
				.setMaxLength(20)
				.build());

		assertField(passport,
			new StringField
				.Builder("country")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

		assertField(passport,
			new StringField
				.Builder("expirationDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(passport,
			new StringField
				.Builder("issueDate")
				.setOptional()
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
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
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("number")
				.setMaxLength(40)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("checkDigit")
				.setMaxLength(2)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
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
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setOptional()
				.build());
	}

	private void assertInnerNationalityFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("otherNationalitiesInfo")
				.setMaxLength(40)
				.setPattern("^\\S[\\s\\S]*$")
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("documents")
				.setValidator(this::assertInnerNationalityDocumentsFields)
				.build());
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
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("issueDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("country")
				.setMaxLength(80)
				.setPattern("\\w*\\W*")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());
	}

	private void assertInnerFiliationFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(FILIATION_TYPE)
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
		assertField(body,
			new BooleanField
				.Builder("isMain")
				.build());

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
				.setOptional()
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
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("postCode")
				.setMaxLength(8)
				.setPattern("^\\d{8}$")
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
				.setPattern("^([A-Z]{3})$")
				.setMaxLength(3)
				.build());

		assertGeographicCoordinates(body);
	}

	private void assertInnerPhonesFields(JsonObject body) {
		assertField(body,
			new BooleanField.
				Builder("isMain")
				.build());

		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(ENUM_CUSTOMER_PHONE_TYPE)
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
				.setPattern("^\\d{1,4}$")
				.build());

		assertField(body,
			new StringField
				.Builder("areaCode")
				.setEnums(ENUM_AREA_CODES)
				.build());

		assertField(body,
			new StringField
				.Builder("number")
				.setMaxLength(11)
				.setPattern("^([0-9]{8,11})$")
				.build());

		assertField(body,
			new StringField
				.Builder("phoneExtension")
				.setMaxLength(5)
				.setPattern("^\\d{1,5}$")
				.setOptional()
				.build());
	}

	private void assertInnerEmailsFields(JsonObject body) {
		assertField(body,
			new BooleanField
				.Builder("isMain")
				.build());

		assertField(body,
			new StringField
				.Builder("email")
				.setMaxLength(320)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}
}
