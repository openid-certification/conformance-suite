var express = require("express");
var url = require("url");
var bodyParser = require('body-parser');
var randomstring = require("randomstring");
var cons = require('consolidate');
var querystring = require('querystring');
var qs = require("qs");
var __ = require('underscore');
__.string = require('underscore.string');
var base64url = require('base64url');
var jose = require('jsrsasign');

var app = express();

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true })); // support form-encoded bodies (for the token endpoint)

app.set('json spaces', 4);

// authorization server information
var authServer = {
	authorizationEndpoint: 'http://localhost:9001/authorize',
	tokenEndpoint: 'http://localhost:9001/token'
};

// client information
var clients = [
	{
		"client_id": process.env.client_id ? process.env.client_id : "oauth-client-1",
		"client_secret": process.env.client_secret ? process.env.client_secret : "oauth-client-secret-1"
	}
];

var sharedTokenSecret = "shared token secret!";

var preplacedToken = {
	value: randomstring.generate(),
	sub: 'AUTOMATED-TEST',
	user_id: 'automated-test',
	scope: ['fapi-test-suite']
}

var protectedResources = [
	{
		"resource_id": process.env.resource_id ? process.env.resource_id : "protected-resource-1",
		"resource_secret": process.env.resource_secret ? process.env.resource_secret : "protected-resource-secret-1"
	}
];

var getClient = function(clientId) {
	return __.find(clients, function(client) { return client.client_id == clientId; });
};

var getProtectedResource = function(resourceId) {
	return __.find(protectedResources, function(resource) { return resource.resource_id == resourceId; });
};


app.post("/token", function(req, res){
	
	var auth = req.headers['authorization'];
	if (auth) {
		// check the auth header
		var clientCredentials = new Buffer(auth.slice('basic '.length), 'base64').toString().split(':');
		var clientId = querystring.unescape(clientCredentials[0]);
		var clientSecret = querystring.unescape(clientCredentials[1]);
	}
	
	// otherwise, check the post body
	if (req.body.client_id) {
		if (clientId) {
			// if we've already seen the client's credentials in the authorization header, this is an error
			console.log('Client attempted to authenticate with multiple methods');
			res.status(401).json({error: 'invalid_client'});
			return;
		}
		
		var clientId = req.body.client_id;
		var clientSecret = req.body.client_secret;
	}
	
	var client = getClient(clientId);
	if (!client) {
		console.log('Unknown client %s', clientId);
		res.status(401).json({error: 'invalid_client'});
		return;
	}
	
	if (client.client_secret != clientSecret) {
		console.log('Mismatched client secret, expected %s got %s', client.client_secret, clientSecret);
		res.status(401).json({error: 'invalid_client'});
		return;
	}
	
	if (req.body.grant_type == 'client_credentials') {
		var scope = preplacedToken.scope;

		var access_token = preplacedToken.value;
		
		var token_response = { access_token: access_token, token_type: 'Bearer', scope: scope.join(' ') };
		
		console.log('Issuing access token %s', access_token);
		res.status(200).json(token_response);
		return;	
		
	} else {
		console.log('Unknown grant type %s', req.body.grant_type);
		res.status(400).json({error: 'unsupported_grant_type'});
	}
});

app.post('/introspect', function(req, res) {
	var auth = req.headers['authorization'];
	if (auth) {
		var resourceCredentials = new Buffer(auth.slice('basic '.length), 'base64').toString().split(':');
		var resourceId = querystring.unescape(resourceCredentials[0]);
		var resourceSecret = querystring.unescape(resourceCredentials[1]);
	} else {
		console.log('Unknown resource');
		res.status(401).end();
		return;
	}

	var resource = getProtectedResource(resourceId);
	if (!resource) {
		console.log('Unknown resource %s', resourceId);
		res.status(401).end();
		return;
	}
	
	if (resource.resource_secret != resourceSecret) {
		console.log('Mismatched secret, expected %s got %s', resource.resource_secret, resourceSecret);
		res.status(401).end();
		return;
	}
	
	var inToken = req.body.token;
	console.log('Introspecting token %s', inToken);
	
	if (inToken == preplacedToken.value) {
		console.log("We found a matching token: %s", inToken);
		
		var introspectionResponse = {};
		introspectionResponse.active = true;
		introspectionResponse.iss = authServer.iss;
		introspectionResponse.sub = preplacedToken.sub;
		introspectionResponse.scope = preplacedToken.scope.join(' ');
		introspectionResponse.client_id = clients[0].client_id;

		res.status(200).json(introspectionResponse);
		return;
	} else {
		console.log('No matching token was found.');

		var introspectionResponse = {};
		introspectionResponse.active = false;
		res.status(200).json(introspectionResponse);
		return;
	}
	
});

var buildUrl = function(base, options, hash) {
	var newUrl = url.parse(base, true);
	delete newUrl.search;
	if (!newUrl.query) {
		newUrl.query = {};
	}
	__.each(options, function(value, key, list) {
		newUrl.query[key] = value;
	});
	if (hash) {
		newUrl.hash = hash;
	}
	
	return url.format(newUrl);
};

var server = app.listen(9001, '0.0.0.0', function () {
  var host = server.address().address;
  var port = server.address().port;

  console.log('MicrOAuth Authorization Server is listening at http://%s:%s', host, port);
});
 
