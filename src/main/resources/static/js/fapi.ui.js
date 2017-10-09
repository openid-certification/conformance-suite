var FAPI_UI = {

	goHome : function() {
		window.location.replace("logs.html");
	},

	/**
	 * 
	 */
	loadAvailableLogs : function(url) {
		$.ajax({ 
	        type: 'GET', 
	        url: "log", 
	        data: {}, 
	        success: function (data) { 
	            FAPI_UI.renderTableOfContents(data);
	        }
	    });
	},

	/**
	 * 
	 */
	renderTableOfContents : function(fileIds) {

		for (var i=0;i<fileIds.length;i++) {
			var item = fileIds[i];
			$("#logs .content").append("<div class='link'><a href='log-detail.html?log="+item+"' class='item'>"+item+"</a></div>");
		}
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

	            $.each(data, function(i, item) {

	            	$("#logDetail .content").append("<div class='item'>");

	            	$.each(item, function(key, val) {
	 
	            		// filter out anything starting with an underscore as its injected by Mongo
	            		if (key.charAt(0) != "_") {
	            			var nodeID = i + "_" + key;
	            			var str = "<div class='wordwrap'>";
	            			str += "<span class='key'>"+key+"</span>";

	            			if (key=="server_config_string" || key == "access_token" || key == "token_endpoint_response") {
	            				str += "<span class='hypen'> : <a class='toggle' id='toggle_"+nodeID+"' href='javaScript:toggleShow(\""+nodeID+"\");'>SHOW</a></span>";
	            				str += "<div id='"+nodeID+"' class='value hidden'>" + val + "</div>";
	            			}
	            			else if (key=="time") {
	            				str += "<span class='hypen'> : </span><span class='value'>" + new Date(val) + "</span>";
	            			} else {
	            				str += "<span class='hypen'> : </span><span class='value'>" + val + "</span>";
	            			}

	            			str += "</div>";

	            			$("#logDetail .content").append(str);
	            			
	            			
	            		}
	            	});

	            	$("#logDetail .content").append("</div>");

	            });

	        }
	    });

	}
	
}

