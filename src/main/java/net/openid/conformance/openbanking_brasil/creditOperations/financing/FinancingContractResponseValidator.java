package net.openid.conformance.openbanking_brasil.creditOperations.financing;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * API: swagger_financings_apis.yaml
 * URL: /contracts/{contractId
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 */
@ApiName("Financing Contract")
public class FinancingContractResponseValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> PRODUCT_TYPE = Sets.newHashSet("FINANCIAMENTOS", "FINANCIAMENTOS_RURAIS", "FINANCIAMENTOS_IMOBILIARIOS");
	public static final Set<String> PRODUCT_SUB_TYPE = Sets.newHashSet("AQUISICAO_BENS_VEICULOS_AUTOMOTORES", "AQUISICAO_BENS_OUTROS_BENS",
		" MICROCREDITO", "CUSTEIO", "INVESTIMENTO", "INDUSTRIALIZACAO",
		"COMERCIALIZACAO", "FINANCIAMENTO_HABITACIONAL_SFH", "FINANCIAMENTO_HABITACIONAL_EXCETO_SFH");
	public static final Set<String> INSTALMENT_PERIODICITY = Sets.newHashSet("SEM_PERIODICIDADE_REGULAR", "SEMANAL", "QUINZENAL", "MENSAL", "BIMESTRAL", "TRIMESTRAL", "SEMESTRAL", "ANUAL", "OUTROS");
	public static final Set<String> AMORTIZATION_SCHEDULED = Sets.newHashSet("SAC", "PRICE", "SAM", "SEM_SISTEMA_AMORTIZACAO", "OUTROS");
	public static final Set<String> CHARGE_TYPE = Sets.newHashSet("JUROS_REMUNERATORIOS_POR_ATRASO", "MULTA_ATRASO_PAGAMENTO",
		"JUROS_MORA_ATRASO", "IOF_CONTRATACAO", "IOF_POR_ATRASO", "SEM_ENCARGO", "OUTROS");
	public static final Set<String> FEE_CHARGE_TYPE = Sets.newHashSet("UNICA", "POR_PARCELA");
	public static final Set<String> FEE_CHARGE = Sets.newHashSet("MINIMO", "MAXIMO", "FIXO", "PERCENTUAL");
	public static final Set<String> TAX_TYPE = Sets.newHashSet("NOMINAL", "EFETIVA");
	public static final Set<String> INTEREST_RATE_TYPE = Sets.newHashSet("SIMPLES", "COMPOSTO");
	public static final Set<String> TAX_PERIODICITY = Sets.newHashSet("AM", "AA");
	public static final Set<String> CALCULATIONS = Sets.newHashSet("21/252", "30/360", "30/365");
	public static final Set<String> REFERENTIAL_RATE_INDEXER_TYPE = Sets.newHashSet("SEM_TIPO_INDEXADOR", "PRE_FIXADO",
		"POS_FIXADO", "FLUTUANTES", "INDICES_PRECOS", "CREDITO_RURAL", "OUTROS_INDEXADORES");
	public static final Set<String> REFERENTIAL_RATE_INDEXER_SUB_TYPE = Sets.newHashSet("SEM_SUB_TIPO_INDEXADOR", "PRE_FIXADO", "TR_TBF",
		"TJLP", "LIBOR", "TLP", "OUTRAS_TAXAS_POS_FIXADAS", "CDI", "SELIC", "OUTRAS_TAXAS_FLUTUANTES", "IGPM", "IPCA", "IPCC",
		"OUTROS_INDICES_PRECO", "TCR_PRE", "TCR_POS", "TRFC_PRE", "TRFC_POS", "OUTROS_INDEXADORES");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertField(body, new ObjectField.Builder(ROOT_PATH).setValidator(this::assertInnerFields).build());
		return environment;
	}

	private void assertInnerFields(JsonObject data) {

		assertField(data,
			new StringField
				.Builder("contractNumber")
				.setMaxLength(100)
				.build());

		assertField(data,
			new StringField
				.Builder("ipocCode")
				.setMaxLength(67)
				.build());

		assertField(data,
			new StringField
				.Builder("productName")
				.setMaxLength(140)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(data,
			new StringField
				.Builder("productType")
				.setEnums(PRODUCT_TYPE)
				.setMaxLength(27)
				.build());

		assertField(data,
			new StringField
				.Builder("productSubType")
				.setEnums(PRODUCT_SUB_TYPE)
				.setMaxLength(37)
				.build());

		assertField(data, CommonFields.dataYYYYMMDD("contractDate").build());

		assertField(data, CommonFields.dataYYYYMMDD("disbursementDate").setOptional().build());

		assertField(data,
			new DatetimeField
				.Builder("settlementDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$|^NA$")
				.setMaxLength(10)
				.build());

		assertField(data,
			new DoubleField
				.Builder("contractAmount")
				.setNullable()
				.setMaxLength(20)
				.setMinLength(0)
				.setPattern("^-?\\d{1,15}(\\.\\d{1,4})?$")
				.build());

		assertField(data, CommonFields.currency().build());

		assertField(data, CommonFields.dataYYYYMMDD("dueDate").build());

		assertField(data,
			new StringField
				.Builder("instalmentPeriodicity")
				.setEnums(INSTALMENT_PERIODICITY)
				.setMaxLength(25)
				.build());

		assertField(data,
			new StringField
				.Builder("instalmentPeriodicityAdditionalInfo")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(50)
				.build());

		assertField(data, CommonFields.dataYYYYMMDD("firstInstalmentDueDate").build());

		assertField(data,
			new DoubleField
				.Builder("CET")
				.setMaxLength(19)
				.build());

		assertField(data,
			new StringField
				.Builder("amortizationScheduled")
				.setEnums(AMORTIZATION_SCHEDULED)
				.setMaxLength(23)
				.build());

		assertField(data,
			new StringField
				.Builder("amortizationScheduledAdditionalInfo")
				.setMaxLength(50)
				.build());

		assertInterestRates(data);
		assertContractedFees(data);
		assertContractedFinanceCharges(data);
	}

	private void assertInterestRates(JsonObject data) {
		assertField(data,
			new ObjectArrayField
				.Builder("interestRates")
				.setValidator(this::assertInnerFieldsInterestRates)
				.build());
	}

	private void assertContractedFinanceCharges(JsonObject data) {
		assertField(data,
			new ObjectArrayField
				.Builder("contractedFinanceCharges")
				.setValidator(this::assertInnerFieldsContractedFinanceCharges)
				.build());
	}

	private void assertContractedFees(JsonObject data) {
		assertField(data,
			new ObjectArrayField
				.Builder("contractedFees")
				.setValidator(this::assertInnerFieldsContractedFees)
				.build());
	}

	private void assertInnerFieldsContractedFinanceCharges(JsonObject data) {

		assertField(data,
			new StringField
				.Builder("chargeType")
				.setMaxLength(31)
				.setEnums(CHARGE_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("chargeAdditionalInfo")
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(data,
			new DoubleField
				.Builder("chargeRate")
				.setOptional()
				.setMaxLength(19)
				.build());
	}

	private void assertInnerFieldsContractedFees(JsonObject data) {

		assertField(data,
			new StringField
				.Builder("feeName")
				.setMaxLength(140)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(data,
			new StringField
				.Builder("feeCode")
				.setMaxLength(140)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(data,
			new StringField
				.Builder("feeChargeType")
				.setMaxLength(11)
				.setEnums(FEE_CHARGE_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("feeCharge")
				.setEnums(FEE_CHARGE)
				.setMaxLength(10)
				.build());

		assertField(data,
			new DoubleField
				.Builder("feeAmount")
				.setNullable()
				.setPattern("^-?\\d{1,15}(\\.\\d{1,4})?$")
				.setMinLength(0)
				.setMaxLength(20)
				.build());

		assertField(data,
			new DoubleField
				.Builder("feeRate")
				.setMaxLength(19)
				.setNullable()
				.build());
	}

	private void assertInnerFieldsInterestRates(JsonObject data) {

		assertField(data,
			new StringField
				.Builder("taxType")
				.setEnums(TAX_TYPE)
				.setMaxLength(7)
				.build());

		assertField(data,
			new StringField
				.Builder("interestRateType")
				.setEnums(INTEREST_RATE_TYPE)
				.setMaxLength(8)
				.build());

		assertField(data,
			new StringField
				.Builder("taxPeriodicity")
				.setMaxLength(2)
				.setEnums(TAX_PERIODICITY)
				.build());

		assertField(data,
			new StringField
				.Builder("calculation")
				.setMaxLength(6)
				.setEnums(CALCULATIONS)
				.build());

		assertField(data,
			new StringField
				.Builder("referentialRateIndexerType")
				.setEnums(REFERENTIAL_RATE_INDEXER_TYPE)
				.setMaxLength(18)
				.build());

		assertField(data,
			new StringField
				.Builder("referentialRateIndexerSubType")
				.setMaxLength(24)
				.setEnums(REFERENTIAL_RATE_INDEXER_SUB_TYPE)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("referentialRateIndexerAdditionalInfo")
				.setMaxLength(140)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(data,
			new DoubleField
				.Builder("preFixedRate")
				.setMaxLength(19)
				.build());

		assertField(data,
			new DoubleField
				.Builder("postFixedRate")
				.setMaxLength(19)
				.setNullable()
				.build());

		assertField(data,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(1200)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}
}
