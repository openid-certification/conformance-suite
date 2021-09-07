package net.openid.conformance.openbanking_brasil.productsNServices.accounts;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductsNServicesCommonFields;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductsNServicesCommonValidatorParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;
import java.util.Set;

/**
 * https://openbanking-brasil.github.io/areadesenvolvedor/swagger/swagger_products_services_apis.yaml
 * /personal-accounts
 */

@ApiName("ProductsNServices Personal Accounts")
public class PersonalAccountsValidator extends AbstractJsonAssertingCondition {

	private final ProductsNServicesCommonValidatorParts parts;

	public PersonalAccountsValidator() {
		parts = new ProductsNServicesCommonValidatorParts(this);
	}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertJsonObject(body, ROOT_PATH, this::assertInnerFields);

		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		assertField(body,
			new ObjectField
				.Builder("brand")
				.setValidator(this::assertBrandFields)
				.build());
	}

	private void assertBrandFields(JsonObject brand) {
		assertField(brand, ProductsNServicesCommonFields.name().build());

		assertField(brand,
			new ObjectArrayField
				.Builder("companies")
				.setValidator(this::assertCompanies)
				.setMinItems(1)
				.build());
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, ProductsNServicesCommonFields.cnpjNumber().build());
		assertField(companies, ProductsNServicesCommonFields.name().build());
		assertField(companies, ProductsNServicesCommonFields.urlComplementaryList().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("personalAccounts")
				.setValidator(this::assertPersonalAccounts)
				.setMinItems(1)
				.setMaxItems(3)
				.setOptional()
				.build());
	}

	private void assertPersonalAccounts(JsonObject personalAccounts) {
		Set<String> accountTypes = Sets.newHashSet("CONTA_DEPOSITO_A_VISTA", "CONTA_POUPANCA", "CONTA_PAGAMENTO_PRE_PAGA");
		Set<String> OpeningClosingChannels = Sets.newHashSet("DEPENDENCIAS_PROPRIAS", "CORRESPONDENTES_BANCARIOS", "INTERNET_BANKING", "MOBILE_BANKING", "CENTRAL_TELEFONICA", "CHAT", "OUTROS");
		Set<String> transactionMethods = Sets.newHashSet("MOVIMENTACAO_ELETRONICA", "MOVIMENTACAO_CHEQUE", "MOVIMENTACAO_CARTAO", "MOVIMENTACAO_PRESENCIAL");

		assertField(personalAccounts, ProductsNServicesCommonFields.type(accountTypes).build());

		assertField(personalAccounts,
			new ObjectField
				.Builder("fees")
				.setValidator(this::assertInnerFees)
				.build());

		assertField(personalAccounts,
			new ObjectArrayField
				.Builder("serviceBundles")
				.setValidator(this::assertServiceBundles)
				.setMinItems(1)
				.build());

		assertField(personalAccounts,
			new StringArrayField
				.Builder("openingClosingChannels")
				.setEnums(OpeningClosingChannels)
				.setMinItems(1)
				.setMaxItems(7)
				.build());

		assertField(personalAccounts,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(100)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(personalAccounts,
			new StringArrayField
				.Builder("transactionMethods")
				.setEnums(transactionMethods)
				.setMinItems(1)
				.setMaxItems(4)
				.build());

		assertField(personalAccounts,
			new ObjectField
				.Builder("termsConditions")
				.setValidator(this::assertTermsConditions)
				.build());

		assertField(personalAccounts,
			new ObjectField
				.Builder("incomeRate")
				.setValidator(this::assertIncomeRate)
				.setOptional()
				.build());
	}

	private void assertTermsConditions(JsonObject termsConditions) {
		assertField(termsConditions,
			new ObjectField
				.Builder("minimumBalance")
				.setValidator(this::assertMinimumBalance)
				.build());

		assertField(termsConditions,
			new StringField
				.Builder("elegibilityCriteriaInfo")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(termsConditions,
			new StringField
				.Builder("closingProcessInfo")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}

	private void assertIncomeRate(JsonObject incomeRate) {
		assertField(incomeRate,
			new StringField
				.Builder("savingAccount")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(incomeRate,
			new StringField
				.Builder("prepaidPaymentAccount")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());
	}

	private void assertMinimumBalance(JsonObject minimumBalance) {
		assertField(minimumBalance, ProductsNServicesCommonFields.value().build());
		assertField(minimumBalance, ProductsNServicesCommonFields.currency().build());
	}

	private void assertInnerOtherServices(JsonObject otherServices) {
		assertField(otherServices, ProductsNServicesCommonFields.name().setMaxLength(250).build());

		assertField(otherServices,
			new StringField
				.Builder("code")
				.setMaxLength(100)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(otherServices, ProductsNServicesCommonFields.chargingTriggerInfo().build());

		parts.assertPrices(otherServices);
		parts.applyAssertingForCommonMinimumAndMaximum(otherServices);
	}

	private void assertServiceBundles(JsonObject servicesBundles) {
		assertField(servicesBundles, ProductsNServicesCommonFields.name().setMaxLength(250).build());

		assertField(servicesBundles,
			new ObjectArrayField
				.Builder("services")
				.setValidator(this::assertInnerServices)
				.setMinItems(1)
				.build());

		parts.assertMonthlyPrices(servicesBundles);
		parts.applyAssertingForCommonMinimumAndMaximum(servicesBundles);
	}

	private void assertInnerServices(JsonObject services) {
		assertField(services,
			new StringField
				.Builder("code")
				.setMaxLength(100)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(services, ProductsNServicesCommonFields.chargingTriggerInfo().build());

		assertField(services,
			new StringField
				.Builder("eventLimitQuantity")
				.setMaxLength(6)
				.setPattern("^(\\d{1,6}){1}$")
				.build());

		assertField(services,
			new StringField
				.Builder("freeEventQuantity")
				.setMaxLength(6)
				.setPattern("^(\\d{1,6}){1}$")
				.build());
	}

	private void assertInnerFees(JsonObject fees) {
		assertField(fees,
			new ObjectArrayField
				.Builder("priorityServices")
				.setValidator(this::assertPriorityServices)
				.setMinItems(1)
				.setMaxItems(40)
				.build());

		assertField(fees,
			new ObjectArrayField
				.Builder("otherServices")
				.setValidator(this::assertInnerOtherServices)
				.setMinLength(1)
				.setOptional()
				.build());
	}

	private void assertPriorityServices(JsonObject priorityServices) {
		Set<String> names = Sets.newHashSet("CONFECCAO_CADASTRO_INICIO_RELACIONAMENTO",
			"FORNECIMENTO_2_VIA_CARTAO_FUNCAO_DEBITO", "FORNECIMENTO_2_VIA_CARTAO_FUNCAO_MOVIMENTACAO_CONTA_POUPANCA",
			"EXCLUSAO_CADASTRO_EMITENTES_CHEQUES_SEM_FUNDO_CCF", "CONTRA_ORDEM_REVOGACAO_E_OPOSICAO_OU_SUSTACAO_PAGAMENTO_CHEQUE",
			"FORNECIMENTO_FOLHAS_CHEQUE", "CHEQUE_ADMINISTRATIVO", "CHEQUE_VISADO", "SAQUE_CONTA_DEPOSITO_A_VISTA_POUPANCA_PRESENCIAL_OU_PESSOAL",
			"SAQUE_CONTA_DEPOSITO_A_VISTA_POUPANCA_TERMINAL_AUTOATENDIMENTO", "SAQUE_CONTA_DEPOSITO_A_VISTA_POUPANCA_CORRESPONDENTES_PAIS",
			"DEPOSITO_IDENTIFICADO", "FORNECIMENTO_EXTRATO_MENSAL_CONTA_DEPOSITOS_A_VISTA_E_POUPANCA_PRESENCIAL_OU_PESSOAL",
			"FORNECIMENTO_EXTRATO_MENSAL_CONTA_DEPOSITOS_A_VISTA_E_POUPANCA_TERMINAL_AUTOATENDIMENTO",
			"FORNECIMENTO_EXTRATO_MENSAL_CONTA_DEPOSITOS_A_VISTA_E_POUPANCA_CORRESPONDENTES_PAIS",
			"FORNECIMENTO_EXTRATO_DE_UM_PERIODO_CONTA_DEPOSITOS_A_VISTA_E_POUPANCA_PRESENCIAL_OU_PESSOAL",
			"FORNECIMENTO_EXTRATO_DE_UM_PERIODO_CONTA_DEPOSITOS_A_VISTA_E_POUPANCA_TERMINAL_AUTOATENDIMENTO",
			"FORNECIMENTO_EXTRATO_DE_UM_PERIODO_CONTA_DEPOSITOS_A_VISTA_E_POUPANCA_CORRESPONDENTES_PAIS",
			"FORNECIMENTO_COPIA_MICROFILME_MICROFICHA_ASSEMELHADO", "TRANSFERENCIA_DOC_PESSOAL_OU_PRESENCIAL",
			"TRANSFERENCIA_DOC_TERMINAL_AUTOATENDIMENTO_OUTROS_MEIOS_ELETRONICOS", "TRANSFERENCIA_DOC_INTERNET",
			"TRANSFERENCIA_TED_PESSOAL_OU_PRESENCIAL", "TRANSFERENCIA_TED_TERMINAL_AUTOATENDIMENTO_OUTROS_MEIOS_ELETRONICOS",
			"TRANSFERENCIA_TED_INTERNET", "TRANSFERENCIA_DOC_TED_PESSOAL_OU_PRESENCIAL", "TRANSFERENCIA_DOC_TED_TERMINAL_AUTOATENDIMENTO_OUTROS_MEIOS_ELETRONICOS",
			"TRANSFERENCIA_DOC_TED_INTERNET", "TRANSFERENCIA_ENTRE_CONTAS_PROPRIA_INSTITUICAO_PESSOAL_OU_PRESENCIAL",
			"TRANSFERENCIA_ENTRE_CONTAS_PROPRIA_INSTITUICAO_TERMINAL_AUTOATENDIMENTO_OUTROS_MEIOS_ELETRONICOS_INCLUSIVE_INTERNET",
			"ORDEM_PAGAMENTO", "ANUIDADE_CARTAO_BASICO_NACIONAL", "ANUIDADE_CARTAO_BASICO_INTERNACIONAL", "ANUIDADE_DIFERENCIADA",
			"UTILIZACAO_CANAIS_ATENDIMENTO_RETIRADA_ESPECIE_BRASIL", "UTILIZACAO_CANAIS_ATENDIMENTO_RETIRADA_ESPECIE_EXTERIOR",
			"AVALIACAO_EMERGENCIAL_CREDITO", "FORNECIMENTO_SEGUNDA_VIA_FUNCAO_CREDITO",
			"PAGAMENTO_CONTAS_UTILIZANDO_FUNCAO_CREDITO", "SMS");

		Set<String> codes = Sets.newHashSet("CADASTRO", "2_VIA_CARTAO_DEBITO",
			"2_VIA_CARTAO_POUPANCA", "EXCLUSAO_CCF", "SUSTACAO_REVOGACAO", "FOLHA_CHEQUE",
			"CHEQUE_ADMINISTRATIVO", "CHEQUE_VISADO", "SAQUE_PESSOAL", "SAQUE_TERMINAL",
			"SAQUE_CORRESPONDENTE", "DEPOSITO_IDENTIFICADO", "EXTRATO_MES_P", "EXTRATO_MES_E",
			"EXTRATO_MES_C", "EXTRATO_MOVIMENTO_P", "EXTRATO_MOVIMENTO_E", "EXTRATO_MOVIMENTO_C",
			"MICROFILME", "DOC_PESSOAL", "DOC_ELETRONICO", "DOC_INTERNET", "TED_PESSOAL",
			"TED_ELETRONICO", "TED_INTERNET", "DOC_TED_AGENDADO_P", "DOC_TED_AGENDADO_E",
			"DOC_TED_AGENDADO_I", "TRANSF_RECURSO_P", "TRANSF_RECURSO_E", "ORDEM_PAGAMENTO",
			"ANUIDADE_NACIONAL", "ANUIDADE_INTERNACIONAL", "ANUIDADE_DIFERENCIADA",
			"SAQUE_CARTAO_BRASIL", "SAQUE_CARTAO_EXTERIOR", "AVALIACAO_EMERGENCIAL_CREDITO",
			"EMISSAO_SEGUNDA_VIA", "TARIFA_PAGAMENTO_CONTAS", "SMS");

		assertField(priorityServices,
			new StringField
				.Builder("name")
				.setEnums(names)
				.build());

		assertField(priorityServices,
			new StringField
				.Builder("code")
				.setEnums(codes)
				.build());

		assertField(priorityServices, ProductsNServicesCommonFields.chargingTriggerInfo().build());
		parts.assertPrices(priorityServices);
		parts.applyAssertingForCommonMinimumAndMaximum(priorityServices);
	}
}
