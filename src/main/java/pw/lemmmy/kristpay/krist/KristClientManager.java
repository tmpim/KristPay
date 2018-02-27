package pw.lemmmy.kristpay.krist;

import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.Getter;
import org.spongepowered.api.scheduler.Task;
import pw.lemmmy.kristpay.KristPay;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Getter
public class KristClientManager implements Runnable {
	private KristClient kristClient;
	
	@Override
	public void run() {
		startClient();
	}
	
	protected void clientClosed() {
		kristClient = null;
		startClient();
	}
	
	protected void startClient() {
		try {
			KristPay.INSTANCE.getLogger().info("Master address is " + KristPay.INSTANCE.getConfig().getMasterWallet().getAddress());
			KristPay.INSTANCE.getLogger().info("Websocket connecting to krist node " + KristAPI.getKristNode());
			
			Optional<String> websocketURL = KristAPI.getWebsocketURL("test");
			
			if (websocketURL.isPresent()) {
				kristClient = new KristClient(this, new URI(websocketURL.get()));
				try {
					kristClient.connectBlocking();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				KristPay.INSTANCE.getLogger().error("Unable to get websocket URL");
				reconnect();
			}
		} catch (UnirestException e) {
			KristPay.INSTANCE.getLogger().error("Unable to get websocket URL", e);
			reconnect();
		} catch (URISyntaxException e) {
			KristPay.INSTANCE.getLogger().error("Unable to parse websocket URI", e);
			reconnect();
		}
	}
	
	private void reconnect() {
		Task.builder().execute(this::startClient)
			.async().delay(2500, TimeUnit.MILLISECONDS)
			.name("KristPay - Reconnect attempt")
			.submit(KristPay.INSTANCE);
	}
}
