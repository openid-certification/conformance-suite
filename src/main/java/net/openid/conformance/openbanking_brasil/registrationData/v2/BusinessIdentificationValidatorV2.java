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
 * Api url: swagger/openinsurance/registrationData/swagger-customers.yaml
 * Api endpoint: /business/identifications
 * Api version: 2.0.0-RC1.0
 **/

@ApiName("Business Identification V2")
public class BusinessIdentificationValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);


	public static final Set<String> PERSON_TYPES = SetUtils.createSet("PESSOA_NATURAL, PESSOA_JURIDICA");
	public static final Set<String> AREA_CODES = SetUtils.createSet("11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 24, 27, 28, 31, 32, 33, 34, 35, 37, 38, 41, 42, 43, 44, 45, 46, 47, 48, 49, 51, 53, 54, 55, 61, 62, 63, 64, 65, 66, 67, 68, 69, 71, 73, 74, 75, 77, 79, 81, 82, 83, 84, 85, 86, 87, 88, 89, 91, 92, 93, 94, 95, 96, 97, 98, 99, NA");
	public static final Set<String> COUNTRY_SUB_DIVISIONS = SetUtils.createSet("AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO, NA");
	public static final Set<String> PARTIES_TYPE = SetUtils.createSet("SOCIO, ADMINISTRADOR");
	public static final Set<String> DOCUMENT_TYPE = SetUtils.createSet("CPF, PASSAPORTE, OUTRO_DOCUMENTO_VIAGEM, CNPJ");
	public static final Set<String> PHONE_TYPE = SetUtils.createSet("FIXO, MOVEL, OUTRO");

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
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("businessId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{1,100}$")
				.setMaxLength(100)
				.setMinLength(1)
				.build());

		assertField(body,
			new StringField
				.Builder("brandName")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(80)
				.build());

		assertField(body,
			new StringField
				.Builder("companyName")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(70)
				.build());

		assertField(body,
			new StringField
				.Builder("tradeName")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(70)
				.setOptional()
				.build());

		assertField(body,
			new DatetimeField
				.Builder("incorporationDate")
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("cnpjNumber")
				.setPattern("^\\d{14}$")
				.setMaxLength(14)
				.build());

		assertField(body,
			new StringArrayField
				.Builder("companiesCnpj")
				.setMaxLength(14)
				.setMinItems(1)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("otherDocuments")
				.setValidator(this::assertOtherDocuments)
				.setMinItems(0)
				.setOptional()
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("parties")
				.setValidator(this::assertParties)
				.setMinItems(1)
				.build());

		assertField(body,
			new ObjectField
				.Builder("contacts")
				.setValidator(this::assertContacts)
				.build());
	}

	private void assertOtherDocuments(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("type")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("number")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("country")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

		assertField(body,
			new StringField
				.Builder("expirationDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.setOptional()
				.build());
	}

	private void assertParties(JsonObject body) {

		assertField(body,
			new StringField
				.Builder("personType")
				.setEnums(PERSON_TYPES)
				.build());

		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(PARTIES_TYPE)
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
				.Builder("companyName")
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("tradeName")
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(body,
			new DatetimeField
				.Builder("startDate")
				.setOptional()
				.setMaxLength(20)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.build());

		assertField(body,
			new StringField
				.Builder("shareholding")
				.setMaxLength(8)
				.setMinLength(8)
				.setPattern("^[01]\\.\\d{6}$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("documentType")
				.setEnums(DOCUMENT_TYPE)
				.build());

		assertField(body,
			new StringField
				.Builder("documentNumber")
				.setMaxLength(20)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("documentAdditionalInfo")
				.setMaxLength(100)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("documentCountry")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(3)
				.setOptional()
				.build());

		assertField(body,
			new DatetimeField
				.Builder("documentExpirationDate")
				.setMaxLength(10)
				.setOptional()
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.build());

		assertField(body,
			new DatetimeField
				.Builder("documentIssueDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setOptional()
				.build());
	}

	private void assertContacts(JsonObject body) {
		assertField(body,
			new ObjectArrayField
				.Builder("postalAddresses")
				.setValidator(this::assertInnerPostalAddresses)
				.setMinItems(1)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("phones")
				.setValidator(this::assertInnerPhones)
				.setMinItems(0)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("emails")
				.setValidator(this::assertInnerEmails)
				.setMinItems(0)
				.build());
	}

	private void assertInnerEmails(JsonObject body) {
		assertField(body,
			new BooleanField
				.Builder("isMain").build());

		assertField(body,
			new StringField
				.Builder("email")
				.setMaxLength(320)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertInnerPhones(JsonObject body) {

		assertField(body,
			new BooleanField
				.Builder("isMain")
				.build());

		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(PHONE_TYPE)
				.build());

		assertField(body,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("countryCallingCode")
				.setPattern("^\\d{2,4}$")
				.setMaxLength(4)
				.build());

		assertField(body,
			new StringField
				.Builder("areaCode")
				.setEnums(AREA_CODES)
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

	private void assertInnerPostalAddresses(JsonObject body) {

		assertField(body,
			new BooleanField
				.Builder("isMain")
				.build());

		assertField(body,
			new StringField
				.Builder("address")
				.setMaxLength(150)
				.setMinLength(2)
				.setPattern("^\\w{2}[\\w\\W\\s]*$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(30)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
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
				.setMinLength(2)
				.setPattern("^\\w{2}[\\w\\W\\s]*$")
				.build());

		assertField(body,
			new StringField
				.Builder("ibgeTownCode")
				.setMaxLength(7)
				.setPattern("\\d{7}$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("countrySubDivision")
				.setEnums(COUNTRY_SUB_DIVISIONS)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("postCode")
				.setMaxLength(8)
				.setPattern("^\\d{8}$")
				.setOptional()
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
				.setMaxLength(3)
				.setPattern("^([A-Z]{3})$")
				.build());

		assertGeographicCoordinates(body);
	}
}
