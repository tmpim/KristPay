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
	private boolean up = false;
	private boolean stopping = false;
	
	@Override
	public void run() {
		startClient();
	}
	
	protected void clientClosed() {
		up = false;
		kristClient = null;
		
		if (!stopping) reconnect();
	}
	
	public void stopClient() {
		stopping = true;
		up = false;
		kristClient.close();
	}
	
	public void startClient() {
		stopping = false;
		
		try {
			KristPay.INSTANCE.getLogger().info("Master address is " + KristPay.INSTANCE.getMasterWallet().getAddress());
			KristPay.INSTANCE.getLogger().info("Websocket connecting to krist node " + KristAPI.getKristNode());
			
			Optional<String> websocketURL = KristAPI.getWebsocketURL("test");
			
			if (websocketURL.isPresent()) {
				kristClient = new KristClient(this, new URI(websocketURL.get()));
				kristClient.connect();
				up = true;
			} else {
				KristPay.INSTANCE.getLogger().error("Unable to get websocket URL. Krist node appears to be down.");
				up = false;
				reconnect();
			}
		} catch (UnirestException e) {
			KristPay.INSTANCE.getLogger().error("Unable to get websocket URL. Krist node appears to be down.");
			KristPay.INSTANCE.getLogger().debug("", e);
			up = false;
			reconnect();
		} catch (URISyntaxException e) {
			KristPay.INSTANCE.getLogger().error("Unable to parse websocket URI", e);
			up = false;
			reconnect();
		}
	}
	
	private void reconnect() {
		Task.builder().execute(this::startClient)
			.async().delay(2000, TimeUnit.MILLISECONDS)
			.name("KristPay - Reconnect attempt")
			.submit(KristPay.INSTANCE);
	}
}
