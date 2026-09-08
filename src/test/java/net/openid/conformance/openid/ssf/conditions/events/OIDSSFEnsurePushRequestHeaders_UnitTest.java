package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the RFC 8935 2.1 push delivery request envelope checks:
 * {@link OIDSSFEnsurePushRequestMethodIsPost}, {@link OIDSSFEnsurePushRequestContentTypeIsSecEventJwt}
 * and {@link OIDSSFEnsurePushRequestAcceptHeaderIncludesJson}.
 */
@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsurePushRequestHeaders_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private <T extends AbstractCondition> T createCondition(T condition) {
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void preparePushRequest(String method, String headersJson) {
		JsonObject pushRequest = new JsonObject();
		if (method != null) {
			pushRequest.addProperty("method", method);
		}
		pushRequest.add("headers", JsonParser.parseString(headersJson));
		JsonObject ssf = new JsonObject();
		ssf.add("push_request", pushRequest);
		env.putObject("ssf", ssf);
	}

	@Test
	void postMethodPasses() {
		preparePushRequest("POST", "{}");
		assertDoesNotThrow(() -> createCondition(new OIDSSFEnsurePushRequestMethodIsPost()).execute(env));
	}

	@Test
	void nonPostMethodFails() {
		preparePushRequest("GET", "{}");
		assertThrows(ConditionError.class, () -> createCondition(new OIDSSFEnsurePushRequestMethodIsPost()).execute(env));
	}

	@Test
	void secEventJwtContentTypePasses() {
		preparePushRequest("POST", "{\"content-type\":\"application/secevent+jwt\"}");
		assertDoesNotThrow(() -> createCondition(new OIDSSFEnsurePushRequestContentTypeIsSecEventJwt()).execute(env));
	}

	@Test
	void secEventJwtContentTypeWithParameterAndCasePasses() {
		preparePushRequest("POST", "{\"content-type\":\"Application/SecEvent+JWT; charset=UTF-8\"}");
		assertDoesNotThrow(() -> createCondition(new OIDSSFEnsurePushRequestContentTypeIsSecEventJwt()).execute(env));
	}

	@Test
	void missingContentTypeFails() {
		preparePushRequest("POST", "{}");
		assertThrows(ConditionError.class, () -> createCondition(new OIDSSFEnsurePushRequestContentTypeIsSecEventJwt()).execute(env));
	}

	@Test
	void wrongContentTypeFails() {
		preparePushRequest("POST", "{\"content-type\":\"application/json\"}");
		assertThrows(ConditionError.class, () -> createCondition(new OIDSSFEnsurePushRequestContentTypeIsSecEventJwt()).execute(env));
	}

	@Test
	void jsonAcceptHeaderPasses() {
		preparePushRequest("POST", "{\"accept\":\"application/json\"}");
		assertDoesNotThrow(() -> createCondition(new OIDSSFEnsurePushRequestAcceptHeaderIncludesJson()).execute(env));
	}

	@Test
	void jsonInAcceptListPasses() {
		preparePushRequest("POST", "{\"accept\":\"text/plain, application/json;q=0.9\"}");
		assertDoesNotThrow(() -> createCondition(new OIDSSFEnsurePushRequestAcceptHeaderIncludesJson()).execute(env));
	}

	@Test
	void missingAcceptHeaderFails() {
		preparePushRequest("POST", "{}");
		assertThrows(ConditionError.class, () -> createCondition(new OIDSSFEnsurePushRequestAcceptHeaderIncludesJson()).execute(env));
	}

	@Test
	void nonJsonAcceptHeaderFails() {
		preparePushRequest("POST", "{\"accept\":\"text/html\"}");
		assertThrows(ConditionError.class, () -> createCondition(new OIDSSFEnsurePushRequestAcceptHeaderIncludesJson()).execute(env));
	}
}
