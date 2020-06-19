package com.vicpin.kotlinrealmextensions.view

import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import com.vicpin.kotlinrealmextensions.R
import com.vicpin.kotlinrealmextensions.extensions.isMainThread
import com.vicpin.kotlinrealmextensions.extensions.wait
import com.vicpin.kotlinrealmextensions.model.Address
import com.vicpin.kotlinrealmextensions.model.Item
import com.vicpin.kotlinrealmextensions.model.User
import com.vicpin.kotlinrealmextensions.model.UserModule
import com.vicpin.krealmextensions.*
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import io.realm.Realm
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val dbSize = 100
    val userSize = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        test.setOnClickListener {
            populateUserDb(2)
            var list = User().queryAll()
            list.forEachIndexed { index, user ->
                Log.e("查询结果出来：", user.name)
                Log.e("查询结果出来：", user.address!!.city)
                Log.e("查询结果出来：", user.address!!.street)
                Log.e("查询结果出来：", user.address!!.zip)
            }
           Log.e("查询结果出来：", list.size.toString())


        }
        //***********************************
        //See tests for complete usage
        //***********************************

        performTest("main thread") {
            Thread {
                performTest("background thread items") {
                    // User perform Test
                    performUserTest("main thread users") {
                        Thread { performUserTest("background thread users") }.start()
                    }
                }
            }.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteAll<User>()
        deleteAll<Item>()
    }

    private fun performUserTest(threadName: String, finishCallback: (() -> Unit)? = null) {

        addMessage("Starting test on $threadName with User realm configuration", important = true)

        deleteAll<User>()
        populateUserDb(userSize)

        addMessage("DB populated with $userSize users")

        addMessage("Querying users on $threadName...")

        addMessage("Result: ${User().queryAll().size} items ")
        addMessage("Result: ${queryAll<User>().size} items ")

        addMessage("Deleting users on $threadName...")

        deleteAll<User>()

        addMessage("Querying users on $threadName...")

        addMessage("Result: ${User().queryAll().size} items ")

        addMessage("Observing table changes...")

        val subscription = User().queryAllAsFlowable().subscribe {
            addMessage("Changes received on ${if (Looper.myLooper() == Looper.getMainLooper()) "main thread" else "background thread"}, total items: " + it.size)
        }

        wait(1) {
            populateUserDb(10)
        }

        wait(if (isMainThread()) 2 else 1) {
            populateUserDb(10)
        }

        wait(if (isMainThread()) 3 else 1) {
            populateUserDb(10)
        }

        wait(if (isMainThread()) 4 else 1) {
            subscription.dispose()
            addMessage("Subscription finished")
            var defaultCount = Realm.getDefaultInstance().where(User::class.java).count()
            var userCount = User().getRealmInstance().where(User::class.java).count()

            addMessage("All users from default configuration : $defaultCount")
            addMessage("All users from user configuration : $userCount")
            finishCallback?.invoke()
        }
    }

    private fun performTest(threadName: String, finishCallback: (() -> Unit)? = null) {

        addMessage("Starting test on $threadName...", important = true)

        deleteAll<Item>()
        populateDB(numItems = dbSize)

        addMessage("DB populated with $dbSize items")

        addMessage("Querying items on $threadName...")

        addMessage("Result: ${Item().queryAll().size} items ")
        addMessage("Result: ${queryAll<Item>().size} items ")

        addMessage("Deleting items on $threadName...")

        Item().deleteAll()

        addMessage("Querying items on $threadName...")

        addMessage("Result: ${Item().queryAll().size} items ")

        addMessage("Observing table changes...")

        val subscription = Item().queryAllAsFlowable().subscribe {
            addMessage("Changes received on ${if (Looper.myLooper() == Looper.getMainLooper()) "main thread" else "background thread"}, total items: " + it.size)
        }
        wait(1) {
            populateDB(numItems = 10)
        }

        wait(if (isMainThread()) 2 else 1) {
            populateDB(numItems = 10)
        }

        wait(if (isMainThread()) 3 else 1) {
            populateDB(numItems = 10)
        }

        wait(if (isMainThread()) 4 else 1) {
            subscription.dispose()
            addMessage("Subscription finished")
            finishCallback?.invoke()
        }
    }

    private fun populateUserDb(numUsers: Int) {
        Array(numUsers) {
            Log.e("看看IT", it.toString())
            User("name_%s".format("周老师"), Address("street_%d".format(it), "东莞", "松山湖"))
        }.saveAll()
    }

    private fun populateDB(numItems: Int) {
        Array(numItems) { Item() }.saveAll()

        val firstEvent = Item().queryFirst() //Or val first = queryFirst<Event>
//        Item().query { equalTo() }
//        Item().querySorted("name", Sort.DESCENDING)
        var result = Item().queryAllAsSingle()
        // Item().query { equalTo() }

        result.subscribe(object : SingleObserver<List<Item>> {


            override fun onSubscribe(d: Disposable) {

            }

            override fun onError(e: Throwable) {

            }

            override fun onSuccess(t: List<Item>) {
                //查询成功
            }

        })
//      var list= Item().queryAll()
//        Item().queryAndUpdate(Item(),)
    }

    private fun addMessage(message: String, important: Boolean = false) {
        Handler(Looper.getMainLooper()).post {
            val view = TextView(this)
            if (important) view.typeface = Typeface.DEFAULT_BOLD
            view.text = message
            mainContainer.addView(view)
            scroll.smoothScrollBy(0, 1000)
        }
    }
}
