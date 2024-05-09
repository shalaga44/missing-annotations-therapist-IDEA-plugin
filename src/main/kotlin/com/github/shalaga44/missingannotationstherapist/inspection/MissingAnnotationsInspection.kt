package com.github.shalaga44.missingannotationstherapist.inspection

import com.github.shalaga44.missingannotationstherapist.services.MyMatches
import com.github.shalaga44.missingannotationstherapist.services.MyProjectService
import com.github.shalaga44.missingannotationstherapist.services.MyScope
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.psi.KtClass
import java.util.*

class MissingAnnotationsInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: com.intellij.psi.PsiElement) {
                super.visitElement(element)
                val service = holder.project.service<MyProjectService>()
                service.matches.forEach { match ->
                    match.scope.forEach { scope ->
                        when (scope) {
                            MyScope.Class -> {
                                if (element is KtClass) {
                                    val kotlinClass: KtClass = element as KtClass

                                    // Example logic: Check if class ends with "dto"
                                    val className: String = kotlinClass.name
                                        ?: throw Throwable("IDK what I'm doing because the indexing isn't working now")
                                    val finderMatchers = match.matches.map { matchFinder ->
                                        val matcher: (String) -> Boolean = {
                                            when (matchFinder.type) {
                                                MyMatches.EndWith -> it.endsWith(matchFinder.value)
                                                MyMatches.StartWith -> it.startsWith(matchFinder.value)
                                                MyMatches.Contains -> it.contains(matchFinder.value)
                                            }
                                        };matcher


                                    }
                                    val annotationsMatchers = match.annotations.map { matchFinder ->
                                        val matcher: (String) -> Boolean = {
                                            it == matchFinder.shortName
                                        };matcher


                                    }
                                    val classNameLowercase = className.lowercase(Locale.getDefault())
                                    if (className != null && finderMatchers.any { it.invoke(classNameLowercase) }) {
                                        // Check for missing annotation, e.g., @JsExport


                                        if (annotationsMatchers.any { mathcer ->
                                                val matches = kotlinClass.annotationEntries.toList()
                                                    .map { it.shortName!!.asString() }
                                                    .map { mathcer.invoke(it) }
                                                if (matches.all { true }) return@any true
                                                else false

                                            }) {
                                            holder.registerProblem(
                                                kotlinClass.getNameIdentifier()!!,
                                                "Class ends with 'dto' but is missing @kotlin.js.JsExport annotation."
                                            )
                                        }
                                    }
                                }


                            }
                        }
                    }

                }
            }
        }
    }


    @Override
    @Nls(capitalization = Nls.Capitalization.Sentence)
//    @NotNull
    override fun getDisplayName(): String {
        return "Missing required annotations on DTO classes"
    }

    @Override
    override fun getGroupDisplayName(): String {
        return "Kotlin Inspections"
    }

    @Override
    override fun getShortName(): String {
        return "MissingAnnotations"
    }

}
