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

import org.bson.BsonBoolean;
import org.bson.BsonDouble;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

import com.google.gson.JsonPrimitive;

/**
 * Converts JsonPrimitives from GSON into BSON Values for Mongo.
 * 
 * @author jricher
 *
 */
@Component
@WritingConverter
public class GsonPrimitiveToBsonValueConverter implements Converter<JsonPrimitive, BsonValue> {

	@Override
	public BsonValue convert(JsonPrimitive source) {
		if (source == null) {
			return null;
		} else if (source.isBoolean()) {
			return BsonBoolean.valueOf(source.getAsBoolean());
		} else if (source.isNumber()) {
			// TODO: should we have this optimize for integers, too?
			return new BsonDouble(source.getAsDouble());
		} else if (source.isString()) {
			return new BsonString(source.getAsString());
		} else {
			throw new IllegalArgumentException("Source JsonPrimitive not a known category: " + source);
		}

	}

}
