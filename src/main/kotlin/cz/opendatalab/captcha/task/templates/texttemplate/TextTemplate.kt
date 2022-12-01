package cz.opendatalab.captcha.task.templates.texttemplate

import cz.opendatalab.captcha.datamanagement.ImageUtils
import cz.opendatalab.captcha.task.templates.GenerationConfig
import cz.opendatalab.captcha.task.templates.TaskTemplate
import cz.opendatalab.captcha.verification.entities.*
import org.springframework.stereotype.Component
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Component("TEXT")
object TextTemplate: TaskTemplate {
    override fun generateTask(generationConfig: GenerationConfig, currentUser: String): Triple<Description, TaskData, AnswerSheet> {
        val description = Description("Type the text")
        val textToType = generateRandomText(5, 8)
        val data = TextData(textToType)

        val maskedTextImage = toMaskedImage(textToType)
        val answerSheet = AnswerSheet(ImageDisplayData(maskedTextImage), AnswerType.Text)

        return Triple(description, data, answerSheet)
    }

    override fun evaluateTask(taskData: TaskData, answer: Answer): EvaluationResult {
        val textAnswer = (answer as TextAnswer).text
        val expectedAnswer = (taskData as TextData).text

        return evaluate(textAnswer, expectedAnswer)
    }

    private fun evaluate(guessed: String, expected: String): EvaluationResult {
        val correctlyGuessed = guessed.zip(expected).filter { (guessed, expected) -> guessed == expected }.size

        return EvaluationResult(correctlyGuessed.toDouble() / expected.length)
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
        val font = Font("Courier", Font.PLAIN, 48)

        val (width, height) = getImageDimensions(text, font)

        val img = BufferedImage(width + 20, height + 20, BufferedImage.TYPE_INT_ARGB)
        drawImage(text, img, font)

        val output = printImage(img).use { it.toByteArray() }

        return ImageUtils.getBase64StringWithImage(output, format)
    }

    private fun drawImage(text: String, img: BufferedImage, font: Font) {
        drawDrawText(text, img, font)
        drawLine(img)
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

        setGraphics(g2d, font)

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


    private fun setGraphics(g2d: Graphics2D, font: Font) {
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
        setGraphics(g2d, font)
        val baselineY = g2d.fontMetrics.ascent + 10

        text.forEachIndexed { index, char ->
            val x = g2d.fontMetrics.stringWidth(text.substring(0, index)) + 10
            drawChar(char, img, font, x, baselineY)
        }

        g2d.dispose()
    }

    private fun drawLine(img: BufferedImage) {
        val g2d = img.createGraphics()
        g2d.color = Color.BLACK
        g2d.stroke = BasicStroke(4f)

        val maxX = 3 * img.height / 4
        val minX = img.height / 4

        var previousY = Random.nextInt(minX, maxX)

        val withDivider = 10

        val coordinates = (1 .. (img.width / withDivider - 1)).map { x ->
            val shift = Random.nextInt(minX/2, minX) * (if (Random.nextBoolean()) -1 else 1)
            val newY = previousY + shift
            val y = min(max(minX, newY), maxX)

            previousY = y

            Pair(x * withDivider, y)
        }

        val xPoints = coordinates.map { (x, _)  -> x }.toIntArray()
        val yPoints = coordinates.map { (_, y)  -> y }.toIntArray()
        g2d.drawPolyline(xPoints, yPoints, coordinates.size)

        g2d.dispose()
    }
}
