/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.condition;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class GenerateIdTokenClaims extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public GenerateIdTokenClaims(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = {"user_info", "client", "authorization_endpoint_request"}, strings = "issuer")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		String subject = env.getString("user_info", "sub");
		String issuer = env.getString("issuer");
		String clientId = env.getString("client", "client_id");
		String nonce = env.getString("authorization_endpoint_request", "nonce");
		
		if (Strings.isNullOrEmpty(subject)) {
			return error("Couldn't find subject");
		}
		
		if (Strings.isNullOrEmpty(issuer)) {
			return error("Couldn't find issuer");
		}

		if (Strings.isNullOrEmpty(clientId)) {
			return error("Couldn't find client ID");
		}
		
		JsonObject claims = new JsonObject();
		claims.addProperty("iss", issuer);
		claims.addProperty("sub", subject);
		claims.addProperty("aud", clientId);
		
		if (!Strings.isNullOrEmpty(nonce)) {
			claims.addProperty("nonce", nonce);
		}
		
		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(5 * 60);
		
		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());

		env.put("id_token_claims", claims);

		logSuccess("Created ID Token Claims", claims);
		
		return env;
		
	}

}
