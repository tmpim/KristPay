package pw.lemmmy.kristpay.prometheus.reporters;

import io.prometheus.client.CollectorRegistry;

public interface Reporter {
	String NAMESPACE = "kristpay";
	
	void register(CollectorRegistry registry);
	
	default void fetch() {}
}
