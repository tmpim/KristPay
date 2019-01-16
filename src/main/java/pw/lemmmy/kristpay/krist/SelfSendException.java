package pw.lemmmy.kristpay.krist;

public class SelfSendException extends RuntimeException {
	public SelfSendException() {
		super("You can't send Krist to yourself.");
	}
}
