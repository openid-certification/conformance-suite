package net.openid.conformance.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureSelfLinkEndsInPaymentId;
import net.openid.conformance.testmodule.Environment;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class EnsureSelfLinkEndsInPaymentIdTests {

	@Test
	public void passesIfSelfLinkEndsInPaymentId() {

		Environment environment = new Environment();
		JsonObject response = new JsonObject();
		JsonObject data = new JsonObject();
		data.addProperty("paymentId", "abcde12345");
		JsonObject links = new JsonObject();
		links.addProperty("self", "https://obb.example.com/payments/v1/pix/payments/abcde12345");

		response.add("data", data);
		response.add("links", links);

		environment.putObject("resource_endpoint_response", response);
		new JsonObject();

		EnsureSelfLinkEndsInPaymentId condition = new EnsureSelfLinkEndsInPaymentId();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.execute(environment);

	}

	@Test
	public void failsIfSelfLinkDoesNotEndInPaymentId() {

		Environment environment = new Environment();
		JsonObject response = new JsonObject();
		JsonObject data = new JsonObject();
		data.addProperty("paymentId", "abcde12345");
		JsonObject links = new JsonObject();
		links.addProperty("self", "https://obb.example.com/payments/v1/pix/payments/");

		response.add("data", data);
		response.add("links", links);

		environment.putObject("resource_endpoint_response", response);
		new JsonObject();

		EnsureSelfLinkEndsInPaymentId condition = new EnsureSelfLinkEndsInPaymentId();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		try {
			condition.execute(environment);
			fail("This should have thrown an error");
		} catch (ConditionError e) {}

	}

}
