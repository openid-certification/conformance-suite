package net.openid.conformance.openbanking_brasil.channels;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_channels_apis.yaml
 * Api endpoint: /banking-agents
 * Api version: 1.0.2
 * Api git hash: ba747ce30bdf7208a246ebf1e8a2313f85263d91
 */
@ApiName("Banking Agents Channels")
public class BankingAgentsChannelValidator extends AbstractJsonAssertingCondition {

	private final static Set<String> NAMES_ENUM = Sets.newHashSet(
		"RECEPCAO_ENCAMINHAMENTO_PROPOSTAS_ABERTURA_CONTAS_DEPOSITOS_VISTA_PRAZO_POUPANCA_MANTIDOS_INSTITUICAO_CONTRATANTE",
		"REALIZACAO_RECEBIMENTOS_PAGAMENTOS_TRANSFERENCIAS_ELETRONICAS_VISANDO_MOVIMENTACAO_CONTAS_DEPOSITOS_TITULARIDADE_CLIENTES_MANTIDAS_INSTITUICAO_CONTRATANTE",
		"RECEBIMENTOS_PAGAMENTOS_QUALQUER_NATUREZA_OUTRAS_ATIVIDADES_DECORRENTES_EXECUCAO_CONTRATOS_CONVENIOS_PRESTACAO_SERVICOS",
		"EXECUCAO_ATIVA_PASSIVA_ORDENS_PAGAMENTO_CURSADAS_INTERMEDIO_INSTITUICAO_CONTRATANTE_SOLICITACAO_CLIENTES_USUARIOS",
		"RECEPCAO_ENCAMINHAMENTO_PROPOSTAS_OPERACAO_CREDITO_ARRENDAMENTO_MERCANTIL_CONCESSAO_INSTITUICAO_CONTRATANTE",
		"RECEBIMENTOS_PAGAMENTOS_RELACIONADOS_LETRAS_CAMBIO_ACEITE_INSTITUICAO_CONTRATANTE",
		"RECEPCAO_ENCAMINHAMENTO_PROPOSTAS_FORNECIMENTO_CARTAO_CREDITO_RESPONSABILIDADE_INSTITUICAO_CONTRATANTE",
		"REALIZACAO_OPERACOES_CAMBIO_RESPONSABILIDADE_INSTITUICAO_CONTRATANTE", "OUTRO");

	private final static Set<String> CODES_ENUM = Sets.newHashSet("RECEBE_ENCAMINHA_PROPOSTAS_ABERTURA_CONTAS",
		"REALIZA_RECEBIMENTOS_PAGAMENTOS_TRANSFERENCIAS_ELETRONICAS",
		"RECEBIMENTOS_PAGAMENTOS_QUALQUER_NATUREZA_EXECUCAO_CONTRATOS_CONVENIO",
		"EXECUCAO_ATIVA_PASSIVA_ORDENS_PAGAMENTO", "RECEBE_ENCAMINHA_PROPOSTAS_CREDITO_ARRENDAMENTO_MERCANTIL",
		"RECEBE_PAGAMENTOS_RELACIONADOS_LETRAS_CAMBIO_ACEITE_INSTITUICAO",
		"RECEBE_ENCAMINHA_PROPOSTAS_FORNECIMENTO_CARTAO_CREDITO", "REALIZA_OPERACOES_CAMBIO, OUTROS");

	private final ChannelsCommonParts parts;
	public BankingAgentsChannelValidator() {
		parts = new ChannelsCommonParts(this);
	}
	private static class Fields extends CommonFields {}

	@Override
	public Environment evaluate(Environment environment) {
		setLogOnlyFailure();
		JsonElement body = bodyFrom(environment);

		assertField(body, new ObjectField.Builder(ROOT_PATH).setValidator(
			data -> assertField(data, new ObjectField.Builder("brand").setValidator(
				brand -> {
					assertField(brand, Fields.name().build());
					assertField(brand,
						new ObjectArrayField.Builder("companies")
							.setMinItems(1)
							.setValidator(this::assertCompanies)
							.build());}
			).build())
		).build());

		logFinalStatus();
		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, Fields.cnpjNumber().build());
		assertField(companies, Fields.name().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("contractors")
				.setValidator(this::assertContractors)
				.setMinItems(1)
				.build());
	}

	private void assertContractors(JsonObject contractors) {
		assertField(contractors, Fields.cnpjNumber().build());
		assertField(contractors, Fields.name().setMaxLength(100).build());

		assertField(contractors,
			new ObjectArrayField
				.Builder("bankingAgents")
				.setValidator(this::assertBankingAgents)
				.setMinItems(1)
				.setOptional()
				.build());
	}

	private void assertBankingAgents(JsonObject bankingAgents) {
		assertField(bankingAgents,
			new ObjectField
				.Builder("identification")
				.setValidator(this::assertIdentification)
				.build());

		assertField(bankingAgents,
			new ObjectArrayField
				.Builder("locations")
				.setValidator(this::assertLocations)
				.setMinItems(1)
				.build());

		assertField(bankingAgents,
			new ObjectArrayField
				.Builder("services")
				.setValidator(this::assertServices)
				.setMinItems(1)
				.setMaxItems(20)
				.build());
	}

	private void assertServices(JsonObject services) {
		assertField(services,
			new StringField
				.Builder("name")
				.setEnums(NAMES_ENUM)
				.build());

		assertField(services,
			new StringField
				.Builder("code")
				.setEnums(CODES_ENUM)
				.build());

		assertField(services,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(2000)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());
	}

	private void assertIdentification(JsonObject identification) {
		assertField(identification,
			new StringField
				.Builder("cnpjNumber")
				.setPattern("^(\\d{14})$|^NA$")
				.setMaxLength(14)
				.build());

		assertField(identification,
			new StringField
				.Builder("corporationName")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(100)
				.build());

		assertField(identification,
			new StringField
				.Builder("groupName")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(identification,
			new BooleanField
				.Builder("isUnderestablishment")
				.setOptional()
				.build());
	}

	private void assertLocations(JsonObject locations) {
		parts.assertPostalAddress(locations, false);
		parts.assertAvailability(locations);
		parts.assertPhones(locations);
	}
}
