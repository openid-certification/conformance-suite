package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonObjectBuilder;

import java.util.Map;
import java.util.Optional;

public class GeneratePaymentRequestEntityClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config" )
	@PostEnvironment(required = "resource_request_entity_claims")
	public Environment evaluate(Environment env) {

		JsonElement pixPayment = env.getElementFromObject("resource", "brazilPixPayment");
		if(pixPayment == null || !pixPayment.isJsonObject()) {
			throw error("As 'payments' is included in the 'scope' within the test configuration, a payment initiation request JSON object must also be provided in the test configuration.");
		}

		pixPayment = buildFromNewConfigFields(env);
		env.putObject("resource_request_entity_claims", (JsonObject)pixPayment);

		logSuccess(args("resource_request_entity_claims", pixPayment));
		return env;
	}

	private JsonObject buildFromNewConfigFields(Environment env) {
		String cnpjInitiator = extractOrDie(env, "resource", "brazilPixPayment.data.cnpjInitiator");
		String paymentAmount = extractOrDie(env, "resource", "brazilPixPayment.data.payment.amount");

		JsonObjectBuilder paymentRequestObject = new JsonObjectBuilder()
			.addField("data.proxy", DictHomologKeys.PROXY_EMAIL)
			.addField("data.localInstrument", DictHomologKeys.PROXY_EMAIL_STANDARD_LOCALINSTRUMENT)
			.addField("data.remittanceInformation", DictHomologKeys.PROXY_EMAIL_STANDARD_REMITTANCEINFORMATION)

			.addFields( "data.creditorAccount",
				Map.of("ispb", DictHomologKeys.PROXY_EMAIL_ISPB,
					"issuer", DictHomologKeys.PROXY_EMAIL_BRANCH_NUMBER,
					"number", DictHomologKeys.PROXY_EMAIL_ACCOUNT_NUMBER,
					"accountType", DictHomologKeys.PROXY_EMAIL_ACCOUNT_TYPE))

			.addField("data.cnpjInitiator", cnpjInitiator)
			.addField("data.payment.amount", paymentAmount)
			.addField("data.payment.currency", "BRL")
			.addField( "data.payment.ibgeTownCode", DictHomologKeys.PROXY_EMAIL_STANDARD_IBGETOWNCODE);

		return paymentRequestObject.build();
	}

	private String extractOrDie(Environment env, final String key, final String path) {
		Optional<String> string = Optional.ofNullable(env.getString(key, path));
		return string.orElseThrow(() -> error(String.format("Unable to find element %s in config at %s", key, path)));
	}

}
