package pw.lemmmy.kristpay.krist;

import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.Getter;
import pw.lemmmy.kristpay.KristPay;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Getter
public class Wallet {
	private String password, privatekey;
	private String address;
	private int balance;
	
	public Wallet(String password) {
		this.password = password;
		this.privatekey = KristAPI.makeKristWalletPrivatekey(password);
		this.address = KristAPI.makeKristWalletAddress(password);
	}
	
	public void syncWithNode(Consumer<Boolean> callback) {
		if (!KristPay.INSTANCE.isUp()) {
			callback.accept(false);
			return;
		}
		
		KristPay.INSTANCE.getKristClientManager().getKristClient().getAddressBalanceAync(address, newBalance -> {
			balance = newBalance;
			callback.accept(true);
		});
	}
	
	public CompletableFuture<KristTransaction> transfer(String to, int amount, String metadata) {
		return CompletableFuture.supplyAsync(() -> {
			if (!KristPay.INSTANCE.isUp()) throw new KristDownException();
			if (address.equalsIgnoreCase(to)) throw new SelfSendException();
			
			try {
				return KristAPI.makeTransaction(privatekey, to, amount, metadata);
			} catch (UnirestException e) {
				KristPay.INSTANCE.getLogger().error("Error transferring KST from " + address + " to " + to, e);
				throw new KristDownException();
			}
		}).thenApplyAsync(kristTransaction -> {
			syncWithNode(cb -> {});
			return kristTransaction;
		});
	}
}
