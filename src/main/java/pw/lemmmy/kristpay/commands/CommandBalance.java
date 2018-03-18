package pw.lemmmy.kristpay.commands;

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

import java.util.Optional;
import java.util.UUID;

public class CommandBalance implements CommandExecutor {
	public static final CommandSpec SPEC = CommandSpec.builder()
		.description(Text.of("Check your Krist wallet balance."))
		.permission("kristpay.command.balance")
		.executor(new CommandBalance())
		.build();
	
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if (!(src instanceof Player)) {
			src.sendMessage(Text.of(TextColors.RED, "This command can only be used by players."));
			return CommandResult.empty();
		}
		
		Player player = (Player) src;
		EconomyService economy = KristPay.INSTANCE.getEconomyService();
		UUID uuid = player.getUniqueId();
		
		Optional<UniqueAccount> opt = economy.getOrCreateAccount(uuid);
		
		if (!opt.isPresent()) {
			src.sendMessage(Text.of(TextColors.RED, "Failed to find your account."));
			return CommandResult.empty();
		}
		
		UniqueAccount account = opt.get();
		int balance = account.getBalance(economy.getDefaultCurrency()).intValue();
		
		src.sendMessage(
			Text.builder("Your balance: ").color(TextColors.GREEN)
				.append(CommandHelpers.formatKrist(balance))
				.append(Text.of(TextColors.GREEN, "."))
				.build()
		);
		
		return CommandResult.builder()
			.queryResult(balance)
			.build();
	}
}
