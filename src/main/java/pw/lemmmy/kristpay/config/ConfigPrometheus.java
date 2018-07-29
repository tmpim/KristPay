package pw.lemmmy.kristpay.config;

import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
@Getter
public class ConfigPrometheus {
	@Setting
	private boolean enabled = false;
	
	@Setting
	private int port = 9227;
}
