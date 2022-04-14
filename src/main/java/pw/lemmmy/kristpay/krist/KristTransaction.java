package pw.lemmmy.kristpay.krist;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.json.JSONObject;

import java.time.ZonedDateTime;
import java.util.Date;

@AllArgsConstructor
public class KristTransaction {
	public final int id;
	@NonNull public final String from, to;
	public final int value;
	@NonNull public final Date time;
	public final String name, metadata;
	
	public static KristTransaction fromJSON(JSONObject transactionJSON) {
		KristTransaction transaction = new KristTransaction(
			transactionJSON.optInt("id", 0),
			transactionJSON.optString("from", ""),
			transactionJSON.optString("to", ""),
			transactionJSON.optInt("value", 0),
			Date.from(ZonedDateTime.parse(transactionJSON.optString("time", "")).toInstant()), // wtf?
			transactionJSON.optString("name", null),
			transactionJSON.optString("metadata", null)
		);
		
		return transaction;
	}
}
