package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureContentTypeIdentifierListCwt_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureContentTypeIdentifierListCwt cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureContentTypeIdentifierListCwt();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void skipsWhenNoResponseRecorded() {
		cond.execute(env);
	}

	@Test
	public void acceptsMatchingContentType() {
		putResponse("application/identifierlist+cwt");
		cond.execute(env);
	}

	@Test
	public void rejectsTheStatusListContentType() {
		putResponse("application/statuslist+cwt");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void rejectsMissingContentTypeHeader() {
		JsonObject response = new JsonObject();
		response.add("headers", new JsonObject());
		env.putObject(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_RESPONSE, response);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putResponse(String contentType) {
		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", contentType);
		JsonObject response = new JsonObject();
		response.add("headers", headers);
		env.putObject(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_RESPONSE, response);
	}
}
