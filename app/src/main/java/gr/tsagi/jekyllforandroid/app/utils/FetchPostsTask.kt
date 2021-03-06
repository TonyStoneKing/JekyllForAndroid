package gr.tsagi.jekyllforandroid.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.TextView
import com.jchanghong.data.DatabaseManager
import com.jchanghong.model.Note
import org.eclipse.egit.github.core.Blob
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.TreeEntry
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.CommitService
import org.eclipse.egit.github.core.service.DataService
import org.eclipse.egit.github.core.service.RepositoryService
import java.io.IOException

/**
\* Created with IntelliJ IDEA.
\* User: jchanghong
\* Date: 1/30/14
\* Time: 15:14
\*/

class FetchPostsTask(c: Context?, log: TextView?) : AsyncTask<Void, String, Void>() {

    @SuppressLint("StaticFieldLeak")
    val mContext = c
    @SuppressLint("StaticFieldLeak")
    val logview = log
    private val LOG_TAG = FetchPostsTask::class.java.simpleName
    val parsePostData = ParsePostData()
    // Create the needed services
    internal var repositoryService: RepositoryService
    internal var commitService: CommitService
    internal var dataService: DataService
    internal var utility: Utility = Utility(mContext!!)

    init {

        val token = utility.token

        // Start the client
        val client = GitHubClient()
        client.setOAuth2Token(token)

        // Initiate services
        repositoryService = RepositoryService()
        commitService = CommitService(client)
        dataService = DataService(client)
    }

    private fun prin(o: Any) {
        Log.i(LOG_TAG, o.toString())
    }

    override fun onPreExecute() {
        allnotes.clear()
        super.onPreExecute()
        logview?.text = "began to syn github data......"
    }

    override fun onProgressUpdate(vararg values: String?) {
        values.forEach { logview?.text = it }
    }

    var allnotes = mutableListOf<Note>()
    /**
     * Take the List with the posts and parse the posts for data
     *postslist: post目录
     * 可能有子目录
     * currentparent当前列表的父目录。
     * 也就是分类。
     * 比如blog。project
     */
    private fun getPostDataFromList(repository: Repository, postslist: List<TreeEntry>, type: Int,currentparent:String?) {
        prin(postslist.size)
        // Get and insert the new posts information into the database
        for (post in postslist) {
//            prin(" type is: ${post.type}  path: {$post.path}")
            if (post.type == "blob") {

                val filename = post.path
//                println(post.url+"------------")
//                println(post.path+"------------")
                val filenameParts = filename.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val id = filenameParts[0]

                if (id == "") {
                    Log.d(LOG_TAG, "No id...")
                    continue
                }

                val postSha = post.sha
                val postBlob: Blob? =
                        try {
                            dataService.getBlob(repository, postSha).setEncoding(Blob.ENCODING_UTF8)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            null
                        }
                val blobBytes = postBlob?.content
                publishProgress("loading $id...")
                allnotes.add(parsePostData.getNoteFrombyte(id, filename!!,
                        blobBytes ?: "null", type,currentparent?:""))
            } else {
                try {
                    val subdir = dataService.getTree(repository, post.sha).tree
                    getPostDataFromList(repository, subdir, type,post.path)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        logview?.text = "load data complete!!!"
    }

    override fun doInBackground(vararg params: Void): Void? {
        Log.d(LOG_TAG, "Background started")
        // TODO: Support subdirectories
        val user = utility.user
        val repo = utility.repo


        // get some sha's from current state in git
        Log.d(LOG_TAG, user + " - " + repo)
        val repository: Repository

        try {
            repository = repositoryService.getRepository(user, repo)
            var baseCommitSha = ""

            // maybe the user has many branches
            val branchList = repositoryService.getBranches(repository)
            for (i in 0..branchList.size) {
                val name = branchList[i].name
                if (name == "master") {
                    baseCommitSha = repositoryService.getBranches(repository)[i]
                            .commit
                            .sha
                    break
                }
            }

            // No sync when the same sha.
            val oldSha = utility.baseCommitSha
            if (baseCommitSha == oldSha) {
                Log.d(LOG_TAG, "No Sync---------")
                publishProgress("No need to syn!")
                this.cancel(true)
                return null
            } else {
                Log.d(LOG_TAG, "Syncing...")
                publishProgress("syning...")
                utility.baseCommitSha = baseCommitSha
            }
            val treeSha = commitService.getCommit(repository, baseCommitSha).sha

            // TODO: Refactor naming here.
            val list = dataService.getTree(repository, treeSha).tree
            // Position of Posts.
            var pPos = ""
            // Position of drafts.
//            var dPos = ""

            for (aList in list) {

//                Log.d(LOG_TAG, aList.path)
                if (aList.path == "_posts") {
                    Log.d(LOG_TAG, "Found posts!")
                    pPos = aList.sha
                }
                if (aList.path == "_drafts") {
                    Log.d(LOG_TAG, "Found drafts!")
//                    dPos = aList.sha
                }
            }

            if (pPos != "") {
                val postslist = dataService.getTree(repository, pPos).tree
                getPostDataFromList(repository, postslist, 0,null)
            }
            //            if (!dPos.equals("")) {
            //                List<TreeEntry> draftslist = dataService.getTree(repository, dPos).getTree();
            //                getPostDataFromList(repository, draftslist, 1);
            //            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
        updatelocalDB()
        return null
    }

    private fun updatelocalDB() {
        publishProgress("update local database......")
        for (a in allnotes) {
            DatabaseManager.insertNoteorupdate(a)
        }
        publishProgress("update local database success")
    }
}
