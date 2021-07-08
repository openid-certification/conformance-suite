package net.openid.conformance.openbanking_brasil.creditOperations.advances;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ArrayField;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * This is validator for API - Adiantamento a Depositantes - Contrato | Contract
 * See https://openbanking-brasil.github.io/areadesenvolvedor/#adiantamento-a-depositantes-contrato
 */

@ApiName("Advances Contract")
public class AdvancesContractResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertDataFields(body);
		return environment;
	}

	private void assertDataFields(JsonObject body) {
		final Set<String> productType = Set.of("ADIANTAMENTO_A_DEPOSITANTES");
		final Set<String> contractProductSubTypes = Set.of("ADIANTAMENTO_A_DEPOSITANTES");
		final Set<String> contractInstalmentPeriodicity = Set.of("SEM_PERIODICIDADE_REGULAR",
			"SEMANAL", "QUINZENAL", "MENSAL", "BIMESTRAL", "TRIMESTRAL", "SEMESTRAL",
			"ANUAL", "OUTROS");
		final Set<String> contractAmortizationScheduled = Set.of("SAC", "PRICE", "SAM",
			"SEM_SISTEMA_AMORTIZACAO", "OUTROS");

		JsonObject data = findByPath(body, ROOT_PATH).getAsJsonObject();

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
				.build());

		assertField(data,
			new StringField
				.Builder("productType")
				.setEnums(productType)
				.build());

		assertField(data,
			new StringField
				.Builder("productSubType")
				.setEnums(contractProductSubTypes)
				.build());

		assertField(data,
			new DatetimeField
				.Builder("contractDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.build());

		assertField(data,
			new DatetimeField
				.Builder("disbursementDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(data,
			new DatetimeField
				.Builder("settlementDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$|^NA$")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(data,
			new DoubleField
				.Builder("contractAmount")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMinLength(0)
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

		assertField(data,
			new DatetimeField
				.Builder("dueDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.build());

		assertField(data,
			new StringField
				.Builder("instalmentPeriodicity")
				.setEnums(contractInstalmentPeriodicity)
				.build());

		assertField(data,
			new StringField
				.Builder("instalmentPeriodicityAdditionalInfo")
				.setMaxLength(50)
				.build());

		assertField(data,
			new DatetimeField
				.Builder("firstInstalmentDueDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.build());

		assertField(data,
			new DoubleField
				.Builder("CET")
				.setMaxLength(19)
				.build());

		assertField(data,
			new StringField
				.Builder("amortizationScheduled")
				.setMaxLength(24)
				.setEnums(contractAmortizationScheduled)
				.build());

		assertField(data,
			new StringField
				.Builder("amortizationScheduledAdditionalInfo")
				.setMaxLength(50)
				.build());

		assertInterestRate(data);
		assertContractedFees(data);
		assertContractedFinanceCharges(data);
	}

	private void assertInterestRate(JsonObject element) {
		assertJsonArrays(element, "interestRates", this::assertInnerFieldsForInterestRate);
	}

	private void assertContractedFees(JsonObject element) {
		assertField(element,
			new ArrayField
				.Builder("contractedFees")
				.setMinItems(1)
				.build());
		assertJsonArrays(element, "contractedFees", this::assertInnerFieldsContractedFees);
	}

	private void assertContractedFinanceCharges(JsonObject element) {
		assertField(element,
			new ArrayField
				.Builder("contractedFinanceCharges")
				.setMinItems(1)
				.build());
		assertJsonArrays(element, "contractedFinanceCharges", this::assertInnerFieldsCharges);
	}

	private void assertInnerFieldsForInterestRate(JsonObject body) {
		final Set<String> contractTaxTypes = Set.of( "NOMINAL", "EFETIVA");
		final Set<String> contractInterestRateTypes = Set.of("SIMPLES", "COMPOSTO");
		final Set<String> contractTaxPeriodicity = Set.of( "AM", "AA");
		final Set<String> contractCalculation = Set.of( "21/252", "30/360", "30/365");
		final Set<String> contractReferentialRateIndexerTypes = Set.of("SEM_TIPO_INDEXADOR",
			"PRE_FIXADO", "POS_FIXADO", "FLUTUANTES", "INDICES_PRECOS",
			"CREDITO_RURAL", "OUTROS_INDEXADORES");
		final Set<String> contractReferentialRateIndexerSubTypes = Set.of("SEM_SUB_TIPO_INDEXADOR",
			"PRE_FIXADO", "TR_TBF", "TJLP", "LIBOR", "TLP", "OUTRAS_TAXAS_POS_FIXADAS", "CDI",
			"SELIC", "OUTRAS_TAXAS_FLUTUANTES", "IGPM", "IPCA", "IPCC", "OUTROS_INDICES_PRECO",
			"TCR_PRE", "TCR_POS", "TRFC_PRE", "TRFC_POS", "OUTROS_INDEXADORES");

		assertField(body,
			new StringField
				.Builder("taxType")
				.setEnums(contractTaxTypes)
				.setMaxLength(10)
				.build());

		assertField(body,
			new StringField
				.Builder("interestRateType")
				.setEnums(contractInterestRateTypes)
				.setMaxLength(10)
				.build());

		assertField(body,
			new StringField
				.Builder("taxPeriodicity")
				.setEnums(contractTaxPeriodicity)
				.setMaxLength(2)
				.build());

		assertField(body,
			new StringField
				.Builder("calculation")
				.setEnums(contractCalculation)
				.setMaxLength(6)
				.build());

		assertField(body,
			new StringField
				.Builder("referentialRateIndexerType")
				.setEnums(contractReferentialRateIndexerTypes)
				.setMaxLength(18)
				.build());

		assertField(body,
			new StringField
				.Builder("referentialRateIndexerSubType")
				.setEnums(contractReferentialRateIndexerSubTypes)
				.setMaxLength(24)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("referentialRateIndexerAdditionalInfo")
				.setMaxLength(140)
				.setOptional()
				.build());

		assertField(body,
			new DoubleField
				.Builder("preFixedRate")
				.setMaxLength(19)
				.build());

		assertField(body,
			new DoubleField
				.Builder("postFixedRate")
				.setMaxLength(19)
				.build());

		assertField(body,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(1200)
				.build());
	}

	private void assertInnerFieldsContractedFees(JsonObject body) {
		final Set<String> contractFeeChargeTypes = Set.of("UNICA", "POR_PARCELA");
		final Set<String> contractFeeCharges = Set.of( "MINIMO", "MAXIMO", "FIXO", "PERCENTUAL");

		assertField(body,
			new StringField
				.Builder("feeName")
				.setMaxLength(140)
				.build());

		assertField(body,
			new StringField
				.Builder("feeCode")
				.setMaxLength(140)
				.build());

		assertField(body,
			new StringField
				.Builder("feeChargeType")
				.setMaxLength(10)
				.setEnums(contractFeeChargeTypes)
				.build());

		assertField(body,
			new StringField
				.Builder("feeCharge")
				.setMaxLength(10)
				.setEnums(contractFeeCharges)
				.build());

		assertField(body,
			new DoubleField
				.Builder("feeAmount")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(body,
			new DoubleField
				.Builder("feeRate")
				.setMaxLength(19)
				.build());
	}

	private void assertInnerFieldsCharges(JsonObject body) {
		final Set<String> contractFeeChargeTypes = Set.of("JUROS_REMUNERATORIOS_POR_ATRASO",
			"MULTA_ATRASO_PAGAMENTO", "JUROS_MORA_ATRASO", "IOF_CONTRATACAO",
			"IOF_POR_ATRASO", "SEM_ENCARGO", "OUTROS");

		assertField(body,
			new StringField
				.Builder("chargeType")
				.setMaxLength(31)
				.setEnums(contractFeeChargeTypes)
				.build());

		assertField(body,
			new StringField
				.Builder("chargeAdditionalInfo")
				.build());

		assertField(body,
			new DoubleField
				.Builder("chargeRate")
				.setMaxLength(19)
				.setOptional()
				.build());
	}
}
