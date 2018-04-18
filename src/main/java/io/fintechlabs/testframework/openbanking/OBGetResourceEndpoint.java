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

package io.fintechlabs.testframework.openbanking;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.testmodule.Environment;

public final class OBGetResourceEndpoint {

	public static enum Endpoint {
		ACCOUNT_REQUESTS,
		ACCOUNTS_RESOURCE
	}

	
	/**
	 * Private constructor.
	 */
	private OBGetResourceEndpoint() {
		
	}
	
	/**
	 * Takes the three possible environment variables and return the one that the user really wants
	 * based on the requested prioritisation. 
	 * @param urlToPrioritise -- Endpoint(Base), AccountRequest or AccountsResource
	 * @return
	 */
	public static String getBaseResourceURL(Environment env, Endpoint urlToPrioritise) {
		String resourceEndpoint = env.getString("resource", "resourceUrl");
		String resourceAccountRequest = env.getString("resource","resourceUrlAccountRequests");
		String resourceAccountsResource = env.getString("resource","resourceUrlAccountRequests");
		
		switch (urlToPrioritise) {
			case ACCOUNT_REQUESTS:
				if (!Strings.isNullOrEmpty(resourceAccountRequest)) {
					return resourceAccountRequest;
				} else {
					return resourceEndpoint;
				}
			case ACCOUNTS_RESOURCE:
				if(!Strings.isNullOrEmpty(resourceAccountsResource)) {
					return resourceAccountsResource; 
				} else {
					return resourceEndpoint;
				}
			default:
				return resourceEndpoint;
		}
	}
	
}
