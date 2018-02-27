package pw.lemmmy.kristpay;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import lombok.Getter;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import pw.lemmmy.kristpay.config.Config;
import pw.lemmmy.kristpay.config.ConfigLoader;
import pw.lemmmy.kristpay.krist.KristClientManager;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(id = "kristpay", name = "KristPay", version = "2.0")
public class KristPay {
	public static KristPay INSTANCE;
	
	@Inject @Getter private Logger logger;
	
	@Inject
	@DefaultConfig(sharedRoot = false)
	private ConfigurationLoader<CommentedConfigurationNode> configLoader;
	
	
	@Inject
	@DefaultConfig(sharedRoot = false)
	private Path configPath;
	
	@Getter private Config config;
	
	@Getter private KristClientManager kristClientManager;
	
	@Listener
	public void preInit(GamePreInitializationEvent event) throws IOException {
		INSTANCE = this;
		
		try {
			config = new ConfigLoader(configLoader, configPath).loadConfig();
		} catch (ObjectMappingException e) {
			logger.error("Error loading KristPay config", e);
		}
	}
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		kristClientManager = new KristClientManager();
		new Thread(kristClientManager).start();
	}
}
