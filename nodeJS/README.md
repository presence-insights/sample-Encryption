# MobileFirst Platform - Decrypting Device Data With Node.js

## Overview

To save sensitive user information in Presence Insights the preferred method is to add the information to the "data" field when registering or updating a device. Data entered in this field will be automatically encrypted when the device is registered or updated. 

Using this field requires [adding a public key](https://presenceinsights.ibmcloud.com/pidocs/configure/security) to an organization through the Presence Insights dashboard.

## Purpose

The main objective of this project is to demonstrate how to decrypt data encrypted data added to a device in Presence Insights.

To do this the code takes the following steps:

1. Logs into Bluemix using the user's credentials and acquires a token for future API calls
2. Creates an org in Presence Insights
3. Registers a device with a generic email and description
4. Retrieves the encrypted email and description
5. Decrypts the email and description using the user's private key

## Contents

This project contains the following files:

* **app.js** - main source code
* **test_key.pub** - public key file
* **test_key.pem** - private key file

## Running this code

This code can be run from the command line by navigating to the folder it's in and running the following commands:

		npm install node-rsa fs sha1 superagent q
		node app.js <bluemixuser> <bluemixpassword> <tenantId>
where `<bluemixuser>` is the user's Bluemix username, `<bluemixpassword>` is the user's Bluemix password, and `<tenantId>` is the user's tenant ID of their Presence Insights instance.

This command will install all the necessary packages as well as start the program.

===

Copyright 2015 IBM Corp.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
