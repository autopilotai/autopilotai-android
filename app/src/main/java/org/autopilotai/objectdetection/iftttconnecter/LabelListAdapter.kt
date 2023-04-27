package org.autopilotai.objectdetection.iftttconnecter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.autopilotai.objectdetection.R
import org.json.JSONArray

class LabelListAdapter(
    private val mContext: Context,
    private val mOnLabelClickListener: OnLabelClickListener,
    private val mLabelList: ArrayList<LabelModel> = ArrayList()
) : RecyclerView.Adapter<LabelListAdapter.LabelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val inflater = LayoutInflater.from(mContext)
        val view = inflater.inflate(R.layout.item_label, parent, false)
        val holder = LabelViewHolder(view)

        // item view is the root view for each row
        holder.itemView.setOnClickListener {

            // adapterPosition give the actual position of the item in the RecyclerView
            val position = holder.adapterPosition
            val model = mLabelList[position]
            mOnLabelClickListener.onUpdate(position, model)
        }

        // to delete the item in recycler view
        holder.labelDelete.setOnClickListener {
            val position = holder.adapterPosition
            val model = mLabelList[position]
            mOnLabelClickListener.onDelete(model)
        }

        return holder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {

        // data will be set here whenever the system thinks it's required

        // get the label at position
        val label = mLabelList[position]

        holder.labelName.text = label.name
    }

    /**
     * Returns the total number of items in the list to be displayed.
     * this will refresh when we call notifyDataSetChanged() or other related methods.
     */
    override fun getItemCount(): Int {
        return mLabelList.size
    }

    /**
     * Adds each item to list for recycler view.
     */
    fun addLabel(model: LabelModel) {
        mLabelList.add(model)
        // notifyDataSetChanged() // this method is costly I avoid it whenever possible
        notifyItemInserted(mLabelList.size)
    }

    /**
     * Updates the existing label at specific position of the list.
     */
    fun updateLabel(model: LabelModel?) {

        if (model == null) return // we cannot update the value because it is null

        for (item in mLabelList) {
            // search by id
            if (item.id == model.id) {
                val position = mLabelList.indexOf(model)
                mLabelList[position] = model
                notifyItemChanged(position)
                break // we don't need to continue anymore
            }
        }
    }

    /**
     * Removes the specified label from the list.
     *
     * @param model to be removed
     */
    fun removeLabel(model: LabelModel) {
        val position = mLabelList.indexOf(model)
        mLabelList.remove(model)
        notifyItemRemoved(position)
    }

    /**
     * Clear the list.
     */
    fun clearLabels() {
        mLabelList.clear()
        notifyDataSetChanged()
    }

    /**
     * Get the list.
     */
    fun getList(): JSONArray {
        val labelList = JSONArray()
        for (item in mLabelList) {
            labelList.put(item.name)
        }
        return labelList
    }

    fun getNextItemId(): Int {
        var id = 1
        if (mLabelList.isNotEmpty()) {
            // .last is equivalent to .size() - 1
            // we want to add 1 to that id and return it
            id = mLabelList.last().id + 1
        }
        return id
    }

    /**
     * ViewHolder implementation for holding the mapped views.
     */
    inner class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val labelName: TextView = itemView.findViewById(R.id.label_name)
        val labelDelete: TextView = itemView.findViewById(R.id.delete_label)
        //val labelDelete: ImageView = itemView.findViewById(R.id.delete_label)
    }

}