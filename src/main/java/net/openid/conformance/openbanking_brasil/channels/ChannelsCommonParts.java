package net.openid.conformance.openbanking_brasil.channels;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

public class ChannelsCommonParts {

	public static final Set<String> WEEKDAY_ENUM = Sets.newHashSet("DOMINGO", "SEGUNDA_FEIRA", "TERCA_FEIRA", "QUARTA_FEIRA", "QUINTA_FEIRA", "SEXTA_FEIRA", "SABADO");

	private static final Set<String> NAMES_ENUM = Sets.newHashSet("ABERTURA_CONTAS_DEPOSITOS_OU_PAGAMENTO_PRE_PAGA",
		"SAQUE_MOEDA_EM_ESPECIE",
		"RECEBIMENTOS_PAGAMENTOS_QUALQUER_NATUREZA", "TRANSFERENCIAS_ELETRONICAS_VISANDO_MOVIMENTACAO_CONTAS_DEPOSITOS_OU_PAGAMENTO_TITULARIDADE_CLIENTES",
		"CONSULTA_SALDOS_EXTRATOS_CONTAS_DEPOSITOS_CONTAS_PAGAMENTOS", "APLICACOES_RESGATES_INVESTIMENTOS", "EXECUCAO_ATIVA_PASSIVA_ORDENS_PAGAMENTO_SOLICITACAO_CLIENTES_USUARIOS",
		"DEPOSITOS_MOEDA_ESPECIE_CHEQUE", "OPERACOES_CREDITO_BEM_COMO_OUTROS_SERVICOS_PRESTADOS_ACOMPANHAMENTO_OPERACAO", "CARTAO_CREDITO", "SEGUROS", "OPERACOES_ARRENDAMENTO_MERCANTIL",
		"ABERTURA_CONTA_PAGAMENTO_POS_PAGA", "COMPRA_VENDA_MOEDA_ESTRANGEIRA_ESPECIE", "COMPRA_VENDA_CHEQUE_CHEQUE_VIAGEM_BEM_COMO_CARGA_MOEDA_ESTRANGEIRA_CARTAO_PRE_PAGO",
		"COMPRA_VENDA_OURO", "OUTROS_PRODUTOS_SERVICOS", "CANCELAMENTO", "INFORMACOES", "RECLAMACOES");

	private static final Set<String> CODES_ENUM = Sets.newHashSet("ABRE_CONTA_DEPOSITO_OU_PRE_PAGA", "SAQUE_MOEDA_ESPECIE", "RECEBE_PAGA_QUALQUER_NATUREZA",
		"TRANSFERENCIAS_ELETRONICAS_MOVIMENTA_CONTAS_DEPOSITOS_OU_PAGTO_TITULARES_CLIENTES",
		"CONSULTA_SALDOS_EXTRATOS_CONTAS_DEPOSITOS_PAGTOS", "APLICA_RESGATA_INVESTIMENTOS", "EXECUCAO_ATIVA_PASSIVA_ORDENS_PAGTO",
		"DEPOSITO_MOEDA_ESPECIE_CHEQUE", "OPERA_CREDITO_OUTROS_SERVICOS_ACOMPANHA_OPERACAO", "CARTAO_CREDITO", "SEGUROS",
		"OPERA_ARRENDAMENTO_MERCANTIL", "ABERTURA_CONTA_PAGAMENTO_POS_PAGA", "COMPRA_VENDA_MOEDA_ESTRANGEIRA_ESPECIE",
		"COMPRA_VENDA_CHEQUE_CHEQUE_VIAGEM_CARGA_MOEDA_ESTRANGEIRA_CARTAO_PRE_PAGO",
		"COMPRA_VENDA_OURO", "OUTROS_PRODUTOS_SERVICOS", "CANCELAMENTO", "INFORMACOES", "RECLAMACOES");

	private static final Set<String> PHONES_ENUM = Sets.newHashSet("FIXO", "MOVEL");

	private final AbstractJsonAssertingCondition validator;

	public ChannelsCommonParts(AbstractJsonAssertingCondition validator) {
		this.validator = validator;
	}



	public void assertCommonServices(JsonObject data, boolean isOptionalServices) {


		if (isOptionalServices) {
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

						validator.assertField(services,
							new StringField
								.Builder("additionalInfo")
								.setMaxLength(2000)
								.setPattern("[\\w\\W\\s]*")
								.setOptional()
								.build());
					})
					.setMinItems(1)
					.setMaxItems(20)
					.build());
		} else {
			validator.assertField(data,
				new ObjectArrayField
					.Builder("services")
					.setValidator(services -> {
						validator.assertField(services,
							new StringField
								.Builder("name")
								.setEnums(NAMES_ENUM)
								.setOptional()
								.build());

						validator.assertField(services,
							new StringField
								.Builder("code")
								.setEnums(CODES_ENUM)
								.setOptional()
								.build());

						validator.assertField(services,
							new StringField
								.Builder("additionalInfo")
								.setMaxLength(2000)
								.setPattern("[\\w\\W\\s]*")
								.setOptional()
								.build());
					})
					.setMinItems(1)
					.setMaxItems(20)
					.setOptional()
					.build());
		}
	}

	public void assertPostalAddress(JsonObject locations, boolean isOptional) {
		ObjectField.Builder fieldBuilder = new ObjectField.Builder("postalAddress")
			.setValidator(this::assertInnerPostalAddress);
		if (isOptional) {
			fieldBuilder.setOptional();
		}
		validator.assertField(locations,fieldBuilder.build());
	}

	private void assertInnerPostalAddress(JsonObject postalAddress) {
		Set<String> countrySubDivisionEnum = Sets.newHashSet("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

		validator.assertField(postalAddress,
			new StringField
				.Builder("address")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(150)
				.build());

		validator.assertField(postalAddress,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(30)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		validator.assertField(postalAddress,
			new StringField
				.Builder("districtName")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.build());

		validator.assertField(postalAddress,
			new StringField
				.Builder("townName")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.build());

		validator.assertField(postalAddress,
			new StringField
				.Builder("ibgeCode")
				.setMaxLength(7)
				.setPattern("^\\d{7}$")
				.setOptional()
				.build());

		validator.assertField(postalAddress,
			new StringField
				.Builder("countrySubDivision")
				.setEnums(countrySubDivisionEnum)
				.build());

		validator.assertField(postalAddress,
			new StringField
				.Builder("postCode")
				.setMaxLength(8)
				.setPattern("(\\d{8}|^NA$)")
				.build());

		validator.assertField(postalAddress,
			new StringField
				.Builder("country")
				.setMaxLength(80)
				.setPattern("\\w*\\W*")
				.setOptional()
				.build());

		validator.assertField(postalAddress,
			new StringField
				.Builder("countryCode")
				.setOptional()
				.build());

		validator.assertGeographicCoordinates(postalAddress);
	}

	public void assertPhones(JsonObject locations) {
		validator.assertField(locations,
			new ObjectArrayField.Builder("phones")
				.setValidator(this::assertInnerPhones)
				.setOptional()
				.build());
	}

	public void assertAvailability(JsonObject locations) {
		validator.assertField(locations,
			new ObjectField
				.Builder("availability")
				.setValidator(availability -> {
					validator.assertField(availability,
						new ObjectArrayField
							.Builder("standards")
							.setValidator(this::assertStandards)
							.build());

					validator.assertField(availability,
						new StringField
							.Builder("exception")
							.setMaxLength(2000)
							.setPattern("[\\w\\W\\s]*")
							.setOptional()
							.build());

					validator.assertField(availability,
						new BooleanField
							.Builder("isPublicAccessAllowed")
							.setOptional()
							.build());
				})
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
				.setOptional()
				.build());

		validator.assertField(standards,
			new StringField
				.Builder("closingTime")
				.setMaxLength(13)
				.setOptional()
				.build());
	}

	private void assertInnerPhones(JsonObject phones) {

		validator.assertField(phones,
			new StringField
				.Builder("type")
				.setEnums(PHONES_ENUM)
				.setOptional()
				.build());

		validator.assertField(phones,
			new StringField
				.Builder("countryCallingCode")
				.setMaxLength(4)
				.setPattern("^\\d{1,4}$")
				.setOptional()
				.build());

		validator.assertField(phones,
			new StringField
				.Builder("areaCode")
				.setMaxLength(2)
				.setPattern("^\\d{2}$")
				.setOptional()
				.build());

		validator.assertField(phones,
			new StringField
				.Builder("number")
				.setMaxLength(11)
				.setPattern("^([0-9]{8,11})$")
				.setOptional()
				.build());
	}
}
