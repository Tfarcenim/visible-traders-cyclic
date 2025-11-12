package com.leclowndu93150.visibletraders;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("visibletraders")
public class VisibleTraders {

    public static final Logger LOGGER = LoggerFactory.getLogger("Visible Traders");
    public static final String LOCKED_OFFERS = "LockedOffers";


    public static void wrapAndSendMerchantOffers(ServerPlayer player, Merchant merchant, int syncId, MerchantOffers merchantOffers, int levelProgress, int experience, boolean leveled, boolean refreshable) {
        if(!(merchant instanceof Villager villager)) {
            player.sendMerchantOffers(syncId, merchantOffers, levelProgress, experience, leveled, refreshable);
            return;
        }

        VisibleTraders.LOGGER.info("Sending merchant offers. Regular offers: {}", merchantOffers.size());

        MerchantOffers lockedOffers = ((VillagerDuck) villager).visibleTraders$getLockedOffers();
        VisibleTraders.LOGGER.info("Locked offers: {}", lockedOffers.size());

        int level = levelProgress | (((VillagerDuck)villager).visibleTraders$getAvailableOffersCount() << 8);
        MerchantOffers offersCopy = new MerchantOffers();
        offersCopy.addAll(merchantOffers);
        offersCopy.addAll(lockedOffers);

        VisibleTraders.LOGGER.info("Total offers being sent: {}, Level info: {}", offersCopy.size(), level);

        player.sendMerchantOffers(syncId, offersCopy, level, experience, leveled, refreshable);
    }
}
