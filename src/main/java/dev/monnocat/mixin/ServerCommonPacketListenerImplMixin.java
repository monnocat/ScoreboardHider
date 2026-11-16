// Copyright (c) 2026 monnocat

package dev.monnocat.mixin;

import static dev.monnocat.ScoreboardHider.hiddenMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {
	@Shadow
	abstract GameProfile playerProfile();

	@Inject(method = "send", at = @At("HEAD"), cancellable = true)
	private void cancelWhenHidden(Packet<?> packet, CallbackInfo info) {
		boolean hidden = hiddenMap.getOrDefault(this.playerProfile().id(), false);

		if (hidden) {
			if (packet instanceof ClientboundSetDisplayObjectivePacket
					|| packet instanceof ClientboundSetObjectivePacket) {
				info.cancel();
			}
		}
	}
}
