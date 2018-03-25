package pw.lemmmy.kristpay.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import pw.lemmmy.kristpay.KristPay;

import java.util.Optional;
import java.util.UUID;

import static org.spongepowered.api.command.args.GenericArguments.*;

public class CommandBalance implements CommandExecutor {
	public static final CommandSpec SPEC = CommandSpec.builder()
		.description(Text.of("Check your Krist wallet balance."))
		.permission("kristpay.command.balance.base")
		.arguments(optional(requiringPermission(user(Text.of("user")), "kristpay.command.balance.others")))
		.executor(new CommandBalance())
		.build();
	
	private static final EconomyService ECONOMY_SERVICE = KristPay.INSTANCE.getEconomyService();
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		Optional<User> userArg = args.getOne("user");
		User user;
		if (userArg.isPresent() && src.hasPermission("kristpay.command.balance.others")) {
			user = userArg.get();
		} else if (src instanceof User) {
			user = (User) src;
		} else {
			throw new CommandException(Text.of("User must be valid."));
		}
		
		UniqueAccount account = ECONOMY_SERVICE.getOrCreateAccount(user.getUniqueId())
			.orElseThrow(() -> new CommandException(Text.of("Failed to find that account.")));
		
		int balance = account.getBalance(KristPay.INSTANCE.getCurrency()).intValue();
		
		src.sendMessage(
			Text.builder()
				.append(Text.of(TextColors.YELLOW, user.getName()))
				.append(Text.of(TextColors.GREEN, "'s balance: "))
				.append(CommandHelpers.formatKrist(balance))
				.append(Text.of(TextColors.GREEN, "."))
				.build()
		);
		
		return CommandResult.queryResult(balance);
	}
}
