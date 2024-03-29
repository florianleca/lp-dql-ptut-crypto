package fr.lpdql.ptut.blocklytrader.runtest;

import fr.lpdql.ptut.blocklytrader.datasettings.DataSettingsService;
import fr.lpdql.ptut.blocklytrader.deserialisation.BlocklyJsonParser;
import net.minidev.json.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Service
public class RunTestService {

    public static SortedMap<String, Map<String, String>> transactions = new TreeMap<>();
    public static Map.Entry<String, Map<String, String>> currentEntry;
    public static double currentCryptoBalance;
    public static double currentDeviseBalance;
    public static double exchangeFees;
    public static String firstOpen;
    public static String lastClose;
    private final DataSettingsService dataSettingsService;

    @Autowired
    public RunTestService(DataSettingsService dataSettingsService) {
        this.dataSettingsService = dataSettingsService;
    }

    public static void addTransaction(String type, double cryptoAmount, double currencyAmount, double rate) {
        Map<String, String> map = new HashMap<>();
        map.put("type", type);
        map.put("crypto_amount", String.valueOf(cryptoAmount));
        map.put("currency_amount", String.valueOf(currencyAmount));
        map.put("rate", String.valueOf(rate));
        String timestamp = currentEntry.getKey();
        transactions.put(timestamp, map);
    }

    private static void setStaticProperties(String cryptoBalance, String deviseBalance, String exchangeFees,
                                            SortedMap<String, Map<String, String>> klinesJson) {
        if (Double.parseDouble(cryptoBalance) < 0 | Double.parseDouble(deviseBalance) < 0 | Double.parseDouble(
                exchangeFees) < 0) {
            throw new IllegalArgumentException();
        }
        RunTestService.currentCryptoBalance = Double.parseDouble(cryptoBalance);
        RunTestService.currentDeviseBalance = Double.parseDouble(deviseBalance);
        RunTestService.exchangeFees = Double.parseDouble(exchangeFees);
        RunTestService.firstOpen = klinesJson.get(klinesJson.firstKey()).get("open");
        RunTestService.lastClose = klinesJson.get(klinesJson.lastKey()).get("close");
        RunTestService.transactions = new TreeMap<>();
    }

    public Map<Object, Object> getTestResult(String blocklyJson, String cryptoBalance, String deviseBalance,
                                             String exchangeFees) throws ParseException, IllegalArgumentException {
        SortedMap<String, Map<String, String>> klinesJson = dataSettingsService.getCurrentUserDataSet();
        RunTestService.setStaticProperties(cryptoBalance, deviseBalance, exchangeFees, klinesJson);
        for (Map.Entry<String, Map<String, String>> entry : klinesJson.entrySet()) {
            RunTestService.currentEntry = entry;
            BlocklyJsonParser blocklyJsonParser = new BlocklyJsonParser(blocklyJson);
            blocklyJsonParser.processEachBlock();
        }
        return miseEnFormeResult(cryptoBalance, deviseBalance);
    }

    public Map<Object, Object> miseEnFormeResult(String cryptoBalance, String deviseBalance) {
        Map<String, String> balances = createBalanceJson(cryptoBalance, deviseBalance);
        Map<Object, Object> result = new HashMap<>();
        result.put("balances", balances);
        result.put("transactions", transactions);
        return result;
    }

    public Map<String, String> createBalanceJson(String cryptoBalance, String deviseBalance) {
        Map<String, String> balances = new HashMap<>();
        double newValue = currentDeviseBalance + currentCryptoBalance * Double.parseDouble(lastClose);
        double previousValue = Double.parseDouble(deviseBalance) + Double.parseDouble(
                cryptoBalance) * Double.parseDouble(firstOpen);
        balances.put("new_crypto", String.valueOf(currentCryptoBalance));
        balances.put("new_currency", String.valueOf(currentDeviseBalance));
        balances.put("previous_crypto", cryptoBalance);
        balances.put("previous_currency", deviseBalance);
        balances.put("new_rate", lastClose);
        balances.put("previous_rate", firstOpen);
        balances.put("new_value", String.valueOf(newValue));
        balances.put("previous_value", String.valueOf(previousValue));
        balances.put("result", String.valueOf(newValue - previousValue));
        return balances;
    }

}
