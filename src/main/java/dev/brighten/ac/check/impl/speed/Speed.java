package dev.brighten.ac.check.impl.speed;

import dev.brighten.ac.check.Action;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.CheckType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.MathHelper;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.potion.PotionEffectType;

@CheckData(name = "Speed", type = CheckType.MOVEMENT)
public class Speed extends Check {
    private boolean lastLastClientGround;
    private float buffer;

    private static boolean[] TRUE_FALSE = new boolean[]{true, false};
    private static float[] VALUES = new float[]{-0.98f, 0f, 0.98f};

    public Speed(APlayer player) {
        super(player);
    }

    @Action
    public void onFlying(WPacketPlayInFlying packet) {

        Block underBlock = BlockUtils.getBlock(getPlayer().getMovement().getTo().getLoc()
                .toLocation(getPlayer().getBukkitPlayer().getWorld())
                .subtract(0, 1, 0)),
                lastUnderBlock = BlockUtils.getBlock(getPlayer().getMovement().getFrom().getLoc()
                        .toLocation(getPlayer().getBukkitPlayer().getWorld())
                        .subtract(0, 1, 0));

        if (underBlock == null || lastUnderBlock == null) {
            getPlayer().getBukkitPlayer().sendMessage("Null");
            return;
        }

        float friction = CraftMagicNumbers.getBlock(underBlock).frictionFactor,
                lfriction = CraftMagicNumbers.getBlock(lastUnderBlock).frictionFactor;

        check:
        {
            if (!packet.isMoved()
                    /*|| getPlayer().playerInfo.generalCancel
                    || getPlayer().playerInfo.onLadder
                    || getPlayer().playerInfo.lastEntityCollision.isNotPassed(2)
                    || getPlayer().playerInfo.lastVelocity.isNotPassed(1)*/
                    || getPlayer().getBlockInformation().inLiquid
                    || getPlayer().getBlockInformation().collidesHorizontally) {
                break check;
            }
            double smallestDelta = Double.MAX_VALUE;

            double pmotionx = 0, pmotionz = 0;
            boolean onGround = getPlayer().getMovement().getFrom().isOnGround();
            loop:
            {
                for (int f = -1; f < 2; f++) {
                    for (int s = -1; s < 2; s++) {
                        for (boolean sprinting : TRUE_FALSE) {
                            for (int fastMath = 0; fastMath <= 2; fastMath++) {
                                for (boolean attack : TRUE_FALSE) {
                                    for (boolean using : TRUE_FALSE) {
                                        for (boolean sneaking : TRUE_FALSE) {
                                            for (boolean jumped : TRUE_FALSE) {

                                                float forward = f, strafe = s;

                                                if (sneaking) {
                                                    forward *= 0.3;
                                                    strafe *= 0.3;
                                                }

                                                if (using) {
                                                    forward *= 0.2;
                                                    strafe *= 0.2;
                                                }

                                                //Multiplying by 0.98 like in client
                                                forward *= 0.9800000190734863F;
                                                strafe *= 0.9800000190734863F;

                                                double aiMoveSpeed = getPlayer().getBukkitPlayer().getWalkSpeed() / 2;

                                                float drag = 0.91f;
                                                double lmotionX = getPlayer().getMovement().getLDeltaX(),
                                                        lmotionZ = getPlayer().getMovement().getLDeltaZ();

                                                //The "1" will effectively remove lastFriction from the equation
                                                lmotionX *= (lastLastClientGround ? lfriction : 1) * 0.9100000262260437D;
                                                lmotionZ *= (lastLastClientGround ? lfriction : 1) * 0.9100000262260437D;

                                                //Running multiplication done after previous prediction
                                                if (getPlayer().getPlayerVersion().isOrAbove(ProtocolVersion.V1_9)) {
                                                    if (Math.abs(lmotionX) < 0.003)
                                                        lmotionX = 0;
                                                    if (Math.abs(lmotionZ) < 0.003)
                                                        lmotionZ = 0;
                                                } else {
                                                    if (Math.abs(lmotionX) < 0.005)
                                                        lmotionX = 0;
                                                    if (Math.abs(lmotionZ) < 0.005)
                                                        lmotionZ = 0;
                                                }

                                                // Attack slowdown
                                                if (attack) {
                                                    lmotionX *= 0.6;
                                                    lmotionZ *= 0.6;
                                                }

                                                if (sprinting) aiMoveSpeed += aiMoveSpeed * 0.30000001192092896D;

                                                if (getPlayer().getPotionHandler().hasPotionEffect(PotionEffectType.SPEED))
                                                    aiMoveSpeed += (getPlayer().getPotionHandler().getEffectByType(PotionEffectType.SPEED)
                                                            .get()
                                                            .getAmplifier() + 1) * (double) 0.20000000298023224D * aiMoveSpeed;
                                                if (getPlayer().getPotionHandler().hasPotionEffect(PotionEffectType.SLOW))
                                                    aiMoveSpeed += (getPlayer().getPotionHandler().getEffectByType(PotionEffectType.SLOW)
                                                            .get()
                                                            .getAmplifier() + 1) * (double) -0.15000000596046448D * aiMoveSpeed;

                                                float f5;
                                                if (onGround) {
                                                    drag *= friction;

                                                    f5 = (float) (aiMoveSpeed * (0.16277136F / (drag * drag * drag)));

                                                    if (sprinting && jumped) {
                                                        float rot = getPlayer().getMovement().getTo().getLoc().yaw * 0.017453292F;
                                                        lmotionX -= sin(fastMath, rot) * 0.2F;
                                                        lmotionZ += cos(fastMath, rot) * 0.2F;
                                                    }

                                                } else f5 = sprinting ? 0.025999999F : 0.02f;

                                                if (getPlayer().getPlayerVersion().isOrAbove(ProtocolVersion.V1_9)) {
                                                    double keyedMotion = forward * forward + strafe * strafe;

                                                    if (keyedMotion >= 1.0E-4F) {
                                                        keyedMotion = f5 / Math.max(1.0, Math.sqrt(keyedMotion));
                                                        forward *= keyedMotion;
                                                        strafe *= keyedMotion;

                                                        final float yawSin = sin(fastMath,
                                                                getPlayer().getMovement().getTo().getLoc().yaw * (float) Math.PI / 180.F),
                                                                yawCos = cos(fastMath,
                                                                        getPlayer().getMovement().getTo().getLoc().yaw * (float) Math.PI / 180.F);

                                                        lmotionX += (strafe * yawCos - forward * yawSin);
                                                        lmotionZ += (forward * yawCos + strafe * yawSin);
                                                    }
                                                } else {
                                                    float keyedMotion = forward * forward + strafe * strafe;

                                                    if (keyedMotion >= 1.0E-4F) {
                                                        keyedMotion = f5 / Math.max(1.0f, MathHelper.sqrt_float(keyedMotion));
                                                        forward *= keyedMotion;
                                                        strafe *= keyedMotion;

                                                        final float yawSin = sin(fastMath,
                                                                getPlayer().getMovement().getTo().getLoc().yaw * (float) Math.PI / 180.F),
                                                                yawCos = cos(fastMath,
                                                                        getPlayer().getMovement().getTo().getLoc().yaw * (float) Math.PI / 180.F);

                                                        lmotionX += (strafe * yawCos - forward * yawSin);
                                                        lmotionZ += (forward * yawCos + strafe * yawSin);
                                                    }
                                                }

                                                double diffX = getPlayer().getMovement().getDeltaX() - lmotionX,
                                                        diffZ = getPlayer().getMovement().getDeltaZ() - lmotionZ;
                                                double delta = (diffX * diffX) + (diffZ * diffZ);

                                                if (delta < smallestDelta) {
                                                    smallestDelta = delta;
                                                    pmotionx = lmotionX;
                                                    pmotionz = lmotionZ;

                                                    if (delta < 1E-15) {
                                                        break loop;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            double pmotion = Math.hypot(pmotionx, pmotionz);

            if (getPlayer().getMovement().getDeltaXZ() > pmotion && smallestDelta > 1E-4 && getPlayer().getMovement().getDeltaXZ() > 0.1) {
                if (++buffer > 3) {
                    buffer = Math.min(3.5f, buffer); //Ensuring we don't have a run-away buffer
                    flag("smallest=%s b=%.1f to=%s dxz=%.2f", smallestDelta, buffer, getPlayer().getMovement().getTo().getLoc(), getPlayer().getMovement().getDeltaXZ());
                }
            } else if (buffer > 0) buffer -= 0.1f;

            //getPlayer().getBukkitPlayer().sendMessage(String.format("smallest=%s b=%.1f to=%s dxz=%.2f", smallestDelta, buffer, getPlayer().getMovement().getTo().getLoc(), getPlayer().getMovement().getDeltaXZ()));

            //debug("smallest=%s b=%.1f", smallestDelta, buffer);
        }
        lastLastClientGround = getPlayer().getMovement().getFrom().isOnGround();
    }

    private static final float[] SIN_TABLE_FAST = new float[4096], SIN_TABLE_FAST_NEW = new float[4096];
    private static final float[] SIN_TABLE = new float[65536];
    private static final float radToIndex = roundToFloat(651.8986469044033D);

    public static float sin(int type, float value) {
        switch(type) {
            case 0:
            default: {
                return SIN_TABLE[(int) (value * 10430.378F) & 65535];
            }
            case 1: {
                return SIN_TABLE_FAST[(int) (value * 651.8986F) & 4095];
            }
            case 2: {
                return SIN_TABLE_FAST_NEW[(int)(value * radToIndex) & 4095];
            }
        }
    }

    public static float cos(int type, float value) {
        switch (type) {
            case 0:
            default:
                return SIN_TABLE[(int) (value * 10430.378F + 16384.0F) & 65535];
            case 1:
                return SIN_TABLE_FAST[(int) ((value + ((float) Math.PI / 2F)) * 651.8986F) & 4095];
            case 2:
                return SIN_TABLE_FAST_NEW[(int)(value * radToIndex + 1024.0F) & 4095];
        }
    }

    static {
        for (int i = 0; i < 65536; ++i)
        {
            SIN_TABLE[i] = (float)Math.sin((double)i * Math.PI * 2.0D / 65536.0D);
        }

        for (int j = 0; j < 4096; ++j)
        {
            SIN_TABLE_FAST[j] = (float)Math.sin((double)(((float)j + 0.5F) / 4096.0F * ((float)Math.PI * 2F)));
        }

        for (int l = 0; l < 360; l += 90)
        {
            SIN_TABLE_FAST[(int)((float)l * 11.377778F) & 4095] = (float)Math.sin((double)((float)l * 0.017453292F));
        }

        for (int j = 0; j < SIN_TABLE_FAST_NEW.length; ++j)
        {
            SIN_TABLE_FAST_NEW[j] = roundToFloat(Math.sin((double)j * Math.PI * 2.0D / 4096.0D));
        }
    }

    private static float roundToFloat(double d)
    {
        return (float)((double)Math.round(d * 1.0E8D) / 1.0E8D);
    }
}