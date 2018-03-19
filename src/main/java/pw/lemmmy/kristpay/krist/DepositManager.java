package pw.lemmmy.kristpay.krist;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.commands.CommandHelpers;
import pw.lemmmy.kristpay.database.Database;
import pw.lemmmy.kristpay.economy.KristAccount;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DepositManager {
	private static final EconomyService ECONOMY_SERVICE = KristPay.INSTANCE.getEconomyService();
	
	private Database database;
	private MasterWallet masterWallet;
	
	private UserStorageService userStorage;
	
	public DepositManager(Database database, MasterWallet masterWallet) {
		this.database = database;
		this.masterWallet = masterWallet;
		
		userStorage = Sponge.getServiceManager().provide(UserStorageService.class).get();
	}
	
	private Optional<KristAccount> findAccountByAddress(String address) {
		return database.getAccounts().values().stream()
			.filter(kristAccount -> kristAccount.getDepositWallet().getAddress().equalsIgnoreCase(address))
			.findFirst();
	}
	
	private void handleDeposit(KristAccount account, int depositAmount) {
		Wallet depositWallet = account.getDepositWallet();
		if (depositWallet == null) return;
		
		depositWallet.transfer(masterWallet.getAddress(), depositAmount, null, (success, transaction) -> {
			account.deposit(
				KristPay.INSTANCE.getCurrency(),
				BigDecimal.valueOf(depositAmount),
				Cause.of(EventContext.empty(), this)
			);
			
			// notify player of their deposit if they are online
			Sponge.getServer().getPlayer(UUID.fromString(account.getOwner())).ifPresent(player -> {
				if (!player.isOnline()) return; // TODO: queue deposit message until next time player is on
				
				player.sendMessage(
					Text.builder()
						.append(CommandHelpers.formatKrist(depositAmount))
						.append(Text.of(TextColors.GREEN, " was sucessfully deposited into your account."))
						.build()
				);
			});
		});
	}
	
	public void handleTransaction(KristTransaction transaction) {
		String address = transaction.getTo();
		
		if (address.equalsIgnoreCase(masterWallet.getAddress())
			&& transaction.getMetadata() != null && !transaction.getMetadata().isEmpty()) {
			handleNameTransaction(transaction);
		} else {
			findAccountByAddress(address).ifPresent(account -> handleDeposit(account, transaction.getValue()));
		}
		
		masterWallet.syncWithNode(cb -> {}); // TODO: does anything need to be handled here?
	}
	
	private void handleNameTransaction(KristTransaction transaction) {
		Optional<Map<String, String>> commonMetaOpt = KristAPI.parseCommonMeta(transaction.getMetadata());
		if (!commonMetaOpt.isPresent()) return; // keep the funds as donation
		
		Map<String, String> commonMeta = commonMetaOpt.get();
		if (!commonMeta.containsKey("metaname")) return;
		String metaname = commonMeta.get("metaname");
		
		Optional<User> userOpt = userStorage.get(metaname); // case insensitive
		if (!userOpt.isPresent()) return;
		User user = userOpt.get();
		
		Optional<UniqueAccount> accountOpt = ECONOMY_SERVICE.getOrCreateAccount(user.getUniqueId());
		if (!accountOpt.isPresent()) return;
		UniqueAccount account = accountOpt.get();
		if (!(account instanceof KristAccount)) return;
		
		handleDeposit((KristAccount) account, transaction.getValue());
	}
	
	public void walletSynced(KristAccount account) {
		Wallet depositWallet = account.getDepositWallet();
		if (depositWallet == null) return;
		
		int depositAmount = depositWallet.getBalance();
		if (depositAmount <= 0) return;
		
		handleDeposit(account, depositAmount);
	}
}
