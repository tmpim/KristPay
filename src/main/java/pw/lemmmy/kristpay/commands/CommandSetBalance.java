package pw.lemmmy.kristpay.commands;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import pw.lemmmy.kristpay.KristPay;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.spongepowered.api.command.args.GenericArguments.integer;
import static org.spongepowered.api.command.args.GenericArguments.userOrSource;

public class CommandSetBalance implements CommandExecutor {
	public static final CommandSpec SPEC = CommandSpec.builder()
		.description(Text.of("Sets a user's balance."))
		.permission("kristpay.command.setbalance.base")
		.arguments(
			userOrSource(Text.of("user")),
			integer(Text.of("balance"))
		)
		.executor(new CommandSetBalance())
		.build();
	
	private static final EconomyService ECONOMY_SERVICE = KristPay.INSTANCE.getEconomyService();
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		Optional<User> targetOptional = args.getOne("user");
		Optional<Integer> balanceOptional = args.getOne("balance");
		
		if (!targetOptional.isPresent()) {
			src.sendMessage(Text.of(TextColors.RED, "Must specify a valid user."));
			return CommandResult.empty();
		}
		
		if (!balanceOptional.isPresent()) {
			src.sendMessage(Text.of(TextColors.RED, "Must specify a valid balance."));
			return CommandResult.empty();
		}
		
		User target = targetOptional.get();
		UUID targetUUID = target.getUniqueId();
		Optional<UniqueAccount> targetAccountOpt = ECONOMY_SERVICE.getOrCreateAccount(targetUUID);
		
		if (!targetAccountOpt.isPresent()) {
			src.sendMessage(Text.of(TextColors.RED, "Failed to find that account."));
			return CommandResult.empty();
		}
		
		UniqueAccount targetAccount = targetAccountOpt.get();
		
		int balance = balanceOptional.get();
		
		if (balance < 0) {
			src.sendMessage(Text.of(TextColors.RED, "Balance must be positive."));
			return CommandResult.empty();
		}
		
		TransactionResult result = targetAccount.setBalance(
			KristPay.INSTANCE.getCurrency(),
			BigDecimal.valueOf(balance),
			Sponge.getCauseStackManager().getCurrentCause()
		);
		
		switch (result.getResult()) {
			case SUCCESS:
				src.sendMessage(
					Text.builder()
						.append(Text.of(TextColors.DARK_GREEN, "Success! "))
						.append(Text.of(TextColors.GREEN, "Set balance of player "))
						.append(Text.of(TextColors.YELLOW, target.getName()))
						.append(Text.of(TextColors.GREEN, " to "))
						.append(CommandHelpers.formatKrist(balance))
						.append(Text.of(TextColors.GREEN, "."))
						.build()
				);
				
				return CommandResult.queryResult(balance);
			default:
				src.sendMessage(Text.of(TextColors.RED, "Could not set the user's balance."));
				return CommandResult.empty();
		}
	}
}
