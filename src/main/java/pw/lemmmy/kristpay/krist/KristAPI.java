package pw.lemmmy.kristpay.krist;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KristAPI {
	private static final Pattern KRIST_NAME_PATTERN = Pattern.compile("^(?:([a-z0-9-_]{1,32})@)?([a-z0-9]{1,64}\\.kst)$");
	
	public static String getKristNode() {
		return KristPay.INSTANCE.getConfig().getNode().getAddress();
	}
	
	public static Optional<String> getWebsocketURL(String privatekey) throws UnirestException {
		HttpResponse<JsonNode> response = Unirest.post(getKristNode() + "/ws/start")
			.field("privatekey", privatekey)
			.asJson();
		
		JSONObject body = response.getBody().getObject();
		
		if (!body.getBoolean("ok")) return Optional.empty();
		return Optional.of(body.getString("url"));
	}
	
	public static KristTransaction makeTransaction(String privatekey, String to, int amount, String metadata) throws UnirestException, NameNotFoundException {
		HttpResponse<JsonNode> response = Unirest.post(getKristNode() + "/transactions")
			.field("privatekey", privatekey)
			.field("to", to)
			.field("amount", amount)
			.field("metadata", metadata)
			.asJson();
		
		JSONObject body = response.getBody().getObject();
		
		if (!body.getBoolean("ok")) {
			if (body.getString("error").equals("name_not_found")) throw new NameNotFoundException();
			else throw new KristException(body.getString("error"));
		}
		
		return KristTransaction.fromJSON(body.getJSONObject("transaction"));
	}
	
	// https://github.com/Lignum/JKrist
	public static char byteToHexChar(int inp) {
		int b = 48 + inp / 7;
		if (b > 57) b += 39;
		if (b > 122) b = 101;
		return (char) b;
	}
	
	// https://github.com/Lignum/JKrist
	public static String makeV2Address(String pkey) {
		StringBuilder address = new StringBuilder("k");
		String hash = Utils.sha256(Utils.sha256(pkey));
		String[] proteins = new String[9];
		
		for (int i = 0; i < proteins.length; i++) {
			proteins[i] = hash.substring(0, 2);
			hash = Utils.sha256(Utils.sha256(hash));
		}
		
		for (int i = 0; i < 9;) {
			String pair = hash.substring(i * 2, (i * 2) + 2);
			int index = Integer.parseInt(pair, 16) % 9;
			
			if (proteins[index] == null) {
				hash = Utils.sha256(hash);
			} else {
				int protein = Integer.parseInt(proteins[index], 16);
				address.append(byteToHexChar(protein));
				proteins[index] = null;
				i++;
			}
		}
		
		return address.toString();
	}
	
	public static String makeKristWalletAddress(String password) {
		return makeV2Address(makeKristWalletPrivatekey(password));
	}
	
	public static String makeKristWalletPrivatekey(String password) {
		return Utils.sha256("KRISTWALLET" + password) + "-000";
	}
	
	public static Optional<Map<String, String>> parseCommonMeta(String metadata) {
		if (metadata == null || metadata.isEmpty()) return Optional.empty();
		
		Map<String, String> parts = new HashMap<>();
		
		String[] metaParts = metadata.split(";");
		if (metaParts.length <= 0) return Optional.empty();
		
		Matcher nameMatcher = KRIST_NAME_PATTERN.matcher(metaParts[0]);
		
		if (nameMatcher.matches()) { // first meta argument is a name
			if (nameMatcher.group(1) != null) parts.put("metaname", nameMatcher.group(1));
			if (nameMatcher.group(2) != null) parts.put("name", nameMatcher.group(2));
		}
		
		for (int i = 0; i < metaParts.length; i++) { // add the rest of the arguments
			String metaPart = metaParts[i];
			String[] kv = metaPart.split("=", 2);
			
			if (kv.length == 1) { // no key specified, use argn as key
				parts.put(String.valueOf(i), kv[0]);
			} else {
				parts.put(kv[0], kv[1]);
			}
		}
		
		return Optional.of(parts);
	}
}
