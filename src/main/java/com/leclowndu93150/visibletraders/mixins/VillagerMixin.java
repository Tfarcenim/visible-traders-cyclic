package com.leclowndu93150.visibletraders.mixins;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ReputationEventHandler;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import com.leclowndu93150.visibletraders.ServerPlayerDuck;
import com.leclowndu93150.visibletraders.VillagerDuck;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager implements ReputationEventHandler, VillagerDataHolder, VillagerDuck {

    @Shadow public abstract @NonNull VillagerData getVillagerData();

    @Shadow public abstract void setVillagerData(@NonNull VillagerData villagerData);

    @Shadow public abstract void updateTrades();

    @Shadow public abstract void onReputationEventFrom(@NonNull ReputationEventType reputationEventType, @NonNull Entity entity);

    @Unique
    private static final Logger visibleTraders_NeoForge$visibleTradersLogger = LoggerFactory.getLogger("Visible Traders");

    @Unique
    private List<MerchantOffers> visibleTraders_NeoForge$lockedOffers = null;

    @Unique
    private MerchantOffer visibleTraders_NeoForge$cachedTrade = null;

    @Unique
    private int visibleTraders_NeoForge$prevLevel = 0;

    public VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void writeOfferingLevel(CompoundTag compoundTag, CallbackInfo ci) {
        if(this.visibleTraders_NeoForge$lockedOffers == null) return;

        ListTag lockedOffersTag = new ListTag();
        for(MerchantOffers offers : this.visibleTraders_NeoForge$lockedOffers) {
            CompoundTag offerTag = offers.createTag();
            lockedOffersTag.add(offerTag);
        }
        compoundTag.put("LockedOffers", lockedOffersTag);
    }

    @Unique
    private void visibleTraders_NeoForge$lockedTradesTick() {
        int level = this.getVillagerData().getLevel();
        visibleTraders_NeoForge$prevLevel = level;

        if(this.offers == null) {
            this.visibleTraders_NeoForge$lockedOffers = null;
            return;
        }

        if(visibleTraders_NeoForge$cachedTrade == null || this.offers.isEmpty()) {
            this.visibleTraders_NeoForge$lockedOffers = null;
            if(!this.offers.isEmpty()) {
                this.visibleTraders_NeoForge$cachedTrade = this.offers.get(0);
            }
            return;
        }

        if(visibleTraders_NeoForge$cachedTrade != this.offers.get(0)) {
            this.visibleTraders_NeoForge$lockedOffers = null;
            this.visibleTraders_NeoForge$cachedTrade = this.offers.get(0);
            return;
        }

        if(this.visibleTraders_NeoForge$lockedOffers == null) {
            this.visibleTraders_NeoForge$lockedOffers = new ArrayList<>();
        }

        int size = this.visibleTraders_NeoForge$lockedOffers.size();

        if(size + level == 5) {
            return;
        }

        if(size > 0 && size + level > 5) {
            this.visibleTraders_NeoForge$lockedOffers.remove(0);
            return;
        }

        VillagerData data = this.getVillagerData();
        this.setVillagerData(data.setLevel(data.getLevel() + size + 1));
        int prev = this.offers.size();
        this.updateTrades();
        int dif = this.offers.size() - prev;

        MerchantOffers newOffers = new MerchantOffers();
        for(int i = 0; i < dif; i++) {
            newOffers.add(this.offers.remove(this.offers.size() - 1));
        }

        this.visibleTraders_NeoForge$lockedOffers.add(newOffers);
        this.setVillagerData(data);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void updateLockedTradesOnTick(CallbackInfo ci) {
        if(this.isClientSide()) return;
        if(!this.level().hasChunk((int) (this.getX() / 16), (int) (this.getZ() / 16))) return;

        visibleTraders_NeoForge$lockedTradesTick();
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readOfferingLevel(CompoundTag compoundTag, CallbackInfo ci) {
        if(compoundTag.contains("LockedOffers")) {
            ListTag lockedOffersTag = compoundTag.getList("LockedOffers", 10); // 10 is for CompoundTag
            this.visibleTraders_NeoForge$lockedOffers = new ArrayList<>();

            for(int i = 0; i < lockedOffersTag.size(); i++) {
                CompoundTag offerTag = lockedOffersTag.getCompound(i);
                MerchantOffers offers = new MerchantOffers(offerTag);
                this.visibleTraders_NeoForge$lockedOffers.add(offers);
            }
        } else {
            this.visibleTraders_NeoForge$lockedOffers = new ArrayList<>();
        }

        if(this.offers != null && !this.offers.isEmpty()) {
            visibleTraders_NeoForge$cachedTrade = this.offers.get(0);
        }
    }

    @Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
    private void preventAdditionalTradesOnRankIncrease(CallbackInfo ci) {
        if(this.offers == null) return;
        if(this.visibleTraders_NeoForge$lockedOffers == null) return;
        if(visibleTraders_NeoForge$prevLevel != this.getVillagerData().getLevel()) return;
        if(!this.visibleTraders_NeoForge$lockedOffers.isEmpty() && this.visibleTraders_NeoForge$lockedOffers.size() + this.getVillagerData().getLevel() > 5) {
            MerchantOffers newOffers = visibleTraders_NeoForge$lockedOffers.remove(0);
            this.offers.addAll(newOffers);
            ci.cancel();
        }
    }

    @Inject(method = "increaseMerchantCareer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;updateTrades()V"))
    private void updateLastKnownLevelOnCareerIncrease(CallbackInfo ci) {
        this.visibleTraders_NeoForge$prevLevel = this.getVillagerData().getLevel();
    }

    @Override
    public void visibleTraders$forceTradeGeneration() {
        for(int i = 0; i < 5; i++) visibleTraders_NeoForge$lockedTradesTick();
    }

    @Override
    public int visibleTraders$getAvailableOffersCount() {
        if(this.offers == null) return 0;
        return this.offers.size();
    }

    @Override
    public MerchantOffers visibleTraders$getLockedOffers() {
        if(this.visibleTraders_NeoForge$lockedOffers == null) return new MerchantOffers();
        MerchantOffers lockedOffers = new MerchantOffers();
        for(MerchantOffers listOffers : List.copyOf(this.visibleTraders_NeoForge$lockedOffers)) for(MerchantOffer offer : listOffers) {
            if(offer.getResult().isEmpty()) {
                this.visibleTraders_NeoForge$lockedOffers = new ArrayList<>();
                visibleTraders_NeoForge$visibleTradersLogger.error("detected incomplete trade. Rebuilding locked offers");
                return new MerchantOffers();
            }
            lockedOffers.add(offer);
        }
        return lockedOffers;
    }

    @Override
    public void openTradingScreen(Player player, Component displayName, int level) {
        OptionalInt containerID = player.openMenu(new SimpleMenuProvider((syncId, playerInventory, playerx) -> new MerchantMenu(syncId, playerInventory, this), displayName));

        if (containerID.isPresent() && player instanceof ServerPlayer serverPlayer) {
            ((ServerPlayerDuck)serverPlayer).visibleTraders$wrapAndSendMerchantOffers(this, containerID.getAsInt(), this.getOffers(),
                    level, this.getVillagerXp(), this.showProgressBar(), this.canRestock());
        }
    }

    @Inject(
            method = "startTrading",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/npc/Villager;openTradingScreen(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;I)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void beforeOpenTrading(Player player, CallbackInfo ci) {
        Villager villager = (Villager)(Object)this;
        if (villager instanceof VillagerDuck duck) {
            duck.visibleTraders$forceTradeGeneration();
        }
    }
}
