package pw.lemmmy.kristpay.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.economy.KristAccount;

import static org.spongepowered.api.command.args.GenericArguments.integer;
import static org.spongepowered.api.command.args.GenericArguments.userOrSource;

public class CommandSetWelfare implements CommandExecutor {
	public static final CommandSpec SPEC = CommandSpec.builder()
		.description(Text.of("Sets a user's welfare amount."))
		.permission("kristpay.command.setwelfare.base")
		.arguments(
			userOrSource(Text.of("user")),
			integer(Text.of("amount"))
		)
		.executor(new CommandSetWelfare())
		.build();
	
	private static final EconomyService ECONOMY_SERVICE = KristPay.INSTANCE.getEconomyService();
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		int amount = args.<Integer>getOne("amount")
			.orElseThrow(() -> new CommandException(Text.of("Must specify a valid amount.")));
		if (amount < 0) throw new CommandException(Text.of("Amount must be positive."));
		
		User target = args.<User>getOne("user")
			.orElseThrow(() -> new CommandException(Text.of("Must specify a valid user.")));
		KristAccount targetAccount = (KristAccount) ECONOMY_SERVICE.getOrCreateAccount(target.getUniqueId())
			.orElseThrow(() -> new CommandException(Text.of("Failed to find that account.")));
		
		targetAccount.setWelfareAmount(amount);
		
		src.sendMessage(
			Text.builder()
				.append(Text.of(TextColors.DARK_GREEN, "Success! "))
				.append(Text.of(TextColors.GREEN, "Set welfare amount of player "))
				.append(Text.of(TextColors.YELLOW, target.getName()))
				.append(Text.of(TextColors.GREEN, " to "))
				.append(CommandHelpers.formatKrist(amount, true))
				.append(Text.of(TextColors.GREEN, "."))
				.build()
		);
		
		return CommandResult.queryResult(amount);
	}
}
