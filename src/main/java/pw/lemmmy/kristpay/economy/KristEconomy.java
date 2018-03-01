package pw.lemmmy.kristpay.economy;

import lombok.Getter;
import lombok.val;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;

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
		return false;
	}
	
	@Override
	public boolean hasAccount(String identifier) {
		return false;
	}
	
	@Override
	public Optional<UniqueAccount> getOrCreateAccount(UUID uuid) {
		return null;
	}
	
	@Override
	public Optional<Account> getOrCreateAccount(String identifier) {
		return null;
	}
	
	@Override
	public void registerContextCalculator(ContextCalculator<Account> calculator) {}
}
