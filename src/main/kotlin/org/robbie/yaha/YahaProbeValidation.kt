package org.robbie.yaha

import com.mojang.authlib.GameProfile
import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv
import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.iota.Vec3Iota
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import at.petrak.hexcasting.api.item.IotaHolderItem
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.common.lib.HexItems
import at.petrak.hexcasting.common.lib.HexDataComponents
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap
import net.minecraft.advancements.AdvancementProgress
import net.minecraft.advancements.critereon.PlayerTrigger
import net.minecraft.core.Registry
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.core.component.DataComponentType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.entity.projectile.ThrownPotion
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.common.util.FakePlayerFactory
import org.robbie.yaha.features.anvil.AnvilEntity
import org.robbie.yaha.features.anvil.OpAnvil
import org.robbie.yaha.features.armor_stand.OpStandPose
import org.robbie.yaha.features.armor_stand.OpStandToggle
import org.robbie.yaha.features.armor_stand.OpStandYaw
import org.robbie.yaha.features.bundles.BundleSelection
import org.robbie.yaha.features.bundles.IotaHolderBundle
import org.robbie.yaha.features.paper_plane.OpPaperPlane
import org.robbie.yaha.features.paper_plane.OpPaperPlaneTarget
import org.robbie.yaha.features.paper_plane.PaperPlaneEntity
import org.robbie.yaha.features.spells.OpPotionToItem
import org.robbie.yaha.features.spells.OpSussifyBlock
import org.robbie.yaha.features.time_bomb.OpTimeBomb
import org.robbie.yaha.features.time_bomb.OpTimeBombPos
import org.robbie.yaha.features.time_bomb.TimeBombCastEnv
import org.robbie.yaha.features.time_bomb.TimeBombEntity
import org.robbie.yaha.features.trident.OpTrident
import org.robbie.yaha.features.trident.TridentEntity
import org.robbie.yaha.network.YahaNetwork
import org.robbie.yaha.registry.YahaCriteria
import org.robbie.yaha.registry.YahaEntities
import org.robbie.yaha.registry.YahaItems
import kotlin.math.PI
import kotlin.math.abs
import java.util.Optional
import java.util.UUID

object YahaProbeValidation {
    private const val PROPERTY = "yaha.probe.validateRegistries"

    private val itemIds = listOf("time_bomb", "spindle", "pouch").map { Yaha.id(it) }
    private val entityIds = listOf("paper_plane", "time_bomb", "anvil", "trident").map { Yaha.id(it) }
    private val soundIds = listOf("plane_shatter", "anvil_hit", "anvil_shatter", "trident_hit", "trident_shatter").map { Yaha.id(it) }
    private val triggerIds = listOf("collide_planes", "susception", "bomb_defusal").map { Yaha.id(it) }
    private val recipeIds = listOf("spindle", "pouch").map { Yaha.id(it) }
    private val advancementIds = listOf("collide_planes", "susception", "bomb_defusal", "eat_bomb").map { Yaha.id(it) }
    private val damageTypeIds = listOf("paper_plane", "anvil", "trident").map { Yaha.id(it) }
    private val actionIds = listOf(
        "paper_plane",
        "paper_plane_target",
        "time_bomb",
        "time_bomb_pos",
        "anvil",
        "trident",
        "sussify_block",
        "potion_to_item",
        "stand_toggle_arms",
        "stand_toggle_base",
        "stand_toggle_tiny",
        "stand_rotate_head",
        "stand_rotate_body",
        "stand_rotate_yaw",
        "stand_rotate_left_arm",
        "stand_rotate_right_arm",
        "stand_rotate_left_leg",
        "stand_rotate_right_leg",
    ).map { Yaha.id(it) }
    private val potionToItemTag: TagKey<EntityType<*>> =
        TagKey.create(Registries.ENTITY_TYPE, Yaha.id("potion_to_item"))

    fun onServerStarted(event: ServerStartedEvent) {
        if (!java.lang.Boolean.getBoolean(PROPERTY)) {
            return
        }

        var failures = 0
        try {
            failures += checkRegistry("items", BuiltInRegistries.ITEM, itemIds)
            failures += checkRegistry("entities", BuiltInRegistries.ENTITY_TYPE, entityIds)
            failures += checkRegistry("sounds", BuiltInRegistries.SOUND_EVENT, soundIds)
            failures += checkRegistry("criteria", BuiltInRegistries.TRIGGER_TYPES, triggerIds)
            failures += checkRegistry("damage_types", event.server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE), damageTypeIds)
            failures += checkRegistry("hex_actions", event.server.registryAccess().registryOrThrow(HexRegistries.ACTION), actionIds)
            failures += checkRecipes(event)
            failures += checkAdvancements(event)
            failures += checkAdvancementCriterionTriggers(event)
            failures += checkAdvancementTriggerExecution(event)
            failures += checkArmorStandActions(event)
            failures += checkProjectileSpellActions(event)
            failures += checkPotionToItemAction(event)
            failures += checkSussifyBlockAction(event)
            failures += checkProjectileCollisionDamage(event)
            failures += checkProjectileTickCollision(event)
            failures += checkTimeBombActions(event)
            failures += checkTimeBombNonEmptyHex(event)
            failures += checkTimeBombStateChangingHex(event)
            failures += checkBundleItems(event)

            if (failures == 0) {
                Yaha.LOGGER.info(
                    "[YAHA-PROBE] registries=PASS items={} entities={} sounds={} criteria={} damage_types={} actions={} recipes={} advancements={} advancement_criteria=PASS advancement_triggers=PASS armor_stand_actions=PASS projectile_spells=PASS potion_to_item=PASS sussify_block=PASS projectile_collision=PASS projectile_tick_collision=PASS time_bomb_actions=PASS time_bomb_non_empty=PASS time_bomb_state_change=PASS bundle_items=PASS bundle_written_iotas=PASS",
                    itemIds.size,
                    entityIds.size,
                    soundIds.size,
                    triggerIds.size,
                    damageTypeIds.size,
                    actionIds.size,
                    recipeIds.size,
                    advancementIds.size,
                )
            } else {
                Yaha.LOGGER.error("[YAHA-PROBE] registries=FAIL failure_count={}", failures)
            }
        } catch (throwable: Throwable) {
            Yaha.LOGGER.error("[YAHA-PROBE] registries=FAIL exception", throwable)
        } finally {
            event.server.halt(false)
        }
    }

    private fun <T> checkRegistry(label: String, registry: Registry<T>, ids: List<ResourceLocation>): Int {
        val missing = ids.filterNot { registry.containsKey(it) }
        if (missing.isEmpty()) {
            Yaha.LOGGER.info("[YAHA-PROBE] {}=PASS count={}", label, ids.size)
        } else {
            Yaha.LOGGER.error("[YAHA-PROBE] {}=FAIL missing={}", label, missing.joinToString(","))
        }
        return missing.size
    }

    private fun checkArmorStandActions(event: ServerStartedEvent): Int {
        val level = event.server.overworld()
        val env = probeEnv(level)
        val origin = env.castingEntity!!.position()
        val stand = ArmorStand(level, origin.x + 1.0, origin.y, origin.z + 1.0)

        return try {
            if (!level.addFreshEntity(stand)) {
                Yaha.LOGGER.error("[YAHA-PROBE] armor_stand_actions=FAIL could_not_add_entity")
                return 1
            }

            OpStandToggle(OpStandToggle.Toggle.SHOW_ARMS).execute(listOf(EntityIota(stand)), env).effect.cast(env)
            OpStandToggle(OpStandToggle.Toggle.HIDE_BASEPLATE).execute(listOf(EntityIota(stand)), env).effect.cast(env)
            OpStandToggle(OpStandToggle.Toggle.MAKE_SMALL).execute(listOf(EntityIota(stand)), env).effect.cast(env)
            OpStandPose(OpStandPose.Part.HEAD).execute(
                listOf(EntityIota(stand), Vec3Iota(net.minecraft.world.phys.Vec3(0.0, PI / 2.0, PI))),
                env,
            ).effect.cast(env)
            OpStandYaw.execute(listOf(EntityIota(stand), DoubleIota(PI / 2.0)), env).effect.cast(env)

            val head = stand.headPose
            val ok = stand.isShowArms &&
                stand.isNoBasePlate &&
                stand.isSmall &&
                close(head.x, 0.0f) &&
                close(head.y, 90.0f) &&
                close(head.z, 180.0f) &&
                close(stand.yRot, 90.0f)

            if (ok) {
                Yaha.LOGGER.info(
                    "[YAHA-PROBE] armor_stand_actions=PASS arms={} baseplate_hidden={} small={} head=({},{},{}) yaw={}",
                    stand.isShowArms,
                    stand.isNoBasePlate,
                    stand.isSmall,
                    head.x,
                    head.y,
                    head.z,
                    stand.yRot,
                )
                0
            } else {
                Yaha.LOGGER.error(
                    "[YAHA-PROBE] armor_stand_actions=FAIL arms={} baseplate_hidden={} small={} head=({},{},{}) yaw={}",
                    stand.isShowArms,
                    stand.isNoBasePlate,
                    stand.isSmall,
                    head.x,
                    head.y,
                    head.z,
                    stand.yRot,
                )
                1
            }
        } catch (throwable: Throwable) {
            Yaha.LOGGER.error("[YAHA-PROBE] armor_stand_actions=FAIL exception", throwable)
            1
        } finally {
            stand.discard()
        }
    }

    private fun checkProjectileSpellActions(event: ServerStartedEvent): Int {
        val level = event.server.overworld()
        val env = probeEnv(level)
        val origin = env.castingEntity!!.position()
        val target = ArmorStand(level, origin.x + 2.0, origin.y, origin.z + 2.0)
        val spawned = mutableListOf<Entity>()

        return try {
            if (!level.addFreshEntity(target)) {
                Yaha.LOGGER.error("[YAHA-PROBE] projectile_spells=FAIL could_not_add_target")
                return 1
            }

            val planeImage = OpPaperPlane.execute(
                listOf(Vec3Iota(origin.add(0.0, 1.0, 0.0)), EntityIota(target)),
                env,
            ).effect.cast(env, CastingImage()) ?: throw IllegalStateException("paper_plane did not return a casting image")
            val plane = entityFromImage(planeImage, level) as? PaperPlaneEntity
            if (plane != null) spawned += plane

            val targetResult = plane?.let { OpPaperPlaneTarget.execute(listOf(EntityIota(it)), env) }
            val resolvedTarget = (targetResult?.firstOrNull() as? EntityIota)?.getEntity(level)

            val anvilImage = OpAnvil.execute(listOf(Vec3Iota(origin.add(1.0, 1.0, 0.0))), env).effect.cast(env, CastingImage())
                ?: throw IllegalStateException("anvil did not return a casting image")
            val anvil = entityFromImage(anvilImage, level) as? AnvilEntity
            if (anvil != null) spawned += anvil

            val tridentImage = OpTrident.execute(listOf(Vec3Iota(origin.add(0.0, 1.0, 1.0))), env).effect.cast(env, CastingImage())
                ?: throw IllegalStateException("trident did not return a casting image")
            val trident = entityFromImage(tridentImage, level) as? TridentEntity
            if (trident != null) spawned += trident

            val ok = plane != null &&
                plane.getTarget() == target &&
                resolvedTarget == target &&
                anvil != null &&
                trident != null &&
                spawned.all { !it.isRemoved }

            if (ok) {
                Yaha.LOGGER.info(
                    "[YAHA-PROBE] projectile_spells=PASS plane={} target_resolved={} anvil={} trident={}",
                    plane?.type?.descriptionId,
                    resolvedTarget?.type?.descriptionId,
                    anvil?.type?.descriptionId,
                    trident?.type?.descriptionId,
                )
                0
            } else {
                Yaha.LOGGER.error(
                    "[YAHA-PROBE] projectile_spells=FAIL plane={} plane_target_ok={} target_resolved={} anvil={} trident={}",
                    plane?.javaClass?.name,
                    plane?.getTarget() == target,
                    resolvedTarget?.javaClass?.name,
                    anvil?.javaClass?.name,
                    trident?.javaClass?.name,
                )
                1
            }
        } catch (throwable: Throwable) {
            Yaha.LOGGER.error("[YAHA-PROBE] projectile_spells=FAIL exception", throwable)
            1
        } finally {
            spawned.forEach(Entity::discard)
            target.discard()
        }
    }

    private fun checkPotionToItemAction(event: ServerStartedEvent): Int {
        val level = event.server.overworld()
        val env = probeEnv(level)
        val owner = env.castingEntity!!
        val origin = owner.position().add(4.0, 3.0, 6.0)
        val velocity = Vec3(0.25, 0.05, -0.125)
        val spawned = mutableListOf<Entity>()

        return try {
            val tagOk = EntityType.POTION.`is`(potionToItemTag) &&
                EntityType.EGG.`is`(potionToItemTag) &&
                EntityType.EXPERIENCE_BOTTLE.`is`(potionToItemTag) &&
                YahaEntities.TIME_BOMB_ENTITY.get().`is`(potionToItemTag)

            val potion = ThrownPotion(level, owner)
            potion.setItem(ItemStack(Items.SPLASH_POTION))
            potion.setPos(origin.x, origin.y, origin.z)
            potion.deltaMovement = velocity
            if (!level.addFreshEntity(potion)) {
                throw IllegalStateException("could not add pluckable potion")
            }
            spawned += potion

            val nearbyItems = AABB.ofSize(origin, 6.0, 6.0, 6.0)
            val existingItems = level.getEntitiesOfClass(ItemEntity::class.java, nearbyItems)
                .map { it.uuid }
                .toSet()

            OpPotionToItem.execute(listOf(EntityIota(potion)), env).effect.cast(env)

            val dropped = level.getEntitiesOfClass(ItemEntity::class.java, nearbyItems)
                .firstOrNull { it.uuid !in existingItems && it.item.item == Items.SPLASH_POTION }
            if (dropped != null) {
                spawned += dropped
            }

            val removed = potion.isRemoved
            val itemOk = dropped?.item?.item == Items.SPLASH_POTION
            val velocityOk = dropped?.deltaMovement?.distanceToSqr(velocity)?.let { it < 0.000001 } ?: false
            val ok = tagOk && removed && itemOk && velocityOk

            if (ok) {
                Yaha.LOGGER.info(
                    "[YAHA-PROBE] potion_to_item=PASS tag=true removed={} item={} velocity_copied={}",
                    removed,
                    BuiltInRegistries.ITEM.getKey(dropped?.item?.item ?: Items.AIR),
                    velocityOk,
                )
                0
            } else {
                Yaha.LOGGER.error(
                    "[YAHA-PROBE] potion_to_item=FAIL tag={} removed={} item_ok={} velocity_ok={} dropped={}",
                    tagOk,
                    removed,
                    itemOk,
                    velocityOk,
                    dropped?.javaClass?.name,
                )
                1
            }
        } catch (throwable: Throwable) {
            Yaha.LOGGER.error("[YAHA-PROBE] potion_to_item=FAIL exception", throwable)
            1
        } finally {
            spawned.forEach(Entity::discard)
        }
    }

    private fun checkSussifyBlockAction(event: ServerStartedEvent): Int {
        val level = event.server.overworld()
        val env = probeEnv(level)
        val origin = env.castingEntity!!.position()
        val sandPos = BlockPos.containing(origin.add(2.0, 0.0, 6.0))
        val gravelPos = sandPos.offset(1, 0, 0)
        val invalidPos = sandPos.offset(2, 0, 0)
        val spawned = mutableListOf<Entity>()

        fun addBrushItem(pos: BlockPos, stack: ItemStack): ItemEntity {
            val entity = ItemEntity(level, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5, stack)
            if (!level.addFreshEntity(entity)) {
                throw IllegalStateException("could not add sussify brush item")
            }
            spawned += entity
            return entity
        }

        return try {
            level.setBlockAndUpdate(sandPos, Blocks.SAND.defaultBlockState())
            level.setBlockAndUpdate(gravelPos, Blocks.GRAVEL.defaultBlockState())
            level.setBlockAndUpdate(invalidPos, Blocks.STONE.defaultBlockState())

            val sandItem = addBrushItem(sandPos, ItemStack(Items.SAND))
            val gravelItem = addBrushItem(gravelPos, ItemStack(Items.GRAVEL))
            val invalidItem = addBrushItem(invalidPos, ItemStack(Items.STONE))

            OpSussifyBlock.execute(
                listOf(Vec3Iota(sandPos.center), EntityIota(sandItem)),
                env,
            ).effect.cast(env)
            OpSussifyBlock.execute(
                listOf(Vec3Iota(gravelPos.center), EntityIota(gravelItem)),
                env,
            ).effect.cast(env)

            val sandOk = level.getBlockState(sandPos).`is`(Blocks.SUSPICIOUS_SAND) && sandItem.isRemoved
            val gravelOk = level.getBlockState(gravelPos).`is`(Blocks.SUSPICIOUS_GRAVEL) && gravelItem.isRemoved
            val invalidRejected = try {
                OpSussifyBlock.execute(
                    listOf(Vec3Iota(invalidPos.center), EntityIota(invalidItem)),
                    env,
                ).effect.cast(env)
                false
            } catch (_: MishapBadBlock) {
                level.getBlockState(invalidPos).`is`(Blocks.STONE) && !invalidItem.isRemoved
            }

            if (sandOk && gravelOk && invalidRejected) {
                Yaha.LOGGER.info(
                    "[YAHA-PROBE] sussify_block=PASS sand={} gravel={} invalid_rejected={}",
                    level.getBlockState(sandPos).block.descriptionId,
                    level.getBlockState(gravelPos).block.descriptionId,
                    invalidRejected,
                )
                0
            } else {
                Yaha.LOGGER.error(
                    "[YAHA-PROBE] sussify_block=FAIL sand_ok={} gravel_ok={} invalid_rejected={} sand_block={} gravel_block={} invalid_block={} sand_item_removed={} gravel_item_removed={} invalid_item_removed={}",
                    sandOk,
                    gravelOk,
                    invalidRejected,
                    level.getBlockState(sandPos).block.descriptionId,
                    level.getBlockState(gravelPos).block.descriptionId,
                    level.getBlockState(invalidPos).block.descriptionId,
                    sandItem.isRemoved,
                    gravelItem.isRemoved,
                    invalidItem.isRemoved,
                )
                1
            }
        } catch (throwable: Throwable) {
            Yaha.LOGGER.error("[YAHA-PROBE] sussify_block=FAIL exception", throwable)
            1
        } finally {
            spawned.forEach(Entity::discard)
            level.setBlockAndUpdate(sandPos, Blocks.AIR.defaultBlockState())
            level.setBlockAndUpdate(gravelPos, Blocks.AIR.defaultBlockState())
            level.setBlockAndUpdate(invalidPos, Blocks.AIR.defaultBlockState())
        }
    }

    private fun checkProjectileCollisionDamage(event: ServerStartedEvent): Int {
        val level = event.server.overworld()
        val env = probeEnv(level)
        val owner = env.castingEntity!!
        val origin = owner.position().add(12.0, 8.0, 0.0)
        val spawned = mutableListOf<Entity>()

        fun add(entity: Entity, label: String) {
            if (!level.addFreshEntity(entity)) {
                throw IllegalStateException("could not add $label")
            }
            spawned += entity
        }

        fun pig(label: String, pos: Vec3): Pig {
            val entity = Pig(EntityType.PIG, level)
            entity.setPos(pos)
            add(entity, label)
            return entity
        }

        return try {
            val planeTarget = pig("paper_plane_target", origin.add(1.0, 0.0, 0.0))
            val plane = PaperPlaneEntity(level, owner, planeTarget, origin)
            add(plane, "paper_plane")
            val planeHealth = planeTarget.health
            invokeEntityHit(plane, planeTarget)
            val planeDamaged = planeTarget.health < planeHealth
            val planeRemoved = plane.isRemoved

            val anvilTarget = pig("anvil_target", origin.add(5.0, 0.0, 0.0))
            val anvil = AnvilEntity(level, owner, origin.add(4.0, 0.0, 0.0))
            add(anvil, "anvil")
            repeat(2) { anvil.tick() }
            anvil.deltaMovement = Vec3(2.0, 0.0, 0.0)
            val anvilPickableBefore = anvil.isPickable
            val anvilHealth = anvilTarget.health
            invokeEntityHit(anvil, anvilTarget)
            val anvilDamaged = anvilTarget.health < anvilHealth
            val anvilVelocityTransferred = anvilTarget.deltaMovement.distanceToSqr(Vec3(2.0, 0.0, 0.0)) < 0.000001
            val anvilCooldownRestored = !anvil.isPickable

            val tridentTargets = listOf(
                pig("trident_target_1", origin.add(9.0, 0.0, 0.0)),
                pig("trident_target_2", origin.add(10.0, 0.0, 0.0)),
                pig("trident_target_3", origin.add(11.0, 0.0, 0.0)),
            )
            val trident = TridentEntity(level, owner, origin.add(8.0, 0.0, 0.0))
            add(trident, "trident")
            trident.deltaMovement = Vec3(2.0, 0.0, 0.0)
            val tridentHealth = tridentTargets.first().health
            invokeEntityHit(trident, tridentTargets[0])
            val tridentDamaged = tridentTargets[0].health < tridentHealth
            val tridentPierced = trident.piercedEntities.contains(tridentTargets[0].id)
            val tridentVelocityReversed = tridentTargets[0].deltaMovement.distanceToSqr(Vec3(-2.0, -0.0, -0.0)) < 0.000001
            invokeEntityHit(trident, tridentTargets[1])
            invokeEntityHit(trident, tridentTargets[2])
            val tridentRemovedAfterThird = trident.isRemoved

            val ok = planeDamaged &&
                planeRemoved &&
                anvilPickableBefore &&
                anvilDamaged &&
                anvilVelocityTransferred &&
                anvilCooldownRestored &&
                tridentDamaged &&
                tridentPierced &&
                tridentVelocityReversed &&
                tridentRemovedAfterThird

            if (ok) {
                Yaha.LOGGER.info(
                    "[YAHA-PROBE] projectile_collision=PASS plane_damage={} plane_removed={} anvil_damage={} anvil_cooldown_restored={} trident_damage={} trident_pierced={} trident_removed_after_third={}",
                    planeHealth - planeTarget.health,
                    planeRemoved,
                    anvilHealth - anvilTarget.health,
                    anvilCooldownRestored,
                    tridentHealth - tridentTargets[0].health,
                    tridentPierced,
                    tridentRemovedAfterThird,
                )
                0
            } else {
                Yaha.LOGGER.error(
                    "[YAHA-PROBE] projectile_collision=FAIL plane_damaged={} plane_removed={} anvil_pickable_before={} anvil_damaged={} anvil_velocity_transferred={} anvil_cooldown_restored={} trident_damaged={} trident_pierced={} trident_velocity_reversed={} trident_removed_after_third={}",
                    planeDamaged,
                    planeRemoved,
                    anvilPickableBefore,
                    anvilDamaged,
                    anvilVelocityTransferred,
                    anvilCooldownRestored,
                    tridentDamaged,
                    tridentPierced,
                    tridentVelocityReversed,
                    tridentRemovedAfterThird,
                )
                1
            }
        } catch (throwable: Throwable) {
            Yaha.LOGGER.error("[YAHA-PROBE] projectile_collision=FAIL exception", throwable)
            1
        } finally {
            spawned.forEach(Entity::discard)
        }
    }

    private fun checkProjectileTickCollision(event: ServerStartedEvent): Int {
        val level = event.server.overworld()
        val env = probeEnv(level)
        val owner = env.castingEntity!!
        val horizontalOrigin = owner.position().add(28.0, 0.0, 0.0)
        val originBlockX = kotlin.math.floor(horizontalOrigin.x).toInt()
        val originBlockZ = kotlin.math.floor(horizontalOrigin.z).toInt()
        var topBlockingY = level.minBuildHeight
        for (x in (originBlockX - 2)..(originBlockX + 22)) {
            for (z in (originBlockZ - 2)..(originBlockZ + 2)) {
                topBlockingY = maxOf(
                    topBlockingY,
                    level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z),
                )
            }
        }
        val clearY = topBlockingY + 3
        require(clearY < level.maxBuildHeight) {
            "No clear projectile probe corridor below build height: y=$clearY"
        }
        val origin = Vec3(horizontalOrigin.x, clearY.toDouble(), horizontalOrigin.z)
        val spawned = mutableListOf<Entity>()

        fun add(entity: Entity, label: String) {
            if (!level.addFreshEntity(entity)) {
                throw IllegalStateException("could not add $label")
            }
            spawned += entity
        }

        fun pig(label: String, pos: Vec3): Pig {
            val entity = Pig(EntityType.PIG, level)
            entity.setPos(pos)
            add(entity, label)
            return entity
        }

        return try {
            val planeTarget = pig("paper_plane_tick_target", origin.add(2.0, 0.0, 0.0))
            val plane = PaperPlaneEntity(level, owner, planeTarget, origin)
            plane.deltaMovement = Vec3(2.0, 0.0, 0.0)
            add(plane, "paper_plane_tick")
            val planeHealth = planeTarget.health
            val planeHitBeforeTick = ProjectileUtil.getHitResultOnMoveVector(plane) {
                invokeCanHitEntity(plane, it)
            }
            val planeCandidatesBeforeTick = level.getEntities(
                plane,
                plane.boundingBox.expandTowards(plane.deltaMovement).inflate(1.0),
            ) {
                invokeCanHitEntity(plane, it)
            }
            plane.tick()
            val planeDamaged = planeTarget.health < planeHealth
            val planeRemoved = plane.isRemoved

            val anvilOrigin = origin.add(8.0, 0.0, 0.0)
            val anvil = AnvilEntity(level, owner, anvilOrigin)
            anvil.isNoGravity = true
            anvil.deltaMovement = Vec3.ZERO
            add(anvil, "anvil_tick")
            repeat(2) { anvil.tick() }
            val anvilTarget = pig("anvil_tick_target", anvil.position().add(2.25, 0.0, 0.0))
            val anvilHealth = anvilTarget.health
            anvil.deltaMovement = Vec3(1.0, 0.0, 0.0)
            anvil.tick()
            val anvilDamaged = anvilTarget.health < anvilHealth
            val anvilCooldownRestored = !anvil.isPickable

            val tridentOrigin = origin.add(16.0, 0.0, 0.0)
            val tridentTarget = pig("trident_tick_target", tridentOrigin.add(2.0, 0.0, 0.0))
            val trident = TridentEntity(level, owner, tridentOrigin)
            trident.isNoGravity = true
            trident.deltaMovement = Vec3(1.0, 0.0, 0.0)
            add(trident, "trident_tick")
            val tridentHealth = tridentTarget.health
            val tridentHitBeforeTick = ProjectileUtil.getHitResultOnMoveVector(trident) {
                invokeCanHitEntity(trident, it)
            }
            val tridentCandidatesBeforeTick = level.getEntities(
                trident,
                trident.boundingBox.expandTowards(trident.deltaMovement).inflate(1.0),
            ) {
                invokeCanHitEntity(trident, it)
            }
            trident.tick()
            val tridentDamaged = tridentTarget.health < tridentHealth
            val tridentPierced = trident.piercedEntities.contains(tridentTarget.id)
            val tridentHitAfterTick = if (trident.isRemoved) null else {
                ProjectileUtil.getHitResultOnMoveVector(trident) {
                    invokeCanHitEntity(trident, it)
                }
            }

            val ok = planeDamaged &&
                planeRemoved &&
                anvilDamaged &&
                anvilCooldownRestored &&
                tridentDamaged &&
                tridentPierced

            if (ok) {
                Yaha.LOGGER.info(
                    "[YAHA-PROBE] projectile_tick_collision=PASS plane_damage={} plane_removed={} anvil_damage={} anvil_cooldown_restored={} trident_damage={} trident_pierced={}",
                    planeHealth - planeTarget.health,
                    planeRemoved,
                    anvilHealth - anvilTarget.health,
                    anvilCooldownRestored,
                    tridentHealth - tridentTarget.health,
                    tridentPierced,
                )
                0
            } else {
                Yaha.LOGGER.error(
                    "[YAHA-PROBE] projectile_tick_collision=FAIL plane_damaged={} plane_removed={} anvil_damaged={} anvil_cooldown_restored={} trident_damaged={} trident_pierced={}",
                    planeDamaged,
                    planeRemoved,
                    anvilDamaged,
                    anvilCooldownRestored,
                    tridentDamaged,
                    tridentPierced,
                )
                Yaha.LOGGER.error(
                    "[YAHA-PROBE] projectile_tick_collision_diagnostic plane_pos={} plane_velocity={} plane_target_pos={} plane_target_box={} plane_hit_before={} plane_candidates={} trident_pos={} trident_velocity={} trident_target_pos={} trident_target_box={} trident_hit_before={} trident_hit_after={} trident_candidates={}",
                    plane.position(),
                    plane.deltaMovement,
                    planeTarget.position(),
                    planeTarget.boundingBox,
                    describeHit(planeHitBeforeTick),
                    planeCandidatesBeforeTick.joinToString(",") { "${it.type.descriptionId}#${it.id}" },
                    trident.position(),
                    trident.deltaMovement,
                    tridentTarget.position(),
                    tridentTarget.boundingBox,
                    describeHit(tridentHitBeforeTick),
                    tridentHitAfterTick?.let(::describeHit) ?: "removed",
                    tridentCandidatesBeforeTick.joinToString(",") { "${it.type.descriptionId}#${it.id}" },
                )
                1
            }
        } catch (throwable: Throwable) {
            Yaha.LOGGER.error("[YAHA-PROBE] projectile_tick_collision=FAIL exception", throwable)
            1
        } finally {
            spawned.forEach(Entity::discard)
        }
    }

    private fun checkTimeBombActions(event: ServerStartedEvent): Int {
        val level = event.server.overworld()
        val env = probeEnv(level)
        val origin = env.castingEntity!!.position()
        val spawned = mutableListOf<Entity>()

        return try {
            val mediaMultiplier = 1.0
            val lifetimeTicks = 0.0
            val bombImage = OpTimeBomb.execute(
                listOf(
                    Vec3Iota(origin.add(3.0, 2.0, 0.0)),
                    ListIota(emptyList()),
                    DoubleIota(mediaMultiplier),
                    DoubleIota(lifetimeTicks),
                ),
                env,
            ).effect.cast(env, CastingImage()) ?: throw IllegalStateException("time_bomb did not return a casting image")
            val bomb = entityFromImage(bombImage, level) as? TimeBombEntity
                ?: throw IllegalStateException("time_bomb did not push a TimeBombEntity")
            spawned += bomb

            val posIota = OpTimeBombPos.execute(emptyList(), TimeBombCastEnv(level, bomb)).firstOrNull() as? Vec3Iota
                ?: throw IllegalStateException("time_bomb_pos did not return a vector")
            val expectedMedia = (MediaConstants.DUST_UNIT * mediaMultiplier).toLong()
            val mediaOk = bomb.getMedia() == expectedMedia
            val posOk = posIota.vec3.distanceToSqr(bomb.position()) < 0.000001
            val presentBeforeTicks = !bomb.isRemoved

            bomb.tick()
            val expired = bomb.isRemoved

            if (mediaOk && posOk && presentBeforeTicks && expired) {
                Yaha.LOGGER.info(
                    "[YAHA-PROBE] time_bomb_actions=PASS media={} pos=({},{},{}) expired={}",
                    bomb.getMedia(),
                    posIota.vec3.x,
                    posIota.vec3.y,
                    posIota.vec3.z,
                    expired,
                )
                0
            } else {
                Yaha.LOGGER.error(
                    "[YAHA-PROBE] time_bomb_actions=FAIL media={} expected_media={} pos_ok={} present_before_ticks={} expired={}",
                    bomb.getMedia(),
                    expectedMedia,
                    posOk,
                    presentBeforeTicks,
                    expired,
                )
                1
            }
        } catch (throwable: Throwable) {
            Yaha.LOGGER.error("[YAHA-PROBE] time_bomb_actions=FAIL exception", throwable)
            1
        } finally {
            spawned.forEach(Entity::discard)
        }
    }

    private fun checkTimeBombNonEmptyHex(event: ServerStartedEvent): Int {
        val level = event.server.overworld()
        val env = probeEnv(level)
        val origin = env.castingEntity!!.position()
        val spawned = mutableListOf<Entity>()

        return try {
            val timeBombPosPattern = PatternIota(HexPattern.fromAngles("eewaqaweedd", HexDir.NORTH_WEST))
            val mediaMultiplier = 1.0
            val lifetimeTicks = 0.0
            val bombImage = OpTimeBomb.execute(
                listOf(
                    Vec3Iota(origin.add(7.0, 2.0, 0.0)),
                    ListIota(listOf(timeBombPosPattern)),
                    DoubleIota(mediaMultiplier),
                    DoubleIota(lifetimeTicks),
                ),
                env,
            ).effect.cast(env, CastingImage()) ?: throw IllegalStateException("time_bomb did not return a casting image")
            val bomb = entityFromImage(bombImage, level) as? TimeBombEntity
                ?: throw IllegalStateException("time_bomb did not push a TimeBombEntity")
            spawned += bomb

            val directView = CastingVM(CastingImage(), TimeBombCastEnv(level, bomb))
                .queueExecuteAndWrapIota(timeBombPosPattern, level)
            val directPos = directView.stackDescs.lastOrNull() as? Vec3Iota
            val directOk = directView.resolutionType.success &&
                directPos != null &&
                directPos.vec3.distanceToSqr(bomb.position()) < 0.000001
            val presentBeforeTicks = !bomb.isRemoved

            bomb.tick()
            val expired = bomb.isRemoved

            if (directOk && presentBeforeTicks && expired) {
                Yaha.LOGGER.info(
                    "[YAHA-PROBE] time_bomb_non_empty=PASS pattern=time_bomb_pos resolution={} stack_vec_ok={} expired={}",
                    directView.resolutionType,
                    directOk,
                    expired,
                )
                0
            } else {
                Yaha.LOGGER.error(
                    "[YAHA-PROBE] time_bomb_non_empty=FAIL resolution={} success={} stack_vec_ok={} present_before_ticks={} expired={}",
                    directView.resolutionType,
                    directView.resolutionType.success,
                    directOk,
                    presentBeforeTicks,
                    expired,
                )
                1
            }
        } catch (throwable: Throwable) {
            Yaha.LOGGER.error("[YAHA-PROBE] time_bomb_non_empty=FAIL exception", throwable)
            1
        } finally {
            spawned.forEach(Entity::discard)
        }
    }

    private fun checkTimeBombStateChangingHex(event: ServerStartedEvent): Int {
        val level = event.server.overworld()
        val env = probeEnv(level)
        val origin = env.castingEntity!!.position()
        val spawned = mutableListOf<Entity>()
        val changedPos = BlockPos.containing(origin.add(5.0, 1.0, 4.0))

        return try {
            level.setBlockAndUpdate(changedPos, Blocks.STONE.defaultBlockState())
            val bomb = TimeBombEntity(
                level,
                env.castingEntity,
                listOf(ProbeStateChangeIota(changedPos)),
                MediaConstants.DUST_UNIT,
                env.pigment,
                0,
                origin.add(5.0, 2.0, 4.0),
            )
            if (!level.addFreshEntity(bomb)) {
                throw IllegalStateException("could not add state-changing time bomb")
            }
            spawned += bomb
            val beforeIsStone = level.getBlockState(changedPos).`is`(Blocks.STONE)
            val presentBeforeTicks = !bomb.isRemoved

            bomb.tick()

            val expired = bomb.isRemoved
            val changedToGold = level.getBlockState(changedPos).`is`(Blocks.GOLD_BLOCK)

            if (beforeIsStone && presentBeforeTicks && expired && changedToGold) {
                Yaha.LOGGER.info(
                    "[YAHA-PROBE] time_bomb_state_change=PASS payload=probe_executable_iota block={} expired={}",
                    level.getBlockState(changedPos).block.descriptionId,
                    expired,
                )
                0
            } else {
                Yaha.LOGGER.error(
                    "[YAHA-PROBE] time_bomb_state_change=FAIL before_is_stone={} present_before_ticks={} expired={} changed_to_gold={} block={}",
                    beforeIsStone,
                    presentBeforeTicks,
                    expired,
                    changedToGold,
                    level.getBlockState(changedPos).block.descriptionId,
                )
                1
            }
        } catch (throwable: Throwable) {
            Yaha.LOGGER.error("[YAHA-PROBE] time_bomb_state_change=FAIL exception", throwable)
            1
        } finally {
            spawned.forEach(Entity::discard)
            level.setBlockAndUpdate(changedPos, Blocks.AIR.defaultBlockState())
        }
    }

    private class ProbeStateChangeIota(private val pos: BlockPos) : Iota({ NullIota.TYPE }) {
        override fun isTruthy(): Boolean = true

        override fun toleratesOther(other: Iota): Boolean =
            other is ProbeStateChangeIota && other.pos == pos

        override fun executable(): Boolean = true

        override fun execute(vm: CastingVM, world: ServerLevel, continuation: SpellContinuation): CastResult {
            world.setBlockAndUpdate(pos, Blocks.GOLD_BLOCK.defaultBlockState())
            return CastResult(
                this,
                continuation,
                vm.image,
                emptyList(),
                ResolvedPatternType.EVALUATED,
                HexEvalSounds.NORMAL_EXECUTE.get(),
            )
        }

        override fun display(): Component = Component.literal("yaha probe state change")

        override fun hashCode(): Int = pos.hashCode()
    }

    private fun checkBundleItems(event: ServerStartedEvent): Int {
        val level = event.server.overworld()
        val player = FakePlayerFactory.getMinecraft(level)
        val spindleItem = YahaItems.SPINDLE.get()
        val pouchItem = YahaItems.POUCH.get()
        val spindle = ItemStack(spindleItem, 1)
        val pouch = ItemStack(pouchItem, 1)
        val thought = ItemStack(HexItems.THOUGHT_KNOT.get(), 1)
        val focus = ItemStack(HexItems.FOCUS.get(), 1)
        val writtenSpindle = ItemStack(spindleItem, 1)
        val writtenPouch = ItemStack(pouchItem, 1)
        val writtenThought = ItemStack(HexItems.THOUGHT_KNOT.get(), 1)
        val writtenFocus = ItemStack(HexItems.FOCUS.get(), 1)
        val firstSyncedFocus = ItemStack(HexItems.FOCUS.get(), 1)
        val secondSyncedFocus = ItemStack(HexItems.FOCUS.get(), 1)
        firstSyncedFocus.set(DataComponents.CUSTOM_NAME, Component.literal("first-synced-focus"))
        secondSyncedFocus.set(DataComponents.CUSTOM_NAME, Component.literal("second-synced-focus"))

        return try {
            val spindleAdded = spindleItem.addOneToBundle(spindle, thought)
            val spindleRejectedFocus = !spindleItem.addOneToBundle(spindle, focus)
            val spindleRead = spindleItem.readIota(spindle)
            val spindleReadOk = spindleRead is NullIota
            val spindleBarOk = spindleItem.isBarVisible(spindle) && spindleItem.getBarWidth(spindle) > 0
            val spindleTooltipOk = spindleItem.getTooltipImage(spindle).isPresent
            val removedThought = spindleItem.removeFirst(spindle)
            val spindleRemoveOk = removedThought?.item == HexItems.THOUGHT_KNOT.get() &&
                IotaHolderBundle.getBundleOccupancy(spindle) == 0

            val pouchAdded = pouchItem.addOneToBundle(pouch, focus)
            val pouchRejectedThought = !pouchItem.addOneToBundle(pouch, thought)
            val pouchRead = pouchItem.readIota(pouch)
            val pouchReadOk = pouchRead is NullIota
            BundleSelection.set(player, 0)
            val removedFocus = pouchItem.removeSelected(pouch, player)
            val pouchRemoveOk = removedFocus?.item == HexItems.FOCUS.get() &&
                IotaHolderBundle.getBundleOccupancy(pouch) == 0

            val syncedSelectionPouch = ItemStack(pouchItem, 1)
            val syncedFirstAdded = pouchItem.addOneToBundle(syncedSelectionPouch, firstSyncedFocus)
            val syncedSecondAdded = pouchItem.addOneToBundle(syncedSelectionPouch, secondSyncedFocus)
            YahaNetwork.applyBundleSelection(player, -8)
            val clampedSelectionOk = BundleSelection.get(player) == 0
            YahaNetwork.applyBundleSelection(player, 1)
            val syncedRemoved = pouchItem.removeSelected(syncedSelectionPouch, player)
            val networkSelectionOk = syncedFirstAdded &&
                syncedSecondAdded &&
                clampedSelectionOk &&
                syncedRemoved?.get(DataComponents.CUSTOM_NAME)?.string == "first-synced-focus" &&
                IotaHolderBundle.getBundleOccupancy(syncedSelectionPouch) == 1

            spindleItem.addOneToBundle(spindle, thought)
            val dropAllOk = spindleItem.dropAll(spindle, player) &&
                IotaHolderBundle.getBundleOccupancy(spindle) == 0

            val writtenThoughtIota = Vec3Iota(Vec3(1.25, 2.5, 3.75))
            val writtenFocusIota = Vec3Iota(Vec3(-4.0, 0.5, 9.0))
            val thoughtHolder = writtenThought.item as? IotaHolderItem
                ?: throw IllegalStateException("thought knot does not implement IotaHolderItem")
            val focusHolder = writtenFocus.item as? IotaHolderItem
                ?: throw IllegalStateException("focus does not implement IotaHolderItem")
            seedIotaComponentForProbe(writtenThought, writtenThoughtIota)
            seedIotaComponentForProbe(writtenFocus, writtenFocusIota)
            val directThoughtReadOk = ((thoughtHolder.readIota(writtenThought) as? Vec3Iota)?.vec3
                ?.distanceToSqr(writtenThoughtIota.vec3) ?: Double.POSITIVE_INFINITY) < 0.000001
            val directFocusReadOk = ((focusHolder.readIota(writtenFocus) as? Vec3Iota)?.vec3
                ?.distanceToSqr(writtenFocusIota.vec3) ?: Double.POSITIVE_INFINITY) < 0.000001

            val writtenSpindleAdded = spindleItem.addOneToBundle(writtenSpindle, writtenThought)
            val writtenSpindleRead = spindleItem.readIota(writtenSpindle)
            val writtenSpindleReadOk = ((writtenSpindleRead as? Vec3Iota)?.vec3?.distanceToSqr(writtenThoughtIota.vec3)
                ?: Double.POSITIVE_INFINITY) < 0.000001

            val writtenPouchAdded = pouchItem.addOneToBundle(writtenPouch, writtenFocus)
            val writtenPouchRead = pouchItem.readIota(writtenPouch)
            val writtenPouchReadOk = ((writtenPouchRead as? Vec3Iota)?.vec3?.distanceToSqr(writtenFocusIota.vec3)
                ?: Double.POSITIVE_INFINITY) < 0.000001

            val ok = spindleAdded &&
                spindleRejectedFocus &&
                spindleReadOk &&
                spindleBarOk &&
                spindleTooltipOk &&
                spindleRemoveOk &&
                pouchAdded &&
                pouchRejectedThought &&
                pouchReadOk &&
                pouchRemoveOk &&
                networkSelectionOk &&
                dropAllOk &&
                directThoughtReadOk &&
                directFocusReadOk &&
                writtenSpindleAdded &&
                writtenSpindleReadOk &&
                writtenPouchAdded &&
                writtenPouchReadOk

            if (ok) {
                Yaha.LOGGER.info(
                    "[YAHA-PROBE] bundle_items=PASS spindle_read={} pouch_read={} spindle_removed={} pouch_removed={} drop_all={} written_spindle={} written_pouch={}",
                    spindleRead?.javaClass?.simpleName,
                    pouchRead?.javaClass?.simpleName,
                    removedThought?.item?.descriptionId,
                    removedFocus?.item?.descriptionId,
                    dropAllOk,
                    writtenSpindleRead?.javaClass?.simpleName,
                    writtenPouchRead?.javaClass?.simpleName,
                )
                Yaha.LOGGER.info(
                    "[YAHA-PROBE] bundle_selection_sync=PASS clamped={} removed={} remaining={}",
                    clampedSelectionOk,
                    syncedRemoved?.get(DataComponents.CUSTOM_NAME)?.string,
                    IotaHolderBundle.getBundleOccupancy(syncedSelectionPouch),
                )
                0
            } else {
                Yaha.LOGGER.error(
                    "[YAHA-PROBE] bundle_items=FAIL spindle_added={} spindle_rejected_focus={} spindle_read_ok={} spindle_bar_ok={} spindle_tooltip_ok={} spindle_remove_ok={} pouch_added={} pouch_rejected_thought={} pouch_read_ok={} pouch_remove_ok={} network_selection_ok={} drop_all_ok={} direct_thought_read_ok={} direct_focus_read_ok={} written_spindle_added={} written_spindle_read_ok={} written_pouch_added={} written_pouch_read_ok={}",
                    spindleAdded,
                    spindleRejectedFocus,
                    spindleReadOk,
                    spindleBarOk,
                    spindleTooltipOk,
                    spindleRemoveOk,
                    pouchAdded,
                    pouchRejectedThought,
                    pouchReadOk,
                    pouchRemoveOk,
                    networkSelectionOk,
                    dropAllOk,
                    directThoughtReadOk,
                    directFocusReadOk,
                    writtenSpindleAdded,
                    writtenSpindleReadOk,
                    writtenPouchAdded,
                    writtenPouchReadOk,
                )
                1
            }
        } catch (throwable: Throwable) {
            Yaha.LOGGER.error("[YAHA-PROBE] bundle_items=FAIL exception", throwable)
            1
        } finally {
            BundleSelection.clear(player)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun seedIotaComponentForProbe(stack: ItemStack, iota: Vec3Iota) {
        val componentsField = ItemStack::class.java.getDeclaredField("components")
        componentsField.isAccessible = true
        val components = componentsField.get(stack)

        val patchField = components.javaClass.getDeclaredField("patch")
        patchField.isAccessible = true
        val copyOnWriteField = components.javaClass.getDeclaredField("copyOnWrite")
        copyOnWriteField.isAccessible = true

        val patch = Reference2ObjectArrayMap<DataComponentType<*>, Optional<Any>>()
        (patchField.get(components) as? Reference2ObjectMap<DataComponentType<*>, Optional<Any>>)?.let(patch::putAll)
        patch[HexDataComponents.IOTA_HOLDER_IOTA.get()] = Optional.of(iota)
        patchField.set(components, patch)
        copyOnWriteField.setBoolean(components, false)
    }

    private fun checkRecipes(event: ServerStartedEvent): Int {
        val missing = recipeIds.filterNot { event.server.recipeManager.byKey(it).isPresent }
        if (missing.isEmpty()) {
            Yaha.LOGGER.info("[YAHA-PROBE] recipes=PASS count={}", recipeIds.size)
        } else {
            Yaha.LOGGER.error("[YAHA-PROBE] recipes=FAIL missing={}", missing.joinToString(","))
        }
        return missing.size
    }

    private fun checkAdvancements(event: ServerStartedEvent): Int {
        val missing = advancementIds.filter { event.server.advancements.get(it) == null }
        if (missing.isEmpty()) {
            Yaha.LOGGER.info("[YAHA-PROBE] advancements=PASS count={}", advancementIds.size)
        } else {
            Yaha.LOGGER.error("[YAHA-PROBE] advancements=FAIL missing={}", missing.joinToString(","))
        }
        return missing.size
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkAdvancementCriterionTriggers(event: ServerStartedEvent): Int {
        val checks = listOf(
            Triple(Yaha.id("collide_planes"), "collide_planes", Yaha.id("collide_planes")),
            Triple(Yaha.id("susception"), "collide_planes", Yaha.id("susception")),
            Triple(Yaha.id("bomb_defusal"), "collide_planes", Yaha.id("bomb_defusal")),
        )

        return try {
            var failures = 0
            val results = mutableListOf<String>()
            for ((advancementId, criterionName, expectedTriggerId) in checks) {
                val advancement = event.server.advancements.get(advancementId)
                if (advancement == null) {
                    Yaha.LOGGER.error("[YAHA-PROBE] advancement_criteria=FAIL missing_advancement={}", advancementId)
                    failures++
                    continue
                }
                val criterion = advancement.value().criteria()[criterionName]
                if (criterion == null) {
                    Yaha.LOGGER.error("[YAHA-PROBE] advancement_criteria=FAIL missing_criterion={} criterion={}", advancementId, criterionName)
                    failures++
                    continue
                }

                val playerTrigger = criterion.trigger() as? PlayerTrigger
                if (playerTrigger == null) {
                    Yaha.LOGGER.error("[YAHA-PROBE] advancement_criteria=FAIL non_player_trigger={} criterion={}", advancementId, criterionName)
                    failures++
                    continue
                }
                val triggerId = BuiltInRegistries.TRIGGER_TYPES.getKey(playerTrigger)
                val triggerIdOk = triggerId == expectedTriggerId
                val progress = AdvancementProgress()
                progress.update(advancement.value().requirements())
                val granted = progress.grantProgress(criterionName)
                val done = progress.getCriterion(criterionName)?.isDone ?: false
                val ok = triggerIdOk && granted && done
                results += "${advancementId.path}=$ok"
                if (!ok) failures++
            }

            if (failures == 0) {
                Yaha.LOGGER.info("[YAHA-PROBE] advancement_criteria=PASS {}", results.joinToString(" "))
                0
            } else {
                Yaha.LOGGER.error("[YAHA-PROBE] advancement_criteria=FAIL {}", results.joinToString(" "))
                failures
            }
        } catch (throwable: Throwable) {
            Yaha.LOGGER.error("[YAHA-PROBE] advancement_criteria=FAIL exception", throwable)
            1
        }
    }

    private data class AdvancementTriggerProbe(
        val advancementId: ResourceLocation,
        val criterionName: String,
        val trigger: (ServerPlayer) -> Unit,
    )

    private fun checkAdvancementTriggerExecution(event: ServerStartedEvent): Int {
        val player = ServerPlayer(
            event.server,
            event.server.overworld(),
            GameProfile(UUID.nameUUIDFromBytes("yaha-probe-advancement-triggers".toByteArray()), "YahaProbe"),
            ClientInformation.createDefault(),
        )
        val playerAdvancements = player.advancements
        val checks = listOf(
            AdvancementTriggerProbe(Yaha.id("collide_planes"), "collide_planes") { YahaCriteria.COLLIDE_PLANES.trigger(it) },
            AdvancementTriggerProbe(Yaha.id("susception"), "collide_planes") { YahaCriteria.SUSCEPTION.trigger(it) },
            AdvancementTriggerProbe(Yaha.id("bomb_defusal"), "collide_planes") { YahaCriteria.BOMB_DEFUSAL.trigger(it) },
        )

        return try {
            playerAdvancements.setPlayer(player)
            playerAdvancements.reload(event.server.advancements)

            var failures = 0
            val results = mutableListOf<String>()
            for (check in checks) {
                val advancement = event.server.advancements.get(check.advancementId)
                if (advancement == null) {
                    Yaha.LOGGER.error("[YAHA-PROBE] advancement_triggers=FAIL missing_advancement={}", check.advancementId)
                    failures++
                    continue
                }

                repeat(3) {
                    val progress = playerAdvancements.getOrStartProgress(advancement)
                    if (progress.getCriterion(check.criterionName)?.isDone == true) {
                        playerAdvancements.revoke(advancement, check.criterionName)
                    }
                }

                val beforeProgress = playerAdvancements.getOrStartProgress(advancement)
                val beforeDone = beforeProgress.getCriterion(check.criterionName)?.isDone == true
                check.trigger(player)

                val afterProgress = playerAdvancements.getOrStartProgress(advancement)
                val criterionDone = afterProgress.getCriterion(check.criterionName)?.isDone == true
                val advancementDone = afterProgress.isDone
                val ok = !beforeDone && criterionDone && advancementDone
                results += "${check.advancementId.path}=$ok"
                if (!ok) {
                    failures++
                    Yaha.LOGGER.error(
                        "[YAHA-PROBE] advancement_triggers=FAIL advancement={} before_done={} criterion_done={} advancement_done={}",
                        check.advancementId,
                        beforeDone,
                        criterionDone,
                        advancementDone,
                    )
                }
            }

            if (failures == 0) {
                Yaha.LOGGER.info("[YAHA-PROBE] advancement_triggers=PASS {}", results.joinToString(" "))
                0
            } else {
                Yaha.LOGGER.error("[YAHA-PROBE] advancement_triggers=FAIL {}", results.joinToString(" "))
                failures
            }
        } catch (throwable: Throwable) {
            Yaha.LOGGER.error("[YAHA-PROBE] advancement_triggers=FAIL exception", throwable)
            1
        } finally {
            playerAdvancements.stopListening()
        }
    }

    private fun probeEnv(level: ServerLevel): StaffCastEnv {
        val player = FakePlayerFactory.getMinecraft(level)
        val spawn = level.sharedSpawnPos
        player.moveTo(spawn.x + 0.5, spawn.y + 2.0, spawn.z + 0.5, 0.0f, 0.0f)
        return StaffCastEnv(player, InteractionHand.MAIN_HAND)
    }

    private fun entityFromImage(image: CastingImage, level: ServerLevel): Entity? =
        (image.stack.lastOrNull() as? EntityIota)?.getEntity(level)

    private fun invokeEntityHit(projectile: Entity, target: Entity) {
        var type: Class<*>? = projectile.javaClass
        while (type != null) {
            try {
                val method = type.getDeclaredMethod("onHitEntity", EntityHitResult::class.java)
                method.isAccessible = true
                method.invoke(projectile, EntityHitResult(target))
                return
            } catch (ignored: NoSuchMethodException) {
                type = type.superclass
            }
        }
        throw NoSuchMethodException("${projectile.javaClass.name}.onHitEntity")
    }

    private fun invokeCanHitEntity(projectile: Entity, target: Entity): Boolean {
        var type: Class<*>? = projectile.javaClass
        while (type != null) {
            try {
                val method = type.getDeclaredMethod("canHitEntity", Entity::class.java)
                method.isAccessible = true
                return method.invoke(projectile, target) as Boolean
            } catch (ignored: NoSuchMethodException) {
                type = type.superclass
            }
        }
        throw NoSuchMethodException("${projectile.javaClass.name}.canHitEntity")
    }

    private fun describeHit(hit: HitResult): String = when (hit) {
        is EntityHitResult -> "entity:${hit.entity.type.descriptionId}#${hit.entity.id}@${hit.location}"
        else -> "${hit.type.name.lowercase()}:${hit.location}"
    }

    private fun close(actual: Float, expected: Float): Boolean =
        abs(actual - expected) < 0.01f
}
