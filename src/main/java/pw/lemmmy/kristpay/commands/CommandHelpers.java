package pw.lemmmy.kristpay.commands;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;

public class CommandHelpers {
	public static Text formatKrist(BigDecimal amount) {
		return formatKrist(amount.intValue());
	}
	
	public static Text formatKrist(int amount) {
		return Text.builder(String.format("%,d KST", amount)).color(TextColors.GREEN).build();
	}
}
