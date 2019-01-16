package pw.lemmmy.kristpay.krist;

public class KristDownException extends RuntimeException {
	public KristDownException() {
		super("Krist is down.");
	}
}
