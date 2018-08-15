package io.fintechlabs.testframework.logging;

import org.springframework.core.convert.converter.Converter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.mongodb.BasicDBList;
import com.mongodb.util.JSON;

/**
 * @author jricher
 *
 */
public class GsonArrayToBsonArrayConverter implements Converter<JsonArray, BasicDBList> {

	private Gson gson = new GsonBuilder().create();

	/* (non-Javadoc)
	 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
	 */
	@Override
	public BasicDBList convert(JsonArray source) {
		if (source == null) {
			return null;
		} else {
			return (BasicDBList) JSON.parse(gson.toJson(GsonObjectToBsonDocumentConverter.convertFieldsToStructure(source)));

		}
	}

}
