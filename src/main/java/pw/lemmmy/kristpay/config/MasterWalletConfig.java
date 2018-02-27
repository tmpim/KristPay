package pw.lemmmy.kristpay.config;

import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import pw.lemmmy.kristpay.Utils;
import pw.lemmmy.kristpay.krist.KristAPI;

@ConfigSerializable
@Getter
public class MasterWalletConfig {
	@Setting private String privatekey = Utils.generatePassword();
	
	public String getAddress() {
		return KristAPI.makeV2Address(privatekey);
	}
}
