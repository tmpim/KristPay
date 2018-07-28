package pw.lemmmy.kristpay.commands;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.database.FaucetReward;
import pw.lemmmy.kristpay.economy.FaucetManager;

import java.net.InetSocketAddress;
import java.util.Optional;

public class CommandFaucet implements CommandExecutor {
	public static final CommandSpec SPEC = CommandSpec.builder()
		.description(Text.of("Redeem your regular faucet reward."))
		.permission("kristpay.command.faucet.base")
		.executor(new CommandFaucet())
		.build();
	
	private static final EconomyService ECONOMY_SERVICE = KristPay.INSTANCE.getEconomyService();
	
	@Override
	public synchronized CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if (!(src instanceof Player)) throw new CommandException(Text.of("Must be ran by a player."));
		Player player = (Player) src;
		InetSocketAddress ip = player.getConnection().getAddress();
		UniqueAccount account = ECONOMY_SERVICE.getOrCreateAccount(player.getUniqueId())
			.orElseThrow(() -> new CommandException(Text.of("Failed to find that account.")));
		
		Optional<FaucetReward> lastRewardOpt = FaucetReward.getLastReward(
			KristPay.INSTANCE.getDatabase().getDataSource(),
			ip, account
		);
		
		// start them on the first tier if they haven't claimed a reward before
		int nextTier = lastRewardOpt.map(FaucetManager::getNextRewardTier).orElse(0);
		
		if (nextTier <= -1) {
			FaucetReward lastReward = lastRewardOpt.get();
			long duration = lastReward.getMinimum().getTime() - System.currentTimeMillis();
			int actualNextTier = FaucetManager.getNextRewardTier(lastReward, true);
			int nextReward = FaucetManager.getRewardValue(actualNextTier);
			
			throw new CommandException(Text.builder()
				.append(Text.of("You have already redeemed your faucet reward.\n"))
				.append(Text.of("You can next redeem in: "))
				.append(Text.of(TextColors.YELLOW, DurationFormatUtils.formatDurationWords(duration, true, true)))
				.append(Text.of("\nNext reward: "))
				.append(CommandHelpers.formatKrist(nextReward))
				.build());
		} else {
			FaucetReward newReward = FaucetManager.redeem(ip, account, nextTier);
			long duration = newReward.getMinimum().getTime() - System.currentTimeMillis();
			int newNextTier = FaucetManager.getNextRewardTier(newReward, true);
			int nextReward = FaucetManager.getRewardValue(newNextTier);
			
			src.sendMessage(Text.builder()
				.append(Text.of(TextColors.DARK_GREEN, "Success! "))
				.append(Text.of(TextColors.GREEN, "Redeemed your faucet reward of "))
				.append(CommandHelpers.formatKrist(newReward.getValue()))
				.append(Text.of(TextColors.GREEN, ".\n"))
				.append(Text.of(TextColors.GREEN, "You can next redeem in: "))
				.append(Text.of(TextColors.YELLOW, DurationFormatUtils.formatDurationWords(duration, true, true)))
				.append(Text.of(TextColors.GREEN, "\nNext reward: "))
				.append(CommandHelpers.formatKrist(nextReward))
				.build());
		}
		
		return CommandResult.success();
	}
}
