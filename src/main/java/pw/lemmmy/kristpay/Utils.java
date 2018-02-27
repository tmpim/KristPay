package pw.lemmmy.kristpay;

import org.apache.commons.lang3.RandomStringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Formatter;

public class Utils {
	private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	
	public static String byteArrayToHexString(byte[] bytes) {
		Formatter formatter = new Formatter();
		
		for (byte b : bytes) {
			formatter.format("%02x", b);
		}
		
		return formatter.toString();
	}
	
	public static String sha256(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return byteArrayToHexString(hash);
		} catch (NoSuchAlgorithmException e) {
			// This shouldn't happen.
			e.printStackTrace();
			return null;
		}
	}
	
	public static String generatePassword() {
		return RandomStringUtils.random(64, 0, ALPHANUM.length(),
			false, false, ALPHANUM.toCharArray(), new SecureRandom());
	}
}
