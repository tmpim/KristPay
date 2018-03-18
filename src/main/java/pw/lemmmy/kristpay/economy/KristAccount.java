package pw.lemmmy.kristpay.economy;

import lombok.Getter;
import lombok.val;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransactionTypes;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.text.Text;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.Utils;
import pw.lemmmy.kristpay.krist.Wallet;

import java.math.BigDecimal;
import java.util.HashMap;
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
		this.owner = owner;
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
		return Text.of(owner);
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
		return BigDecimal.valueOf(balance);
	}
	
	@Override
	public Map<Currency, BigDecimal> getBalances(Set<Context> contexts) {
		val balances = new HashMap<Currency, BigDecimal>();
		balances.put(KristPay.INSTANCE.getCurrency(), getBalance(KristPay.INSTANCE.getCurrency()));
		return balances;
	}
	
	@Override
	public Map<Currency, BigDecimal> getBalances() {
		return getBalances(null);
	}
	
	@Override
	public Map<Currency, TransactionResult> resetBalances(Cause cause, Set<Context> contexts) {
		TransactionResult result = resetBalance(KristPay.INSTANCE.getCurrency(), cause, contexts);
		
		Map<Currency, TransactionResult> map = new HashMap<>();
		map.put(KristPay.INSTANCE.getCurrency(), result);
		return map;
	}
	
	@Override
	public TransactionResult resetBalance(Currency currency, Cause cause, Set<Context> contexts) {
		return setBalance(currency, getDefaultBalance(currency), cause, contexts);
	}
	
	@Override
	public TransactionResult deposit(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
		return setBalance(currency, BigDecimal.valueOf(balance + amount.intValue()), cause, contexts);
	}
	
	@Override
	public TransactionResult withdraw(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
		int amt = amount.intValue();
		int newBalance = balance - amt;
		
		if (newBalance < 0) {
			return new KristTransactionResult(this, amount, contexts, ResultType.ACCOUNT_NO_FUNDS, TransactionTypes.WITHDRAW);
		} else {
			return setBalance(currency, BigDecimal.valueOf(newBalance), cause, contexts);
		}
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
	
	@Override
	public TransactionResult setBalance(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts) {
		int oldBalance = this.balance;
		
		// TODO: logging
		System.out.println("Changing balance of " + owner + " from " + oldBalance + " to " + amount.intValue());
		
		if (amount.intValue() < 0) { // balance should never be negative
			// TODO: log result
			return new KristTransactionResult(this, amount, contexts, ResultType.FAILED, TransactionTypes.WITHDRAW);
		}
		
		int delta = balance - amount.intValue();
		
		if (delta < 0) { // increase in balance - check master wallet can fund it
			int masterBalance = KristPay.INSTANCE.getMasterWallet().getBalance();
			int used = KristPay.INSTANCE.getDatabase().getTotalDistributedKrist();
			
			int available = masterBalance - used;
			int increase = Math.abs(delta);
			
			if (increase > available) {
				// TODO: log result (failed, master wallet can't fund)
				return new KristTransactionResult(this, BigDecimal.valueOf(0), contexts, ResultType.FAILED, TransactionTypes.DEPOSIT);
			} else {
				balance = amount.intValue();
				KristPay.INSTANCE.getDatabase().save();
				// TODO: log result (success, deposit)
				return new KristTransactionResult(this, BigDecimal.valueOf(increase), contexts, ResultType.SUCCESS, TransactionTypes.DEPOSIT);
			}
		} else if (delta > 0) { // decrease in balance
			int decrease = Math.abs(delta); // use this in log
			balance = amount.intValue();
			KristPay.INSTANCE.getDatabase().save();
			// TODO: log result (success, withdraw)
			return new KristTransactionResult(this, BigDecimal.valueOf(Math.abs(delta)), contexts, ResultType.SUCCESS, TransactionTypes.WITHDRAW);
		} else { // no change in balance
			// TODO: log result (no change)
			return new KristTransactionResult(this, BigDecimal.valueOf(0), contexts, ResultType.SUCCESS, TransactionTypes.WITHDRAW);
		}
	}
	
	@Override
	public TransferResult transfer(Account to,
								   Currency currency,
								   BigDecimal amount,
								   Cause cause,
								   Set<Context> contexts) {
		if (!(to instanceof KristAccount)) {
			// TODO: log result (failed, target not a krist account)
			return new KristTransferResult(to, this, currency, amount, contexts, ResultType.FAILED, TransactionTypes.TRANSFER);
		}
		
		KristAccount target = (KristAccount) to;
		
		if (balance - amount.intValue() < 0) {
			// TODO: log result (failed, no funds)
			return new KristTransferResult(to, this, currency, amount, contexts, ResultType.ACCOUNT_NO_FUNDS, TransactionTypes.TRANSFER);
		}
		
		if (amount.intValue() < 0) {
			// TODO: log result (failed, amount < 0)
			return new KristTransferResult(to, this, currency, amount, contexts, ResultType.FAILED, TransactionTypes.TRANSFER);
		}
		
		balance -= amount.intValue();
		target.balance += amount.intValue();
		KristPay.INSTANCE.getDatabase().save();
		// TODO: log result (success)
		return new KristTransferResult(to, this, currency, amount, contexts, ResultType.SUCCESS, TransactionTypes.TRANSFER);
	}
}
