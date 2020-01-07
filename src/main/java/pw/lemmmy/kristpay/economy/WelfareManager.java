package pw.lemmmy.kristpay.economy;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.commands.CommandHelpers;
import pw.lemmmy.kristpay.database.TransactionLogEntry;
import pw.lemmmy.kristpay.database.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

public class WelfareManager {
	private static final EconomyService ECONOMY_SERVICE = KristPay.INSTANCE.getEconomyService();
	
	public static int getWelfareAmount(KristAccount account) {
		return account.getWelfareAmount() != -1 ? account.getWelfareAmount() : KristPay.INSTANCE.getConfig().getWelfare().getWelfareAmount();
	}
	
	public static int getWelfareMinimumDays() {
		return KristPay.INSTANCE.getConfig().getWelfare().getWelfareMinimumDays();
	}
	
	private static boolean canIncreaseCounter(KristAccount account) {
		ZoneId timezone = ZoneId.systemDefault();
		
		// convert them to dates, so we only act on the days
		LocalDate lastPayment = LocalDateTime.ofInstant(account.getWelfareLastPayment(), timezone).toLocalDate();
		LocalDate now = LocalDateTime.ofInstant(Instant.now(), timezone).toLocalDate();
		
		return now.isAfter(lastPayment);
	}
	
	private static void reward(KristAccount account) {
		account.deposit(
			KristPay.INSTANCE.getCurrency(),
			BigDecimal.valueOf(getWelfareAmount(account)),
			Sponge.getCauseStackManager().getCurrentCause()
		);
		
		new TransactionLogEntry()
			.setSuccess(true)
			.setType(TransactionType.WELFARE)
			.setToAccount(account)
			.setAmount(getWelfareAmount(account))
			.addAsync();
	}
	
	private static void notifyReward(KristAccount account, Player player) {
		player.sendMessage(Text.builder()
			.append(Text.of(TextColors.GREEN, "You received today's "))
			.append(CommandHelpers.formatKrist(getWelfareAmount(account)))
			.append(Text.of(TextColors.GREEN, " daily login bonus."))
			.build());
	}
	
	public static void checkWelfare(Player player, KristAccount account) {
		if (!player.hasPermission("kristpay.welfare.receive")) return;
		if (account.getWelfareCounter() == 0 || canIncreaseCounter(account)) {
			account.incrementWelfareCounter();
			
			if (account.getWelfareCounter() >= getWelfareMinimumDays()) {
				Task.builder() // run this on the main thread
					.execute(() -> {
						reward(account);
						notifyReward(account, player);
					})
					.name("KristPay - Welfare reward")
					.submit(KristPay.INSTANCE);
			}
		}
	}
	
	public static void handleLogin(Player player, KristAccount account) {
		Task.builder()
			.execute(() -> checkWelfare(player, account))
			.async()
			.delay(1500, TimeUnit.MILLISECONDS)
			.name("KristPay - Welfare payment")
			.submit(KristPay.INSTANCE);
	}
	
	public static void checkAllOnlinePlayers() {
		Sponge.getServer().getOnlinePlayers().forEach(player -> {
			if (ECONOMY_SERVICE.hasAccount(player.getUniqueId())) {
				ECONOMY_SERVICE.getOrCreateAccount(player.getUniqueId()).ifPresent(uniqueAccount -> {
					if (!(uniqueAccount instanceof KristAccount)) return;
					KristAccount account = (KristAccount) uniqueAccount;
					checkWelfare(player, account);
				});
			}
		});
	}
}
