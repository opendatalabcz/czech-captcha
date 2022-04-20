package com.example.captcha.task.templates.texttemplate

import com.example.captcha.verification.*
import com.example.captcha.verification.entities.*
import com.example.captcha.task.templates.TaskTemplate
import com.example.captcha.task.templates.TemplateUtils.toBase64Image
import com.example.captcha.task.templates.TemplateUtils.toBase64String
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Component
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.random.Random

@Component("TEXT")
object TextTemplate: TaskTemplate {
    override fun generateTask(generationConfig: JsonNode, userName: String): Triple<Description, TaskData, AnswerSheet> {
        val description = Description("Type the text")
        val textToType = generateRandomText(5, 8)
        val data = TextData(textToType)

        val maskedTextImage = toMaskedImage(textToType)
        val answerSheet = AnswerSheet(ImageDisplayData(maskedTextImage), AnswerType.Text)

        return Triple(description, data, answerSheet)
    }

    override fun evaluateTask(taskData: TaskData, answer: Answer): EvaluationResult {
        val textAnswer = (answer as TextAnswer).text
        val correctAnswer = (taskData as TextData).text
        return if (correctAnswer == textAnswer) EvaluationResult(1F) else EvaluationResult(0F)
    }

    private fun generateRandomText(minLength: Int, maxLength: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        val length = Random.nextInt(minLength, maxLength + 1)
        return (1..length).map {
            allowedChars.random()
        }.joinToString("")
    }

    private fun toMaskedImage(text: String): String {
        val format = "png"
        val font = Font("Arial", Font.PLAIN, 48)

        val (width, height) = getImageDimensions(text, font)

        val img = BufferedImage(width + 20, height + 20, BufferedImage.TYPE_INT_ARGB)
        drawImage(text, img, font)

        val output = printImage(img).toByteArray()

        return toBase64Image(toBase64String(output), format)
    }

    private fun drawImage(text: String, img: BufferedImage, font: Font) {
        drawDrawText(text, img, font)
        drawOval(img)
    }

    private fun printImage(img: BufferedImage): ByteArrayOutputStream {
        val output = ByteArrayOutputStream()
        try {
            ImageIO.write(img, "png", output)
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return output
    }

    private fun getImageDimensions(text: String, font: Font): Pair<Int, Int> {
        val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g2d = img.createGraphics()
        g2d.font = font
        val fm = g2d.fontMetrics
        val width = fm.stringWidth(text)
        val height = fm.height
        g2d.dispose()

        return Pair(width, height)
    }

    private fun drawChar(c: Char, img: BufferedImage, font: Font, x: Int, baselineY: Int) {
        val g2d = img.createGraphics()
        g2d.translate(x, baselineY)
        g2d.translate(Random.nextInt(-5, 5), Random.nextInt(-5, 5))

        setGrapics(g2d, font)

        g2d.color = Color.BLACK
        val minRotation = 0.05
        val maxRotation = 0.45
        val rotation = Random.nextDouble(minRotation, maxRotation)
        val negativeModifier = if (Random.nextBoolean()) -1 else 1
        val finalRotation = negativeModifier * rotation

        g2d.rotate(finalRotation)
        val scale = Random.nextDouble(0.8, 1.2)
        g2d.scale(scale, scale)

        g2d.drawString(c.toString(), 0, 0)
        g2d.dispose()
    }


    private fun setGrapics(g2d: Graphics2D, font: Font) {
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE)
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g2d.font = font
    }

    private fun drawDrawText(text: String, img: BufferedImage, font: Font) {
        val g2d = img.createGraphics()
        setGrapics(g2d, font)
        val baselineY = g2d.fontMetrics.ascent + 10

        text.forEachIndexed { index, char ->
            val x = g2d.fontMetrics.stringWidth(text.substring(0, index)) + 10
            drawChar(char, img, font, x, baselineY)
        }

        g2d.dispose()
    }

    private fun drawOval(img: BufferedImage) {
        val heightAdjustment = Random.nextInt(-5, 5)
        val g2d = img.createGraphics()
        g2d.color = Color.BLACK
        val stroke =  BasicStroke(4f)
        g2d.setStroke(stroke)
        g2d.drawOval(5, img.height/3 + heightAdjustment, img.width - 5, img.height / 3 + heightAdjustment)
        g2d.dispose()
    }
}
