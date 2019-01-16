package pw.lemmmy.kristpay.krist;

public class NameNotFoundException extends RuntimeException {
	public NameNotFoundException() {
		super("Name not found.");
	}
}
