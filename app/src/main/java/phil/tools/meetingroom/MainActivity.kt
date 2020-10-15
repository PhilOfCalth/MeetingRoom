package phil.tools.meetingroom

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity() {


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

    private val rowMetaData = HashMap<Int, Pair<String, String>>()

    private val rowLp = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
    private val buttonLp = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)

    init {
        buttonLp.setMargins(2, 2, 2, 2)
        buttonLp.gravity = Gravity.RIGHT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val callbackId = 42;
        Log.d(this.localClassName, "0 asking for permission ");
        checkPermissions(callbackId, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR);
        Log.d(this.localClassName, "Checking for permission ");
        if (ContextCompat.checkSelfPermission( this, android.Manifest.permission.READ_CALENDAR ) == PackageManager.PERMISSION_GRANTED ) {

            populateCalendarTable()
        }
    }

    private fun populateCalendarTable(){
        val cal = Calendar.getInstance()
        val endTimestamp = cal.timeInMillis

        val startTimestamp = endTimestamp - 1209600000 // 2 weeks

        val uri: Uri = CalendarContract.Events.CONTENT_URI
        val selection =
            "(( " + CalendarContract.Events.DTSTART + " >= " + startTimestamp + " ) AND ( " + CalendarContract.Events.DTSTART + " <= " + endTimestamp + " ) AND ( deleted != 1 ))"
//            val selection = "((${CalendarContract.Events.DTSTART} > ?) AND (" +
//                    "${CalendarContract.Events.DTSTART} < ?)"
        val selectionArgs =
            arrayOf(startTimestamp.toString(), endTimestamp.toString())
        val cur: Cursor? =
            contentResolver.query(uri, EVENT_PROJECTION, selection, null, null)
        Log.d(
            this.localClassName,
            "searched ($startTimestamp, $endTimestamp) ${cur?.count}"
        );
        while (true == cur?.moveToNext()) {
            // Get the field values
            val calId = cur.getInt(0)
            val accountName = cur.getString(1)
            val displayName = cur.getString(2)
            val ownerName = cur.getString(3)
            val startDate = cur.getString(5)
            val organiser = cur.getString(7)
            // Do something with the values...
            Log.d(this.localClassName, "found an event $displayName");
            val text =
                "$displayName, calId:$calId, startDate:$startDate, ownerName:$ownerName, accountName: $accountName, organiser:$organiser";

            addItem(calId, displayName, organiser)
        }
    }

    private fun addItem(id: Int, subject: String, organiser: String){

        rowMetaData.put(id, Pair(organiser, subject))

        val row = TableRow(this)
        row.layoutParams = rowLp
        val tv = TextView(this)
        tv.text = subject
        row.id = id
        row.addView(tv)

        addButton(id, Color.GREEN, row, "Good")
        addButton(id, Color.YELLOW, row, "OK")
        addButton(id, Color.RED, row, "Bad")

        Cal_Table.addView(row)
    }

    private fun addButton(id: Int, colour: Int, row: TableRow, feedback: String) {
        val button = FloatingActionButton(this)
        button.layoutParams = buttonLp
        button.setOnClickListener { view -> sendFeedback(view.parent as View, feedback) }
        button.backgroundTintList = ColorStateList.valueOf(colour)

        row.addView(button)
    }


    private fun sendFeedback(row: View, feedback: String){

        val metaData = rowMetaData[row.id]

        intent = Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL  , arrayOf(metaData?.first));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Meeting Feedback - ${metaData?.second}");
        intent.putExtra(Intent.EXTRA_TEXT   , feedback);
        try {
            startActivity(Intent.createChooser(intent, "Send mail..."));
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, "There are no email clients installed.",Toast.LENGTH_SHORT).show();
        }

        Cal_Table.removeView(row);
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