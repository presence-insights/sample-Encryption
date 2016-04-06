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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.net.ssl.HttpsURLConnection;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class RSA {
	
	// Static vars
	public static String ALGORITHM = "RSA";
	public static String text = "Hello RSA!";
	public static String PUBLIC_KEY_FILE = "resources/test_key.pub";
	public static String PRIVATE_KEY_FILE = "resources/test_key.pem";
	
	// URLs
	public static String BLUEMIX_URL = "https://login.ng.bluemix.net/UAALoginServerWAR/oauth/token";
	public static String ORG_URL = "https://presenceinsights.ng.bluemix.net/pi-config/v1/tenants/";
	
	// Command line args
	public static String user;
	public static String password;
	public static String tenantId;
	
	
	// Get the file as bytes before returning as keys
	public String getFileAsString(String name, String keyType) throws Exception {

		ClassLoader load  =  getClass().getClassLoader();
		File file =  new File(load.getResource(name).getFile());
		
		FileInputStream fis = new FileInputStream(file);
		DataInputStream dis = new DataInputStream(fis);
		
		byte[] key = new byte[(int)file.length()];
		
		dis.readFully(key);
		dis.close();
		
		if (keyType.toLowerCase() == "public") {
			
			String temp = new String(key);
			String formattedKey = temp.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
			
		    return formattedKey;
		    
		} else if (keyType.toLowerCase() == "private") {
			
			String temp = new String(key);
			String formattedKey = temp.replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "");
			
			return formattedKey;
			
		} else {
			String failure = "keyType needs to be either \"public\" or \"private\"";
			return failure;
		}
		
	}
	
	// Read public key
	public PublicKey getPublicKeyFromFile(String fileName) throws Exception {
		String formattedKey = getFileAsString(fileName, "public");
		
	    byte[] decoded = Base64.decode(formattedKey);

		X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
	    KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
	    
	    return kf.generatePublic(spec);
	}
	
	// Read private key
	public PrivateKey getPrivateKeyFromFile(String fileName) throws Exception {
		String formattedKey = getFileAsString(fileName, "private");
		
	    byte[] decoded = Base64.decode(formattedKey);
		
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
	    KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
	    
	    return kf.generatePrivate(spec);
	}
	
	// Encrypt string
	public byte[] encrypt(String decryptedText, PublicKey publicKey) {
		
		byte[] cipherOut = null;
		
		try {
			Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA1AndMGF1Padding", "BC"); // Specify padding
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			cipherOut = cipher.doFinal(decryptedText.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return cipherOut;
	}
	
	// Decrypt string
	public String decrypt(byte[] encrtypedText, PrivateKey privateKey) {
		byte[] decrypted = null;
		
		try {
			Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA1AndMGF1Padding", "BC"); // Specify padding
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			decrypted = cipher.doFinal(encrtypedText);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String plaintext = new String(decrypted);
		
		return plaintext;
	}
	
	
	
	
	
	// Login to Bluemix
	public void bluemixLogin() throws Exception {
	
		URL url = new URL(BLUEMIX_URL);
		
		byte[] base = Base64.encode(("cf:").getBytes());
		String encode = new String(base);
		
		String body = "grant_type=password"+
					"&password="+password+
					"&scope="+
					"&username="+user;
		byte[] bodyBytes = body.getBytes();
		
		// POST data
		HttpsURLConnection conn =  (HttpsURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Authorization", "Basic " + encode);

		conn.getOutputStream().write(bodyBytes);
		
		// Get response
		BufferedReader br = null;
	    br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    StringBuilder sb = new StringBuilder();
        String line;
        
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();
        
        JSONObject json = JSONObject.parse(sb.toString());
        
        String token = (String)json.get("access_token");
		
        // Create an org
		createOrg(tenantId, token);
	}
	
	// Add an org
	public void createOrg(String tenantId, String token) throws Exception {
		
		URL url = new URL(ORG_URL + tenantId + "/orgs");
		
		JSONObject body = new JSONObject();
		JSONArray reg = new JSONArray();
		reg.add("Internal");
		
		String[] registration = new String[1];
		registration[0] = "Internal";
		
		body.put("name", "testOrg");
		body.put("registrationTypes", reg);
		body.put("description", "test description");
		
		String tempKey = getFileAsString(PUBLIC_KEY_FILE, "public");
		String tempKey2 = "-----BEGIN PUBLIC KEY-----" + tempKey + "-----END PUBLIC KEY-----";
		
		body.put("publicKey", tempKey2);
		
		byte[] bodyBytes = body.toString().getBytes();
		
		// POST data
		HttpsURLConnection conn =  (HttpsURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Authorization", "Bearer " + token);
		
		conn.getOutputStream().write(bodyBytes);
		
		// Get response
		BufferedReader br = null;
	    br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    StringBuilder sb = new StringBuilder();
        String line;
        
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();

        // Get org code
        JSONObject response = JSONObject.parse(sb.toString());
        
        String orgCode = (String)response.get("@code");
        
        // Register a device
        registerDevice(ORG_URL + tenantId + "/orgs/" + orgCode, token);
	}
	
	// Add a device
	public void registerDevice(String orgURL, String token) throws Exception {
		
		URL url = new URL(orgURL + "/devices");
		
		JSONObject data = new JSONObject();
		data.put("email", "bob@email.com");
		data.put("description", "Bob's phone");
		
		JSONObject body = new JSONObject();
		
		body.put("name", "string");
		body.put("descriptor", "00:04:AC:FF:FF:FF");
		body.put("registered", true);
		body.put("registrationType", "Internal");
		body.put("data", data);
		
		byte[] bodyBytes = body.toString().getBytes();
		
		// POST data
		HttpsURLConnection conn =  (HttpsURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Authorization", "Bearer " + token);
		
		conn.getOutputStream().write(bodyBytes);
		
		// Get response
		BufferedReader br = null;
	    br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    StringBuilder sb = new StringBuilder();
        String line;
        
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();
        
    	// Get org code
        JSONObject response = JSONObject.parse(sb.toString());
        
        String deviceCode = (String)response.get("@code");
		
		// Get the device data
        getDevice(orgURL + "/devices/" + deviceCode, token);
	}
	
	// Get the device data back
	public void getDevice(String deviceURL, String token) throws Exception {
		
		URL url = new URL(deviceURL);
		
		// GET data
		HttpsURLConnection conn =  (HttpsURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Authorization", "Bearer " + token);
		
		// Get response
		BufferedReader br = null;
	    br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    StringBuilder sb = new StringBuilder();
        String line;
        
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();
        
        // Get device data as strings
        JSONObject response = JSONObject.parse(sb.toString());
        JSONObject data = (JSONObject)response.get("data");
        
        String email = (String)data.get("email");
        String description = (String)data.get("description");
        
        System.out.println("encrypted email: " + email);
        System.out.println("encrypted description: " + description);

        // Decode encrypted data
        byte[] encryptedEmail = Base64.decode(email);
        byte[] encryptedDescription = Base64.decode(description);
        
        PrivateKey privKey = getPrivateKeyFromFile(PRIVATE_KEY_FILE);
        
        // Decrypt email and description
        String emailOut = decrypt(encryptedEmail, privKey);
        String descriptionOut = decrypt(encryptedDescription, privKey);
        
        System.out.println();
        
        System.out.println("decrypted email: " + emailOut);
        System.out.println("decrypted description: " + descriptionOut);
	}
	
	

	public static void main(String[] args) throws Exception {
		
		System.out.println("Usage: javac RSA.java && java RSA <bluemixuser> <bluemixpassword> <tenantId>");
		
		// Add Bouncy Castle
		Security.addProvider(new BouncyCastleProvider());
		
		// Assign args
		user = args[0];
		password = args[1];
		tenantId = args[2];
		
		System.out.println("arg[1] = " + user);
		System.out.println("arg[3] = " + tenantId);
		
		System.out.println();
		
		RSA h = new RSA();
		
		// Get keys
		PublicKey pubKey = h.getPublicKeyFromFile(PUBLIC_KEY_FILE);
		PrivateKey privKey = h.getPrivateKeyFromFile(PRIVATE_KEY_FILE);
		
		// Test encrypt
		byte[] encrypted = h.encrypt(text, pubKey);
		System.out.println("encrypted string: " + new String(encrypted) + "\n");
		
		//Test decrypt
		String decrypted = h.decrypt(encrypted, privKey);
		System.out.println("decrypted string: " + decrypted + "\n");
	
		// Run the login and PI API
		h.bluemixLogin();
	}
}