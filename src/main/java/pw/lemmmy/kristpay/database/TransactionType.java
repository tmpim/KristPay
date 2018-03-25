package pw.lemmmy.kristpay.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

@Getter
@AllArgsConstructor
public enum TransactionType {
	DEPOSIT(Text.of(TextColors.DARK_GREEN, "D"), Text.of(TextColors.DARK_GREEN, "Deposit")),
	WITHDRAW(Text.of(TextColors.DARK_RED, "W"), Text.of(TextColors.DARK_RED, "Withdraw")),
	TRANSFER(Text.of(TextColors.GOLD, "T"), Text.of(TextColors.GOLD, "Transfer"));
	
	private Text shortText;
	private Text longText;
	
	public static Text getShortTextOf(String type) {
		try {
			return valueOf(type.toUpperCase()).shortText;
		} catch (Exception ignored) {
			return Text.of(TextColors.GRAY, ("" + type.charAt(1)).toUpperCase());
		}
	}
	
	public static Text getLongTextOf(String type) {
		try {
			return valueOf(type.toUpperCase()).longText;
		} catch (Exception ignored) {
			return Text.of(TextColors.GRAY, StringUtils.capitalize(type));
		}
	}
}
