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
		int balance = args.<Integer>getOne("balance")
			.orElseThrow(() -> new CommandException(Text.of("Must specify a valid balance.")));
		if (balance < 0) throw new CommandException(Text.of("Balance must be positive."));
		
		User target = args.<User>getOne("user")
			.orElseThrow(() -> new CommandException(Text.of("Must specify a valid user.")));
		UniqueAccount targetAccount = ECONOMY_SERVICE.getOrCreateAccount(target.getUniqueId())
			.orElseThrow(() -> new CommandException(Text.of("Failed to find that account.")));
		
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
						.append(CommandHelpers.formatKrist(balance, true))
						.append(Text.of(TextColors.GREEN, "."))
						.build()
				);
				
				return CommandResult.queryResult(balance);
			default:
				throw new CommandException(Text.of("Could not set the user's balance."));
		}
	}
}
