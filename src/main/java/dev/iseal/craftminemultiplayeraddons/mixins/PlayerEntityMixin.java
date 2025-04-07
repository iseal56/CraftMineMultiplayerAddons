package dev.iseal.craftminemultiplayeraddons.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.jetbrains.annotations.UnknownNullability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Unique
    private boolean experienceLocked = false;

    @WrapMethod(method = "addExperience")
    @SuppressWarnings("DataFlowIssue")
    public void addExperience(int experience, Operation<Void> original) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (
                !experienceLocked
                && experience > 0
                && player instanceof ServerPlayerEntity serverPlayer
        ) {
            experienceLocked = true;
            original.call(experience);

            PlayerLookup
                    .all(serverPlayer.getServerWorld().getGameInstance().getServer())
                    .stream()
                    .filter(p -> p != serverPlayer)
                    .filter(p -> !((PlayerEntityMixin) (Object) p).experienceLocked)
                    .forEach(p -> p.addExperience(experience));
            experienceLocked = false;
        }
    }
}
