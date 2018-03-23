package pw.lemmmy.kristpay.commands;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.economy.KristAccount;
import pw.lemmmy.kristpay.krist.MasterWallet;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.spongepowered.api.command.args.GenericArguments.*;

public class CommandPay implements CommandExecutor {
	private static final String KRIST_TRANSFER_PATTERN = "^(?:[a-f0-9]{10}|k[a-z0-9]{9}|(?:[a-z0-9-_]{1,32}@)?[a-z0-9]{1,64}\\.kst)$";
	
	private static final CommandElement TARGET_ELEMENT = firstParsing(
		user(Text.of("user")),
		string(Text.of("address"))
	);
	
	private static final CommandElement AMOUNT_ELEMENT = integer(Text.of("amount"));
	
	public static final CommandSpec SPEC = CommandSpec.builder()
		.description(Text.of("Sends Krist to another user or address."))
		.permission("kristpay.command.pay.base")
		.arguments(
			flags().flag("k").buildWith(none()),
			TARGET_ELEMENT,
			AMOUNT_ELEMENT
		)
		.executor(new CommandPay())
		.build();
	
	private static final EconomyService ECONOMY_SERVICE = KristPay.INSTANCE.getEconomyService();
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if (!(src instanceof User)) {
			src.sendMessage(Text.of(TextColors.RED, "Must be ran by a user."));
			return CommandResult.empty();
		}
		
		User owner = (User) src;
		UUID ownerUUID = owner.getUniqueId();
		Optional<UniqueAccount> ownerAccountOpt = ECONOMY_SERVICE.getOrCreateAccount(ownerUUID);
		
		if (!ownerAccountOpt.isPresent()) {
			src.sendMessage(Text.of(TextColors.RED, "Failed to find that account."));
			return CommandResult.empty();
		}
		
		UniqueAccount ownerAccount = ownerAccountOpt.get();
		
		if (!args.<Integer>getOne("amount").isPresent()) {
			src.sendMessage(Text.of(TextColors.RED, "Must specify a valid amount to pay."));
			return CommandResult.empty();
		}
		
		int amount = args.<Integer>getOne("amount").get();
		
		if (amount <= 0) {
			src.sendMessage(Text.of(TextColors.RED, "Amount must be positive."));
			return CommandResult.empty();
		}
		
		if (args.hasAny("user") && !args.hasAny("k")) {
			if (!args.<User>getOne("user").isPresent()) {
				src.sendMessage(Text.of(TextColors.RED, "Must specify a valid user or address to pay to."));
				return CommandResult.empty();
			}
			
			User target = args.<User>getOne("user").get();
			UUID targetUUID = target.getUniqueId();
			Optional<UniqueAccount> targetAccountOpt = ECONOMY_SERVICE.getOrCreateAccount(targetUUID);
			
			if (!targetAccountOpt.isPresent() || !(targetAccountOpt.get() instanceof KristAccount)) {
				src.sendMessage(Text.of(TextColors.RED, "Failed to find the target user's account."));
				return CommandResult.empty();
			}
			
			KristAccount targetAccount = (KristAccount) targetAccountOpt.get();
			
			if (target.getName().toLowerCase().matches(KRIST_TRANSFER_PATTERN)) {
				src.sendMessage(Text.builder()
					.append(Text.of(TextColors.GOLD, "Warning: "))
					.append(Text.of(TextColors.YELLOW, "The target you specified is both a valid user and a valid " +
						"Krist address. This command prefers users by default. If you wish to transfer to the address" +
						" instead, add the "))
					.append(Text.of(TextColors.GREEN, "-k "))
					.append(Text.of(TextColors.YELLOW, "flag. "))
					.append(Text.of(TextColors.GOLD, "This warning will not be shown again.")) // TODO: lol
					.build()
				);
			}
			
			return payUser(src, owner, ownerAccount, target, targetAccount, amount);
		} else {
			if (!args.<String>getOne("address").isPresent()) {
				src.sendMessage(Text.of(TextColors.RED, "Must specify a valid user or address to pay to."));
				return CommandResult.empty();
			}
			
			String target = args.<String>getOne("address").get().toLowerCase();
			
			if (!target.matches(KRIST_TRANSFER_PATTERN)) {
				src.sendMessage(Text.of(TextColors.RED, "Must specify a valid user or address to pay to."));
				return CommandResult.empty();
			}
			
			return payAddress(src, owner, ownerAccount, target, amount);
		}
	}
	
	private CommandResult payUser(CommandSource src, User owner, UniqueAccount ownerAccount, User target,
								  UniqueAccount targetAccount, int amount) {
		TransferResult result = ownerAccount.transfer(
			targetAccount,
			KristPay.INSTANCE.getCurrency(),
			BigDecimal.valueOf(amount),
			Sponge.getCauseStackManager().getCurrentCause()
		);
		
		KristPay.INSTANCE.getDatabase().addTransactionLogEntry(
			result,
			ownerAccount.getIdentifier(), targetAccount.getIdentifier(),
			null, null,
			amount,
			null, null, null,
			-1
		);
		
		switch (result.getResult()) {
			case SUCCESS:
				src.sendMessage(
					Text.builder()
						.append(Text.of(TextColors.DARK_GREEN, "Success! "))
						.append(Text.of(TextColors.GREEN, "Transferred "))
						.append(CommandHelpers.formatKrist(result.getAmount()))
						.append(Text.of(TextColors.GREEN, " to player "))
						.append(Text.of(TextColors.YELLOW, target.getName()))
						.append(Text.of(TextColors.GREEN, "."))
						.build()
				);
				
				target.getPlayer().ifPresent(player -> player.sendMessage(
					Text.builder()
						.append(Text.of(TextColors.GREEN, "You have received "))
						.append(CommandHelpers.formatKrist(result.getAmount()))
						.append(Text.of(TextColors.GREEN, " from player "))
						.append(Text.of(TextColors.YELLOW, owner.getName()))
						.append(Text.of(TextColors.GREEN, "."))
						.build()
				));
				
				return CommandResult.success();
			case ACCOUNT_NO_FUNDS:
				src.sendMessage(Text.of(TextColors.RED, "You don't have enough funds for this transaction."));
				return CommandResult.empty();
			default:
				src.sendMessage(
					Text.builder()
						.append(Text.of(TextColors.RED, "Transaction failed ("))
						.append(Text.of(TextColors.DARK_RED, result.getResult().toString()))
						.append(Text.of(TextColors.RED, ")."))
						.build()
				);
				return CommandResult.empty();
		}
	}
	
	private CommandResult payAddress(CommandSource src, User owner, UniqueAccount ownerAccount, String target, int amount) {
		TransactionResult result = ownerAccount.withdraw(
			KristPay.INSTANCE.getCurrency(),
			BigDecimal.valueOf(amount),
			Sponge.getCauseStackManager().getCurrentCause()
		);
		
		switch (result.getResult()) {
			case SUCCESS:
				MasterWallet masterWallet = KristPay.INSTANCE.getMasterWallet();
				
				StringBuilder metadata = new StringBuilder()
					// return address
					.append("return=")
					.append(owner.getName().toLowerCase())
					.append("@")
					.append(KristPay.INSTANCE.getConfig().getMasterWallet().getPrimaryDepositName())
					// username
					.append(";")
					.append("username=")
					.append(owner.getName());
				
				masterWallet.transfer(target, amount, metadata.toString(), (success, transaction) -> {
					if (!success) {
						src.sendMessage(Text.of(TextColors.RED, "Transaction failed. Try again later, or contact an admin."));
						
						// refund their transaction
						ownerAccount.deposit(
							KristPay.INSTANCE.getCurrency(),
							BigDecimal.valueOf(amount),
							Sponge.getCauseStackManager().getCurrentCause()
						);
					} else {
						Text.Builder builder = Text.builder()
							.append(Text.of(TextColors.DARK_GREEN, "Success! "))
							.append(Text.of(TextColors.GREEN, "Transferred "))
							.append(CommandHelpers.formatKrist(result.getAmount()))
							.append(Text.of(TextColors.GREEN, " to address "))
							.append(CommandHelpers.formatAddress(target));
						
						if (!target.equalsIgnoreCase(transaction.getTo())) {
							builder.append(Text.of(TextColors.GREEN, " ("))
								.append(CommandHelpers.formatAddress(transaction.getTo()))
								.append(Text.of(TextColors.GREEN, ")"));
						}
						
						src.sendMessage(builder.append(Text.of(TextColors.GREEN, ".")).build());
					}
				});
				
				return CommandResult.success();
			case ACCOUNT_NO_FUNDS:
				src.sendMessage(Text.of(TextColors.RED, "You don't have enough funds for this transaction."));
				return CommandResult.empty();
			default:
				src.sendMessage(
					Text.builder()
						.append(Text.of(TextColors.RED, "Transaction failed ("))
						.append(Text.of(TextColors.DARK_RED, result.getResult().toString()))
						.append(Text.of(TextColors.RED, ")."))
						.build()
				);
				return CommandResult.empty();
		}
	}
}