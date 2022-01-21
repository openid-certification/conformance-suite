package net.openid.conformance.openbanking_brasil.channels;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_channels_apis.yaml
 * Api endpoint: /shared-automated-teller-machines
 * Api version: 1.0.2
 * Api git hash: ba747ce30bdf7208a246ebf1e8a2313f85263d91
 */

@ApiName("Shared Automated Teller Machines Channels")
public class SharedAutomatedTellerMachinesValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> COUNTRY_SUB_DIVISION_ENUM = Sets.newHashSet("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");
	public static final Set<String> WEEKDAY_ENUM = Sets.newHashSet("DOMINGO", "SEGUNDA_FEIRA", "TERCA_FEIRA", "QUARTA_FEIRA", "QUINTA_FEIRA", "SEXTA_FEIRA", "SABADO");
	public static final Set<String> NAMES_ENUM = Sets.newHashSet("ABERTURA_CONTAS_DEPOSITOS_OU_PAGAMENTO_PRE_PAGA", "SAQUE_MOEDA_EM_ESPECIE",
		"RECEBIMENTOS_PAGAMENTOS_QUALQUER_NATUREZA", "TRANSFERENCIAS_ELETRONICAS_VISANDO_MOVIMENTACAO_CONTAS_DEPOSITOS_OU_PAGAMENTO_TITULARIDADE_CLIENTES",
		"CONSULTA_SALDOS_EXTRATOS_CONTAS_DEPOSITOS_CONTAS_PAGAMENTOS", "APLICACOES_RESGATES_INVESTIMENTOS", "EXECUCAO_ATIVA_PASSIVA_ORDENS_PAGAMENTO_SOLICITACAO_CLIENTES_USUARIOS",
		"DEPOSITOS_MOEDA_ESPECIE_CHEQUE", "OPERACOES_CREDITO_BEM_COMO_OUTROS_SERVICOS_PRESTADOS_ACOMPANHAMENTO_OPERACAO", "CARTAO_CREDITO", "SEGUROS", "OPERACOES_ARRENDAMENTO_MERCANTIL",
		"ABERTURA_CONTA_PAGAMENTO_POS_PAGA", "COMPRA_VENDA_MOEDA_ESTRANGEIRA_ESPECIE", "COMPRA_VENDA_CHEQUE_CHEQUE_VIAGEM_BEM_COMO_CARGA_MOEDA_ESTRANGEIRA_CARTAO_PRE_PAGO",
		"COMPRA_VENDA_OURO", "OUTROS_PRODUTOS_SERVICOS", "CANCELAMENTO", "INFORMACOES", "RECLAMACOES");
	public static final Set<String> CODES_ENUM = Sets.newHashSet("ABRE_CONTA_DEPOSITO_OU_PRE_PAGA", "SAQUE_MOEDA_ESPECIE", "RECEBE_PAGA_QUALQUER_NATUREZA",
		"TRANSFERENCIAS_ELETRONICAS_MOVIMENTA_CONTAS_DEPOSITOS_OU_PAGTO_TITULARES_CLIENTES",
		"CONSULTA_SALDOS_EXTRATOS_CONTAS_DEPOSITOS_PAGTOS", "APLICA_RESGATA_INVESTIMENTOS", "EXECUCAO_ATIVA_PASSIVA_ORDENS_PAGTO",
		"DEPOSITO_MOEDA_ESPECIE_CHEQUE", "OPERA_CREDITO_OUTROS_SERVICOS_ACOMPANHA_OPERACAO", "CARTAO_CREDITO, SEGUROS",
		"OPERA_ARRENDAMENTO_MERCANTIL", "ABERTURA_CONTA_PAGAMENTO_POS_PAGA", "COMPRA_VENDA_MOEDA_ESTRANGEIRA_ESPECIE",
		"COMPRA_VENDA_CHEQUE_CHEQUE_VIAGEM_CARGA_MOEDA_ESTRANGEIRA_CARTAO_PRE_PAGO",
		"COMPRA_VENDA_OURO", "OUTROS_PRODUTOS_SERVICOS", "CANCELAMENTO", "INFORMACOES", "RECLAMACOES");

	private static class Fields extends CommonFields {
	}

	@Override
	public Environment evaluate(Environment environment) {
		setLogOnlyFailure();
		JsonObject body = bodyFrom(environment);

		assertField(body, new ObjectField.Builder(ROOT_PATH).setValidator(
			data -> assertField(data, new ObjectField.Builder("brand").setValidator(
				brand -> {
					assertField(brand, Fields.name().setOptional().build());
					assertField(brand,
						new ObjectArrayField.Builder("companies")
							.setMinItems(1)
							.setValidator(this::assertCompanies)
							.setOptional()
							.build());}
			).build())
		).setOptional().build());

		logFinalStatus();
		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, Fields.name().build());
		assertField(companies, Fields.urlComplementaryList().build());
		assertField(companies, Fields.cnpjNumber().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("sharedAutomatedTellerMachines")
				.setValidator( this::assertSharedAutomatedTellerMachines)
				.setMinItems(0)
				.setMaxItems(1)
				.setOptional()
				.build());
	}

	private void assertSharedAutomatedTellerMachines(JsonObject tellerMachines) {
		assertField(tellerMachines,
			new ObjectField
				.Builder("identification")
				.setValidator(identification -> assertField(identification,
					new StringField
						.Builder("ownerName")
						.setMaxLength(100)
						.setPattern("[\\w\\W\\s]*")
						.setOptional()
						.build()))
				.setOptional()
				.build());

		assertField(tellerMachines,
			new ObjectField
				.Builder("postalAddress")
				.setValidator(this::assertBranchPostalAddress)
				.setOptional()
				.build());

		assertField(tellerMachines,
			new ObjectField
				.Builder("availability")
				.setValidator(this::assertBranchAvailability)
				.setOptional()
				.build());

		assertField(tellerMachines,
			new ObjectArrayField
				.Builder("services")
				.setValidator(this::assertServices)
				.setOptional()
				.build());
	}

	private void assertBranchPostalAddress(JsonObject postalAddress) {

		assertField(postalAddress,
			new StringField
				.Builder("address")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(150)
				.setOptional()
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(30)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("districtName")
				.setMaxLength(50)
				.setPattern("\\w*\\W*")
				.setOptional()
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("townName")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("ibgeCode")
				.setMaxLength(7)
				.setPattern("^\\d{7}$")
				.setOptional()
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("countrySubDivision")
				.setEnums(COUNTRY_SUB_DIVISION_ENUM)
				.setOptional()
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("postCode")
				.setMaxLength(8)
				.setPattern("(\\d{8}|^NA$)")
				.setOptional()
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("country")
				.setMaxLength(80)
				.setPattern("\\w*\\W*")
				.setOptional()
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("countryCode")
				.setOptional()
				.build());

		assertGeographicCoordinates(postalAddress);
	}

	private void assertBranchAvailability(JsonObject availability) {
		assertField(availability,
			new ObjectArrayField
				.Builder("standards")
				.setValidator(this::assertStandards)
				.setOptional()
				.build());

		assertField(availability,
			new StringField
				.Builder("exception")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(availability,
			new BooleanField
				.Builder("isPublicAccessAllowed")
				.setOptional()
				.build());
	}

	private void assertStandards(JsonObject data) {

		assertField(data,
			new StringField
				.Builder("weekday")
				.setEnums(WEEKDAY_ENUM)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("openingTime")
				.setMaxLength(13)
				.setOptional()
				//	.setPattern("^(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)|Z$|^NA$")
				.build());


		assertField(data,
			new StringField
				.Builder("closingTime")
				.setMaxLength(13)
				.setOptional()
				//.setPattern("^(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)|Z$|^NA$")
				.build());
	}

	private void assertServices(JsonObject data) {

		assertField(data,
			new StringField
				.Builder("name")
				.setEnums(NAMES_ENUM)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("code")
				.setEnums(CODES_ENUM)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());
	}
}
