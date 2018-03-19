package pw.lemmmy.kristpay.krist;

import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.Getter;
import pw.lemmmy.kristpay.KristPay;

import java.util.Optional;
import java.util.function.BiConsumer;
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
	
	public void transfer(String to, int amount, String metadata, BiConsumer<Boolean, KristTransaction> callback) {
		if (!KristPay.INSTANCE.isUp()) {
			callback.accept(false, null);
			return;
		}
		
		try {
			Optional<KristTransaction> opt = KristAPI.makeTransaction(privatekey, to, amount, metadata);
			
			if (opt.isPresent()) {
				syncWithNode(success -> {
					if (!success) {
						// TODO: handle this better?
						balance -= amount;
					}
					
					callback.accept(true, opt.get());
				});
			} else {
				callback.accept(false, null);
			}
		} catch (UnirestException e) {
			KristPay.INSTANCE.getLogger().error("Error transferring KST from " + address + " to " + to, e);
			callback.accept(false, null);
		}
	}
}
