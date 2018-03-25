package pw.lemmmy.kristpay.krist;

import lombok.Getter;
import lombok.NonNull;
import org.json.JSONObject;

import java.time.ZonedDateTime;
import java.util.Date;

@Getter
public class KristTransaction {
	private int id;
	@NonNull private String from, to;
	private int value;
	@NonNull private Date time;
	private String name, metadata;
	
	public KristTransaction(int id, String from, String to, int value, Date time) {
		this.id = id;
		this.from = from;
		this.to = to;
		this.value = value;
		this.time = time;
	}
	
	public static KristTransaction fromJSON(JSONObject transactionJSON) {
		KristTransaction transaction = new KristTransaction(
			transactionJSON.optInt("id", 0),
			transactionJSON.optString("from", ""),
			transactionJSON.optString("to", ""),
			transactionJSON.optInt("value", 0),
			Date.from(ZonedDateTime.parse(transactionJSON.optString("time", "")).toInstant()) // wtf?
		);
		
		if (transactionJSON.has("name")) transaction.name = transactionJSON.getString("name");
		if (transactionJSON.has("metadata")) transaction.metadata = transactionJSON.getString("metadata");
		
		return transaction;
	}
}
