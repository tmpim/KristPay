package pw.lemmmy.kristpay.economy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransactionType;
import pw.lemmmy.kristpay.KristPay;

import java.math.BigDecimal;
import java.util.Set;

@AllArgsConstructor
@Getter
public class KristTransactionResult implements TransactionResult {
	private KristAccount account;
	private BigDecimal amount;
	private Set<Context> contexts;
	private ResultType result;
	private TransactionType type;
	private String error;
	
	@Override
	public Currency getCurrency() {
		return KristPay.INSTANCE.getCurrency();
	}
}
