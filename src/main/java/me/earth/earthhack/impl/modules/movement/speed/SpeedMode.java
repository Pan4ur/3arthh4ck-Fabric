package me.earth.earthhack.impl.modules.movement.speed;

import me.earth.earthhack.api.util.interfaces.Globals;
import me.earth.earthhack.impl.event.events.movement.MoveEvent;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.math.MathUtil;
import me.earth.earthhack.impl.util.math.position.PositionUtil;
import me.earth.earthhack.impl.util.minecraft.MovementUtil;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

import java.util.HashSet;
// constant conditions... needs @SuppressWarnings, but you can't annotate Enums........
public enum SpeedMode implements Globals
{
    Instant()
            {
                @Override
                public void move(MoveEvent event, Speed module)
                {
                    if (mc.player.isFallFlying()) return;
                    if (module.LONG_JUMP.isEnabled()) return;
                    if (!module.noWaterInstant.getValue()
                            || (!mc.player.isTouchingWater() && !mc.player.isInLava()))
                    {
                        MovementUtil.strafe(event, MovementUtil.getSpeed(module.slow.getValue()));
                    }
                }
            },
    OldGround()
            {
                @Override
                public void move(MoveEvent event, Speed module)
                {
                    // unused block
                }
            },
    OnGround()
            {
                @Override
                public void move(MoveEvent event, Speed module)
                {
                    if (mc.player.isOnGround() || module.onGroundStage == 3)
                    {
                        if ((!mc.player.horizontalCollision
                                && mc.player.forwardSpeed != 0.0f)
                                || mc.player.sidewaysSpeed != 0.0f)
                        {
                            if (module.onGroundStage == 2)
                            {
                                module.speed *= 2.149;
                                module.onGroundStage = 3;
                            }
                            else if (module.onGroundStage == 3)
                            {
                                module.onGroundStage = 2;
                                module.speed = module.distance - 0.66
                                        * (module.distance - MovementUtil.getSpeed(module.slow.getValue()));
                            }
                            else if (PositionUtil.isBoxColliding()
                                    || mc.player.verticalCollision)
                            {
                                module.onGroundStage = 1;
                            }
                        }

                        module.speed = Math.min(module.speed, module.getCap());
                        module.speed = Math.max(module.speed, MovementUtil.getSpeed(module.slow.getValue()));
                        MovementUtil.strafe(event, module.speed);
                    }
                }
            },
    Vanilla()
            {
                @Override
                public void move(MoveEvent event, Speed module)
                {
                    MovementUtil.strafe(event, module.speedSet.getValue() / 10.0);
                }
            },
    NCP
            {
                @Override
                public void move(MoveEvent event, Speed module)
                {
                    if (mc.player.isFallFlying()) return;
                    if (module.LONG_JUMP.isEnabled()) return;
                    switch (module.ncpStage) {
                        case 0:
                            ++module.ncpStage;
                            module.lastDist = 0.0D;
                            break;
                        case 2:
                            if ((mc.player.forwardSpeed != 0.0F
                                    || mc.player.sidewaysSpeed != 0.0F)
                                    && mc.player.isOnGround())
                            {
                                event.setY(mc.player.getVelocity().getY() +
                                        (PositionUtil.isBoxColliding()
                                                ? 0.2
                                                : 0.3999)
                                                + MovementUtil.getJumpSpeed());
                                module.speed *= 2.149;
                            }
                            break;
                        case 3:
                            module.speed = module.lastDist -
                                    (0.7095 * (module.lastDist - MovementUtil.getSpeed(module.slow.getValue())));
                            break;
                        default:
                            // TODO: this also gets the collision boxes of entities...?
                            if ((mc.world.getEntityCollisions(null,
                                            mc.player
                                                    .getBoundingBox()
                                                    .offset(0.0D, mc.player.getVelocity().getY(), 0.0D))
                                    .size() > 0
                                    || mc.player.verticalCollision)
                                    && module.ncpStage > 0)
                            {
                                module.ncpStage = mc.player.forwardSpeed == 0.0F
                                        && mc.player.sidewaysSpeed == 0.0F ? 0 : 1;
                            }

                            module.speed = module.lastDist - module.lastDist / 159.0D;
                            break;
                    }

                    module.speed = Math.min(module.speed, module.getCap());
                    module.speed = Math.max(module.speed, MovementUtil.getSpeed(module.slow.getValue()));
                    MovementUtil.strafe(event, module.speed);
                    ++module.ncpStage;
                }
            },
    Strafe()
            {
                @Override
                public void move(MoveEvent event, Speed module) {
                    if (!MovementUtil.isMoving()) {
                        return;
                    }

                    if (mc.player.isFallFlying()) return;
                    if (module.LONG_JUMP.isEnabled()) return;

                    if (!Managers.NCP.passed(module.lagTime.getValue())) {
                        return;
                    }

                    if (module.useTimer.getValue() && Managers.NCP.passed(250)) {
                        Managers.TIMER.setTimer(1.0888f);
                    }

                    if (module.stage == 1 && MovementUtil.isMoving()) {
                        module.speed = 1.35 * MovementUtil.getSpeed(module.slow.getValue(), module.strafeSpeed.getValue()) - 0.01;
                    } else if (module.stage == 2 && MovementUtil.isMoving()) {
                        double yMotion = 0.3999 + MovementUtil.getJumpSpeed();
                        mc.player.setVelocity(mc.player.getVelocity().getX(), yMotion, mc.player.getVelocity().getZ());
                        event.setY(yMotion);
                        module.speed = module.speed * (module.boost ? 1.6835 : 1.395);
                    } else if (module.stage == 3) {
                        module.speed = module.distance - 0.66
                                * (module.distance - MovementUtil.getSpeed(module.slow.getValue(), module.strafeSpeed.getValue()));

                        module.boost = !module.boost;
                    } else {
                        HashSet<VoxelShape> collisions = (HashSet<VoxelShape>) mc.world.getCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().getY(), 0.0));
                        if ((collisions.size() > 0
                                || mc.player.verticalCollision)
                                && module.stage > 0) {
                            module.stage = MovementUtil.isMoving() ? 1 : 0;
                        }

                        module.speed = module.distance - module.distance / 159.0;
                    }

                    module.speed = Math.min(module.speed, module.getCap());
                    module.speed = Math.max(module.speed, MovementUtil.getSpeed(module.slow.getValue(), module.strafeSpeed.getValue()));
                    MovementUtil.strafe(event, module.speed);

                    if (MovementUtil.isMoving())
                    {
                        module.stage++;
                    }
                }
            },
    GayHop
            {
                @Override
                public void move(MoveEvent event, Speed module) {
                    if (!Managers.NCP.passed(100)) {
                        module.gayStage = 1;
                        return;
                    }

                    if (!MovementUtil.isMoving()) {
                        module.speed = MovementUtil.getSpeed(module.slow.getValue());
                    }

                    if (module.gayStage == 1
                            && mc.player.verticalCollision
                            && MovementUtil.isMoving()) {
                        module.speed = 0.25 + MovementUtil.getSpeed(module.slow.getValue()) - 0.01;
                    } else if (module.gayStage == 2
                            && mc.player.verticalCollision
                            && MovementUtil.isMoving()) {
                        double yMotion = (PositionUtil.isBoxColliding() ? 0.2 : 0.4)
                                + MovementUtil.getJumpSpeed();

                        mc.player.setVelocity(mc.player.getVelocity().withAxis(Direction.Axis.Y, yMotion));
                        event.setY(yMotion);
                        module.speed *= 2.149;
                    } else if (module.gayStage == 3) {
                        module.speed = module.distance
                                - (0.66 * (module.distance - MovementUtil.getSpeed(module.slow.getValue())));
                    } else {
                        if (mc.player.isOnGround() && module.gayStage > 0) {
                            if (1.35 * MovementUtil.getSpeed(module.slow.getValue()) - 0.01 > module.speed) {
                                module.gayStage = 0;
                            } else {
                                module.gayStage = MovementUtil.isMoving() ? 1 : 0;
                            }
                        }

                        module.speed = module.distance - module.distance / 159.0;
                    }

                    module.speed = Math.min(module.speed, module.getCap());
                    module.speed = Math.max(module.speed, MovementUtil.getSpeed(module.slow.getValue()));

                    if (module.gayStage > 0) {
                        MovementUtil.strafe(event, module.speed);
                    }

                    if (MovementUtil.isMoving()) {
                        module.gayStage++;
                    }
                }
            },
    Bhop
            {
                @Override
                public void move(MoveEvent event, Speed module) {
                    if (!Managers.NCP.passed(100)) {
                        module.bhopStage = 4;
                        return;
                    }

                    if (MathUtil.round(mc.player.getPos().y - ((int) mc.player.getPos().y), 3)
                            == MathUtil.round(0.138, 3)) {
                        // mc.player.getVelocity().getY() -= 0.08 + MovementUtil.getJumpSpeed();
                        //mc.player.getVelocity().withAxis(Direction.Axis.Y, mc.player.getVelocity().getY() -= 0.08 + MovementUtil.getJumpSpeed());
                        mc.player.setVelocity(mc.player.getVelocity().withAxis(Direction.Axis.Y, mc.player.getVelocity().getY() - 0.08 + MovementUtil.getJumpSpeed()));
                        event.setY(event.getY()
                                - (0.0931 + MovementUtil.getJumpSpeed()));

                        mc.player.setPos(mc.player.getX(),  mc.player.getY() - 0.0931 + MovementUtil.getJumpSpeed(), mc.player.getZ() - 0.0931 + MovementUtil.getJumpSpeed());
                    }

                    if (module.bhopStage != 2.0 || !MovementUtil.isMoving()) {
                        if (module.bhopStage == 3.0) {
                            module.speed = module.distance
                                    - (0.66 * (module.distance - MovementUtil.getSpeed(module.slow.getValue())));
                        } else {
                            if (mc.player.isOnGround()) {
                                module.bhopStage = 1;
                            }

                            module.speed = module.distance - module.distance / 159.0;
                        }
                    } else {
                        double yMotion = (PositionUtil.isBoxColliding() ? 0.2 : 0.4)
                                + MovementUtil.getJumpSpeed();

                        mc.player.setVelocity(mc.player.getVelocity().withAxis(Direction.Axis.Y, yMotion));

                        event.setY(yMotion);
                        module.speed *= 2.149;
                    }

                    module.speed = Math.min(module.speed, module.getCap());
                    module.speed = Math.max(module.speed, MovementUtil.getSpeed(module.slow.getValue()));
                    MovementUtil.strafe(event, module.speed);
                    module.bhopStage++;
                }
            },
    VHop
            {
                @Override
                public void move(MoveEvent event, Speed module) {
                    if (!Managers.NCP.passed(100)) {
                        module.vStage = 1;
                        return;
                    }

                    if (!MovementUtil.isMoving()) {
                        module.speed = MovementUtil.getSpeed(module.slow.getValue());
                    }

                    if (MathUtil.round(mc.player.getY() - (int) mc.player.getY(), 3)
                            == MathUtil.round(0.4, 3)) {
                        event.setY(mc.player.getVelocity().getY() + 0.31
                                + MovementUtil.getJumpSpeed());
                    } else if (MathUtil.round(mc.player.getY() - (int) mc.player.getY(), 3)
                            == MathUtil.round(0.71, 3)) {
                        event.setY(mc.player.getVelocity().getY() + 0.04
                                + MovementUtil.getJumpSpeed());
                    } else if (MathUtil.round(mc.player.getY() - (int) mc.player.getY(), 3)
                            == MathUtil.round(0.75, 3)) {
                        event.setY(mc.player.getVelocity().getY() - 0.2
                                + MovementUtil.getJumpSpeed());
                    }
                    HashSet<VoxelShape> collisions = (HashSet<VoxelShape>) mc.world.getCollisions(null, mc.player.getBoundingBox().offset(0.0, 0.56, 0.0));
                    if (collisions
                            .size() > 0
                            && MathUtil.round(mc.player.getY() - (int) mc.player.getY(), 3)
                            == MathUtil.round(0.55, 3)) {
                        event.setY(mc.player.getVelocity().getY() - 0.14
                                + MovementUtil.getJumpSpeed());
                    }

                    if (module.vStage != 1
                            || !mc.player.verticalCollision
                            || !MovementUtil.isMoving()) {
                        if (module.vStage != 2
                                || !mc.player.verticalCollision
                                || !MovementUtil.isMoving()) {
                            if (module.vStage == 3) {
                                module.speed = module.distance - 0.66
                                        * (module.distance - MovementUtil.getSpeed(module.slow.getValue()));
                            } else {
                                if (mc.player.isOnGround() && module.vStage > 0) {
                                    if (1.35 * MovementUtil.getSpeed(module.slow.getValue()) - 0.01
                                            > module.speed) {
                                        module.vStage = 0;
                                    } else {
                                        module.vStage = MovementUtil.isMoving() ? 1 : 0;
                                    }
                                }

                                module.speed = module.distance
                                        - module.distance / 159.0;
                            }
                        } else {
                            event.setY(mc.player.getVelocity().getY() +
                                    (PositionUtil.isBoxColliding()
                                            ? 0.2
                                            : 0.4)
                                            + MovementUtil.getJumpSpeed());

                            module.speed *= 2.149;
                        }
                    } else {
                        module.speed = 2.0 * MovementUtil.getSpeed(module.slow.getValue()) - 0.01;
                    }

                    if (module.vStage > 8) {
                        module.speed = MovementUtil.getSpeed(module.slow.getValue());
                    }

                    module.speed = Math.min(module.speed, module.getCap());
                    module.speed = Math.max(module.speed, MovementUtil.getSpeed(module.slow.getValue()));

                    if (module.vStage > 0) {
                        MovementUtil.strafe(event, module.speed);
                    }

                    if (MovementUtil.isMoving()) {
                        module.vStage++;
                    }
                }
            },
    LowHop
            {
                @Override
                public void move(MoveEvent event, Speed module) {
                    if (!Managers.NCP.passed(100)) {
                        return;
                    }

                    if (module.useTimer.getValue() && Managers.NCP.passed(250)) {
                        Managers.TIMER.setTimer(1.0888f);
                    }

                    if (!mc.player.verticalCollision) {
                        if (MathUtil.round(mc.player.getY() - (int) mc.player.getY(), 3)
                                == MathUtil.round(0.4, 3)) {
                            event.setY(mc.player.getVelocity().getY() + 0.31
                                    + MovementUtil.getJumpSpeed());
                        } else if (MathUtil.round(mc.player.getY() - (int) mc.player.getY(), 3)
                                == MathUtil.round(0.71, 3)) {
                            event.setY(mc.player.getVelocity().getY() + 0.04
                                    + MovementUtil.getJumpSpeed());
                        } else if (MathUtil.round(mc.player.getY() - (int) mc.player.getY(), 3)
                                == MathUtil.round(0.75, 3)) {
                            event.setY(mc.player.getVelocity().getY() - 0.2
                                    - MovementUtil.getJumpSpeed());
                        } else if (MathUtil.round(mc.player.getY() - (int) mc.player.getY(), 3)
                                == MathUtil.round(0.55, 3)) {
                            event.setY(mc.player.getVelocity().getY() - 0.14
                                    + MovementUtil.getJumpSpeed());
                        } else if (MathUtil.round(mc.player.getY()
                                - (int) mc.player.getY(), 3)
                                == MathUtil.round(0.41, 3)) {
                            event.setY(mc.player.getVelocity().getY() - 0.2
                                    + MovementUtil.getJumpSpeed());
                        }
                    }

                    if (module.lowStage == 1 && MovementUtil.isMoving()) {
                        module.speed = 1.35 * MovementUtil.getSpeed(module.slow.getValue()) - 0.01;
                    } else if (module.lowStage == 2 && MovementUtil.isMoving()) {
                        event.setY(mc.player.getVelocity().getY() +
                                (PositionUtil.isBoxColliding()
                                            ? 0.2
                                            : 0.3999)
                                        + MovementUtil.getJumpSpeed());

                        module.speed *= module.boost ? 1.5685 : 1.3445;
                    } else if (module.lowStage == 3) {
                        module.speed = module.distance - 0.66
                                * (module.distance - MovementUtil.getSpeed(module.slow.getValue()));

                        module.boost = !module.boost;
                    } else {
                        if (mc.player.isOnGround() && module.lowStage > 0) {
                            module.lowStage = MovementUtil.isMoving() ? 1 : 0;
                        }

                        module.speed = module.distance - module.distance / 159.0;
                    }

                    module.speed = Math.min(module.speed, module.getCap());
                    module.speed = Math.max(module.speed, MovementUtil.getSpeed(module.slow.getValue()));
                    MovementUtil.strafe(event, module.speed);

                    if (MovementUtil.isMoving()) {
                        module.lowStage++;
                    }
                }
            },
    Constantiam
            {
                @Override
                public void move(MoveEvent event, Speed module) {
                    if (!Managers.NCP.passed(100)) {
                        module.constStage = 0;
                        return;
                    }

                    if (!MovementUtil.isMoving()) {
                        module.speed = MovementUtil.getSpeed(module.slow.getValue());
                    }

                    if (module.constStage == 0
                            && MovementUtil.isMoving()
                            && mc.player.isOnGround()) {
                        module.speed = 0.08;
                    } else if (module.constStage == 1
                            && mc.player.verticalCollision
                            && MovementUtil.isMoving()) {
                        module.speed = 0.25 + MovementUtil.getSpeed(module.slow.getValue()) - 0.01;
                    } else if (module.constStage == 2
                            && mc.player.verticalCollision
                            && MovementUtil.isMoving()) {
                        double yMotion = (PositionUtil.isBoxColliding() ? 0.2 : 0.4)
                                + MovementUtil.getJumpSpeed();

                        mc.player.setVelocity(mc.player.getVelocity().withAxis(Direction.Axis.Y, yMotion));
                        event.setY(yMotion);
                        module.speed *= module.constFactor.getValue();
                    } else if (module.constStage == 3) {
                        module.speed = module.distance
                                - (0.66 * (module.distance - MovementUtil.getSpeed(module.slow.getValue())));
                    } else {
                        if (mc.player.isOnGround() && module.constStage > 0) {
                            module.constStage = 0;
                        }
                        if (!mc.player.isOnGround()
                                && module.constStage > module.constOff.getValue()
                                && module.constStage < module.constTicks.getValue()) {
                            if (mc.player.age % 2 == 0) {
                                event.setY(0.00118212);
                            } else {
                                event.setY(-0.00118212);
                            }
                        }
                        module.speed = module.distance - module.distance / 159.0;
                    }

                    module.speed = Math.min(module.speed, module.getCap());
                    if (module.constStage != 0)
                    {
                        module.speed = Math.max(module.speed, MovementUtil.getSpeed(module.slow.getValue()));
                    }

                    MovementUtil.strafe(event, module.speed);

                    if (MovementUtil.isMoving()) {
                        module.constStage++;
                    }
                }
            },
    None {
        @Override
        public void move(MoveEvent event, Speed module) {
            // NOP
        }
    };

    public abstract void move(MoveEvent event, Speed module);
}
