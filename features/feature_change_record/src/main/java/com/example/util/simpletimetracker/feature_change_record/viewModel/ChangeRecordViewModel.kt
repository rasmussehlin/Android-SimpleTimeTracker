package com.example.util.simpletimetracker.feature_change_record.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.interactor.AddRunningRecordMediator
import com.example.util.simpletimetracker.core.interactor.RecordTagViewDataInteractor
import com.example.util.simpletimetracker.core.interactor.RecordTypesViewDataInteractor
import com.example.util.simpletimetracker.core.interactor.RemoveRunningRecordMediator
import com.example.util.simpletimetracker.core.interactor.WidgetInteractor
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.extension.orFalse
import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.interactor.RecordInteractor
import com.example.util.simpletimetracker.domain.interactor.RunningRecordInteractor
import com.example.util.simpletimetracker.domain.model.RangeLength
import com.example.util.simpletimetracker.domain.model.Record
import com.example.util.simpletimetracker.domain.model.WidgetType
import com.example.util.simpletimetracker.feature_change_record.interactor.ChangeRecordViewDataInteractor
import com.example.util.simpletimetracker.feature_change_record.viewData.ChangeRecordViewData
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.ChangeRecordParams
import com.example.util.simpletimetracker.navigation.params.screen.ChangeRecordTagFromChangeRecordParams
import com.example.util.simpletimetracker.navigation.params.screen.ChangeRecordTagFromScreen
import com.example.util.simpletimetracker.navigation.params.screen.ChangeTagData
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChangeRecordViewModel @Inject constructor(
    recordTypesViewDataInteractor: RecordTypesViewDataInteractor,
    recordTagViewDataInteractor: RecordTagViewDataInteractor,
    prefsInteractor: PrefsInteractor,
    resourceRepo: ResourceRepo,
    private val router: Router,
    private val recordInteractor: RecordInteractor,
    private val changeRecordViewDataInteractor: ChangeRecordViewDataInteractor,
    private val runningRecordInteractor: RunningRecordInteractor,
    private val addRunningRecordMediator: AddRunningRecordMediator,
    private val removeRunningRecordMediator: RemoveRunningRecordMediator,
    private val timeMapper: TimeMapper,
    private val widgetInteractor: WidgetInteractor,
) : ChangeRecordBaseViewModel(
    router,
    resourceRepo,
    prefsInteractor,
    recordTypesViewDataInteractor,
    recordTagViewDataInteractor,
    changeRecordViewDataInteractor,
) {

    lateinit var extra: ChangeRecordParams

    val record: LiveData<ChangeRecordViewData> by lazy {
        return@lazy MutableLiveData<ChangeRecordViewData>().let { initial ->
            viewModelScope.launch {
                initializePreviewViewData()
                initial.value = loadPreviewViewData()
            }
            initial
        }
    }

    fun onVisible() {
        updateCategoriesViewData()
    }

    fun onDeleteClick() {
        router.back()
    }

    override suspend fun onSaveClickDelegate() {
        // Zero id creates new record
        val id = (extra as? ChangeRecordParams.Tracked)?.id.orZero()
        Record(
            id = id,
            typeId = newTypeId,
            timeStarted = newTimeStarted,
            timeEnded = newTimeEnded,
            comment = newComment,
            tagIds = newCategoryIds
        ).let {
            recordInteractor.add(it)
            widgetInteractor.updateWidgets(listOf(WidgetType.STATISTICS_CHART))
            router.back()
        }
    }

    override suspend fun onSplitClickDelegate() {
        Record(
            id = 0L, // Zero id creates new record
            typeId = newTypeId,
            timeStarted = newTimeStarted,
            timeEnded = newTimeSplit,
            comment = newComment,
            tagIds = newCategoryIds
        ).let {
            recordInteractor.add(it)
        }
        newTimeStarted = newTimeSplit
        onSaveClick()
    }

    override suspend fun onContinueClickDelegate() {
        // Remove current record if exist.
        (extra as? ChangeRecordParams.Tracked)?.id?.let {
            recordInteractor.remove(it)
            widgetInteractor.updateWidgets(listOf(WidgetType.STATISTICS_CHART))
        }
        // Stop same type running record if exist (only one of the same type can run at once).
        runningRecordInteractor.get(newTypeId)
            ?.let { removeRunningRecordMediator.removeWithRecordAdd(it) }
        // Add new running record.
        addRunningRecordMediator.startTimer(
            typeId = newTypeId,
            timeStarted = newTimeStarted,
            comment = newComment,
            tagIds = newCategoryIds,
        )
        // Exit.
        router.back()
    }

    override suspend fun onMergeClickDelegate() {
        // Find previous record.
        val previousRecord = recordInteractor.getAll()
            .sortedByDescending { it.timeEnded }
            .firstOrNull {
                it.timeEnded <= newTimeStarted
            }
        // Change it.
        previousRecord?.copy(
            timeEnded = newTimeEnded,
        )?.let {
            recordInteractor.add(it)
            widgetInteractor.updateWidgets(listOf(WidgetType.STATISTICS_CHART))
            router.back()
        }
    }

    override suspend fun adjustAdjacentRecords() {
        if (adjustPrevRecordCheckbox.value.orFalse()) {
            val records = recordInteractor.getAll()

            // Find previous record.
            val previousRecord = records
                .sortedByDescending { it.timeEnded }
                .firstOrNull { it.timeEnded <= originalTimeStarted }
            // Change it.
            previousRecord?.copy(
                timeEnded = newTimeStarted,
            )?.let {
                recordInteractor.add(it)
            }

            // Find next record.
            val nextRecord = records
                .sortedByDescending { it.timeStarted }
                .firstOrNull { it.timeStarted >= originalTimeEnded }
            // Change it.
            nextRecord?.copy(
                timeStarted = newTimeEnded,
            )?.let {
                recordInteractor.add(it)
            }
        }
    }

    override fun getChangeCategoryParams(data: ChangeTagData): ChangeRecordTagFromScreen {
        return ChangeRecordTagFromChangeRecordParams(data)
    }

    override fun onTimeEndedChanged() {
        super.onTimeEndedChanged()
        if (newTimeEnded < newTimeStarted) newTimeStarted = newTimeEnded
        if (newTimeEnded < newTimeSplit) newTimeSplit = newTimeEnded
    }

    override fun onTimeStartedChanged() {
        super.onTimeStartedChanged()
        if (newTimeStarted > newTimeEnded) newTimeEnded = newTimeStarted
        if (newTimeStarted > newTimeSplit) newTimeSplit = newTimeStarted
    }

    override fun onTimeSplitChanged() {
        newTimeSplit = newTimeSplit.coerceIn(newTimeStarted..newTimeEnded)
    }

    private fun getInitialDate(daysFromToday: Int): Long {
        return timeMapper.toTimestampShifted(daysFromToday, RangeLength.Day)
    }

    override suspend fun updatePreview() {
        (record as MutableLiveData).value = loadPreviewViewData()
    }

    override suspend fun initializePreviewViewData() {
        when (extra) {
            is ChangeRecordParams.Tracked -> {
                recordInteractor.get((extra as ChangeRecordParams.Tracked).id)?.let { record ->
                    newTypeId = record.typeId.orZero()
                    newTimeStarted = record.timeStarted
                    newTimeEnded = record.timeEnded
                    newComment = record.comment
                    newCategoryIds = record.tagIds.toMutableList()
                }
            }
            is ChangeRecordParams.Untracked -> {
                newTimeStarted = (extra as ChangeRecordParams.Untracked).timeStarted
                newTimeEnded = (extra as ChangeRecordParams.Untracked).timeEnded
            }
            is ChangeRecordParams.New -> {
                newTimeEnded = getInitialDate((extra as ChangeRecordParams.New).daysFromToday)
                newTimeStarted = newTimeEnded - ONE_HOUR
            }
        }
        newTimeSplit = newTimeStarted
        originalTimeStarted = newTimeStarted
        originalTimeEnded = newTimeEnded
        updateTimeSplitValue()
    }

    private suspend fun loadPreviewViewData(): ChangeRecordViewData {
        val record = Record(
            typeId = newTypeId,
            timeStarted = newTimeStarted,
            timeEnded = newTimeEnded,
            comment = newComment,
            tagIds = newCategoryIds
        )

        return changeRecordViewDataInteractor.getPreviewViewData(record)
    }

    companion object {
        private const val ONE_HOUR: Int = 60 * 60 * 1000
    }
}
