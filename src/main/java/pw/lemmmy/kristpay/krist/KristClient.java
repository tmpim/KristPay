package pw.lemmmy.kristpay.krist;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import pw.lemmmy.kristpay.KristPay;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class KristClient extends WebSocketClient {
	private KristClientManager manager;
	
	private Map<Integer, Consumer<JSONObject>> responseCallbacks = new HashMap<>();
	private int idCounter;
	
	public KristClient(KristClientManager manager, URI connectionURI) throws URISyntaxException {
		super(connectionURI, new Draft_6455());
		
		this.manager = manager;
	}
	
	@Override
	public void onOpen(ServerHandshake handshakedata) {
		KristPay.INSTANCE.getLogger().info("Krist websocket connected");
	}
	
	@Override
	public void onMessage(String message) {
		KristPay.INSTANCE.getLogger().info(message);
		
		try {
			JSONObject data = new JSONObject(message);
			
			// handle response callbacks
			if (data.has("id") && responseCallbacks.containsKey(data.getInt("id")))
				responseCallbacks.remove(data.getInt("id")).accept(data);
			
			// handle events etc
			if (data.has("type"))
				handleMessage(data.getString("type"), data);
		} catch (Exception e) {
			KristPay.INSTANCE.getLogger().error("Error handling websocket message", e);
		}
	}
	
	private void sendMessage(String type, JSONObject data, Consumer<JSONObject> onResponse) {
		int id = ++idCounter;
		
		JSONObject message = new JSONObject(data, JSONObject.getNames(data));
		message.put("type", type);
		message.put("id", id);
		
		responseCallbacks.put(id, onResponse);
		
		System.out.println(message.toString());
		
		send(message.toString());
	}
	
	private void getAddressBalanceAync(String address, Consumer<Integer> cb) {
		sendMessage("address", new JSONObject().put("address", address), response -> {
			if (response.getBoolean("ok")) {
				cb.accept(response.getJSONObject("address").getInt("balance"));
			} else {
				cb.accept(0);
			}
		});
	}
	
	private void handleMessage(String type, JSONObject data) {
		switch (type) {
			case "hello":
				KristPay.INSTANCE.getLogger().info("Server MOTD: " + data.optString("motd", "N/A"));
				
				getAddressBalanceAync(KristPay.INSTANCE.getConfig().getMasterWallet().getAddress(), balance ->
					KristPay.INSTANCE.getLogger().info("Master wallet balance: " + balance + " KST"));
				
				break;
		}
	}
	
	@Override
	public void onClose(int code, String reason, boolean remote) {
		KristPay.INSTANCE.getLogger().info("Websocket disconnected, trying to connect again...");
		manager.clientClosed();
	}
	
	@Override
	public void onError(Exception ex) {
		KristPay.INSTANCE.getLogger().error("Error in websocket", ex);
	}
}
