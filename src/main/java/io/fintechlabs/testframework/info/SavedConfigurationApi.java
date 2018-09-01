package io.fintechlabs.testframework.info;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.google.gson.JsonObject;
import com.mongodb.DBObject;

/**
 * @author jricher
 *
 */
@Controller
public class SavedConfigurationApi {

	@Autowired
	private SavedConfigurationService savedConfigurationService;

	@GetMapping(value = "/lastconfig", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getLastConfig() {

		DBObject config = savedConfigurationService.getLastConfigForCurrentUser();

		if (config == null) {
			// always return a json object even if it's empty
			return new ResponseEntity<>(new JsonObject(), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(config, HttpStatus.OK);
		}

	}

}
