package com.example.trainerengine.session

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.CheckBox
import android.widget.TextView
import com.example.trainerengine.R

class ModuleConfigAdapter(
    private val context: Context,
    private val expandableListTitle: List<String>,
    private val expandableListDetail: HashMap<String, List<String>>,
    private val onCheckBox: (Int, Int, CheckBox) -> Unit
) : BaseExpandableListAdapter() {
    private val childCheckBoxState = HashMap<Int, HashMap<Int, Boolean>>()

    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return expandableListDetail[expandableListTitle[listPosition]]!![expandedListPosition]
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    override fun getChildView(
        listPosition: Int,
        expandedListPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var convertView = convertView
        val expandedListText = getChild(listPosition, expandedListPosition) as String
        if (convertView == null) {
            val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.list_item, null)
        }
        val expandedListCheckbox = convertView!!.findViewById<View>(R.id.expandedListItem) as CheckBox
        expandedListCheckbox.text = expandedListText
        expandedListCheckbox.isChecked = childCheckBoxState[listPosition]?.get(expandedListPosition) ?: false
        expandedListCheckbox.setOnClickListener {
            onCheckBox(listPosition, expandedListPosition, expandedListCheckbox)
            if (childCheckBoxState[listPosition] == null) {
                childCheckBoxState[listPosition] = hashMapOf<Int, Boolean>(expandedListPosition to expandedListCheckbox.isChecked)
            } else{
                childCheckBoxState[listPosition]?.set(expandedListPosition, expandedListCheckbox.isChecked)
            }
        }
        return convertView
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return expandableListDetail[expandableListTitle[listPosition]]!!.size
    }

    override fun getGroup(listPosition: Int): Any {
        return expandableListTitle[listPosition]
    }

    override fun getGroupCount(): Int {
        return expandableListTitle.size
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    override fun getGroupView(listPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val listTitle = getGroup(listPosition) as String
        if (convertView == null) {
            val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.list_group, null)
        }
        val listTitleTextView = convertView!!.findViewById<View>(R.id.listTitle) as TextView
        listTitleTextView.setTypeface(null, Typeface.BOLD)
        listTitleTextView.text = listTitle
        return convertView
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return true
    }
}