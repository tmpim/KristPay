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
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.format.TextColors;
import pw.lemmmy.kristpay.commands.*;
import pw.lemmmy.kristpay.config.Config;
import pw.lemmmy.kristpay.config.ConfigLoader;
import pw.lemmmy.kristpay.database.AccountDatabase;
import pw.lemmmy.kristpay.database.Database;
import pw.lemmmy.kristpay.database.FaucetReward;
import pw.lemmmy.kristpay.economy.*;
import pw.lemmmy.kristpay.krist.DepositManager;
import pw.lemmmy.kristpay.krist.KristClientManager;
import pw.lemmmy.kristpay.krist.MasterWallet;
import pw.lemmmy.kristpay.prometheus.PrometheusManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Plugin(id = "kristpay", name = "KristPay", version = "2.5.1")
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
	private AccountDatabase accountDatabase;
	private Database database;
	private DepositManager depositManager;
	private KristCurrency currency = new KristCurrency();
	
	private KristEconomy economyService = new KristEconomy();
	
	private PrometheusManager prometheusManager;
	
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
		Sponge.getServiceManager().setProvider(this, EconomyService.class, economyService);
	}
	
	@Listener
	public void init(GameInitializationEvent event) {
		loadDatabase();
		
		kristClientManager = new KristClientManager();
		new Thread(kristClientManager).start();
		
		if (config.getPrometheus().isEnabled()) {
			prometheusManager = new PrometheusManager();
			prometheusManager.startServer();
		}
	}
	
	@Listener
	public void reload(GameReloadEvent event, @First MessageReceiver receiver) {
		receiver.sendMessage(Text.of("Reloading KristPay."));
		
		kristClientManager.stopClient();
		
		if (prometheusManager != null) {
			prometheusManager.stopServer();
			prometheusManager = null;
		}
		
		try {
			accountDatabase.load();
			database.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		kristClientManager.startClient();
		
		if (config.getPrometheus().isEnabled()) {
			prometheusManager = new PrometheusManager();
			prometheusManager.startServer();
		}
		
		receiver.sendMessage(Text.of("Reloaded KristPay."));
	}
	
	@Listener
	public void serverStarted(GameStartedServerEvent event) {
		Sponge.getCommandManager().register(this, CommandBalance.SPEC, "balance", "bal");
		Sponge.getCommandManager().register(this, CommandMasterBal.SPEC, "masterbalance", "masterbal");
		Sponge.getCommandManager().register(this, CommandPay.SPEC, "pay", "withdraw", "transfer");
		Sponge.getCommandManager().register(this, CommandDeposit.SPEC, "deposit");
		Sponge.getCommandManager().register(this, CommandSetBalance.SPEC, "setbalance", "setbal");
		Sponge.getCommandManager().register(this, CommandSetWelfare.SPEC, "setwelfare");
		Sponge.getCommandManager().register(this, CommandTransaction.SPEC, "transaction", "tx");
		Sponge.getCommandManager().register(this, CommandTransactions.SPEC, "transactions", "txs", "alltx");
		Sponge.getCommandManager().register(this, CommandFaucet.SPEC, "faucet", "redeem");
	}
	
	@Listener
	public void serverStopped(GameStoppedServerEvent event) {
		if (accountDatabase != null) accountDatabase.save();
		if (isUp()) kristClientManager.stopClient();
		
		if (prometheusManager != null) {
			prometheusManager.stopServer();
			prometheusManager = null;
		}
	}
	
	@Listener
	public void playerJoin(ClientConnectionEvent.Join event, @First MessageReceiver receiver) {
		Player player = event.getTargetEntity();
		
		if (economyService.hasAccount(player.getUniqueId())) { // dont create accounts for players who join then leave
			economyService.getOrCreateAccount(player.getUniqueId()).ifPresent(uniqueAccount -> {
				if (!(uniqueAccount instanceof KristAccount)) return;
				KristAccount account = (KristAccount) uniqueAccount;
				
				Task.builder()
					.execute(() -> {
						int unseenDeposit = account.getUnseenDeposit();
						int unseenTransfer = account.getUnseenTransfer();
						
						if (unseenDeposit > 0) player.sendMessage(Text.builder()
							.append(CommandHelpers.formatKrist(unseenDeposit))
							.append(Text.of(TextColors.GREEN, " was deposited into your account while you were offline."))
							.build());
						
						if (unseenTransfer > 0) player.sendMessage(Text.builder()
							.append(CommandHelpers.formatKrist(unseenTransfer))
							.append(Text.of(TextColors.GREEN, " was transferred into your account while you were offline."))
							.build());
						
						if (unseenDeposit > 0 || unseenTransfer > 0) player.sendMessage(Text.builder()
							.append(Text.of(TextColors.GREEN, "Run "))
							.append(Text.of(TextColors.AQUA, "/transactions"))
							.append(Text.of(TextColors.GREEN, " to see your recent transactions."))
							.onHover(TextActions.showText(Text.of(TextColors.AQUA, "/transactions")))
							.onClick(TextActions.runCommand("/transactions"))
							.build());
						
						account.setUnseenDeposit(0).setUnseenTransfer(0);
						getAccountDatabase().save();
					})
					.async()
					.delay(1, TimeUnit.SECONDS)
					.name("KristPay - Offline transaction notifications")
					.submit(INSTANCE);
				
				FaucetManager.handleLogin(player, account);
				WelfareManager.handleLogin(player, account);
			});
		}
	}
	
	public boolean isUp() {
		return kristClientManager != null && kristClientManager.getKristClient() != null && kristClientManager.isUp();
	}
	
	public void loadDatabase() {
		if (accountDatabase == null) {
			accountDatabase = new AccountDatabase(KristPay.INSTANCE.getConfigDir().resolve("kristpay.db").toFile());
			
			try {
				accountDatabase.load();
				depositManager = new DepositManager(accountDatabase, masterWallet);
			} catch (IOException e) {
				logger.error("Error loading KristPay account database", e);
			}
			
			Task.builder().execute(() -> this.getAccountDatabase().save())
				.async()
				.interval(config.getDatabase().getSaveInterval(), TimeUnit.SECONDS)
				.name("KristPay - Automatic account database save")
				.submit(INSTANCE);
			
			Task.builder().execute(() -> this.getAccountDatabase().syncWallets())
				.async()
				.delay(2, TimeUnit.MINUTES)
				.interval(config.getDatabase().getLegacyWalletSyncInterval(), TimeUnit.SECONDS)
				.name("KristPay - Legacy wallet sync")
				.submit(INSTANCE);
			
			Task.builder().execute(WelfareManager::checkAllOnlinePlayers)
				.async()
				.delay(1, TimeUnit.MINUTES)
				.interval(15, TimeUnit.MINUTES)
				.name("KristPay - Welfare auto-checker")
				.submit(INSTANCE);
		}
		
		if (database == null) {
			database = new Database();
			
			try {
				database.load();
			} catch (Exception e) {
				logger.error("Error loading KristPay database", e);
			}
		}
	}
}
