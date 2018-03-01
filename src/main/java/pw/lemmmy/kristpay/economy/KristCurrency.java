package pw.lemmmy.kristpay.economy;

import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.Text;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class KristCurrency implements Currency {
	@Override
	public Text getDisplayName() {
		return Text.of(getName());
	}
	
	@Override
	public Text getPluralDisplayName() {
		return getDisplayName();
	}
	
	@Override
	public Text getSymbol() {
		return Text.of("KST");
	}
	
	@Override
	public Text format(BigDecimal amount, int numFractionDigits) {
		NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
		numberFormat.setMaximumFractionDigits(numFractionDigits);
		return Text.of(numberFormat.format(amount));
	}
	
	@Override
	public int getDefaultFractionDigits() {
		return 0;
	}
	
	@Override
	public boolean isDefault() {
		return true;
	}
	
	@Override
	public String getId() {
		return "kristpay:kristCurrency";
	}
	
	@Override
	public String getName() {
		return "Krist";
	}
}
