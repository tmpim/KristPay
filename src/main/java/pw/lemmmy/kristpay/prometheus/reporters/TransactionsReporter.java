package pw.lemmmy.kristpay.prometheus.reporters;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;

public class TransactionsReporter implements Reporter {
	private final Counter transactionsCountCounter = Counter.build()
		.namespace(NAMESPACE)
		.name("transactions_count")
		.help("Counter of all transactions that have been made")
		.create();
	
	private final Counter withdrawalsCountCounter = Counter.build()
		.namespace(NAMESPACE)
		.name("withdrawals_count")
		.help("Counter of all withdrawals that have been made")
		.create();
	
	private final Counter transfersCountCounter = Counter.build()
		.namespace(NAMESPACE)
		.name("transfers_count")
		.help("Counter of all transfers that have been made")
		.create();
	
	private final Counter depositsCountCounter = Counter.build()
		.namespace(NAMESPACE)
		.name("deposits_count")
		.help("Counter of all deposits that have been made")
		.create();
	
	private final Counter transactionsAmountCounter = Counter.build()
		.namespace(NAMESPACE)
		.name("transactions_amount")
		.help("Counter of amount of krist that has been transacted")
		.create();
	
	private final Counter withdrawalsAmountCounter = Counter.build()
		.namespace(NAMESPACE)
		.name("withdrawals_amount")
		.help("Counter of amount of krist that has been withdrawn")
		.create();
	
	private final Counter transfersAmountCounter = Counter.build()
		.namespace(NAMESPACE)
		.name("transfers_amount")
		.help("Counter of amount of krist that has been transferred")
		.create();
	
	private final Counter depositsAmountCounter = Counter.build()
		.namespace(NAMESPACE)
		.name("deposits_amount")
		.help("Counter of amount of krist that has been deposited")
		.create();
	
	@Override
	public void register(CollectorRegistry registry) {
		transactionsCountCounter.register(registry);
		withdrawalsCountCounter.register(registry);
		transfersCountCounter.register(registry);
		depositsCountCounter.register(registry);
		transactionsAmountCounter.register(registry);
		withdrawalsAmountCounter.register(registry);
		transfersAmountCounter.register(registry);
		depositsAmountCounter.register(registry);
	}
	
	public void incrementWithdrawals(int amount) {
		transactionsCountCounter.inc();
		withdrawalsCountCounter.inc();
		
		transactionsAmountCounter.inc(amount);
		withdrawalsAmountCounter.inc(amount);
	}
	
	public void incrementDeposits(int amount) {
		transactionsCountCounter.inc();
		depositsCountCounter.inc();
		
		transactionsAmountCounter.inc(amount);
		depositsAmountCounter.inc(amount);
	}
	
	public void incrementTransfers(int amount) {
		transactionsCountCounter.inc();
		transfersCountCounter.inc();
		
		transactionsAmountCounter.inc(amount);
		transfersAmountCounter.inc(amount);
	}
}
