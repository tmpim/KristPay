package pw.lemmmy.kristpay.krist;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.commands.CommandHelpers;
import pw.lemmmy.kristpay.commands.CommandPay;
import pw.lemmmy.kristpay.database.AccountDatabase;
import pw.lemmmy.kristpay.database.TransactionLogEntry;
import pw.lemmmy.kristpay.database.TransactionType;
import pw.lemmmy.kristpay.economy.KristAccount;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DepositManager {
	private static final EconomyService ECONOMY_SERVICE = KristPay.INSTANCE.getEconomyService();
	
	private AccountDatabase accountDatabase;
	private MasterWallet masterWallet;
	
	private UserStorageService userStorage;
	
	public DepositManager(AccountDatabase accountDatabase, MasterWallet masterWallet) {
		this.accountDatabase = accountDatabase;
		this.masterWallet = masterWallet;
		
		userStorage = Sponge.getServiceManager().provide(UserStorageService.class).get();
	}
	
	private Optional<KristAccount> findAccountByAddress(String address) {
		return accountDatabase.getAccounts().values().stream()
			.filter(kristAccount -> kristAccount.getDepositWallet().getAddress().equalsIgnoreCase(address))
			.findFirst();
	}
	
	private void refundDeposit(String refundAddress, int depositAmount, String refundReason) {
		String primaryDepositName = KristPay.INSTANCE.getConfig().getMasterWallet().getPrimaryDepositName();
		
		refundReason += " (send to `username@" + primaryDepositName + "` to deposit, or set `donate=true` to donate)";
		
		masterWallet.transfer(refundAddress, depositAmount, "error=" + refundReason);
	}
	
	private void handleDeposit(KristAccount account,
							   KristTransaction fromTx,
							   Map<String, String> meta,
							   int depositAmount) {
		Wallet depositWallet = account.getDepositWallet();
		if (depositWallet == null) return;
		
		String fromAddress = fromTx != null ? fromTx.getFrom() : null;
		
		depositWallet.transfer(masterWallet.getAddress(), depositAmount, null).handle((tx, ex) -> {
			account.deposit(
				KristPay.INSTANCE.getCurrency(),
				BigDecimal.valueOf(depositAmount),
				Cause.of(EventContext.empty(), this)
			);
			
			if (KristPay.INSTANCE.getPrometheusManager() != null)
				KristPay.INSTANCE.getPrometheusManager().getTransactionsReporter().incrementDeposits(depositAmount);
			
			new TransactionLogEntry()
				.setSuccess(true)
				.setType(TransactionType.DEPOSIT)
				.setToAccount(account)
				.setAmount(depositAmount)
				.setMeta(meta)
				.setTransaction(fromTx)
				.addAsync();
			
			Optional<Player> playerOptional = Sponge.getServer().getPlayer(UUID.fromString(account.getOwner()));
			
			// notify player of their deposit if they are online
			if (playerOptional.isPresent()) {
				Player player = playerOptional.get();
				if (!player.isOnline()) return tx;
				
				Text.Builder builder = Text.builder()
					.append(CommandHelpers.formatKrist(depositAmount, true))
					.append(Text.of(TextColors.GREEN, " was deposited into your account"));
				
				if (fromAddress != null) {
					builder.append(Text.of(TextColors.GREEN, " from "));
					
					if (meta != null && meta.containsKey("return")) {
						builder.append(CommandHelpers.formatAddress(meta.get("return")))
							.append(Text.of(TextColors.GREEN, " ("))
							.append(CommandHelpers.formatAddress(fromAddress))
							.append(Text.of(TextColors.GREEN, ")"));
					} else {
						builder.append(CommandHelpers.formatAddress(fromAddress));
					}
				}
				
				builder.append(Text.of(TextColors.GREEN, "."));
				
				if (meta != null && meta.containsKey("error")) {
					Text error = TextSerializers.FORMATTING_CODE.deserialize(meta.get("error").replaceAll("&r", "&r&c"));
					
					builder.append(Text.of("\n"))
						.append(Text.of(TextColors.DARK_RED, "Error: "))
						.append(Text.of(TextColors.RED, error));
				}
				
				if (meta != null && meta.containsKey("message")) {
					Text message = TextSerializers.FORMATTING_CODE.deserialize(meta.get("message"));
					
					builder.append(Text.of("\n"))
						.append(Text.of(TextColors.DARK_GREEN, "Message: "))
						.append(message);
				}
				
				player.sendMessage(builder.build());
			} else {
				account.setUnseenDeposit(account.getUnseenDeposit() + depositAmount);
				KristPay.INSTANCE.getAccountDatabase().save();
			}
			
			return tx;
		});
	}
	
	public void handleTransaction(KristTransaction transaction) {
		String address = transaction.getTo();
		
		if (address.equalsIgnoreCase(masterWallet.getAddress())
			&& transaction.getMetadata() != null && !transaction.getMetadata().isEmpty()) {
			handleNameTransaction(transaction);
		} else {
			findAccountByAddress(address).ifPresent(account -> handleDeposit(account, transaction, null, transaction.getValue()));
		}
		
		masterWallet.syncWithNode(cb -> {});
	}
	
	private void handleNameTransaction(KristTransaction transaction) {
		String refundAddress = transaction.getFrom();
		int amount = transaction.getValue();
		
		Optional<Map<String, String>> commonMetaOpt = KristAPI.parseCommonMeta(transaction.getMetadata());
		if (!commonMetaOpt.isPresent()) {
			refundDeposit(refundAddress, amount, "Could not parse CommonMeta");
			return;
		}
		Map<String, String> commonMeta = commonMetaOpt.get();

		// If a return address is specified, send back to that. This should stop infinite transaction loops occuring
		// when someone uses KristPay on a different server to send Krist to a username which doesn't exist.
		if (commonMeta.containsKey("return")) {
			if (commonMeta.get("return").matches(CommandPay.KRIST_TRANSFER_PATTERN)) {
				refundAddress = commonMeta.get("return");
			} else {
				refundDeposit(refundAddress, amount, "Invalid return address");
				return;
			}
		}


		// Donation
		if (commonMeta.containsKey("donate") && commonMeta.get("donate").trim().equalsIgnoreCase("true")) {
			// TODO: notify users with certain permission?
			return;
		}
		
		if (!commonMeta.containsKey("metaname")) {
			if (commonMeta.containsKey("error")) return; // We can't find a valid route
			refundDeposit(refundAddress, amount, "Username not specified");
			return;
		}
		String metaname = commonMeta.get("metaname");
		
		Optional<User> userOpt = userStorage.get(metaname); // case insensitive
		if (!userOpt.isPresent()) {
			if (commonMeta.containsKey("error")) return; // We can't find a valid route
			refundDeposit(refundAddress, amount, "Could not find user");
			return;
		}
		User user = userOpt.get();
		
		Optional<UniqueAccount> accountOpt = ECONOMY_SERVICE.getOrCreateAccount(user.getUniqueId());
		if (!accountOpt.isPresent()) {
			if (commonMeta.containsKey("error")) return; // We can't find a valid route
			refundDeposit(refundAddress, amount, "Could not find user");
			return;
		}
		UniqueAccount account = accountOpt.get();
		if (!(account instanceof KristAccount)) {
			if (commonMeta.containsKey("error")) return; // We can't find a valid route
			refundDeposit(refundAddress, amount, "Could not find user");
			return;
		}
		
		handleDeposit((KristAccount) account, transaction, commonMeta, amount);
	}
	
	public void walletSynced(KristAccount account) {
		Wallet depositWallet = account.getDepositWallet();
		if (depositWallet == null) return;
		
		int depositAmount = depositWallet.getBalance();
		if (depositAmount <= 0) return;
		
		handleDeposit(account, null, null, depositAmount);
	}
}
