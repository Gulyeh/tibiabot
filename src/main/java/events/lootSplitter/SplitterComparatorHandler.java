package events.lootSplitter;

import discord.Connector;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ComponentData;
import discord4j.rest.util.Color;
import events.abstracts.InteractionEvent;
import lombok.extern.slf4j.Slf4j;
import observers.InteractionObserver;
import reactor.core.publisher.Mono;
import services.lootSplitter.LootSplitterService;
import services.lootSplitter.model.ComparatorMember;
import services.lootSplitter.model.HuntComparatorModel;
import services.lootSplitter.model.SplitLootModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static discord.Connector.client;
import static discord.messages.GetMessages.getChannelMessages;
import static utils.Emojis.getBlankEmoji;

@Slf4j
public class SplitterComparatorHandler extends InteractionEvent {
    private final LootSplitterService lootSplitterService;

    protected SplitterComparatorHandler(LootSplitterService lootSplitterService, InteractionObserver observer) {
        super("comparePreviousHunts", observer);
        this.lootSplitterService = lootSplitterService;
    }

    @Override
    public void executeEvent() {
        client.on(ButtonInteractionEvent.class, event -> {
            if (!event.getCustomId().contains(getButtonId()) || !observer.add(getMessage(event).getId())) return Mono.empty();

            Message message = getMessage(event);
            event.deferReply().subscribe();
            List<ComponentData> buttons = getInteractionButtons(message);
            message.edit().withComponentsOrNull(splitActionRows(toggleLockButton(buttons, true)))
                    .subscribe();

            try {
                List<Message> msgs = getChannelMessages((GuildMessageChannel) event.getInteraction().getChannel().block(), message.getTimestamp())
                        .collectList()
                        .block();
                if (msgs == null) throw new Exception("No messages found");

                List<Message> embeddedMsgs = msgs.stream().filter(x ->
                                x.getAuthor().get().getId().equals(Snowflake.of(Connector.getId())) &&
                                x.getEmbeds().size() == 1 &&
                                x.getAttachments().size() == 1 &&
                                x.getEmbeds().get(0).getTitle().get().equals(message.getEmbeds().get(0).getTitle().get())).toList();

                List<EmbedCreateSpec> comparedMessages = buildCompareData(message, embeddedMsgs);
                message.edit().withComponentsOrNull(splitActionRows(updateButtons(buttons, !embeddedMsgs.isEmpty())))
                        .subscribe();
                if (embeddedMsgs.isEmpty())
                    return event.createFollowup().withEphemeral(true)
                            .withContent("There is no previous data to compare with");
                return event.createFollowup().withEmbeds(comparedMessages);
            } catch (Exception e) {
                message.edit().withComponentsOrNull(splitActionRows(toggleLockButton(buttons, false)))
                        .subscribe();
                return event.createFollowup("Something went wrong").withEphemeral(true);
            } finally {
                observer.remove(message.getId());
            }
        }).subscribe();
    }

    private List<Button> updateButtons(List<ComponentData> buttons, boolean isValid) {
        List<Button> buttonsList = new ArrayList<>();
        for(ComponentData component : buttons) {
            if(!component.customId().get().equals(getButtonId())) {
                buttonsList.add((Button) Button.fromData(component));
                continue;
            }

            Button btn = Button.primary(component.customId().get(), isValid ? "Compared"  : "No data to compare");
            btn = btn.disabled();
            buttonsList.add(btn);
        }
        return buttonsList;
    }

    private List<EmbedCreateSpec> buildCompareData(Message currentHunt, List<Message> previousHuntSessions) {
        List<SplitLootModel> previousHunts = new ArrayList<>();
        int maxHuntsToCompare = 5;
        String session = lootSplitterService.getHuntSession(currentHunt.getAttachments().get(0).getUrl());
        SplitLootModel currentHuntModel = lootSplitterService.splitLoot(session, "");

        for(Message msg : previousHuntSessions) {
            if(previousHunts.size() >= maxHuntsToCompare) break;
            session = lootSplitterService.getHuntSession(msg.getAttachments().get(0).getUrl());
            SplitLootModel previousHuntModel = lootSplitterService.splitLoot(session, "");
            if(containsAllMembers(currentHuntModel, previousHuntModel))
                previousHunts.add(previousHuntModel);
        }

        HuntComparatorModel comparedData = lootSplitterService.compareHunts(currentHuntModel, previousHunts);
        List<EmbedCreateFields.Field> fields = fieldsBuilder(comparedData);

        return createEmbeddedMessages(fields,
                previousHunts.size() + " Hunt session(s) comparison - " + comparedData.getComparedMembers().size() + " members (per hour)",
                "",
                "",
                "",
                Color.SEA_GREEN,
                EmbedCreateFields.Footer.of("Comparison values are approx. only", null));
    }

    private List<EmbedCreateFields.Field> fieldsBuilder(HuntComparatorModel data) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>(buildHuntInfoComparison(data));
        fields.add(emptyField(false));
        fields.add(emptyField(false));
        fields.add(EmbedCreateFields.Field.of("", "**Members**", false));
        fields.addAll(buildHuntMembersComparison(data.getComparedMembers()));
        return fields;
    }

    private List<EmbedCreateFields.Field> buildHuntInfoComparison(HuntComparatorModel data) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();
        String lootBuilder = "Loot: **" + data.getLootPerHour() + "**\n" + getBlankEmoji() +
                "Avg. loot: **" + data.getAvgLootPerHour() + "**\n" + getBlankEmoji() +
                "Difference: **" + data.getLootPerHourDifference() + "** (" + data.getLootPerHourDifferencePercentage() + ")";
        String suppliesBuilder = "Supplies: **" + data.getSuppliesPerHour() + "**\n" + getBlankEmoji() +
                "Avg. supplies: **" + data.getAvgSuppliesPerHour() + "**\n" + getBlankEmoji() +
                "Difference: **" + data.getSuppliesPerHourDifference() + "** (" + data.getSuppliesPerHourDifferencePercentage() + ")";
        String balanceBuilder = "Balance: **" + data.getBalancePerHour() + "**\n" + getBlankEmoji() +
                "Avg. balance: **" + data.getAvgBalancePerHour() + "**\n" + getBlankEmoji() +
                "Difference: **" + data.getBalancePerHourDifference() + "** (" + data.getBalancePerHourDifferencePercentage() + ")";
        String individualBalanceBuilder = "Individual balance: **" + data.getIndividualBalancePerHour() + "**\n" + getBlankEmoji() +
                "Avg. individual balance: **" + data.getAvgIndividualBalancePerHour() + "**\n" + getBlankEmoji() +
                "Difference: **" + data.getIndividualBalancePerHourDifference() + "** (" + data.getIndividualBalancePerHourDifferencePercentage() + ")";
        fields.add(EmbedCreateFields.Field.of("", String.join("\n", lootBuilder, suppliesBuilder), true));
        fields.add(EmbedCreateFields.Field.of("", String.join("\n", balanceBuilder, individualBalanceBuilder), true));
        fields.add(emptyField(true));
        return fields;
    }

    private List<EmbedCreateFields.Field> buildHuntMembersComparison(List<ComparatorMember> members) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();
        for(ComparatorMember member : members) {
            String loot = getBlankEmoji() + "Loot: **" + member.getLootPerHour() + "**\n" +
                    getBlankEmoji() + getBlankEmoji() + "Avg. loot: **" + member.getAvgLootPerHour() + "**\n" +
                    getBlankEmoji() + getBlankEmoji() + "Difference: **" + member.getLootDifference() + "** (" + member.getLootDifferencePercentage() + ")";
            String supplies = getBlankEmoji() + "Supplies: **" + member.getSuppliesPerHour() + "**\n" +
                    getBlankEmoji() + getBlankEmoji() + "Avg. supplies: **" + member.getAvgSuppliesPerHour() + "**\n" +
                    getBlankEmoji() + getBlankEmoji() + "Difference: **" + member.getSuppliesDifference() + "** (" + member.getSuppliesDifferencePercentage() + ")";
            String damage = getBlankEmoji() + "Damage: **" + member.getDamagePerHour() + "**\n" +
                    getBlankEmoji() + getBlankEmoji() + "Avg. damage: **" + member.getAvgDamagePerHour() + "**\n" +
                    getBlankEmoji() + getBlankEmoji() + "Difference: **" + member.getDamageDifference() + "** (" + member.getDamageDifferencePercentage() + ")";
            String healing = getBlankEmoji() + "Healing: **" + member.getHealingPerHour() + "**\n" +
                    getBlankEmoji() + getBlankEmoji() + "Avg. healing: **" + member.getAvgHealingPerHour() + "**\n" +
                    getBlankEmoji() + getBlankEmoji() + "Difference: **" + member.getHealingDifference() + "** (" + member.getHealingDifferencePercentage() + ")";
            fields.add(EmbedCreateFields.Field.of("‣ " + member.getName(), String.join("\n", loot, supplies), true));
            fields.add(EmbedCreateFields.Field.of(getBlankEmoji(), String.join("\n", damage, healing), true));
            fields.add(emptyField(true));
        }
        return fields;
    }

    private boolean containsAllMembers(SplitLootModel currentHuntModel, SplitLootModel previousHuntModel) {
        Set<String> previousHuntMemberNames = previousHuntModel.getMembers().stream()
                .map(member -> member.getName().toLowerCase())
                .collect(Collectors.toSet());

        return currentHuntModel.getMembers().stream().map(member -> member.getName().toLowerCase())
                .allMatch(previousHuntMemberNames::contains);
    }

    @Override
    public String getEventName() {
        return "";
    }

    @Override
    protected void executeEventProcess() {
    }
}
