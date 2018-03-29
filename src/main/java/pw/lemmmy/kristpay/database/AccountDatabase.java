package pw.lemmmy.kristpay.database;

import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.spongepowered.api.scheduler.Task;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.economy.KristAccount;
import pw.lemmmy.kristpay.krist.Wallet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AccountDatabase {
	private File dbFile;
	
	@Getter
	private Map<String, KristAccount> accounts = new HashMap<>();
	
	private Lock dbLock = new ReentrantLock();
	
	public AccountDatabase(File dbFile) {
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
	
	public int getTotalDistributedKrist() {
		return accounts.values().stream()
			.mapToInt(KristAccount::getBalance)
			.sum();
	}
	
	public void load() throws IOException, JSONException {
		accounts.clear();
		
		JSONObject data = (JSONObject) new JSONTokener(new String(Files.readAllBytes(dbFile.toPath()))).nextValue();
		
		if (!data.has("accounts")) throw new RuntimeException("KristPay config has no 'accounts' entry");
		
		JSONArray accountsJSON = data.getJSONArray("accounts");
		
		for (int i = 0; i < accountsJSON.length(); i++) {
			JSONObject accountJSON = accountsJSON.getJSONObject(i);
			
			String privatekey = accountJSON.getString("depositPassword");
			String owner = accountJSON.getString("owner");
			int balance = accountJSON.getInt("balance");
			int unseenDeposit = accountJSON.optInt("unseenDeposit", 0);
			int unseenTransfer = accountJSON.optInt("unseenTransfer", 0);
			
			Wallet wallet = new Wallet(privatekey);
			KristAccount account = new KristAccount(owner, wallet, balance, unseenDeposit, unseenTransfer);
			
			accounts.put(owner, account);
		}
		
		accounts.values().stream()
			.filter(KristAccount::isNeedsSave)
			.findFirst()
			.ifPresent(ignored -> save());
	}
	
	private void saveFile() throws IOException {
		JSONObject data = new JSONObject();
		JSONArray accountsJSON = new JSONArray();
		
		accounts.forEach((uuid, kristAccount) -> accountsJSON.put(new JSONObject()
			.put("owner", kristAccount.getOwner())
			.put("balance", kristAccount.getBalance())
			.put("depositPassword", kristAccount.getDepositWallet().getPassword())
			.put("unseenDeposit", kristAccount.getUnseenDeposit())
			.put("unseenTransfer", kristAccount.getUnseenTransfer())
		));
		
		data.put("accounts", accountsJSON);
		
		PrintWriter printWriter = new PrintWriter(dbFile);
		printWriter.write(data.toString(4));
		printWriter.close();
	}
	
	public void save() {
		Task.builder()
			.execute(() -> {
				dbLock.lock();
				
				try {
					saveFile();
				} catch (IOException e) {
					KristPay.INSTANCE.getLogger().error("Error saving KristPay database", e);
				} finally {
					dbLock.unlock();
				}
			})
			.async()
			.name("KristPay - Saving database")
			.submit(KristPay.INSTANCE);
	}
	
	public void syncWallets() {
		KristPay.INSTANCE.getLogger().info("Syncing wallets.");
		
		accounts.forEach((uuid, kristAccount) -> kristAccount.getDepositWallet().syncWithNode(success -> {
			if (KristPay.INSTANCE.getDepositManager() != null)
				KristPay.INSTANCE.getDepositManager().walletSynced(kristAccount);
		}));
	}
}
