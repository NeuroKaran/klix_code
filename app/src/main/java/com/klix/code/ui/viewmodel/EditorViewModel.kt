package com.klix.code.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.klix.code.data.model.DiffCalculator
import com.klix.code.data.model.EditorTab
import com.klix.code.data.model.FileDiff
import com.klix.code.data.model.FileNode
import com.klix.code.data.model.ProjectItem
import com.klix.code.data.repository.WorkspaceRepository
import com.klix.code.data.repository.WorkspaceRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel(
    application: Application,
    private val repository: WorkspaceRepository = WorkspaceRepositoryImpl(application)
) : AndroidViewModel(application) {

    private val _recentProjects = MutableStateFlow<List<ProjectItem>>(emptyList())
    val recentProjects: StateFlow<List<ProjectItem>> = _recentProjects.asStateFlow()

    private val _currentProject = MutableStateFlow<ProjectItem?>(null)
    val currentProject: StateFlow<ProjectItem?> = _currentProject.asStateFlow()

    private val _fileTree = MutableStateFlow<FileNode?>(null)
    val fileTree: StateFlow<FileNode?> = _fileTree.asStateFlow()

    private val _openTabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val openTabs: StateFlow<List<EditorTab>> = _openTabs.asStateFlow()

    private val _activeTabUri = MutableStateFlow<Uri?>(null)
    val activeTabUri: StateFlow<Uri?> = _activeTabUri.asStateFlow()

    private val _isWordWrap = MutableStateFlow(false)
    val isWordWrap: StateFlow<Boolean> = _isWordWrap.asStateFlow()

    private val _fontSizeSp = MutableStateFlow(14f)
    val fontSizeSp: StateFlow<Float> = _fontSizeSp.asStateFlow()

    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()

    private val _activeDiff = MutableStateFlow<FileDiff?>(null)
    val activeDiff: StateFlow<FileDiff?> = _activeDiff.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>("Ready")
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _cursorPosition = MutableStateFlow(Pair(1, 1))
    val cursorPosition: StateFlow<Pair<Int, Int>> = _cursorPosition.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadRecentProjects()
    }

    fun loadRecentProjects() {
        viewModelScope.launch {
            val list = repository.getRecentProjects()
            _recentProjects.value = list
            if (_currentProject.value == null && list.isNotEmpty()) {
                // Restore the most recent project automatically across app restarts
                openProject(list.first().uri, list.first().name)
            }
        }
    }

    fun openProject(uri: Uri, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Opening project $name..."
            try {
                repository.persistProjectPermission(uri, name)
                _currentProject.value = ProjectItem(uri, name)
                refreshTree()
                _statusMessage.value = "Workspace loaded"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to open project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshTree() {
        val proj = _currentProject.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tree = repository.loadTree(proj.uri)
                _fileTree.value = tree
            } catch (e: Exception) {
                _statusMessage.value = "Failed to load files: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openFile(fileNode: FileNode) {
        if (fileNode.isDirectory) return

        val existingTab = _openTabs.value.find { it.uri == fileNode.uri }
        if (existingTab != null) {
            _activeTabUri.value = existingTab.uri
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val content = repository.readFile(fileNode.uri)
                val newTab = EditorTab(
                    uri = fileNode.uri,
                    name = fileNode.name,
                    content = content,
                    originalContent = content
                )
                _openTabs.value = _openTabs.value + newTab
                _activeTabUri.value = newTab.uri
                _statusMessage.value = "Opened ${fileNode.name}"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to read file: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectTab(uri: Uri) {
        _activeTabUri.value = uri
    }

    fun closeTab(uri: Uri) {
        val currentTabs = _openTabs.value
        val index = currentTabs.indexOfFirst { it.uri == uri }
        if (index >= 0) {
            val updated = currentTabs.toMutableList().apply { removeAt(index) }
            _openTabs.value = updated
            if (_activeTabUri.value == uri) {
                _activeTabUri.value = when {
                    updated.isEmpty() -> null
                    index < updated.size -> updated[index].uri
                    else -> updated.last().uri
                }
            }
        }
    }

    fun updateActiveTabContent(newContent: String) {
        val activeUri = _activeTabUri.value ?: return
        _openTabs.value = _openTabs.value.map { tab ->
            if (tab.uri == activeUri) {
                val isModified = newContent != tab.originalContent
                tab.copy(content = newContent, isModified = isModified)
            } else {
                tab
            }
        }
    }

    fun saveActiveTab() {
        val activeUri = _activeTabUri.value ?: return
        val tab = _openTabs.value.find { it.uri == activeUri } ?: return
        if (!tab.isModified) return

        viewModelScope.launch {
            try {
                repository.writeFile(tab.uri, tab.content)
                _openTabs.value = _openTabs.value.map {
                    if (it.uri == activeUri) it.copy(isModified = false, originalContent = it.content) else it
                }
                _statusMessage.value = "Saved ${tab.name}"
            } catch (e: Exception) {
                _statusMessage.value = "Error saving file: ${e.message}"
            }
        }
    }

    fun createFile(parentUri: Uri, fileName: String) {
        viewModelScope.launch {
            try {
                val newUri = repository.createFile(parentUri, fileName)
                if (newUri != null) {
                    refreshTree()
                    _statusMessage.value = "Created $fileName"
                    val node = FileNode(uri = newUri, name = fileName, isDirectory = false)
                    openFile(node)
                }
            } catch (e: Exception) {
                _statusMessage.value = "Failed to create file: ${e.message}"
            }
        }
    }

    fun createDirectory(parentUri: Uri, dirName: String) {
        viewModelScope.launch {
            try {
                val newUri = repository.createDirectory(parentUri, dirName)
                if (newUri != null) {
                    refreshTree()
                    _statusMessage.value = "Created directory $dirName"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Failed to create directory: ${e.message}"
            }
        }
    }

    fun deleteNode(node: FileNode) {
        viewModelScope.launch {
            try {
                val deleted = repository.delete(node.uri)
                if (deleted) {
                    closeTab(node.uri)
                    refreshTree()
                    _statusMessage.value = "Deleted ${node.name}"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Failed to delete: ${e.message}"
            }
        }
    }

    fun renameNode(node: FileNode, newName: String) {
        viewModelScope.launch {
            try {
                val renamed = repository.rename(node.uri, newName)
                if (renamed) {
                    refreshTree()
                    _openTabs.value = _openTabs.value.map {
                        if (it.uri == node.uri) it.copy(name = newName) else it
                    }
                    _statusMessage.value = "Renamed to $newName"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Failed to rename: ${e.message}"
            }
        }
    }

    fun updateCursor(line: Int, col: Int) {
        _cursorPosition.value = Pair(line, col)
    }

    fun toggleWordWrap() {
        _isWordWrap.value = !_isWordWrap.value
    }

    fun zoomIn() {
        _fontSizeSp.value = (_fontSizeSp.value + 1f).coerceAtMost(28f)
    }

    fun zoomOut() {
        _fontSizeSp.value = (_fontSizeSp.value - 1f).coerceAtLeast(9f)
    }

    fun toggleSearch() {
        _isSearchOpen.value = !_isSearchOpen.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setReplaceQuery(query: String) {
        _replaceQuery.value = query
    }

    fun showProposedDiff(filePath: String, originalText: String, modifiedText: String, uri: Uri? = null) {
        val diff = DiffCalculator.computeDiff(
            filePath = filePath,
            originalText = originalText,
            modifiedText = modifiedText,
            fileUri = uri
        )
        _activeDiff.value = diff
    }

    fun applyActiveDiff() {
        val diff = _activeDiff.value ?: return
        val uri = diff.fileUri
        if (uri != null) {
            updateTabContentForUri(uri, diff.modifiedText)
            viewModelScope.launch {
                try {
                    repository.writeFile(uri, diff.modifiedText)
                    _statusMessage.value = "Applied diff to ${diff.filePath}"
                } catch (e: Exception) {
                    _statusMessage.value = "Failed to apply diff: ${e.message}"
                }
            }
        }
        _activeDiff.value = null
    }

    fun discardActiveDiff() {
        _activeDiff.value = null
        _statusMessage.value = "Diff discarded"
    }

    private fun updateTabContentForUri(uri: Uri, content: String) {
        _openTabs.value = _openTabs.value.map { tab ->
            if (tab.uri == uri) {
                tab.copy(content = content, isModified = false, originalContent = content)
            } else {
                tab
            }
        }
    }
}
