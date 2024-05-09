package com.github.shalaga44.missingannotationstherapist.services

import com.github.shalaga44.missingannotationstherapist.MyBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

enum class MyScope {
    Class
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
    val shortName: String
)

data class MyMissingMatcher(
    val annotations: List<MyAnnotation>,
    val scope: List<MyScope>,
    val matches: List<MyMatchFinder>
)

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {
    val jsExport = MyMissingMatcher(
        listOf(MyAnnotation(shortName = "JsExport")), listOf(MyScope.Class), listOf(
            MyMatchFinder(
                MyMatches.EndWith,
                value = "dto"
            )
        )
    )
    val matches = listOf(jsExport)


    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    fun getRandomNumber() = (1..100).random()
}
