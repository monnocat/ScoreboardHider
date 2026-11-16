// Copyright (c) 2026 monnocat

package dev.monnocat;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.Command;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

public class ScoreboardHider implements ModInitializer {
	public static final String MOD_ID = "scoreboardhider";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static HashMap<UUID, Boolean> hiddenMap = new HashMap<>();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("sb").executes(context -> {
				ServerPlayer player = context.getSource().getPlayer();

				if (player == null) {
					context.getSource().sendFailure(Component.literal("This command can only be used by a player!"));
					return 0;
				}

				boolean hidden = !hiddenMap.getOrDefault(player.getUUID(), false);

				MinecraftServer server = context.getSource().getServer();
				Scoreboard scoreboard = server.getScoreboard();
				Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

				// Stop cancelling packets for a moment
				hiddenMap.put(player.getUUID(), false);

				// Four cases:
				if (!hidden && objective == null) {
					// hidden=false, no scoreboard
					// -> do nothing
				} else if (!hidden && objective != null) {
					// hidden=false, yes scoreboard
					// -> send show packet and scores
					player.connection.send(new ClientboundSetObjectivePacket(objective, 0));
					player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));
					scoreboard.listPlayerScores(objective).forEach((score) -> {
						player.connection.send(new ClientboundSetScorePacket(score.owner(), objective.getName(),
								score.value(), Optional.ofNullable(score.display()),
								Optional.ofNullable(score.numberFormatOverride())
										.or(() -> Optional.ofNullable(objective.numberFormat()))));
					});
				} else if (hidden && objective == null) {
					// hidden=true, no scoreboard
					// -> do nothing
				} else if (hidden && objective != null) {
					// hidden=true, yes scoreboard
					// -> send hide packet
					player.connection.send(new ClientboundSetObjectivePacket(objective, 1));
				}

				hiddenMap.put(player.getUUID(), hidden);
				context.getSource().sendSuccess(
						() -> Component.literal(hidden ? "Hiding scoreboard." : "Showing scoreboard."), false);
				return Command.SINGLE_SUCCESS;
			}));
		});
	}

	public static Identifier id(@NonNull String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
