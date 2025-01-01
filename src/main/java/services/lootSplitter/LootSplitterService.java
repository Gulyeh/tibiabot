package services.lootSplitter;

import com.google.common.util.concurrent.AtomicDouble;
import services.lootSplitter.model.SplitLootModel;
import services.lootSplitter.model.SplittingMember;
import services.lootSplitter.model.TransferData;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class LootSplitterService {
    private final String splitter = ":";

    public SplitLootModel splitLoot(String session, String spot) {
        String[] splittedSession = splitSession(session);
        SplitLootModel model = getSessionData(splittedSession);
        model.setMembers(splitMembers(splittedSession));
        model.setNegative(parseInteger(model.getBalance()) < 0);
        model.setSpotName(spot);

        calculateLootPerHour(model);
        determineTransfers(model);
        calculatePercentage(model);

        return model.validateModel() ? model : null;
    }

    private List<SplittingMember> splitMembers(String[] session) {
        List<SplittingMember> members = new ArrayList<>();
        for(int i = 0; i < session.length; i++) {
            if (session[i].contains(":")) continue;
            SplittingMember member = new SplittingMember();
            try {
                member.setName(session[i].replace("(Leader)", "").trim());
                member.setLoot(session[++i].split(splitter)[1].trim());
                member.setSupplies(session[++i].split(splitter)[1].trim());
                member.setBalance(session[++i].split(splitter)[1].trim());
                member.setDamage(session[++i].split(splitter)[1].trim());
                member.setHealing(session[++i].split(splitter)[1].trim());
            } catch (Exception ignore) {}
            members.add(member);
        }
        return members;
    }

    private SplitLootModel getSessionData(String[] session) {
        SplitLootModel model = new SplitLootModel();
        for(String line : session) {
            if (line.startsWith("Session data: From")) {
                String[] timeParts = line.split("From | to ");
                model.setHuntFrom(timeParts[1].trim());
                model.setHuntTo(timeParts[2].trim());
            }
            else if (line.startsWith("Session:"))
                model.setHuntTime(line.split("Session:")[1].trim());
            else if (line.startsWith("Loot Type:"))
                model.setLootType(line.split(splitter)[1].trim());
            else if (line.startsWith("Loot:") && model.getLoot() == null)
                model.setLoot(line.split(splitter)[1].trim());
            else if (line.startsWith("Supplies:") && model.getSupplies() == null)
                model.setSupplies(line.split(splitter)[1].trim());
            else if (line.startsWith("Balance:") && model.getBalance() == null)
                model.setBalance(line.split(splitter)[1].trim());
        }
        return model;
    }

    private String[] splitSession(String session) {
        return session.split("\n");
    }

    private void calculateLootPerHour(SplitLootModel model) {
        String[] timeParts = model.getHuntTime().split("[:h]");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        double totalHours = hours + (minutes / 60.0);
        double lootPerHour = parseInteger(model.getLoot()) / totalHours;
        int value = (int) lootPerHour;
        model.setLootPerHour(formatToSessionBalance(value));
    }

    private String formatToSessionBalance(int balance) {
        DecimalFormat formatter = new DecimalFormat("#,###", new DecimalFormatSymbols(Locale.US));
        return formatter.format(balance);
    }

    private void calculatePercentage(SplitLootModel model) {
        double lootTotal = parseInteger(model.getLoot()).doubleValue();
        double suppliesTotal = parseInteger(model.getSupplies()).doubleValue();
        AtomicDouble damageTotal = new AtomicDouble();
        AtomicDouble healingTotal = new AtomicDouble();

        model.getMembers().forEach(x -> {
            damageTotal.addAndGet(parseInteger(x.getDamage()).doubleValue());
            healingTotal.addAndGet(parseInteger(x.getHealing()).doubleValue());
        });

        model.getMembers().forEach(x -> {
            double loot = parseInteger(x.getLoot()).doubleValue();
            double damage = parseInteger(x.getDamage()).doubleValue();
            double healing = parseInteger(x.getHealing()).doubleValue();
            double supplies = parseInteger(x.getSupplies()).doubleValue();

            x.setLootPercentage((loot/ lootTotal) * 100);
            x.setDamagePercentage((damage / damageTotal.get()) * 100);
            x.setHealingPercentage((healing / healingTotal.get()) * 100);
            x.setSuppliesPercentage((supplies / suppliesTotal) * 100);
        });
    }

    private void determineTransfers(SplitLootModel model) {
        int equalShare = parseInteger(model.getBalance()) / model.getMembers().size();
        model.setIndividualBalance(formatToSessionBalance(equalShare));

        Map<SplittingMember, Integer> neededTransfers = new HashMap<>();
        Map<SplittingMember, Integer> needsToTransfer = new HashMap<>();

        for(SplittingMember member : model.getMembers()) {
            int balance = parseInteger(member.getBalance()) - equalShare;
            if(balance > 0) {
                needsToTransfer.put(member, balance);
                continue;
            }
            neededTransfers.put(member, balance * -1);
        }

        for(SplittingMember transferFrom : needsToTransfer.keySet()) {
            int transferAmount = needsToTransfer.get(transferFrom);

            for(SplittingMember transferTo : neededTransfers.keySet()) {
                int amountNeeded = neededTransfers.get(transferTo);
                if(amountNeeded == 0) continue;

                int actualTransfer = Math.min(transferAmount, amountNeeded);
                transferFrom.getTransfers().add(new TransferData(transferTo.getName(), String.valueOf(actualTransfer)));
                transferAmount -= actualTransfer;
                amountNeeded -= actualTransfer;
                neededTransfers.put(transferTo, amountNeeded);
                if(transferAmount == 0) break;
            }
        }
    }

    private Integer parseInteger(String data) {
        return Integer.parseInt(data.replace(",", ""));
    }
}
