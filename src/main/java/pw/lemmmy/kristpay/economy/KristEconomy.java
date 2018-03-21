package pw.lemmmy.kristpay.economy;

import lombok.Getter;
import lombok.val;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.database.AccountDatabase;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Getter
public class KristEconomy implements EconomyService {
	private KristCurrency kristCurrency = new KristCurrency();
	
	@Override
	public Currency getDefaultCurrency() {
		return kristCurrency;
	}
	
	@Override
	public Set<Currency> getCurrencies() {
		val set = new HashSet<Currency>();
		set.add(kristCurrency);
		return set;
	}
	
	@Override
	public boolean hasAccount(UUID uuid) {
		return KristPay.INSTANCE.getAccountDatabase().getAccounts().containsKey(uuid.toString());
	}
	
	@Override
	public boolean hasAccount(String identifier) {
		return KristPay.INSTANCE.getAccountDatabase().getAccounts().containsKey(identifier);
	}
	
	@Override
	public Optional<UniqueAccount> getOrCreateAccount(UUID uuid) {
		val opt = getOrCreateAccount(uuid.toString());
		
		return opt.isPresent()
			   ? (opt.get() instanceof UniqueAccount ? Optional.of((UniqueAccount) opt.get()) : Optional.empty())
			   : Optional.empty();
	}
	
	@Override
	public Optional<Account> getOrCreateAccount(String identifier) {
		AccountDatabase db = KristPay.INSTANCE.getAccountDatabase();
		
		if (db.getAccounts().containsKey(identifier)) {
			return Optional.of(db.getAccounts().get(identifier));
		} else {
			KristAccount account = new KristAccount(identifier);
			
			account.setBalance(
				getDefaultCurrency(),
				account.getDefaultBalance(getDefaultCurrency()),
				Cause.of(EventContext.empty(), this),
				null
			);
			
			db.getAccounts().put(account.getOwner(), account);
			db.save();
			
			return Optional.of(account);
		}
	}
	
	@Override
	public void registerContextCalculator(ContextCalculator<Account> calculator) {}
}
