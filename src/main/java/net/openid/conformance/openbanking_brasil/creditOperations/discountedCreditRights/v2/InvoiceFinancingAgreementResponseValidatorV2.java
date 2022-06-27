package net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openinsurance.validator.OpenBankingLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger/openinsurance/discountedCreditRights/swagger-invoice-financings.yaml
 * Api endpoint: /contracts/{contractId}
 * Api version: 2.0.0-RC1.0
 */
@ApiName("Invoice Financing Agreement V2")
public class InvoiceFinancingAgreementResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final OpenBankingLinksAndMetaValidator linksAndMetaValidator = new OpenBankingLinksAndMetaValidator(this);

	final Set<String> PRODUCT_TYPE = SetUtils.createSet("DIREITOS_CREDITORIOS_DESCONTADOS");
	final Set<String> PRODUCT_SUB_TYPE = SetUtils.createSet("DESCONTO_DUPLICATAS, DESCONTO_CHEQUES, ANTECIPACAO_FATURA_CARTAO_CREDITO, OUTROS_DIREITOS_CREDITORIOS_DESCONTADOS, OUTROS_TITULOS_DESCONTADOS");
	final Set<String> INSTALMENT_PERIODICITY = SetUtils.createSet("SEM_PERIODICIDADE_REGULAR, SEMANAL, QUINZENAL, MENSAL, BIMESTRAL, TRIMESTRAL, SEMESTRAL, ANUAL, OUTROS");
	final Set<String> AMORTIZATION_SCHEDULED = SetUtils.createSet("SAC, PRICE, SAM, SEM_SISTEMA_AMORTIZACAO, OUTROS");
	final Set<String> TAX_TYPE = SetUtils.createSet("NOMINAL, EFETIVA");
	final Set<String> INTEREST_RATE_TYPE = SetUtils.createSet("SIMPLES, COMPOSTO");
	final Set<String> TAX_PERIODICITY = SetUtils.createSet("AM, AA");
	final Set<String> CALCULATION = SetUtils.createSet("21/252, 30/360, 30/365");
	final Set<String> REFERENTIAL_RATE_INDEXER_TYPE = SetUtils.createSet("SEM_TIPO_INDEXADOR, PRE_FIXADO, POS_FIXADO, FLUTUANTES, INDICES_PRECOS, CREDITO_RURAL, OUTROS_INDEXADORES");
	final Set<String> REFERENTIAL_RATE_INDEXER_SUB_TYPE = SetUtils.createSet("SEM_SUB_TIPO_INDEXADOR, PRE_FIXADO, TR_TBF, TJLP, LIBOR, TLP, OUTRAS_TAXAS_POS_FIXADAS, CDI, SELIC, OUTRAS_TAXAS_FLUTUANTES, IGPM, IPCA, IPCC, OUTROS_INDICES_PRECO, TCR_PRE, TCR_POS, TRFC_PRE, TRFC_POS, OUTROS_INDEXADORES");
	final Set<String> FEE_CHARGE_TYPE = SetUtils.createSet("UNICA, POR_PARCELA");
	final Set<String> FEE_CHARGE = SetUtils.createSet("MINIMO, MAXIMO, FIXO, PERCENTUAL");
	final Set<String> CHARGE_TYPE = SetUtils.createSet("JUROS_REMUNERATORIOS_POR_ATRASO, MULTA_ATRASO_PAGAMENTO, JUROS_MORA_ATRASO, IOF_CONTRATACAO, IOF_POR_ATRASO, SEM_ENCARGO, OUTROS");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body,
			new ObjectField
				.Builder(ROOT_PATH)
				.setValidator(this::assertDataFields)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertDataFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("contractNumber")
				.setMaxLength(100)
				.setPattern("^\\d{1,100}$")
				.build());

		assertField(body,
			new StringField
				.Builder("ipocCode")
				.setMaxLength(67)
				.setMinLength(22)
				.setPattern("^\\d{22,67}$")
				.build());

		assertField(body,
			new StringField
				.Builder("productName")
				.setMaxLength(140)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("productType")
				.setEnums(PRODUCT_TYPE)
				.build());

		assertField(body,
			new StringField
				.Builder("productSubType")
				.setEnums(PRODUCT_SUB_TYPE)
				.build());

		assertField(body,
			new DatetimeField
				.Builder("contractDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.build());

		assertField(body,
			new StringArrayField
				.Builder("disbursementDates")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.setMinItems(1)
				.setOptional()
				.build());

		assertField(body,
			new DatetimeField
				.Builder("settlementDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("contractAmount")
				.setOptional()
				.setMinLength(4)
				.setMaxLength(20)
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(body,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.setOptional()
				.build());

		assertField(body,
			new DatetimeField
				.Builder("dueDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.setMinLength(2)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("instalmentPeriodicity")
				.setEnums(INSTALMENT_PERIODICITY)
				.build());

		assertField(body,
			new StringField
				.Builder("instalmentPeriodicityAdditionalInfo")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(body,
			new DatetimeField
				.Builder("firstInstalmentDueDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.setMinLength(2)
				.build());

		assertField(body,
			new StringField
				.Builder("CET")
				.setMaxLength(8)
				.setMinLength(8)
				.setPattern("^[01]\\.\\d{6}$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("amortizationScheduled")
				.setEnums(AMORTIZATION_SCHEDULED)
				.build());

		assertField(body,
			new StringField
				.Builder("amortizationScheduledAdditionalInfo")
				.setMaxLength(200)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("interestRates")
				.setValidator(this::assertInnerFieldsForInterestRate)
				.setMinItems(0)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("contractedFees")
				.setValidator(this::assertInnerFieldsContractedFees)
				.setMinItems(0)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("contractedFinanceCharges")
				.setValidator(this::assertInnerFieldsCharges)
				.setMinItems(0)
				.build());
	}

	private void assertInnerFieldsForInterestRate(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("taxType")
				.setEnums(TAX_TYPE)
				.build());

		assertField(body,
			new StringField
				.Builder("interestRateType")
				.setEnums(INTEREST_RATE_TYPE)
				.build());

		assertField(body,
			new StringField
				.Builder("taxPeriodicity")
				.setEnums(TAX_PERIODICITY)
				.build());

		assertField(body,
			new StringField
				.Builder("calculation")
				.setEnums(CALCULATION)
				.build());

		assertField(body,
			new StringField
				.Builder("referentialRateIndexerType")
				.setEnums(REFERENTIAL_RATE_INDEXER_TYPE)
				.build());

		assertField(body,
			new StringField
				.Builder("referentialRateIndexerSubType")
				.setEnums(REFERENTIAL_RATE_INDEXER_SUB_TYPE)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("referentialRateIndexerAdditionalInfo")
				.setMaxLength(140)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("preFixedRate")
				.setMaxLength(8)
				.setMinLength(8)
				.setPattern("^[01]\\.\\d{6}$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("postFixedRate")
				.setMaxLength(8)
				.setMinLength(8)
				.setPattern("^[01]\\.\\d{6}$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(1200)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());
	}

	private void assertInnerFieldsContractedFees(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("feeName")
				.setMaxLength(140)
				.setMinLength(2)
				.setPattern("[\\w\\W\\s]+")
				.build());

		assertField(body,
			new StringField
				.Builder("feeCode")
				.setMaxLength(140)
				.setMinLength(2)
				.setPattern("[\\w\\W\\s]+")
				.build());

		assertField(body,
			new StringField
				.Builder("feeChargeType")
				.setEnums(FEE_CHARGE_TYPE)
				.build());

		assertField(body,
			new StringField
				.Builder("feeCharge")
				.setEnums(FEE_CHARGE)
				.build());

		assertField(body,
			new StringField
				.Builder("feeAmount")
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.setMinLength(4)
				.setMaxLength(20)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("feeRate")
				.setPattern("^[01]\\.\\d{6}$")
				.setMaxLength(8)
				.setMinLength(8)
				.setOptional()
				.build());
	}

	private void assertInnerFieldsCharges(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("chargeType")
				.setEnums(CHARGE_TYPE)
				.build());

		assertField(body,
			new StringField
				.Builder("chargeAdditionalInfo")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("chargeRate")
				.setPattern("^[01]\\.\\d{6}$")
				.setMaxLength(8)
				.setMinLength(8)
				.setOptional()
				.build());
	}
}
