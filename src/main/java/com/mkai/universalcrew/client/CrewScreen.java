package com.mkai.universalcrew.client;

import com.mkai.universalcrew.network.CrewCommandPacket;
import com.mkai.universalcrew.network.CrewCreatePacket;
import com.mkai.universalcrew.network.GiveInvitePacket;
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

    private EditBox nameBox;

    private String logoLabel =
            "Logo seçilmedi";

    private byte[] logoBytes =
            new byte[0];

    private final Set<UUID> selected =
            new HashSet<>();

    private boolean setupMode;

    private boolean confirmDisband =
            false;

    public CrewScreen() {

        super(
                Component.literal(
                        "⚓ Tayfa"
                )
        );
    }

    // =========================================================
    // INIT
    // =========================================================

    @Override
    protected void init() {

        super.init();

        rebuildWidgets();

        requestMembers();
    }

    // =========================================================
    // GUI
    // =========================================================

    @Override
    protected void rebuildWidgets() {

        clearWidgets();

        setupMode =
                ClientCrewState.crewName.isBlank();

        if (confirmDisband) {

            buildConfirmScreen();

        } else if (setupMode) {

            buildCreateScreen();

        } else {

            buildManagementScreen();
        }

        /*
         * Artık olmayan üyelerin seçimlerini temizle.
         */
        selected.removeIf(
                id ->
                        ClientCrewState.members
                                .stream()
                                .noneMatch(
                                        member ->
                                                member.id()
                                                        .equals(id)
                                )
        );
    }

    // =========================================================
    // İLK KURULUM
    // =========================================================

    private void buildCreateScreen() {

        int center =
                this.width / 2;

        nameBox =
                new EditBox(
                        this.font,
                        center - 140,
                        75,
                        280,
                        22,
                        Component.literal(
                                "Tayfa adı"
                        )
                );

        nameBox.setMaxLength(32);

        nameBox.setValue(
                ClientCrewState.crewName
        );

        addRenderableWidget(
                nameBox
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "📁 LOGO SEÇ"
                        ),
                        button ->
                                chooseLogo()
                )
                .bounds(
                        center - 140,
                        115,
                        135,
                        24
                )
                .build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "⚓ KAYDET"
                        ),
                        button ->
                                saveCrew()
                )
                .bounds(
                        center + 5,
                        115,
                        135,
                        24
                )
                .build()
        );
    }

    // =========================================================
    // CREW YÖNETİMİ
    // =========================================================

    private void buildManagementScreen() {

        int center =
                this.width / 2;

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "✉ DAVET EŞYASI"
                        ),
                        button ->
                                giveInvite()
                )
                .bounds(
                        center - 170,
                        70,
                        120,
                        22
                )
                .build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "↻ YENİLE"
                        ),
                        button ->
                                requestMembers()
                )
                .bounds(
                        center - 45,
                        70,
                        90,
                        22
                )
                .build()
        );

        int memberStartY =
                105;

        for (
                int i = 0;
                i < ClientCrewState.members.size();
                i++
        ) {

            final int index = i;

            addRenderableWidget(
                    Button.builder(
                            memberLabel(i),
                            button ->
                                    toggleMember(index)
                    )
                    .bounds(
                            center - 170,
                            memberStartY
                                    + i * 26,
                            340,
                            22
                    )
                    .build()
            );
        }

        int bottom =
                Math.max(
                        155,
                        memberStartY
                                + ClientCrewState.members.size()
                                * 26
                                + 10
                );

        // =====================================================
        // ATAK
        // =====================================================

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "⚔ ATAK"
                        ),
                        button ->
                                sendCommand(
                                        "attack"
                                )
                )
                .bounds(
                        center - 170,
                        bottom,
                        78,
                        24
                )
                .build()
        );

        // =====================================================
        // TAKİP
        // =====================================================

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "➜ TAKİP"
                        ),
                        button ->
                                sendCommand(
                                        "follow"
                                )
                )
                .bounds(
                        center - 84,
                        bottom,
                        78,
                        24
                )
                .build()
        );

        // =====================================================
        // SAVUN
        // =====================================================

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "🛡 SAVUN"
                        ),
                        button ->
                                sendCommand(
                                        "defend"
                                )
                )
                .bounds(
                        center + 2,
                        bottom,
                        78,
                        24
                )
                .build()
        );

        // =====================================================
        // DUR
        // =====================================================

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "■ DUR"
                        ),
                        button ->
                                sendCommand(
                                        "stop"
                                )
                )
                .bounds(
                        center + 88,
                        bottom,
                        82,
                        24
                )
                .build()
        );

        // =====================================================
        // SEÇİM
        // =====================================================

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "TÜMÜNÜ SEÇ"
                        ),
                        button ->
                                selectAll()
                )
                .bounds(
                        center - 170,
                        bottom + 32,
                        105,
                        22
                )
                .build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "SEÇİMİ TEMİZLE"
                        ),
                        button -> {

                            selected.clear();

                            rebuildWidgets();
                        }
                )
                .bounds(
                        center - 58,
                        bottom + 32,
                        110,
                        22
                )
                .build()
        );

        // =====================================================
        // TAYFAYI DAĞIT
        // =====================================================

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "☠ TAYFAYI DAĞIT"
                        ),
                        button -> {

                            confirmDisband =
                                    true;

                            rebuildWidgets();
                        }
                )
                .bounds(
                        center + 60,
                        bottom + 32,
                        110,
                        22
                )
                .build()
        );
    }

    // =========================================================
    // ONAY
    // =========================================================

    private void buildConfirmScreen() {

        int center =
                this.width / 2;

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "EVET, TAYFAYI DAĞIT"
                        ),
                        button ->
                                disband()
                )
                .bounds(
                        center - 150,
                        105,
                        140,
                        24
                )
                .build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "HAYIR"
                        ),
                        button -> {

                            confirmDisband =
                                    false;

                            rebuildWidgets();
                        }
                )
                .bounds(
                        center + 10,
                        105,
                        140,
                        24
                )
                .build()
        );
    }

    // =========================================================
    // ÜYE ETİKETİ
    // =========================================================

    private Component memberLabel(
            int index
    ) {

        ClientCrewState.Member member =
                ClientCrewState.members
                        .get(index);

        String prefix =
                selected.contains(
                        member.id()
                )
                        ? "✓ "
                        : "□ ";

        String state =
                switch (
                        member.state()
                ) {

                    case "attack" ->
                            "  ⚔ ATAK";

                    case "follow" ->
                            "  ➜ TAKİP";

                    case "defend" ->
                            "  🛡 SAVUN";

                    default ->
                            "  ■ DUR";
                };

        return Component.literal(
                prefix
                        + member.name()
                        + state
        );
    }

    // =========================================================
    // ÜYE SEÇ
    // =========================================================

    private void toggleMember(
            int index
    ) {

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

        ClientCrewState.members
                .forEach(
                        member ->
                                selected.add(
                                        member.id()
                                )
                );

        rebuildWidgets();
    }

    // =========================================================
    // KOMUT
    // =========================================================

    private void sendCommand(
            String command
    ) {

        if (selected.isEmpty()) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(
                new CrewCommandPacket(
                        new ArrayList<>(
                                selected
                        ),
                        command
                )
        );

        requestMembers();
    }

    // =========================================================
    // YENİLE
    // =========================================================

    private void requestMembers() {

        ModNetwork.CHANNEL.sendToServer(
                new RequestCrewMembersPacket()
        );
    }

    public void onCrewDataUpdated() {

        selected.removeIf(
                id ->
                        ClientCrewState.members
                                .stream()
                                .noneMatch(
                                        member ->
                                                member.id()
                                                        .equals(id)
                                )
        );

        rebuildWidgets();
    }

    // =========================================================
    // CREW KAYDET
    // =========================================================

    private void saveCrew() {

        if (nameBox == null) {
            return;
        }

        String name =
                nameBox
                        .getValue()
                        .trim();

        if (name.isBlank()) {
            return;
        }

        ClientCrewState.crewName =
                name;

        ModNetwork.CHANNEL.sendToServer(
                new CrewCreatePacket(
                        name,
                        logoBytes
                )
        );

        setupMode = false;

        rebuildWidgets();
    }

    // =========================================================
    // DAVET
    // =========================================================

    private void giveInvite() {

        ModNetwork.CHANNEL.sendToServer(
                new GiveInvitePacket()
        );
    }

    // =========================================================
    // DAĞIT
    // =========================================================

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

    // =========================================================
    // LOGO SEÇ
    // =========================================================

    private void chooseLogo() {

        if (
                GraphicsEnvironment.isHeadless()
        ) {
            return;
        }

        try {

            /*
             * Minecraft'ın gerçek oyun klasörü.
             */
            Path logoDirectory =
                    Minecraft.getInstance()
                            .gameDirectory
                            .toPath()
                            .resolve("config")
                            .resolve("universalcrew")
                            .resolve("logos");

            /*
             * Klasör yoksa oluştur.
             */
            Files.createDirectories(
                    logoDirectory
            );

            FileDialog dialog =
                    new FileDialog(
                            (Frame) null,
                            "Tayfa logosu seç",
                            FileDialog.LOAD
                    );

            /*
             * DOĞRUDAN logo klasörünü aç.
             */
            dialog.setDirectory(
                    logoDirectory
                            .toAbsolutePath()
                            .toString()
            );

            /*
             * Sadece PNG göster.
             */
            dialog.setFilenameFilter(
                    (dir, name) ->
                            name != null
                                    && name
                                    .toLowerCase()
                                    .endsWith(".png")
            );

            dialog.setVisible(
                    true
            );

            if (
                    dialog.getFile()
                            == null
            ) {
                return;
            }

            Path path =
                    Path.of(
                            dialog.getDirectory(),
                            dialog.getFile()
                    );

            BufferedImage image =
                    ImageIO.read(
                            path.toFile()
                    );

            if (
                    image == null
                            || image.getWidth()
                            > 512
                            || image.getHeight()
                            > 512
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

            if (
                    logoBytes.length
                            > 262_144
            ) {

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

        } catch (Exception ex) {

            logoLabel =
                    "Logo seçilemedi.";
        }
    }

    // =========================================================
    // TICK
    // =========================================================

    @Override
    public void tick() {

        super.tick();

        if (
                setupMode
                        && !confirmDisband
                        && !ClientCrewState
                        .crewName
                        .isBlank()
        ) {

            rebuildWidgets();
        }

        if (
                !setupMode
                        && !confirmDisband
                        && ClientCrewState
                        .crewName
                        .isBlank()
        ) {

            rebuildWidgets();
        }
    }

    // =========================================================
    // RENDER
    // =========================================================

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

        renderBackground(
                graphics
        );

        int center =
                this.width / 2;

        if (confirmDisband) {

            graphics.drawCenteredString(
                    this.font,
                    "☠ TAYFAYI DAĞIT?",
                    center,
                    35,
                    0xFFFFFF
            );

            graphics.drawCenteredString(
                    this.font,
                    "Bu işlem tayfanı dağıtır.",
                    center,
                    55,
                    0xAAAAAA
            );

            graphics.drawCenteredString(
                    this.font,
                    "Devam etmek istiyor musun?",
                    center,
                    68,
                    0xAAAAAA
            );

        } else if (setupMode) {

            graphics.drawCenteredString(
                    this.font,
                    "⚓ TAYFANI KUR",
                    center,
                    25,
                    0xFFFFFF
            );

            graphics.drawCenteredString(
                    this.font,
                    "Tayfa adı + isteğe bağlı PNG logo",
                    center,
                    45,
                    0xAAAAAA
            );

            graphics.drawCenteredString(
                    this.font,
                    logoLabel,
                    center,
                    150,
                    0xDDDDDD
            );

        } else {

            graphics.drawCenteredString(
                    this.font,
                    "⚓ "
                            + ClientCrewState
                            .crewName,
                    center,
                    25,
                    0xFFFFFF
            );

            graphics.drawCenteredString(
                    this.font,
                    "Üyeleri seç → emir ver",
                    center,
                    45,
                    0xAAAAAA
            );

            if (
                    ClientCrewState.members
                            .isEmpty()
            ) {

                graphics.drawCenteredString(
                        this.font,
                        "Henüz tayfa üyesi yok.",
                        center,
                        140,
                        0xFFFF55
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