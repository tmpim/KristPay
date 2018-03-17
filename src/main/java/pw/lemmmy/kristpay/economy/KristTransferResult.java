package pw.lemmmy.kristpay.economy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionType;
import org.spongepowered.api.service.economy.transaction.TransferResult;

import java.math.BigDecimal;
import java.util.Set;

@AllArgsConstructor
@Getter
public class KristTransferResult implements TransferResult {
	private Account accountTo, account; // to, from
	private Currency currency;
	private BigDecimal amount;
	private Set<Context> contexts;
	private ResultType result;
	private TransactionType type;
}
