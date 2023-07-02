package studio.coni.epsilon.module.render

import studio.coni.epsilon.common.Category
import studio.coni.epsilon.event.SafeClientEvent
import studio.coni.epsilon.management.GUIManager
import studio.coni.epsilon.module.Module
import studio.coni.epsilon.util.graphics.ProjectionUtils
import studio.coni.epsilon.util.math.MathUtils.scale
import studio.coni.epsilon.util.onRender2D
import studio.coni.epsilon.util.onRender3D
import studio.coni.epsilon.util.threads.runSafe
import studio.coni.epsilon.util.world.getSelectedBox
import studio.coni.epsilon.common.extensions.*
import studio.coni.epsilon.util.graphics.font.renderer.MainFontRenderer
import net.minecraft.client.renderer.DestroyBlockProgress
import net.minecraft.client.renderer.Tessellator
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import org.lwjgl.opengl.GL11
import studio.coni.epsilon.event.events.Render2DEvent
import studio.coni.epsilon.event.events.Render3DEvent
import studio.coni.epsilon.event.listener
import studio.coni.epsilon.event.safeListener
import studio.coni.epsilon.util.ColorRGB
import studio.coni.epsilon.util.graphics.ESPRenderer
import kotlin.math.max
import kotlin.math.pow

object BreakESP :
    Module(name = "BreakESP", category = Category.Render, description = "Display brock breaking progress") {

    private val self by setting("Self", true)
    private val other by setting("Other", true)
    private val boxColor by setting("BoxColor", ColorRGB(200, 205, 210))
    private val textColor by setting("TextColor", ColorRGB(255, 255, 255))
    private val aFilled by setting("Filled Alpha", 31, 0..255, 1)
    private val aOutline by setting("Outline Alpha", 255, 0..255, 1)


    private val renderer = ESPRenderer()

    init {
        safeListener<Render2DEvent> {

            for (progress in mc.renderGlobal.damagedBlocks.values) {
                if (isInvalidBreaker(progress)) continue

                val text = "${(progress.partialBlockDamage + 1) * 10} %"
                val center = getBoundingBox(progress.position).center
                val screenPos = ProjectionUtils.toAbsoluteScreenPos(center)
                val distFactor = max(ProjectionUtils.distToCamera(center) - 1.0, 0.0)
                val scale = max(6.0f / 2.0.pow(distFactor).toFloat(), 1.0f)

                val x = MainFontRenderer.getWidth(text, scale) * -0.5f
                val y = MainFontRenderer.getHeight(scale) * -0.5f
                MainFontRenderer.drawString(text, screenPos.x.toFloat() + x, screenPos.y.toFloat() + y, scale = scale, color = textColor )
            }
        }

        safeListener<Render3DEvent> {
            renderer.aOutline = aOutline
            renderer.aFilled = aFilled

            for (progress in mc.renderGlobal.damagedBlocks.values) {
                if (isInvalidBreaker(progress)) continue
                val box = getBoundingBox(progress.position, progress.partialBlockDamage + 1)
                renderer.add(box, boxColor)
            }

            renderer.render(true)
        }
    }

    private fun SafeClientEvent.getBoundingBox(pos: BlockPos, progress: Int): AxisAlignedBB {
        return getBoundingBox(pos).scale(progress / 10.0)
    }


    private fun SafeClientEvent.getBoundingBox(pos: BlockPos): AxisAlignedBB {
        return world.getSelectedBox(pos)
    }

    private fun isInvalidBreaker(progress: DestroyBlockProgress): Boolean {
        val breakerID = progress.entityID

        return if (breakerID == mc.player?.entityId) {
            !self
        } else {
            !other
        }
    }
}
