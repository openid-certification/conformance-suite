package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExtractRequestObject_UnitTest {

	String request = "eyJraWQiOiJkZXJpdmVkLWRlZmF1bHRQcm9wZXJ0aWVzQ2xpZW50U2VjcmV0IiwiY3R5IjoiSldUIiwiZW5jIjoiQTI1NkdDTSIsImFsZyI6ImRpciJ9..4x-FeDrAHbVpXhV7.gW6Dmx0TACWDhOdFNKV_uU8XHVqBMMR3UVRdvG919ugjnt_6SH7XcBuBAMVKsTHcUQFwQiEny3WwPWfKLVNSHLEAMAFOvIToUXkMvVaAjqCXqN3fKH2KH2l7ak6Jj2sLE_K4dl4cWPkYKaMTH4gJupbOVGzB_91FTRCsfRlRJU_p68Gl0kaQfW4PYCdG0aGWOSxqStuGLPP8KpT659J3QexCj94AKzWCGMC42xKH4vQILjQGPHwdEgOpUxYafRigkQHNFhjQI2ozbQGydGunwr0lmfMh9tueRTRsVHYBGyCic862phHy1xKWAj8Gvn_rwGOoK3YIZui_PBcxKv8fhnjvKYZymvURJ4QIGFb1LIgtwTWC_iwtko40RY0TZGi6XtFItByjrdpofdtkCR8EDittJTtUtv8dbf2xjU_6xoYR0CGy-Uajpq3EOfQARo6X862uKFsMtAUQUfVEeC1knC6lBnk7v_UNqQ8UOYJvN7tmBlD-EZ_czAqvalP9cEWgS4nwu_t9Uz64YauPn9NBAgRkOI011WT31UJyD3TnKrnW_XLktYddkrngeEjca-Gn-qq1b0hq6qljWgS7Yv4GFpszuoh6WjQzkqB1un8OUyrzhri9o1cRB6zAPriSYsu_HTOedGkaPMUHtAOTTX_OEdaDVcCr0UYplgSpKP6_oHe0neHHONqPmqgUHhONF-vl4jz-dUNfTfxjEG3yt26JAc3MNjrzY0IuIC_iZ4H1j1Q1wgT0rwd7fz2Ht7Y5xk9pDDeRr_dHqDyLcjR6G_5fjUqcMmhQQwJbcyp84TYAal_ZR6von4I6MUI286X89xLLdvS1e55d-Tu8vJjmvtNteueDBAevhogb-jXvTd8uf9GOplcqDiqxHvpG95-9q2sbA2g2IA3-rS95mzDNcZZrvnk4QKFkAjFvGGr6FlFdcOOGsOZPTNy_zZYVn5yyyG-d4IOT32t65Uft5KlL388xhqh9oWjY5ZsyCk0EIATdcebZDFoD7RB_SwMrcyqYr8gk4HzS63kpiZnp_n7uTm2GvqCacJxudbar5pCIOQrM6oDMj-v728_NjLCoIG0u-5xgTTAItfSZQ8vPIc_VyLIMzofkVp5KmMetk63B7EZkf0f8-ZL1E4LCZ6nn5fhsZYrxX-8cD4jDCEVyrzJRWIuyjaqlFH7Dq1FMoxdcn1YDRuKbIP9RvsAZcGGx4_8FNGTzursfFMAhLCmHoAQoAQR_R_-jj2sDdRMiyuw9f59Zvd1Ob9stK9QMs4YH.jSR2ddjij2me04XHT_Obew";

	public JsonObject getClient() {
		String clientConfig = "{ \"client_secret\": \"UjWnZr4u7x!A%D*G-KaPdSgVkYp2s5v8\" }";
		return JsonParser.parseString(clientConfig).getAsJsonObject();
	}

	@Test
	public void succeeds_in_extracting_request_object_when_payload_is_a_direct_symmetric_A256GCM_encrypted_payload() {
		JsonObject jsonObjectIrrelevantForTheTest = new JsonObject();

		Environment env = mock(Environment.class);
		when(env.getString("authorization_endpoint_http_request_params", "request")).thenReturn(request);
		when(env.getObject("client")).thenReturn(getClient());
		when(env.getObject("server_encryption_keys")).thenReturn(jsonObjectIrrelevantForTheTest);

		ArgumentCaptor<JsonObject> extractedRequestObjectCaptor = ArgumentCaptor.forClass(JsonObject.class);
		when(env.putObject(anyString(), extractedRequestObjectCaptor.capture())).thenReturn(jsonObjectIrrelevantForTheTest);

		ExtractRequestObject condition = new ExtractRequestObject();
		condition.setProperties("testId", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.evaluate(env);

		JsonObject extractedRequestedObject = extractedRequestObjectCaptor.getValue();

		assertEquals("{\"kid\":\"derived-defaultPropertiesClientSecret\",\"cty\":\"JWT\",\"enc\":\"A256GCM\",\"alg\":\"dir\"}", extractedRequestedObject.get("jwe_header").toString());
		assertEquals("mytestclient", OIDFJSON.getString(extractedRequestedObject.get("claims").getAsJsonObject().get("client_id")));

	}
}
