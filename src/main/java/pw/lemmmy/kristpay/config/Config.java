package pw.lemmmy.kristpay.config;

import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
@Getter
public class Config {
	@Setting private ConfigMasterWallet masterWallet = new ConfigMasterWallet();
	@Setting private ConfigNode node = new ConfigNode();
	@Setting private ConfigEconomy economy = new ConfigEconomy();
}
