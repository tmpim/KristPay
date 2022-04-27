package pw.lemmmy.kristpay.krist;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.commands.CommandHelpers;
import pw.lemmmy.kristpay.database.AccountDatabase;
import pw.lemmmy.kristpay.economy.KristAccount;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.spongepowered.api.text.Text.of;
import static org.spongepowered.api.text.format.TextColors.GREEN;
import static org.spongepowered.api.text.format.TextColors.YELLOW;
import static pw.lemmmy.kristpay.krist.KristAPI.getKristNode;

public class DepositMiningManager {
    private static final Logger LOG = KristPay.INSTANCE.getLogger();

    private static final int LOOKUP_BATCH_SIZE = 100;

    private final AccountDatabase accountDatabase;
    private final MasterWallet masterWallet;
    private final DepositManager depositManager;

    private final UserStorageService userStorage;

    @Getter private Instant nextRun = Instant.now().plus(2, ChronoUnit.MINUTES);

    public DepositMiningManager(
        AccountDatabase accountDatabase,
        MasterWallet masterWallet,
        DepositManager depositManager
    ) {
        this.accountDatabase = accountDatabase;
        this.masterWallet = masterWallet;
        this.depositManager = depositManager;

        userStorage = Sponge.getServiceManager().provide(UserStorageService.class).get();
    }

    public void handleMiningDeposit(String address, int amount) {
        Optional<KristAccount> optAccount = findAccountByAddress(address);
        if (!optAccount.isPresent() || amount <= 0) return;
        KristAccount account = optAccount.get();

        Optional<Player> optPlayer = Sponge.getServer().getPlayer(UUID.fromString(account.getOwner()));
        if (optPlayer.isPresent()) {
            Player player = optPlayer.get();
            if (!player.isOnline()) return;

            long duration = nextRun.toEpochMilli() - System.currentTimeMillis();

            player.sendMessage(Text.builder()
                .append(of(GREEN, "Your mining deposit of "))
                .append(CommandHelpers.formatKrist(amount, true))
                .append(of(GREEN, " was received by the server and your account will be credited in "))
                .append(of(YELLOW, DurationFormatUtils.formatDurationWords(duration, true, true)))
                .append(of(GREEN, "."))
                .build());
        }
    }

    private Optional<KristAccount> findAccountByAddress(String address) {
        return accountDatabase.getAccounts().values().stream()
            .filter(kristAccount -> kristAccount.getDepositWallet().getAddress().equalsIgnoreCase(address))
            .findFirst();
    }

    private Map<String, KristAccount> getDepositMiningAddressAccountMap() {
        Map<String, KristAccount> map = new HashMap<>();
        accountDatabase.getAccounts().values().forEach(account -> {
            if (account.getDepositWallet().getAddress() != null) {
                map.put(account.getDepositWallet().getAddress(), account);
            }
        });
        return map;
    }

    public void syncDepositMiningWallets() {
        int nextRunMins = KristPay.INSTANCE.getConfig().getNode().isDebugTiming() ? 1 : 30;
        nextRun = Instant.now().plus(nextRunMins, ChronoUnit.MINUTES);

        Map<String, KristAccount> accountMap = getDepositMiningAddressAccountMap();
        List<KristAccount> accounts = new ArrayList<>(accountDatabase.getAccounts().values());
        KristPay.INSTANCE.getLogger().info("Syncing {} deposit mining wallets.", accounts.size());

        Map<KristAccount, Integer> toTransfer = new HashMap<>();
        AtomicInteger totalToTransfer = new AtomicInteger();

        // Split addresses into batches of 100. This is to avoid hitting the lookup API limit of 128 addresses per
        // request.
        for (List<KristAccount> batch : ListUtils.partition(accounts, LOOKUP_BATCH_SIZE)) {
            String addressList = batch.stream()
                .map(a -> a.getDepositWallet().getAddress())
                .collect(Collectors.joining(","));

            try {
                HttpResponse<JsonNode> res = Unirest.get(getKristNode() + "/lookup/addresses/" + addressList)
                    .asJson();

                JSONObject body = res.getBody().getObject();
                if (!body.getBoolean("ok")) throw new KristException(body.getString("error"));

                JSONObject addresses = body.getJSONObject("addresses");
                addresses.keys().forEachRemaining(key -> {
                    Object value = addresses.get(key);
                    if (value instanceof JSONObject) {
                        JSONObject address = (JSONObject) value;
                        String addressStr = address.optString("address", key);
                        int amount = address.optInt("balance", 0);

                        if (amount > 0) {
                            toTransfer.put(accountMap.get(addressStr), amount);
                            totalToTransfer.addAndGet(amount);
                        }
                    }
                });
            } catch (UnirestException e) {
                LOG.error("Error syncing deposit mining wallets. Address list: " + addressList, e);
                // Still allow the rest of the requests to be attempted
            }
        }

        if (totalToTransfer.get() <= 0) {
            LOG.info("Nothing to transfer.");
            return;
        }

        LOG.info("Transferring {} KST total from {} deposit mining wallets.", totalToTransfer.get(), toTransfer.size());

        for (Map.Entry<KristAccount, Integer> entry : toTransfer.entrySet()) {
            KristAccount account = entry.getKey();
            int amount = entry.getValue();

            LOG.info(" - "
                + account.getDepositWallet().getAddress()
                + ": "
                + amount + " KST "
                + "(owner: " + userStorage.get(UUID.fromString(account.getOwner()))
                .map(User::getName)
                .orElse(account.getOwner())
                + ")");

            try {
                syncDepositWallet(account, amount);
            } catch (Exception e) {
                LOG.error("Error transferring KST from deposit wallet " + account.getDepositWallet().getAddress(), e);
            }
        }
    }

    /** Transfers Krist from the deposit mining wallet to the master wallet and credits the account. */
    private void syncDepositWallet(KristAccount account, int amount) {
        Wallet depositWallet = account.getDepositWallet();
        if (depositWallet == null) return;

        LOG.info("Transferring {} KST from {} to the master wallet.", amount, depositWallet.getAddress());
        depositWallet.transfer(masterWallet.getAddress(), amount, null).handle((tx, ex) -> {
            if (ex != null) {
                LOG.error("Error transferring {} KST from {} to the master wallet.", amount,
                    depositWallet.getAddress(), ex);
                return tx;
            }

            // Transaction successful, credit the account and notify the user
            depositManager.handleDeposit(account, null, null, amount);
            return tx;
        });
    }
}
