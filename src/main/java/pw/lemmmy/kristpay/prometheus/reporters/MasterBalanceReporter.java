package pw.lemmmy.kristpay.prometheus.reporters;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import pw.lemmmy.kristpay.KristPay;

public class MasterBalanceReporter implements Reporter {
	private final Gauge totalBalanceGauge = Gauge.build()
		.namespace(NAMESPACE)
		.name("master_balance_total")
		.help("Total amount of krist in the master wallet")
		.create();
	
	private final Gauge allocatedBalanceGauge = Gauge.build()
		.namespace(NAMESPACE)
		.name("master_balance_allocated")
		.help("Allocated amount of krist in the master wallet")
		.create();
	
	private final Gauge unallocatedBalanceGauge = Gauge.build()
		.namespace(NAMESPACE)
		.name("master_balance_unallocated")
		.help("Unallocated amount of krist in the master wallet")
		.create();
	
	@Override
	public void register(CollectorRegistry registry) {
		totalBalanceGauge.register(registry);
		allocatedBalanceGauge.register(registry);
		unallocatedBalanceGauge.register(registry);
	}
	
	@Override
	public void fetch() {
		if (KristPay.INSTANCE.getMasterWallet() == null || KristPay.INSTANCE.getAccountDatabase() == null) return;
		int masterBalance = KristPay.INSTANCE.getMasterWallet().getBalance();
		int allocated = KristPay.INSTANCE.getAccountDatabase().getTotalDistributedKrist();
		
		totalBalanceGauge.set(masterBalance);
		allocatedBalanceGauge.set(allocated);
		unallocatedBalanceGauge.set(masterBalance - allocated);
	}
}
