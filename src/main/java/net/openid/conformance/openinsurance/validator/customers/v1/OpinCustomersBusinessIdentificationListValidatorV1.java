package net.openid.conformance.openinsurance.validator.customers.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: swagger/openinsurance/customers/v1/swagger-customer-api.yaml
 * Api endpoint: /business/identifications
 * Api version: 1.05
 **/

@ApiName("Business Identifications V1")
public class OpinCustomersBusinessIdentificationListValidatorV1 extends AbstractJsonAssertingCondition {

	private final OpinLinksAndMetaValidator linksAndMetaValidator = new OpinLinksAndMetaValidator(this);

	public static final Set<String> AREA_CODES = SetUtils.createSet("11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 24, 27, 28, 31, 32, 33, 34, 35, 37, 38, 41, 42, 43, 44, 45, 46, 47, 48, 49, 51, 53, 54, 55, 61, 62, 63, 64, 65, 66, 67, 68, 69, 71, 73, 74, 75, 77, 79, 81, 82, 83, 84, 85, 86, 87, 88, 89, 91, 92, 93, 94, 95, 96, 97, 98, 99, NA");
	public static final Set<String> COUNTRY_SUB_DIVISIONS = SetUtils.createSet("AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO");
	public static final Set<String> PARTIES_TYPE = SetUtils.createSet("SOCIO, ADMINISTRADOR");
	public static final Set<String> TYPE = SetUtils.createSet("PRIVADO, PUBLICO");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertData)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertData(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("updateDateTime")
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("businessId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("brandName")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(80)
				.build());

		assertField(body,
			new ObjectField
				.Builder("companyInfo")
				.setValidator(companyInfo -> {
					assertField(companyInfo,
						new StringField
							.Builder("cnpjNumber")
							.setPattern("\\d{14}|^NA$")
							.setMaxLength(14)
							.build());

					assertField(companyInfo,
						new StringField
							.Builder("name")
							.setMaxLength(70)
							.setPattern("[\\w\\W\\s]*")
							.build());
				})
				.build());

		assertField(body,
			new StringField
				.Builder("businessName")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(70)
				.build());

		assertField(body,
			new StringField
				.Builder("businessTradeName")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(70)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("incorporationDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("document")
				.setValidator(this::assertDocument)
				.build());

		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(TYPE)
				.setOptional()
				.setMaxLength(7)
				.build());

		assertField(body,
			new ObjectField
				.Builder("contact")
				.setValidator(this::assertContacts)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("parties")
				.setValidator(this::assertParties)
				.setOptional()
				.build());
	}

	private void assertDocument(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("businesscnpjNumber")
				.setMaxLength(14)
				.setPattern("\\d{14}|^NA$")
				.build());

		assertField(body,
			new StringField
				.Builder("businessRegisterNumberOriginCountry")
				.setOptional()
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("country")
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
				.Builder("type")
				.setEnums(PARTIES_TYPE)
				.setOptional()
				.setMaxLength(13)
				.build());

		assertField(body,
			new StringField
				.Builder("civilName")
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("socialName")
				.setMaxLength(70)
				.setPattern("^[\\w\\W]*$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("startDate")
				.setOptional()
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
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
				.setMaxLength(15)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("documentNumber")
				.setMaxLength(20)
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
			new StringField
				.Builder("documentExpirationDate")
				.setMaxLength(10)
				.setOptional()
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
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
				.setMinItems(1)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("emails")
				.setValidator(emails -> assertField(emails,
					new StringField
						.Builder("email")
						.setMaxLength(320)
						.setPattern("[\\w\\W\\s]*")
						.setOptional()
						.build()))
				.setOptional()
				.setMinItems(1)
				.build());
	}

	private void assertInnerPhones(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("countryCallingCode")
				.setPattern("^\\d{2,4}$|^NA$")
				.setMaxLength(4)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("areaCode")
				.setEnums(AREA_CODES)
				.setOptional()
				.setMaxLength(2)
				.build());

		assertField(body,
			new StringField
				.Builder("number")
				.setMaxLength(11)
				.setPattern("^([0-9]{8,11})|^NA$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("phoneExtension")
				.setMaxLength(5)
				.setPattern("^\\d{1,5}$|^NA$")
				.setOptional()
				.build());
	}

	private void assertInnerPostalAddresses(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("address")
				.setMaxLength(150)
				.setPattern("[\\w\\W\\s]*")
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
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("countrySubDivision")
				.setEnums(COUNTRY_SUB_DIVISIONS)
				.build());

		assertField(body,
			new StringField
				.Builder("postCode")
				.setMaxLength(8)
				.setPattern("\\d{8}|^NA$")
				.build());

		assertField(body,
			new StringField
				.Builder("ibgeTownCode")
				.setMaxLength(8)
				.setPattern("\\d{8}|^NA$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("country")
				.setMaxLength(60)
				.build());

		assertField(body,
			new StringField
				.Builder("countryCode")
				.setMaxLength(3)
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("geographicCoordinates")
				.setValidator(companyInfo -> {
					assertField(companyInfo,
						new StringField
							.Builder("latitude")
							.setPattern("^-?\\d{1,2}\\.\\d{1,9}$")
							.setMaxLength(13)
							.build());

					assertField(companyInfo,
						new StringField
							.Builder("longitude")
							.setPattern("^-?\\d{1,3}\\.\\d{1,8}$")
							.setMaxLength(13)
							.build());
				})
				.setOptional()
				.build());
	}
}
