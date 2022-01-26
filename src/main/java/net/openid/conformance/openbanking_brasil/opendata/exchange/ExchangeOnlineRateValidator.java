package net.openid.conformance.openbanking_brasil.opendata.exchange;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api url: https://sensedia.github.io/areadesenvolvedor/swagger/swagger_exchange_apis.yaml
 * Api endpoint: /online-rate
 * Api version: 1.0.0
 */

@ApiName("Online Rate")
public class ExchangeOnlineRateValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductNServicesCommonFields { }

	public static final Set<String> CURRENCY = Sets.newHashSet("USD", "EUR");
	public static final Set<String> DELIVERY_CURRENCY = Sets.newHashSet("ESPECIE", "CARTAO_PRE_PAGO", "TELETRANSMISSAO_SWIFT");
	public static final Set<String> TRANSANCTION_TYPE = Sets.newHashSet("COMPRA", "VENDA");
	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PF", "PJ");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(data -> {
					assertField(data,
						new ObjectField
							.Builder("participantIdentification")
							.setValidator(this::assertParticipantIdentification)
							.build());

					assertField(data,
						new ObjectField
							.Builder("onlineRate")
							.setValidator(this::assertOnlineRate)
							.build());
				}).build());

		logFinalStatus();
		return environment;
	}

	private void assertOnlineRate(JsonObject onlineRate) {
		assertField(onlineRate,
			new ObjectArrayField
				.Builder("values")
				.setValidator(this::assertValues)
				.build());

		assertField(onlineRate,
			new DatetimeField
				.Builder("timestamp")
				.build());

		assertField(onlineRate,
			new StringField
				.Builder("disclaimer")
				.build());
	}

	private void assertValues(JsonObject values) {
		assertField(values,
			new StringField
				.Builder("foreignCurrency")
				.setMaxLength(3)
				.setEnums(CURRENCY)
				.build());

		assertField(values,
			new StringField
				.Builder("deliveryForeignCurrency")
				.setMaxLength(21)
				.setEnums(DELIVERY_CURRENCY)
				.build());

		assertField(values,
			new StringField
				.Builder("transactionType")
				.setMaxLength(6)
				.setEnums(TRANSANCTION_TYPE)
				.build());

		assertField(values,
			new StringField
				.Builder("targetAudience")
				.setMaxLength(2)
				.setEnums(TARGET_AUDIENCE)
				.setOptional()
				.build());

		assertField(values,
			new StringField
				.Builder("value")
				.setMaxLength(7)
				.setPattern("^\\d{1}\\.\\d{1,5}$")
				.build());
	}

	private void assertParticipantIdentification(JsonObject participantIdentification) {
		assertField(participantIdentification,
			new StringField
				.Builder("brand")
				.setMaxLength(80)
				.build());

		assertField(participantIdentification, Fields.name().setMaxLength(80).build());
		assertField(participantIdentification, Fields.cnpjNumber().setPattern("\\d{14}$").build());

		assertField(participantIdentification,
			new StringField
				.Builder("urlComplementaryList")
				.setMaxLength(1024)
				.setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
				.setOptional()
				.build());
	}
}
