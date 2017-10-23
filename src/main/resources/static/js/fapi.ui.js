var FAPI_UI = {

	visibleFields : ["msg", "src", "time", "result", "requirements", "upload"],
	
	/**
	 * 
	 */
	goHome : function() {
		window.location.replace("logs.html");
	},

	/**
	 * 
	 */
	loadAvailableLogs : function() {
		$.ajax({ 
	        type: 'GET', 
	        url: "log", 
	        data: {}, 
	        success: render
	    });
	},

	/**
	 * 
	 */
	showLogDetail : function(fileId) {
		$.ajax({ 
	        type: 'GET', 
	        url: "log/" + encodeURIComponent(fileId), 
	        data: {}, 
	        success: function (data) { 

				var logs = [];

	            $.each(data, function(i, item) {

	            	var log = {};
	            	
	            	$.each(item, function(key, val) {
	 
	            		if (!key.startsWith("_") // filter out anything starting with an underscore as it's injected by Mongo
	            				&& key.toLowerCase() != 'testid' // we also don't need to display the test ID
	            				) {

	            			log[key] = val;

	            			if (key.toLowerCase() == "time") {
	            				// format it as a timestamp value
	            				log[key] = new Date(val);
	            			}
	            		}
	            	});

					if (Object.keys(log).length) {
						logs.push(log);
					}	
					
	            });

				render(fileId, logs);
	        }
	    });
	}
	
}

