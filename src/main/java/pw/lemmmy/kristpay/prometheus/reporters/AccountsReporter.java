package pw.lemmmy.kristpay.prometheus.reporters;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import pw.lemmmy.kristpay.KristPay;

public class AccountsReporter implements Reporter {
	private final Gauge accountsTotalGauge = Gauge.build()
		.namespace(NAMESPACE)
		.name("accounts_total")
		.help("Total amount of accounts that have been created")
		.create();
	
	@Override
	public void register(CollectorRegistry registry) {
		accountsTotalGauge.register(registry);
	}
	
	@Override
	public void fetch() {
		if (KristPay.INSTANCE.getAccountDatabase() == null) return;
		accountsTotalGauge.set(KristPay.INSTANCE.getAccountDatabase().getAccounts().size());
	}
}
