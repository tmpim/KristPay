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
import org.spongepowered.api.text.serializer.TextSerializers;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.database.TransactionLogEntry;
import pw.lemmmy.kristpay.database.TransactionType;
import pw.lemmmy.kristpay.krist.MasterWallet;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.spongepowered.api.command.args.GenericArguments.integer;

public class CommandTransaction implements CommandExecutor {
	public static final CommandSpec SPEC = CommandSpec.builder()
		.description(Text.of("Gets information on a transaction."))
		.permission("kristpay.command.transaction.base")
		.arguments(integer(Text.of("id")))
		.executor(new CommandTransaction())
		.build();
	
	private static final EconomyService ECONOMY_SERVICE = KristPay.INSTANCE.getEconomyService();
	
	private static final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		UserStorageService userStorage = Sponge.getServiceManager().provide(UserStorageService.class).get();
		MasterWallet masterWallet = KristPay.INSTANCE.getMasterWallet();
		boolean allowOthers = src.hasPermission("kristpay.command.transaction.others");
		
		int id = args.<Integer>getOne("id")
			.orElseThrow(() -> new CommandException(Text.of("Must specify a valid transaction ID.")));
		
		Task.builder()
			.execute(() -> {
				try {
					handleCommand(src, userStorage, masterWallet, allowOthers, id);
				} catch (CommandException e) {
					Text eText = e.getText();
					if (eText != null) src.sendMessage(CommandMessageFormatting.error(eText));
				}
			})
			.async()
			.name("KristPay - /transaction command")
			.submit(KristPay.INSTANCE);
		
		return CommandResult.success();
	}
	
	private void handleCommand(CommandSource src,
							   UserStorageService userStorage,
							   MasterWallet masterWallet,
							   boolean allowOthers, int id) throws CommandException {
		TransactionLogEntry entry = TransactionLogEntry.getEntry(KristPay.INSTANCE.getDatabase().getDataSource(), id)
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
		
		TextColor successColour = entry.isSuccess() ? TextColors.GREEN : TextColors.RED;
		
		PaginationList.Builder pagination = PaginationList.builder()
			.padding(Text.of(TextColors.GRAY, "="))
			.title(Text.builder()
				.append(Text.of(successColour, "Transaction "))
				.append(Text.of(TextColors.YELLOW, entry.getId()))
				.onHover(TextActions
					.showText(Text.of(successColour, entry.isSuccess() ? "Successful Transaction" : "Failed Transaction")))
				.build());
		
		List<Text> contents = new ArrayList<>();
		
		Text.Builder typeBuilder = Text.builder()
			.append(Text.of(TextColors.GREEN, "Type: "))
			.append(Text.of(TextStyles.BOLD, TransactionType.getLongTextOf(entry.getType())))
			.append(Text.of(" ("))
			.append(Text.of(successColour, entry.isSuccess() ? "success" : "failed"))
			.append(Text.of(")"));
		contents.add(typeBuilder.build());
		
		contents.add(Text.builder()
			.append(Text.of(TextColors.GREEN, "Date: "))
			.append(Text.of(TextColors.YELLOW, timeFormat.format(entry.getTime())))
			.build());
		
		Text.Builder amountBuilder = Text.builder()
			.append(Text.of(TextColors.GREEN, "Amount: "))
			.append(CommandHelpers.formatKrist(entry.getAmount(), true));
		if (entry.getKristTXID() != -1) amountBuilder.append(Text.builder()
			.append(Text.of(" ("))
			.append(Text.of(TextColors.GRAY, "Krist TXID: "))
			.append(Text.of(TextColors.YELLOW, entry.getKristTXID()))
			.append(Text.of(")"))
			.onHover(TextActions.showText(Text.of(TextColors.AQUA, "https://krist.club/transactions/" + entry.getKristTXID())))
			.onClick(TextActions.openUrl(getKristTransactionURL(entry)))
			.build());
		contents.add(amountBuilder.build());
		
		if (entry.getFromAccount() != null)
			userStorage.get(UUID.fromString(entry.getFromAccount())).ifPresent(fromUser -> contents.add(Text.builder()
				.append(Text.of(TextColors.GREEN, "From: "))
				.append(CommandHelpers.formatUser(user.orElse(null), fromUser))
				.build()));
		
		if (entry.getReturnAddress() != null && entry.getFromAddress() != null
			&& !entry.getReturnAddress().equalsIgnoreCase(entry.getFromAddress())
			&& !entry.getFromAddress().equalsIgnoreCase(masterWallet.getAddress())) {
			contents.add(Text.builder()
				.append(Text.of(TextColors.GREEN, "From: "))
				.append(CommandHelpers.formatAddress(entry.getReturnAddress()))
				.append(Text.of(" ("))
				.append(CommandHelpers.formatAddress(entry.getFromAddress()))
				.append(Text.of(")"))
				.build());
		} else if (entry.getReturnAddress() != null) {
			contents.add(Text.builder()
				.append(Text.of(TextColors.GREEN, "From: "))
				.append(CommandHelpers.formatAddress(entry.getReturnAddress()))
				.build());
		} else if (entry.getFromAddress() != null
			&& !entry.getFromAddress().equalsIgnoreCase(masterWallet.getAddress())) {
			contents.add(Text.builder()
				.append(Text.of(TextColors.GREEN, "From: "))
				.append(CommandHelpers.formatAddress(entry.getFromAddress()))
				.build());
		}
		
		if (entry.getToAccount() != null)
			userStorage.get(UUID.fromString(entry.getToAccount())).ifPresent(fromUser -> contents.add(Text.builder()
				.append(Text.of(TextColors.GREEN, "To: "))
				.append(CommandHelpers.formatUser(user.orElse(null), fromUser))
				.build()));
		
		if (entry.getDestAddress() != null && entry.getToAddress() != null
			&& !entry.getDestAddress().equalsIgnoreCase(entry.getToAddress())
			&& !entry.getToAddress().equalsIgnoreCase(masterWallet.getAddress())) {
			contents.add(Text.builder()
				.append(Text.of(TextColors.GREEN, "To: "))
				.append(CommandHelpers.formatAddress(entry.getDestAddress()))
				.append(Text.of(" ("))
				.append(CommandHelpers.formatAddress(entry.getToAddress()))
				.append(Text.of(")"))
				.build());
		} else if (entry.getDestAddress() != null) {
			contents.add(Text.builder()
				.append(Text.of(TextColors.GREEN, "To: "))
				.append(CommandHelpers.formatAddress(entry.getDestAddress()))
				.build());
		} else if (entry.getToAddress() != null
			&& !entry.getToAddress().equalsIgnoreCase(masterWallet.getAddress())) {
			contents.add(Text.builder()
				.append(Text.of(TextColors.GREEN, "To: "))
				.append(CommandHelpers.formatAddress(entry.getToAddress()))
				.build());
		}
		
		if (entry.getError() != null) contents.add(Text.builder()
			.append(Text.of(TextColors.DARK_RED, "Error: "))
			.append(Text.of(TextColors.RED, TextSerializers.FORMATTING_CODE.deserialize(entry.getError())))
			.build());
		
		if (entry.getMetaMessage() != null) contents.add(Text.builder()
			.append(Text.of(TextColors.DARK_GREEN, "Message: "))
			.append(Text.of(TextSerializers.FORMATTING_CODE.deserialize(entry.getMetaMessage())))
			.build());
		
		if (entry.getMetaError() != null) contents.add(Text.builder()
			.append(Text.of(TextColors.DARK_RED, "Error: "))
			.append(Text.of(TextColors.RED, TextSerializers.FORMATTING_CODE.deserialize(entry.getMetaError().replaceAll("&r", "&r&c"))))
			.build());
		
		pagination.contents(contents).sendTo(src);
	}
	
	private URL getKristTransactionURL(TransactionLogEntry entry) throws CommandException {
		try {
			return new URL("https://krist.club/transactions/" + entry.getKristTXID());
		} catch (MalformedURLException e) {
			KristPay.INSTANCE.getLogger().error("Ughrrr", e);
		}
		
		throw new CommandException(Text.of("Java sucks"));
	}
}
