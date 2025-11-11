package com.leclowndu93150.visibletraders.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import com.leclowndu93150.visibletraders.MerchantMenuDuck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixinClient extends AbstractContainerScreen<MerchantMenu> {

    @Shadow int scrollOff;

    public MerchantScreenMixinClient(MerchantMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Inject(method = "render",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen$TradeOfferButton;visible:Z",
                    shift = At.Shift.AFTER))
    private void setLockedTradeVisibility(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick,
                                          CallbackInfo ci, @Local MerchantScreen.TradeOfferButton tradeOfferButton) {
        tradeOfferButton.active = ((MerchantMenuDuck) this.menu).visibleTraders$shouldAllowTrade(tradeOfferButton.getIndex() + scrollOff);
    }
}
