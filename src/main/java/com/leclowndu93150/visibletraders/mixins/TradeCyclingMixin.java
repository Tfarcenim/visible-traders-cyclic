package com.leclowndu93150.visibletraders.mixins;

import com.leclowndu93150.visibletraders.VillagerDuck;
import com.leclowndu93150.visibletraders.VisibleTraders;
import com.llamalad7.mixinextras.sugar.Local;
import de.maxhenkel.tradecycling.TradeCyclingMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TradeCyclingMod.class, remap = false)
public class TradeCyclingMixin {

    private static Villager capturedVillager;

    @Inject(
            method = "onCycleTrades(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/npc/Villager;setOffers(Lnet/minecraft/world/item/trading/MerchantOffers;)V"
            ),remap = false
    )
    private static void onCycleTrades(ServerPlayer player, CallbackInfo ci, @Local Villager villager) {
        capturedVillager = villager;
        if (villager instanceof VillagerDuck duck) {
            duck.visibleTraders$forceTradeGeneration();
        }
    }

    @Redirect(
            method = "onCycleTrades(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;sendMerchantOffers(ILnet/minecraft/world/item/trading/MerchantOffers;IIZZ)V"
            ),remap = false
    )
    private static void redirectSendOffers(ServerPlayer player, int containerId, MerchantOffers offers,
                                           int level, int xp, boolean showProgress, boolean canRestock) {
        if (capturedVillager != null) {
            VisibleTraders.wrapAndSendMerchantOffers(player,capturedVillager, containerId, offers, level, xp, showProgress, canRestock);
        } else {
            player.sendMerchantOffers(containerId, offers, level, xp, showProgress, canRestock);
        }
    }
}
