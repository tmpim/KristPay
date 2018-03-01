package pw.lemmmy.kristpay.krist;

import lombok.Getter;
import pw.lemmmy.kristpay.KristPay;

import java.util.function.Consumer;

@Getter
public class Wallet {
	private String privatekey;
	private String address;
	private int balance;
	
	public Wallet(String privatekey) {
		this.privatekey = privatekey;
		this.address = KristAPI.makeKristWalletAddress(privatekey);
	}
	
	public void syncWithNode(Consumer<Boolean> callback) {
		if (!KristPay.INSTANCE.isUp()) callback.accept(false);
		
		KristPay.INSTANCE.getKristClientManager().getKristClient().getAddressBalanceAync(address, newBalance -> {
			balance = newBalance;
			callback.accept(true);
		});
	}
	
	public void transfer(String address, int amount, Consumer<Boolean> callback) {
		// TODO: implement
	}
}
