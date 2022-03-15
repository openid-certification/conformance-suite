package net.openid.conformance.openbanking_brasil.opendata.exchange;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.opendata.OpenDataLinksAndMetaValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api: swagger/opendata/swagger-exchange.yaml
 * Api endpoint: /vet-values
 * Api version: 1.0.0
 * Git hash: f3774e4268d7cd7c8a5977a31dae8f727cc9153d
 */

@ApiName("Online Rate")
public class ExchangeVetValueValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductNServicesCommonFields {}

	public static final Set<String> DELIVERY_CURRENCY = Sets.newHashSet("ESPECIE", "CARTAO_PRE_PAGO", "TELETRANSMISSAO_SWIFT");
	public static final Set<String> TRANSANCTION_TYPE = Sets.newHashSet("COMPRA", "VENDA");
	public static final Set<String> RANGE_TRANSANCTION_CATEGORY = Sets.newHashSet("0,01_200", "200,01_500", "500,01_1.000", "1.000,01_3.000", "3.000,01_10.000", "10.000,01_30.000", "30.000,01_100.000");
	public static final Set<String> TARGET_AUDIENCE = Sets.newHashSet("PESSOA_NATURAL", "PESSOA_JURIDICA", "PESSOA_NATURAL_JURIDICA");
	public static final Set<String> INTERVAL = Sets.newHashSet("1_FAIXA","2_FAIXA","3_FAIXA","4_FAIXA");
	private final OpenDataLinksAndMetaValidator linksAndMetaValidator = new OpenDataLinksAndMetaValidator(this);

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertData)
				.mustNotBeEmpty()
				.build());

		linksAndMetaValidator.assertMetaAndLinks(body);
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
				.Builder("transactionType")
				.setMaxLength(6)
				.setEnums(TRANSANCTION_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("foreignCurrency")
				.setPattern("^[A-Z]{3}$")
				.build());

		assertField(data,
			new StringField
				.Builder("deliveryForeignCurrency")
				.setMaxLength(21)
				.setEnums(DELIVERY_CURRENCY)
				.build());

		assertField(data,
			new StringField
				.Builder("rangeTransactionCategory")
				.setMaxLength(17)
				.setEnums(RANGE_TRANSANCTION_CATEGORY)
				.build());

		assertField(data,
			new StringField
				.Builder("targetAudience")
				.setMaxLength(23)
				.setEnums(TARGET_AUDIENCE)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField.Builder("vetAmount")
				.setValidator(this::assertVetAmount)
				.build());
	}

	private void assertVetAmount(JsonObject vetAmount) {
		assertField(vetAmount, Fields.name().setMaxLength(200).build());
		assertField(vetAmount, Fields.code().setMaxLength(200).build());

		assertField(vetAmount,
			new StringField
				.Builder("chargingTriggerInfo")
				.setMaxLength(200)
				.build());

		assertField(vetAmount,
			new ObjectArrayField.Builder("prices")
				.setValidator(this::assertPrices)
				.setMinItems(4)
				.setMaxItems(4)
				.build());

		assertField(vetAmount,
			new StringField
				.Builder("minimum")
				.setMaxLength(8)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
				.build());

		assertField(vetAmount,
			new StringField
				.Builder("maximum")
				.setMaxLength(8)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
				.build());
	}

	private void assertPrices(JsonObject prices) {
		assertField(prices,
			new StringField
				.Builder("interval")
				.setEnums(INTERVAL)
				.build());

		assertField(prices,
			new StringField
				.Builder("value")
				.setMaxLength(8)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
				.build());

		assertField(prices,
			new StringField
				.Builder("customerRate")
				.setMaxLength(8)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
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
