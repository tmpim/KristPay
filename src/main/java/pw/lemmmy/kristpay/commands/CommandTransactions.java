package pw.lemmmy.kristpay.commands;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandMessageFormatting;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.database.TransactionLogEntry;
import pw.lemmmy.kristpay.database.TransactionType;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.spongepowered.api.command.args.GenericArguments.*;

public class CommandTransactions implements CommandExecutor {
	public static final CommandSpec SPEC = CommandSpec.builder()
		.description(Text.of("Lists transactions involving a user."))
		.permission("kristpay.command.transactions.base")
		.arguments(
			optional(requiringPermission(flags().flag("g").buildWith(none()), "kristpay.command.transactions.others")),
			optional(requiringPermission(user(Text.of("user")), "kristpay.command.transactions.others"))
		)
		.executor(new CommandTransactions())
		.build();
	
	private static final EconomyService ECONOMY_SERVICE = KristPay.INSTANCE.getEconomyService();
	
	private static final SimpleDateFormat timeFormat = new SimpleDateFormat("MM-dd HH:mm");
	private static final SimpleDateFormat largeTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) {
		Task.builder()
			.execute(() -> {
				try {
					handleCommand(src, args);
				} catch (CommandException e) {
					Text eText = e.getText();
					if (eText != null) src.sendMessage(CommandMessageFormatting.error(eText));
				}
			})
			.async()
			.name("KristPay - /transactions command")
			.submit(KristPay.INSTANCE);
		
		return CommandResult.success();
	}
	
	private void handleCommand(CommandSource src, CommandContext args) throws CommandException {
		List<TransactionLogEntry> entries;
		
		if (args.hasAny("g") && src.hasPermission("kristpay.command.transactions.others")) {
			entries = TransactionLogEntry.getAllEntries(KristPay.INSTANCE.getDatabase().getDataSource());
		} else {
			Optional<User> userArg = args.getOne("user");
			User user;
			if (userArg.isPresent() && src.hasPermission("kristpay.command.transactions.others")) {
				user = userArg.get();
			} else if (src instanceof User) {
				user = (User) src;
			} else {
				throw new CommandException(Text.of("User must be valid."));
			}
			
			UniqueAccount account = ECONOMY_SERVICE.getOrCreateAccount(user.getUniqueId())
				.orElseThrow(() -> new CommandException(Text.of("Failed to find that account.")));
			
			entries = TransactionLogEntry.getEntries(KristPay.INSTANCE.getDatabase().getDataSource(), account.getIdentifier());
		}
		
		listEntries(src, entries, src instanceof User ? (User) src : null);
	}
	
	private void listEntries(CommandSource src, List<TransactionLogEntry> entries, User self) {
		UserStorageService userStorage = Sponge.getServiceManager().provide(UserStorageService.class).get();
		
		PaginationList.Builder pagination = PaginationList.builder()
			.padding(Text.of(TextColors.GRAY, "="))
			.title(Text.builder()
				.append(Text.of(TextColors.YELLOW, entries.size()))
				.append(Text.of(TextColors.GREEN, " transactions"))
				.build());
		
		pagination.contents(entries.stream().map(entry -> {
			TextColor successColour = entry.isSuccess() ? TextColors.GREEN : TextColors.RED;
			
			AtomicReference<Text> from = new AtomicReference<>(CommandHelpers.formatAddress(
				entry.getReturnAddress() != null ? entry.getReturnAddress() : entry.getFromAddress()));
			
			if (entry.getFromAccount() != null) userStorage.get(UUID.fromString(entry.getFromAccount()))
				.ifPresent(user -> from.set(CommandHelpers.formatUser(self, user)));
			
			AtomicReference<Text> to = new AtomicReference<>(CommandHelpers.formatAddress(
				entry.getDestAddress() != null ? entry.getDestAddress() : entry.getToAddress()));
			
			if (entry.getToAccount() != null) userStorage.get(UUID.fromString(entry.getToAccount()))
				.ifPresent(user -> to.set(CommandHelpers.formatUser(self, user)));
			
			Text.Builder builder = Text.builder()
				// date
				.append(Text.builder()
					.append(Text.of(successColour, timeFormat.format(entry.getTime())))
					.onHover(TextActions.showText(Text.builder()
						.append(Text.of(TextColors.YELLOW, largeTimeFormat.format(entry.getTime())))
						.append(Text.of("\n"))
						.append(Text.of(successColour, entry.isSuccess() ? "Successful Transaction" : "Failed Transaction"))
						.build()))
					.build())
				.append(Text.of(" "))
				// type
				.append(Text.builder()
					.append(Text.of(TextStyles.BOLD, TransactionType.getShortTextOf(entry.getType())))
					.onHover(TextActions.showText(TransactionType.getLongTextOf(entry.getType())))
					.build())
				.append(Text.of(" "));
			
			switch (TransactionType.valueOf(entry.getType().toUpperCase())) {
				case FAUCET:
					builder
						.append(Text.of(TextColors.YELLOW, "Redeemed "))
						.append(CommandHelpers.formatKrist(entry.getAmount(), successColour))
						.append(Text.of(TextColors.YELLOW, " via faucet"));
					break;
				case DEPOSIT:
				case WITHDRAW:
				case TRANSFER:
					builder
						.append(from.get())
						.append(Text.of(TextColors.WHITE, " \u27a1 "))
						.append(CommandHelpers.formatKrist(entry.getAmount(), successColour))
						.append(Text.of(TextColors.WHITE, " \u27a1 "))
						.append(to.get());
					break;
			}
			
			return builder
				.onClick(TextActions.runCommand("/transaction " + entry.getId()))
				.build();
		}).collect(Collectors.toList())).sendTo(src);
	}
}
