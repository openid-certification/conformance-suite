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

import java.util.Map;

import org.bson.conversions.Bson;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * @author jricher
 *
 */
@Component
@ReadingConverter
public class BsonCollapsingConverter implements Converter<DBObject, DBObject> {
	
	@Override
	public DBObject convert(DBObject source) {
		if (source == null) {
			return null;
		} else {
			return (DBObject) convertStructureToField(source);
		}
	}

	/**
	 * @param source
	 * @return
	 */
	private Object convertStructureToField(Object source) {
		if (source instanceof DBObject) {
			DBObject dbo = (DBObject) source;
			
			// need to look through all the fields and convert any weird ones

			DBObject converted = new BasicDBObject();
			for (String key : dbo.keySet()) {
				if (key.startsWith("__wrapped_key_element_")) {
					DBObject wrapped = (DBObject) dbo.get(key);
					converted.put((String) wrapped.get("key"), convertStructureToField(wrapped.get("value")));
				} else {
					converted.put(key, convertStructureToField(dbo.get(key)));
				}
			}
			return converted;
		} else {
			return source;
		}
	}

}
