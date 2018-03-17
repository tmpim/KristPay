package pw.lemmmy.kristpay;

import com.google.inject.Inject;
import lombok.Getter;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageReceiver;
import pw.lemmmy.kristpay.config.Config;
import pw.lemmmy.kristpay.config.ConfigLoader;
import pw.lemmmy.kristpay.database.Database;
import pw.lemmmy.kristpay.economy.KristCurrency;
import pw.lemmmy.kristpay.economy.KristEconomy;
import pw.lemmmy.kristpay.krist.KristClientManager;
import pw.lemmmy.kristpay.krist.MasterWallet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Plugin(id = "kristpay", name = "KristPay", version = "2.0")
@Getter
public class KristPay {
	public static KristPay INSTANCE;
	
	@Inject private Logger logger;
	
	@Inject @DefaultConfig(sharedRoot = false)
	private ConfigurationLoader<CommentedConfigurationNode> configLoader;
	@Inject @DefaultConfig(sharedRoot = false)
	private Path configPath;
	@Inject @ConfigDir(sharedRoot = false)
	private Path configDir;
	private Config config;
	
	private KristClientManager kristClientManager;
	private MasterWallet masterWallet;
	private Database database;
	private KristCurrency currency = new KristCurrency();
	
	private KristEconomy economyService = new KristEconomy();
	
	@Listener
	public void preInit(GamePreInitializationEvent event) throws IOException {
		INSTANCE = this;
		
		try {
			config = new ConfigLoader(configLoader, configPath).loadConfig();
		} catch (ObjectMappingException e) {
			logger.error("Error loading KristPay config", e);
		}
		
		masterWallet = new MasterWallet(config.getMasterWallet().getPrivatekey());
		
		economyService = new KristEconomy();
		Sponge.getServiceManager().setProvider(this, KristEconomy.class, economyService);
	}
	
	@Listener
	public void init(GameInitializationEvent event) {
		loadDatabase();
		
		kristClientManager = new KristClientManager();
		new Thread(kristClientManager).start();
	}
	
	@Listener
	public void reload(GameReloadEvent event, @First MessageReceiver receiver) {
		receiver.sendMessage(Text.of("Reloading KristPay."));
		kristClientManager.stopClient();
		try {
			database.load(); // todo: clear db before loading
		} catch (IOException e) {
			e.printStackTrace();
		}
		kristClientManager.startClient();
		receiver.sendMessage(Text.of("Reloaded KristPay."));
	}
	
	public boolean isUp() {
		return kristClientManager != null && kristClientManager.getKristClient() != null && kristClientManager.isUp();
	}
	
	public void loadDatabase() {
		if (database == null) {
			database = new Database(KristPay.INSTANCE.getConfigDir().resolve("kristpay.db").toFile());
			
			try {
				database.load();
			} catch (IOException e) {
				logger.error("Error loading KristPay database", e);
			}
			
			Task.builder().execute(() -> this.getDatabase().save())
				.async().interval(30, TimeUnit.SECONDS)
				.name("KristPay - Automatic database save")
				.submit(KristPay.INSTANCE);
		}
	}
}
