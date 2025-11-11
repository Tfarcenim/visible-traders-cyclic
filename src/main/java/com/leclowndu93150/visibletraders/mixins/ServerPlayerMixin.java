package com.leclowndu93150.visibletraders.mixins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import com.leclowndu93150.visibletraders.ServerPlayerDuck;
import com.leclowndu93150.visibletraders.VillagerDuck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements ServerPlayerDuck {

    @Shadow public abstract void sendMerchantOffers(int i, MerchantOffers merchantOffers, int j, int k, boolean bl, boolean bl2);

    @Unique
    private static final Logger visibleTraders_NeoForge$visibleTradersLogger = LoggerFactory.getLogger("Visible Traders");


    @Override
    public void visibleTraders$wrapAndSendMerchantOffers(Merchant merchant, int syncId, MerchantOffers merchantOffers, int levelProgress, int experience, boolean leveled, boolean refreshable) {
        if(!(merchant instanceof Villager villager)) {
            sendMerchantOffers(syncId, merchantOffers, levelProgress, experience, leveled, refreshable);
            return;
        }

        visibleTraders_NeoForge$visibleTradersLogger.info("Sending merchant offers. Regular offers: {}", merchantOffers.size());

        MerchantOffers lockedOffers = ((VillagerDuck) villager).visibleTraders$getLockedOffers();
        visibleTraders_NeoForge$visibleTradersLogger.info("Locked offers: {}", lockedOffers.size());

        int level = levelProgress | (((VillagerDuck)villager).visibleTraders$getAvailableOffersCount() << 8);
        MerchantOffers offersCopy = new MerchantOffers();
        offersCopy.addAll(merchantOffers);
        offersCopy.addAll(lockedOffers);

        visibleTraders_NeoForge$visibleTradersLogger.info("Total offers being sent: {}, Level info: {}", offersCopy.size(), level);

        sendMerchantOffers(syncId, offersCopy, level, experience, leveled, refreshable);
    }
}
