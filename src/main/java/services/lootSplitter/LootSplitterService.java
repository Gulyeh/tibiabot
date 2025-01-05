package services.lootSplitter;

import apis.WebClient;
import com.google.common.util.concurrent.AtomicDouble;
import discord4j.discordjson.json.ComponentData;
import services.lootSplitter.model.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LootSplitterService extends WebClient {
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

    public String getHuntSession(String url) {
        return sendRequest(getCustomRequest(url));
    }

    public HuntComparatorModel compareHunts(SplitLootModel currentHunt, List<SplitLootModel> previousHunts) {
        HuntComparatorModel comparatorModel = new HuntComparatorModel();
        int totalLoot = 0, totalSupplies = 0;
        double totalTime = 0, currentHuntTime = calculateTotalHours(currentHunt);
        ConcurrentHashMap<String, List<SplittingMember>> membersMap = new ConcurrentHashMap<>();

        for(SplitLootModel model : previousHunts) {
            totalLoot += parseInteger(model.getLoot());
            totalSupplies += parseInteger(model.getSupplies());
            totalTime += calculateTotalHours(model);
            model.getMembers().forEach(x -> {
                List<SplittingMember> memberList = membersMap.get(x.getName());
                if(memberList == null) memberList = new ArrayList<>();
                memberList.add(x);
                membersMap.put(x.getName(), memberList);
            });
        }

        setCompareHuntData(comparatorModel, currentHunt, totalLoot, totalSupplies, totalTime, currentHuntTime);
        comparatorModel.setComparedMembers(compareMembers(membersMap, currentHunt, totalTime, currentHuntTime));

        return comparatorModel;
    }

    private List<SplittingMember> splitMembers(String[] session) {
        List<SplittingMember> members = new ArrayList<>();
        for(int i = 0; i < session.length; i++) {
            if (session[i].contains(":") || session[i].isEmpty()) continue;
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
        double totalHours = calculateTotalHours(model);
        double lootPerHour = parseInteger(model.getLoot()) / totalHours;
        int value = cutDoubleDecimals(lootPerHour);
        model.setLootPerHour(formatToSessionBalance(value));
    }

    private double calculateTotalHours(SplitLootModel model) {
        String[] timeParts = model.getHuntTime().split("[:h]");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        return hours + (minutes / 60.0);
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


    private List<ComparatorMember> compareMembers(ConcurrentHashMap<String, List<SplittingMember>> membersMap, SplitLootModel currentHunt,
                                                  double totalHuntTime, double currentHuntTime) {
        List<ComparatorMember> members = new ArrayList<>();

        membersMap.forEach((k, v) -> {
            ComparatorMember member = new ComparatorMember();
            SplittingMember currentHuntMember = currentHunt.getMembers().stream().filter(x -> x.getName().equals(k)).findFirst().get();

            member.setName(k);
            AtomicInteger totalHealing = new AtomicInteger(),
                    totalDamage = new AtomicInteger(),
                    totalLoot = new AtomicInteger(),
                    totalSupplies = new AtomicInteger();

            v.forEach(x -> {
                totalHealing.addAndGet(parseInteger(x.getHealing()));
                totalDamage.addAndGet(parseInteger(x.getDamage()));
                totalLoot.addAndGet(parseInteger(x.getLoot()));
                totalSupplies.addAndGet(parseInteger(x.getSupplies()));
            });

            int avgLootPerHour = cutDoubleDecimals(totalLoot.get() / totalHuntTime),
                    avgSuppliesPerHour = cutDoubleDecimals(totalSupplies.get() / totalHuntTime),
                    avgDamagePerHour = cutDoubleDecimals(totalDamage.get() / totalHuntTime),
                    avgHealingPerHour = cutDoubleDecimals(totalHealing.get() / totalHuntTime),
                    lootPerHour = cutDoubleDecimals(parseInteger(currentHuntMember.getLoot()) / currentHuntTime),
                    suppliesPerHour = cutDoubleDecimals(parseInteger(currentHuntMember.getSupplies()) / currentHuntTime),
                    damagePerHour = cutDoubleDecimals(parseInteger(currentHuntMember.getDamage()) / currentHuntTime),
                    healingPerHour = cutDoubleDecimals(parseInteger(currentHuntMember.getHealing()) / currentHuntTime);

            member.setAvgLootPerHour(formatToSessionBalance(avgLootPerHour));
            member.setAvgSuppliesPerHour(formatToSessionBalance(avgSuppliesPerHour));
            member.setAvgDamagePerHour(formatToSessionBalance(avgDamagePerHour));
            member.setAvgHealingPerHour(formatToSessionBalance(avgHealingPerHour));

            member.setLootPerHour(formatToSessionBalance(lootPerHour));
            member.setSuppliesPerHour(formatToSessionBalance(suppliesPerHour));
            member.setDamagePerHour(formatToSessionBalance(damagePerHour));
            member.setHealingPerHour(formatToSessionBalance(healingPerHour));

            member.setDamageDifference(formatToSessionBalance(damagePerHour - avgDamagePerHour));
            member.setHealingDifference(formatToSessionBalance(healingPerHour - avgHealingPerHour));
            member.setLootDifference(formatToSessionBalance(lootPerHour - avgLootPerHour));
            member.setSuppliesDifference(formatToSessionBalance(suppliesPerHour - avgSuppliesPerHour));

            member.setDamageDifferencePercentage(((double) damagePerHour / avgDamagePerHour) * 100 - 100);
            member.setHealingDifferencePercentage(((double) healingPerHour / avgHealingPerHour) * 100 - 100);
            member.setLootDifferencePercentage(((double) lootPerHour / avgLootPerHour) * 100 - 100);
            member.setSuppliesDifferencePercentage(((double) suppliesPerHour / avgSuppliesPerHour) * 100 - 100);

            members.add(member);
        });

        return members;
    }

    private void setCompareHuntData(HuntComparatorModel comparatorModel, SplitLootModel currentHunt, int totalLoot, int totalSupplies,
                                    double totalTime, double currentHuntTime) {
        int avgLootPerHour = cutDoubleDecimals(totalLoot / totalTime),
                avgSuppliesPerHour = cutDoubleDecimals(totalSupplies / totalTime),
                avgBalancePerHour = avgLootPerHour - avgSuppliesPerHour,
                avgIndividualBalancePerHour = avgBalancePerHour / currentHunt.getMembers().size(),
                lootPerHour = parseInteger(currentHunt.getLootPerHour()),
                suppliesPerHour = cutDoubleDecimals(parseInteger(currentHunt.getSupplies()) / currentHuntTime),
                balancePerHour = cutDoubleDecimals(parseInteger(currentHunt.getBalance()) / currentHuntTime),
                individualBalancePerHour = balancePerHour / currentHunt.getMembers().size();

        comparatorModel.setAvgLootPerHour(formatToSessionBalance(avgLootPerHour));
        comparatorModel.setAvgSuppliesPerHour(formatToSessionBalance(avgSuppliesPerHour));
        comparatorModel.setAvgBalancePerHour(formatToSessionBalance(avgBalancePerHour));
        comparatorModel.setAvgIndividualBalancePerHour(formatToSessionBalance(avgIndividualBalancePerHour));

        comparatorModel.setLootPerHour(formatToSessionBalance(lootPerHour));
        comparatorModel.setSuppliesPerHour(formatToSessionBalance(suppliesPerHour));
        comparatorModel.setBalancePerHour(formatToSessionBalance(balancePerHour));
        comparatorModel.setIndividualBalancePerHour(formatToSessionBalance(individualBalancePerHour));

        comparatorModel.setLootPerHourDifference(formatToSessionBalance(lootPerHour - avgLootPerHour));
        comparatorModel.setBalancePerHourDifference(formatToSessionBalance(balancePerHour - avgBalancePerHour));
        comparatorModel.setSuppliesPerHourDifference(formatToSessionBalance(suppliesPerHour - avgSuppliesPerHour));
        comparatorModel.setIndividualBalancePerHourDifference(formatToSessionBalance(individualBalancePerHour - avgIndividualBalancePerHour));

        comparatorModel.setLootPerHourDifferencePercentage(((double) lootPerHour / avgLootPerHour) * 100 - 100);
        comparatorModel.setBalancePerHourDifferencePercentage(((double) balancePerHour / avgBalancePerHour) * 100 - 100);
        comparatorModel.setSuppliesPerHourDifferencePercentage(((double) suppliesPerHour / avgSuppliesPerHour) * 100 - 100);
        comparatorModel.setIndividualBalancePerHourDifferencePercentage(((double) individualBalancePerHour / avgIndividualBalancePerHour) * 100 - 100);
    }

    private Integer parseInteger(String data) {
        return Integer.parseInt(data.replace(",", ""));
    }

    private Integer cutDoubleDecimals(double value) {
        DecimalFormat decimalFormat = new DecimalFormat("#");
        return Integer.parseInt(decimalFormat.format(Math.round(value)));
    }

    @Override
    protected String getUrl() {
        return null;
    }
}
