package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.util.MdocIacaCrl;
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
public class EnsureContentTypeMdocDsCertificateCrl_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureContentTypeMdocDsCertificateCrl cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureContentTypeMdocDsCertificateCrl();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
	}

	@Test
	public void acceptsPkixCrl() {
		putResponse(MdocIacaCrl.CONTENT_TYPE);
		cond.execute(env);
	}

	@Test
	public void rejectsAnotherContentType() {
		putResponse("application/octet-stream");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void rejectsAMissingContentTypeHeader() {
		JsonObject response = new JsonObject();
		response.add("headers", new JsonObject());
		env.putObject(AbstractMdocDsCertificateCrlCondition.ENV_RESPONSE, response);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putResponse(String contentType) {
		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", contentType);
		JsonObject response = new JsonObject();
		response.add("headers", headers);
		env.putObject(AbstractMdocDsCertificateCrlCondition.ENV_RESPONSE, response);
	}
}
