package com.example.aiplantbutlernew

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

class ItemMoveCallback(private val listener: ItemMoveListener) : ItemTouchHelper.Callback() {

    /**
     * 어댑터가 이 인터페이스를 구현하여, 아이템 이동 이벤트를 받을 수 있도록 합니다.
     */
    interface ItemMoveListener {
        fun onItemMove(fromPosition: Int, toPosition: Int)
    }

    /**
     * 어떤 방향으로 드래그할지 결정합니다. (위/아래)
     * 스와이프는 사용하지 않습니다.
     */
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    /**
     * 아이템이 드래그되어 다른 아이템 위로 이동했을 때 호출됩니다.
     */
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        // 리스너를 통해 어댑터에게 아이템이 이동했음을 알립니다.
        listener.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    /**
     * 아이템이 스와이프될 때 호출됩니다. (이 앱에서는 사용하지 않음)
     */
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // No action needed
    }
}