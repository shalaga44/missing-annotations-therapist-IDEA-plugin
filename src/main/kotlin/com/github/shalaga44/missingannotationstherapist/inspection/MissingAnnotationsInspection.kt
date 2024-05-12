package com.github.shalaga44.missingannotationstherapist.inspection

import com.github.shalaga44.missingannotationstherapist.services.MyMatches
import com.github.shalaga44.missingannotationstherapist.services.MyProjectService
import com.github.shalaga44.missingannotationstherapist.services.MyScope
import com.intellij.codeInspection.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath
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
                            MyScope.KtClass -> {
                                if (element is KtClass) {
                                    val kotlinClass: KtClass = element

                                    // Example logic: Check if class ends with "dto"
                                    val className: String = kotlinClass.name
                                        ?: ""
                                    val finderMatchers = match.matches.map { matchFinder ->
                                        val matcher: (String) -> Boolean = {
                                            when (matchFinder.type) {
                                                MyMatches.EndWith -> it.endsWith(matchFinder.value)
                                                MyMatches.StartWith -> it.startsWith(matchFinder.value)
                                                MyMatches.Contains -> it.contains(matchFinder.value)
                                            }
                                        };matcher


                                    }
                                    val classNameLowercase = className.lowercase(Locale.getDefault())
                                    if (className.ifBlank { null } != null && finderMatchers.any {
                                            it.invoke(
                                                classNameLowercase
                                            )
                                        }) {
                                        val allAnnotationsFoundMap = match.annotations.map {
                                            val isAnnotationFound = element.findAnnotation(FqName(it.fqName)) != null
                                            it to isAnnotationFound
                                        }
                                        val allAnnotationsFound = allAnnotationsFoundMap.all { it.second }
                                        if (allAnnotationsFound.not()) {

                                            val addAnnotation = object : LocalQuickFix {

                                                @Nls(capitalization = Nls.Capitalization.Sentence)
                                                override fun getFamilyName(): String {
                                                    return "Add ${
                                                        allAnnotationsFoundMap.filterNot { it.second }
                                                            .joinToString { "@${it.first.shortName}" }
                                                    } to class declaration"
                                                }


                                                override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

                                                    fun PsiElement.addImport(import: String) {
                                                        val file = this.containingFile
                                                        if (file !is KtFile) return
                                                        val psiFactory = KtPsiFactory(project)
                                                        val importDirective = psiFactory.createImportDirective(
                                                            ImportPath(
                                                                FqName(import), false
                                                            )
                                                        )
                                                        if (file.importDirectives.none { it.importPath == importDirective.importPath }) {
                                                            file.importList?.add(importDirective)
                                                        }
                                                    }

                                                    var psiElement = descriptor.psiElement

                                                    // Navigate up the PSI tree to find the nearest enclosing KtClass
                                                    while (psiElement != null && psiElement !is KtClass) {
                                                        psiElement = psiElement.parent
                                                    }

                                                    if (psiElement is KtClass) {
                                                        val factory = KtPsiFactory(project)
                                                        allAnnotationsFoundMap.filterNot { it.second }.map { it.first }
                                                            .forEach {
                                                                val annotationEntry =
                                                                    factory.createAnnotationEntry("@${it.shortName}")
                                                                val isAnnotationNotFound =
                                                                    psiElement.findAnnotation(FqName(it.fqName)) == null

                                                                if (isAnnotationNotFound) {
                                                                    psiElement.addAnnotationEntry(annotationEntry)
                                                                    psiElement.addImport(it.fqName)
                                                                }
                                                            }


                                                    }
                                                }
                                            }
                                            val problemHighlightType: ProblemHighlightType = match.problemHighlightType
                                            holder.registerProblem(
                                                kotlinClass.nameIdentifier!!,
                                                "Class ends with 'dto' missing ${
                                                    allAnnotationsFoundMap.filterNot { it.second }
                                                        .joinToString { "@${it.first.shortName}" }
                                                } annotation.", problemHighlightType,
                                                addAnnotation
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

    companion object {
        private const val TAG = "MissingAnnotationsInspection"
    }


}

