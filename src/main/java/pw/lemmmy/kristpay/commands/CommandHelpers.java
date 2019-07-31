package pw.lemmmy.kristpay.commands;

import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.krist.Wallet;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHelpers {
	private static final Pattern NAME_PATTERN = Pattern.compile("^(?:[a-z0-9-_]{1,32}@)?([a-z0-9]{1,64})\\.kst$");
	
	public static Text formatKrist(BigDecimal amount) {
		return formatKrist(amount.intValue());
	}
	
	public static Text formatKrist(int amount) {
		return formatKrist(amount, TextColors.YELLOW);
	}
	
	public static Text formatKrist(int amount, TextColor colour) {
		return Text.of(colour, String.format("%,d KST", amount));
	}
	
	public static Text formatAddress(Wallet wallet) {
		return formatAddress(wallet.getAddress());
	}
	
	public static Text formatAddress(String address) {
		if (address == null) return Text.of(TextStyles.ITALIC, TextColors.GRAY, "unknown");
		
		Matcher nameMatcher = NAME_PATTERN.matcher(address);
		// TextColor colour = nameMatcher.matches() ? TextColors.AQUA : TextColors.BLUE;
		URL url = nameMatcher.matches() ? getKristWebURL("names", nameMatcher.group(1))
										: getKristWebURL("addresses", address);
		
		return Text.builder()
			.append(Text.of(TextColors.AQUA, address))
			.onHover(TextActions.showText(Text.of(TextColors.AQUA, url.toString())))
			.onClick(TextActions.openUrl(url))
			.build();
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
	
	private static URL getKristWebURL(String endpoint, String url) {
		try {
			return new URL("https://krist.club/" + endpoint + "/" + URLEncoder.encode(url, "UTF-8"));
		} catch (MalformedURLException | UnsupportedEncodingException e) {
			KristPay.INSTANCE.getLogger().error("Ughrrr", e);
		}
		
		return null;
	}
}
