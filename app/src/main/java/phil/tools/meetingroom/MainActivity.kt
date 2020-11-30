package phil.tools.meetingroom

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.text.Html
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.graphics.drawable.DrawableCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet

private const val CAL_STATE_KEY = "MeetingRoomCalendarState"
private const val EMAIL_FORMAT =
"""<table width="100%%"><tr>
  <td width="50%%">%s</td>
  <td width="50%%" style="vertical-align: inherit;">
    <span style="height: 100px;
                 width: 100px;
                 background-color: %s;
                 border-radius: 50%%;
                 display: inline-block;" />
  </td>
</td></table>"""
private val RATING_COLOUR_MAP = mapOf("positive" to "GREEN", "neutral" to "DARKORANGE", "negative" to "RED")


private val EVENT_PROJECTION: Array<String> = arrayOf(
        CalendarContract.Calendars._ID,                     // 0
        CalendarContract.Calendars.ACCOUNT_NAME,            // 1
        CalendarContract.Events.TITLE,                      // 2
        CalendarContract.Calendars.OWNER_ACCOUNT,           // 3
        CalendarContract.Calendars.ACCOUNT_TYPE,            // 4
        CalendarContract.Events.DTSTART,                    // 5
        CalendarContract.Events.DTEND,                      // 6
        CalendarContract.Events.ORGANIZER                   // 7
)

class MainActivity : AppCompatActivity() {

    private lateinit var interestedInEmail:String
    private var tableMetaData = LinkedHashMap<Int, RowMetaData>()
    private lateinit var emailClient:GMailSender
    private val rowLp = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)
    private val textLp = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)
    private val buttonLp = TableRow.LayoutParams(100, 120)

    private lateinit var greenCircle:Drawable
    private lateinit var yellowCircle:Drawable
    private lateinit var redCircle:Drawable

    init {
        rowLp.gravity = Gravity.CENTER_VERTICAL
        textLp.setMargins(20, 35, 0, 35)
        textLp.weight = 5f
        textLp.gravity = Gravity.CENTER_VERTICAL
        buttonLp.setMargins(15, 15, 15, 15)
        buttonLp.weight = 1f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val config = this.resources.openRawResource(R.raw.config)
        val configProperties = Properties()
        configProperties.load(config)
        interestedInEmail = configProperties.getProperty("interested_in_email")

        val unwrappedDrawable = AppCompatResources.getDrawable(this, R.drawable.circle)
        greenCircle = DrawableCompat.wrap(unwrappedDrawable!!.constantState?.newDrawable()!!)
        DrawableCompat.setTint(greenCircle, Color.GREEN)
        yellowCircle = DrawableCompat.wrap(unwrappedDrawable.constantState?.newDrawable()!!)
        DrawableCompat.setTint(yellowCircle, Color.YELLOW)
        redCircle = DrawableCompat.wrap(unwrappedDrawable.constantState?.newDrawable()!!)
        DrawableCompat.setTint(redCircle, Color.RED)

        emailClient = GMailSender(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        pullToRefresh.setOnRefreshListener(this::populateCalendarTable)

        if ( savedInstanceState?.containsKey(CAL_STATE_KEY) != true ) {
            populateCalendarTable()
        }
    }

    // invoked when the activity may be temporarily destroyed, save the instance state here
    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putSerializable(CAL_STATE_KEY, tableMetaData)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        tableMetaData = savedInstanceState.getSerializable(CAL_STATE_KEY) as LinkedHashMap<Int, RowMetaData>

        for(key in tableMetaData.keys){
            val rowMetaData = tableMetaData[key]
            addItem(key, rowMetaData!!.description, rowMetaData.organiser, rowMetaData.account)
        }
    }

    private fun populateCalendarTable(){

        val callbackId = 42
        Log.d(this.localClassName, "0 asking for permission ")
        checkPermissions(callbackId, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        Log.d(this.localClassName, "Checking for permission ")

        if(ContextCompat.checkSelfPermission( this, Manifest.permission.READ_CALENDAR ) == PackageManager.PERMISSION_GRANTED ) {
            val cal = Calendar.getInstance()
            val endTimestamp = cal.timeInMillis

            val startTimestamp = endTimestamp - 1209600000 // 2 weeks

            val uri: Uri = CalendarContract.Events.CONTENT_URI
//            val selection =
//                "(( " + CalendarContract.Events.DTSTART + " >= " + startTimestamp + " ) AND ( " + CalendarContract.Events.DTSTART + " <= " + endTimestamp + " ) AND ( deleted != 1 ))"
            val selection = "${CalendarContract.Events.DTSTART} >= ? AND " +
                    "${CalendarContract.Events.DTSTART} <= ? AND " +
                    "${CalendarContract.Events.ORGANIZER} like ? AND " +
                    " deleted != 1 AND  'declined' != ${CalendarContract.Events.SELF_ATTENDEE_STATUS}"
            val selectionArgs =
                arrayOf(startTimestamp.toString(), endTimestamp.toString(), interestedInEmail)
            val cur: Cursor? =
                contentResolver.query(uri, EVENT_PROJECTION, selection, selectionArgs, CalendarContract.Events.DTSTART)
            Log.d(
                this.localClassName,
                "searched ($startTimestamp, $endTimestamp) ${cur?.count}"
            )
            val addedMeetings = HashSet<String>()

            while (true == cur?.moveToNext()) {
                // Get the field values
                val calId = cur.getInt(0)
                val account = cur.getString(1)
                val displayName = cur.getString(2)
                val ownerName = cur.getString(3)
                val startDate = cur.getLong(5)
                val organiser = cur.getString(7)
                // Do something with the values...
                Log.d(this.localClassName, "found an event $displayName")
                val text =
                    "$displayName, calId:$calId, startDate:$startDate, ownerName:$ownerName, accountName: $account, organiser:$organiser"

                val description = "$displayName\n ${formatDate(startDate)}"
                if( ! addedMeetings.contains(description) ) {
                    tableMetaData.put(calId, RowMetaData(description, organiser, account))
                    addItem(calId, description, organiser, account)
                    addedMeetings.add(description)
                }
            }
            cur?.close()
        }
    }

    private fun addItem(id: Int, description: String, organiser: String, account: String){

        val row = TableRow(this)
        row.layoutParams = rowLp
        row.setBackgroundResource(R.drawable.row_background)
        val tv = TextView(this)
        tv.text = description
        tv.setTextColor(Color.WHITE)
        tv.layoutParams = textLp
        row.id = id
        row.addView(tv)

        if(account != organiser) {
            addButton(greenCircle, row, "positive")
            addButton(yellowCircle, row, "neutral")
            addButton(redCircle, row, "negative")
        }

        Cal_Table.addView(row)
    }

    private fun addButton(background: Drawable, row: TableRow, feedback: String) {
        val button = Button(this)
        button.layoutParams = buttonLp
        button.setOnClickListener { view -> sendFeedback(view.parent as View, feedback) }
        button.background = background

        row.addView(button)
    }


    private fun sendFeedback(row: View, rating: String){

        val metaData = tableMetaData[row.id]

        val alert = AlertDialog.Builder(this)
        alert.setTitle("${rating.capitalize()} Feedback Message\n(optional)")
        val input = EditText(this)
        alert.setView(input)

        alert.setPositiveButton("Send") { _, _ ->
            run {
                val feedbackColour = RATING_COLOUR_MAP[rating]
                val feedbackMessage = stripHtml(
                        input.text
                                .toString()
                                .replace("\n", "\\n"))
                        .replace("\\n","<br />")
                var feedback = "<u><b>You received <span style=\"color:${feedbackColour} \">$rating</span> feedback<b></u>"

                if("" != feedbackMessage){
                    feedback = "$feedback <br /><br /><b>Message</b>:<br />$feedbackMessage"
                }

                val htmlContent = String.format(EMAIL_FORMAT, feedback, feedbackColour)
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                    emailClient.sendEmail("Meeting Feedback - ${metaData?.description}", htmlContent, metaData?.organiser)
                }
                Cal_Table.removeView(row)
                tableMetaData.remove(row.id)
            }
        }

        alert.setNegativeButton("Cancel", null)
        alert.show()
    }

    private fun checkPermissions(callbackId: Int, vararg permissionsId: String) {
        var permissions = true
        for (p in permissionsId) {
            permissions =
                permissions && ContextCompat.checkSelfPermission(this, p) == PERMISSION_GRANTED
        }
        if (!permissions)
            ActivityCompat.requestPermissions(this, permissionsId, callbackId)
    }

}

@SuppressLint("SimpleDateFormat")
fun formatDate(milliSeconds: Long): String {
    // Create a DateFormatter object for displaying date in specified format.
    val formatter = SimpleDateFormat("dd/MM/yyyy hh:mm")

    // Create a calendar object that will convert the date and time value in milliseconds to date.
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = milliSeconds
    return formatter.format(calendar.time)
}

fun stripHtml(html:String): String {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
    } else {
        Html.fromHtml(html).toString()
    }
}

data class RowMetaData(val description : String, val organiser: String, val account:String): Serializable
