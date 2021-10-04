package net.openid.conformance.openbanking_brasil.creditOperations.financing;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ArrayField;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * API: swagger_financings_apis.yaml
 * URL: /contracts/{contractId
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 */
@ApiName("Financing Contract")
public class FinancingContractResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertJsonObject(body, ROOT_PATH, this::assertInnerFields);
		return environment;
	}

	private void assertInnerFields(JsonObject data) {
		Set<String> productType = Sets.newHashSet("FINANCIAMENTOS", "FINANCIAMENTOS_RURAIS", "FINANCIAMENTOS_IMOBILIARIOS");
		Set<String> productSubType = Sets.newHashSet("AQUISICAO_BENS_VEICULOS_AUTOMOTORES", "AQUISICAO_BENS_OUTROS_BENS",
			" MICROCREDITO", "CUSTEIO", "INVESTIMENTO", "INDUSTRIALIZACAO",
			"COMERCIALIZACAO", "FINANCIAMENTO_HABITACIONAL_SFH", "FINANCIAMENTO_HABITACIONAL_EXCETO_SFH");
		Set<String> instalmentPeriodicity = Sets.newHashSet("SEM_PERIODICIDADE_REGULAR", "SEMANAL", "QUINZENAL", "MENSAL", "BIMESTRAL", "TRIMESTRAL", "SEMESTRAL", "ANUAL", "OUTROS");
		Set<String> amortizationScheduled = Sets.newHashSet("SAC", "PRICE", "SAM", "SEM_SISTEMA_AMORTIZACAO", "OUTROS");

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
				.setEnums(productType)
				.setMaxLength(27)
				.build());

		assertField(data,
			new StringField
				.Builder("productSubType")
				.setEnums(productSubType)
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
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(data, CommonFields.currency().build());

		assertField(data, CommonFields.dataYYYYMMDD("dueDate").build());

		assertField(data,
			new StringField
				.Builder("instalmentPeriodicity")
				.setEnums(instalmentPeriodicity)
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
				.setEnums(amortizationScheduled)
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
			new ArrayField
				.Builder("interestRates")
				.build());

		assertJsonArrays(data, "interestRates", this::assertInnerFieldsInterestRates);
	}

	private void assertContractedFinanceCharges(JsonObject data) {
		assertField(data,
			new ArrayField
				.Builder("contractedFinanceCharges")
				.build());

		assertJsonArrays(data, "contractedFinanceCharges", this::assertInnerFieldsContractedFinanceCharges);
	}

	private void assertContractedFees(JsonObject data) {
		assertField(data,
			new ArrayField
				.Builder("contractedFees")
				.build());

		assertJsonArrays(data, "contractedFees", this::assertInnerFieldsContractedFees);
	}

	private void assertInnerFieldsContractedFinanceCharges(JsonObject data) {
		Set<String> chargeType = Sets.newHashSet("JUROS_REMUNERATORIOS_POR_ATRASO", "MULTA_ATRASO_PAGAMENTO",
			"JUROS_MORA_ATRASO", "IOF_CONTRATACAO", "IOF_POR_ATRASO", "SEM_ENCARGO","OUTROS");

		assertField(data,
			new StringField
				.Builder("chargeType")
				.setMaxLength(31)
				.setEnums(chargeType)
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
		Set<String> feeChargeType = Sets.newHashSet("UNICA", "POR_PARCELA");
		Set<String> feeCharge = Sets.newHashSet("MINIMO", "MAXIMO", "FIXO", "PERCENTUAL");

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
				.setEnums(feeChargeType)
				.build());

		assertField(data,
			new StringField
				.Builder("feeCharge")
				.setEnums(feeCharge)
				.setMaxLength(10)
				.build());

		assertField(data,
			new DoubleField
				.Builder("feeAmount")
				.setNullable()
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
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
		Set<String> taxType = Sets.newHashSet("NOMINAL", "EFETIVA");
		Set<String> interestRateType = Sets.newHashSet("SIMPLES", "COMPOSTO");
		Set<String> taxPeriodicity = Sets.newHashSet("AM", "AA");
		Set<String> calculations = Sets.newHashSet("21/252", "30/360", "30/365");
		Set<String> referentialRateIndexerType = Sets.newHashSet("SEM_TIPO_INDEXADOR", "PRE_FIXADO",
			"POS_FIXADO", "FLUTUANTES", "INDICES_PRECOS", "CREDITO_RURAL", "OUTROS_INDEXADORES");
		Set<String> referentialRateIndexerSubType = Sets.newHashSet("SEM_SUB_TIPO_INDEXADOR", "PRE_FIXADO", "TR_TBF",
			"TJLP", "LIBOR", "TLP", "OUTRAS_TAXAS_POS_FIXADAS", "CDI", "SELIC", "OUTRAS_TAXAS_FLUTUANTES", "IGPM", "IPCA", "IPCC",
			"OUTROS_INDICES_PRECO", "TCR_PRE", "TCR_POS", "TRFC_PRE", "TRFC_POS", "OUTROS_INDEXADORES");

		assertField(data,
			new StringField
				.Builder("taxType")
				.setEnums(taxType)
				.setMaxLength(7)
				.build());

		assertField(data,
			new StringField
				.Builder("interestRateType")
				.setEnums(interestRateType)
				.setMaxLength(8)
				.build());

		assertField(data,
			new StringField
				.Builder("taxPeriodicity")
				.setMaxLength(2)
				.setEnums(taxPeriodicity)
				.build());

		assertField(data,
			new StringField
				.Builder("calculation")
				.setMaxLength(6)
				.setEnums(calculations)
				.build());

		assertField(data,
			new StringField
				.Builder("referentialRateIndexerType")
				.setEnums(referentialRateIndexerType)
				.setMaxLength(18)
				.build());

		assertField(data,
			new StringField
				.Builder("referentialRateIndexerSubType")
				.setMaxLength(24)
				.setEnums(referentialRateIndexerSubType)
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
