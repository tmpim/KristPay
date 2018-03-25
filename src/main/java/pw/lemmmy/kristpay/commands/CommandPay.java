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
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.database.TransactionType;
import pw.lemmmy.kristpay.database.TransactionLogEntry;
import pw.lemmmy.kristpay.economy.KristAccount;
import pw.lemmmy.kristpay.krist.MasterWallet;

import java.math.BigDecimal;

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
		if (!(src instanceof User)) throw new CommandException(Text.of("Must be ran by a user."));
		User owner = (User) src;
		UniqueAccount ownerAccount = ECONOMY_SERVICE.getOrCreateAccount(owner.getUniqueId())
			.orElseThrow(() -> new CommandException(Text.of("Failed to find that account.")));
		
		int amount = args.<Integer>getOne("amount")
			.orElseThrow(() ->  new CommandException(Text.of("Must specify a valid amount to pay.")));
		if (amount <= 0) throw new CommandException(Text.of("Amount must be positive."));
		
		if (args.hasAny("user") && !args.hasAny("k")) {
			User target = args.<User>getOne("user")
				.orElseThrow(() -> new CommandException(Text.of("Must specify a valid user or address to pay to.")));
			KristAccount targetAccount = (KristAccount) ECONOMY_SERVICE.getOrCreateAccount(target.getUniqueId())
				.orElseThrow(() -> new CommandException(Text.of("Failed to find the target user's account.")));
			
			if (target.getName().toLowerCase().matches(KRIST_TRANSFER_PATTERN)) {
				src.sendMessage(Text.builder()
					.append(Text.of(TextColors.GOLD, "Warning: "))
					.append(Text.of(TextColors.YELLOW, "The target you specified is both a valid user and a valid " +
						"Krist address. This command prefers users by default. If you wish to transfer to the address" +
						" instead, add the "))
					.append(Text.of(TextColors.GREEN, "-k "))
					.append(Text.of(TextColors.YELLOW, "flag. "))
					.build()
				);
			}
			
			return payUser(src, owner, ownerAccount, target, targetAccount, amount);
		} else {
			String target = args.<String>getOne("address")
				.orElseThrow(() -> new CommandException(Text.of("Must specify a valid user or address to pay to.")))
				.toLowerCase();
			
			if (!target.matches(KRIST_TRANSFER_PATTERN))
				throw new CommandException(Text.of("Must specify a valid user or address to pay to."));
			
			return payAddress(src, owner, ownerAccount, target, amount);
		}
	}
	
	private CommandResult payUser(CommandSource src, User owner, UniqueAccount ownerAccount, User target,
								  UniqueAccount targetAccount, int amount) throws CommandException {
		TransferResult result = ownerAccount.transfer(
			targetAccount,
			KristPay.INSTANCE.getCurrency(),
			BigDecimal.valueOf(amount),
			Sponge.getCauseStackManager().getCurrentCause()
		);
		
		new TransactionLogEntry()
			.setTransferResult(result)
			.setFromAccount(ownerAccount)
			.setToAccount(targetAccount)
			.setAmount(amount)
			.addAsync();
		
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
				throw new CommandException(Text.of("You don't have enough funds for this transaction."));
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
	
	private CommandResult payAddress(CommandSource src, User owner, UniqueAccount ownerAccount, String target,
									 int amount) throws CommandException {
		TransactionResult result = ownerAccount.withdraw(
			KristPay.INSTANCE.getCurrency(),
			BigDecimal.valueOf(amount),
			Sponge.getCauseStackManager().getCurrentCause()
		);
		
		if (result.getResult() != ResultType.SUCCESS) {
			new TransactionLogEntry()
				.setTransactionResult(result)
				.setFromAccount(ownerAccount)
				.setDestAddress(target)
				.setAmount(amount)
				.addAsync();
		}
		
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
					new TransactionLogEntry()
						.setSuccess(success)
						.setError(success ? null : "Transaction failed.")
						.setType(TransactionType.WITHDRAW)
						.setFromAccount(ownerAccount)
						.setDestAddress(target)
						.setTransaction(transaction)
						.setAmount(amount)
						.addAsync();
					
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
				throw new CommandException(Text.of("You don't have enough funds for this transaction."));
			default:
				throw new CommandException(
					Text.builder()
						.append(Text.of(TextColors.RED, "Transaction failed ("))
						.append(Text.of(TextColors.DARK_RED, result.getResult().toString()))
						.append(Text.of(TextColors.RED, ")."))
						.build()
				);
		}
	}
}