/*
   © Copyright 2015 IBM Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

// Load modules
var NodeRSA = require('node-rsa'),
	fs = require('fs'),
	superagent = require('superagent'),
	Q = require('q');

console.log ("Usage node app.js <bluemixuser> <bluemixpassword> <tenantId>")

// Get args
var username =  process.argv[2];
var password =  process.argv[3];
var tenantId =  process.argv[4];

// Test encryption
var text = 'Hello RSA!';
var encryptor = new NodeRSA(fs.readFileSync(__dirname + '/resources/test_key.pub')); // Specify which public key to use
var encryptedString = encryptor.encrypt(text, 'base64');
console.log('encryptedString: ', encryptedString);

// Test decryption
var decryptor = new NodeRSA(fs.readFileSync(__dirname + '/resources/test_key.pem')); // Specify which private key to use
var decryptedString = decryptor.decrypt(encryptedString, 'utf8');
console.log('decryptedString: ', decryptedString);

var deferred = Q.defer();


/*
 *	Bluemix and PI APIs
 */
var bluemixLogin = function(){
	var base64auth = new Buffer('cf:').toString('base64');
	var body =  "grant_type=password"+
				   "&password="+password+
				   "&scope="+
				   "&username="+username;

	// Get Bluemix token
	superagent.post('https://login.ng.bluemix.net'+'/UAALoginServerWAR/oauth/token')
		.type('application/x-www-form-urlencoded')
		.set('Authorization', 'Basic ' + base64auth)
		.send(body)
		.end(function(err, res){
			var error = err;
			if (error) {
				console.log('security local.authenticate.error', error);
				deferred.reject(error)
			} else {
				console.log('security local.authenticate.success', res.body.access_token);
				var bearerToken = res.body.access_token;
				createOrg(tenantId, bearerToken); // Create an org
				deferred.resolve(res.body);
			}

		}
	);
}

// Create an org
var createOrg = function(tenantId, bearerToken){
	console.log("createOrg for tenant", tenantId);

	var body  = {
				  "name": "toddOrg2",
				  "registrationTypes": [
					"Internal"
				  ],
				  "description": "my test description",
				}
	body.publicKey = encryptor.exportKey('public');

	superagent.post('https://presenceinsights.ng.bluemix.net'+'/pi-config/v1/tenants/' + tenantId + '/orgs')
		.type('application/json')
		.set('Authorization', 'Bearer ' + bearerToken)
		.send(body)
		.end(function(err, res){
			var error = err;
			if (error) {
				deferred.reject(error)
			} else {
				console.log('created org status', res.status);
				registerDevice(res.headers.location, bearerToken); // Register a new device
				deferred.resolve(res.body);
			}
	});
}

// Register a device
var registerDevice = function(orgURL, bearerToken){
	console.log("registerDevice", orgURL);

	var body  = {
				  "name": "string",
				  "descriptor": "00:04:AC:FF:FF:FF",
				  "registered": true,
				  "registrationType": "Internal",
				  "data": {
					"email": "bob@gmail.com",
					"description": "Bob's iPhone"
				  }
				}
	superagent.post(orgURL + '/devices')
		.type('application/json')
		.set('Authorization', 'Bearer ' + bearerToken)
		.send(body)
		.end(function(err, res){
			var error = err;
			if (error) {
				deferred.reject(error)
			} else {
				console.log('registerDevice status', res.status);
				getDevice(res.headers.location, bearerToken); // Get the encrypted device data
				deferred.resolve(res.body);
			}
	});
}

// Get the device data
var getDevice = function(deviceUrl, bearerToken){
	console.log("getDevice", deviceUrl);

	superagent.get(deviceUrl)
		.set('Authorization', 'Bearer ' + bearerToken)
		.end(function(err, res){
			var error = err;
			if (error) {
				console.log('org org.creation.error', error);
				deferred.reject(error)
			} else {
				console.log('getDevice status', res.status);

				// Decrypt the data
				var decryptedEmail = decryptor.decrypt(res.body.data.email, 'utf8');
				console.log('decryptedEmail: ', decryptedEmail);

				var decryptedDescription = decryptor.decrypt(res.body.data.description, 'utf8');
				console.log('decryptedDescription: ', decryptedDescription);

				deferred.resolve(res.body);
			}
	});
}

// Run the login
bluemixLogin();
