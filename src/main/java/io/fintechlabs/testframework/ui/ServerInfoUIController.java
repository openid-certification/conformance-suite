package io.fintechlabs.testframework.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ServerInfoUIController {

	@Autowired
	private ServerInfoTemplate serverInfoTemplate;
	/**
	 * Provide a JSON result that represents the currently Server Info.
	 *
	 * @return the info of server
	 */
	@RequestMapping(value = "/api/server", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getServerInfo() {
		return new ResponseEntity<>(serverInfoTemplate.getServerInfo(), HttpStatus.OK);
	}
}
