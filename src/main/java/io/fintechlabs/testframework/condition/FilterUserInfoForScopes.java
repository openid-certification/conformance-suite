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

import java.util.List;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * @author jricher
 *
 */
public class FilterUserInfoForScopes extends AbstractCondition {

	private static SetMultimap<String, String> scopesToClaims = HashMultimap.create();
	{
		scopesToClaims.put("openid", "sub");

		scopesToClaims.put("profile", "name");
		scopesToClaims.put("profile", "preferred_username");
		scopesToClaims.put("profile", "given_name");
		scopesToClaims.put("profile", "family_name");
		scopesToClaims.put("profile", "middle_name");
		scopesToClaims.put("profile", "nickname");
		scopesToClaims.put("profile", "profile");
		scopesToClaims.put("profile", "picture");
		scopesToClaims.put("profile", "website");
		scopesToClaims.put("profile", "gender");
		scopesToClaims.put("profile", "zoneinfo");
		scopesToClaims.put("profile", "locale");
		scopesToClaims.put("profile", "updated_at");
		scopesToClaims.put("profile", "birthdate");

		scopesToClaims.put("email", "email");
		scopesToClaims.put("email", "email_verified");

		scopesToClaims.put("phone", "phone_number");
		scopesToClaims.put("phone", "phone_number_verified");

		scopesToClaims.put("address", "address");
	}
	
	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public FilterUserInfoForScopes(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {

		String scope = env.getString("scope");
		JsonObject userInfo = env.get("user_info");
		
		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope));
		
		JsonObject out = new JsonObject();
		
		// look through all the scopes that we have approved
		for (String s: scopes) {
			if (scopesToClaims.containsKey(s)) {
				for (String claim: scopesToClaims.get(s)) {
					if (userInfo.has(claim)) {
						// if we have a claim that fits that scope, copy it over
						out.add(claim, userInfo.get(claim));
					}
				}
			}
		}
		
		env.put("user_info_endpoint_response", out);
		
		logSuccess("User info endpoint output", out);

		return env;
	}

}
