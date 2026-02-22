package net.sweenus.simplybows.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.sweenus.simplybows.SimplyBows;

@Environment(EnvType.CLIENT)
public class WaveParticle extends SpriteBillboardParticle {

    private static final float SPIN_SPEED = 0.022F;
    private static final int FADE_OUT_TICKS = 8;

    protected WaveParticle(ClientWorld world, double x, double y, double z,
                            double vx, double vy, double vz, SpriteProvider sprites) {
        super(world, x, y, z);
        this.setSprite(sprites);

        this.maxAge = 10 + world.random.nextInt(8);

        this.scale = 0.14F + world.random.nextFloat() * 0.18F;

        // Slow horizontal spread, slow upward float
        this.velocityX = vx + (world.random.nextDouble() - 0.5) * 0.025;
        this.velocityY = vy + 0.005 + world.random.nextDouble() * 0.005;
        this.velocityZ = vz + (world.random.nextDouble() - 0.5) * 0.025;

        this.angle = world.random.nextFloat() * (float) (Math.PI * 2.0);
        this.prevAngle = this.angle;

        this.alpha = 0.55F; // softer visibility
        this.collidesWithWorld = false;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        this.prevAngle = this.angle;

        if (this.age >= this.maxAge) {
            this.markDead();
            return;
        }
        this.age++;

        // Fade out
        if (this.age >= this.maxAge - FADE_OUT_TICKS) {
            this.alpha = 0.35F * (float) (this.maxAge - this.age) / FADE_OUT_TICKS;
        } else {
            this.alpha = 0.35F;
        }
        this.alpha = Math.max(0.0F, this.alpha);

        this.angle += SPIN_SPEED;

        this.x += this.velocityX;
        this.y += this.velocityY;
        this.z += this.velocityZ;
        this.velocityX *= 0.97;
        this.velocityY *= 0.99;
        this.velocityZ *= 0.97;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private static int createdCount = 0;
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            createdCount++;
            if (createdCount % 200 == 0) {
                SimplyBows.LOGGER.info("WaveParticle factory invoked {} times", createdCount);
            }
            return new WaveParticle(world, x, y, z, vx, vy, vz, sprites);
        }
    }
}
