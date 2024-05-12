package com.github.shalaga44.missingannotationstherapist.toolWindow

import com.github.shalaga44.missingannotationstherapist.services.*
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import java.awt.Container
import javax.swing.*

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {
        private val service = toolWindow.project.service<MyProjectService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            val matchTypeSelector = JComboBox(MyMatches.values())
            val matchValueField = JTextField(20)
            val annotationsField = JTextField(20)
            val scopeSelector = JComboBox(MyScope.values())
            val problemHighlightTypeSelector = JComboBox(ProblemHighlightType.values())
            val listModel = DefaultListModel<String>()
            val list = JList(listModel).apply {
                addListSelectionListener {
                    if (!selectionModel.isSelectionEmpty) {
                        val selected = service.matches[this.selectedIndex]
                        matchTypeSelector.setSelectedItem(selected.matches.first().type)
                        matchValueField.text = selected.matches.first().value
                        annotationsField.text = selected.annotations.joinToString(", ") { it.fqName }
                        scopeSelector.setSelectedItem(selected.scope.first())
                        problemHighlightTypeSelector.setSelectedItem(selected.problemHighlightType)
                    }
                }
            }

            // Setup UI components
            setupUIComponents(matchTypeSelector, matchValueField, annotationsField, scopeSelector, problemHighlightTypeSelector, list)

            // Initialize display with current data
            updateDisplay(listModel, service)
        }

        private fun Container.setupUIComponents(
            matchTypeSelector: JComboBox<MyMatches>,
            matchValueField: JTextField,
            annotationsField: JTextField,
            scopeSelector: JComboBox<MyScope>,
            problemHighlightTypeSelector: JComboBox<ProblemHighlightType>,
            list: JList<String>
        ) {
            add(JLabel("Select Matcher:"))
            add(JScrollPane(list))
            add(JLabel("Match Type:"))
            add(matchTypeSelector)
            add(JLabel("Match Value:"))
            add(matchValueField)
            add(JLabel("Annotations (comma-separated):"))
            add(annotationsField)
            add(JLabel("Scope:"))
            add(scopeSelector)
            add(JLabel("Problem Highlight Type:"))
            add(problemHighlightTypeSelector)
            add(createButtons(matchValueField, matchTypeSelector, annotationsField, scopeSelector, problemHighlightTypeSelector, list))
        }

        private fun createButtons(
            matchValueField: JTextField,
            matchTypeSelector: JComboBox<MyMatches>,
            annotationsField: JTextField,
            scopeSelector: JComboBox<MyScope>,
            problemHighlightTypeSelector: JComboBox<ProblemHighlightType>,
            list: JList<String>
        ): JPanel {
            val buttonPanel = JPanel().apply {
                add(JButton("Add").apply {
                    addActionListener {
                        val annotations = annotationsField.text.split(",").map { MyAnnotation(it.trim()) }
                        val newMatch = MyMatchFinder(type = matchTypeSelector.selectedItem as MyMatches, value = matchValueField.text)
                        val newMatcher = MyMissingMatcher(
                            annotations = annotations,
                            scope = listOf(scopeSelector.selectedItem as MyScope),
                            matches = listOf(newMatch),
                            problemHighlightType = problemHighlightTypeSelector.selectedItem as ProblemHighlightType
                        )
                        service.matches.add(newMatcher)
                        updateDisplay(list.model as DefaultListModel<String>, service)
                    }
                })
                add(JButton("Update").apply {
                    addActionListener {
                        val selectedIndex = list.selectedIndex
                        if (selectedIndex != -1) {
                            val matcherToUpdate = service.matches[selectedIndex]
                            val annotations = annotationsField.text.split(",").map { MyAnnotation(it.trim()) }
                            matcherToUpdate.matches = listOf(MyMatchFinder(matchTypeSelector.selectedItem as MyMatches, matchValueField.text))
                            matcherToUpdate.annotations = annotations
                            matcherToUpdate.scope = listOf(scopeSelector.selectedItem as MyScope)
                            matcherToUpdate.problemHighlightType = problemHighlightTypeSelector.selectedItem as ProblemHighlightType
                            updateDisplay(list.model as DefaultListModel<String>, service)
                        }
                    }
                })
                add(JButton("Remove").apply {
                    addActionListener {
                        val selectedIndex = list.selectedIndex
                        if (selectedIndex != -1) {
                            service.matches.removeAt(selectedIndex)
                            updateDisplay(list.model as DefaultListModel<String>, service)
                        }
                    }
                })
            }
            return buttonPanel
        }

        private fun updateDisplay(listModel: DefaultListModel<String>, service: MyProjectService) {
            listModel.clear()
            service.matches.forEach { matcher ->
                listModel.addElement("Matcher: ${matcher.scope.joinToString()}, ${matcher.annotations.joinToString { it.fqName }}, ${matcher.matches.joinToString { "${it.type} - ${it.value}" }}, Highlight: ${matcher.problemHighlightType}")
            }
        }
    }
}



