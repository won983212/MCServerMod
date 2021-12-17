package com.won983212.servermod.utility;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class EntityUtils {

    @Nullable
    private static Entity createEntityFromNBTSingle(CompoundNBT nbt, World world) {
        try {
            Optional<Entity> optional = EntityType.create(nbt, world);

            if (optional.isPresent()) {
                Entity entity = optional.get();
                entity.setUUID(UUID.randomUUID());
                return entity;
            }
        } catch (Exception e) {
        }

        return null;
    }

    /**
     * Note: This does NOT spawn any of the entities in the world!
     *
     * @param nbt
     * @param world
     * @return
     */
    @Nullable
    public static Entity createEntityAndPassengersFromNBT(CompoundNBT nbt, World world) {
        Entity entity = createEntityFromNBTSingle(nbt, world);

        if (entity == null) {
            return null;
        } else {
            if (nbt.contains("Passengers", Constants.NBT.TAG_LIST)) {
                ListNBT taglist = nbt.getList("Passengers", Constants.NBT.TAG_COMPOUND);

                for (int i = 0; i < taglist.size(); ++i) {
                    Entity passenger = createEntityAndPassengersFromNBT(taglist.getCompound(i), world);

                    if (passenger != null) {
                        passenger.startRiding(entity, true);
                    }
                }
            }

            return entity;
        }
    }

    public static void spawnEntityAndPassengersInWorld(Entity entity, World world) {
        if (world.addFreshEntity(entity) && entity.isVehicle()) {
            for (Entity passenger : entity.getPassengers()) {
                passenger.moveTo(
                        entity.getX(),
                        entity.getY() + entity.getPassengersRidingOffset() + passenger.getMyRidingOffset(),
                        entity.getZ(),
                        passenger.yRot, passenger.xRot);
                setEntityRotations(passenger, passenger.yRot, passenger.xRot);
                spawnEntityAndPassengersInWorld(passenger, world);
            }
        }
    }

    public static void setEntityRotations(Entity entity, float yaw, float pitch) {
        entity.yRot = yaw;
        entity.yRotO = yaw;

        entity.xRot = pitch;
        entity.xRotO = pitch;

        if (entity instanceof LivingEntity) {
            LivingEntity livingBase = (LivingEntity) entity;
            livingBase.yHeadRot = yaw;
            livingBase.yBodyRot = yaw;
            livingBase.yHeadRotO = yaw;
            livingBase.yBodyRotO = yaw;
        }
    }

    @Nullable
    public static Vector3d readEntityPositionFromTag(@Nullable CompoundNBT tag) {
        if (tag != null && tag.contains("Pos", Constants.NBT.TAG_LIST)) {
            ListNBT tagList = tag.getList("Pos", Constants.NBT.TAG_DOUBLE);

            if (tagList.getElementType() == Constants.NBT.TAG_DOUBLE && tagList.size() == 3) {
                return new Vector3d(tagList.getDouble(0), tagList.getDouble(1), tagList.getDouble(2));
            }
        }

        return null;
    }

    public static Vector3d getTransformedPosition(Vector3d originalPos, Mirror mirror, Rotation rotation) {
        double x = originalPos.x;
        double y = originalPos.y;
        double z = originalPos.z;
        boolean transformed = true;

        switch (mirror) {
            case LEFT_RIGHT:
                z = 1.0D - z;
                break;
            case FRONT_BACK:
                x = 1.0D - x;
                break;
            default:
                transformed = false;
        }

        switch (rotation) {
            case COUNTERCLOCKWISE_90:
                return new Vector3d(z, y, 1.0D - x);
            case CLOCKWISE_90:
                return new Vector3d(1.0D - z, y, x);
            case CLOCKWISE_180:
                return new Vector3d(1.0D - x, y, 1.0D - z);
            default:
                return transformed ? new Vector3d(x, y, z) : originalPos;
        }
    }
}
