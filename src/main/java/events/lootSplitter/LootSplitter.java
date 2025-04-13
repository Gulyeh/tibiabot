package events.lootSplitter;

import discord.Connector;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionPresentModalSpec;
import discord4j.core.spec.MessageCreateFields;
import discord4j.discordjson.json.EmbedData;
import discord4j.rest.util.Color;
import events.abstracts.InteractionEvent;
import events.utils.EventName;
import lombok.extern.slf4j.Slf4j;
import observers.InteractionObserver;
import reactor.core.publisher.Mono;
import services.lootSplitter.LootSplitterService;
import services.lootSplitter.model.SplitLootModel;
import services.lootSplitter.model.SplittingMember;
import services.lootSplitter.model.TransferData;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static builders.commands.names.CommandsNames.splitLootCommand;
import static discord.Connector.client;
import static discord.messages.GetMessages.getChannelMessages;
import static utils.Emojis.getBlankEmoji;
import static utils.Emojis.getCoinEmoji;

@Slf4j
public class LootSplitter extends InteractionEvent {
    private final LootSplitterService service;
    private final SplitterTransfersHandler splitterTransfersHandler;
    private final SplitterComparatorHandler splitterComparatorHandler;
    private final String splitModalId = "lootSplitterModal", spotModalId = "spotModal", splitterModalId = "splitterModal";

    public LootSplitter() {
        super("", new InteractionObserver());
        service = new LootSplitterService();
        splitterTransfersHandler = new SplitterTransfersHandler(observer);
        splitterComparatorHandler = new SplitterComparatorHandler(service, observer);
    }

    private void subscribeCommandEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(splitLootCommand.getCommandName())) return Mono.empty();
                return event.presentModal(InteractionPresentModalSpec.builder()
                        .title("Loot Splitter")
                        .customId(splitModalId)
                        .addAllComponents(Arrays.asList(
                                ActionRow.of(TextInput.small(spotModalId, "Spot name")
                                        .placeholder("Hunt spot").required(false)),
                                ActionRow.of(TextInput.paragraph(splitterModalId, "Session data")
                                        .placeholder("Party hunt analyzer").required(true))))
                        .build());
            } catch (Exception e) {
                log.error(e.getMessage());
                return Mono.empty();
            }
        }).subscribe();
    }

    private void subscribeModalEvent() {
        client.on(ModalSubmitInteractionEvent.class, event -> {
            if (!splitModalId.equals(event.getCustomId())) return Mono.empty();

            try {
                event.deferReply().subscribe();
                String spot = "", analyzer = "";

                for (TextInput component : event.getComponents(TextInput.class)) {
                    if (spotModalId.equals(component.getCustomId()))
                        spot = component.getValue().orElse("");
                    else if (splitterModalId.equals(component.getCustomId()))
                        analyzer = component.getValue().orElse("");
                }

                SplitLootModel model = service.splitLoot(analyzer, spot);
                analyzer = spot.isEmpty() ? analyzer : "Hunted on: " + spot + "\n\n" + analyzer;
                List<Button> buttons = splitterTransfersHandler.getSplittingButtons(model, getGuildId(event));

                if (isComparableData(event, model))
                    buttons.add(Button.primary(splitterComparatorHandler.getButtonId(), "Compare"));

                return event.createFollowup().withFiles(
                                MessageCreateFields.File.of("session.txt",
                                        new ByteArrayInputStream(analyzer.getBytes(StandardCharsets.UTF_8))))
                        .withEmbeds(createMessage(model))
                        .withComponents(splitActionRows(buttons));
            } catch (Exception ignore) {
                return event.createFollowup("Wrong parsing data");
            }
        }).subscribe();
    }

    @Override
    public void executeEvent() {
        subscribeCommandEvent();
        subscribeModalEvent();
        splitterTransfersHandler.executeEvent();
        splitterComparatorHandler.executeEvent();
    }

    private List<EmbedCreateSpec> createMessage(SplitLootModel model) {
        String title = "Party Hunt Session - " + model.getMembers().size() + " member(s)";
        if(!model.getSpotName().isEmpty()) title += "\nHunted on " + model.getSpotName();

        Color color = model.isNegative() ? Color.RED : Color.GREEN;
        String footer = model.getHuntTime() + " hunt from " + model.getHuntFrom() + " to " + model.getHuntTo();

        return createEmbeddedMessages(
                createFields(model),
                title,
                "",
                "",
                "",
                color,
                EmbedCreateFields.Footer.of(footer, null)
        );
    }

    private List<EmbedCreateFields.Field> createDescriptionFields(SplitLootModel model) {
        String description = "Type: **" + model.getLootType() +
                "**\nLoot: **" + model.getLoot() + "**" + getCoinEmoji() +
                "\nSupplies: **" + model.getSupplies() + "**" + getCoinEmoji()+
                "\nBalance: **" + model.getBalance() + "**" + getCoinEmoji();

        List<EmbedCreateFields.Field> fields = new ArrayList<>();
        fields.add(EmbedCreateFields.Field.of("", description, true));

        description = getBlankEmoji() + "\nLoot per hour: **" + model.getLootPerHour() + "**" + getCoinEmoji() +
                        "\nIndividual balance **" + model.getIndividualBalance() + "**" + getCoinEmoji();
        fields.add(EmbedCreateFields.Field.of("", description, true));
        fields.add(EmbedCreateFields.Field.of("\t", "\t", true));
        return fields;
    }

    private EmbedCreateFields.Field createDamageSortField(SplitLootModel model) {
        List<SplittingMember> members = new ArrayList<>(model.getMembers());
        members.sort(Comparator.comparing(SplittingMember::getDamagePercentage).reversed());
        StringBuilder damage = new StringBuilder();
        for(SplittingMember member : members) {
            damage.append("‣ ")
                    .append(member.getName())
                    .append(" (")
                    .append(member.getDamagePercentageString())
                    .append(")\n");
        }
        return EmbedCreateFields.Field.of("Damage", damage.toString(), true);
    }

    private EmbedCreateFields.Field createHealingSortField(SplitLootModel model) {
        List<SplittingMember> members = new ArrayList<>(model.getMembers());
        members.sort(Comparator.comparing(SplittingMember::getHealingPercentage).reversed());
        StringBuilder healing = new StringBuilder();
        for(SplittingMember member : members) {
            healing.append("‣ ")
                    .append(member.getName())
                    .append(" (")
                    .append(member.getHealingPercentageString())
                    .append(")\n");
        }
        return EmbedCreateFields.Field.of("Healing", healing.toString(), true);
    }

    private List<EmbedCreateFields.Field> createFields(SplitLootModel model) {
        StringBuilder splitting = new StringBuilder();
        List<EmbedCreateFields.Field> fields = new ArrayList<>(createDescriptionFields(model));
        fields.add(createDamageSortField(model));
        fields.add(createHealingSortField(model));
        fields.add(emptyField(true));
        fields.add(emptyField(false));
        fields.add(EmbedCreateFields.Field.of("", "**Members**", false));

        int iterations = 1;
        for(SplittingMember member : model.getMembers()) {
            if(model.getMembers().size() <= 8)
                fields.add(buildEmbedField(member));
            if(iterations % 2 == 0) fields.add(emptyField(true));
            iterations++;
            if(member.getTransfers().isEmpty()) continue;

            splitting.append("‣ **")
                    .append(member.getName())
                    .append("**\n");
            for(TransferData transfer : member.getTransfers()) {
                splitting.append("```")
                        .append("transfer ")
                        .append(transfer.getTransferAmount())
                        .append(" to ")
                        .append(transfer.getTransferTo())
                        .append("```");
            }
            splitting.append("\n");
        }

        fields.add(emptyField(false));
        fields.add(EmbedCreateFields.Field.of("**Splitting Instructions**", splitting.toString(), false));
        return fields;
    }

    private EmbedCreateFields.Field buildEmbedField(SplittingMember data) {
        String value = getBlankEmoji() + getBlankEmoji() + "Loot: **" + data.getLoot() + "** (" + data.getLootPercentageString() + ")\n" +
                getBlankEmoji() + getBlankEmoji() + "Supplies: **" + data.getSupplies() + "** (" + data.getSuppliesPercentageString() + ")\n" +
                getBlankEmoji() + getBlankEmoji() + "Balance: **" + data.getBalance() + "**\n" +
                getBlankEmoji() + getBlankEmoji() + "Damage: **" + data.getDamage() + "**\n" +
                getBlankEmoji() + getBlankEmoji() + "Healing: **" + data.getHealing() +
                "**";
        return EmbedCreateFields.Field.of("‣ " + data.getName(), value, true);
    }

    private boolean isComparableData(ModalSubmitInteractionEvent event, SplitLootModel model) {
        if(model.getSpotName().isEmpty()) return false;
        List<Message> msgs = getChannelMessages((GuildMessageChannel) event.getInteraction().getChannel().block())
                .collectList()
                .block();
        if(msgs == null || msgs.isEmpty()) return false;

        List<Message> embeddedMsgs = msgs.stream().filter(x ->
                x.getAuthor().get().getId().equals(Snowflake.of(Connector.getId())) &&
                x.getEmbeds().size() == 1 &&
                x.getAttachments().size() == 1)
                .toList();

        return embeddedMsgs.stream().anyMatch(x -> {
            EmbedData embed = x.getData().embeds().get(0);
            String title = embed.title().get();
            return title.contains(model.getSpotName()) &&
                    title.split("\n")[0].contains(String.valueOf(model.getMembers().size())) &&
                    model.getMembers().stream().allMatch(y -> embed.fields()
                                    .get()
                                    .stream()
                                    .anyMatch(z -> z.value().contains(y.getName())));
        });
    }

    @Override
    public String getEventName() {
        return EventName.lootSplitter;
    }

    @Override
    protected void executeEventProcess() {
    }
}
