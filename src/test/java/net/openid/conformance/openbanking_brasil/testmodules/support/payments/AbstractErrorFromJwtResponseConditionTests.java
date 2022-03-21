package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonObjectBuilder;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class AbstractErrorFromJwtResponseConditionTests {

	@Test
	void willPassIfExpectedErorrPresent() {

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.subject("alice")
			.issuer("https://c2id.com")
			.expirationTime(new Date(new Date().getTime() + 60 * 1000))
			.claim("errors", List.of(
				Map.of("code", "COBRANCA_INVALIDA", "title", "Cobrança inválida","detail", "Validação de expiração, validação de vencimento, Status Válido.")))
			.build();
		PlainJWT jwt = new PlainJWT(claimsSet);
		String serialisedJwt = jwt.serialize();

		JsonObject body = new JsonObjectBuilder()
			.addField("status", 422)
			.addField("body", serialisedJwt)
			.build();

		Environment environment = new Environment();
		environment.putObject("body", body);

		ContrivedErrorFromJwtCondition condition = new ContrivedErrorFromJwtCondition();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		condition.evaluate(environment);

	}

	@Test
	void willFailIfExpectedErorrNotPresent() {

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.subject("alice")
			.issuer("https://c2id.com")
			.expirationTime(new Date(new Date().getTime() + 60 * 1000))
			.claim("errors", List.of(
				Map.of("code", "VALOR_INVALIDO", "title", "Valor inválido.", "detail", "O valor enviado não é válido para o QR Code informado."))
			)
			.build();
		PlainJWT jwt = new PlainJWT(claimsSet);
		String serialisedJwt = jwt.serialize();

		JsonObject body = new JsonObjectBuilder()
			.addField("status", 422)
			.addField("body", serialisedJwt)
			.build();

		Environment environment = new Environment();
		environment.putObject("body", body);

		ContrivedErrorFromJwtCondition condition = new ContrivedErrorFromJwtCondition();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		try {
			condition.evaluate(environment);
			fail("Should have failed");
		} catch (ConditionError ce) {}

	}

	@Test
	void willPassIfMultipleErrorsPresentIncludingExpected() {

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.subject("alice")
			.issuer("https://c2id.com")
			.expirationTime(new Date(new Date().getTime() + 60 * 1000))
			.claim("errors", List.of(
				Map.of("code", "VALOR_INVALIDO", "title", "Valor inválido.", "detail", "O valor enviado não é válido para o QR Code informado."),
				Map.of("code", "COBRANCA_INVALIDA", "title", "Cobrança inválida","detail", "Validação de expiração, validação de vencimento, Status Válido.")))
			.build();
		PlainJWT jwt = new PlainJWT(claimsSet);
		String serialisedJwt = jwt.serialize();

		JsonObject body = new JsonObjectBuilder()
			.addField("status", 422)
			.addField("body", serialisedJwt)
			.build();

		Environment environment = new Environment();
		environment.putObject("body", body);

		ContrivedErrorFromJwtCondition condition = new ContrivedErrorFromJwtCondition();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		condition.evaluate(environment);

	}

	@Test
	void willFailIfMultipleErrorsPresentButNotExpected() {

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.subject("alice")
			.issuer("https://c2id.com")
			.expirationTime(new Date(new Date().getTime() + 60 * 1000))
			.claim("errors", List.of(
				Map.of("code", "VALOR_INVALIDO", "title", "Valor inválido.", "detail", "O valor enviado não é válido para o QR Code informado."),
				Map.of("code", "COBRANCA_BATMAN", "title", "Cobrança inválida","detail", "Validação de expiração, validação de vencimento, Status Válido.")))
			.build();
		PlainJWT jwt = new PlainJWT(claimsSet);
		String serialisedJwt = jwt.serialize();

		JsonObject body = new JsonObjectBuilder()
			.addField("status", 422)
			.addField("body", serialisedJwt)
			.build();

		Environment environment = new Environment();
		environment.putObject("body", body);

		ContrivedErrorFromJwtCondition condition = new ContrivedErrorFromJwtCondition();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		try {
			condition.evaluate(environment);
			fail("Should have failed");
		} catch (ConditionError ce) {}

	}

}
