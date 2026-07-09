package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.EventLog;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtractRequestObjectFromBackchannelEndpointRequest_UnitTest {

	@Test
	public void parsedRequestObjectLogRedactsBindingMessageAndJwtValue() throws Exception {
		CapturingEventLog capturingEventLog = new CapturingEventLog();
		TestInstanceEventLog eventLog = new TestInstanceEventLog("UNIT-TEST", Map.of(), capturingEventLog);
		ExtractRequestObjectFromBackchannelEndpointRequest cond =
			new ExtractRequestObjectFromBackchannelEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		Environment env = new Environment();
		String bindingMessage = "Review https://example.test/consent";

		cond.processRequestObjectString(createSignedJwt(bindingMessage), env);

		JsonObject loggedRequestObject = (JsonObject) capturingEventLog.lastMap.get("request_object");
		assertThat(OIDFJSON.getString(loggedRequestObject.getAsJsonObject("claims").get("binding_message")))
			.isEqualTo("[redacted]");
		assertThat(OIDFJSON.getString(loggedRequestObject.get("value")))
			.isEqualTo("[redacted because binding_message is present]");
		assertThat(env.getString("backchannel_request_object", "claims.binding_message"))
			.isEqualTo(bindingMessage);
	}

	private static String createSignedJwt(String bindingMessage) throws JOSEException {
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.claim("binding_message", bindingMessage)
			.build();
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
		jwt.sign(new MACSigner("01234567890123456789012345678901"));
		return jwt.serialize();
	}

	private static class CapturingEventLog implements EventLog {

		private Map<String, Object> lastMap;

		@Override
		public void log(String testId, String source, Map<String, String> owner, String msg) {
			// This condition logs a map.
		}

		@Override
		public void log(String testId, String source, Map<String, String> owner, JsonObject obj) {
			// This condition logs a map.
		}

		@Override
		public void log(String testId, String source, Map<String, String> owner, Map<String, Object> map) {
			lastMap = map;
		}

		@Override
		public void createIndexes() {
		}
	}
}
