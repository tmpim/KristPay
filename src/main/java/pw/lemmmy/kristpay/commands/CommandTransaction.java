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
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.database.TransactionLogEntry;

import java.util.Optional;

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
		
		int id = args.<Integer>getOne("id")
			.orElseThrow(() -> new CommandException(Text.of("Must specify a valid transaction ID.")));
		
		TransactionLogEntry entry =  TransactionLogEntry.getEntry(KristPay.INSTANCE.getDatabase().getDataSource(), id)
			.orElseThrow(() -> new CommandException(Text.of("Could not find that transaction.")));
		
		Optional<User> user = src instanceof User ? Optional.of((User) src) : Optional.empty();
		Optional<UniqueAccount> accountOpt = user.flatMap(u -> ECONOMY_SERVICE.getOrCreateAccount(u.getUniqueId()));
		
		if (!allowOthers) {
			if (!accountOpt.isPresent()) throw new CommandException(Text.of("Failed to find that account."));
			
			UniqueAccount account = accountOpt.get();
			String identifier = account.getUniqueId().toString();
			
			if (!identifier.equals(entry.getFromAccount()) && !identifier.equals(entry.getToAccount()))
				throw new CommandException(Text.of("You don't have permission to view this transaction."));
		}
		
		return CommandResult.empty();
	}
}
