package net.openid.conformance.openbanking_brasil.opendata.investments;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.opendata.OpenDataLinksAndMetaValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_investments_apis.yaml
 * Api endpoint: /credit-fixed-incomes
 * Git hash: c90e531a2693825fe55fd28a076367cefcb01ad8
 */

@ApiName("Investments Credit Fixed Incomes")
public class GetFixedIncomeCreditValidator extends AbstractJsonAssertingCondition {
	private static class Fields extends ProductNServicesCommonFields {
	}
	private final OpenDataLinksAndMetaValidator linksAndMetaValidator = new OpenDataLinksAndMetaValidator(this);
	private static final Set<String> INVESTMENT_TYPE = Sets.newHashSet("DEBENTURES", "CRI", "CRA");
	private static final Set<String> INTERVAL = Sets.newHashSet("1_FAIXA", "2_FAIXA", "3_FAIXA", "4_FAIXA");

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
			new ObjectField
				.Builder("participant")
				.setValidator(this::assertParticipant)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("investmentType")
				.setMaxLength(11)
				.setEnums(INVESTMENT_TYPE)
				.build());

		assertField(data,
			new ObjectField
				.Builder("custodyFee")
				.setValidator(this::assertCustodyFee)
				.build());

		assertField(data,
			new ObjectField
				.Builder("loadingRate")
				.setValidator(this::assertCustodyFee)
				.build());

	}

	private void assertCustodyFee(JsonObject custodyFee) {
		assertField(custodyFee,
			new ObjectArrayField
				.Builder("prices")
				.setValidator(this::assertPrices)
				.setMinItems(4)
				.setMaxItems(4)
				.build());

		assertField(custodyFee,
			new StringField.
				Builder("minimum")
				.setMaxLength(8)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
				.build());

		assertField(custodyFee,
			new StringField.
				Builder("maximum")
				.setMaxLength(8)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
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
				.setMaxLength(8)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
				.build());

		assertField(prices,
			new StringField.
				Builder("customerRate")
				.setMaxLength(8)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
				.build());
	}

	private void assertParticipant(JsonObject participantIdentification) {
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
