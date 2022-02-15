package net.openid.conformance.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import static net.openid.conformance.util.JsonObjectBuilder.addField;
import static net.openid.conformance.util.JsonObjectBuilder.addFields;
import static org.junit.Assert.assertEquals;

public class JsonObjectBuilderTests {

	@Test
	public void buildObjectFromScratch() throws IOException {

		String expectedJson = IOUtils.resourceToString("simple_json_object.json", Charset.defaultCharset(), getClass().getClassLoader());
		JsonElement expected = new JsonParser().parse(expectedJson);

		JsonObject obj = new JsonObjectBuilder()
				.addField( "data.loggedUser.document.identification", "76109277673")
				.addField( "data.loggedUser.document.rel", "CPF")
				.addField( "data.creditor.personType", "PESSOA_NATURAL")
				.addField( "data.creditor.cpfCnpj", "48847377765")
				.addField( "data.creditor.name", "Marco Antonio de Brito")
			.build();

		assertEquals(expected, obj);

	}

	@Test
	public void buildObject() throws IOException {

		String expectedJson = IOUtils.resourceToString("simple_json_object.json", Charset.defaultCharset(), getClass().getClassLoader());
		JsonElement expected = new JsonParser().parse(expectedJson);

		JsonObject obj = new JsonObject();
		addField(obj, "data.loggedUser.document.identification", "76109277673");
		addField(obj, "data.loggedUser.document.rel", "CPF");
		addField(obj, "data.creditor.personType", "PESSOA_NATURAL");
		addField(obj, "data.creditor.cpfCnpj", "48847377765");
		addField(obj, "data.creditor.name", "Marco Antonio de Brito");

		assertEquals(expected, obj);

	}

	@Test
	public void buildWithMultiplePropertiesPerObject() throws IOException {

		String expectedJson = IOUtils.resourceToString("simple_json_object.json", Charset.defaultCharset(), getClass().getClassLoader());
		JsonElement expected = new JsonParser().parse(expectedJson);

		JsonObject obj = new JsonObject();
		addFields(obj, "data.loggedUser.document", Map.of("identification", "76109277673",
															"rel", "CPF"));
		addFields(obj, "data.creditor", Map.of("personType", "PESSOA_NATURAL",
																"cpfCnpj", "48847377765",
																"name", "Marco Antonio de Brito"));

		assertEquals(expected, obj);

	}

}
