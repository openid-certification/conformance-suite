package net.openid.conformance.openinsurance.validator.channels;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

public class ChannelsCommonParts {

	public static final Set<String> WEEKDAY_ENUM = Sets.newHashSet("DOMINGO", "SEGUNDA_FEIRA", "TERCA_FEIRA", "QUARTA_FEIRA", "QUINTA_FEIRA", "SEXTA_FEIRA", "SABADO");
	private static final Set<String> NAMES_ENUM = Sets.newHashSet("ALTERACACOES_FORMA_PAGAMENTO",
		"AVISO_SINISTRO",
		"CANCELAMENTO_SUSPENSAO_PAGAMENTO_PREMIOS_CONTRIBUICAO",
		"EFETIVACAO_APORTE",
		"ENDOSSO",
		"ENVIO_DOCUMENTOS",
		"INFORMACOES_GERAIS_DUVIDAS",
		"INFORMACOES_INTERMEDIARIOS",
		"INFORMACOES_SOBRE_SERVICOS_ASSISTENCIAS",
		"INFORMACOES_SOBRE_SORTEIOS",
		"OUVIDORIA_RECEPCAO_SUGESTOES_ELOGIOS",
		"OUVIDORIA_SOLUCAO_EVENTUAIS_DIVERGENCIAS_SOBRE_CONTRATO_SEGURO_CAPITALIZAÇÃO_PREVIDÊNCIA_APOS_ESGOTADOS_CANAIS_REGULARES_ATENDIMENTO_AQUELAS_ORIUNDAS_ORGAOS_REGULADORES_OU_INTEGRANTES_SISTEMA_NACIONAL_DEFESA_CONSUMIDOR",
		"OUVIDORIA_TRATAMENTO_INSATISFACAO_CONSUMIDOR_RELACAO_ATENDIMENTO_RECEBIDO_CANAIS_REGULARES_ATENDIMENTO",
		"OUVIDORIA_TRATAMENTO_RECLAMACOES_SOBRE_IRREGULARDADES_CONDUTA_COMPANHIA",
		"PORTABILIDADE",
		"RECLAMACAO",
		"RESGATE",
		"SEGUNDA_VIA_DOCUMENTOS_CONTRATUAIS",
		"SUGESTOES_ELOGIOS");
	private static final Set<String> CODES_ENUM = Sets.newHashSet("01", "02", "03", "04", "05", "06", "07", "08", "09",
		"10", "11", "12", "13", "14", "15", "16", "17", "18", "19");

	private final AbstractJsonAssertingCondition validator;

	public ChannelsCommonParts(AbstractJsonAssertingCondition validator) {
		this.validator = validator;
	}

	public void assertCommonServices(JsonObject data) {
		validator.assertField(data,
			new ObjectArrayField
				.Builder("services")
				.setValidator(services -> {
					validator.assertField(services,
						new StringField
							.Builder("name")
							.setEnums(NAMES_ENUM)
							.build());

					validator.assertField(services,
						new StringField
							.Builder("code")
							.setEnums(CODES_ENUM)
							.build());
				})
				.setMinItems(1)
				.setMaxItems(20)
				.build());
	}

	public void assertAvailability(JsonObject phoneChannelsAvailability) {
		validator.assertField(phoneChannelsAvailability,
			new ObjectField
				.Builder("availability")
				.setValidator(availability ->
					validator.assertField(availability,
						new ObjectArrayField
							.Builder("standards")
							.setValidator(this::assertStandards)
							.setMinItems(1)
							.setMaxItems(7)
							.build()))
				.setOptional()
				.build());
	}

	public void assertStandards(JsonObject standards) {
		validator.assertField(standards,
			new StringField
				.Builder("weekday")
				.setEnums(WEEKDAY_ENUM)
				.build());

		validator.assertField(standards,
			new StringField
				.Builder("openingTime")
				.setMaxLength(13)
				.setPattern("^(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)|Z$|^NA$")
				.build());

		validator.assertField(standards,
			new StringField
				.Builder("closingTime")
				.setMaxLength(13)
				.setPattern("^(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)|Z$|^NA$")
				.build());
	}
}
