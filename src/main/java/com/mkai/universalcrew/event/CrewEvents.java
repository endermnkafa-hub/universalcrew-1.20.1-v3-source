package com.mkai.universalcrew.event;

import com.mkai.universalcrew.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    /*
     * Canlının recruit edilmeden önceki gerçek adı.
     *
     * Örnek:
     * Zoro
     * Luffy
     * Warden
     */
    private static final String ORIGINAL_NAME =
            "UniversalCrewOriginalName";

    /*
     * ATAK komutuyla verilen gerçek hedef.
     */
    private static final String COMBAT_TARGET =
            "UniversalCrewCombatTarget";

    /*
     * Kendisine / tayfa arkadaşına saldıran düşman.
     */
    private static final String DEFEND_TARGET =
            "UniversalCrewDefendTarget";

    /*
     * SAVUN başlangıç noktası.
     */
    private static final String DEFEND_X =
            "UniversalCrewDefendX";

    private static final String DEFEND_Y =
            "UniversalCrewDefendY";

    private static final String DEFEND_Z =
            "UniversalCrewDefendZ";

    private static final String DEFEND_POS_SET =
            "UniversalCrewDefendPositionSet";

    private static final String REQ_ITEM =
            "UniversalCrewReqItem";

    private static final String REQ_COUNT =
            "UniversalCrewReqCount";

    private static final String REQ_KNOWN =
            "UniversalCrewReqKnown";

    private static final String CREW_COUNT =
            "UniversalCrewCount";

    public static final int MAX_CREW = 12;

    private static final double FOLLOW_DISTANCE = 5.0D;

    private static final double TELEPORT_DISTANCE = 48.0D;

    private static final double DEFEND_RADIUS = 50.0D;

    private static final double RETURN_DISTANCE = 2.5D;

    private static final double TARGET_MAX_DISTANCE = 128.0D;

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
    // DAVET EŞYASI
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

        // =====================================================
        // FOLLOW
        // =====================================================

        if ("follow".equals(state)) {

            event.setCanceled(true);

            return;
        }

        // =====================================================
        // STOP
        // =====================================================

        if ("stop".equals(state)) {

            event.setCanceled(true);

            return;
        }

        // =====================================================
        // DEFEND
        // =====================================================

        if ("defend".equals(state)) {

            if (
                    isSameCrew(
                            mob,
                            target
                    )
            ) {

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

        // =====================================================
        // ATTACK
        // =====================================================

        if ("attack".equals(state)) {

            if (
                    isSameCrew(
                            mob,
                            target
                    )
            ) {

                event.setCanceled(true);

                return;
            }

            UUID commandTarget =
                    readCombatTarget(
                            mob
                    );

            UUID defendTarget =
                    readDefendTarget(
                            mob
                    );

            if (
                    commandTarget != null
                            && target.getUUID()
                            .equals(
                                    commandTarget
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
    // OYUNCU / TAYFA SALDIRISI
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

        Entity sourceEntity =
                event.getSource()
                        .getEntity();

        if (!(sourceEntity
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

            if (!(captain.level()
                    instanceof ServerLevel level)) {

                return;
            }

            List<Mob> crew =
                    level.getEntitiesOfClass(
                            Mob.class,
                            captain.getBoundingBox()
                                    .inflate(
                                            TARGET_MAX_DISTANCE
                                    ),
                            mob ->
                                    isOwnedBy(
                                            mob,
                                            captain
                                    )
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

                if (!"attack".equals(state)) {
                    continue;
                }

                /*
                 * Kaptanın vurduğu canlıyı
                 * ATAK hedefi yap.
                 */
                member.getPersistentData()
                        .putUUID(
                                COMBAT_TARGET,
                                victim.getUUID()
                        );

                /*
                 * Eski savunma hedefini bırak.
                 */
                member.getPersistentData()
                        .remove(
                                DEFEND_TARGET
                        );

                /*
                 * Hedefi entity'nin kendi AI'sına ver.
                 */
                member.setTarget(
                        victim
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
                    level.getEntitiesOfClass(
                            Mob.class,
                            captain.getBoundingBox()
                                    .inflate(
                                            DEFEND_RADIUS
                                    ),
                            mob ->
                                    isOwnedBy(
                                            mob,
                                            captain
                                    )
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

            if (!(victimMob.level()
                    instanceof ServerLevel level)) {

                return;
            }

            ServerPlayer captain =
                    level.getServer()
                            .getPlayerList()
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
                    level.getEntitiesOfClass(
                            Mob.class,
                            victimMob.getBoundingBox()
                                    .inflate(
                                            DEFEND_RADIUS
                                    ),
                            mob ->
                                    isOwnedBy(
                                            mob,
                                            captain
                                    )
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

        if (!data.getBoolean(
                REQ_KNOWN
        )) {

            ItemChoice request =
                    requestFor(
                            mob
                    );

            data.putString(
                    REQ_ITEM,
                    BuiltInRegistries.ITEM
                            .getKey(
                                    request.item()
                            )
                            .toString()
            );

            data.putInt(
                    REQ_COUNT,
                    request.count()
            );

            data.putBoolean(
                    REQ_KNOWN,
                    true
            );

            msg(
                    player,
                    mob.getName()
                            .getString()
                            + ": "
                            + request.count()
                            + "x "
                            + request.item()
                            .getDescription()
                            .getString()
                            + " getir, sonra tekrar davet et!",
                    ChatFormatting.YELLOW
            );

            return;
        }

        ResourceLocation id =
                ResourceLocation.tryParse(
                        data.getString(
                                REQ_ITEM
                        )
                );

        Item requested =
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
                        data.getInt(
                                REQ_COUNT
                        )
                );

        if (!removeItems(
                player,
                requested,
                count
        )) {

            msg(
                    player,
                    mob.getName()
                            .getString()
                            + " şunu istiyor: "
                            + count
                            + "x "
                            + requested
                            .getDescription()
                            .getString(),
                    ChatFormatting.YELLOW
            );

            return;
        }

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

    private record ItemChoice(
            Item item,
            int count
    ) {
    }

    private static ItemChoice requestFor(
            Mob mob
    ) {

        int pick =
                Math.floorMod(
                        mob.getUUID()
                                .hashCode(),
                        5
                );

        return switch (pick) {

            case 0 ->
                    new ItemChoice(
                            Items.BREAD,
                            4
                    );

            case 1 ->
                    new ItemChoice(
                            Items.COOKED_BEEF,
                            2
                    );

            case 2 ->
                    new ItemChoice(
                            Items.IRON_INGOT,
                            3
                    );

            case 3 ->
                    new ItemChoice(
                            Items.GOLD_INGOT,
                            2
                    );

            default ->
                    new ItemChoice(
                            Items.EMERALD,
                            1
                    );
        };
    }

    private static boolean removeItems(
            Player player,
            Item item,
            int amount
    ) {

        if (
                player.getAbilities()
                        .instabuild
        ) {

            return true;
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

            if (stack.is(item)) {

                remaining -=
                        Math.min(
                                remaining,
                                stack.getCount()
                        );
            }
        }

        if (remaining > 0) {
            return false;
        }

        remaining =
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

        return true;
    }

    // =========================================================
    // RECRUIT
    // =========================================================

    private static void recruit(
            Player player,
            Mob mob
    ) {

        /*
         * Recruit edilmeden önce gerçek adını kaydet.
         */
        String originalName =
                mob.getName()
                        .getString();

        mob.getPersistentData()
                .putString(
                        ORIGINAL_NAME,
                        originalName
                );

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

        /*
         * İstenen isim:
         *
         * <Tayfa Adı> [Canlı Adı]
         */
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

        msg(
                player,
                "⚓ "
                        + originalName
                        + " tayfana katıldı!",
                ChatFormatting.GREEN
        );
    }

    // =========================================================
    // CREW EGG
    // =========================================================

    private static void storeRecruit(
            Player player,
            Mob mob
    ) {

        ItemStack egg =
                new ItemStack(
                        ModItems.CREW_EGG.get()
                );

        CompoundTag data =
                new CompoundTag();

        ResourceLocation id =
                BuiltInRegistries.ENTITY_TYPE
                        .getKey(
                                mob.getType()
                        );

        if (id == null) {
            return;
        }

        data.putString(
                "EntityType",
                id.toString()
        );

        data.putUUID(
                OWNER,
                player.getUUID()
        );

        data.putString(
                "State",
                mob.getPersistentData()
                        .getString(
                                STATE
                        )
        );

        /*
         * Gerçek canlı adını egg içine kaydet.
         */
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

        data.putString(
                "OriginalName",
                originalName
        );

        egg.getOrCreateTag()
                .put(
                        "CrewData",
                        data
                );

        player.getInventory()
                .placeItemBackInInventory(
                        egg
                );

        mob.discard();

        msg(
                player,
                "Tayfa üyesi Crew Egg'e saklandı.",
                ChatFormatting.GREEN
        );
    }

    public static boolean tryReleaseEgg(
            Player player,
            ItemStack stack
    ) {

        if (!stack.is(
                ModItems.CREW_EGG.get()
        )) {

            return false;
        }

        CompoundTag root =
                stack.getTag();

        if (
                root == null
                        || !root.contains(
                        "CrewData"
                )
        ) {

            return false;
        }

        CompoundTag data =
                root.getCompound(
                        "CrewData"
                );

        if (
                !data.hasUUID(OWNER)
                        || !player.getUUID()
                        .equals(
                                data.getUUID(
                                        OWNER
                                )
                        )
        ) {

            msg(
                    player,
                    "Bu egg senin tayfana ait değil.",
                    ChatFormatting.RED
            );

            return true;
        }

        if (!(player.level()
                instanceof ServerLevel level)) {

            return true;
        }

        ResourceLocation id =
                ResourceLocation.tryParse(
                        data.getString(
                                "EntityType"
                        )
                );

        if (id == null) {
            return true;
        }

        Mob mob =
                BuiltInRegistries.ENTITY_TYPE
                        .getOptional(id)
                        .map(
                                type ->
                                        type.create(
                                                level
                                        )
                        )
                        .filter(
                                Mob.class::isInstance
                        )
                        .map(
                                Mob.class::cast
                        )
                        .orElse(null);

        if (mob == null) {
            return true;
        }

        mob.moveTo(
                player.getX() + 1.0D,
                player.getY(),
                player.getZ() + 1.0D,
                player.getYRot(),
                player.getXRot()
        );

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

        /*
         * Egg içindeki gerçek adı geri al.
         */
        String originalName =
                data.getString(
                        "OriginalName"
                );

        if (originalName.isBlank()) {

            originalName =
                    mob.getName()
                            .getString();
        }

        mob.getPersistentData()
                .putString(
                        ORIGINAL_NAME,
                        originalName
                );

        /*
         * Crew isim etiketini yeniden oluştur.
         */
        updateCrewNameTag(
                mob,
                player
        );

        mob.setPersistenceRequired();

        mob.setTarget(null);

        level.addFreshEntity(
                mob
        );

        if (
                !player.getAbilities()
                        .instabuild
        ) {

            stack.shrink(
                    1
            );
        }

        msg(
                player,
                "Tayfa üyesi geri çağrıldı.",
                ChatFormatting.GREEN
        );

        return true;
    }

    @SubscribeEvent
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {

        if (
                event.getEntity()
                        .level()
                        .isClientSide
        ) {

            return;
        }

        if (
                !event.getItemStack()
                        .is(
                                ModItems.CREW_EGG.get()
                        )
        ) {

            return;
        }

        if (
                tryReleaseEgg(
                        event.getEntity(),
                        event.getItemStack()
                )
        ) {

            event.setCancellationResult(
                    InteractionResult.SUCCESS
            );

            event.setCanceled(true);
        }
    }

    // =========================================================
    // CREW LİSTESİ
    // =========================================================

    public static void sendCrewMembers(
            ServerPlayer player
    ) {

        List<
                com.mkai.universalcrew.network.CrewMembersPacket.MemberData
                > list =
                new ArrayList<>();

        List<Mob> mobs =
                player.level()
                        .getEntitiesOfClass(
                                Mob.class,
                                player.getBoundingBox()
                                        .inflate(
                                                128.0D
                                        ),
                                mob ->
                                        isOwnedBy(
                                                mob,
                                                player
                                        )
                        );

        for (
                Mob mob :
                mobs
        ) {

            String state =
                    mob.getPersistentData()
                            .getString(
                                    STATE
                            );

            if (state.isBlank()) {
                state = "stop";
            }

            list.add(
                    new com.mkai.universalcrew.network.CrewMembersPacket.MemberData(
                            mob.getUUID(),
                            mob.getName()
                                    .getString(),
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

    // =========================================================
    // CREW DAĞIT
    // =========================================================

    public static void disbandCrew(
            ServerPlayer captain
    ) {

        if (!hasCrew(captain)) {
            return;
        }

        List<Mob> mobs =
                captain.level()
                        .getEntitiesOfClass(
                                Mob.class,
                                captain.getBoundingBox()
                                        .inflate(
                                                128.0D
                                        ),
                                mob ->
                                        isOwnedBy(
                                                mob,
                                                captain
                                        )
                        );

        for (
                Mob mob :
                mobs
        ) {

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

        if ("disband".equals(command)) {

            disbandCrew(
                    captain
            );

            return;
        }

        if (
                ids == null
                        || ids.size() > MAX_CREW
        ) {

            return;
        }

        Set<UUID> requested =
                new HashSet<>(
                        ids
                );

        List<Mob> selected =
                captain.level()
                        .getEntitiesOfClass(
                                Mob.class,
                                captain.getBoundingBox()
                                        .inflate(
                                                128.0D
                                        ),
                                candidate ->
                                        requested.contains(
                                                candidate.getUUID()
                                        )
                                                && isOwnedBy(
                                                candidate,
                                                captain
                                        )
                        );

        for (
                Mob mob :
                selected
        ) {

            switch (command) {

                // =================================================
                // FOLLOW
                // =================================================

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

                    if (mob.isPassenger()) {
                        mob.stopRiding();
                    }

                    mob.getNavigation()
                            .stop();

                    updateCrewNameTag(
                            mob,
                            captain
                    );
                }

                // =================================================
                // STOP
                // =================================================

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

                    updateCrewNameTag(
                            mob,
                            captain
                    );
                }

                // =================================================
                // DEFEND
                // =================================================

                case "defend" -> {

                    mob.getPersistentData()
                            .putString(
                                    STATE,
                                    "defend"
                            );

                    /*
                     * Savunma başlangıç noktası.
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

                    updateCrewNameTag(
                            mob,
                            captain
                    );
                }

                // =================================================
                // ATTACK
                // =================================================

                case "attack" -> {

                    /*
                     * Her durumda ATAK moduna geç.
                     */
                    mob.getPersistentData()
                            .putString(
                                    STATE,
                                    "attack"
                            );

                    /*
                     * Eski savunma hedefini kaldır.
                     */
                    mob.getPersistentData()
                            .remove(
                                    DEFEND_TARGET
                            );

                    /*
                     * Kaptanın mevcut düşmanını bul.
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

                        /*
                         * Hedef yoksa yine ATAK modunda.
                         */
                        mob.getPersistentData()
                                .remove(
                                        COMBAT_TARGET
                                );

                        mob.setTarget(null);
                    }

                    updateCrewNameTag(
                            mob,
                            captain
                    );
                }
            }
        }

        sendCrewMembers(
                captain
        );
    }

    // =========================================================
    // KAPTANIN MEVCUT HEDEFİ
    // =========================================================

    private static LivingEntity findCaptainCurrentTarget(
            ServerPlayer captain,
            Mob member
    ) {

        /*
         * Önce kaptanın vurduğu canlı.
         */
        LivingEntity target =
                captain.getLastHurtMob();

        if (
                target != null
                        && target.isAlive()
                        && target != captain
                        && target != member
                        && !isSameCrew(
                        member,
                        target
                )
        ) {

            if (
                    target.level()
                            == captain.level()
                            && target.distanceTo(
                            captain
                    ) <= TARGET_MAX_DISTANCE
            ) {

                return target;
            }
        }

        /*
         * Sonra kaptana saldıran canlı.
         */
        target =
                captain.getLastHurtByMob();

        if (
                target != null
                        && target.isAlive()
                        && target != captain
                        && target != member
                        && !isSameCrew(
                        member,
                        target
                )
        ) {

            if (
                    target.level()
                            == captain.level()
                            && target.distanceTo(
                            captain
                    ) <= TARGET_MAX_DISTANCE
            ) {

                return target;
            }
        }

        return null;
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

        UUID ownerId =
                readOwner(mob);

        if (
                ownerId == null
                        || !(mob.level()
                        instanceof ServerLevel level)
        ) {

            return;
        }

        ServerPlayer owner =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(
                                ownerId
                        );

        if (
                owner == null
                        || owner.level() != level
        ) {

            mob.getNavigation()
                    .stop();

            mob.setTarget(null);

            return;
        }

        /*
         * Her tick ismin doğru olduğundan emin ol.
         *
         * Bu sayede crew adı değiştirilmişse bile
         * aktif üyelerin etiketi güncellenebilir.
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

            /*
             * Gerçek tehdit varsa ona saldır.
             */
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

            /*
             * Tehdit yok.
             */
            mob.getPersistentData()
                    .remove(
                            DEFEND_TARGET
                    );

            mob.setTarget(null);

            /*
             * SAVUN noktasına dön.
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
             * ATAK modundaki üyeye birisi saldırdıysa
             * önce onu savun.
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
             * ANA ATAK HEDEFİ.
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

                /*
                 * Hedefi mobun kendi AI'sına ver.
                 */
                if (
                        mob.getTarget() == null
                                || mob.getTarget() != combatTarget
                ) {

                    mob.setTarget(
                            combatTarget
                    );
                }

                /*
                 * Hedefe yaklaş.
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

                /*
                 * BURADA SALDIRI ÇAĞRISI YOK.
                 *
                 * Luffy/Zoro/başka mod hangi combat
                 * sistemini kullanıyorsa o devam ediyor.
                 */
                return;
            }

            /*
             * Hedef yok.
             *
             * ATAK modu açık kalıyor ama kaptanın
             * peşine dönüyor.
             */
            if (combatId != null) {

                mob.getPersistentData()
                        .remove(
                                COMBAT_TARGET
                        );
            }

            mob.setTarget(null);

            if (mob.isPassenger()) {
                mob.stopRiding();
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

        // =====================================================
        // BİLİNMEYEN STATE
        // =====================================================

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
    // TARGET AYARLA
    // =========================================================

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

        if (crewName == null
                || crewName.isBlank()) {

            return;
        }

        String originalName =
                mob.getPersistentData()
                        .getString(
                                ORIGINAL_NAME
                        );

        /*
         * Eski save'lerde ORIGINAL_NAME yoksa
         * mobun mevcut adından al.
         */
        if (originalName.isBlank()) {

            originalName =
                    mob.getName()
                            .getString();

            /*
             * Eğer eski formatlı crew adı varsa,
             * tekrar üst üste yazılmasını engelle.
             */
            if (
                    originalName.startsWith("<")
                            && originalName.contains(">[")
            ) {

                int bracket =
                        originalName.indexOf(">[");
                
                if (bracket >= 0) {

                    String clean =
                            originalName.substring(
                                    bracket + 2
                            );

                    if (
                            clean.endsWith("]")
                    ) {

                        clean =
                                clean.substring(
                                        0,
                                        clean.length() - 1
                                );
                    }

                    originalName = clean;
                }
            }

            mob.getPersistentData()
                    .putString(
                            ORIGINAL_NAME,
                            originalName
                    );
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
    // SAYILAR
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
                        )
                                + delta
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
    // COMBAT TEMİZLE
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

    // =========================================================
    // ÖLÜM
    // =========================================================

    @SubscribeEvent
    public static void onRecruitDeath(
            LivingDeathEvent event
    ) {

        if (
                !(event.getEntity()
                        instanceof Mob mob)
                        || !isOwned(mob)
                        || mob.level().isClientSide
        ) {

            return;
        }

        UUID ownerId =
                readOwner(
                        mob
                );

        if (
                ownerId == null
                        || mob.level()
                        .getServer() == null
        ) {

            return;
        }

        ServerPlayer owner =
                mob.level()
                        .getServer()
                        .getPlayerList()
                        .getPlayer(
                                ownerId
                        );

        if (owner != null) {

            incrementCrewCount(
                    owner,
                    -1
            );
        }
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