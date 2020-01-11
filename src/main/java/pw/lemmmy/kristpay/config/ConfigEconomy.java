package pw.lemmmy.kristpay.config;

import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
@Getter
public class ConfigEconomy {
	@Setting private int startingBalance = 100;
	
	@Setting private String kristSymbol = "\ue000";
	@Setting private String kristCode = "KST";
}
