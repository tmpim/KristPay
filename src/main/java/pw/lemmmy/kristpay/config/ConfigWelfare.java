package pw.lemmmy.kristpay.config;

import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.Arrays;
import java.util.List;

@ConfigSerializable
@Getter
public class ConfigWelfare {
	@Setting(comment = "Amount of Krist to reward every day a user logs in")
	private int welfareAmount = 10;
	
	@Setting(comment = "Minimum days before welfare is given to users")
	private int welfareMinimumDays = 5;
}
