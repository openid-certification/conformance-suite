package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWEUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class EncryptIdToken_UnitTest
{
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EncryptIdToken cond;

	private String clientWithRSAOAEP = "{\n" +
	"  \"id_token_signed_response_alg\": \"HS256\",\n" +
	"  \"token_endpoint_auth_method\": \"client_secret_basic\",\n" +
	"  \"response_types\": [\n" +
	"    \"code\"\n" +
	"  ],\n" +
	"  \"request_object_signing_alg\": \"RS256\",\n" +
	"  \"grant_types\": [\n" +
	"    \"authorization_code\"\n" +
	"  ],\n" +
	"  \"redirect_uris\": [\n" +
	"    \"https://openid-client.local/cb\"\n" +
	"  ],\n" +
	"  \"jwks\": {\n" +
	"    \"keys\": [\n" +
	"      {\n" +
	"        \"crv\": \"P-256\",\n" +
	"        \"x\": \"ElxWQS3y3xZro6sjdoSNdEZMete9m0H2_9hQ_q-z1a8\",\n" +
	"        \"y\": \"4H4O5G2rREmTgYqj9J6lpt-_d1csGkMofIfQqB6oiCo\",\n" +
	"        \"kty\": \"EC\",\n" +
	"        \"kid\": \"1EO-CpOJdm5GoQZiN0TjZZpbxftjuRYEbbtwuVaHokc\"\n" +
	"      },\n" +
	"      {\n" +
	"        \"e\": \"AQAB\",\n" +
	"        \"n\": \"ru6W-aVvfWEcFJ30w2TcuIgxzll4OPfh8chjNI0HLSBkrDSDlQQkMiOwddWj4kLJaiTRHMdSnxraKdTr76F-_Wqz8IUYQLC2REb9EZ-qafM2VUjcXRPIZj2WVnlJ-2d3Pz2OT51FzJFtikhaEYOhrEq4IzdhVeQpZly1Ic76PEQVYWcdHXb0vkRehpAyBxaqDIrHl66pRNZOSL1LP1V2iaqGc9GwTZ7IVfyA0mIGBEp7cknWx1nNEDjj_YZSCZX9yN7JvUtoBPt6GSHCZrACO5ZnOMYGf5Nv3qZkSH7eBWYizE-4x9vMZlyyiQvueZa1oh4fnwht1SvgwUskAWEqUw\",\n" +
	"        \"use\": \"enc\",\n" +
	"        \"kty\": \"RSA\",\n" +
	"        \"kid\": \"7688SbG4DDpqhlzcpG74dx9d__F6jvwfVbFUrT5lKEs\"\n" +
	"      }\n" +
	"    ]\n" +
	"  },\n" +
	"  \"id_token_encrypted_response_alg\":\"RSA-OAEP\",\n" +
	"  \"client_id\": \"client_KZWXyVhCZrHyECa69468*#+%)\",\n" +
	"  \"client_secret\":\"secret_cTqoutCPrKKvagcDJAqwEBKURqLoFoeRMNruihsfrJzMYYzaui5619007344{{=:\"" +
	"}";

	private String clientWithA128KW = "{\n" +
		"  \"id_token_signed_response_alg\": \"HS256\",\n" +
		"  \"token_endpoint_auth_method\": \"client_secret_basic\",\n" +
		"  \"response_types\": [\n" +
		"    \"code\"\n" +
		"  ],\n" +
		"  \"request_object_signing_alg\": \"RS256\",\n" +
		"  \"grant_types\": [\n" +
		"    \"authorization_code\"\n" +
		"  ],\n" +
		"  \"redirect_uris\": [\n" +
		"    \"https://openid-client.local/cb\"\n" +
		"  ],\n" +
		"  \"jwks\": {\n" +
		"    \"keys\": [\n" +
		"      {\n" +
		"        \"crv\": \"P-256\",\n" +
		"        \"x\": \"ElxWQS3y3xZro6sjdoSNdEZMete9m0H2_9hQ_q-z1a8\",\n" +
		"        \"y\": \"4H4O5G2rREmTgYqj9J6lpt-_d1csGkMofIfQqB6oiCo\",\n" +
		"        \"kty\": \"EC\",\n" +
		"        \"kid\": \"1EO-CpOJdm5GoQZiN0TjZZpbxftjuRYEbbtwuVaHokc\"\n" +
		"      },\n" +
		"      {\n" +
		"        \"e\": \"AQAB\",\n" +
		"        \"n\": \"ru6W-aVvfWEcFJ30w2TcuIgxzll4OPfh8chjNI0HLSBkrDSDlQQkMiOwddWj4kLJaiTRHMdSnxraKdTr76F-_Wqz8IUYQLC2REb9EZ-qafM2VUjcXRPIZj2WVnlJ-2d3Pz2OT51FzJFtikhaEYOhrEq4IzdhVeQpZly1Ic76PEQVYWcdHXb0vkRehpAyBxaqDIrHl66pRNZOSL1LP1V2iaqGc9GwTZ7IVfyA0mIGBEp7cknWx1nNEDjj_YZSCZX9yN7JvUtoBPt6GSHCZrACO5ZnOMYGf5Nv3qZkSH7eBWYizE-4x9vMZlyyiQvueZa1oh4fnwht1SvgwUskAWEqUw\",\n" +
		"        \"use\": \"enc\",\n" +
		"        \"kty\": \"RSA\",\n" +
		"        \"kid\": \"7688SbG4DDpqhlzcpG74dx9d__F6jvwfVbFUrT5lKEs\"\n" +
		"      }\n" +
		"    ]\n" +
		"  },\n" +
		"  \"id_token_encrypted_response_alg\":\"A128KW\",\n" +
		"  \"client_id\": \"client_KZWXyVhCZrHyECa69468*#+%)\",\n" +
		"  \"client_secret\":\"secret_cTqoutCPrKKvagcDJAqwEBKURqLoFoeRMNruihsfrJzMYYzaui5619007344{{=:\"" +
		"}";


	private String clientWithDIR = "{\n" +
		"  \"id_token_signed_response_alg\": \"HS256\",\n" +
		"  \"token_endpoint_auth_method\": \"client_secret_basic\",\n" +
		"  \"response_types\": [\n" +
		"    \"code\"\n" +
		"  ],\n" +
		"  \"request_object_signing_alg\": \"RS256\",\n" +
		"  \"grant_types\": [\n" +
		"    \"authorization_code\"\n" +
		"  ],\n" +
		"  \"redirect_uris\": [\n" +
		"    \"https://openid-client.local/cb\"\n" +
		"  ],\n" +
		"  \"jwks\": {\n" +
		"    \"keys\": [\n" +
		"      {\n" +
		"        \"crv\": \"P-256\",\n" +
		"        \"x\": \"ElxWQS3y3xZro6sjdoSNdEZMete9m0H2_9hQ_q-z1a8\",\n" +
		"        \"y\": \"4H4O5G2rREmTgYqj9J6lpt-_d1csGkMofIfQqB6oiCo\",\n" +
		"        \"kty\": \"EC\",\n" +
		"        \"kid\": \"1EO-CpOJdm5GoQZiN0TjZZpbxftjuRYEbbtwuVaHokc\"\n" +
		"      },\n" +
		"      {\n" +
		"        \"e\": \"AQAB\",\n" +
		"        \"n\": \"ru6W-aVvfWEcFJ30w2TcuIgxzll4OPfh8chjNI0HLSBkrDSDlQQkMiOwddWj4kLJaiTRHMdSnxraKdTr76F-_Wqz8IUYQLC2REb9EZ-qafM2VUjcXRPIZj2WVnlJ-2d3Pz2OT51FzJFtikhaEYOhrEq4IzdhVeQpZly1Ic76PEQVYWcdHXb0vkRehpAyBxaqDIrHl66pRNZOSL1LP1V2iaqGc9GwTZ7IVfyA0mIGBEp7cknWx1nNEDjj_YZSCZX9yN7JvUtoBPt6GSHCZrACO5ZnOMYGf5Nv3qZkSH7eBWYizE-4x9vMZlyyiQvueZa1oh4fnwht1SvgwUskAWEqUw\",\n" +
		"        \"use\": \"enc\",\n" +
		"        \"kty\": \"RSA\",\n" +
		"        \"kid\": \"7688SbG4DDpqhlzcpG74dx9d__F6jvwfVbFUrT5lKEs\"\n" +
		"      }\n" +
		"    ]\n" +
		"  },\n" +
		"  \"id_token_encrypted_response_alg\":\"dir\",\n" +
		"  \"client_id\": \"client_KZWXyVhCZrHyECa69468*#+%)\",\n" +
		"  \"client_secret\":\"secret_cTqoutCPrKKvagcDJAqwEBKURqLoFoeRMNruihsfrJzMYYzaui5619007344{{=:\"" +
		"}";

	@Before
	public void setUp() throws Exception {

		cond = new EncryptIdToken();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_success() {

		JsonObject client = (JsonObject) new JsonParser().parse(clientWithRSAOAEP);
		env.putObject("client", client);
		env.putString("id_token", "eyJraWQiOiIwNzk2Zjg2ZC01MmNkLTQwMTAtYWRjNS1hNjk1N2JhMTc0OGQiLCJhbGciOiJIUzUxMiJ9.eyJhdF9oYXNoIjoiOE5qdWludmR1VXpkZFowYmtHVW9OQ1UzWkhvNGgwWGFqd0ZyeUNGOFZJayIsInN1YiI6InVzZXItc3ViamVjdC0xMjM0NTMxIiwiYXVkIjoiY2xpZW50X0taV1h5VmhDWnJIeUVDYTY5NDY4KiMrJSkiLCJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0LmVtb2JpeC5jby51azo4NDQzXC90ZXN0XC9hXC9vcGVuaWQtY2xpZW50XC8iLCJleHAiOjE1NzkzMzA5NDEsImlhdCI6MTU3OTMzMDY0MX0.q9_V3Hxz74D2xvFgU0SLDmt7nz6UGzTwK3icoNdQJkARitdwKqG9PfoUdlZ4wQuOjiwFTcQQMt_u-ClMOoBPkA");
		cond.execute(env);
		String encrypted = env.getString("id_token");
		assertNotNull(encrypted);
	}

	@Test
	public void testEvaluate_A128KW() {

		JsonObject client = (JsonObject) new JsonParser().parse(clientWithA128KW);
		env.putObject("client", client);
		String idToken = "eyJraWQiOiIwNzk2Zjg2ZC01MmNkLTQwMTAtYWRjNS1hNjk1N2JhMTc0OGQiLCJhbGciOiJIUzUxMiJ9.eyJhdF9oYXNoIjoiOE5qdWludmR1VXpkZFowYmtHVW9OQ1UzWkhvNGgwWGFqd0ZyeUNGOFZJayIsInN1YiI6InVzZXItc3ViamVjdC0xMjM0NTMxIiwiYXVkIjoiY2xpZW50X0taV1h5VmhDWnJIeUVDYTY5NDY4KiMrJSkiLCJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0LmVtb2JpeC5jby51azo4NDQzXC90ZXN0XC9hXC9vcGVuaWQtY2xpZW50XC8iLCJleHAiOjE1NzkzMzA5NDEsImlhdCI6MTU3OTMzMDY0MX0.q9_V3Hxz74D2xvFgU0SLDmt7nz6UGzTwK3icoNdQJkARitdwKqG9PfoUdlZ4wQuOjiwFTcQQMt_u-ClMOoBPkA";
		env.putString("id_token", idToken);
		cond.execute(env);
		String encrypted = env.getString("id_token");
		String clientSecret = env.getString("client", "client_secret");
		try
		{
			JWK key = JWEUtil.createSymmetricJWKForAlgAndSecret(clientSecret, JWEAlgorithm.A128KW, EncryptionMethod.A128CBC_HS256, "1");
			JWEDecrypter decrypter = JWEUtil.createDecrypter(key);

			JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.A128KW, EncryptionMethod.A128CBC_HS256)
				.contentType("JWT") // required to indicate nested JWT
				.build();
			JWEObject parsedJWEObject = JWEObject.parse(encrypted);
			parsedJWEObject.decrypt(decrypter);
			String decrpytedIdToken = parsedJWEObject.getPayload().toString();
			assertEquals(idToken, decrpytedIdToken);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testEvaluate_DIR() {

		JsonObject client = (JsonObject) new JsonParser().parse(clientWithDIR);
		env.putObject("client", client);
		String idToken = "eyJraWQiOiIwNzk2Zjg2ZC01MmNkLTQwMTAtYWRjNS1hNjk1N2JhMTc0OGQiLCJhbGciOiJIUzUxMiJ9.eyJhdF9oYXNoIjoiOE5qdWludmR1VXpkZFowYmtHVW9OQ1UzWkhvNGgwWGFqd0ZyeUNGOFZJayIsInN1YiI6InVzZXItc3ViamVjdC0xMjM0NTMxIiwiYXVkIjoiY2xpZW50X0taV1h5VmhDWnJIeUVDYTY5NDY4KiMrJSkiLCJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0LmVtb2JpeC5jby51azo4NDQzXC90ZXN0XC9hXC9vcGVuaWQtY2xpZW50XC8iLCJleHAiOjE1NzkzMzA5NDEsImlhdCI6MTU3OTMzMDY0MX0.q9_V3Hxz74D2xvFgU0SLDmt7nz6UGzTwK3icoNdQJkARitdwKqG9PfoUdlZ4wQuOjiwFTcQQMt_u-ClMOoBPkA";
		env.putString("id_token", idToken);
		cond.execute(env);
		String encrypted = env.getString("id_token");
		String clientSecret = env.getString("client", "client_secret");
		try
		{
			JWK key = JWEUtil.createSymmetricJWKForAlgAndSecret(clientSecret, JWEAlgorithm.DIR, EncryptionMethod.A128CBC_HS256, "1");
			JWEDecrypter decrypter = JWEUtil.createDecrypter(key);

			JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A128CBC_HS256)
				.contentType("JWT") // required to indicate nested JWT
				.build();
			JWEObject parsedJWEObject = JWEObject.parse(encrypted);
			parsedJWEObject.decrypt(decrypter);
			String decrpytedIdToken = parsedJWEObject.getPayload().toString();
			assertEquals(idToken, decrpytedIdToken);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}


	@Test
	public void testEvaluate_DIR2() {

		JsonObject client = (JsonObject) new JsonParser().parse("{\n" +
			"  \"id_token_encrypted_response_alg\": \"dir\",\n" +
			"  \"id_token_encrypted_response_enc\": \"A128CBC-HS256\",\n" +
			"  \"token_endpoint_auth_method\": \"client_secret_basic\",\n" +
			"  \"response_types\": [\n" +
			"    \"code\"\n" +
			"  ],\n" +
			"  \"request_object_signing_alg\": \"RS256\",\n" +
			"  \"grant_types\": [\n" +
			"    \"authorization_code\"\n" +
			"  ],\n" +
			"  \"redirect_uris\": [\n" +
			"    \"https://openid-client.local/cb\"\n" +
			"  ],\n" +
			"  \"jwks\": {\n" +
			"    \"keys\": [\n" +
			"      {\n" +
			"        \"crv\": \"P-256\",\n" +
			"        \"x\": \"Ihoxy32nDZqRYnNfx6ieE6OGTRD1JmpjgGgJ0jCwx0U\",\n" +
			"        \"y\": \"jUQuWCxy_b6mbSAkxRwMMklik_7Lqm5nS9cCVJbee8k\",\n" +
			"        \"kty\": \"EC\",\n" +
			"        \"kid\": \"sE8gZLVj_AfoHNV2SLF5erDusGh5fuDZYRzKEDxTLDg\"\n" +
			"      },\n" +
			"      {\n" +
			"        \"e\": \"AQAB\",\n" +
			"        \"n\": \"z5GV79TaxGvJBPHFm57sZIrMlGo65XgxcdTkBw-7OjTig2PBJ5SRyHmHM3qmRH7S2ya0nzRun0A2zzT5rYaVLGnjzwou9FH7EDGWDhjYEttmSXjNhYX_vCp2Ly2DQZUh-vGyxNKvZRtxXDyES55JKQnOP1Nc9Ki_p-gOAKpEvacA0clgymu6hpanukDrPCvPBiy2nrSSAGsCbikEsC6On1jz9FQY0QSf02hXR20KGNCCYsmbWoM-ezJdkBj2e-BZ4DrlFRAqnfKYAvDhUYNmTHMNbkppxqbau5NZdifDM1GNGp5Lyg9_ZdtUkrI3o5kpVcIx1mtAZOSgnFZdCEtJEw\",\n" +
			"        \"kty\": \"RSA\",\n" +
			"        \"kid\": \"GW4l_SmkC8d31OjksUAXIvH16UMvLp7EQgGuF1RY1Sw\"\n" +
			"      }\n" +
			"    ]\n" +
			"  },\n" +
			"  \"client_id\": \"client_TFFXiHvUMdnlWUl17918!&@`,\",\n" +
			"\"client_secret\":\"secret_njaRuFPAsuJvPVAjRGlbeOuamFFoQAWyFMEGagxDtuSZMlnYBJ6923523325?-~\"\n" +
			"}");
		env.putObject("client", client);
		String idToken = "eyJraWQiOiI2MTMzZjU1Yy02MDFhLTQxZjktOTM5OS1kZTRlMmE1YTU5MzkiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiQXlqVW5sOFRwVkZPT0d5QkZSdmVtZyIsInN1YiI6InVzZXItc3ViamVjdC0xMjM0NTMxIiwiYXVkIjoiY2xpZW50X1RGRlhpSHZVTWRubFdVbDE3OTE4ISZAYCwiLCJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0LmVtb2JpeC5jby51azo4NDQzXC90ZXN0XC9hXC9vcGVuaWQtY2xpZW50XC8iLCJleHAiOjE1Nzk0NjUxMDksImlhdCI6MTU3OTQ2NDgwOX0.KGNxzr4F7V7gU7BD2M00IoZfrAYPCRVz5yaa11paEc_AgDgJYs9QrCV3vi_4pODIiugG0nZIvXoiqwzfuu5h-T1F5tIIRbox5-FBo6x7xal7Cdzmj6stuKKbrPbe0yB469msujOCW4czYXPxcO2nvNl4thnwfvvq8gbMxQdGR19otG3j-DxzkRgBIZfvfVvK5nsuP6LHJkp26-uRX8nnRxCUXv2uAJAjmKHiXS_TyPfiWoId2bI_-weNDKl4q470PeP0A7HRSvZhNaSdELWdjX6O4nmIG36dlkdtAsEfKGG4wS3QmEeTTyqtr6yDMuxSsjwj-PhWZ23NMkRYtNLhfg";
		env.putString("id_token", idToken);
		cond.execute(env);
		String encrypted = env.getString("id_token");
		String clientSecret = env.getString("client", "client_secret");
		try
		{
			JWK key = JWEUtil.createSymmetricJWKForAlgAndSecret(clientSecret, JWEAlgorithm.DIR, EncryptionMethod.A128CBC_HS256, "1");
			JWEDecrypter decrypter = JWEUtil.createDecrypter(key);

			JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A128CBC_HS256)
				.contentType("JWT") // required to indicate nested JWT
				.build();
			JWEObject parsedJWEObject = JWEObject.parse(encrypted);
			parsedJWEObject.decrypt(decrypter);
			String decrpytedIdToken = parsedJWEObject.getPayload().toString();
			assertEquals(idToken, decrpytedIdToken);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}


	@Test
	public void testEvaluate_A192KW() {

		JsonObject client = (JsonObject) new JsonParser().parse("{\n" +
			"  \"id_token_encrypted_response_alg\": \"A192KW\",\n" +
			"  \"id_token_encrypted_response_enc\": \"A128CBC-HS256\",\n" +
			"  \"token_endpoint_auth_method\": \"client_secret_basic\",\n" +
			"  \"response_types\": [\n" +
			"    \"code\"\n" +
			"  ],\n" +
			"  \"request_object_signing_alg\": \"RS256\",\n" +
			"  \"grant_types\": [\n" +
			"    \"authorization_code\"\n" +
			"  ],\n" +
			"  \"redirect_uris\": [\n" +
			"    \"https://openid-client.local/cb\"\n" +
			"  ],\n" +
			"  \"jwks\": {\n" +
			"    \"keys\": [\n" +
			"      {\n" +
			"        \"crv\": \"P-256\",\n" +
			"        \"x\": \"Ihoxy32nDZqRYnNfx6ieE6OGTRD1JmpjgGgJ0jCwx0U\",\n" +
			"        \"y\": \"jUQuWCxy_b6mbSAkxRwMMklik_7Lqm5nS9cCVJbee8k\",\n" +
			"        \"kty\": \"EC\",\n" +
			"        \"kid\": \"sE8gZLVj_AfoHNV2SLF5erDusGh5fuDZYRzKEDxTLDg\"\n" +
			"      },\n" +
			"      {\n" +
			"        \"e\": \"AQAB\",\n" +
			"        \"n\": \"z5GV79TaxGvJBPHFm57sZIrMlGo65XgxcdTkBw-7OjTig2PBJ5SRyHmHM3qmRH7S2ya0nzRun0A2zzT5rYaVLGnjzwou9FH7EDGWDhjYEttmSXjNhYX_vCp2Ly2DQZUh-vGyxNKvZRtxXDyES55JKQnOP1Nc9Ki_p-gOAKpEvacA0clgymu6hpanukDrPCvPBiy2nrSSAGsCbikEsC6On1jz9FQY0QSf02hXR20KGNCCYsmbWoM-ezJdkBj2e-BZ4DrlFRAqnfKYAvDhUYNmTHMNbkppxqbau5NZdifDM1GNGp5Lyg9_ZdtUkrI3o5kpVcIx1mtAZOSgnFZdCEtJEw\",\n" +
			"        \"kty\": \"RSA\",\n" +
			"        \"kid\": \"GW4l_SmkC8d31OjksUAXIvH16UMvLp7EQgGuF1RY1Sw\"\n" +
			"      }\n" +
			"    ]\n" +
			"  },\n" +
			"  \"client_id\": \"client_TFFXiHvUMdnlWUl17918!&@`,\",\n" +
			"\"client_secret\":\"secret_njaRuFPAsuJvPVAjRGlbeOuamFFoQAWyFMEGagxDtuSZMlnYBJ6923523325?-~\"\n" +
			"}");
		env.putObject("client", client);
		String idToken = "eyJraWQiOiI2MTMzZjU1Yy02MDFhLTQxZjktOTM5OS1kZTRlMmE1YTU5MzkiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiQXlqVW5sOFRwVkZPT0d5QkZSdmVtZyIsInN1YiI6InVzZXItc3ViamVjdC0xMjM0NTMxIiwiYXVkIjoiY2xpZW50X1RGRlhpSHZVTWRubFdVbDE3OTE4ISZAYCwiLCJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0LmVtb2JpeC5jby51azo4NDQzXC90ZXN0XC9hXC9vcGVuaWQtY2xpZW50XC8iLCJleHAiOjE1Nzk0NjUxMDksImlhdCI6MTU3OTQ2NDgwOX0.KGNxzr4F7V7gU7BD2M00IoZfrAYPCRVz5yaa11paEc_AgDgJYs9QrCV3vi_4pODIiugG0nZIvXoiqwzfuu5h-T1F5tIIRbox5-FBo6x7xal7Cdzmj6stuKKbrPbe0yB469msujOCW4czYXPxcO2nvNl4thnwfvvq8gbMxQdGR19otG3j-DxzkRgBIZfvfVvK5nsuP6LHJkp26-uRX8nnRxCUXv2uAJAjmKHiXS_TyPfiWoId2bI_-weNDKl4q470PeP0A7HRSvZhNaSdELWdjX6O4nmIG36dlkdtAsEfKGG4wS3QmEeTTyqtr6yDMuxSsjwj-PhWZ23NMkRYtNLhfg";
		env.putString("id_token", idToken);
		cond.execute(env);
		String encrypted = env.getString("id_token");
		String clientSecret = env.getString("client", "client_secret");
		try
		{
			JWK key = JWEUtil.createSymmetricJWKForAlgAndSecret(clientSecret, JWEAlgorithm.A192KW, EncryptionMethod.A128CBC_HS256, "1");
			JWEDecrypter decrypter = JWEUtil.createDecrypter(key);

			JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.A192KW, EncryptionMethod.A128CBC_HS256)
				.contentType("JWT") // required to indicate nested JWT
				.build();
			JWEObject parsedJWEObject = JWEObject.parse(encrypted);
			parsedJWEObject.decrypt(decrypter);
			String decrpytedIdToken = parsedJWEObject.getPayload().toString();
			assertEquals(idToken, decrpytedIdToken);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
