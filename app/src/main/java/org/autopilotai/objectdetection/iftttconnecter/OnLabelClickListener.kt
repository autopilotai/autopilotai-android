package org.autopilotai.objectdetection.iftttconnecter

interface OnLabelClickListener {

    /**
     * When the user clicks on each row this method will be invoked.
     */
    fun onUpdate(position: Int, model: LabelModel)

    /**
     * when the user clicks on delete icon this method will be invoked to remove item at position.
     */
    fun onDelete(model: LabelModel)

}