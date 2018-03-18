package pw.lemmmy.kristpay.config;

import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import pw.lemmmy.kristpay.Utils;

@ConfigSerializable
@Getter
public class ConfigMasterWallet {
	@Setting private String privatekey = Utils.generatePassword();
	
	@Setting private String primaryDepositName;
}
