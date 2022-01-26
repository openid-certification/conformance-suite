package net.openid.conformance.openbanking_brasil.opendata.acquiringServices;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_acquiring_services_apis.yaml
 * Api endpoint: /personal-acquiring-services
 * Api version: 1.0.0
 * Git hash: c90e531a2693825fe55fd28a076367cefcb01ad8
 */

@ApiName("Personal Acquiring Services")
public class PersonalAcquiringServicesValidator extends AbstractJsonAssertingCondition {
	private static class Fields extends ProductNServicesCommonFields { }

	public static final Set<String> FEE_NAME = Sets.newHashSet(
		"TAXA_DESCONTO_MODALIDADE_CREDITO", "TAXA_DESCONTO_MODALIDADE_DEBITO");
	public static final Set<String> CODE = Sets.newHashSet("MDR_CREDITO", "MDR_DEBITO");
	public static final Set<String> INTERVAL = Sets.newHashSet("1_FAIXA", "2_FAIXA", "3_FAIXA", "4_FAIXA");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField.Builder("data")
				.setValidator(this::assertData)
				.build());

		logFinalStatus();
		return environment;
	}

	private void assertData(JsonObject data) {
		assertField(data,
			new ObjectField.Builder("participant")
				.setValidator(this::assertParticipantIdentification)
				.build());

		assertField(data,
			new StringField
				.Builder("feeName")
				.setMaxLength(38)
				.setEnums(FEE_NAME)
				.build());

		assertField(data,
			new StringField.
				Builder("code")
				.setMaxLength(11)
				.setEnums(CODE)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("prices")
				.setValidator(this::assertPrices)
				.setMinItems(4)
				.setMaxItems(4)
				.build());

		assertField(data,
			new StringField.
				Builder("chargingTriggerInfo")
				.setMaxLength(200)
				.build());

		assertField(data,
			new StringField.
				Builder("minimum")
				.setMinLength(1)
				.setMaxLength(999999)
				.setPattern("^\\d{1}\\.\\d{1,4}$")
				.build());

		assertField(data,
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
