package pw.lemmmy.kristpay.database;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.economy.KristAccount;
import pw.lemmmy.kristpay.krist.KristAPI;
import pw.lemmmy.kristpay.krist.Wallet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Database {
	private File dbFile;
	
	private Map<UUID, KristAccount> accounts = new HashMap<>();
	
	public Database(File dbFile) {
		this.dbFile = dbFile;
		
		try {
			if (!dbFile.createNewFile()) return;
			
			JSONObject data = new JSONObject();
			data.put("accounts", new JSONArray());
			
			PrintWriter printWriter = new PrintWriter(dbFile);
			printWriter.print(data.toString(4));
			printWriter.close();
		} catch (IOException e) {
			KristPay.INSTANCE.getLogger().error("Error creating KristPay database", e);
		}
	}
	
	public void load() throws IOException, JSONException {
		JSONObject data = (JSONObject) new JSONTokener(new String(Files.readAllBytes(dbFile.toPath()))).nextValue();
		
		if (!data.has("accounts")) throw new RuntimeException("KristPay config has no 'accounts' entry");
		
		JSONArray accountsJSON = data.getJSONArray("accounts");
		
		accountsJSON.forEach(accountObject -> {
			JSONObject accountJSON = (JSONObject) accountObject;
			
			String privatekey = accountJSON.getString("depositPassword");
			String owner = accountJSON.getString("owner");
			int balance = accountJSON.getInt("balance");
			
			Wallet wallet = new Wallet(privatekey);
			KristAccount account = new KristAccount(owner, wallet, balance);
			
			// TODO: key as uuid or string?
			accounts.put(UUID.fromString(owner), account);
		});
	}
	
	public void syncWallets() {
	
	}
}
