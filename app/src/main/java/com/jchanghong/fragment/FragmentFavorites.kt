package com.jchanghong.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.LinearLayout
import com.jchanghong.ActivityNoteEdit
import com.jchanghong.R
import com.jchanghong.adapter.ListAdapterNote
import com.jchanghong.data.DatabaseManager
import com.jchanghong.model.Note

class FragmentFavorites : Fragment() {
    lateinit private var recyclerView: RecyclerView
    lateinit private var mAdapter: ListAdapterNote
    lateinit private var mview: View
    lateinit private var searchView: SearchView
    lateinit private var lyt_not_found: LinearLayout

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mview = inflater.inflate(R.layout.fragment_favorites, null)

        //connect DatabaseManager

        // activate fragment menu
        setHasOptionsMenu(true)

        recyclerView = mview.findViewById<View>(R.id.recyclerView) as RecyclerView
        lyt_not_found = mview.findViewById<View>(R.id.lyt_not_found) as LinearLayout

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = DefaultItemAnimator()

        // specify an adapter (see also next example)
        displayData(DatabaseManager.allFavNote)
        return mview
    }

    override fun onResume() {
        displayData(DatabaseManager.allFavNote)
        super.onResume()
    }

    private fun displayData(items: List<Note>) {
        mAdapter = ListAdapterNote(activity, items)
        recyclerView.adapter = mAdapter
        mAdapter.setOnItemClickListener(object : ListAdapterNote.OnItemClickListener {
            override fun onItemClick(view: View, model: Note) {
                val intent = Intent(activity, ActivityNoteEdit::class.java)
                intent.putExtra(ActivityNoteEdit.EXTRA_OBJCT, model)
                activity.startActivity(intent)
            }
        })
        if (mAdapter.itemCount == 0) {
            lyt_not_found.visibility = View.VISIBLE
        } else {
            lyt_not_found.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_note, menu)
        val searchItem = menu.findItem(R.id.action_search)
        searchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.isIconified = false
        searchView.queryHint = "Search note..."
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                try {
                    mAdapter.filter.filter(s)
                } catch (e: Exception) {
                }

                return true
            }
        })
        // Detect SearchView icon clicks
        searchView.setOnSearchClickListener { setItemsVisibility(menu, searchItem, false) }

        // Detect SearchView close
        searchView.setOnCloseListener {
            setItemsVisibility(menu, searchItem, true)
            false
        }
        searchView.onActionViewCollapsed()
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setItemsVisibility(menu: Menu, exception: MenuItem, visible: Boolean) {
        (0..menu.size() - 1)
                .map { menu.getItem(it) }
                .filter { it !== exception }
                .forEach { it.isVisible = visible }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
}
