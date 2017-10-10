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

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author jricher
 *
 */
@Controller
public class ImageAPI {


	@PostMapping(path = "/log/{id}/{entry}")
	public ResponseEntity<Object> uploadImageToLogEntry(@RequestParam("file") MultipartFile file) throws IOException {
		
		byte[] bytes = file.getBytes();
		String contentType = file.getContentType();
		
		String encoded = Base64.encodeBase64String(bytes);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	
}
