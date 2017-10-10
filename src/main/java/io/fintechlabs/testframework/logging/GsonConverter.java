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

package io.fintechlabs.testframework.logging;

import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.mongodb.util.JSON;

/**
 * @author jricher
 *
 */
@Component
@WritingConverter
public class GsonConverter implements Converter<JsonElement, Bson> {
	
	@Override
	public Bson convert(JsonElement source) {
		if (source == null) {
			return null;
		} else {
			return (Bson) JSON.parse(source.toString());
		}
	}

}
