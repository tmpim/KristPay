package pw.lemmmy.kristpay.economy;

import lombok.Getter;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.text.Text;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.Utils;
import pw.lemmmy.kristpay.krist.Wallet;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
public class KristAccount implements UniqueAccount {
	private String owner;
	private Wallet depositWallet;
	private int balance = 0;
	
	private boolean needsSave = false;
	
	public KristAccount(String owner) {
		this.depositWallet = new Wallet(Utils.generatePassword());
		this.balance = 0;
		needsSave = true;
	}
	
	public KristAccount(String owner, Wallet depositWallet, int balance) {
		this.owner = owner;
		this.depositWallet = depositWallet;
		this.balance = balance;
	}
	
	@Override
	public Text getDisplayName() {
		return null;
	}
	
	@Override
	public BigDecimal getDefaultBalance(Currency currency) {
		return BigDecimal.valueOf(KristPay.INSTANCE.getConfig().getEconomy().getStartingBalance());
	}
	
	@Override
	public boolean hasBalance(Currency currency, Set<Context> contexts) {
		return true;
	}
	
	@Override
	public BigDecimal getBalance(Currency currency, Set<Context> contexts) {
		return null;
	}
	
	@Override
	public Map<Currency, BigDecimal> getBalances(Set<Context> contexts) {
		return null;
	}
	
	@Override
	public TransactionResult setBalance(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
		return null;
	}
	
	@Override
	public Map<Currency, TransactionResult> resetBalances(Cause cause, Set<Context> contexts) {
		return null;
	}
	
	@Override
	public TransactionResult resetBalance(Currency currency, Cause cause, Set<Context> contexts) {
		return setBalance(currency, getDefaultBalance(currency), cause, contexts);
	}
	
	@Override
	public TransactionResult deposit(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
		return null;
	}
	
	@Override
	public TransactionResult withdraw(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
		return null;
	}
	
	@Override
	public TransferResult transfer(Account to,
								   Currency currency,
								   BigDecimal amount,
								   Cause cause,
								   Set<Context> contexts) {
		return null;
	}
	
	@Override
	public String getIdentifier() {
		return owner;
	}
	
	@Override
	public Set<Context> getActiveContexts() {
		return null;
	}
	
	@Override
	public UUID getUniqueId() {
		return UUID.fromString(owner);
	}
}
