package pw.lemmmy.kristpay.config;

import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.Arrays;
import java.util.List;

@ConfigSerializable
@Getter
public class ConfigFaucet {
	@Setting
	private List<Integer> rewardTiers = Arrays.asList(5, 10, 15, 20);
	
	@Setting(comment = "Minimum time until next reward is claimable (milliseconds)")
	private long minimumTime = 1000 * 60 * 60 * 24; // 24 hours
	
	@Setting(comment = "Maximum time until next reward is no longer claimable and tier resets (milliseconds)")
	private long expireTime = 1000 * 60 * 60 * 48; // 48 hours
}
