package pw.lemmmy.kristpay.config;

import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
@Getter
public class Config {
	@Setting private MasterWalletConfig masterWallet = new MasterWalletConfig();
	@Setting private NodeConfig node = new NodeConfig();
	@Setting private EconomyConfig economy = new EconomyConfig();
}
