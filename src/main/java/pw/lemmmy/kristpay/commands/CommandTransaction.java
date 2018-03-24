package pw.lemmmy.kristpay.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;
import pw.lemmmy.kristpay.KristPay;

import static org.spongepowered.api.command.args.GenericArguments.*;

public class CommandTransaction implements CommandExecutor {
	public static final CommandSpec SPEC = CommandSpec.builder()
		.description(Text.of("Gets information on a transaction."))
		.permission("kristpay.command.transaction.base")
		.arguments(integer(Text.of("id")))
		.executor(new CommandTransaction())
		.build();
	
	private static final EconomyService ECONOMY_SERVICE = KristPay.INSTANCE.getEconomyService();
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		boolean allowOthers = src.hasPermission("kristpay.command.transaction.others");
		
		return CommandResult.empty();
	}
}
