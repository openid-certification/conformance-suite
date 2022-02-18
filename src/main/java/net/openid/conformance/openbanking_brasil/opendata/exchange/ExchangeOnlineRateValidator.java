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
 * Api: swagger/opendata/swagger-exchange.yaml
 * Api endpoint: /online-rates
 * Api version: 1.0.0
 * Git hash: f3774e4268d7cd7c8a5977a31dae8f727cc9153d
 */

@ApiName("Online Rate")
public class ExchangeOnlineRateValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductNServicesCommonFields { }

	public static final Set<String> DELIVERY_CURRENCY = Sets.newHashSet("ESPECIE", "CARTAO_PRE_PAGO", "TELETRANSMISSAO_SWIFT");
	public static final Set<String> TRANSANCTION_TYPE = Sets.newHashSet("COMPRA", "VENDA");
	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PF", "PJ");
	public static final Set<String> TRANSANCTION_CATEGORY = Sets.newHashSet("COMERCIO_EXTERIOR","TRANSPORTE","SEGUROS","VIAGENS_INTERNACIONAIS","TRANSFERENCIAS_UNILATERAIS","SERVICOS_DIVERSOS","RENDAS_CAPITAIS","CAPITAIS_BRASILEIROS","CAPITAIS_ESTRANGEIROS","PRESTACAO_SERVICO_PAGAMENTO_OU_TRANSFERENCIA_INTERNACIONAL_EFX");

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
							.Builder("participant")
							.setValidator(this::assertParticipantIdentification)
							.build());

					assertField(data,
						new ObjectArrayField
							.Builder("values")
							.setValidator(this::assertValues)
							.build());

					assertField(data,
						new DatetimeField
							.Builder("timestamp")
							.build());

					assertField(data,
						new StringField
							.Builder("disclaimer")
							.build());


				}).mustNotBeEmpty().build());

		logFinalStatus();
		return environment;
	}

	private void assertValues(JsonObject values) {
		assertField(values,
			new StringField
				.Builder("foreignCurrency")
				.setPattern("^[A-Z]{3}$")
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
				.Builder("transactionCategory")
				.setMaxLength(68)
				.setEnums(TRANSANCTION_CATEGORY)
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
