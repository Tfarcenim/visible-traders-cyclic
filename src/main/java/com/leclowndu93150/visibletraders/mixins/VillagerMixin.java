package com.leclowndu93150.visibletraders.mixins;

import com.leclowndu93150.visibletraders.VisibleTraders;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import com.leclowndu93150.visibletraders.VillagerDuck;
import org.checkerframework.checker.nullness.qual.NonNull;
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
public abstract class VillagerMixin extends AbstractVillager implements VillagerDuck {

    @Shadow public abstract @NonNull VillagerData getVillagerData();

    @Shadow public abstract void setVillagerData(@NonNull VillagerData villagerData);

    @Shadow
    protected abstract void updateTrades();

    @Unique
    private List<MerchantOffers> visibleTraders$lockedOffers = null;

    @Unique
    private MerchantOffer visibleTraders$cachedTrade = null;

    @Unique
    private int visibleTraders$prevLevel = 0;

    public VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void writeOfferingLevel(CompoundTag compoundTag, CallbackInfo ci) {
        if(this.visibleTraders$lockedOffers == null) return;

        ListTag lockedOffersTag = new ListTag();
        for(MerchantOffers offers : this.visibleTraders$lockedOffers) {
            CompoundTag offerTag = offers.createTag();
            lockedOffersTag.add(offerTag);
        }
        compoundTag.put(VisibleTraders.LOCKED_OFFERS, lockedOffersTag);
    }

    @Unique
    private void visibleTraders$lockedTradesTick() {
        int level = this.getVillagerData().getLevel();
        visibleTraders$prevLevel = level;

        if(this.offers == null) {
            this.visibleTraders$lockedOffers = null;
            return;
        }

        if(visibleTraders$cachedTrade == null || this.offers.isEmpty()) {
            this.visibleTraders$lockedOffers = null;
            if(!this.offers.isEmpty()) {
                this.visibleTraders$cachedTrade = this.offers.get(0);
            }
            return;
        }

        if(visibleTraders$cachedTrade != this.offers.get(0)) {
            this.visibleTraders$lockedOffers = null;
            this.visibleTraders$cachedTrade = this.offers.get(0);
            return;
        }

        if(this.visibleTraders$lockedOffers == null) {
            this.visibleTraders$lockedOffers = new ArrayList<>();
        }

        int size = this.visibleTraders$lockedOffers.size();

        if(size + level == 5) {
            return;
        }

        if(size > 0 && size + level > 5) {
            this.visibleTraders$lockedOffers.remove(0);
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

        this.visibleTraders$lockedOffers.add(newOffers);
        this.setVillagerData(data);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void updateLockedTradesOnTick(CallbackInfo ci) {
        if(this.isClientSide()) return;
        if(!this.level().hasChunk((int) (this.getX() / 16), (int) (this.getZ() / 16))) return;

        visibleTraders$lockedTradesTick();
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readOfferingLevel(CompoundTag compoundTag, CallbackInfo ci) {
        if(compoundTag.contains(VisibleTraders.LOCKED_OFFERS)) {
            ListTag lockedOffersTag = compoundTag.getList(VisibleTraders.LOCKED_OFFERS, Tag.TAG_COMPOUND);
            this.visibleTraders$lockedOffers = new ArrayList<>();

            for(int i = 0; i < lockedOffersTag.size(); i++) {
                CompoundTag offerTag = lockedOffersTag.getCompound(i);
                MerchantOffers offers = new MerchantOffers(offerTag);
                this.visibleTraders$lockedOffers.add(offers);
            }
        } else {
            this.visibleTraders$lockedOffers = new ArrayList<>();
        }

        if(this.offers != null && !this.offers.isEmpty()) {
            visibleTraders$cachedTrade = this.offers.get(0);
        }
    }

    @Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
    private void preventAdditionalTradesOnRankIncrease(CallbackInfo ci) {
        if(this.offers == null) return;
        if(this.visibleTraders$lockedOffers == null) return;
        if(visibleTraders$prevLevel != this.getVillagerData().getLevel()) return;
        if(!this.visibleTraders$lockedOffers.isEmpty() && this.visibleTraders$lockedOffers.size() + this.getVillagerData().getLevel() > 5) {
            MerchantOffers newOffers = visibleTraders$lockedOffers.remove(0);
            this.offers.addAll(newOffers);
            ci.cancel();
        }
    }

    @Inject(method = "increaseMerchantCareer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;updateTrades()V"))
    private void updateLastKnownLevelOnCareerIncrease(CallbackInfo ci) {
        this.visibleTraders$prevLevel = this.getVillagerData().getLevel();
    }

    @Override
    public void visibleTraders$forceTradeGeneration() {
        for(int i = 0; i < 5; i++) visibleTraders$lockedTradesTick();
    }

    @Override
    public int visibleTraders$getAvailableOffersCount() {
        if(this.offers == null) return 0;
        return this.offers.size();
    }

    @Override
    public MerchantOffers visibleTraders$getLockedOffers() {
        if(this.visibleTraders$lockedOffers == null) return new MerchantOffers();
        MerchantOffers lockedOffers = new MerchantOffers();
        for(MerchantOffers listOffers : List.copyOf(this.visibleTraders$lockedOffers)) for(MerchantOffer offer : listOffers) {
            if(offer.getResult().isEmpty()) {
                this.visibleTraders$lockedOffers = new ArrayList<>();
                VisibleTraders.LOGGER.error("detected incomplete trade. Rebuilding locked offers");
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
            VisibleTraders.wrapAndSendMerchantOffers(serverPlayer,this, containerID.getAsInt(), this.getOffers(),
                    level, this.getVillagerXp(), this.showProgressBar(), this.canRestock());
        }
    }

    @Inject(
            method = "startTrading",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/npc/Villager;openTradingScreen(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;I)V"
            )
    )
    private void beforeOpenTrading(Player player, CallbackInfo ci) {
        Villager villager = (Villager)(Object)this;
        if (villager instanceof VillagerDuck duck) {
            duck.visibleTraders$forceTradeGeneration();
        }
    }
}
