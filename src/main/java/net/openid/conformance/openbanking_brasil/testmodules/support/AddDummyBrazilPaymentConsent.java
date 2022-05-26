package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.DictHomologKeys;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonObjectBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

public class AddDummyBrazilPaymentConsent extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config" )
	public Environment evaluate(Environment env) {

		JsonObjectBuilder brazilPaymentConsentObjectBuild = new JsonObjectBuilder()
			.addFields( "data.loggedUser.document", Map.of("identification", "11111111111", "rel", "CPF"))
			.addFields( "data.creditor",
				Map.of("personType", "PESSOA_NATURAL",
					"cpfCnpj", "11111111111",
					"name", "Pessoa Inexistente"))

			.addFields( "data.payment", Map.of("type", "PIX",
				"currency", "BRL",
				"amount", "0.01"))

			.addFields( "data.payment.details", Map.of("localInstrument", "DICT",
				"proxy", "11111111111"))

			.addFields( "data.payment.details.creditorAccount",
				Map.of("ispb", "99999008",
					"issuer", "0001",
					"number", "0012345678",
					"accountType", "CACC"));

		JsonObject brazilPaymentConsentObject = brazilPaymentConsentObjectBuild.build();

		LocalDate scheduledDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		JsonObjectBuilder.addField(brazilPaymentConsentObject, "data.payment.date", scheduledDate.toString());

		env.putObject("config", "resource.brazilPaymentConsent", brazilPaymentConsentObject);
		logSuccess("Dummy brazilPaymentConsent added successfully", brazilPaymentConsentObject);
		return env;
	}
}
