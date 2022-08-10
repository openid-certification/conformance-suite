package net.openid.conformance.openinsurance.validator.customers.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: swagger/openinsurance/customers/v1/swagger-customer-api.yaml
 * Api endpoint: /personal/identifications
 * Api version: 1.05
 **/

@ApiName("Personal Identifications V1")
public class OpinCustomersPersonalIdentificationListValidatorV1 extends AbstractJsonAssertingCondition {
	private final OpinLinksAndMetaValidator opinLinksAndMetaValidator = new OpinLinksAndMetaValidator(this);
	public static final Set<String> ENUM_COUNTRY_SUB_DIVISION = SetUtils.createSet("AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO");
	public static final Set<String> ENUM_AREA_CODES = SetUtils.createSet("11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 24, 27, 28, 31, 32, 33, 34, 35, 37, 38, 41, 42, 43, 44, 45, 46, 47, 48, 49, 51, 53, 54, 55, 61, 62, 63, 64, 65, 66, 67, 68, 69, 71, 73, 74, 75, 77, 79, 81, 82, 83, 84, 85, 86, 87, 88, 89, 91, 92, 93, 94, 95, 96, 97, 98, 99, NA");
	final Set<String> FILIATION_TYPE = SetUtils.createSet("MAE, PAI");
	final Set<String> TYPE = SetUtils.createSet("CNH, RG, NIF, RNE, OUTROS, SEM_OUTROS_DOCUMENTOS");
	final Set<String> STATUS_CODE = SetUtils.createSet("SOLTEIRO, CASADO, VIUVO, SEPARADO_JUDICIALMENTE, DIVORCIADO, UNIAO_ESTAVEL, OUTROS ");


	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertData)
				.build());
		opinLinksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertData(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("updateDateTime")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$")
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("personalId")
				.setMaxLength(100)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.setOptional()
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
				.Builder("cpfNumber")
				.setMaxLength(11)
				.setPattern("^\\d{11}$|^NA$")
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
			new ObjectArrayField
				.Builder("documents")
				.setValidator(documents -> {
					assertField(documents,
						new StringField
							.Builder("type")
							.setEnums(TYPE)
							.setOptional()
							.build());

					assertField(documents,
						new StringField
							.Builder("number")
							.setMaxLength(20)
							.setOptional()
							.build());

					assertField(documents,
						new StringField
							.Builder("expirationDate")
							.setMaxLength(10)
							.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
							.setOptional()
							.build());

					assertField(documents,
						new StringField
							.Builder("issueLocation")
							.setMaxLength(40)
							.setOptional()
							.build());
				})
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("hasBrazilianNationality")
				.setNullable()
				.build());

		assertField(body,
			new StringField
				.Builder("otherNationalitiesInfo")
				.setMaxLength(3)
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("otherDocuments")
				.setValidator(this::assertDocuments)
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("contact")
				.setValidator(this::assertContact)
				.build());

		assertField(body,
			new StringField
				.Builder("civilStatusCode")
				.setEnums(STATUS_CODE)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("sex")
				.setMaxLength(20)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("birthDate")
				.setMaxLength(10)
				.setOptional()
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.build());

		assertField(body,
			new ObjectField
				.Builder("filiation")
				.setValidator(this::assertInnerFiliationFields)
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("identificationDetails")
				.setValidator(companyInfo -> {
					assertField(companyInfo,
						new StringField
							.Builder("civilName")
							.setPattern("[\\w\\W\\s]*")
							.setMaxLength(70)
							.setOptional()
							.build());

					assertField(companyInfo,
						new StringField
							.Builder("cpfNumber")
							.setMaxLength(11)
							.setPattern("^\\d{11}$|^NA$")
							.setOptional()
							.build());
				})
				.setOptional()
				.build());
	}

	private void assertContact(JsonObject body) {
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
				.setOptional()
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
				.setMinItems(1)
				.setOptional()
				.build());
	}

	private void assertDocuments(JsonObject passport) {
		assertField(passport,
			new StringField
				.Builder("type")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(passport,
			new StringField
				.Builder("number")
				.setMaxLength(20)
				.setOptional()
				.build());

		assertField(passport,
			new StringField
				.Builder("country")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(passport,
			new StringField
				.Builder("expirationDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.setOptional()
				.build());
	}

	private void assertInnerFiliationFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(FILIATION_TYPE)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("civilName")
				.setMaxLength(70)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertInnerPostalAddressesFields(JsonObject body) {
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
				.Builder("countrySubDivision")
				.setEnums(ENUM_COUNTRY_SUB_DIVISION)
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
				.setMaxLength(3)
				.build());
	}

	private void assertInnerPhonesFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("countryCallingCode")
				.setMaxLength(4)
				.setPattern("^\\d{2,4}$|^NA$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("areaCode")
				.setEnums(ENUM_AREA_CODES)
				.setMaxLength(2)
				.setOptional()
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
}
