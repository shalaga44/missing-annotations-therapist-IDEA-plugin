package com.github.shalaga44.missingannotationstherapist.services

import com.github.shalaga44.missingannotationstherapist.MyBundle
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

enum class MyScope {
    KtClass
}

enum class MyMatches {
    EndWith,
    StartWith,
    Contains,
}

data class MyMatchFinder(
    val type: MyMatches,
    val value: String,
)

data class MyAnnotation(
    val fqName: String,
) {
    val shortName = fqName.substringAfterLast(".")
}

data class MyMissingMatcher(
    var annotations: List<MyAnnotation>,
    var scope: List<MyScope>,
    var matches: List<MyMatchFinder>,
    var problemHighlightType: ProblemHighlightType,

    )

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {
    val jsExport = MyMissingMatcher(
        mutableListOf(
            MyAnnotation(fqName = "kotlin.js.JsExport"),
        ),
        mutableListOf(MyScope.KtClass),
        mutableListOf(
            MyMatchFinder(
                MyMatches.EndWith,
                value = "dto"
            )
        ),
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
    )
    val matches: MutableList<MyMissingMatcher> = mutableListOf(jsExport)


    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }

    fun getRandomNumber() = (1..100).random()
}
