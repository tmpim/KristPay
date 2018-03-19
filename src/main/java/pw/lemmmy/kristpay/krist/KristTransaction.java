package pw.lemmmy.kristpay.krist;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.time.ZonedDateTime;
import java.util.Date;

@Getter
@RequiredArgsConstructor
public class KristTransaction {
	@NonNull private int id;
	@NonNull private String from, to;
	@NonNull private int value;
	@NonNull private Date time;
	private String name, metadata;
	
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
