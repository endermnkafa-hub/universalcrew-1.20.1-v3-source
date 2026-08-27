package com.mkai.universalcrew.client;

import com.mkai.universalcrew.network.CrewCommandPacket;
import com.mkai.universalcrew.network.CrewCreatePacket;
import com.mkai.universalcrew.network.GiveInvitePacket;
import com.mkai.universalcrew.network.HealCrewMemberPacket;
import com.mkai.universalcrew.network.ModNetwork;
import com.mkai.universalcrew.network.RequestCrewMembersPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.imageio.ImageIO;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CrewScreen extends Screen {

    private static final int ROW_HEIGHT = 26;
    private static final int ROW_GAP = 2;

    // Liste ile alt butonlar arasında bırakılacak boşluk
    private static final int LIST_BOTTOM_GAP = 8;

    private EditBox nameBox;

    private String logoLabel = "Logo seçilmedi";
    private byte[] logoBytes = new byte[0];

    private final Set<UUID> selected = new HashSet<>();

    private boolean setupMode;
    private boolean confirmDisband = false;

    private int scrollOffset = 0;
    private int visibleRows = 1;

    private int listTop;
    private int listBottom;
    private int controlsTop;

    private int refreshTicker = 0;

    public CrewScreen() {
        super(Component.literal("⚓ Tayfa"));
    }

    @Override
    protected void init() {
        super.init();

        rebuildWidgets();
        requestMembers();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();

        setupMode = ClientCrewState.crewName.isBlank();

        selected.removeIf(id -> ClientCrewState.members.stream()
                .noneMatch(member -> member.id().equals(id)));

        if (confirmDisband) {
            buildConfirmScreen();
            return;
        }

        if (setupMode) {
            buildCreateScreen();
            return;
        }

        buildManagementScreen();
    }

    private void buildCreateScreen() {
        int center = width / 2;

        nameBox = new EditBox(
                font,
                center - 140,
                75,
                280,
                22,
                Component.literal("Tayfa adı")
        );

        nameBox.setMaxLength(32);
        nameBox.setValue(ClientCrewState.crewName);

        addRenderableWidget(nameBox);

        addRenderableWidget(Button.builder(
                Component.literal("📁 LOGO SEÇ"),
                button -> chooseLogo()
        ).bounds(
                center - 140,
                115,
                135,
                24
        ).build());

        addRenderableWidget(Button.builder(
                Component.literal("⚓ KAYDET"),
                button -> saveCrew()
        ).bounds(
                center + 5,
                115,
                135,
                24
        ).build());
    }

    private void buildManagementScreen() {
        int center = width / 2;

        /*
         * ÜST BUTONLAR
         */

        addRenderableWidget(Button.builder(
                Component.literal("✉ DAVET EŞYASI"),
                button -> giveInvite()
        ).bounds(
                center - 220,
                65,
                115,
                22
        ).build());

        addRenderableWidget(Button.builder(
                Component.literal("↻ YENİLE"),
                button -> requestMembers()
        ).bounds(
                center - 100,
                65,
                90,
                22
        ).build());

        addRenderableWidget(Button.builder(
                Component.literal("TÜMÜNÜ SEÇ"),
                button -> selectAll()
        ).bounds(
                center - 5,
                65,
                100,
                22
        ).build());

        addRenderableWidget(Button.builder(
                Component.literal("SEÇİMİ TEMİZLE"),
                button -> {
                    selected.clear();
                    rebuildWidgets();
                }
        ).bounds(
                center + 100,
                65,
                125,
                22
        ).build());


        /*
         * SABİT ALT KONTROL ALANI
         *
         * Buradaki butonlar üye sayısından bağımsız olarak
         * aynı yerde kalır.
         */

        controlsTop = Math.max(
                190,
                height - 75
        );

        /*
         * LİSTE ALANI
         *
         * Liste, alt butonlara kadar olan boşluğu kullanır.
         * Böylece üye sayısı arttığında liste aşağı taşmaz.
         */

        listTop = 98;

        listBottom = controlsTop - LIST_BOTTOM_GAP;

        int availableHeight = Math.max(
                ROW_HEIGHT,
                listBottom - listTop
        );

        visibleRows = Math.max(
                1,
                (availableHeight + ROW_GAP)
                        / (ROW_HEIGHT + ROW_GAP)
        );

        int maxOffset = Math.max(
                0,
                ClientCrewState.members.size() - visibleRows
        );

        scrollOffset = Math.max(
                0,
                Math.min(scrollOffset, maxOffset)
        );

        int end = Math.min(
                ClientCrewState.members.size(),
                scrollOffset + visibleRows
        );


        /*
         * ÜYE BUTONLARI
         */

        for (int i = scrollOffset; i < end; i++) {

            final int index = i;

            int rowY = listTop
                    + (i - scrollOffset)
                    * (ROW_HEIGHT + ROW_GAP);

            addRenderableWidget(Button.builder(
                    memberLabel(i),
                    button -> toggleMember(index)
            ).bounds(
                    center - 220,
                    rowY,
                    440,
                    ROW_HEIGHT
            ).build());
        }


        /*
         * ALT KONTROLLER
         */

        addRenderableWidget(Button.builder(
                Component.literal("⚔ ATAK"),
                button -> sendCommand("attack")
        ).bounds(
                center - 220,
                controlsTop,
                82,
                24
        ).build());

        addRenderableWidget(Button.builder(
                Component.literal("➜ TAKİP"),
                button -> sendCommand("follow")
        ).bounds(
                center - 130,
                controlsTop,
                82,
                24
        ).build());

        addRenderableWidget(Button.builder(
                Component.literal("🛡 SAVUN"),
                button -> sendCommand("defend")
        ).bounds(
                center - 40,
                controlsTop,
                82,
                24
        ).build());

        addRenderableWidget(Button.builder(
                Component.literal("■ DUR"),
                button -> sendCommand("stop")
        ).bounds(
                center + 50,
                controlsTop,
                82,
                24
        ).build());


        /*
         * İKİNCİ BUTON SATIRI
         */

        addRenderableWidget(Button.builder(
                Component.literal("❤ İYİLEŞTİR"),
                button -> healSelected()
        ).bounds(
                center - 220,
                controlsTop + 30,
                110,
                24
        ).build());

        addRenderableWidget(Button.builder(
                Component.literal("✦ IŞINLA"),
                button -> sendCommand("teleport")
        ).bounds(
                center - 105,
                controlsTop + 30,
                90,
                24
        ).build());

        addRenderableWidget(Button.builder(
                Component.literal("☠ TAYFAYI DAĞIT"),
                button -> {
                    confirmDisband = true;
                    rebuildWidgets();
                }
        ).bounds(
                center - 10,
                controlsTop + 30,
                140,
                24
        ).build());

        addRenderableWidget(Button.builder(
                Component.literal("KAPAT"),
                button -> onClose()
        ).bounds(
                center + 135,
                controlsTop + 30,
                85,
                24
        ).build());
    }

    private void buildConfirmScreen() {
        int center = width / 2;

        addRenderableWidget(Button.builder(
                Component.literal("EVET, TAYFAYI DAĞIT"),
                button -> disband()
        ).bounds(
                center - 150,
                105,
                140,
                24
        ).build());

        addRenderableWidget(Button.builder(
                Component.literal("HAYIR"),
                button -> {
                    confirmDisband = false;
                    rebuildWidgets();
                }
        ).bounds(
                center + 10,
                105,
                140,
                24
        ).build());
    }

    private Component memberLabel(int index) {
        ClientCrewState.Member member =
                ClientCrewState.members.get(index);

        String prefix = selected.contains(member.id())
                ? "✓ "
                : "□ ";

        String state = switch (member.state()) {
            case "attack" -> " ⚔ ATAK";
            case "follow" -> " ➜ TAKİP";
            case "defend" -> " 🛡 SAVUN";
            default -> " ■ DUR";
        };

        float health = Math.max(
                0.0F,
                member.health()
        );

        float maxHealth = Math.max(
                1.0F,
                member.maxHealth()
        );

        String healthText = String.format(
                "❤ %.1f/%.1f",
                health / 2.0F,
                maxHealth / 2.0F
        );

        return Component.literal(
                prefix
                        + member.name()
                        + "  "
                        + healthText
                        + state
        );
    }

    private void toggleMember(int index) {
        UUID id =
                ClientCrewState.members
                        .get(index)
                        .id();

        if (!selected.add(id)) {
            selected.remove(id);
        }

        rebuildWidgets();
    }

    private void selectAll() {
        selected.clear();

        ClientCrewState.members.forEach(
                member -> selected.add(member.id())
        );

        rebuildWidgets();
    }

    private void sendCommand(String command) {

        if (selected.isEmpty()) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(
                new CrewCommandPacket(
                        new ArrayList<>(selected),
                        command
                )
        );

        requestMembers();
    }

    private void healSelected() {

        if (selected.size() != 1) {
            return;
        }

        UUID memberId =
                selected.iterator().next();

        ModNetwork.CHANNEL.sendToServer(
                new HealCrewMemberPacket(memberId)
        );

        requestMembers();
    }

    private void requestMembers() {

        ModNetwork.CHANNEL.sendToServer(
                new RequestCrewMembersPacket()
        );
    }

    public void onCrewDataUpdated() {

        selected.removeIf(id ->
                ClientCrewState.members.stream()
                        .noneMatch(
                                member -> member.id().equals(id)
                        )
        );

        int maxOffset = Math.max(
                0,
                ClientCrewState.members.size()
                        - visibleRows
        );

        scrollOffset = Math.min(
                scrollOffset,
                maxOffset
        );

        rebuildWidgets();
    }

    private void saveCrew() {

        if (nameBox == null) {
            return;
        }

        String name =
                nameBox.getValue().trim();

        if (name.isBlank()) {
            return;
        }

        ClientCrewState.crewName = name;

        ModNetwork.CHANNEL.sendToServer(
                new CrewCreatePacket(
                        name,
                        logoBytes
                )
        );

        setupMode = false;

        rebuildWidgets();
    }

    private void giveInvite() {

        ModNetwork.CHANNEL.sendToServer(
                new GiveInvitePacket()
        );
    }

    private void disband() {

        selected.clear();

        ModNetwork.CHANNEL.sendToServer(
                new CrewCommandPacket(
                        new ArrayList<>(),
                        "disband"
                )
        );

        ClientCrewState.clearCrew();

        confirmDisband = false;
        setupMode = true;

        rebuildWidgets();
    }

    private void chooseLogo() {

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        try {

            Path logoDirectory =
                    Minecraft.getInstance()
                            .gameDirectory
                            .toPath()
                            .resolve("config")
                            .resolve("universalcrew")
                            .resolve("logos");

            Files.createDirectories(
                    logoDirectory
            );

            FileDialog dialog =
                    new FileDialog(
                            (Frame) null,
                            "Tayfa logosu seç",
                            FileDialog.LOAD
                    );

            dialog.setDirectory(
                    logoDirectory
                            .toAbsolutePath()
                            .toString()
            );

            dialog.setFilenameFilter(
                    (dir, name) ->
                            name != null
                                    && name
                                    .toLowerCase()
                                    .endsWith(".png")
            );

            dialog.setVisible(true);

            if (dialog.getFile() == null) {
                return;
            }

            Path path = Path.of(
                    dialog.getDirectory(),
                    dialog.getFile()
            );

            BufferedImage image =
                    ImageIO.read(path.toFile());

            if (
                    image == null
                            || image.getWidth() > 512
                            || image.getHeight() > 512
            ) {

                logoLabel =
                        "Logo 512x512'den büyük olamaz.";

                return;
            }

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            ImageIO.write(
                    image,
                    "PNG",
                    out
            );

            logoBytes =
                    out.toByteArray();

            if (logoBytes.length > 262_144) {

                logoBytes =
                        new byte[0];

                logoLabel =
                        "Logo dosyası çok büyük.";

                return;
            }

            ClientCrewState.logoBytes =
                    logoBytes;

            ClientCrewState.logoPath =
                    path;

            logoLabel =
                    "Logo: "
                            + path.getFileName();

        } catch (Exception ignored) {

            logoLabel =
                    "Logo seçilemedi.";
        }
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {

        /*
         * SADECE ÜYE LİSTESİNDE SCROLL
         *
         * Alt butonların olduğu bölgeye gelince
         * scroll çalışmaz.
         */

        if (
                !setupMode
                        && !confirmDisband
                        && mouseX >= width / 2 - 230
                        && mouseX <= width / 2 + 230
                        && mouseY >= listTop
                        && mouseY <= listBottom
                        && ClientCrewState.members.size()
                        > visibleRows
        ) {

            int maxOffset =
                    ClientCrewState.members.size()
                            - visibleRows;

            if (delta < 0) {

                scrollOffset =
                        Math.min(
                                maxOffset,
                                scrollOffset + 1
                        );

            } else if (delta > 0) {

                scrollOffset =
                        Math.max(
                                0,
                                scrollOffset - 1
                        );
            }

            rebuildWidgets();

            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                delta
        );
    }

    @Override
    public void tick() {

        super.tick();

        if (!setupMode && !confirmDisband) {

            refreshTicker++;

            if (refreshTicker >= 10) {

                refreshTicker = 0;

                requestMembers();
            }
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

        renderBackground(graphics);

        int center = width / 2;

        if (confirmDisband) {

            graphics.drawCenteredString(
                    font,
                    "☠ TAYFAYI DAĞIT?",
                    center,
                    35,
                    0xFFFFFF
            );

            graphics.drawCenteredString(
                    font,
                    "Bu işlem tayfanı dağıtır.",
                    center,
                    55,
                    0xAAAAAA
            );

        } else if (setupMode) {

            graphics.drawCenteredString(
                    font,
                    "⚓ TAYFANI KUR",
                    center,
                    25,
                    0xFFFFFF
            );

            graphics.drawCenteredString(
                    font,
                    "Tayfa adı + isteğe bağlı PNG logo",
                    center,
                    45,
                    0xAAAAAA
            );

            graphics.drawCenteredString(
                    font,
                    logoLabel,
                    center,
                    150,
                    0xDDDDDD
            );

        } else {

            graphics.drawCenteredString(
                    font,
                    "⚓ " + ClientCrewState.crewName,
                    center,
                    25,
                    0xFFFFFF
            );

            graphics.drawCenteredString(
                    font,
                    "Üyeleri seç → emir ver",
                    center,
                    45,
                    0xAAAAAA
            );

            /*
             * ÜYE YOKSA
             */

            if (ClientCrewState.members.isEmpty()) {

                graphics.drawCenteredString(
                        font,
                        "Henüz tayfa üyesi yok.",
                        center,
                        listTop + 10,
                        0xFFFF55
                );
            }


            /*
             * SCROLLBAR
             */

            if (
                    ClientCrewState.members.size()
                            > visibleRows
            ) {

                int trackHeight =
                        Math.max(
                                20,
                                listBottom - listTop
                        );

                int thumbHeight =
                        Math.max(
                                20,
                                (int) (
                                        (double) visibleRows
                                                / ClientCrewState.members.size()
                                                * trackHeight
                                )
                        );

                int maxOffset =
                        ClientCrewState.members.size()
                                - visibleRows;

                int travel =
                        Math.max(
                                0,
                                trackHeight
                                        - thumbHeight
                        );

                int thumbY =
                        listTop
                                + (
                                maxOffset == 0
                                        ? 0
                                        : (int) (
                                        (double) scrollOffset
                                                / maxOffset
                                                * travel
                                )
                        );

                /*
                 * Scrollbar arka planı
                 */

                graphics.fill(
                        center + 226,
                        listTop,
                        center + 230,
                        listBottom,
                        0x55333333
                );

                /*
                 * Scrollbar hareketli parçası
                 */

                graphics.fill(
                        center + 226,
                        thumbY,
                        center + 230,
                        thumbY + thumbHeight,
                        0xFFAAAAAA
                );
            }
        }

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }
}