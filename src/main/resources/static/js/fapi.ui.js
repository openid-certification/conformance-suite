var FAPI_UI = {
	
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
	        success: function (data) { 
	        	render(data);
	        }
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

	            	var log = {keys:[]};
	            	
	            	$.each(item, function(key, val) {
	 
	            		if (!key.startsWith("_") // filter out anything starting with an underscore as it's injected by Mongo
	            				&& key.toLowerCase() != 'testid' // we also don't need to display the test ID
	            				) {

	            			log[key] = val;
	            			log.keys.push(key);

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

