package ai.metabind.feature.detail.screens

import ai.metabind.ai.MetabindAssistant
import ai.metabind.ai.MetabindAgentProvider
import ai.metabind.data.home.preview.MCPPreviewLink
import ai.metabind.data.home.preview.PreviewCredentials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.metabind.data.home.room.RecentsRepository
import ai.metabind.ui.delegates.AnalyticsDelegate
import ai.metabind.ui.delegates.AnalyticsDelegateImpl
import ai.metabind.ui.delegates.ViewStateProviderDelegate
import ai.metabind.ui.delegates.ViewStateProviderDelegateImpl
import ai.metabind.ui.navigation.NavigationConductor
import ai.metabind.ui.navigation.Screens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Serializable
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val itemRepository: RecentsRepository,
    private val credentials: PreviewCredentials,
    private val navigationConductor: NavigationConductor,
) : ViewModel(),
    AnalyticsDelegate by AnalyticsDelegateImpl(
        navigationName = "Detail"
    ),
    ViewStateProviderDelegate<DetailViewModel.ViewState> by ViewStateProviderDelegateImpl(
        ViewState.Loading,
        savedState
    ) {

    var assistant: MetabindAssistant? = null
        private set
    private var currentItem: Long? = null

    fun initialize(itemId: Long) {
        if (currentItem == itemId && viewState.value !is ViewState.Error) return
        currentItem = itemId
        updateState(ViewState.Loading)
        viewModelScope.launch {
            try {
                val item = withContext(Dispatchers.IO) { itemRepository.getById(itemId) }
                    ?: error("Missing preview")
                val project = MCPPreviewLink.parse(item.url)
                if (project == null) {
                    updateState(ViewState.Success(contentId = item.token))
                } else {
                    val key = withContext(Dispatchers.IO) { credentials.load(project) }
                        ?: error("Missing project access")
                    assistant?.close()
                    val chat = MetabindAssistant(apiKey = key, orgId = project.organizationId,
                        projectId = project.projectId, mcpHost = project.mcpHost, draft = true,
                        agentHost = if (project.isDevelopment) MetabindAgentProvider.DEVELOPMENT_HOST else MetabindAgentProvider.PRODUCTION_HOST)
                    assistant = chat
                    chat.awaitReady()
                    updateState(ViewState.Project(item.name ?: project.title, project.isDevelopment))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                assistant?.close()
                assistant = null
                updateState(ViewState.Error)
            }
        }
    }

    fun retry() { currentItem?.let(::initialize) }

    override fun onCleared() {
        assistant?.close()
        super.onCleared()
    }

    fun onBackPressed() {
        navigationConductor.request(Screens.RecentsPop)
    }

    sealed class ViewState : Serializable {
        object Loading : ViewState()
        object Error : ViewState()
        data class Project(val title: String, val development: Boolean) : ViewState()
        data class Success(
            val contentId: String,
        ) : ViewState()
    }
}
