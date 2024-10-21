package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddVpTokenToAuthorizationEndpointResponseParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);

		// FIXME need to generate our own vp_token and presentation submission

		// from https://demo.certification.openid.net/log-detail.html?log=IpwFC08eo5tgfaK
		String vpToken;

		// the one Timo gave me
		vpToken = "eyJ4NWMiOlsiTUlJQ2REQ0NBaHVnQXdJQkFnSUJBakFLQmdncWhrak9QUVFEQWpDQmlERUxNQWtHQTFVRUJoTUNSRVV4RHpBTkJn"+
			"TlZCQWNNQmtKbGNteHBiakVkTUJzR0ExVUVDZ3dVUW5WdVpHVnpaSEoxWTJ0bGNtVnBJRWR0WWtneEVUQVBCZ05WQkFzTUNGUWdRMU1nU1V"+
			"SRk1UWXdOQVlEVlFRRERDMVRVRkpKVGtRZ1JuVnVhMlVnUlZWRVNTQlhZV3hzWlhRZ1VISnZkRzkwZVhCbElFbHpjM1ZwYm1jZ1EwRXdIaGN"+
			"OTWpRd05UTXhNRGd4TXpFM1doY05NalV3TnpBMU1EZ3hNekUzV2pCc01Rc3dDUVlEVlFRR0V3SkVSVEVkTUJzR0ExVUVDZ3dVUW5WdVpHVnpaS"+
			"EoxWTJ0bGNtVnBJRWR0WWtneENqQUlCZ05WQkFzTUFVa3hNakF3QmdOVkJBTU1LVk5RVWtsT1JDQkdkVzVyWlNCRlZVUkpJRmRoYkd4bGRDQlFjbT"+
			"kwYjNSNWNHVWdTWE56ZFdWeU1Ga3dFd1lIS29aSXpqMENBUVlJS29aSXpqMERBUWNEUWdBRU9GQnE0WU1LZzR3NWZUaWZzeXR3QnVKZi83RTdWaFJ"+
			"QWGlObTUyUzNxMUVUSWdCZFh5REsza1Z4R3hnZUhQaXZMUDN1dU12UzZpREVjN3FNeG12ZHVLT0JrRENCalRBZEJnTlZIUTRFRmdRVWlQaENrTEVyR"+
			"FhQTFcyL0owV1ZlZ2h5dyttSXdEQVlEVlIwVEFRSC9CQUl3QURBT0JnTlZIUThCQWY4RUJBTUNCNEF3TFFZRFZSMFJCQ1l3SklJaVpHVnRieTV3YVd"+
			"RdGFYTnpkV1Z5TG1KMWJtUmxjMlJ5ZFdOclpYSmxhUzVrWlRBZkJnTlZIU01FR0RBV2dCVFVWaGpBaVRqb0RsaUVHTWwyWXIrcnU4V1F2akFLQmdncWhrak9QUVFEQWdOSEFEQkVBaUFiZjVUemtjUXpoZldvSW95aTFWTjdkOEk5QnNGS20xTVdsdVJwaDJieUdRSWdLWWtkck5mMnhYUGpWU2JqVy9VLzVTNXZBRUM1WHhjT2FudXNPQnJvQmJVPSIsIk1JSUNlVENDQWlDZ0F3SUJBZ0lVQjVFOVFWWnRtVVljRHRDaktCL0gzVlF2NzJnd0NnWUlLb1pJemowRUF3SXdnWWd4Q3pBSkJnTlZCQVlUQWtSRk1ROHdEUVlEVlFRSERBWkNaWEpzYVc0eEhUQWJCZ05WQkFvTUZFSjFibVJsYzJSeWRXTnJaWEpsYVNCSGJXSklNUkV3RHdZRFZRUUxEQWhVSUVOVElFbEVSVEUyTURRR0ExVUVBd3d0VTFCU1NVNUVJRVoxYm10bElFVlZSRWtnVjJGc2JHVjBJRkJ5YjNSdmRIbHdaU0JKYzNOMWFXNW5JRU5CTUI0WERUSTBNRFV6TVRBMk5EZ3dPVm9YRFRNME1EVXlPVEEyTkRnd09Wb3dnWWd4Q3pBSkJnTlZCQVlUQWtSRk1ROHdEUVlEVlFRSERBWkNaWEpzYVc0eEhUQWJCZ05WQkFvTUZFSjFibVJsYzJSeWRXTnJaWEpsYVNCSGJXSklNUkV3RHdZRFZRUUxEQWhVSUVOVElFbEVSVEUyTURRR0ExVUVBd3d0VTFCU1NVNUVJRVoxYm10bElFVlZSRWtnVjJGc2JHVjBJRkJ5YjNSdmRIbHdaU0JKYzNOMWFXNW5JRU5CTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0RR"+
			"Z0FFWUd6ZHdGRG5jNytLbjVpYkF2Q09NOGtlNzdWUXhxZk1jd1pMOElhSUErV0NST2NDZm1ZL2dpSDkycU1ydTVwL2t5T2l2RTBSQy9JYmRNT052RG9VeWFObU1HUXdIUVlEVlIwT0JCWUVGTlJXR01DSk9PZ09XSVFZeVhaaXY2dTd4WkMrTUI4R0ExVWRJd1FZTUJhQUZOUldHTUNKT09nT1dJUVl5WFppdjZ1N3haQytNQklHQTFVZEV3RUIvd1FJTUFZQkFmOENBUUF3RGdZRFZSMFBBUUgvQkFRREFnR0dNQW9HQ0NxR1NNNDlCQU1DQTBjQU1FUUNJR0VtN3drWktIdC9hdGI0TWRGblhXNnlybndNVVQydTEzNmdkdGwxMFk2aEFpQnVURnF2Vll0aDFyYnh6Q1AweFdaSG1RSzlrVnl4bjhHUGZYMjdFSXp6c3c9PSJdLCJraWQiOiJNSUdVTUlHT3BJR0xNSUdJTVFzd0NRWURWUVFHRXdKRVJURVBNQTBHQTFVRUJ3d0dRbVZ5YkdsdU1SMHdHd1lEVlFRS0RCUkNkVzVrWlhOa2NuVmphMlZ5WldrZ1IyMWlTREVSTUE4R0ExVUVDd3dJVkNCRFV5QkpSRVV4TmpBMEJnTlZCQU1NTFZOUVVrbE9SQ0JHZFc1clpTQkZWVVJKSUZkaGJHeGxkQ0JRY205MGIzUjVjR1VnU1hOemRXbHVaeUJEUVFJQkFnPT0iLCJ0eXAiOiJ2YytzZC1qd3QiLCJhbGciOiJFUzI1NiJ9.eyJwbGFjZV9vZl9iaXJ0aCI6eyJfc2QiOlsiZUtpb1lYa0ZqVS1mSkhFRUtmY0xQMEtkUGZJZnlaaHk0em5DUGRZa0lqcyJdfSwiX3NkIjpbIkM0ZUctRGZrSTZiQUhVcDlZcldFVjItSWpvNkJEdFBw"+
			"VTlEY2h0Z0lTVFEiLCJEcDNtem1BNlFaX2xUSWJGUTRGYlNhTHl6alV1ZEZxSnZoZ3k3dndJRXpnIiwiVWMzUEFmb0p0WG1VX2FDSlA2ZGl6SFp0cGNzNlNtSXhJMVZ3VjU1UDdyUSIsImFDUnRKb1ZwQVlDM0hGOGZGZlRBNmtlenNFR3AtUkZzVWdjM3c5Qm4zNW8iLCJjSlZQVmNuWkZfbHlkZVVlVU13eFI2V05zVmVBTFh5T0gxSUs4Y3MwTWh3IiwiZzdLMTBNN2hQa0RITlEzTmVNaWJUQmNVQ2dlU0RPbTY5VU05d2xPTTZCOCIsInJWX2xLTVNjQnlFeDBtbi1fQjlEc3BtTnVGZ3hjRWR2dGNqajVRbGRrblkiXSwiYWRkcmVzcyI6eyJfc2QiOlsiMlkxbWhRbjFsQ3Z3LXZCNmNmbnVQYkZPd1hPdm9DWlN4WmpTMG04RE9uQSIsIjR6X2VIR1JncVp2WEJfNlo1MVZIMEpQWWI5VjZ5WEpXLURTVEY1aE9oSFUiLCJDamJQSEF6WDkyRldNMkluRzdrNUlQdUFUa1pXbDhVN2hiX3l4ZzlNY3ZnIiwiUGFiMVpxUUpVQjJQSThnSmRWTGRZU090alRQR2VQT3RYVVpmNGh2c2tkYyJdfSwiaXNzdWluZ19jb3VudHJ5IjoiREUiLCJ2Y3QiOiJodHRwczovL2V4YW1wbGUuYm1pLmJ1bmQuZGUvY3JlZGVudGlhbC9waWQvMS4wIiwiaXNzdWluZ19hdXRob3JpdHkiOiJERSIsIl9zZF9hbGciOiJzaGEtMjU2IiwiaXNzIjoiaHR0cHM6Ly9kZW1vLnBpZC1pc3N1ZXIuYnVuZGVzZHJ1Y2tlcmVpLmRlL2MiLCJjbmYiOnsiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yN"+
			"TYiLCJ4IjoicTFTbWFUTWFhMko1eTV6cmYzcksxeUJDc09TQUpuQ01idTBZZG5BWlRPdyIsInkiOiJOSHo1bXVleDAzU1dOTWxYcUpBTnhwbnI5WDY4a0d1VG5Xdk1nSUVkcTBvIn19LCJleHAiOjE3MzE1MDIyMzMsImlhdCI6MTczMDI5MjYzMywiYWdlX2VxdWFsX29yX292ZXIiOnsiX3NkIjpbIjdMYWNyMWN1RUtYeThobDBwMDdnaUxCOGpNTll4QVhUb2JkVTlXVjZSb3ciLCJLWkc5bGZjWlJKM1NMS0lLQjdQZk9ESXd0UlZ0S3BzbEFrdk9xMldkR2lNIiwiTjcxQXh2eTdkTUpHU2g1azkyZkVZU0FwYnRCOHRqTC1DUW56WkwyaEItcyIsIldxdlV5cnZkV3BzeFhpM1NQY2M4Z01JaXlmSmRTa2I3ejJ0VVQ4VEM4U1EiLCJYQ3lxSG9wM09YajhaNzJUNWs0YjZGMUdRUVBxQXRNeHhWeUxUWWVxVnNRIiwicGE3S2diR2xEMmZ2TnAxSTFLWElINWRRcDJmd0d0OFg1dVFVNGlZSEhtOCJdfX0.LN50nCp9R80N6kaGpV-8_NXhNOPbhMtPdTL2jXgasZdxXCatPOOTa7oZ8Nk_TKpEg9uqlncGv1uzlB9cSpxUbQ~WyJjVnRGQmJGVFVtdjI3bHJVQy1PRUdBIiwiMjEiLHRydWVd~WyJwUjRNWXVsWU0zejhnNXFWbk9ENnNBIiwiZmFtaWx5X25hbWUiLCJNVVNURVJNQU5OIl0~WyJfOE1RMHVWRUt4ZVpSaWxZNkp4QndRIiwiZ2l2ZW5fbmFtZSIsIkVSSUtBIl0~eyJ0eXAiOiJrYitqd3QiLCJhbGciOiJFUzI1NiJ9.eyJpYXQiOjE3MzAzMDg1OTcsIm5vbmNlI"+
			"joiNjE1NTI0ODk4NjAyNDk5NzcyNjU5NTM0IiwiYXVkIjoiZnVua2UuYW5pbW8uaWQiLCJzZF9oYXNoIjoibE90OVhpbC1UOGlYLS1NQ2FDaks5Q0lub1ctTm1KSkdEMGF1ZE5CWEFwVSJ9.NjEWxQyKAHfE085izLWRbVFAfMvPqPXe5rXoDWXcs8b7u7Eh3l4gO0MyLaLtD5RfYjDTbK3vQd-WORz1IAkHYg";

		params.addProperty("vp_token", vpToken);
		String ps;

		//ps = "{\"id\":\"7b9fc8f3-a853-4dd7-8152-1c4a4dbf34cf\",\"definition_id\":\"747cbc80-e222-4879-a2dc-df8f1c6983dc\",\"descriptor_map\":[{\"id\":\"sd-jwt-pid\",\"format\":\"vc+sd-jwt\",\"path\":\"$\"}]}";

		String id = env.getString("authorization_request_object", "claims.presentation_definition.id");
		JsonElement descArray = env.getElementFromObject("authorization_request_object", "claims.presentation_definition.input_descriptors");
		JsonObject foo = descArray.getAsJsonArray().get(0).getAsJsonObject();


		String descriptKey = OIDFJSON.getString(foo.get("id"));

		ps = "{\n" +
			"      \"id\": \"vFB9qd4_0P-7fWRBBKHZx\",\n" +
			"      \"definition_id\": \""+id+"\",\n" +
			"      \"descriptor_map\": [\n" +
			"        {\n" +
			"          \"id\": \""+descriptKey+"\",\n" +
			"          \"format\": \"vc+sd-jwt\",\n" +
			"          \"path\": \"$\"\n" +
			"        }\n" +
			"      ]\n" +
			"    }";
		JsonElement jsonRoot = JsonParser.parseString(ps);
		params.add("presentation_submission", jsonRoot);

		logSuccess("Added vp_token to authorization endpoint response params", args(CreateAuthorizationEndpointResponseParams.ENV_KEY, params));

		return env;

	}

}
