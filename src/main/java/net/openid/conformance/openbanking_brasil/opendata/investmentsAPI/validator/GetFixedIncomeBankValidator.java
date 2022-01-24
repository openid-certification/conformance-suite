package net.openid.conformance.openbanking_brasil.opendata.investmentsAPI.validator;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: https://sensedia.github.io/areadesenvolvedor/swagger/swagger_investments_apis.yaml
 * Api endpoint: /fixed-income-bank
 * Git hash:
 */

@ApiName("Investments Fixed Income Bank")
public class GetFixedIncomeBankValidator extends AbstractJsonAssertingCondition {
	private static class Fields extends ProductNServicesCommonFields {
	}

	private static final Set<String> PRODUCT_TYPE = Sets.newHashSet("CDB","RDB","LCI","LCA");
	private static final Set<String> REDEMPTION_TERM = Sets.newHashSet("DIARIA","NA_DATA_DE_VENCIMENTO","DIARIA_APOS_PRAZO_DE_CARENCIA");
	private static final Set<String> INDEXER = Sets.newHashSet("CDI","DI","TR","IPCA","IGP_M","IGP_DI","INPC","BCP","TLC","SELIC","OUTROS");
	private static final Set<String> INTERVAL = Sets.newHashSet("1_FAIXA","2_FAIXA","3_FAIXA","4_FAIXA");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertData)
				.build());

		logFinalStatus();
		return environment;
	}

	private void assertData(JsonObject data) {
		assertField(data,
			new ObjectField
				.Builder("participantIdentification")
				.setValidator(this::assertParticipantIdentification)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("productIdentification")
				.setValidator(productIdentification -> {
					assertField(productIdentification,
						new StringField
							.Builder("issuerInstitutionCNPJ")
							.setMaxLength(14)
							.setPattern("^\\d{14}$")
							.build());

					assertField(productIdentification,
						new StringField
							.Builder("productType")
							.setMaxLength(3)
							.setEnums(PRODUCT_TYPE)
							.build());
				})
				.build());

		assertField(data,
			new ObjectField
				.Builder("index")
				.setValidator(index -> {
					assertField(index,
						new StringField
							.Builder("indexer")
							.setMaxLength(3)
							.setEnums(INDEXER)
							.build());

					assertField(index,
						new StringField
							.Builder("indexerAdditionalInfo")
							.setMaxLength(50)
							.setOptional()
							.build());

					assertField(index,
						new ObjectField
							.Builder("issueRemunerationRate")
							.setValidator(this::assertIssueRemunerationRate)
							.build());
				})
				.build());

		assertField(data,
			new ObjectField
				.Builder("investmentConditions")
				.setValidator(this::assertInvestmentConditions)
				.build());
	}

	private void assertInvestmentConditions(JsonObject investmentConditions) {
		assertField(investmentConditions,
			new StringField
				.Builder("minimumAmount")
				.setMinLength(4)
				.setMaxLength(19)
				.setPattern("^\\d{1,16}\\.\\d{2}$")
				.build());

		assertField(investmentConditions,
			new StringField.
				Builder("redemptionTerm")
				.setMaxLength(29)
				.setEnums(REDEMPTION_TERM)
				.build());

		assertField(investmentConditions,
			new IntField
				.Builder("minimumExpirationTerm")
				.setMinValue(1)
				.build());

		assertField(investmentConditions,
			new IntField
				.Builder("maximumExpirationTerm")
				.setMinValue(1)
				.build());

		assertField(investmentConditions,
			new IntField
				.Builder("minimumGracePeriod")
				.setMinValue(0)
				.build());

		assertField(investmentConditions,
			new IntField
				.Builder("maximumGracePeriod")
				.setMinValue(0)
				.build());
	}

	private void assertIssueRemunerationRate(JsonObject issueRemunerationRate) {
		assertField(issueRemunerationRate, Fields.name().setMaxLength(200).build());
		assertField(issueRemunerationRate, Fields.code().setMaxLength(200).build());

		assertField(issueRemunerationRate,
			new StringField
				.Builder("chargingTriggerInfo")
				.setMaxLength(200)
				.build());

		assertField(issueRemunerationRate,
			new ObjectArrayField
				.Builder("prices")
				.setValidator(this::assertPrices)
				.setMinItems(4)
				.setMaxItems(4)
				.build());

		assertField(issueRemunerationRate,
			new StringField
				.Builder("minimum")
				.setMaxLength(8)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
				.build());

		assertField(issueRemunerationRate,
			new StringField
				.Builder("maximum")
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
