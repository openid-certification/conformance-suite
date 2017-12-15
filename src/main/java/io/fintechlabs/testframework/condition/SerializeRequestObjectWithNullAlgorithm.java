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

import java.text.ParseException;

import com.google.gson.JsonObject;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class SerializeRequestObjectWithNullAlgorithm extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public SerializeRequestObjectWithNullAlgorithm(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "request_object_claims")
	@PostEnvironment(strings = "request_object")
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.get("request_object_claims");

		if (requestObjectClaims == null) {
			return error("Couldn't find request object claims");
		}

		// FIXME: this processing should be handled in a separate condition
		if (!requestObjectClaims.has("iss")) {
			String clientId = env.getString("client_id");
			if (clientId != null) {
				requestObjectClaims.addProperty("iss", clientId);
			} else {
				// Only a "should" requirement
				log("Request object contains no issuer and client ID not found");
			}
		}

		// FIXME: this processing should be handled in a separate condition
		if (!requestObjectClaims.has("aud")) {
			String serverIssuerUrl = env.getString("server", "issuer");
			if (serverIssuerUrl != null) {
				requestObjectClaims.addProperty("aud", serverIssuerUrl);
			} else {
				// Only a "should" requirement
				log("Request object contains no audience and server issuer URL not found");
			}
		}

		try {
			JWTClaimsSet claimSet = JWTClaimsSet.parse(requestObjectClaims.toString());

			PlainHeader header = new PlainHeader();

			PlainJWT requestObject = new PlainJWT(header, claimSet);

			env.putString("request_object", requestObject.serialize());

			logSuccess("Serialized the request object", args("request_object", requestObject.serialize()));

			return env;
		} catch (ParseException e) {
			return error(e);
		}

	}

}
