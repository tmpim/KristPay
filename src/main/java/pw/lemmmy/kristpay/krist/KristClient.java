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
	
	private MasterWallet masterWallet;
	
	public KristClient(KristClientManager manager, URI connectionURI) throws URISyntaxException {
		super(connectionURI, new Draft_6455());
		
		this.manager = manager;
		this.masterWallet = KristPay.INSTANCE.getMasterWallet();
	}
	
	@Override
	public void onOpen(ServerHandshake handshakedata) {
		KristPay.INSTANCE.getLogger().info("Krist websocket connected");
	}
	
	@Override
	public void onMessage(String message) {
		// KristPay.INSTANCE.getLogger().info(message);
		
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
		
		if (onResponse != null) responseCallbacks.put(id, onResponse);
		
		send(message.toString());
	}
	
	public void getAddressBalanceAync(String address, Consumer<Integer> cb) {
		sendMessage("address", new JSONObject().put("address", address), response -> {
			if (response.getBoolean("ok")) {
				cb.accept(response.getJSONObject("address").getInt("balance"));
			} else {
				cb.accept(0);
			}
		});
	}
	
	public void subscribe(String event) {
		sendMessage("subscribe", new JSONObject().put("event", event), null);
	}
	
	private void handleMessage(String type, JSONObject data) {
		switch (type) {
			case "hello":
				KristPay.INSTANCE.getLogger().info("Server MOTD: " + data.optString("motd", "N/A").trim());
				
				masterWallet.syncWithNode(balance ->
					KristPay.INSTANCE.getLogger().info("Master wallet balance: " + masterWallet.getBalance() + " KST"));
				
				KristPay.INSTANCE.getDatabase().syncWallets();
				
				subscribe("transactions");
				
				break;
			case "event":
				String event = data.optString("event");
				
				if (event.equals("transaction")) handleTransactionEvent(data);
				
				break;
		}
	}
	
	private void handleTransactionEvent(JSONObject data) {
		if (!data.has("transaction")) return;
		
		JSONObject transactionJSON = data.getJSONObject("transaction");
		KristTransaction transaction = KristTransaction.fromJSON(transactionJSON);
		
		if (KristPay.INSTANCE.getDepositManager() != null)
			KristPay.INSTANCE.getDepositManager().handleTransaction(transaction);
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
