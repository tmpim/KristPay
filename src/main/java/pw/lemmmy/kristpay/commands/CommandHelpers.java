package pw.lemmmy.kristpay.commands;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import pw.lemmmy.kristpay.krist.Wallet;

import java.math.BigDecimal;

public class CommandHelpers {
	public static Text formatKrist(BigDecimal amount) {
		return formatKrist(amount.intValue());
	}
	
	public static Text formatKrist(int amount) {
		return Text.builder(String.format("%,d KST", amount)).color(TextColors.YELLOW).build();
	}
	
	public static Text formatAddress(Wallet wallet) {
		return formatAddress(wallet.getAddress());
	}
	
	public static Text formatAddress(String address) {
		return Text.builder(address).color(TextColors.YELLOW).build();
	}
}
