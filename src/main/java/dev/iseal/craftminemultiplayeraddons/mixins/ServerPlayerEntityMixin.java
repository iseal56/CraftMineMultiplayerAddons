package dev.iseal.craftminemultiplayeraddons.mixins;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.aprilfools.PlayerUnlock;
import net.minecraft.class_10959;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Final
    @Shadow
    private class_10959 field_58300;

    @Shadow public abstract ServerWorld getServerWorld();

    /**
     * @author ISeal
     * @reason idk
     */
    @Overwrite
    public boolean method_69140(RegistryEntry<PlayerUnlock> registryEntry) {
        // weird ass check default did
        if (!field_58300.method_68941(registryEntry))
            return false;

        int unlockPrice = registryEntry.value().unlockPrice();

        Collection<ServerPlayerEntity> allPlayers = PlayerLookup.all(getServerWorld().getGameInstance().getServer());

        // find players who don't have enough experience
        List<ServerPlayerEntity> playersWithInsufficientExp = allPlayers.stream()
                .filter(player -> (player.experienceLevel - unlockPrice) < 0)
                .toList();

        allPlayers
                .forEach(player -> System.out.println("player "+player.getName().getString()+" has "+player.experienceLevel+" exp levels"));

        if (!playersWithInsufficientExp.isEmpty()) {

            // format player names (joined with commas)
            String playerNames = playersWithInsufficientExp.stream()
                    .map(player -> player.getName().getString())
                    .collect(Collectors.joining(", "));

            // send message to all players
            Text message = Text.literal("The following players don't have enough experience for that upgrade: " + playerNames);
            allPlayers.forEach(player ->
                    player.sendMessage(message, false));
            return false;
        }

        allPlayers.forEach(player -> {
            player.experienceLevel = player.experienceLevel - unlockPrice;

            if (player.experienceLevel < 0) {
                System.out.println("experience level less than 0 for player "+player.getName().getString());
                player.experienceLevel = 0;
                player.experienceProgress = 0.0F;
                player.totalExperience = 0;
            }

            player.method_69142(registryEntry);
            Criteria.PLAYER_UNLOCK_BOUGHT.trigger(player);
            player.syncedExperience = -1;
        });

        return true;
    }
}