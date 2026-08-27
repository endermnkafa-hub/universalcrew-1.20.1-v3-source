package com.mkai.universalcrew.event;

import com.mkai.universalcrew.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CrewEvents {

    public static final String CREW_EXISTS = "UniversalCrewExists";
    public static final String CREW_NAME = "UniversalCrewName";
    public static final String CREW_LOGO = "UniversalCrewLogo";

    private static final String OWNER =
            "UniversalCrewOwner";

    private static final String STATE =
            "UniversalCrewState";

    private static final String RECRUITED =
            "UniversalCrewRecruited";

    private static final String ORIGINAL_NAME =
            "UniversalCrewOriginalName";

    private static final String COMBAT_TARGET =
            "UniversalCrewCombatTarget";

    private static final String DEFEND_TARGET =
            "UniversalCrewDefendTarget";

    private static final String DEFEND_X =
            "UniversalCrewDefendX";

    private static final String DEFEND_Y =
            "UniversalCrewDefendY";

    private static final String DEFEND_Z =
            "UniversalCrewDefendZ";

    private static final String DEFEND_POS_SET =
            "UniversalCrewDefendPositionSet";

    /*
     * Oyuncunun kalıcı tayfa kayıt listesi.
     *
     * Her üye:
     * UUID
     * Name
     * Dimension
     * X/Y/Z
     * State
     */
    private static final String ROSTER =
            "UniversalCrewRoster";

    private static final String R_UUID =
            "UUID";

    private static final String R_NAME =
            "Name";

    private static final String R_DIMENSION =
            "Dimension";

    private static final String R_X =
            "X";

    private static final String R_Y =
            "Y";

    private static final String R_Z =
            "Z";

    private static final String R_STATE =
            "State";

    /*
     * Eski tek-eşyalı istek sisteminin anahtarı.
     * Eski save'lerin uyumluluğu için bırakıldı.
     */
    private static final String REQ_ITEM =
            "UniversalCrewReqItem";

    private static final String REQ_COUNT =
            "UniversalCrewReqCount";

    private static final String REQ_KNOWN =
            "UniversalCrewReqKnown";

    /*
     * Yeni çoklu gereksinim sistemi.
     */
    private static final String REQUIREMENTS =
            "UniversalCrewRequirements";

    private static final String REQ_GENERATED =
            "UniversalCrewRequirementsGenerated";

    private static final String CREW_COUNT =
            "UniversalCrewCount";

    public static final int MAX_CREW = 12;

    private static final double FOLLOW_DISTANCE = 5.0D;

    private static final double TELEPORT_DISTANCE = 48.0D;

    private static final double DEFEND_RADIUS = 50.0D;

    private static final double RETURN_DISTANCE = 2.5D;

    private static final double TARGET_MAX_DISTANCE = 128.0D;

    /*
     * Liste güncellemesini her tick yapmıyoruz.
     * 10 tick = 0.5 saniye.
     */
    private static final int ROSTER_UPDATE_INTERVAL = 10;

    private CrewEvents() {
    }

    // =========================================================
    // CREW OLUŞTURMA
    // =========================================================

    public static void createOrUpdateCrew(
            ServerPlayer player,
            String rawName,
            byte[] logoPng
    ) {

        String name =
                sanitizeName(rawName);

        if (name.isBlank()) {

            msg(
                    player,
                    "Önce tayfana bir isim ver.",
                    ChatFormatting.RED
            );

            return;
        }

        CompoundTag data =
                player.getPersistentData();

        data.putBoolean(
                CREW_EXISTS,
                true
        );

        data.putString(
                CREW_NAME,
                name
        );

        if (
                logoPng != null
                        && logoPng.length > 0
                        && logoPng.length <= 262_144
        ) {

            try {

                Path dir =
                        player.getServer()
                                .getServerDirectory()
                                .toPath()
                                .resolve("config")
                                .resolve("universalcrew")
                                .resolve("logos");

                Files.createDirectories(
                        dir
                );

                Path file =
                        dir.resolve(
                                player.getUUID()
                                        + ".png"
                        );

                Files.write(
                        file,
                        logoPng
                );

                data.putString(
                        CREW_LOGO,
                        file.toString()
                );

            } catch (Exception ignored) {

                msg(
                        player,
                        "Logo kaydedilemedi ama tayfan oluşturuldu.",
                        ChatFormatting.YELLOW
                );
            }
        }

        /*
         * Eski oyuncularda roster yoksa oluştur.
         */
        getRoster(data);

        msg(
                player,
                "⚓ " + name + " tayfası hazır!",
                ChatFormatting.GOLD
        );
    }

    private static String sanitizeName(
            String value
    ) {

        if (value == null) {
            return "";
        }

        String s =
                value
                        .trim()
                        .replaceAll(
                                "[\\r\\n\\t]",
                                " "
                        );

        return s.length() > 32
                ? s.substring(0, 32)
                : s;
    }

    public static boolean hasCrew(
            Player player
    ) {

        return player
                .getPersistentData()
                .getBoolean(
                        CREW_EXISTS
                );
    }

    // =========================================================
    // DAVET
    // =========================================================

    public static void giveInvite(
            ServerPlayer player
    ) {

        if (!hasCrew(player)) {

            msg(
                    player,
                    "Önce tayfanı oluştur.",
                    ChatFormatting.RED
            );

            return;
        }

        ItemStack stack =
                new ItemStack(
                        ModItems.CREW_INVITATION.get()
                );

        CompoundTag tag =
                stack.getOrCreateTag();

        tag.putString(
                "CrewName",
                player.getPersistentData()
                        .getString(
                                CREW_NAME
                        )
        );

        tag.putUUID(
                "Captain",
                player.getUUID()
        );

        player.getInventory()
                .placeItemBackInInventory(
                        stack
                );

        msg(
                player,
                "Davet mektubu envanterine verildi.",
                ChatFormatting.GREEN
        );
    }

    // =========================================================
    // RECRUIT
    // =========================================================

    @SubscribeEvent
    public static void onEntityInteract(
            PlayerInteractEvent.EntityInteract event
    ) {

        if (!(event.getTarget()
                instanceof Mob mob)) {

            return;
        }

        Player player =
                event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        ItemStack held =
                player.getItemInHand(
                        event.getHand()
                );

        if (
                !held.is(
                        ModItems.CREW_INVITATION.get()
                )
        ) {

            return;
        }

        if (!hasCrew(player)) {

            msg(
                    player,
                    "Önce kendi tayfanı oluştur.",
                    ChatFormatting.RED
            );

        } else if (isOwned(mob)) {

            msg(
                    player,
                    "Bu canlı zaten bir kaptana bağlı.",
                    ChatFormatting.YELLOW
            );

        } else if (
                countCrew(player)
                        >= MAX_CREW
        ) {

            msg(
                    player,
                    "Tayfan dolu. Maksimum "
                            + MAX_CREW
                            + " kişi.",
                    ChatFormatting.RED
            );

        } else if (!mob.isAlive()) {

            msg(
                    player,
                    "Bu canlı recruit edilemez.",
                    ChatFormatting.RED
            );

        } else {

            handleRecruitRequest(
                    player,
                    mob,
                    held
            );
        }

        event.setCancellationResult(
                InteractionResult.SUCCESS
        );

        event.setCanceled(true);
    }

    // =========================================================
    // HEDEFLEME
    // =========================================================

    @SubscribeEvent
    public static void onLivingChangeTarget(
            LivingChangeTargetEvent event
    ) {

        if (!(event.getEntity()
                instanceof Mob mob)) {

            return;
        }

        if (!isOwned(mob)) {
            return;
        }

        LivingEntity target =
                event.getNewTarget();

        if (target == null) {
            return;
        }

        String state =
                mob.getPersistentData()
                        .getString(
                                STATE
                        );

        if ("follow".equals(state)) {

            event.setCanceled(true);
            return;
        }

        if ("stop".equals(state)) {

            event.setCanceled(true);
            return;
        }

        if ("defend".equals(state)) {

            if (isSameCrew(
                    mob,
                    target
            )) {

                event.setCanceled(true);
                return;
            }

            UUID defendTarget =
                    readDefendTarget(
                            mob
                    );

            if (
                    defendTarget != null
                            && target.getUUID()
                            .equals(
                                    defendTarget
                            )
            ) {

                return;
            }

            event.setCanceled(true);
            return;
        }

        if ("attack".equals(state)) {

            if (isSameCrew(
                    mob,
                    target
            )) {

                event.setCanceled(true);
                return;
            }

            UUID combatTarget =
                    readCombatTarget(
                            mob
                    );

            UUID defendTarget =
                    readDefendTarget(
                            mob
                    );

            if (
                    combatTarget != null
                            && target.getUUID()
                            .equals(
                                    combatTarget
                            )
            ) {

                return;
            }

            if (
                    defendTarget != null
                            && target.getUUID()
                            .equals(
                                    defendTarget
                            )
            ) {

                return;
            }

            event.setCanceled(true);
        }
    }

    // =========================================================
    // SALDIRI OLAYI
    // =========================================================

    @SubscribeEvent
    public static void onLivingAttack(
            LivingAttackEvent event
    ) {

        if (
                event.getEntity()
                        .level()
                        .isClientSide
        ) {

            return;
        }

        LivingEntity victim =
                event.getEntity();

        Entity source =
                event.getSource()
                        .getEntity();

        if (!(source
                instanceof LivingEntity attacker)) {

            return;
        }

        // =====================================================
        // KAPTAN BİR CANLIYA VURDU
        // =====================================================

        if (
                attacker instanceof ServerPlayer captain
        ) {

            if (!hasCrew(captain)) {
                return;
            }

            if (victim == captain) {
                return;
            }

            if (
                    victim instanceof Mob targetMob
                            && isOwnedBy(
                            targetMob,
                            captain
                    )
            ) {

                return;
            }

            List<RosterEntry> roster =
                    readRoster(captain);

            for (
                    RosterEntry entry :
                    roster
            ) {

                if (!"attack".equals(entry.state())) {
                    continue;
                }

                Mob member =
                        findRecruit(
                                captain.getServer(),
                                captain,
                                entry
                        );

                if (member == null) {
                    continue;
                }

                member.getPersistentData()
                        .putUUID(
                                COMBAT_TARGET,
                                victim.getUUID()
                        );

                member.getPersistentData()
                        .remove(
                                DEFEND_TARGET
                        );

                member.setTarget(
                        victim
                );

                updateRosterFromEntity(
                        captain,
                        member
                );
            }

            return;
        }

        // =====================================================
        // BİR CANLI KAPTANA VURDU
        // =====================================================

        if (
                victim instanceof ServerPlayer captain
                        && hasCrew(captain)
        ) {

            if (
                    attacker instanceof Mob attackerMob
                            && isOwnedBy(
                            attackerMob,
                            captain
                    )
            ) {

                return;
            }

            if (!(captain.level()
                    instanceof ServerLevel level)) {

                return;
            }

            List<Mob> crew =
                    getLoadedCrewMembers(
                            captain
                    );

            for (
                    Mob member :
                    crew
            ) {

                String state =
                        member.getPersistentData()
                                .getString(
                                        STATE
                                );

                if (
                        !"attack".equals(state)
                                && !"defend".equals(state)
                ) {

                    continue;
                }

                if (
                        isSameCrew(
                                member,
                                attacker
                        )
                ) {

                    continue;
                }

                member.getPersistentData()
                        .putUUID(
                                DEFEND_TARGET,
                                attacker.getUUID()
                        );

                member.setTarget(
                        attacker
                );
            }
        }

        // =====================================================
        // TAYFA ÜYESİNE SALDIRILDI
        // =====================================================

        if (
                victim instanceof Mob victimMob
                        && isOwned(victimMob)
        ) {

            UUID ownerId =
                    readOwner(
                            victimMob
                    );

            if (ownerId == null) {
                return;
            }

            MinecraftServer server =
                    victimMob.getServer();

            if (server == null) {
                return;
            }

            ServerPlayer captain =
                    server.getPlayerList()
                            .getPlayer(
                                    ownerId
                            );

            if (captain == null) {
                return;
            }

            if (
                    attacker instanceof Mob attackerMob
                            && isOwnedBy(
                            attackerMob,
                            captain
                    )
            ) {

                return;
            }

            List<Mob> crew =
                    getLoadedCrewMembers(
                            captain
                    );

            for (
                    Mob member :
                    crew
            ) {

                String state =
                        member.getPersistentData()
                                .getString(
                                        STATE
                                );

                if (
                        !"attack".equals(state)
                                && !"defend".equals(state)
                ) {

                    continue;
                }

                if (
                        isSameCrew(
                                member,
                                attacker
                        )
                ) {

                    continue;
                }

                member.getPersistentData()
                        .putUUID(
                                DEFEND_TARGET,
                                attacker.getUUID()
                        );

                member.setTarget(
                        attacker
                );
            }
        }
    }

    // =========================================================
    // RECRUIT İSTEKLERİ
    // =========================================================

    private static void handleRecruitRequest(
            Player player,
            Mob mob,
            ItemStack invitation
    ) {

        CompoundTag data =
                mob.getPersistentData();

        /*
         * Yeni sistem daha önce oluşturulmadıysa
         * maksimum cana göre oluştur.
         */
        if (!data.getBoolean(
                REQ_GENERATED
        )) {

            generateRequirements(
                    mob
            );

            msg(
                    player,
                    mob.getName()
                            .getString()
                            + " için recruit isteği oluşturuldu: "
                            + formatRequirements(
                            data
                    ),
                    ChatFormatting.YELLOW
            );

            return;
        }

        if (!hasRequirements(
                player,
                data
        )) {

            msg(
                    player,
                    mob.getName()
                            .getString()
                            + " şunu istiyor: "
                            + formatRequirements(
                            data
                    ),
                    ChatFormatting.YELLOW
            );

            return;
        }

        removeRequirements(
                player,
                data
        );

        recruit(
                player,
                mob
        );

        if (
                !player.getAbilities()
                        .instabuild
        ) {

            invitation.shrink(
                    1
            );
        }
    }

    // =========================================================
    // CANA GÖRE İSTEK OLUŞTUR
    // =========================================================

    private static void generateRequirements(
            Mob mob
    ) {

        CompoundTag data =
                mob.getPersistentData();

        double maxHealth =
                mob.getMaxHealth();

        double hearts =
                maxHealth / 2.0D;

        ListTag requirements =
                new ListTag();

        /*
         * 0 - 19.99 kalp
         */
        if (hearts < 20.0D) {

            addRequirement(
                    requirements,
                    Items.BREAD,
                    3
            );

        /*
         * 20 - 50 kalp
         */
        } else if (hearts < 50.0D) {

            addRequirement(
                    requirements,
                    Items.IRON_INGOT,
                    64
            );

        /*
         * 50 - 100
         */
        } else if (hearts < 100.0D) {

            addRequirement(
                    requirements,
                    Items.GOLD_INGOT,
                    32
            );

            addRequirement(
                    requirements,
                    Items.IRON_BLOCK,
                    8
            );

        /*
         * 100 - 250
         */
        } else if (hearts < 250.0D) {

            addRequirement(
                    requirements,
                    Items.DIAMOND,
                    16
            );

            addRequirement(
                    requirements,
                    Items.GOLD_BLOCK,
                    16
            );

        /*
         * 250 - 500
         */
        } else if (hearts < 500.0D) {

            addRequirement(
                    requirements,
                    Items.DIAMOND_BLOCK,
                    16
            );

            addRequirement(
                    requirements,
                    Items.DIAMOND,
                    32
            );

        /*
         * 500 - 1000
         */
        } else if (hearts < 1000.0D) {

            addRequirement(
                    requirements,
                    Items.DIAMOND_BLOCK,
                    64
            );

            addRequirement(
                    requirements,
                    Items.GOLD_BLOCK,
                    16
            );

        /*
         * 1000 - 2500
         */
        } else if (hearts < 2500.0D) {

            addRequirement(
                    requirements,
                    Items.NETHERITE_INGOT,
                    32
            );

            addRequirement(
                    requirements,
                    Items.DIAMOND_BLOCK,
                    32
            );

        /*
         * 2500 - 5000
         */
        } else if (hearts < 5000.0D) {

            addRequirement(
                    requirements,
                    Items.NETHER_STAR,
                    10
            );

            addRequirement(
                    requirements,
                    Items.DIAMOND_BLOCK,
                    64
            );

        /*
         * 5000+
         */
        } else {

            addRequirement(
                    requirements,
                    Items.NETHER_STAR,
                    20
            );

            addRequirement(
                    requirements,
                    Items.DIAMOND_BLOCK,
                    128
            );

            addRequirement(
                    requirements,
                    Items.NETHERITE_INGOT,
                    64
            );
        }

        data.put(
                REQUIREMENTS,
                requirements
        );

        data.putBoolean(
                REQ_GENERATED,
                true
        );
    }

    private static void addRequirement(
            ListTag list,
            Item item,
            int count
    ) {

        CompoundTag req =
                new CompoundTag();

        req.putString(
                "Item",
                BuiltInRegistries.ITEM
                        .getKey(
                                item
                        )
                        .toString()
        );

        req.putInt(
                "Count",
                count
        );

        list.add(
                req
        );
    }

    private static String formatRequirements(
            CompoundTag data
    ) {

        if (
                !data.contains(
                        REQUIREMENTS,
                        Tag.TAG_LIST
                )
        ) {

            return "belirsiz";
        }

        ListTag list =
                data.getList(
                        REQUIREMENTS,
                        Tag.TAG_COMPOUND
                );

        List<String> parts =
                new ArrayList<>();

        for (
                int i = 0;
                i < list.size();
                i++
        ) {

            CompoundTag req =
                    list.getCompound(
                            i
                    );

            ResourceLocation id =
                    ResourceLocation.tryParse(
                            req.getString(
                                    "Item"
                            )
                    );

            Item item =
                    id == null
                            ? Items.BREAD
                            : BuiltInRegistries.ITEM
                            .getOptional(id)
                            .orElse(
                                    Items.BREAD
                            );

            parts.add(
                    req.getInt(
                            "Count"
                    )
                            + "x "
                            + item.getDescription()
                            .getString()
            );
        }

        return String.join(
                " + ",
                parts
        );
    }

    private static boolean hasRequirements(
            Player player,
            CompoundTag data
    ) {

        if (
                !data.contains(
                        REQUIREMENTS,
                        Tag.TAG_LIST
                )
        ) {

            return false;
        }

        ListTag list =
                data.getList(
                        REQUIREMENTS,
                        Tag.TAG_COMPOUND
                );

        /*
         * Önce bütün şartların mevcut olduğunu
         * kontrol ediyoruz.
         */
        for (
                int i = 0;
                i < list.size();
                i++
        ) {

            CompoundTag req =
                    list.getCompound(
                            i
                    );

            ResourceLocation id =
                    ResourceLocation.tryParse(
                            req.getString(
                                    "Item"
                            )
                    );

            Item item =
                    id == null
                            ? Items.BREAD
                            : BuiltInRegistries.ITEM
                            .getOptional(id)
                            .orElse(
                                    Items.BREAD
                            );

            int count =
                    Math.max(
                            1,
                            req.getInt(
                                    "Count"
                            )
                    );

            if (
                    countItem(
                            player,
                            item
                    ) < count
            ) {

                return false;
            }
        }

        return true;
    }

    private static void removeRequirements(
            Player player,
            CompoundTag data
    ) {

        if (
                player.getAbilities()
                        .instabuild
        ) {

            return;
        }

        if (
                !data.contains(
                        REQUIREMENTS,
                        Tag.TAG_LIST
                )
        ) {

            return;
        }

        ListTag list =
                data.getList(
                        REQUIREMENTS,
                        Tag.TAG_COMPOUND
                );

        for (
                int i = 0;
                i < list.size();
                i++
        ) {

            CompoundTag req =
                    list.getCompound(
                            i
                    );

            ResourceLocation id =
                    ResourceLocation.tryParse(
                            req.getString(
                                    "Item"
                            )
                    );

            Item item =
                    id == null
                            ? Items.BREAD
                            : BuiltInRegistries.ITEM
                            .getOptional(id)
                            .orElse(
                                    Items.BREAD
                            );

            int count =
                    Math.max(
                            1,
                            req.getInt(
                                    "Count"
                            )
                    );

            removeItems(
                    player,
                    item,
                    count
            );
        }
    }

    private static int countItem(
            Player player,
            Item item
    ) {

        int total = 0;

        for (
                int i = 0;
                i < player.getInventory()
                        .getContainerSize();
                i++
        ) {

            ItemStack stack =
                    player.getInventory()
                            .getItem(i);

            if (stack.is(item)) {
                total += stack.getCount();
            }
        }

        return total;
    }

    private static void removeItems(
            Player player,
            Item item,
            int amount
    ) {

        if (
                player.getAbilities()
                        .instabuild
        ) {

            return;
        }

        int remaining =
                amount;

        for (
                int i = 0;
                i < player.getInventory()
                        .getContainerSize()
                        && remaining > 0;
                i++
        ) {

            ItemStack stack =
                    player.getInventory()
                            .getItem(i);

            if (!stack.is(item)) {
                continue;
            }

            int take =
                    Math.min(
                            remaining,
                            stack.getCount()
                    );

            stack.shrink(
                    take
            );

            remaining -=
                    take;
        }
    }

    // =========================================================
    // RECRUIT
    // =========================================================

    private static void recruit(
            Player player,
            Mob mob
    ) {

        String originalName =
                mob.getPersistentData()
                        .getString(
                                ORIGINAL_NAME
                        );

        if (originalName.isBlank()) {

            originalName =
                    mob.getName()
                            .getString();

            mob.getPersistentData()
                    .putString(
                            ORIGINAL_NAME,
                            originalName
                    );
        }

        mob.getPersistentData()
                .putUUID(
                        OWNER,
                        player.getUUID()
                );

        mob.getPersistentData()
                .putBoolean(
                        RECRUITED,
                        true
                );

        mob.getPersistentData()
                .putString(
                        STATE,
                        "follow"
                );

        clearCombatData(
                mob
        );

        mob.getPersistentData()
                .remove(
                        REQ_KNOWN
                );

        updateCrewNameTag(
                mob,
                player
        );

        mob.setTarget(null);

        mob.setPersistenceRequired();

        incrementCrewCount(
                player,
                1
        );

        /*
         * Kalıcı roster'a ekle.
         */
        addToRoster(
                player,
                mob,
                "follow"
        );

        msg(
                player,
                "⚓ "
                        + originalName
                        + " tayfana katıldı!",
                ChatFormatting.GREEN
        );
    }

    // =========================================================
    // TAYFA İSMİ
    // =========================================================

    private static void updateCrewNameTag(
            Mob mob,
            Player captain
    ) {

        String crewName =
                captain.getPersistentData()
                        .getString(
                                CREW_NAME
                        );

        if (
                crewName == null
                        || crewName.isBlank()
        ) {

            return;
        }

        String originalName =
                mob.getPersistentData()
                        .getString(
                                ORIGINAL_NAME
                        );

        if (originalName.isBlank()) {

            originalName =
                    mob.getName()
                            .getString();
        }

        mob.setCustomName(
                Component.literal(
                        "<"
                                + crewName
                                + "> ["
                                + originalName
                                + "]"
                )
        );

        mob.setCustomNameVisible(
                true
        );
    }

    // =========================================================
    // CREW ROSTER
    // =========================================================

    private record RosterEntry(
            UUID uuid,
            String name,
            ResourceKey<Level> dimension,
            double x,
            double y,
            double z,
            String state
    ) {
    }

    private static ListTag getRoster(
            CompoundTag playerData
    ) {

        if (
                !playerData.contains(
                        ROSTER,
                        Tag.TAG_LIST
                )
        ) {

            playerData.put(
                    ROSTER,
                    new ListTag()
            );
        }

        return playerData.getList(
                ROSTER,
                Tag.TAG_COMPOUND
        );
    }

    private static List<RosterEntry> readRoster(
            Player player
    ) {

        List<RosterEntry> result =
                new ArrayList<>();

        ListTag roster =
                getRoster(
                        player.getPersistentData()
                );

        for (
                int i = 0;
                i < roster.size();
                i++
        ) {

            CompoundTag entry =
                    roster.getCompound(
                            i
                    );

            if (!entry.hasUUID(R_UUID)) {
                continue;
            }

            UUID uuid =
                    entry.getUUID(
                            R_UUID
                    );

            String name =
                    entry.getString(
                            R_NAME
                    );

            ResourceLocation dimensionId =
                    ResourceLocation.tryParse(
                            entry.getString(
                                    R_DIMENSION
                            )
                    );

            if (dimensionId == null) {
                dimensionId =
                        Level.OVERWORLD
                                .location();
            }

            ResourceKey<Level> dimension =
                    ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION,
                            dimensionId
                    );

            result.add(
                    new RosterEntry(
                            uuid,
                            name,
                            dimension,
                            entry.getDouble(R_X),
                            entry.getDouble(R_Y),
                            entry.getDouble(R_Z),
                            entry.getString(R_STATE)
                    )
            );
        }

        return result;
    }

    private static void addToRoster(
            Player player,
            Mob mob,
            String state
    ) {

        CompoundTag data =
                player.getPersistentData();

        ListTag roster =
                getRoster(
                        data
                );

        removeRosterEntry(
                roster,
                mob.getUUID()
        );

        CompoundTag entry =
                createRosterEntry(
                        mob,
                        state
                );

        roster.add(
                entry
        );

        data.put(
                ROSTER,
                roster
        );
    }

    private static CompoundTag createRosterEntry(
            Mob mob,
            String state
    ) {

        CompoundTag entry =
                new CompoundTag();

        entry.putUUID(
                R_UUID,
                mob.getUUID()
        );

        String originalName =
                mob.getPersistentData()
                        .getString(
                                ORIGINAL_NAME
                        );

        if (originalName.isBlank()) {
            originalName =
                    mob.getName()
                            .getString();
        }

        entry.putString(
                R_NAME,
                originalName
        );

        entry.putString(
                R_DIMENSION,
                mob.level()
                        .dimension()
                        .location()
                        .toString()
        );

        entry.putDouble(
                R_X,
                mob.getX()
        );

        entry.putDouble(
                R_Y,
                mob.getY()
        );

        entry.putDouble(
                R_Z,
                mob.getZ()
        );

        entry.putString(
                R_STATE,
                state
        );

        return entry;
    }

    private static void updateRosterFromEntity(
            Player player,
            Mob mob
    ) {

        ListTag roster =
                getRoster(
                        player.getPersistentData()
                );

        removeRosterEntry(
                roster,
                mob.getUUID()
        );

        roster.add(
                createRosterEntry(
                        mob,
                        mob.getPersistentData()
                                .getString(
                                        STATE
                                )
                )
        );
    }

    private static void removeRosterEntry(
            ListTag roster,
            UUID uuid
    ) {

        for (
                int i = roster.size() - 1;
                i >= 0;
                i--
        ) {

            CompoundTag entry =
                    roster.getCompound(
                            i
                    );

            if (
                    entry.hasUUID(R_UUID)
                            && entry.getUUID(
                            R_UUID
                    ).equals(uuid)
            ) {

                roster.remove(i);
            }
        }
    }

    // =========================================================
    // UZAKTAN ENTITY BUL
    // =========================================================

    private static Mob findRecruit(
            MinecraftServer server,
            ServerPlayer captain,
            RosterEntry entry
    ) {

        /*
         * Önce kayıtlı boyutu bul.
         */
        ServerLevel level =
                server.getLevel(
                        entry.dimension()
                );

        if (level == null) {
            return null;
        }

        /*
         * Entity zaten yüklüyse direkt bul.
         */
        Entity loaded =
                level.getEntity(
                        entry.uuid()
                );

        if (
                loaded instanceof Mob mob
                        && isOwnedBy(
                        mob,
                        captain
                )
        ) {

            return mob;
        }

        /*
         * Entity'nin bulunduğu chunk'ı yükle.
         */
        int chunkX =
                ((int)Math.floor(entry.x()))
                        >> 4;

        int chunkZ =
                ((int)Math.floor(entry.z()))
                        >> 4;

        try {

            level.getChunk(
                    chunkX,
                    chunkZ
            );

        } catch (Exception ignored) {
            return null;
        }

        Entity found =
                level.getEntity(
                        entry.uuid()
                );

        if (
                found instanceof Mob mob
                        && isOwnedBy(
                        mob,
                        captain
                )
        ) {

            return mob;
        }

        /*
         * Bazı entity sistemlerinde chunk yüklenmiş olsa
         * bile entity hemen bulunamayabilir.
         *
         * Yakın alanı son bir kez tarıyoruz.
         */
        List<Mob> nearby =
                level.getEntitiesOfClass(
                        Mob.class,
                        new net.minecraft.world.phys.AABB(
                                entry.x() - 8.0D,
                                entry.y() - 8.0D,
                                entry.z() - 8.0D,
                                entry.x() + 8.0D,
                                entry.y() + 8.0D,
                                entry.z() + 8.0D
                        ),
                        mob ->
                                mob.getUUID()
                                        .equals(
                                                entry.uuid()
                                        )
                                        && isOwnedBy(
                                        mob,
                                        captain
                                )
                );

        return nearby.isEmpty()
                ? null
                : nearby.get(0);
    }

    private static List<Mob> getLoadedCrewMembers(
            ServerPlayer captain
    ) {

        List<Mob> result =
                new ArrayList<>();

        for (
                RosterEntry entry :
                readRoster(captain)
        ) {

            ServerLevel level =
                    captain.getServer()
                            .getLevel(
                                    entry.dimension()
                            );

            if (level == null) {
                continue;
            }

            Entity entity =
                    level.getEntity(
                            entry.uuid()
                    );

            if (
                    entity instanceof Mob mob
                            && isOwnedBy(
                            mob,
                            captain
                    )
            ) {

                result.add(
                        mob
                );
            }
        }

        return result;
    }

    // =========================================================
    // K MENÜSÜ İÇİN TÜM ÜYELER
    // =========================================================

    public static void sendCrewMembers(
            ServerPlayer player
    ) {

        List<
                com.mkai.universalcrew.network.CrewMembersPacket.MemberData
                > list =
                new ArrayList<>();

        List<RosterEntry> roster =
                readRoster(player);
        ListTag rosterTag =
        getRoster(
                player.getPersistentData()
        );

boolean rosterChanged = false;

for (
        int i = rosterTag.size() - 1;
        i >= 0;
        i--
) {

    CompoundTag entryTag =
            rosterTag.getCompound(i);

    if (!entryTag.hasUUID(R_UUID)) {
        rosterTag.remove(i);
        rosterChanged = true;
        continue;
    }

    UUID memberId =
            entryTag.getUUID(R_UUID);

    RosterEntry entry =
            findRosterEntry(
                    roster,
                    memberId
            );

    if (entry == null) {
        rosterTag.remove(i);
        rosterChanged = true;
        continue;
    }

    Mob mob =
            findRecruit(
                    player.getServer(),
                    player,
                    entry
            );

    /*
     * Kayıtlı entity artık yoksa:
     *
     * - ölmüş olabilir
     * - yok olmuş olabilir
     *
     * K menüsünde göstermeyelim.
     */
    if (mob == null || !mob.isAlive()) {

        rosterTag.remove(i);
        rosterChanged = true;
    }
}

if (rosterChanged) {

    player.getPersistentData().put(
            ROSTER,
            rosterTag
    );
}

        /*
         * Liste artık mesafeye bağlı değil.
         */
        for (
                RosterEntry entry :
                roster
        ) {

            String state =
                    entry.state();

            if (
                    state == null
                            || state.isBlank()
            ) {

                state =
                        "stop";
            }

            list.add(
                    new com.mkai.universalcrew.network.CrewMembersPacket.MemberData(
                            entry.uuid(),
                            entry.name(),
                            state
                    )
            );

            if (
                    list.size()
                            >= MAX_CREW
            ) {

                break;
            }
        }

        /*
         * Aktif entity varsa gerçek state'i kullan.
         */
        for (
                int i = 0;
                i < list.size();
                i++
        ) {

            var packetMember =
                    list.get(i);

            RosterEntry entry =
                    findRosterEntry(
                            roster,
                            packetMember.id()
                    );

            if (entry == null) {
                continue;
            }

            Mob mob =
                    findRecruit(
                            player.getServer(),
                            player,
                            entry
                    );

            if (mob == null) {
                continue;
            }

            String actualState =
                    mob.getPersistentData()
                            .getString(
                                    STATE
                            );

            if (actualState.isBlank()) {
                actualState = "stop";
            }

            list.set(
                    i,
                    new com.mkai.universalcrew.network.CrewMembersPacket.MemberData(
                            mob.getUUID(),
                            entry.name(),
                            actualState
                    )
            );
        }

        com.mkai.universalcrew.network.ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER
                        .with(
                                () -> player
                        ),
                new com.mkai.universalcrew.network.CrewMembersPacket(
                        player.getPersistentData()
                                .getString(
                                        CREW_NAME
                                ),
                        list
                )
        );
    }

    private static RosterEntry findRosterEntry(
            List<RosterEntry> roster,
            UUID uuid
    ) {

        for (
                RosterEntry entry :
                roster
        ) {

            if (
                    entry.uuid()
                            .equals(uuid)
            ) {

                return entry;
            }
        }

        return null;
    }

    // =========================================================
    // KOMUTLAR
    // =========================================================

    public static void commandMembers(
        ServerPlayer captain,
        List<UUID> ids,
        String command
) {

    if (!hasCrew(captain)) {
        return;
    }

    // =====================================================
    // TAYFAYI DAĞIT
    // =====================================================

    if ("disband".equals(command)) {

        disbandCrew(
                captain
        );

        return;
    }

    // =====================================================
    // KOMUTU UYGULANACAK ÜYELERİ BELİRLE
    // =====================================================

    Set<UUID> requested =
            new HashSet<>();

    /*
     * Boş UUID listesi:
     *
     * KISAYOLDAN GELEN KOMUTTUR.
     *
     * Bu durumda TÜM ROSTER'a uygulanır.
     */
    if (
            ids == null
                    || ids.isEmpty()
    ) {

        for (
                RosterEntry entry :
                readRoster(captain)
        ) {

            requested.add(
                    entry.uuid()
            );
        }

    } else {

        /*
         * GUI'den gelen seçili üyeler.
         */
        if (ids.size() > MAX_CREW) {
            return;
        }

        requested.addAll(
                ids
        );
    }

    if (requested.isEmpty()) {

        return;
    }

    // =====================================================
    // ÜYELERİ BUL
    // =====================================================

    for (
            RosterEntry entry :
            readRoster(captain)
    ) {

        if (
                !requested.contains(
                        entry.uuid()
                )
        ) {

            continue;
        }

        /*
         * Uzak / başka boyuttaki entity'yi
         * roster üzerinden bul.
         */
        Mob mob =
                findRecruit(
                        captain.getServer(),
                        captain,
                        entry
                );

        if (mob == null) {
            continue;
        }

        // =================================================
        // KOMUTLAR
        // =================================================

        switch (command) {

            // =============================================
            // TAKİP
            // =============================================

            case "follow" -> {

                mob.getPersistentData()
                        .putString(
                                STATE,
                                "follow"
                        );

                clearCombatData(
                        mob
                );

                mob.setTarget(null);

                /*
                 * Gemide / araçta ise ayrılsın.
                 */
                if (mob.isPassenger()) {
                    mob.stopRiding();
                }

                mob.getNavigation()
                        .stop();

                /*
                 * State'i roster'a kaydet.
                 */
                updateRosterState(
                        captain,
                        mob,
                        "follow"
                );
            }

            // =============================================
            // DUR
            // =============================================

            case "stop" -> {

                mob.getPersistentData()
                        .putString(
                                STATE,
                                "stop"
                        );

                clearCombatData(
                        mob
                );

                mob.setTarget(null);

                mob.getNavigation()
                        .stop();

                if (mob.isPassenger()) {
                    mob.stopRiding();
                }

                updateRosterState(
                        captain,
                        mob,
                        "stop"
                );
            }

            // =============================================
            // SAVUN
            // =============================================

            case "defend" -> {

                mob.getPersistentData()
                        .putString(
                                STATE,
                                "defend"
                        );

                /*
                 * Şu an bulunduğu konumu savunma
                 * noktası yap.
                 */
                mob.getPersistentData()
                        .putDouble(
                                DEFEND_X,
                                mob.getX()
                        );

                mob.getPersistentData()
                        .putDouble(
                                DEFEND_Y,
                                mob.getY()
                        );

                mob.getPersistentData()
                        .putDouble(
                                DEFEND_Z,
                                mob.getZ()
                        );

                mob.getPersistentData()
                        .putBoolean(
                                DEFEND_POS_SET,
                                true
                        );

                clearCombatData(
                        mob
                );

                mob.setTarget(null);

                mob.getNavigation()
                        .stop();

                updateRosterState(
                        captain,
                        mob,
                        "defend"
                );
            }

            // =============================================
            // ATAK
            // =============================================

            case "attack" -> {

                /*
                 * Her durumda ATAK modu aktif.
                 */
                mob.getPersistentData()
                        .putString(
                                STATE,
                                "attack"
                        );

                mob.getPersistentData()
                        .remove(
                                DEFEND_TARGET
                        );

                /*
                 * Kaptanın mevcut hedefini al.
                 */
                LivingEntity target =
                        findCaptainCurrentTarget(
                                captain,
                                mob
                        );

                if (
                        target != null
                                && target != captain
                                && target != mob
                                && !isSameCrew(
                                mob,
                                target
                        )
                ) {

                    setCombatTarget(
                            mob,
                            target
                    );

                } else {

                    mob.getPersistentData()
                            .remove(
                                    COMBAT_TARGET
                            );

                    mob.setTarget(null);
                }

                updateRosterState(
                        captain,
                        mob,
                        "attack"
                );
            }

            // =============================================
            // IŞINLA
            // =============================================

            case "teleport" -> {

                /*
                 * Doğrudan kaptanın yanına getir.
                 *
                 * Aynı dimension:
                 * normal teleport
                 *
                 * Farklı dimension:
                 * ServerLevel teleport
                 */
                teleportRecruitToOwner(
                        mob,
                        captain
                );

                mob.getNavigation()
                        .stop();

                updateRosterState(
                        captain,
                        mob,
                        mob.getPersistentData()
                                .getString(
                                        STATE
                                )
                );
            }
        }
    }

    sendCrewMembers(
            captain
    );
}

    // =========================================================
    // KOMUT DURUMU GÜNCELLE
    // =========================================================

    private static void updateRosterState(
            ServerPlayer captain,
            Mob mob,
            String state
    ) {

        ListTag roster =
                getRoster(
                        captain.getPersistentData()
                );

        for (
                int i = 0;
                i < roster.size();
                i++
        ) {

            CompoundTag entry =
                    roster.getCompound(
                            i
                    );

            if (
                    entry.hasUUID(R_UUID)
                            && entry.getUUID(
                            R_UUID
                    ).equals(
                            mob.getUUID()
                    )
            ) {

                entry.putString(
                        R_STATE,
                        state
                );

                entry.putDouble(
                        R_X,
                        mob.getX()
                );

                entry.putDouble(
                        R_Y,
                        mob.getY()
                );

                entry.putDouble(
                        R_Z,
                        mob.getZ()
                );

                entry.putString(
                        R_DIMENSION,
                        mob.level()
                                .dimension()
                                .location()
                                .toString()
                );

                roster.set(
                        i,
                        entry
                );

                return;
            }
        }
    }

    // =========================================================
    // ATAK HEDEFİ
    // =========================================================

    private static LivingEntity findCaptainCurrentTarget(
            ServerPlayer captain,
            Mob member
    ) {

        LivingEntity target =
                captain.getLastHurtMob();

        if (
                isValidCombatTarget(
                        captain,
                        member,
                        target
                )
        ) {

            return target;
        }

        target =
                captain.getLastHurtByMob();

        if (
                isValidCombatTarget(
                        captain,
                        member,
                        target
                )
        ) {

            return target;
        }

        return null;
    }

    private static boolean isValidCombatTarget(
            ServerPlayer captain,
            Mob member,
            LivingEntity target
    ) {

        if (
                target == null
                        || !target.isAlive()
        ) {

            return false;
        }

        if (
                target == captain
                        || target == member
        ) {

            return false;
        }

        if (
                isSameCrew(
                        member,
                        target
                )
        ) {

            return false;
        }

        if (
                target.level()
                        != captain.level()
        ) {

            return false;
        }

        return target.distanceTo(
                captain
        ) <= TARGET_MAX_DISTANCE;
    }

    private static void setCombatTarget(
            Mob mob,
            LivingEntity target
    ) {

        if (
                target == null
                        || !target.isAlive()
                        || isSameCrew(
                        mob,
                        target
                )
        ) {

            return;
        }

        mob.getPersistentData()
                .putUUID(
                        COMBAT_TARGET,
                        target.getUUID()
                );

        mob.getPersistentData()
                .remove(
                        DEFEND_TARGET
                );

        mob.setTarget(
                target
        );

        if (
                mob.distanceTo(
                        target
                ) > 3.0D
        ) {

            mob.getNavigation()
                    .moveTo(
                            target,
                            1.15D
                    );
        }
    }

    // =========================================================
    // RECRUIT TICK
    // =========================================================

    @SubscribeEvent
    public static void onMobTick(
            LivingEvent.LivingTickEvent event
    ) {

        if (!(event.getEntity()
                instanceof Mob mob)) {

            return;
        }

        if (
                mob.level().isClientSide
                        || !isOwned(mob)
        ) {

            return;
        }

        if (!(mob.level()
                instanceof ServerLevel level)) {

            return;
        }

        UUID ownerId =
                readOwner(mob);

        if (ownerId == null) {
            return;
        }

        ServerPlayer owner =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(
                                ownerId
                        );

        if (owner == null) {
            return;
        }

        /*
         * Roster konumunu belirli aralıklarla güncelle.
         */
        if (
                mob.tickCount
                        % ROSTER_UPDATE_INTERVAL
                        == 0
        ) {

            updateRosterFromEntity(
                    owner,
                    mob
            );
        }

        /*
         * Tayfa adı etiketi.
         */
        updateCrewNameTag(
                mob,
                owner
        );

        String state =
                mob.getPersistentData()
                        .getString(
                                STATE
                        );

        // =====================================================
        // STOP
        // =====================================================

        if (
                "stop".equals(state)
                        || state.isBlank()
        ) {

            mob.setTarget(null);

            mob.getNavigation()
                    .stop();

            clearCombatData(
                    mob
            );

            updateRosterState(
                    owner,
                    mob,
                    "stop"
            );

            return;
        }

        // =====================================================
        // FOLLOW
        // =====================================================

        if ("follow".equals(state)) {

            mob.setTarget(null);

            clearCombatData(
                    mob
            );

            if (mob.isPassenger()) {
                mob.stopRiding();
            }

            /*
             * FARKLI BOYUT
             */
            if (mob.level()
                    != owner.level()
            ) {

                teleportRecruitToOwner(
                        mob,
                        owner
                );

                return;
            }

            double distance =
                    mob.distanceTo(
                            owner
                    );

            /*
             * Çok uzaksa ışınlan.
             */
            if (
                    distance
                            > TELEPORT_DISTANCE
            ) {

                mob.teleportTo(
                        owner.getX(),
                        owner.getY(),
                        owner.getZ()
                );

                mob.getNavigation()
                        .stop();

                return;
            }

            /*
             * Yakınsa yürü.
             */
            if (
                    distance
                            > FOLLOW_DISTANCE
            ) {

                mob.getNavigation()
                        .moveTo(
                                owner,
                                1.15D
                        );

            } else {

                mob.getNavigation()
                        .stop();
            }

            return;
        }

        // =====================================================
        // DEFEND
        // =====================================================

        if ("defend".equals(state)) {

            UUID defendId =
                    readDefendTarget(
                            mob
                    );

            LivingEntity attacker =
                    findLivingEntity(
                            level,
                            defendId
                    );

            if (
                    attacker != null
                            && attacker.isAlive()
                            && !isSameCrew(
                            mob,
                            attacker
                    )
            ) {

                if (
                        mob.getTarget()
                                != attacker
                ) {

                    mob.setTarget(
                            attacker
                    );
                }

                mob.getNavigation()
                        .moveTo(
                                attacker,
                                1.15D
                        );

                return;
            }

            mob.getPersistentData()
                    .remove(
                            DEFEND_TARGET
                    );

            mob.setTarget(null);

            /*
             * Savunma noktasına dön.
             */
            if (
                    mob.getPersistentData()
                            .getBoolean(
                                    DEFEND_POS_SET
                            )
            ) {

                double x =
                        mob.getPersistentData()
                                .getDouble(
                                        DEFEND_X
                                );

                double y =
                        mob.getPersistentData()
                                .getDouble(
                                        DEFEND_Y
                                );

                double z =
                        mob.getPersistentData()
                                .getDouble(
                                        DEFEND_Z
                                );

                double distance =
                        mob.distanceToSqr(
                                x,
                                y,
                                z
                        );

                if (
                        distance
                                > RETURN_DISTANCE
                                * RETURN_DISTANCE
                ) {

                    mob.getNavigation()
                            .moveTo(
                                    x,
                                    y,
                                    z,
                                    1.15D
                            );

                } else {

                    mob.getNavigation()
                            .stop();
                }
            }

            return;
        }

        // =====================================================
        // ATTACK
        // =====================================================

        if ("attack".equals(state)) {

            UUID combatId =
                    readCombatTarget(
                            mob
                    );

            UUID defendId =
                    readDefendTarget(
                            mob
                    );

            /*
             * Önce tayfa arkadaşına saldıranı savun.
             */
            LivingEntity defendTarget =
                    findLivingEntity(
                            level,
                            defendId
                    );

            if (
                    defendTarget != null
                            && defendTarget.isAlive()
                            && defendTarget != owner
                            && !isSameCrew(
                            mob,
                            defendTarget
                    )
            ) {

                if (
                        mob.getTarget()
                                != defendTarget
                ) {

                    mob.setTarget(
                            defendTarget
                    );
                }

                mob.getNavigation()
                        .moveTo(
                                defendTarget,
                                1.15D
                        );

                return;
            }

            if (
                    defendId != null
                            && (
                            defendTarget == null
                                    || !defendTarget.isAlive()
                                    || isSameCrew(
                                    mob,
                                    defendTarget
                            )
                    )
            ) {

                mob.getPersistentData()
                        .remove(
                                DEFEND_TARGET
                        );
            }

            /*
             * Ana hedef.
             */
            LivingEntity combatTarget =
                    findLivingEntity(
                            level,
                            combatId
                    );

            if (
                    combatTarget != null
                            && combatTarget.isAlive()
                            && combatTarget != owner
                            && !isSameCrew(
                            mob,
                            combatTarget
                    )
            ) {

                if (
                        mob.getTarget()
                                != combatTarget
                ) {

                    mob.setTarget(
                            combatTarget
                    );
                }

                /*
                 * Hedefe hareket.
                 */
                if (
                        mob.distanceTo(
                                combatTarget
                        ) > 3.0D
                ) {

                    mob.getNavigation()
                            .moveTo(
                                    combatTarget,
                                    1.15D
                            );
                }

                return;
            }

            if (combatId != null) {

                mob.getPersistentData()
                        .remove(
                                COMBAT_TARGET
                        );
            }

            mob.setTarget(null);

            /*
             * ATAK + hedef yok:
             * kaptanı takip et.
             */

            if (mob.isPassenger()) {
                mob.stopRiding();
            }

            if (mob.level()
                    != owner.level()
            ) {

                teleportRecruitToOwner(
                        mob,
                        owner
                );

                return;
            }

            double distance =
                    mob.distanceTo(
                            owner
                    );

            if (
                    distance
                            > TELEPORT_DISTANCE
            ) {

                mob.teleportTo(
                        owner.getX(),
                        owner.getY(),
                        owner.getZ()
                );

                mob.getNavigation()
                        .stop();

                return;
            }

            if (
                    distance
                            > FOLLOW_DISTANCE
            ) {

                mob.getNavigation()
                        .moveTo(
                                owner,
                                1.15D
                        );

            } else {

                mob.getNavigation()
                        .stop();
            }

            return;
        }

        /*
         * Bilinmeyen state.
         */
        mob.getPersistentData()
                .putString(
                        STATE,
                        "follow"
                );

        clearCombatData(
                mob
        );

        mob.setTarget(null);
    }

    // =========================================================
    // FARKLI BOYUTA TAYFA ÜYESİ TAŞI
    // =========================================================

    private static void teleportRecruitToOwner(
        Mob mob,
        ServerPlayer owner
) {

    if (!(owner.level() instanceof ServerLevel targetLevel)) {
        return;
    }

    /*
     * Recruit'ı kaptanın bulunduğu dimension'a
     * ve konuma taşı.
     */
    mob.teleportTo(
            targetLevel,
            owner.getX(),
            owner.getY(),
            owner.getZ(),
            java.util.Set.of(),
            owner.getYRot(),
            owner.getXRot()
    );

    mob.getNavigation().stop();
} 

    // =========================================================
    // ENTITY BUL
    // =========================================================

    private static LivingEntity findLivingEntity(
            ServerLevel level,
            UUID id
    ) {

        if (id == null) {
            return null;
        }

        Entity entity =
                level.getEntity(
                        id
                );

        if (
                entity instanceof LivingEntity living
        ) {

            return living;
        }

        return null;
    }

    // =========================================================
    // AYNI TAYFA
    // =========================================================

    private static boolean isSameCrew(
            Mob mob,
            LivingEntity entity
    ) {

        UUID ownerA =
                readOwner(
                        mob
                );

        if (ownerA == null) {
            return false;
        }

        if (
                entity instanceof Mob otherMob
        ) {

            UUID ownerB =
                    readOwner(
                            otherMob
                    );

            return ownerB != null
                    && ownerA.equals(
                    ownerB
            );
        }

        if (
                entity instanceof Player player
        ) {

            return ownerA.equals(
                    player.getUUID()
            );
        }

        return false;
    }

    // =========================================================
    // CREW DAĞIT
    // =========================================================

    public static void disbandCrew(
            ServerPlayer captain
    ) {

        if (!hasCrew(captain)) {
            return;
        }

        /*
         * Roster'daki bütün üyeleri bul.
         */
        List<RosterEntry> roster =
                readRoster(
                        captain
                );

        for (
                RosterEntry entry :
                roster
        ) {

            Mob mob =
                    findRecruit(
                            captain.getServer(),
                            captain,
                            entry
                    );

            if (mob == null) {
                continue;
            }

            mob.getPersistentData()
                    .remove(
                            OWNER
                    );

            mob.getPersistentData()
                    .remove(
                            RECRUITED
                    );

            mob.getPersistentData()
                    .remove(
                            ORIGINAL_NAME
                    );

            clearCombatData(
                    mob
            );

            mob.getPersistentData()
                    .remove(
                            DEFEND_POS_SET
                    );

            mob.setCustomName(
                    null
            );

            mob.setCustomNameVisible(
                    false
            );

            mob.setTarget(null);
        }

        CompoundTag data =
                captain.getPersistentData();

        data.putBoolean(
                CREW_EXISTS,
                false
        );

        data.remove(
                CREW_NAME
        );

        data.remove(
                CREW_LOGO
        );

        data.putInt(
                CREW_COUNT,
                0
        );

        data.remove(
                ROSTER
        );

        msg(
                captain,
                "☠ Tayfan dağıtıldı.",
                ChatFormatting.RED
        );

        sendCrewMembers(
                captain
        );
    }

    // =========================================================
    // ÖLÜM
    // =========================================================

    @SubscribeEvent
    public static void onRecruitDeath(
            LivingDeathEvent event
    ) {

        if (!(event.getEntity()
                instanceof Mob mob)) {

            return;
        }

        if (!isOwned(mob)) {
            return;
        }

        UUID ownerId =
                readOwner(
                        mob
                );

        if (ownerId == null) {
            return;
        }

        MinecraftServer server =
                mob.getServer();

        if (server == null) {
            return;
        }

        ServerPlayer owner =
                server.getPlayerList()
                        .getPlayer(
                                ownerId
                        );

        if (owner == null) {
            return;
        }

        incrementCrewCount(
                owner,
                -1
        );

        ListTag roster =
                getRoster(
                        owner.getPersistentData()
                );

        removeRosterEntry(
                roster,
                mob.getUUID()
        );

        owner.getPersistentData()
                .put(
                        ROSTER,
                        roster
                );
    }

    // =========================================================
    // OWNER
    // =========================================================

    public static boolean isOwned(
            Entity entity
    ) {

        return entity
                .getPersistentData()
                .getBoolean(
                        RECRUITED
                )
                && entity
                .getPersistentData()
                .hasUUID(
                        OWNER
                );
    }

    public static boolean isOwnedBy(
            Entity entity,
            Player player
    ) {

        return isOwned(
                entity
        )
                && player
                        .getUUID()
                        .equals(
                                readOwner(
                                        entity
                                )
                        );
    }

    private static UUID readOwner(
            Entity entity
    ) {

        return entity
                .getPersistentData()
                .hasUUID(
                        OWNER
                )
                ? entity
                .getPersistentData()
                .getUUID(
                        OWNER
                        )
                : null;
    }

    // =========================================================
    // SAYI
    // =========================================================

    private static void incrementCrewCount(
            Player player,
            int delta
    ) {

        CompoundTag data =
                player.getPersistentData();

        data.putInt(
                CREW_COUNT,
                Math.max(
                        0,
                        data.getInt(
                                CREW_COUNT
                        ) + delta
                )
        );
    }

    public static int countCrew(
            Player player
    ) {

        return player
                .getPersistentData()
                .getInt(
                        CREW_COUNT
                );
    }

    // =========================================================
    // COMBAT DATA
    // =========================================================

    private static void clearCombatData(
            Mob mob
    ) {

        mob.getPersistentData()
                .remove(
                        COMBAT_TARGET
                );

        mob.getPersistentData()
                .remove(
                        DEFEND_TARGET
                );
    }

    private static UUID readCombatTarget(
            Mob mob
    ) {

        return mob
                .getPersistentData()
                .hasUUID(
                        COMBAT_TARGET
                )
                ? mob
                .getPersistentData()
                .getUUID(
                        COMBAT_TARGET
                        )
                : null;
    }

    private static UUID readDefendTarget(
            Mob mob
    ) {

        return mob
                .getPersistentData()
                .hasUUID(
                        DEFEND_TARGET
                )
                ? mob
                .getPersistentData()
                .getUUID(
                        DEFEND_TARGET
                        )
                : null;
    }

    // =========================================================
    // MESAJ
    // =========================================================

    private static void msg(
            Player player,
            String text,
            ChatFormatting style
    ) {

        player.displayClientMessage(
                Component.literal(
                        text
                ).withStyle(
                        style
                ),
                true
        );
    }
}