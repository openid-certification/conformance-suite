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

package io.fintechlabs.testframework.condition.util;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.gson.JsonObject;

/**
 * Extracts the host and port of a given URL string (including the default port if none is
 * specified) into a JSON object, for use with TLS testing.
 * 
 * @author jricher
 *
 */
public class TLSTestValueExtractor {

	public static JsonObject extractTlsFromUrl(String urlString) throws MalformedURLException {
		URL url = new URL(urlString);
		JsonObject tls = new JsonObject();
		tls.addProperty("testHost", url.getHost());
		tls.addProperty("testPort", url.getPort() > 0 ? url.getPort() : url.getDefaultPort());
		
		return tls;
	}

}
