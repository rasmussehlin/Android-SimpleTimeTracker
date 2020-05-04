package com.example.util.simpletimetracker.feature_running_records.adapter.recordType

import com.example.util.simpletimetracker.core.di.adapter.BaseRecyclerAdapter
import com.example.util.simpletimetracker.core.di.adapter.ViewHolderType

class RecordTypeAdapter(
    onItemClick: ((RecordTypeViewData) -> Unit)
) : BaseRecyclerAdapter() {

    init {
        delegates[ViewHolderType.VIEW] = RecordTypeAdapterDelegate(onItemClick)
    }
}