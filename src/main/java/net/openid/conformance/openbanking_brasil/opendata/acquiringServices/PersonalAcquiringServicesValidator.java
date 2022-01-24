package net.openid.conformance.openbanking_brasil.opendata.acquiringServices;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: https://sensedia.github.io/areadesenvolvedor/swagger/swagger_acquiring_services_apis.yaml
 * Api endpoint: /personal-acquiring-services
 * Api version: 1.0.0
 */

@ApiName("Personal Acquiring Services")
public class PersonalAcquiringServicesValidator extends AbstractJsonAssertingCondition {
	private static class Fields extends ProductNServicesCommonFields { }

	public static final Set<String> FEE_NAME = Sets.newHashSet(
		"TAXA_DE_DESCONTO_NA_MODALIDADE_CREDITO",
		"TAXA_DE_DESCONTO_NA_MODALIDADE_DEBITO");
	public static final Set<String> CODE = Sets.newHashSet("MDR_CREDITO", "MDR_DEBITO");
	public static final Set<String> INTERVAL = Sets.newHashSet("1_FAIXA", "2_FAIXA", "3_FAIXA", "4_FAIXA");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new ObjectField.Builder("data")
				.setValidator(this::assertData)
				.build());

		logFinalStatus();
		return environment;
	}

	private void assertData(JsonObject data) {
		assertField(data,
			new ObjectField.Builder("participantIdentification")
				.setValidator(this::assertParticipantIdentification)
				.build());

		assertField(data,
			new ObjectArrayField.Builder("feesAndRatesForService")
				.setValidator(this::assertFeesAndRatesForService)
				.setOptional()
				.build());
	}

	private void assertFeesAndRatesForService(JsonObject feesAndRatesForService) {
		assertField(feesAndRatesForService,
			new StringField
				.Builder("feeName")
				.setMaxLength(38)
				.setEnums(FEE_NAME)
				.build());

		assertField(feesAndRatesForService,
			new StringField
				.Builder("code")
				.setMaxLength(11)
				.setEnums(CODE)
				.build());

		assertField(feesAndRatesForService,
			new ObjectArrayField
				.Builder("prices")
				.setValidator(this::assertPrices)
				.setMinItems(4)
				.setMaxItems(4)
				.build());

		assertField(feesAndRatesForService,
			new StringField.
				Builder("chargingTriggerInfo")
				.setMaxLength(200)
				.build());

		assertField(feesAndRatesForService,
			new StringField.
				Builder("minimum")
				.setMinLength(1)
				.setMaxLength(999999)
				.setPattern("^\\d{1}\\.\\d{1,4}$")
				.build());

		assertField(feesAndRatesForService,
			new StringField.
				Builder("maximum")
				.setMinLength(1)
				.setMaxLength(999999)
				.setPattern("^\\d{1}\\.\\d{1,4}$")
				.build());
	}

	private void assertPrices(JsonObject prices) {
		assertField(prices,
			new StringField.
				Builder("interval")
				.setMaxLength(7)
				.setEnums(INTERVAL)
				.build());

		assertField(prices,
			new StringField.
				Builder("value")
				.setMaxLength(6)
				.setPattern("^\\d{1}\\.\\d{1,4}$")
				.build());

		assertField(prices,
			new StringField.
				Builder("customerRate")
				.setMaxLength(6)
				.setPattern("^\\d{1}\\.\\d{1,4}$")
				.build());
	}

	private void assertParticipantIdentification(JsonObject participantIdentification) {
		assertField(participantIdentification,
			new StringField
				.Builder("brand")
				.setMaxLength(80)
				.build());

		assertField(participantIdentification, Fields.name().setMaxLength(80).build());
		assertField(participantIdentification, Fields.cnpjNumber().setPattern("^\\d{14}$").build());

		assertField(participantIdentification,
			new StringField
				.Builder("urlComplementaryList")
				.setMaxLength(1024)
				.setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
				.setOptional()
				.build());
	}
}
