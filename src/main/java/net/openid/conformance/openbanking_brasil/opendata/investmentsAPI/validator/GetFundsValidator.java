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
 * Api endpoint: /funds
 * Git hash:
 */

@ApiName("Investments Funds")
public class GetFundsValidator extends AbstractJsonAssertingCondition {
	private static class Fields extends ProductNServicesCommonFields {
	}

	private static final Set<String> ANBIMA_CATEGORY = Sets.newHashSet("RENDA_FIXA", "ACOES", "MULTIMERCADO", "CAMBIAL");
	private static final Set<String> TAXATION = Sets.newHashSet("CURTO_PRAZO", "LONGO_PRAZO", "VARIAVEL");
	private static final Set<String> REDEMPTION_QUOTATION_TERM = Sets.newHashSet("DIAS_CORRIDOS", "DIAS_UTEIS");
	private static final Set<String> FUND_QUOTA_TYPE = Sets.newHashSet("COTA_DE_ABERTURA", "COTA_DE_FECHAMENTO");
	private static final Set<String> PERFORMANCE_FEE_METHOD = Sets.newHashSet("PASSIVO", "ATIVO", "AJUSTE", "OUTROS");
	private static final Set<String> PERFORMANCE_FEE_BENCHMARK = Sets.newHashSet("CDI","IBOVESPA_FECHAMENTO","IBOVESPA","IMA_B","IBRX","IPCA","IMA_B_5","DOLAR_PTAX","IBRX_100","TAXA_SELIC","IMA_B_5_PLUS","IBOVESPA_MEDIO","IRF_M","IMA_GERAL","INPCIGP_M","SMLL_SMALL_CAP","IDA_IPCA","ISE","IRF_M_1","IBRX_50","IDIV_DIVIDENDOS","IFIX","GLOBAL_BDRX","IMA_S","IDKA_IPCA_2A","IRF_M_1_PLUS","OUTROS");

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
				.build());

		assertField(data,
			new ObjectField
				.Builder("product")
				.setValidator(this::assertProduct)
				.build());

		assertField(data,
			new ObjectField
				.Builder("fees")
				.setValidator(this::assertFees)
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
			new StringField
				.Builder("minimumAmount")
				.setMinLength(4)
				.setMaxLength(19)
				.setPattern("^\\d{1,16}\\.\\d{2}$")
				.build());

		assertField(generalConditions,
			new StringField
				.Builder("currency")
				.setMaxLength(3)
				.build());

		assertField(generalConditions,
			new IntField
				.Builder("redemptionQuotationDays")
				.setMinValue(0)
				.build());

		assertField(generalConditions,
			new StringField
				.Builder("redemptionQuotationTerm")
				.setMaxLength(13)
				.setEnums(REDEMPTION_QUOTATION_TERM)
				.build());

		assertField(generalConditions,
			new IntField
				.Builder("redemptionPaymentTerm")
				.setMinValue(0)
				.build());

		assertField(generalConditions,
			new StringField
				.Builder("redemptionGraceDays")
				.setMaxLength(13)
				.setEnums(REDEMPTION_QUOTATION_TERM)
				.build());

		assertField(generalConditions,
			new IntField
				.Builder("applicationQuotationDays")
				.setMinValue(0)
				.setOptional()
				.build());

		assertField(generalConditions,
			new IntField
				.Builder("applicationQuotationTerm")
				.setMinValue(0)
				.build());

		assertField(generalConditions,
			new StringField
				.Builder("applicationQuotationPeriod")
				.setMaxLength(13)
				.setEnums(REDEMPTION_QUOTATION_TERM)
				.build());

		assertField(generalConditions,
			new StringField
				.Builder("fundQuotaType")
				.setMaxLength(18)
				.setEnums(FUND_QUOTA_TYPE)
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
			new StringField
				.Builder("performanceFeeMethod")
				.setMaxLength(7)
				.setEnums(PERFORMANCE_FEE_METHOD)
				.setOptional()
				.build());

		assertField(fees,
			new StringField
				.Builder("performanceFeeBenchmark")
				.setEnums(PERFORMANCE_FEE_BENCHMARK)
				.setOptional()
				.build());

		assertField(fees,
			new StringField
				.Builder("performanceFeeBenchmarkAdditionalInfo")
				.setOptional()
				.build());

		assertField(fees,
			new StringField
				.Builder("performanceFeeAmount")
				.setMaxLength(8)
				.setMinLength(3)
				.setPattern("^\\d{1}\\.\\d{1,6}$")
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

	private void assertProduct(JsonObject product) {
		assertField(product,
			new StringField
				.Builder("name")
				.setMaxLength(250)
				.build());

		assertField(product,
			new StringField
				.Builder("cnpjNumber")
				.setMaxLength(14)
				.setPattern("^\\d{14}$")
				.build());

		assertField(product,
			new StringField
				.Builder("isinCode")
				.setMaxLength(12)
				.setPattern("^[A-Z]{2}([A-Z0-9]){9}\\d{1}$")
				.setOptional()
				.build());

		assertField(product,
			new StringField
				.Builder("admin")
				.setMaxLength(100)
				.build());

		assertField(product,
			new StringField
				.Builder("adminCnpjNumber")
				.setMaxLength(14)
				.setPattern("^\\d{14}$")
				.build());

		assertField(product,
			new StringField
				.Builder("fundManager")
				.setMaxLength(100)
				.build());

		assertField(product,
			new StringField
				.Builder("fundManagerCnpjNumber")
				.setMaxLength(14)
				.setPattern("^\\d{14}$")
				.build());

		assertField(product,
			new StringField
				.Builder("anbimaCategory")
				.setMaxLength(12)
				.setEnums(ANBIMA_CATEGORY)
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
