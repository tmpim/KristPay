package pw.lemmmy.kristpay.krist;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.Utils;

import java.util.Optional;

public class KristAPI {
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
	
	public static Optional<KristTransaction> makeTransaction(String privatekey, String to, int amount, String metadata) throws UnirestException {
		HttpResponse<JsonNode> response = Unirest.post(getKristNode() + "/transactions")
			.field("privatekey", privatekey)
			.field("to", to)
			.field("amount", amount)
			.field("metadata", metadata)
			.asJson();
		
		JSONObject body = response.getBody().getObject();
		
		if (!body.getBoolean("ok")) return Optional.empty();
		return Optional.of(KristTransaction.fromJSON(body.getJSONObject("transaction")));
	}
	
	// https://github.com/Lignum/JKrist
	public static char byteToHexChar(int inp) {
		int b = 48 + inp / 7;
		if (b > 57) b += 39;
		if (b > 122) b = 101;
		return (char)b;
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
}
