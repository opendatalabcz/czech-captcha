package com.example.captcha.task.templates.simpleimagetemplate

//@Component("SIMPLE_IMAGE")
//class SimpleImageTemplate(val imageMetadataService: ImageMetadataService): TaskTemplate {
//
//    private final val CHOICE_COUNT = 2
//    private final val  CORRECT_COUNT = 1
//    private final val  INCORRECT_COUNT = CHOICE_COUNT - CORRECT_COUNT
//
//    fun descriptionTemplate(label: String): Description {
//        return Description("Select all images that could be labeled with $label")
//    }
//
//    // todo should not send ids from db - instead create temporal ids
//    override fun generateTask(generationConfig: JsonNode, userName: String): Triple<Description, TaskData, AnswerSheet> {
//        // todo check whether there is enough images with/without label
//        val label = imageMetadataService.getRandomLabel()
//
//        val withLabel = imageMetadataService.getRandomWithLabel(label, CORRECT_COUNT)
//        val withoutLabel = imageMetadataService.getRandomWithoutLabel(label, INCORRECT_COUNT)
//
//        val chosenImages = ArrayList<Pair<ImageMetadata, ExpectedResult>>(CHOICE_COUNT)
//        chosenImages.addAll(withLabel.map{ Pair(it, ExpectedResult.CORRECT)})
//        chosenImages.addAll(withoutLabel.map{ Pair(it, ExpectedResult.INCORRECT)})
//
//        val expectedResults = chosenImages.map { Pair(it.first.fileId, it.second) }
//
//        val taskData = SimpleImageData(label, expectedResults)
//
//        val images = chosenImages.map {
//        // todo handle possible errors
//            val imageString = imageMetadataService.getBase64ImageString(it.first.fileId, it.first.format)!!
//            // todo
//            ImageDisplayData(imageString)
//        }.shuffled()
//
//        val answerSheet = AnswerSheet(ListDisplayData(images), AnswerType.MultipleText)
//
//        return Triple(descriptionTemplate(label.label), taskData, answerSheet)
//    }
//
//    override fun evaluateTask(task: Task, answer: Answer): EvaluationResult {
//        // todo answer is the order of images and not the fileIds!!
//        val imageLabelingAnswer = answer as TextListAnswer
//        val data = task.data as SimpleImageData
//
//        val correctAnswers = data.expectedResults.count { (fileId, expected) ->
//            val result = imageLabelingAnswer.texts.contains(fileId.toString())
//            if (expected == ExpectedResult.CORRECT)  result else !result
//        }
//
//        return EvaluationResult(correctAnswers.toFloat() / data.expectedResults.size)
//    }
//}
