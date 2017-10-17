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

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ParseIdToken extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public ParseIdToken(String testId, EventLog log, boolean optional) {
		super(testId, log, optional, "FAPI-1-5.2.2-24");
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {

		if (!env.containsObj("token_endpoint_response")) {
			return error("Couldn't find a Token Endpoint Response");
		}
		
		JsonElement idTokenElement = env.findElement("token_endpoint_response", "id_token");
		if (idTokenElement == null || !idTokenElement.isJsonPrimitive()) {
			return error("Couldn't find an ID Token in Token Endpoint Response");
		}

		String idTokenString = env.getString("token_endpoint_response", "id_token");
		
		try {
			JWT idToken = JWTParser.parse(idTokenString);
			
			// Note: we need to round-trip this to get to GSON objects because the JWT library uses a different parser
			JsonObject header = new JsonParser().parse(idToken.getHeader().toJSONObject().toJSONString()).getAsJsonObject(); 
			JsonObject claims = new JsonParser().parse(idToken.getJWTClaimsSet().toJSONObject().toJSONString()).getAsJsonObject();
			
			JsonObject o = new JsonObject();
			o.addProperty("value", idTokenString); // save the original string to allow for crypto operations
			o.add("header", header);
			o.add("claims", claims);
			
			// save the parsed ID token
			env.put("id_token", o);
			
			log("Found the ID Token", o);
			
			logSuccess();
			
			return env;
			
		} catch (ParseException e) {
			return error("Couldn't parse JWT", e);
		}
		
	}

}
