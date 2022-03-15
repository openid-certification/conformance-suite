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
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_investments_apis.yaml
 * Api endpoint: /funds
 * Git hash: c90e531a2693825fe55fd28a076367cefcb01ad8
 */

@ApiName("Investments Funds")
public class GetFundsValidator extends AbstractJsonAssertingCondition {
	private static class Fields extends ProductNServicesCommonFields {
	}
	private final OpenDataLinksAndMetaValidator linksAndMetaValidator = new OpenDataLinksAndMetaValidator(this);
	private static final Set<String> ANBIMA_CATEGORY = Sets.newHashSet("RENDA_FIXA", "ACOES", "MULTIMERCADO", "CAMBIAL");
	private static final Set<String> TAXATION = Sets.newHashSet("CURTO_PRAZO", "LONGO_PRAZO", "VARIAVEL");
	private static final Set<String> REDEMPTION_QUOTATION_TERM = Sets.newHashSet("DIAS_CORRIDOS", "DIAS_UTEIS");
	private static final Set<String> FUND_QUOTA_TYPE = Sets.newHashSet("COTA_ABERTURA", "COTA_FECHAMENTO");
	private static final Set<String> PERFORMANCE_FEE_METHOD = Sets.newHashSet("PASSIVO", "ATIVO", "AJUSTE", "OUTROS");
	private static final Set<String> PERFORMANCE_FEE_BENCHMARK = Sets.newHashSet("CDI","IBOVESPA_FECHAMENTO","IBOVESPA","IMA_B","IBRX","IPCA","IMA_B_5","DOLAR_PTAX","IBRX_100","TAXA_SELIC","IMA_B_5_PLUS","IBOVESPA_MEDIO","IRF_M","IMA_GERAL","INPCIGP_M","SMLL_SMALL_CAP","IDA_IPCA","ISE","IRF_M_1","IBRX_50","IDIV_DIVIDENDOS","IFIX","GLOBAL_BDRX","IMA_S","IDKA_IPCA_2A","IRF_M_1_PLUS","OUTROS");

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
				.build());

		assertField(data,
			new StringField
				.Builder("name")
				.setMaxLength(250)
				.build());

		assertField(data,
			new StringField
				.Builder("cnpjNumber")
				.setMaxLength(14)
				.setPattern("^\\d{14}$")
				.build());

		assertField(data,
			new StringField
				.Builder("isinCode")
				.setMaxLength(12)
				.setPattern("^[A-Z]{2}([A-Z0-9]){9}\\d{1}$")
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("admin")
				.setValidator(admin -> {
					assertField(admin, Fields.name().setMaxLength(100).build());
					assertField(admin, Fields.cnpjNumber().setMaxLength(14).setPattern("^\\d{14}$").build());
				})
				.build());

		assertField(data,
			new ObjectField
				.Builder("fundManager")
				.setValidator(fundManager -> {
					assertField(fundManager, Fields.name().setMaxLength(100).build());
					assertField(fundManager, Fields.cnpjNumber().setMaxLength(14).setPattern("^\\d{14}$").build());
				})
				.build());

		assertField(data,
			new StringField
				.Builder("anbimaCategory")
				.setMaxLength(12)
				.setEnums(ANBIMA_CATEGORY)
				.build());

		assertField(data,
			new ObjectField
				.Builder("fees")
				.setValidator(this::assertFees)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("generalConditions")
				.setValidator(this::assertGeneralConditions)
				.build());

		assertField(data,
			new StringField
				.Builder("taxation")
				.setMaxLength(11)
				.setEnums(TAXATION)
				.build());
	}

	private void assertGeneralConditions(JsonObject generalConditions) {
		assertField(generalConditions,
			new ObjectField
				.Builder("minimumAmount")
				.setValidator(minimumAmount -> {
					assertField(minimumAmount,
						new StringField
							.Builder("value")
							.setMinLength(4)
							.setMaxLength(19)
							.setPattern("^\\d{1,16}\\.\\d{2}$")
							.build());

					assertField(minimumAmount,
						new StringField
							.Builder("currency")
							.setPattern("^[A-Z]{3}$")
							.build());
				})
				.build());

		assertField(generalConditions,
			new ObjectField
				.Builder("redemption")
				.setValidator(this::assertRedemption)
				.build());

		assertField(generalConditions,
			new ObjectField
				.Builder("application")
				.setValidator(application -> {
					assertField(application,
						new IntField
							.Builder("quotationDays")
							.setMinValue(0)
							.build());

					assertField(application,
						new StringField
							.Builder("quotationTerm")
							.setMaxLength(13)
							.setEnums(REDEMPTION_QUOTATION_TERM)
							.build());
				})
				.build());

		assertField(generalConditions,
			new StringField
				.Builder("fundQuotaType")
				.setMaxLength(18)
				.setEnums(FUND_QUOTA_TYPE)
				.build());
	}

	private void assertRedemption(JsonObject redemption) {
		assertField(redemption,
			new IntField
				.Builder("quotationDays")
				.setMinValue(0)
				.build());

		assertField(redemption,
			new StringField
				.Builder("quotationTerm")
				.setMaxLength(13)
				.setEnums(REDEMPTION_QUOTATION_TERM)
				.build());

		assertField(redemption,
			new IntField
				.Builder("paymentDays")
				.setMinValue(0)
				.build());

		assertField(redemption,
			new StringField
				.Builder("paymentTerm")
				.setMaxLength(13)
				.setEnums(REDEMPTION_QUOTATION_TERM)
				.build());

		assertField(redemption,
			new IntField
				.Builder("graceDays")
				.setMinValue(0)
				.setOptional()
				.build());
	}

	private void assertFees(JsonObject fees) {
		assertField(fees,
			new StringField
				.Builder("maxAdminFee")
				.setMaxLength(8)
				.setMinLength(3)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
				.setOptional()
				.build());

		assertField(fees,
			new StringField
				.Builder("entryFee")
				.setMaxLength(8)
				.setMinLength(3)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
				.setOptional()
				.build());

		assertField(fees,
			new ObjectField
				.Builder("performanceFee")
				.setValidator(this::assertPerformanceFee)
				.setOptional()
				.build());

		assertField(fees,
			new StringField
				.Builder("exitFee")
				.setMaxLength(8)
				.setMinLength(3)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
				.setOptional()
				.build());
	}

	private void assertPerformanceFee(JsonObject performanceFee) {
		assertField(performanceFee,
			new StringField
				.Builder("method")
				.setMaxLength(7)
				.setEnums(PERFORMANCE_FEE_METHOD)
				.setOptional()
				.build());

		assertField(performanceFee,
			new StringField
				.Builder("benchmark")
				.setEnums(PERFORMANCE_FEE_BENCHMARK)
				.setOptional()
				.build());

		assertField(performanceFee,
			new StringField
				.Builder("benchmarkAdditionalInfo")
				.setOptional()
				.build());

		assertField(performanceFee,
			new StringField
				.Builder("amount")
				.setMaxLength(8)
				.setMinLength(3)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
				.setOptional()
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
