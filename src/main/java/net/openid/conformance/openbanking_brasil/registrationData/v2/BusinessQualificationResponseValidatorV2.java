package net.openid.conformance.openbanking_brasil.registrationData.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api url: swagger/openinsurance/registrationData/swagger-customers-v2.yaml
 * Api endpoint: /business/qualifications
 * Api version: 2.0.1.final
 **/
@ApiName("Business Qualification V2")
public class BusinessQualificationResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);

	public static final Set<String> ENUM_FREQUENCY = SetUtils.createSet("DIARIA, SEMANAL, QUINZENAL, MENSAL, BIMESTRAL, TRIMESTRAL, SEMESTRAL, ANUAL, OUTROS");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectField
				.Builder("data")
				.setValidator(this::assertData)
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
			new ObjectArrayField
				.Builder("economicActivities")
				.setValidator(this::assertInnerFieldsEconomicActivities)
				.setMinItems(0)
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("informedRevenue")
				.setValidator(this::assertInformedRevenue)
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("informedPatrimony")
				.setValidator(this::assertInnerFieldsInformedPatrimony)
				.setOptional()
				.build());
	}

	private void assertInnerFieldsEconomicActivities(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("code")
				.setPattern("^\\d{7}$")
				.setMaxLength(7)
				.setMinLength(2)
				.build());

		assertField(body,
			new BooleanField
				.Builder("isMain")
				.build());
	}

	private void assertInformedRevenue(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("frequency")
				.setEnums(ENUM_FREQUENCY)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("frequencyAdditionalInfo")
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.setMaxLength(100)
				.build());

		assertField(data,
			new ObjectField
				.Builder("amount")
				.setValidator(this::assertAmount)
				.build());

		assertField(data,
			new NumberField
				.Builder("year")
				.setOptional()
				.setMaxValue(9999)
				.build());
	}

	private void assertAmount(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("amount")
				.setMinLength(4)
				.setMaxLength(21)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^[A-Z]{3}$")
				.setMaxLength(3)
				.build());
	}

	private void assertInnerFieldsInformedPatrimony(JsonObject data) {
		assertField(data,
			new ObjectField
				.Builder("amount")
				.setValidator(this::assertAmount)
				.build());

		assertField(data,
			new DatetimeField
				.Builder("date")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(20)
				.build());
	}
}
