package net.openid.conformance.openbanking_brasil.creditOperations.financing.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.CommonFields;
import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger/openinsurance/financings/v2/swagger_financings_apis-v2.yaml
 * Api endpoint: /contracts/{contractId}
 * Api version: 2.0.1.final
 */
@ApiName("Financing Contract V2")
public class FinancingContractResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);

	public static final Set<String> PRODUCT_TYPE = SetUtils.createSet("FINANCIAMENTOS, FINANCIAMENTOS_RURAIS, FINANCIAMENTOS_IMOBILIARIOS");
	public static final Set<String> PRODUCT_SUB_TYPE = SetUtils.createSet("AQUISICAO_BENS_VEICULOS_AUTOMOTORES, AQUISICAO_BENS_OUTROS_BENS, MICROCREDITO, CUSTEIO, INVESTIMENTO, INDUSTRIALIZACAO, COMERCIALIZACAO, FINANCIAMENTO_HABITACIONAL_SFH, FINANCIAMENTO_HABITACIONAL_EXCETO_SFH");
	public static final Set<String> INSTALMENT_PERIODICITY = SetUtils.createSet("SEM_PERIODICIDADE_REGULAR, SEMANAL, QUINZENAL, MENSAL, BIMESTRAL, TRIMESTRAL, SEMESTRAL, ANUAL, OUTROS");
	public static final Set<String> AMORTIZATION_SCHEDULED = SetUtils.createSet("SAC, PRICE, SAM, SEM_SISTEMA_AMORTIZACAO, OUTROS");
	public static final Set<String> CHARGE_TYPE = SetUtils.createSet("JUROS_REMUNERATORIOS_POR_ATRASO, MULTA_ATRASO_PAGAMENTO, JUROS_MORA_ATRASO, IOF_CONTRATACAO, IOF_POR_ATRASO, SEM_ENCARGO, OUTROS");
	public static final Set<String> FEE_CHARGE_TYPE = SetUtils.createSet("UNICA, POR_PARCELA");
	public static final Set<String> FEE_CHARGE = SetUtils.createSet("MINIMO, MAXIMO, FIXO, PERCENTUAL");
	public static final Set<String> TAX_TYPE = SetUtils.createSet("NOMINAL, EFETIVA");
	public static final Set<String> INTEREST_RATE_TYPE = SetUtils.createSet("SIMPLES, COMPOSTO");
	public static final Set<String> TAX_PERIODICITY = SetUtils.createSet("AM, AA");
	public static final Set<String> CALCULATIONS = SetUtils.createSet("21/252, 30/360, 30/365");
	public static final Set<String> REFERENTIAL_RATE_INDEXER_TYPE = SetUtils.createSet("SEM_TIPO_INDEXADOR, PRE_FIXADO, POS_FIXADO, FLUTUANTES, INDICES_PRECOS, CREDITO_RURAL, OUTROS_INDEXADORES");
	public static final Set<String> REFERENTIAL_RATE_INDEXER_SUB_TYPE = SetUtils.createSet("SEM_SUB_TIPO_INDEXADOR, PRE_FIXADO, TR_TBF, TJLP, LIBOR, TLP, OUTRAS_TAXAS_POS_FIXADAS, CDI, SELIC, OUTRAS_TAXAS_FLUTUANTES, IGPM, IPCA, IPCC, OUTROS_INDICES_PRECO, TCR_PRE, TCR_POS, TRFC_PRE, TRFC_POS, OUTROS_INDEXADORES");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectField
				.Builder(ROOT_PATH)
				.setValidator(this::assertInnerFields)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertInnerFields(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("contractNumber")
				.setMaxLength(100)
				.setPattern("^\\d{1,100}$")
				.build());

		assertField(data,
			new StringField
				.Builder("ipocCode")
				.setMaxLength(67)
				.setMinLength(22)
				.setPattern("^\\d{22,67}$")
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
				.build());

		assertField(data,
			new StringField
				.Builder("productSubType")
				.setEnums(PRODUCT_SUB_TYPE)
				.build());

		assertField(data,
			new DatetimeField
				.Builder("contractDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.build());

		assertField(data,
			new StringArrayField
				.Builder("disbursementDates")
				.setMinItems(1)
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setOptional()
				.build());

		assertField(data,
			new DatetimeField
				.Builder("settlementDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("contractAmount")
				.setMaxLength(20)
				.setMinLength(4)
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.setOptional()
				.build());

		assertField(data,
			CommonFields
				.currency()
				.setOptional()
				.build());

		assertField(data,
			new	DatetimeField
				.Builder("dueDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("instalmentPeriodicity")
				.setEnums(INSTALMENT_PERIODICITY)
				.build());

		assertField(data,
			new StringField
				.Builder("instalmentPeriodicityAdditionalInfo")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(50)
				.setOptional()
				.build());

		assertField(data,
			new	DatetimeField
				.Builder("firstInstalmentDueDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("CET")
				.setMaxLength(9)
				.setMinLength(8)
				.setPattern("^\\d{1,2}\\.\\d{6}$")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("amortizationScheduled")
				.setEnums(AMORTIZATION_SCHEDULED)
				.build());

		assertField(data,
			new StringField
				.Builder("amortizationScheduledAdditionalInfo")
				.setMaxLength(200)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("interestRates")
				.setValidator(this::assertInnerFieldsInterestRates)
				.setMinItems(0)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("contractedFees")
				.setValidator(this::assertInnerFieldsContractedFees)
				.setMinItems(0)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("contractedFinanceCharges")
				.setValidator(this::assertInnerFieldsContractedFinanceCharges)
				.setMinItems(0)
				.build());
	}

	private void assertInnerFieldsContractedFinanceCharges(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("chargeType")
				.setEnums(CHARGE_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("chargeAdditionalInfo")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(50)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("chargeRate")
				.setOptional()
				.setMaxLength(8)
				.setMinLength(8)
				.setPattern("^[01]\\.\\d{6}$")
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
				.setEnums(FEE_CHARGE_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("feeCharge")
				.setEnums(FEE_CHARGE)
				.build());

		assertField(data,
			new StringField
				.Builder("feeAmount")
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.setMinLength(4)
				.setMaxLength(20)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("feeRate")
				.setMaxLength(8)
				.setMinLength(8)
				.setPattern("^[01]\\.\\d{6}$")
				.setOptional()
				.build());
	}

	private void assertInnerFieldsInterestRates(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("taxType")
				.setEnums(TAX_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("interestRateType")
				.setEnums(INTEREST_RATE_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("taxPeriodicity")
				.setEnums(TAX_PERIODICITY)
				.build());

		assertField(data,
			new StringField
				.Builder("calculation")
				.setEnums(CALCULATIONS)
				.build());

		assertField(data,
			new StringField
				.Builder("referentialRateIndexerType")
				.setEnums(REFERENTIAL_RATE_INDEXER_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("referentialRateIndexerSubType")
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
			new StringField
				.Builder("preFixedRate")
				.setMaxLength(9)
				.setMinLength(8)
				.setPattern("^\\d{1,2}\\.\\d{6}$")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("postFixedRate")
				.setMaxLength(9)
				.setMinLength(8)
				.setPattern("^\\d{1,2}\\.\\d{6}$")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(1200)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());
	}
}
