package net.sweenus.simplybows.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

@Environment(EnvType.CLIENT)
public class LongEndRodParticle extends SpriteBillboardParticle {

    private static final int FADE_OUT_TICKS = 18;

    protected LongEndRodParticle(ClientWorld world, double x, double y, double z,
                                 double vx, double vy, double vz, SpriteProvider sprites) {
        super(world, x, y, z, vx, vy, vz);
        this.setSpriteForAge(sprites);
        this.maxAge = 92 + world.random.nextInt(28);
        this.scale = 0.10F + world.random.nextFloat() * 0.06F;
        this.velocityX = vx + (world.random.nextDouble() - 0.5) * 0.012;
        this.velocityY = vy;
        this.velocityZ = vz + (world.random.nextDouble() - 0.5) * 0.012;
        this.alpha = 0.95F;
        this.collidesWithWorld = false;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getBrightness(float tint) {
        return LightmapTextureManager.MAX_LIGHT_COORDINATE;
    }

    @Override
    public void tick() {
        super.tick();
        this.velocityX *= 0.985;
        this.velocityY *= 0.995;
        this.velocityZ *= 0.985;
        if (this.age >= this.maxAge - FADE_OUT_TICKS) {
            this.alpha = Math.max(0.0F, (this.maxAge - this.age) / (float) FADE_OUT_TICKS);
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new LongEndRodParticle(world, x, y, z, vx, vy, vz, sprites);
        }
    }
}
