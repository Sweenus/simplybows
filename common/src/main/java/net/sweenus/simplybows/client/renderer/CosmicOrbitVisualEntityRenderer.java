package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.entity.CosmicOrbitVisualEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class CosmicOrbitVisualEntityRenderer extends EntityRenderer<CosmicOrbitVisualEntity> {

    private static final Identifier NODE_FILL_SPRITE = Identifier.ofVanilla("block/white_concrete");
    private static final float NODE_R = 0.48F;
    private static final float NODE_G = 0.88F;
    private static final float NODE_B = 1.00F;
    private static final float LINE_R = 0.20F;
    private static final float LINE_G = 0.68F;
    private static final float LINE_B = 1.00F;
    private static final int NODE_SEGMENTS = 12;
    private static final float ORBIT_RADIUS = 1.15F;
    private static final float FIELD_ORBIT_RADIUS = 4.25F;
    private static final float ORBIT_SPEED = 0.22F;
    private static final float FIELD_ORBIT_SPEED = 0.105F;
    private static final float NODE_RADIUS = 0.038F;
    private static final int TRAIL_DURATION_TICKS = 9;
    private static final int TRAIL_LINE_DURATION_TICKS = 5;
    private static final int FIELD_TRAIL_DURATION_TICKS = 34;
    private static final int FIELD_TRAIL_LINE_DURATION_TICKS = 18;
    private static final int FIELD_SCATTER_INTERVAL_TICKS = 8;
    private static final int FIELD_SCATTER_NODE_DURATION_TICKS = 34;
    private static final int FIELD_SCATTER_LINE_DURATION_TICKS = 18;
    private static final float FIELD_SCATTER_SPAWN_CHANCE = 0.42F;
    private static final float FIELD_SCATTER_CONNECTION_CHANCE = 0.60F;
    private static final double FIELD_SCATTER_RADIUS = 2.35;
    private static final double FIELD_SCATTER_HEIGHT = 1.45;
    private static final double FIELD_SCATTER_CONNECTION_DISTANCE_SQ = 2.15 * 2.15;
    private static final int FIELD_CONSTELLATION_INTERVAL_MIN_TICKS = 18;
    private static final int FIELD_CONSTELLATION_INTERVAL_RANDOM_TICKS = 22;
    private static final int FIELD_CONSTELLATION_NODE_DURATION_TICKS = 48;
    private static final int FIELD_CONSTELLATION_LINE_DURATION_TICKS = 28;
    private static final int FIELD_CONSTELLATION_MAX_NODES = 10;
    private static final float FIELD_CONSTELLATION_CONNECTION_CHANCE = 0.60F;
    private static final double FIELD_CONSTELLATION_RADIUS = 4.35;
    private static final double FIELD_CONSTELLATION_HEIGHT = 1.65;
    private static final double FIELD_CONSTELLATION_CONNECTION_DISTANCE_SQ = 2.55 * 2.55;
    private static final float TRAIL_MAX_CONNECTION_DIST = 2.3F;
    private static final float TRAIL_CONNECTION_PROBABILITY = 0.35F;
    private static final int TRAIL_SAMPLE_INTERVAL = 1;
    private static final double FIELD_TETHER_NODE_SNAP_DISTANCE_SQ = 1.65 * 1.65;

    private final Map<CosmicOrbitVisualEntity, ConstellationTrail> trails = new WeakHashMap<>();
    private final Map<CosmicOrbitVisualEntity, FieldScatterState> fieldScatter = new WeakHashMap<>();
    private final Map<CosmicOrbitVisualEntity, FieldConstellationState> fieldConstellations = new WeakHashMap<>();
    private static final Map<UUID, FieldOrbitNodeAnchor> LATEST_FIELD_ORBIT_NODES = new HashMap<>();

    public CosmicOrbitVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(CosmicOrbitVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(CosmicOrbitVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float alpha = Math.max(0.0F, Math.min(1.0F, entity.getVisualScale()));
        if (alpha <= 0.02F) {
            return;
        }

        float age = entity.age + tickDelta;
        long worldTick = entity.getWorld().getTime();
        ConstellationTrail trail = updateTrail(entity, age, worldTick);
        rememberLatestFieldOrbitNode(entity, trail, worldTick);

        VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        VertexConsumer nodeConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        Sprite nodeSprite = MinecraftClient.getInstance()
                .getBakedModelManager()
                .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .getSprite(NODE_FILL_SPRITE);

        matrices.push();
        Vec3d renderPos = new Vec3d(
                lerp(tickDelta, entity.prevX, entity.getX()),
                lerp(tickDelta, entity.prevY, entity.getY()),
                lerp(tickDelta, entity.prevZ, entity.getZ())
        );
        renderTrail(trail, worldTick, renderPos, lineConsumer, nodeConsumer, nodeSprite, matrices, alpha);
        if (entity.isFieldMode()) {
            renderFieldScatter(entity, trail, worldTick, renderPos, lineConsumer, nodeConsumer, nodeSprite, matrices, alpha);
            renderFieldConstellations(entity, worldTick, renderPos, lineConsumer, nodeConsumer, nodeSprite, matrices, alpha);
        }
        matrices.pop();
    }

    private ConstellationTrail updateTrail(CosmicOrbitVisualEntity entity, float age, long worldTick) {
        ConstellationTrail trail = trails.computeIfAbsent(entity, ignored -> new ConstellationTrail(
                entity.isFieldMode() ? FIELD_TRAIL_DURATION_TICKS : TRAIL_DURATION_TICKS,
                entity.isFieldMode() ? FIELD_TRAIL_LINE_DURATION_TICKS : TRAIL_LINE_DURATION_TICKS,
                TRAIL_MAX_CONNECTION_DIST,
                TRAIL_CONNECTION_PROBABILITY,
                TRAIL_SAMPLE_INTERVAL
        ));
        if (entity.age % trail.getSampleInterval() == 0) {
            trail.recordPoint(entity.getPos().add(orbitPoint(entity, age)), worldTick);
        }
        trail.prune(worldTick);
        return trail;
    }

    public static Vec3d snapToLatestFieldOrbitNode(World world, Vec3d desiredWorldPos, long worldTick) {
        Vec3d best = null;
        double bestDistanceSq = FIELD_TETHER_NODE_SNAP_DISTANCE_SQ;
        Iterator<Map.Entry<UUID, FieldOrbitNodeAnchor>> iterator = LATEST_FIELD_ORBIT_NODES.entrySet().iterator();
        while (iterator.hasNext()) {
            FieldOrbitNodeAnchor anchor = iterator.next().getValue();
            if (anchor.world != world || anchor.lastSeenTick + FIELD_TRAIL_DURATION_TICKS < worldTick) {
                iterator.remove();
                continue;
            }
            double distanceSq = anchor.pos.squaredDistanceTo(desiredWorldPos);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = anchor.pos;
            }
        }
        return best == null ? desiredWorldPos : best;
    }

    private static void rememberLatestFieldOrbitNode(CosmicOrbitVisualEntity entity, ConstellationTrail trail, long worldTick) {
        if (!entity.isFieldMode() || trail.getPoints().isEmpty()) {
            return;
        }
        ConstellationTrail.TrailPoint point = trail.getPoints().get(trail.getPoints().size() - 1);
        LATEST_FIELD_ORBIT_NODES.put(entity.getUuid(), new FieldOrbitNodeAnchor(entity.getWorld(), point.pos, worldTick));
    }

    private void renderFieldScatter(CosmicOrbitVisualEntity entity, ConstellationTrail trail, long worldTick, Vec3d renderPos,
                                    VertexConsumer lineConsumer, VertexConsumer nodeConsumer, Sprite nodeSprite,
                                    MatrixStack matrices, float visualAlpha) {
        FieldScatterState state = fieldScatter.computeIfAbsent(entity, ignored -> new FieldScatterState());
        updateFieldScatter(entity, trail, state, worldTick);

        Iterator<FieldScatterNode> iterator = state.nodes.iterator();
        while (iterator.hasNext()) {
            FieldScatterNode node = iterator.next();
            if (node.birthTick + FIELD_SCATTER_NODE_DURATION_TICKS <= worldTick) {
                iterator.remove();
            }
        }

        for (FieldScatterNode node : state.nodes) {
            float lineAlpha = scatterAlpha(worldTick, node.birthTick, FIELD_SCATTER_LINE_DURATION_TICKS) * visualAlpha;
            if (lineAlpha >= 0.02F && node.connectionPos != null) {
                renderLine(
                        matrices,
                        lineConsumer,
                        (float) (node.connectionPos.x - renderPos.x), (float) (node.connectionPos.y - renderPos.y), (float) (node.connectionPos.z - renderPos.z),
                        (float) (node.pos.x - renderPos.x), (float) (node.pos.y - renderPos.y), (float) (node.pos.z - renderPos.z),
                        Math.min(1.0F, lineAlpha * 1.1F)
                );
            }
        }

        for (FieldScatterNode node : state.nodes) {
            float nodeAlpha = scatterAlpha(worldTick, node.birthTick, FIELD_SCATTER_NODE_DURATION_TICKS) * visualAlpha;
            if (nodeAlpha < 0.02F) {
                continue;
            }
            float visibleAlpha = Math.min(1.0F, nodeAlpha * 1.25F);
            renderNodeDisc(
                    matrices,
                    nodeConsumer,
                    nodeSprite,
                    (float) (node.pos.x - renderPos.x),
                    (float) (node.pos.y - renderPos.y),
                    (float) (node.pos.z - renderPos.z),
                    NODE_RADIUS * (0.25F + visibleAlpha * 0.75F),
                    visibleAlpha
            );
        }
    }

    private void updateFieldScatter(CosmicOrbitVisualEntity entity, ConstellationTrail trail, FieldScatterState state, long worldTick) {
        if (state.lastSpawnTick == worldTick || entity.age % FIELD_SCATTER_INTERVAL_TICKS != 0) {
            return;
        }
        state.lastSpawnTick = worldTick;
        if (entity.getWorld().random.nextFloat() > FIELD_SCATTER_SPAWN_CHANCE) {
            return;
        }

        int count = 1 + entity.getWorld().random.nextInt(3);
        for (int i = 0; i < count; i++) {
            Vec3d anchor = randomOrbitTrailAnchor(entity, trail);
            Vec3d pos = randomFieldScatterPos(entity, anchor);
            Vec3d connectionPos = null;
            if (entity.getWorld().random.nextFloat() < FIELD_SCATTER_CONNECTION_CHANCE) {
                connectionPos = findNearbyScatterConnection(state.nodes, trail, pos);
            }
            state.nodes.add(new FieldScatterNode(pos, connectionPos, worldTick));
        }
    }

    private static Vec3d randomOrbitTrailAnchor(CosmicOrbitVisualEntity entity, ConstellationTrail trail) {
        List<ConstellationTrail.TrailPoint> points = trail.getPoints();
        if (points.isEmpty()) {
            return entity.getPos();
        }
        int start = Math.max(0, points.size() - 12);
        return points.get(start + entity.getWorld().random.nextInt(points.size() - start)).pos;
    }

    private static Vec3d randomFieldScatterPos(CosmicOrbitVisualEntity entity, Vec3d anchor) {
        double angle = entity.getWorld().random.nextDouble() * Math.PI * 2.0;
        double radius = 0.35 + Math.sqrt(entity.getWorld().random.nextDouble()) * FIELD_SCATTER_RADIUS;
        double y = (entity.getWorld().random.nextDouble() - 0.5) * FIELD_SCATTER_HEIGHT;
        return anchor.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }

    private static Vec3d findNearbyScatterConnection(List<FieldScatterNode> nodes, ConstellationTrail trail, Vec3d pos) {
        Vec3d best = null;
        double bestDistanceSq = FIELD_SCATTER_CONNECTION_DISTANCE_SQ;
        for (ConstellationTrail.TrailPoint point : trail.getPoints()) {
            double distanceSq = point.pos.squaredDistanceTo(pos);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = point.pos;
            }
        }
        for (FieldScatterNode node : nodes) {
            double distanceSq = node.pos.squaredDistanceTo(pos);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = node.pos;
            }
        }
        return best;
    }

    private static float scatterAlpha(long worldTick, long birthTick, int durationTicks) {
        float age = worldTick - birthTick;
        float fadeIn = Math.min(1.0F, age / 6.0F);
        float fadeOut = Math.min(1.0F, Math.max(0.0F, (durationTicks - age) / 10.0F));
        return Math.max(0.0F, Math.min(fadeIn, fadeOut));
    }

    private void renderFieldConstellations(CosmicOrbitVisualEntity entity, long worldTick, Vec3d renderPos,
                                           VertexConsumer lineConsumer, VertexConsumer nodeConsumer, Sprite nodeSprite,
                                           MatrixStack matrices, float visualAlpha) {
        FieldConstellationState state = fieldConstellations.computeIfAbsent(entity, ignored -> new FieldConstellationState());
        updateFieldConstellations(entity, state, worldTick);

        Iterator<FieldConstellation> iterator = state.constellations.iterator();
        while (iterator.hasNext()) {
            FieldConstellation constellation = iterator.next();
            if (constellation.birthTick + FIELD_CONSTELLATION_NODE_DURATION_TICKS <= worldTick) {
                iterator.remove();
                continue;
            }
            renderFieldConstellation(constellation, worldTick, renderPos, lineConsumer, nodeConsumer, nodeSprite, matrices, visualAlpha);
        }
    }

    private void updateFieldConstellations(CosmicOrbitVisualEntity entity, FieldConstellationState state, long worldTick) {
        if (state.nextSpawnTick == 0) {
            state.nextSpawnTick = worldTick + randomConstellationInterval(entity);
            return;
        }
        if (worldTick < state.nextSpawnTick) {
            return;
        }
        state.nextSpawnTick = worldTick + randomConstellationInterval(entity);

        int nodeCount = 1 + entity.getWorld().random.nextInt(FIELD_CONSTELLATION_MAX_NODES);
        List<FieldConstellationNode> nodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            Vec3d pos = randomFieldConstellationPos(entity);
            int connectionIndex = -1;
            if (i > 0 && entity.getWorld().random.nextFloat() < FIELD_CONSTELLATION_CONNECTION_CHANCE) {
                connectionIndex = findNearbyConstellationConnection(nodes, pos);
            }
            nodes.add(new FieldConstellationNode(pos, connectionIndex));
        }
        state.constellations.add(new FieldConstellation(nodes, worldTick));
    }

    private static int randomConstellationInterval(CosmicOrbitVisualEntity entity) {
        return FIELD_CONSTELLATION_INTERVAL_MIN_TICKS + entity.getWorld().random.nextInt(FIELD_CONSTELLATION_INTERVAL_RANDOM_TICKS + 1);
    }

    private static Vec3d randomFieldConstellationPos(CosmicOrbitVisualEntity entity) {
        double angle = entity.getWorld().random.nextDouble() * Math.PI * 2.0;
        double radius = Math.sqrt(entity.getWorld().random.nextDouble()) * Math.max(FIELD_CONSTELLATION_RADIUS, entity.getFieldRadius());
        double y = 0.45 + (entity.getWorld().random.nextDouble() - 0.5) * FIELD_CONSTELLATION_HEIGHT;
        return entity.getPos().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }

    private static int findNearbyConstellationConnection(List<FieldConstellationNode> nodes, Vec3d pos) {
        int bestIndex = -1;
        double bestDistanceSq = FIELD_CONSTELLATION_CONNECTION_DISTANCE_SQ;
        for (int i = 0; i < nodes.size(); i++) {
            double distanceSq = nodes.get(i).pos.squaredDistanceTo(pos);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static void renderFieldConstellation(FieldConstellation constellation, long worldTick, Vec3d renderPos,
                                                 VertexConsumer lineConsumer, VertexConsumer nodeConsumer, Sprite nodeSprite,
                                                 MatrixStack matrices, float visualAlpha) {
        float lineAlpha = scatterAlpha(worldTick, constellation.birthTick, FIELD_CONSTELLATION_LINE_DURATION_TICKS) * visualAlpha;
        if (lineAlpha >= 0.02F) {
            for (FieldConstellationNode node : constellation.nodes) {
                if (node.connectionIndex < 0 || node.connectionIndex >= constellation.nodes.size()) {
                    continue;
                }
                Vec3d connection = constellation.nodes.get(node.connectionIndex).pos;
                renderLine(
                        matrices,
                        lineConsumer,
                        (float) (connection.x - renderPos.x), (float) (connection.y - renderPos.y), (float) (connection.z - renderPos.z),
                        (float) (node.pos.x - renderPos.x), (float) (node.pos.y - renderPos.y), (float) (node.pos.z - renderPos.z),
                        Math.min(1.0F, lineAlpha * 1.1F)
                );
            }
        }

        float nodeAlpha = scatterAlpha(worldTick, constellation.birthTick, FIELD_CONSTELLATION_NODE_DURATION_TICKS) * visualAlpha;
        if (nodeAlpha < 0.02F) {
            return;
        }
        float visibleAlpha = Math.min(1.0F, nodeAlpha * 1.2F);
        for (FieldConstellationNode node : constellation.nodes) {
            renderNodeDisc(
                    matrices,
                    nodeConsumer,
                    nodeSprite,
                    (float) (node.pos.x - renderPos.x),
                    (float) (node.pos.y - renderPos.y),
                    (float) (node.pos.z - renderPos.z),
                    NODE_RADIUS * (0.25F + visibleAlpha * 0.75F),
                    visibleAlpha
            );
        }
    }

    private static Vec3d orbitPoint(CosmicOrbitVisualEntity entity, float age) {
        boolean fieldMode = entity.isFieldMode();
        float radius = fieldMode ? Math.max(FIELD_ORBIT_RADIUS, entity.getFieldRadius()) : ORBIT_RADIUS;
        float speed = fieldMode ? FIELD_ORBIT_SPEED : ORBIT_SPEED;
        float angle = age * speed;
        double height = fieldMode
                ? 0.45 + Math.sin(angle * 2.1F) * 0.34
                : 0.75 + Math.sin(angle * 1.7F) * 0.28;
        return new Vec3d(
                Math.cos(angle) * radius,
                height,
                Math.sin(angle) * radius
        );
    }

    private static void renderTrail(ConstellationTrail trail, long worldTick, Vec3d renderPos,
                                    VertexConsumer lineConsumer, VertexConsumer nodeConsumer, Sprite nodeSprite,
                                    MatrixStack matrices, float visualAlpha) {
        var points = trail.getPoints();
        for (int i = 0; i < points.size() - 1; i++) {
            float lineAlpha = Math.min(trail.getLineAlpha(i, worldTick), trail.getLineAlpha(i + 1, worldTick)) * visualAlpha;
            if (lineAlpha < 0.02F) {
                continue;
            }
            ConstellationTrail.TrailPoint a = points.get(i);
            ConstellationTrail.TrailPoint b = points.get(i + 1);
            renderLine(
                    matrices,
                    lineConsumer,
                    (float) (a.pos.x - renderPos.x), (float) (a.pos.y - renderPos.y), (float) (a.pos.z - renderPos.z),
                    (float) (b.pos.x - renderPos.x), (float) (b.pos.y - renderPos.y), (float) (b.pos.z - renderPos.z),
                    Math.min(1.0F, lineAlpha * 1.15F)
            );
        }

        for (int i = 0; i < points.size(); i++) {
            float nodeAlpha = trail.getAlpha(i, worldTick) * visualAlpha;
            if (nodeAlpha < 0.02F) {
                continue;
            }
            ConstellationTrail.TrailPoint point = points.get(i);
            float visibleAlpha = Math.min(1.0F, nodeAlpha * 1.3F);
            float radius = NODE_RADIUS * (0.25F + visibleAlpha * 0.75F);
            renderNodeDisc(
                    matrices,
                    nodeConsumer,
                    nodeSprite,
                    (float) (point.pos.x - renderPos.x),
                    (float) (point.pos.y - renderPos.y),
                    (float) (point.pos.z - renderPos.z),
                    radius,
                    visibleAlpha
            );
        }
    }

    private static double lerp(float delta, double start, double end) {
        return start + (end - start) * delta;
    }

    static void renderNodeDisc(MatrixStack matrices, VertexConsumer consumer, Sprite sprite,
                               float x, float y, float z, float radius, float alpha) {
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                -MinecraftClient.getInstance().gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                MinecraftClient.getInstance().gameRenderer.getCamera().getPitch()));

        MatrixStack.Entry entry = matrices.peek();
        float u = (sprite.getMinU() + sprite.getMaxU()) * 0.5F;
        float v = (sprite.getMinV() + sprite.getMaxV()) * 0.5F;
        for (int i = 0; i < NODE_SEGMENTS; i++) {
            double angleA = Math.PI * 2.0 * i / NODE_SEGMENTS;
            double angleB = Math.PI * 2.0 * (i + 1) / NODE_SEGMENTS;
            double angleMid = (angleA + angleB) * 0.5;
            emitNodeWedge(consumer, entry,
                    (float) Math.cos(angleA) * radius, (float) Math.sin(angleA) * radius,
                    (float) Math.cos(angleMid) * radius, (float) Math.sin(angleMid) * radius,
                    (float) Math.cos(angleB) * radius, (float) Math.sin(angleB) * radius,
                    u, v, alpha);
        }
        matrices.pop();
    }

    private static void emitNodeWedge(VertexConsumer consumer, MatrixStack.Entry entry,
                                      float x1, float y1, float xMid, float yMid, float x2, float y2,
                                      float u, float v, float alpha) {
        nodeVertex(consumer, entry, 0.0F, 0.0F, u, v, alpha);
        nodeVertex(consumer, entry, x1, y1, u, v, alpha);
        nodeVertex(consumer, entry, xMid, yMid, u, v, alpha);
        nodeVertex(consumer, entry, x2, y2, u, v, alpha);
        nodeVertex(consumer, entry, 0.0F, 0.0F, u, v, alpha);
        nodeVertex(consumer, entry, x2, y2, u, v, alpha);
        nodeVertex(consumer, entry, xMid, yMid, u, v, alpha);
        nodeVertex(consumer, entry, x1, y1, u, v, alpha);
    }

    private static void nodeVertex(VertexConsumer consumer, MatrixStack.Entry entry,
                                   float x, float y, float u, float v, float alpha) {
        consumer.vertex(entry, x, y, 0.0F)
                .color(NODE_R, NODE_G, NODE_B, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(entry, 0.0F, 0.0F, 1.0F);
    }

    static void renderLine(MatrixStack matrices, VertexConsumer consumer,
                           float x1, float y1, float z1, float x2, float y2, float z2, float alpha) {
        MatrixStack.Entry entry = matrices.peek();
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001F) return;
        consumer.vertex(entry, x1, y1, z1).color(LINE_R, LINE_G, LINE_B, alpha)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(entry, dx / len, dy / len, dz / len);
        consumer.vertex(entry, x2, y2, z2).color(LINE_R, LINE_G, LINE_B, alpha)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(entry, dx / len, dy / len, dz / len);
    }

    private static class FieldScatterState {
        private final List<FieldScatterNode> nodes = new ArrayList<>();
        private long lastSpawnTick = -1;
    }

    private record FieldScatterNode(Vec3d pos, Vec3d connectionPos, long birthTick) {
    }

    private static class FieldConstellationState {
        private final List<FieldConstellation> constellations = new ArrayList<>();
        private long nextSpawnTick;
    }

    private record FieldConstellation(List<FieldConstellationNode> nodes, long birthTick) {
    }

    private record FieldConstellationNode(Vec3d pos, int connectionIndex) {
    }

    private record FieldOrbitNodeAnchor(World world, Vec3d pos, long lastSeenTick) {
    }
}
