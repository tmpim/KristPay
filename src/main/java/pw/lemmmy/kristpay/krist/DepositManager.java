package pw.lemmmy.kristpay.krist;

import org.spongepowered.api.Sponge;
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
import pw.lemmmy.kristpay.database.AccountDatabase;
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
		
		masterWallet.transfer(refundAddress, depositAmount, "error=" + refundReason, (success, transaction) -> {});
	}
	
	private void handleDeposit(KristAccount account,
							   KristTransaction fromTx,
							   Map<String, String> meta,
							   int depositAmount) {
		Wallet depositWallet = account.getDepositWallet();
		if (depositWallet == null) return;
		
		String fromAddress = fromTx != null ? fromTx.getFrom() : null;
		
		depositWallet.transfer(masterWallet.getAddress(), depositAmount, null, (success, depositTx) -> {
			account.deposit(
				KristPay.INSTANCE.getCurrency(),
				BigDecimal.valueOf(depositAmount),
				Cause.of(EventContext.empty(), this)
			);
			
			KristPay.INSTANCE.getDatabase().addTransactionLogEntry(
				true, null,
				"deposit", null, account.getIdentifier(),
				fromAddress, null,
				depositAmount,
				meta.get("return"),
				meta.get("message"),
				meta.get("error"),
				fromTx != null ? fromTx.getId() : -1
			);
			
			// notify player of their deposit if they are online
			Sponge.getServer().getPlayer(UUID.fromString(account.getOwner())).ifPresent(player -> {
				if (!player.isOnline()) return; // TODO: queue deposit message until next time player is on
				
				Text.Builder builder = Text.builder()
					.append(CommandHelpers.formatKrist(depositAmount))
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
					Text error = TextSerializers.FORMATTING_CODE.deserialize(meta.get("error"));
					
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
			});
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
		
		masterWallet.syncWithNode(cb -> {}); // TODO: does anything need to be handled here?
	}
	
	private void handleNameTransaction(KristTransaction transaction) {
		String fromAddress = transaction.getFrom();
		int amount = transaction.getValue();
		
		Optional<Map<String, String>> commonMetaOpt = KristAPI.parseCommonMeta(transaction.getMetadata());
		
		if (!commonMetaOpt.isPresent()) {
			refundDeposit(fromAddress, amount, "Could not parse CommonMeta");
			return;
		}
		
		Map<String, String> commonMeta = commonMetaOpt.get();
		
		if (commonMeta.containsKey("donate") && commonMeta.get("donate").trim().equalsIgnoreCase("true")) {
			// TODO: notify users with certain permission?
			return;
		}
		
		if (!commonMeta.containsKey("metaname")) {
			refundDeposit(fromAddress, amount, "Username not specified");
			return;
		}
		String metaname = commonMeta.get("metaname");
		
		Optional<User> userOpt = userStorage.get(metaname); // case insensitive
		if (!userOpt.isPresent()) {
			refundDeposit(fromAddress, amount, "Could not find user");
			return;
		}
		User user = userOpt.get();
		
		Optional<UniqueAccount> accountOpt = ECONOMY_SERVICE.getOrCreateAccount(user.getUniqueId());
		if (!accountOpt.isPresent()) {
			refundDeposit(fromAddress, amount, "Could not find user");
			return;
		}
		UniqueAccount account = accountOpt.get();
		if (!(account instanceof KristAccount)) {
			refundDeposit(fromAddress, amount, "Could not find user");
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
