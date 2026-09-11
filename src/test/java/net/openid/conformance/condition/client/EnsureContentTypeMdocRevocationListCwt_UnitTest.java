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
public class EnsureContentTypeMdocRevocationListCwt_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureContentTypeMdocRevocationListCwt cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureContentTypeMdocRevocationListCwt();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void acceptsTheStatusListContentTypeForTheStatusListMechanism() {
		env.putString(AbstractRevocationListCwtCondition.ENV_MECHANISM, "status_list");
		putResponse("application/statuslist+cwt");
		cond.execute(env);
	}

	@Test
	public void acceptsTheIdentifierListContentTypeForTheIdentifierListMechanism() {
		env.putString(AbstractRevocationListCwtCondition.ENV_MECHANISM, "identifier_list");
		putResponse("application/identifierlist+cwt");
		cond.execute(env);
	}

	@Test
	public void rejectsTheStatusListContentTypeForTheIdentifierListMechanism() {
		env.putString(AbstractRevocationListCwtCondition.ENV_MECHANISM, "identifier_list");
		putResponse("application/statuslist+cwt");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void rejectsMissingContentTypeHeader() {
		env.putString(AbstractRevocationListCwtCondition.ENV_MECHANISM, "status_list");
		JsonObject response = new JsonObject();
		response.add("headers", new JsonObject());
		env.putObject(AbstractRevocationListCwtCondition.ENV_RESPONSE, response);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putResponse(String contentType) {
		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", contentType);
		JsonObject response = new JsonObject();
		response.add("headers", headers);
		env.putObject(AbstractRevocationListCwtCondition.ENV_RESPONSE, response);
	}
}
