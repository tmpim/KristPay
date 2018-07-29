package pw.lemmmy.kristpay.economy;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.commands.CommandHelpers;
import pw.lemmmy.kristpay.config.ConfigFaucet;
import pw.lemmmy.kristpay.database.FaucetReward;
import pw.lemmmy.kristpay.database.TransactionLogEntry;
import pw.lemmmy.kristpay.database.TransactionType;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class FaucetManager {
	public static List<Integer> getRewardTiers() {
		return KristPay.INSTANCE.getConfig().getFaucet().getRewardTiers();
	}
	
	public static int getRewardValue(int tier) {
		List<Integer> tiers = getRewardTiers();
		return tiers.get(Math.max(Math.min(tier, tiers.size() - 1), 0));
	}
	
	public static int getNextRewardTier(FaucetReward lastReward) {
		return getNextRewardTier(lastReward, false);
	}
	
	public static int getNextRewardTier(FaucetReward lastReward, boolean ignoreMinimum) {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		
		if (!ignoreMinimum && lastReward.getMinimum().after(now)) return -1; // too close to last reward
		if (lastReward.getExpires().before(now)) return 0; // last reward expired, start from first tier again
		
		List<Integer> tiers = getRewardTiers();
		return Math.min(lastReward.getRewardTier() + 1, tiers.size() - 1);
	}
	
	public static FaucetReward redeem(InetSocketAddress address, UniqueAccount account, int tier) {
		ConfigFaucet config = KristPay.INSTANCE.getConfig().getFaucet();
		int value = getRewardValue(tier);
		
		FaucetReward reward = new FaucetReward()
			.setIP(address)
			.setAccount(account)
			.setRewardTier(tier)
			.setValue(value)
			.setTimestamps(config.getMinimumTime(), config.getExpireTime())
			.add();
		
		account.deposit(
			KristPay.INSTANCE.getCurrency(),
			BigDecimal.valueOf(value),
			Sponge.getCauseStackManager().getCurrentCause()
		);
		
		new TransactionLogEntry()
			.setSuccess(true)
			.setType(TransactionType.FAUCET)
			.setToAccount(account)
			.setAmount(value)
			.addAsync();
		
		return reward;
	}
	
	public static void handleLogin(Player player, KristAccount account) {
		Task.builder()
			.execute(() -> {
				Optional<FaucetReward> lastRewardOpt = FaucetReward.getLastReward(
					KristPay.INSTANCE.getDatabase().getDataSource(),
					player.getConnection().getAddress(), account
				);
				
				// start them on the first tier if they haven't claimed a reward before
				int nextTier = lastRewardOpt.map(FaucetManager::getNextRewardTier).orElse(0);
				if (nextTier <= -1) return; // don't notify if they've already redeemed
				
				player.sendMessage(Text.builder()
					.append(Text.of(TextColors.GREEN, "You have not yet redeemed your faucet reward of "))
					.append(CommandHelpers.formatKrist(FaucetManager.getRewardValue(nextTier)))
					.append(Text.of(TextColors.GREEN, ". Run "))
					.append(Text.of(TextColors.AQUA, "/faucet"))
					.append(Text.of(TextColors.GREEN, " to redeem it!"))
					.onHover(TextActions.showText(Text.of(TextColors.AQUA, "/faucet")))
					.onClick(TextActions.runCommand("/faucet"))
					.build());
			})
			.async()
			.delay(1250, TimeUnit.MILLISECONDS)
			.name("KristPay - Faucet notifications")
			.submit(KristPay.INSTANCE);
	}
}
