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
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class WelfareManager {
	private static final EconomyService ECONOMY_SERVICE = KristPay.INSTANCE.getEconomyService();
	
	public static int getWelfareAmount() {
		return KristPay.INSTANCE.getConfig().getWelfare().getWelfareAmount();
	}
	
	public static int getWelfareMinimumDays() {
		return KristPay.INSTANCE.getConfig().getWelfare().getWelfareMinimumDays();
	}
	
	private static boolean canIncreaseCounter(KristAccount account) {
		Instant lastPayment = account.getWelfareLastPayment();
		Instant now = Instant.now();
		
		return lastPayment.plus(Duration.ofHours(18)).isBefore(now);
	}
	
	private static void reward(KristAccount account) {
		account.deposit(
			KristPay.INSTANCE.getCurrency(),
			BigDecimal.valueOf(getWelfareAmount()),
			Sponge.getCauseStackManager().getCurrentCause()
		);
		
		new TransactionLogEntry()
			.setSuccess(true)
			.setType(TransactionType.WELFARE)
			.setToAccount(account)
			.setAmount(getWelfareAmount())
			.addAsync();
	}
	
	private static void notifyReward(Player player) {
		player.sendMessage(Text.builder()
			.append(Text.of(TextColors.GREEN, "You received today's "))
			.append(CommandHelpers.formatKrist(getWelfareAmount()))
			.append(Text.of(TextColors.GREEN, " daily login bonus."))
			.build());
	}
	
	public static void checkWelfare(Player player, KristAccount account) {
		if (account.getWelfareCounter() == 0 || canIncreaseCounter(account)) {
			account.incrementWelfareCounter();
			
			if (account.getWelfareCounter() >= getWelfareMinimumDays()) {
				Task.builder() // run this on the main thread
					.execute(() -> {
						reward(account);
						notifyReward(player);
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
