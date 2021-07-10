package com.example.util.simpletimetracker.core.view

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.databinding.RecordRunningViewLayoutBinding
import com.example.util.simpletimetracker.core.extension.visible
import com.example.util.simpletimetracker.core.viewData.RecordTypeIcon

class RunningRecordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(
    context,
    attrs,
    defStyleAttr
) {

    private val binding: RecordRunningViewLayoutBinding = RecordRunningViewLayoutBinding
        .inflate(LayoutInflater.from(context), this)

    var itemName: String = ""
        set(value) {
            field = value
            setItemName()
        }

    var itemTagName: String = ""
        set(value) {
            field = value
            setItemName()
        }

    var itemColor: Int = 0
        set(value) {
            setCardBackgroundColor(value)
            field = value
        }

    var itemTagColor: Int = Color.WHITE
        set(value) {
            field = value
            setItemName()
        }

    var itemIcon: RecordTypeIcon = RecordTypeIcon.Image(0)
        set(value) {
            binding.ivRunningRecordItemIcon.itemIcon = value
            field = value
        }

    var itemTimeStarted: String = ""
        set(value) {
            binding.tvRunningRecordItemTimeStarted.text = value
            field = value
        }

    var itemTimer: String = ""
        set(value) {
            binding.tvRunningRecordItemTimer.text = value
            field = value
        }

    var itemGoalTime: String = ""
        set(value) {
            binding.tvRunningRecordItemGoalTime.text = value
            binding.tvRunningRecordItemGoalTime.visible = value.isNotEmpty()
            field = value
        }

    var itemComment: String = ""
        set(value) {
            binding.tvRunningRecordItemComment.text = value
            binding.tvRunningRecordItemComment.visible = value.isNotEmpty()
            field = value
        }

    init {
        initProps()
        initAttrs(context, attrs, defStyleAttr)
    }

    private fun initProps() {
        ContextCompat.getColor(context, R.color.black).let(::setCardBackgroundColor)
        radius = resources.getDimensionPixelOffset(R.dimen.record_type_card_corner_radius).toFloat()
        // TODO doesn't work here for some reason, need to set in the layout
        cardElevation = resources.getDimensionPixelOffset(R.dimen.record_type_card_elevation).toFloat()
        preventCornerOverlap = false
        useCompatPadding = true
    }

    private fun initAttrs(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) {
        context.obtainStyledAttributes(attrs, R.styleable.RunningRecordView, defStyleAttr, 0)
            .run {
                if (hasValue(R.styleable.RunningRecordView_itemName)) itemName =
                    getString(R.styleable.RunningRecordView_itemName).orEmpty()

                if (hasValue(R.styleable.RunningRecordView_itemColor)) itemColor =
                    getColor(R.styleable.RunningRecordView_itemColor, Color.BLACK)

                if (hasValue(R.styleable.RunningRecordView_itemTagName)) itemTagName =
                    getString(R.styleable.RunningRecordView_itemTagName).orEmpty()

                if (hasValue(R.styleable.RunningRecordView_itemTagColor)) itemTagColor =
                    getColor(R.styleable.RunningRecordView_itemTagColor, Color.WHITE)

                if (hasValue(R.styleable.RunningRecordView_itemIcon)) itemIcon =
                    getResourceId(R.styleable.RunningRecordView_itemIcon, R.drawable.unknown)
                        .let(RecordTypeIcon::Image)

                if (hasValue(R.styleable.RunningRecordView_itemEmoji)) itemIcon =
                    getString(R.styleable.RunningRecordView_itemEmoji).orEmpty()
                        .let(RecordTypeIcon::Emoji)

                if (hasValue(R.styleable.RunningRecordView_itemTimeStarted)) itemTimeStarted =
                    getString(R.styleable.RunningRecordView_itemTimeStarted).orEmpty()

                if (hasValue(R.styleable.RunningRecordView_itemTimer)) itemTimer =
                    getString(R.styleable.RunningRecordView_itemTimer).orEmpty()

                if (hasValue(R.styleable.RunningRecordView_itemGoalTime)) itemGoalTime =
                    getString(R.styleable.RunningRecordView_itemGoalTime).orEmpty()

                if (hasValue(R.styleable.RunningRecordView_itemComment)) itemComment =
                    getString(R.styleable.RunningRecordView_itemComment).orEmpty()

                recycle()
            }
    }

    private fun setItemName() = with(binding) {
        if (itemTagName.isEmpty()) {
            tvRunningRecordItemName.text = itemName
        } else {
            val name = "$itemName - $itemTagName"
            val spannable = SpannableString(name)
            spannable.setSpan(
                ForegroundColorSpan(itemTagColor),
                itemName.length, name.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            tvRunningRecordItemName.text = spannable
        }
    }
}