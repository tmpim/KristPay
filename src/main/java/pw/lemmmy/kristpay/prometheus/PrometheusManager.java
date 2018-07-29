package pw.lemmmy.kristpay.prometheus;

import lombok.Getter;
import org.eclipse.jetty.server.Server;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.prometheus.reporters.AccountsReporter;
import pw.lemmmy.kristpay.prometheus.reporters.MasterBalanceReporter;
import pw.lemmmy.kristpay.prometheus.reporters.TransactionsReporter;

public class PrometheusManager {
	private Server server;
	private PrometheusController controller;
	@Getter private TransactionsReporter transactionsReporter;
	
	public void startServer() {
		if (server != null) {
			try {
				server.stop();
			} catch (Exception e) {
				KristPay.INSTANCE.getLogger().error("Cannot stop prometheus server", e);
			}
		}
		
		controller = new PrometheusController();
		controller.addReporter(new MasterBalanceReporter());
		controller.addReporter(new AccountsReporter());
		controller.addReporter(transactionsReporter = new TransactionsReporter());
		
		server = new Server(KristPay.INSTANCE.getConfig().getPrometheus().getPort());
		server.setHandler(controller);
		
		try {
			server.start();
		} catch (Exception e) {
			KristPay.INSTANCE.getLogger().error("Cannot start prometheus server", e);
		
			server = null;
			controller = null;
		}
	}
	
	public void stopServer() {
		if (server != null) {
			try {
				server.stop();
			} catch (Exception e) {
				KristPay.INSTANCE.getLogger().error("Cannot stop prometheus server", e);
			}
			
			server = null;
			controller = null;
		}
	}
}
