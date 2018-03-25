package pw.lemmmy.kristpay.commands;

import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
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
	
	public static Text formatUser(User user) {
		if (user == null) return Text.of(TextStyles.ITALIC, TextColors.GRAY, "unknown");
		
		return Text.builder()
			.append(Text.of(TextColors.YELLOW, user.getName()))
			.onHover(TextActions.showText(Text.of(user.getUniqueId().toString())))
			.build();
	}
	
	public static Text formatUser(User self, User user) {
		if (self != null && user != null && user.getUniqueId().equals(self.getUniqueId())) {
			return Text.builder()
				.append(Text.of(TextStyles.ITALIC, TextColors.GOLD, "You"))
				.onHover(TextActions.showText(Text.builder()
					.append(Text.of(TextColors.YELLOW, user.getName()))
					.append(Text.of("\n"))
					.append(Text.of(user.getUniqueId().toString()))
					.build()))
				.build();
		}
		
		return formatUser(user);
	}
}
