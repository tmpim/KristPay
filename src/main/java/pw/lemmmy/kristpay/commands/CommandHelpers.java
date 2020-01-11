package pw.lemmmy.kristpay.commands;

import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.krist.Wallet;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.spongepowered.api.text.Text.of;
import static org.spongepowered.api.text.format.TextColors.*;
import static org.spongepowered.api.text.format.TextStyles.ITALIC;

public class CommandHelpers {
	private static final Pattern NAME_PATTERN = Pattern.compile("^(?:[a-z0-9-_]{1,32}@)?([a-z0-9]{1,64})\\.kst$");
	
	public static String getKristSymbol() {
		return KristPay.INSTANCE.getConfig().getEconomy().getKristSymbol();
	}
	
	public static String getKristCode() {
		return KristPay.INSTANCE.getConfig().getEconomy().getKristCode();
	}
	
	public static Text formatKrist(BigDecimal amount, boolean withSuffix) {
		return formatKrist(amount.intValue(), withSuffix);
	}
	
	public static Text formatKrist(int amount, boolean withSuffix) {
		return formatKrist(amount, YELLOW, withSuffix);
	}
	
	public static Text formatKrist(int amount, TextColor colour, boolean withSuffix) {
		return Text.builder()
			.append(of(colour, String.format("%s%,d%s", getKristSymbol(), amount, withSuffix ? " " + getKristCode() : "")))
		   	.onHover(TextActions.showText(of(String.format("%,d KST", amount, getKristCode()))))
			.build();
	}
	
	public static Text formatAddress(Wallet wallet) {
		return formatAddress(wallet.getAddress());
	}
	
	public static Text formatAddress(String address) {
		if (address == null) return of(ITALIC, GRAY, "unknown");
		
		Matcher nameMatcher = NAME_PATTERN.matcher(address);
		// TextColor colour = nameMatcher.matches() ? TextColors.AQUA : TextColors.BLUE;
		URL url = nameMatcher.matches() ? getKristWebURL("names", nameMatcher.group(1))
										: getKristWebURL("addresses", address);
		
		return Text.builder()
			.append(of(AQUA, address))
			.onHover(TextActions.showText(of(AQUA, url.toString())))
			.onClick(TextActions.openUrl(url))
			.build();
	}
	
	public static Text formatUser(User user) {
		if (user == null) return of(ITALIC, GRAY, "unknown");
		
		return Text.builder()
			.append(of(YELLOW, user.getName()))
			.onHover(TextActions.showText(of(user.getUniqueId().toString())))
			.build();
	}
	
	public static Text formatUser(User self, User user) {
		if (self != null && user != null && user.getUniqueId().equals(self.getUniqueId())) {
			return Text.builder()
				.append(of(ITALIC, GOLD, "You"))
				.onHover(TextActions.showText(Text.builder()
					.append(of(YELLOW, user.getName()))
					.append(of("\n"))
					.append(of(user.getUniqueId().toString()))
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
